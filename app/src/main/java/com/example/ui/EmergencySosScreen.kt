package com.example.ui

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RecorderViewModel
import kotlinx.coroutines.delay

/**
 * Emergency SOS — hold the button to start a 3-second armed countdown. On completion it builds a share
 * intent containing the driver's live GPS coordinates (as a Google Maps link) plus a one-line summary of
 * the most recent logged incident, so the user can fire it off to an emergency contact in one motion.
 * Read-only over existing ViewModel state; sharing uses a standard Android chooser.
 */
@Composable
fun EmergencySosScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val lat by viewModel.latitude.collectAsState()
    val lon by viewModel.longitude.collectAsState()
    val incidents by viewModel.allIncidents.collectAsState()
    val context = LocalContext.current

    var armed by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(3) }

    LaunchedEffect(armed) {
        if (armed) {
            countdown = 3
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
            // Fire share intent
            val maps = "https://maps.google.com/?q=${"%.6f".format(lat)},${"%.6f".format(lon)}"
            val last = incidents.maxByOrNull { it.timestamp }
            val incidentLine = if (last != null)
                "Last incident: ${"%.0f".format(last.speedMph)} mph, ${"%.2f".format(last.maxGForce)}G near ${last.streetOrHighway.ifEmpty { "unknown road" }}."
            else "No incident logged."
            val msg = "🚨 EMERGENCY — I may need help while driving.\n" +
                "My live location: $maps\n$incidentLine\n— Sent by Good Drivers Defender"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
                putExtra(Intent.EXTRA_SUBJECT, "Emergency location share")
            }
            runCatching { context.startActivity(Intent.createChooser(send, "Send emergency alert")) }
            armed = false
        }
    }

    val pulse = rememberInfiniteTransition(label = "sosPulse")
    val ring by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A0606))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text("EMERGENCY SOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (armed) "SENDING IN $countdown… tap to CANCEL"
            else "Hold to arm. Shares your live GPS + last incident with an emergency contact.",
            color = if (armed) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
            fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(48.dp))

        // Big SOS button with pulsing halo
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
            Box(
                modifier = Modifier
                    .size((if (armed) 240 else 210).dp * ring)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(if (armed) Color(0xFFB91C1C) else Color(0xFFDC2626))
                    .border(4.dp, Color(0xFFFCA5A5), CircleShape)
                    .clickable { armed = !armed },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (armed) {
                        Text("$countdown", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Text("TAP TO CANCEL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Sos, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(64.dp))
                        Text("ACTIVATE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        // Live location readout
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("LIVE LOCATION BROADCAST", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "LAT ${"%.6f".format(lat)}   LON ${"%.6f".format(lon)}",
                    color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${incidents.size} incident(s) on file will be referenced",
                    color = Color(0xFF94A3B8), fontSize = 10.sp
                )
            }
        }
    }
}

@Preview
@Composable
private fun EmergencySosPreview() {
    // Preview note: requires a ViewModel at runtime; see qa_screenshots/ for rendered mockups.
}
