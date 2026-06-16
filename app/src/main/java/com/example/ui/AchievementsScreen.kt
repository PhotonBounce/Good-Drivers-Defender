package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IncidentRecord
import com.example.viewmodel.RecorderViewModel
import java.util.Calendar

/**
 * Safety Achievements — turns the driving record into a gamified profile: a driver level with an XP bar
 * plus a grid of unlockable badges, each with progress. Computed purely from the existing incident store.
 */
private data class Badge(
    val name: String,
    val desc: String,
    val icon: ImageVector,
    val progress: Float,   // 0f..1f
    val tint: Color
) { val unlocked get() = progress >= 1f }

private fun buildBadges(incidents: List<IncidentRecord>): List<Badge> {
    val total = incidents.size
    val hardBrakes = incidents.count { it.maxGForce >= 1.5 }
    val auto = incidents.count { it.isAutoCaptured }
    val maxSpeed = incidents.maxOfOrNull { it.speedMph } ?: 0.0
    val night = incidents.count {
        val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
        h >= 20 || h < 5
    }
    fun frac(v: Int, target: Int) = (v.toFloat() / target).coerceIn(0f, 1f)
    return listOf(
        Badge("First Evidence", "Log your first incident", Icons.Default.Verified, frac(total, 1), Color(0xFF22C55E)),
        Badge("Collector", "Log 10 incidents", Icons.Default.EmojiEvents, frac(total, 10), Color(0xFFFBBF24)),
        Badge("Smooth Operator", "Record with 0 hard brakes", Icons.Default.Shield,
            if (total > 0 && hardBrakes == 0) 1f else 0f, Color(0xFF38BDF8)),
        Badge("Sentinel", "Auto-capture 5 events", Icons.Default.Bolt, frac(auto, 5), Color(0xFFF97316)),
        Badge("Night Guardian", "Log a night-time incident", Icons.Default.DarkMode, frac(night, 1), Color(0xFF818CF8)),
        Badge("Speed Aware", "Record a 60+ mph event", Icons.Default.Speed,
            if (maxSpeed >= 60) 1f else (maxSpeed / 60.0).toFloat().coerceIn(0f, 1f), Color(0xFFEF4444))
    )
}

@Composable
fun AchievementsScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val incidents by viewModel.allIncidents.collectAsState()
    val badges = buildBadges(incidents)
    val unlocked = badges.count { it.unlocked }
    val xp = incidents.size * 100 + incidents.count { it.isAutoCaptured } * 50 + unlocked * 75
    val level = xp / 500 + 1
    val levelProgress = (xp % 500) / 500f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("ACHIEVEMENTS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }

        Spacer(Modifier.height(12.dp))
        // Level card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF422006)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$level", color = Color(0xFFFBBF24), fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("DRIVER LEVEL $level", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("$unlocked / ${badges.size} badges • $xp XP", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF0F172A))) {
                    Box(Modifier.fillMaxWidth(levelProgress).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Color(0xFFFBBF24)))
                }
                Text("${(levelProgress * 500).toInt()} / 500 XP to level ${level + 1}", color = Color(0xFF64748B), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("BADGES", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        badges.chunked(2).forEach { rowBadges ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowBadges.forEach { badge -> BadgeCard(Modifier.weight(1f), badge) }
                if (rowBadges.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCard(modifier: Modifier, badge: Badge) {
    val on = badge.unlocked
    Card(
        modifier = modifier.height(168.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (on) badge.tint.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(54.dp).clip(CircleShape)
                    .background(if (on) badge.tint.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (on) badge.icon else Icons.Default.Lock,
                    contentDescription = badge.name,
                    tint = if (on) badge.tint else Color(0xFF475569),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(badge.name, color = if (on) Color.White else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(badge.desc, color = Color(0xFF64748B), fontSize = 9.sp, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            if (on) {
                Text("✓ UNLOCKED", color = badge.tint, fontSize = 10.sp, fontWeight = FontWeight.Black)
            } else {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF0F172A))) {
                    Box(Modifier.fillMaxWidth(badge.progress).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(badge.tint.copy(alpha = 0.7f)))
                }
            }
        }
    }
}
