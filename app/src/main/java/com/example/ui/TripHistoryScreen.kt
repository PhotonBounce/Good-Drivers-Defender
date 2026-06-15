package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IncidentRecord
import com.example.viewmodel.RecorderViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Trip History — groups logged incidents into drive "sessions" (a >2h gap starts a new session) and
 * renders one card per session with a speed sparkline plus headline stats. Gives the user a scrollable
 * record of their past drives. Read-only over the existing incident store.
 */
private data class DriveSession(
    val startTs: Long,
    val records: List<IncidentRecord>
) {
    val maxSpeed get() = records.maxOfOrNull { it.speedMph } ?: 0.0
    val maxG get() = records.maxOfOrNull { it.maxGForce } ?: 0.0
    val speeds get() = records.sortedBy { it.timestamp }.map { it.speedMph.toFloat() }
}

private fun groupSessions(incidents: List<IncidentRecord>): List<DriveSession> {
    if (incidents.isEmpty()) return emptyList()
    val sorted = incidents.sortedByDescending { it.timestamp }
    val sessions = mutableListOf<MutableList<IncidentRecord>>()
    val gap = 2 * 60 * 60 * 1000L
    for (rec in sorted) {
        val last = sessions.lastOrNull()
        if (last == null || last.last().timestamp - rec.timestamp > gap) {
            sessions.add(mutableListOf(rec))
        } else {
            last.add(rec)
        }
    }
    return sessions.map { DriveSession(it.maxOf { r -> r.timestamp }, it) }
}

@Composable
fun TripHistoryScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val incidents by viewModel.allIncidents.collectAsState()
    val sessions = groupSessions(incidents)

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
            Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("TRIP HISTORY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Text(
            "${sessions.size} drive session(s) logged",
            color = Color(0xFF94A3B8), fontSize = 11.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No trips recorded yet", color = Color(0xFF64748B), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(sessions) { session -> SessionCard(session) }
            }
        }
    }
}

@Composable
private fun SessionCard(session: DriveSession) {
    val date = remember(session.startTs) {
        SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US).format(java.util.Date(session.startTs))
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(date, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            // sparkline
            Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                val data = session.speeds
                if (data.size >= 2) {
                    val mv = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
                    val stepX = size.width / (data.size - 1).toFloat()
                    for (i in 0 until data.size - 1) {
                        drawLine(
                            Color(0xFF818CF8),
                            Offset(stepX * i, size.height - data[i] / mv * size.height),
                            Offset(stepX * (i + 1), size.height - data[i + 1] / mv * size.height),
                            strokeWidth = 4f
                        )
                    }
                } else {
                    drawLine(Color(0xFF334155), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3f)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("EVENTS", "${session.records.size}")
                Stat("TOP SPEED", "${session.maxSpeed.toInt()} mph")
                Stat("PEAK G", "%.2f".format(session.maxG))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
