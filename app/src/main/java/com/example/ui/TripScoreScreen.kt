package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IncidentRecord
import com.example.data.ScoreTrend
import com.example.viewmodel.RecorderViewModel
import kotlin.math.roundToInt

/**
 * Drive Score — a gamified "Safe Driver Score" (0..100) derived from the severity and frequency of
 * logged incidents. Encourages safer driving and gives the user a single glance-able health metric for
 * their trips. Pure read-only view over the existing incident store.
 */

private data class DriveScoreStats(
    val score: Int,
    val grade: String,
    val totalIncidents: Int,
    val hardBrakes: Int,
    val autoCaptured: Int,
    val topSpeed: Int,
    val maxG: Double,
    val adaptiveScore: Float = 100f,
    val trend: ScoreTrend = ScoreTrend.STABLE
)

private fun computeDriveScore(incidents: List<IncidentRecord>): DriveScoreStats {
    val hardBrakes = incidents.count { it.maxGForce >= 1.5 }
    val minor = incidents.size - hardBrakes
    val raw = 100 - hardBrakes * 8 - minor * 3
    val score = raw.coerceIn(0, 100)
    val grade = when {
        score >= 95 -> "A+"
        score >= 85 -> "A"
        score >= 70 -> "B"
        score >= 55 -> "C"
        score >= 40 -> "D"
        else -> "F"
    }
    return DriveScoreStats(
        score = score,
        grade = grade,
        totalIncidents = incidents.size,
        hardBrakes = hardBrakes,
        autoCaptured = incidents.count { it.isAutoCaptured },
        topSpeed = (incidents.maxOfOrNull { it.speedMph } ?: 0.0).roundToInt(),
        maxG = incidents.maxOfOrNull { it.maxGForce } ?: 0.0
    )
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF22C55E)
    score >= 50 -> Color(0xFFFBBF24)
    else -> Color(0xFFEF4444)
}

@Composable
fun TripScoreScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val incidents by viewModel.allIncidents.collectAsState()
    val adaptiveScore by viewModel.adaptiveScore.collectAsState()
    val trend by viewModel.scoreTrend.collectAsState()
    val stats = computeDriveScore(incidents).copy(adaptiveScore = adaptiveScore, trend = trend)
    TripScoreContent(stats = stats, onBack = { viewModel.navigateTo("dashboard") }, modifier = modifier)
}

@Composable
private fun TrendChip(trend: ScoreTrend) {
    val targetColor = when (trend) {
        ScoreTrend.IMPROVING -> Color(0xFF22C55E)
        ScoreTrend.STABLE -> Color(0xFFFBBF24)
        ScoreTrend.DECLINING -> Color(0xFFEF4444)
    }
    val chipColor by animateColorAsState(targetColor, animationSpec = tween(600), label = "trendColor")
    val label = when (trend) {
        ScoreTrend.IMPROVING -> "↑ IMPROVING"
        ScoreTrend.STABLE -> "→ STABLE"
        ScoreTrend.DECLINING -> "↓ DECLINING"
    }
    Box(
        modifier = Modifier
            .background(chipColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, chipColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Text(label, color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TripScoreContent(
    stats: DriveScoreStats,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = scoreColor(stats.score)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("DRIVE SCORE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Text(
            "Your safety rating for logged trips",
            color = Color(0xFF94A3B8), fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Circular score gauge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                // Track
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = 135f, sweepAngle = 270f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // Value
                drawArc(
                    color = accent,
                    startAngle = 135f, sweepAngle = 270f * (stats.score / 100f), useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${stats.score}",
                    color = accent, fontSize = 64.sp,
                    fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace
                )
                Text("/ 100", color = Color(0xFF64748B), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text("GRADE ${stats.grade}", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                TrendChip(trend = stats.trend)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Adaptive: ${"%.1f".format(stats.adaptiveScore)}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Stat grid
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), Icons.Default.Speed, "TOP SPEED", "${stats.topSpeed}", "mph", Color(0xFF38BDF8))
            StatCard(Modifier.weight(1f), Icons.Default.Bolt, "MAX FORCE", String.format("%.2f", stats.maxG), "G", Color(0xFFF97316))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), Icons.Default.WarningAmber, "HARD BRAKES", "${stats.hardBrakes}", "events", Color(0xFFEF4444))
            StatCard(Modifier.weight(1f), Icons.Default.Camera, "AUTO-SAVED", "${stats.autoCaptured}", "clips", Color(0xFF22C55E))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (stats.totalIncidents == 0)
                "✓ No incidents logged yet — flawless record. Drive safe!"
            else
                "Based on ${stats.totalIncidents} logged incident(s). Fewer hard-braking events raise your score.",
            color = Color(0xFF94A3B8), fontSize = 11.sp
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    tint: Color
) {
    Card(
        modifier = modifier.height(96.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(4.dp))
                Text(unit, color = Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(label, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
private fun TripScorePreview() {
    TripScoreContent(
        stats = DriveScoreStats(82, "A", 4, 1, 3, 71, 2.13, adaptiveScore = 85.4f, trend = ScoreTrend.IMPROVING),
        onBack = {}
    )
}
