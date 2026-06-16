package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RecorderViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.tan

/**
 * AR Heads-Up Display — a windshield-projection mode. A tilting artificial horizon plus an oversized
 * speed readout and a live G-meter, designed to be reflected off the windshield (toggle "MIRROR" to flip
 * it horizontally). Reads the existing speed / G-force / location flows.
 */
@Composable
fun HudProjectionScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val speed by viewModel.currentSpeed.collectAsState()
    val gForce by viewModel.gForce.collectAsState()
    val lat by viewModel.latitude.collectAsState()
    val lon by viewModel.longitude.collectAsState()

    var mirror by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            clock = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            delay(1000L)
        }
    }

    val speedColor = when {
        speed >= 75 -> Color(0xFFEF4444)
        speed >= 55 -> Color(0xFFFBBF24)
        else -> Color(0xFF22D3EE)
    }
    val tiltDeg = ((gForce - 1.0) * 8.0).coerceIn(-14.0, 14.0).toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { scaleX = if (mirror) -1f else 1f }
    ) {
        // Sky / ground halves
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0A1733)))
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF071A12)))
        }
        // Horizon + pitch ladder + reticle
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val cyc = size.height / 2f
            val off = (w / 2f) * tan(Math.toRadians(tiltDeg.toDouble())).toFloat()
            val cyan = Color(0xFF22D3EE)
            drawLine(cyan, Offset(0f, cyc - off), Offset(w, cyc + off), strokeWidth = 4f)
            // pitch ladder
            for (p in listOf(-2, -1, 1, 2)) {
                val yy = cyc + p * 60f
                drawLine(cyan.copy(alpha = 0.4f), Offset(w * 0.34f, yy), Offset(w * 0.66f, yy), strokeWidth = 2f)
            }
            // fixed center reticle
            val cx = w / 2f
            drawLine(Color(0xFF4ADE80), Offset(cx - 90f, cyc), Offset(cx - 30f, cyc), strokeWidth = 5f)
            drawLine(Color(0xFF4ADE80), Offset(cx + 30f, cyc), Offset(cx + 90f, cyc), strokeWidth = 5f)
            drawCircle(Color(0xFF4ADE80), radius = 6f, center = Offset(cx, cyc))
        }

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text("AR HUD", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            FilledTonalButton(
                onClick = { mirror = !mirror },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (mirror) Color(0xFF155E75) else Color(0xFF1E293B)
                )
            ) {
                Icon(Icons.Default.Flip, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (mirror) "MIRROR ON" else "MIRROR", color = Color.White, fontSize = 12.sp)
            }
        }

        // Center speed
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${speed.toInt()}", color = speedColor, fontSize = 150.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text("MPH", color = speedColor, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }

        // Bottom telemetry strip
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            HudReadout("TIME", clock, Color.White)
            HudReadout("G-FORCE", "%.2f".format(gForce), if (gForce > 2.0) Color(0xFFEF4444) else Color(0xFF22D3EE))
            HudReadout("POSITION", "${"%.4f".format(lat)}, ${"%.4f".format(lon)}", Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun HudReadout(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
