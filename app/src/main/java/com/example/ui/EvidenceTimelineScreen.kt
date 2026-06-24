package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IncidentRecord
import com.example.viewmodel.RecorderViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Evidence Timeline — a vertical, severity-color-coded timeline of every logged incident. Each node is
 * colored by peak G-force (green / amber / red) so the user can see at a glance how serious each event was.
 * Read-only over the existing incident store.
 */
private fun severityColor(g: Double): Color = when {
    g >= 2.0 -> Color(0xFFEF4444)
    g >= 1.5 -> Color(0xFFF97316)
    else -> Color(0xFF22C55E)
}

private fun severityLabel(g: Double): String = when {
    g >= 2.0 -> "SEVERE"
    g >= 1.5 -> "HARD"
    else -> "MINOR"
}

@Composable
fun EvidenceTimelineScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val incidents by viewModel.allIncidents.collectAsState()
    val sorted = incidents.sortedByDescending { it.timestamp }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("EVIDENCE TIMELINE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Text(
            "${sorted.size} incident(s) — newest first, color = peak force",
            color = Color(0xFF94A3B8), fontSize = 11.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )

        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No incidents recorded yet", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(sorted) { incident ->
                    TimelineRow(incident)
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(incident: IncidentRecord) {
    val color = severityColor(incident.maxGForce)
    val time = remember(incident.timestamp) {
        SimpleDateFormat("MMM d • h:mm a", Locale.US).format(java.util.Date(incident.timestamp))
    }
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // Left rail: node + connecting line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp).fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(3.dp, color.copy(alpha = 0.25f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(Color(0xFF1E293B))
            )
        }
        Spacer(Modifier.width(10.dp))
        // Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(time, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                            .border(1.dp, color, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(severityLabel(incident.maxGForce), color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    incident.streetOrHighway.ifEmpty { "Unknown road" } +
                        (if (incident.county.isNotEmpty()) " • ${incident.county}" else ""),
                    color = Color(0xFFCBD5E1), fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Metric("SPEED", "${incident.speedMph.toInt()} mph")
                    Metric("FORCE", "${"%.2f".format(incident.maxGForce)} G")
                    Metric("SOURCE", if (incident.isAutoCaptured) "AUTO" else "MANUAL")
                }
                if (incident.recklessBehaviorObserved.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("“${incident.recklessBehaviorObserved}”", color = Color(0xFFFBBF24), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
