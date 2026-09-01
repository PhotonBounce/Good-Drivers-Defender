package com.example.ui

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RecorderViewModel
import kotlinx.coroutines.delay

private const val IMPACT_THRESHOLD_G = 3.5f
private const val AUTO_ALERT_SECONDS = 15

/**
 * Collision Detection — watches the live G-force WHILE THIS SCREEN IS OPEN; a spike past the
 * impact threshold flips into a full-screen crash alert with a countdown that PREPARES an
 * emergency location alert (opens the share sheet) unless the driver taps "I'M OK". A share
 * still requires the user to pick a recipient — this is deliberately not an unattended
 * auto-send (that would need SMS permission + a configured emergency contact).
 */
@Composable
fun CollisionDetectScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val gForce by viewModel.gForce.collectAsState()
    val lat by viewModel.latitude.collectAsState()
    val lon by viewModel.longitude.collectAsState()
    val context = LocalContext.current

    // Saveable: a rotation mid-countdown used to silently reset an ACTIVE crash alert
    // back to idle monitoring — in the very scenario (a crash) where rotation is likely.
    var impact by rememberSaveable { mutableStateOf(false) }
    var peakG by rememberSaveable { mutableStateOf(0.0) }
    var countdown by rememberSaveable { mutableStateOf(AUTO_ALERT_SECONDS) }

    // Detect impact from the live G stream
    LaunchedEffect(gForce) {
        if (gForce.toFloat() >= IMPACT_THRESHOLD_G && !impact) {
            impact = true
            peakG = gForce
            countdown = AUTO_ALERT_SECONDS
        } else if (impact && gForce > peakG) {
            peakG = gForce
        }
    }

    fun sendAlert() {
        // Guard against a bogus (0,0) "Null Island" link when GPS has no fix yet.
        val hasFix = kotlin.math.abs(lat) > 0.0001 || kotlin.math.abs(lon) > 0.0001
        // Locale.US: default-locale formatting renders comma decimals (48,137154) in
        // much of the world, producing a broken maps link in an actual emergency.
        val locationLine = if (hasFix)
            "Location: https://maps.google.com/?q=${"%.6f".format(java.util.Locale.US, lat)},${"%.6f".format(java.util.Locale.US, lon)}"
        else
            "Location: GPS fix unavailable — please call to check on me."
        val msg = "🚨 COLLISION ALERT — a high-impact event (${"%.1f".format(java.util.Locale.US, peakG)}G) was detected.\n" +
            "$locationLine\n— Sent via Good Drivers Defender"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, msg)
            putExtra(Intent.EXTRA_SUBJECT, "Automatic collision alert")
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Send collision alert")) }
    }

    LaunchedEffect(impact) {
        if (impact) {
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
            sendAlert()
            impact = false
        }
    }

    if (impact) CollisionAlert(peakG, countdown,
        onOk = { impact = false },
        onHelp = { sendAlert(); impact = false })
    else CollisionMonitor(gForce, onBack = { viewModel.navigateTo("dashboard") },
        onSimulate = { impact = true; peakG = 4.2; countdown = AUTO_ALERT_SECONDS })
}

@Composable
private fun CollisionMonitor(gForce: Double, onBack: () -> Unit, onSimulate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0F1E)).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.width(4.dp))
            Text("COLLISION GUARD", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ImpactGauge(current = gForce.toFloat(), threshold = IMPACT_THRESHOLD_G, alert = false)
            Spacer(Modifier.height(20.dp))
            Text("MONITORING FOR IMPACT", color = Color(0xFF22C55E), fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("Alert prepares above ${IMPACT_THRESHOLD_G}G — while this screen is open", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onSimulate,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Run impact drill", color = Color(0xFF94A3B8)) }
    }
}

@Composable
private fun CollisionAlert(peakG: Double, countdown: Int, onOk: () -> Unit, onHelp: () -> Unit) {
    val flash by rememberInfiniteTransition(label = "flash").animateFloat(
        1f, 0.35f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "f"
    )
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A0606)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFDC2626).copy(alpha = flash)).padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("⚠  COLLISION DETECTED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.height(28.dp))
        ImpactGauge(current = peakG.toFloat(), threshold = IMPACT_THRESHOLD_G, alert = true)
        Spacer(Modifier.height(20.dp))
        Text("Preparing alert to share in", color = Color(0xFFFCA5A5), fontSize = 14.sp)
        Text("$countdown", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text("seconds", color = Color(0xFFFCA5A5), fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onOk,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text("I'M OK — CANCEL", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.LocalHospital, null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text("SEND HELP NOW", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ImpactGauge(current: Float, threshold: Float, alert: Boolean) {
    val maxScale = 6f
    val frac = (current / maxScale).coerceIn(0f, 1f)
    val color = if (alert) Color(0xFFEF4444) else Color(0xFF22C55E)
    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = 20.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(Color(0xFF1E293B), 135f, 270f, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * frac, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%.1f".format(current), color = color, fontSize = 56.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text("G-FORCE", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
