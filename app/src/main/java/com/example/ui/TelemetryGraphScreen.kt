package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RecorderViewModel
import kotlinx.coroutines.delay

/**
 * Telemetry Graph — samples live speed and G-force into rolling buffers and plots them as a dual-line
 * chart, with running min / max / average stats. Useful for reviewing how a trip is going in real time.
 * Self-contained: reads the existing currentSpeed / gForce state flows; no new ViewModel API needed.
 */
private const val MAX_SAMPLES = 60

@Composable
fun TelemetryGraphScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val speeds = remember { mutableStateListOf<Float>() }
    val gforces = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        while (true) {
            speeds.add(viewModel.currentSpeed.value.toFloat())
            gforces.add(viewModel.gForce.value.toFloat())
            if (speeds.size > MAX_SAMPLES) speeds.removeAt(0)
            if (gforces.size > MAX_SAMPLES) gforces.removeAt(0)
            delay(400L)
        }
    }

    val speedColor = Color(0xFF38BDF8)
    val gColor = Color(0xFFF97316)
    val maxSpeed = (speeds.maxOrNull() ?: 0f)
    val avgSpeed = if (speeds.isEmpty()) 0f else speeds.sum() / speeds.size
    val curSpeed = speeds.lastOrNull() ?: 0f
    val maxG = (gforces.maxOrNull() ?: 0f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("LIVE TELEMETRY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Text("Real-time speed & G-force trace", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))

        Spacer(Modifier.height(16.dp))

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LegendDot(speedColor, "SPEED (mph)")
            LegendDot(gColor, "G-FORCE")
        }
        Spacer(Modifier.height(10.dp))

        // Chart
        Card(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(Modifier.fillMaxSize().padding(12.dp)) {
                if (speeds.size < 2) {
                    Text("Waiting for telemetry… start driving.", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 0..4) {
                            val gy = size.height * i / 4f
                            drawLine(Color(0xFF1E293B), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.5f)
                        }
                        drawTrace(speeds.toList(), maxOf(maxSpeed, 40f), speedColor)
                        drawTrace(gforces.toList(), maxOf(maxG, 3f), gColor)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GraphStat(Modifier.weight(1f), "NOW", "${curSpeed.toInt()}", "mph", speedColor)
            GraphStat(Modifier.weight(1f), "MAX", "${maxSpeed.toInt()}", "mph", Color(0xFFEF4444))
            GraphStat(Modifier.weight(1f), "AVG", "${avgSpeed.toInt()}", "mph", Color(0xFF22C55E))
            GraphStat(Modifier.weight(1f), "PEAK G", "%.1f".format(maxG), "G", gColor)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun DrawScope.drawTrace(data: List<Float>, maxV: Float, color: Color) {
    if (data.size < 2) return
    val mv = if (maxV <= 0f) 1f else maxV
    val stepX = size.width / (MAX_SAMPLES - 1).toFloat()
    for (i in 0 until data.size - 1) {
        val y1 = size.height - (data[i] / mv) * size.height
        val y2 = size.height - (data[i + 1] / mv) * size.height
        drawLine(color, Offset(stepX * i, y1), Offset(stepX * (i + 1), y2), strokeWidth = 5f)
    }
}

@Composable
private fun GraphStat(modifier: Modifier, label: String, value: String, unit: String, tint: Color) {
    Card(
        modifier = modifier.height(78.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = tint, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text(unit, color = Color(0xFF94A3B8), fontSize = 9.sp)
        }
    }
}
