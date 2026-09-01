package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RecorderViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parking Sentry — a "guard while parked" mode. A rotating radar sweep visualizes active monitoring;
 * when armed, sudden accelerometer spikes (bumps / hit-and-run) are auto-captured as incidents. This
 * screen exposes the armed state, an impact-sensitivity selector, and a live log of detected events
 * (the auto-captured incidents already in the store).
 */
@Composable
fun ParkingSentryScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val incidents by viewModel.allIncidents.collectAsState()
    val events = incidents.filter { it.isAutoCaptured }.sortedByDescending { it.timestamp }

    // Armed state + sensitivity live in the ViewModel where the actual accelerometer
    // monitoring runs — a local remember'd flag armed nothing and reset on navigation.
    val armed by viewModel.sentryArmed.collectAsState()
    val sensitivity by viewModel.sentrySensitivity.collectAsState()

    val sweep = rememberInfiniteTransition(label = "radar")
    val angle by sweep.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "angle"
    )
    val accent = if (armed) Color(0xFF22C55E) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B16))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("PARKING SENTRY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }

        // Radar
        Box(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(250.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val rMax = size.minDimension / 2
                // concentric rings
                for (i in 1..3) {
                    drawCircle(
                        color = accent.copy(alpha = 0.25f),
                        radius = rMax * i / 3f, center = c,
                        style = Stroke(width = 2f)
                    )
                }
                // cross-hairs
                drawLine(accent.copy(alpha = 0.18f), Offset(c.x - rMax, c.y), Offset(c.x + rMax, c.y), strokeWidth = 2f)
                drawLine(accent.copy(alpha = 0.18f), Offset(c.x, c.y - rMax), Offset(c.x, c.y + rMax), strokeWidth = 2f)
                if (armed) {
                    // rotating sweep wedge
                    drawArc(
                        color = accent.copy(alpha = 0.30f),
                        startAngle = angle, sweepAngle = 55f, useCenter = true,
                        topLeft = Offset(c.x - rMax, c.y - rMax),
                        size = androidx.compose.ui.geometry.Size(rMax * 2, rMax * 2)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = accent, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(6.dp))
                Text(if (armed) "SENTRY ARMED" else "SENTRY OFF", color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    if (armed) "Monitoring impacts — keep the app open" else "Tap arm to protect",
                    color = Color(0xFF94A3B8), fontSize = 10.sp
                )
            }
        }

        // Sensitivity chips
        Text("IMPACT SENSITIVITY", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LOW", "MEDIUM", "HIGH").forEach { level ->
                val sel = sensitivity == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Color(0xFF1D4ED8) else Color(0xFF1E293B))
                        .border(1.dp, if (sel) Color(0xFF60A5FA) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { viewModel.setSentrySensitivity(level) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(level, color = if (sel) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.setSentryArmed(!armed) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (armed) Color(0xFF991B1B) else Color(0xFF15803D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (armed) "DISARM SENTRY" else "ARM SENTRY", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))
        Text("DETECTED IMPACTS (${events.size})", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (events.isEmpty()) {
            // No false "your vehicle is clear" assurance — the sentry only knows
            // about impacts that happened while it was armed and the app was open.
            Text("No impacts recorded while armed.", color = Color(0xFF64748B), fontSize = 12.sp)
        } else {
            events.take(4).forEach { ev ->
                val t = SimpleDateFormat("MMM d • h:mm a", Locale.US).format(java.util.Date(ev.timestamp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t, color = Color.White, fontSize = 12.sp)
                    Text("${"%.2f".format(ev.maxGForce)} G", color = Color(0xFFF97316), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
