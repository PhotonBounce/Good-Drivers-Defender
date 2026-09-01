package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.RecorderViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.media.MediaPlayer

@Composable
fun AudioWaveVisualizer(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val barHeight1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val barHeight2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val barHeight3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val barHeight4 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = listOf(barHeight1, barHeight2, barHeight3, barHeight4)
        bars.forEach { heightMultiplier ->
            val height = if (isRecording) 14.dp * heightMultiplier else 3.dp
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isRecording) Color.White else Color.Gray)
            )
        }
    }
}

@Composable
fun LiveStampTimeText(hasGpsFix: Boolean) {
    var liveStampTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        // Label says UTC, so format in UTC — this stamp previously showed LOCAL time
        // under the "chain-of-custody" banner, off by the device's UTC offset.
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        while (true) {
            liveStampTime = fmt.format(Date())
            delay(33L)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "UTC: $liveStampTime",
            color = Color.White,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (hasGpsFix) Color(0xFF22C55E) else Color(0xFFFACC15))
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (hasGpsFix) "GPS FIXED" else "GPS ACQUIRING",
                color = if (hasGpsFix) Color(0xFF22C55E) else Color(0xFFFACC15),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecordingElapsedText(isRecording: Boolean) {
    var recordingElapsedSecs by remember { mutableStateOf(0L) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingElapsedSecs = 0L
            while (true) {
                delay(1000L)
                recordingElapsedSecs++
            }
        } else {
            recordingElapsedSecs = 0L
        }
    }
    val m = recordingElapsedSecs / 60
    val sec = recordingElapsedSecs % 60
    val formatted = String.format(Locale.US, "%d:%02d", m, sec)

    Text(
        text = "REC  $formatted",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
fun StealthClockText() {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US) }
    var timeText by remember { mutableStateOf(timeFormat.format(java.util.Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            timeText = timeFormat.format(java.util.Date())
            delay(1000L)
        }
    }
    Text(
        text = timeText,
        color = Color.Gray.copy(alpha = 0.4f),
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun RiskIntelligenceCard(riskLevel: Float, modifier: Modifier = Modifier) {
    val riskColor = when {
        riskLevel < 0.35f -> Color(0xFF22C55E)
        riskLevel < 0.65f -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }
    val riskLabel = when {
        riskLevel < 0.35f -> "LOW RISK"
        riskLevel < 0.65f -> "MODERATE RISK"
        else -> "HIGH RISK"
    }
    val pulse = rememberInfiniteTransition(label = "riskPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.25f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "riskAlpha"
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, riskColor.copy(alpha = if (riskLevel > 0.5f) pulseAlpha else 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = riskColor.copy(alpha = pulseAlpha))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RISK INTELLIGENCE",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = riskLabel,
                color = riskColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            Canvas(modifier = Modifier.width(80.dp).height(6.dp)) {
                val filled = size.width * riskLevel
                drawRoundRect(color = Color(0xFF1E293B), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
                if (filled > 0f) {
                    drawRoundRect(
                        color = riskColor,
                        size = size.copy(width = filled),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: RecorderViewModel,
    modifier: Modifier = Modifier
) {
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val targetSpeedLimit by viewModel.targetSpeedLimit.collectAsState()
    val isSpeedingViolation by viewModel.isSpeedingViolation.collectAsState()
    val gForce by viewModel.gForce.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val street by viewModel.street.collectAsState()
    val city by viewModel.city.collectAsState()
    val county by viewModel.county.collectAsState()
    val manualStreet by viewModel.manualOverriddenStreet.collectAsState()
    val manualCounty by viewModel.manualOverriddenCounty.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val isFrontActive by viewModel.isFrontCameraActive.collectAsState()
    val soundEnabled by viewModel.isSoundEnabled.collectAsState()
    val autoCapture by viewModel.autoCaptureEnabled.collectAsState()
    val isStealthMode by viewModel.isStealthMode.collectAsState()

    val speedWarningThreshold by viewModel.speedWarningThreshold.collectAsState()
    val frameRateFps by viewModel.frameRateFps.collectAsState()
    val recordingMode by viewModel.recordingMode.collectAsState()
    val riskLevel by viewModel.riskLevel.collectAsState()

    var showReportDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    var showRightsDialog by remember { mutableStateOf(false) }
    var isMirrorMode by remember { mutableStateOf(false) }
    var showLastRecordingDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val incidents by viewModel.allIncidents.collectAsState()

    // GPS fix indicator — green when we have a real non-zero location
    val hasGpsFix = latitude != 0.0 || longitude != 0.0



    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Sleek dark midnight
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Live Video Feed Panel (Dashboard Main Screen Portion)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                if (isStealthMode) {
                    // Dim clock screensaver
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF020617))
                            .graphicsLayer { scaleX = if (isMirrorMode) -1f else 1f },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StealthClockText()
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Good Drivers Defender • Pro Shield Active",
                                color = Color.LightGray.copy(alpha = 0.15f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                } else {
                    // Stacked split screen — ROAD (rear active) on top, CABIN (front active) on bottom
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── ROAD CAM (REAR) ──
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            CameraView(
                                isFrontCamera = false,
                                useHardware = !isFrontActive,
                                isRecording = isRecording,
                                speedMph = currentSpeed,
                                isStealthMode = false,
                                onImageCaptureReady = { capture ->
                                    if (!isFrontActive) viewModel.registerImageCapture(capture)
                                },
                                onVideoCaptureReady = { vc ->
                                    if (!isFrontActive) viewModel.registerVideoCapture(vc)
                                }
                            )
                            // Camera label badge — top-left
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (!isFrontActive) Color(0xFF22C55E) else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (!isFrontActive) "ROAD CAM  ▶ RECORDING" else "ROAD CAM  ⏸ STANDBY",
                                    color = if (!isFrontActive) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(bottom = 12.dp))

                        // ── CABIN CAM (FRONT) ──
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            CameraView(
                                isFrontCamera = true,
                                useHardware = isFrontActive,
                                isRecording = isRecording,
                                speedMph = currentSpeed,
                                isStealthMode = false,
                                onImageCaptureReady = { capture ->
                                    if (isFrontActive) viewModel.registerImageCapture(capture)
                                },
                                onVideoCaptureReady = { vc ->
                                    if (isFrontActive) viewModel.registerVideoCapture(vc)
                                }
                            )
                            // Camera label badge — top-left
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isFrontActive) Color(0xFF22C55E) else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isFrontActive) "CABIN CAM  ▶ RECORDING" else "CABIN CAM  ⏸ STANDBY",
                                    color = if (isFrontActive) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── TOP HUD OVERLAY ROW ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recording status chip with elapsed timer
                    val recBg = if (isRecording) Color(0xCCB91C1C) else Color(0x99334155)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(recBg)
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecording) {
                            // Pulsing red dot
                            val recAlpha by rememberInfiniteTransition(label = "recDot")
                                .animateFloat(
                                    initialValue = 1f, targetValue = 0.3f,
                                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                                    label = "dot"
                                )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = recAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            RecordingElapsedText(isRecording)
                            if (soundEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                AudioWaveVisualizer(isRecording = true)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF64748B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "READY",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // GPS fix indicator + offline location override button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // GPS pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (hasGpsFix) Color(0xFF22C55E) else Color(0xFFFACC15))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasGpsFix) "GPS" else "GPS…",
                                color = if (hasGpsFix) Color(0xFF22C55E) else Color(0xFFFACC15),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Location override button
                        IconButton(
                            onClick = { showOverrideDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditLocation,
                                contentDescription = "Offline Location Override",
                                tint = if (manualStreet.isNotEmpty() || manualCounty.isNotEmpty()) Color(0xFF22C55E) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom control console (Speedometers + Accelerometers + Evidence Action Buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF020617)) // Even deeper absolute black UI
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(16.dp, 12.dp, 16.dp, 16.dp)
            ) {
                // Row 1: SpeedHUD and GForce indicator side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Huge speed card
                    Card(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(130.dp)
                            .graphicsLayer { scaleX = if (isMirrorMode) -1f else 1f },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSpeedingViolation) Color(0xFF7F1D1D) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${currentSpeed.toInt()}",
                                    color = if (isSpeedingViolation) Color.White else Color.Green,
                                    fontSize = 58.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.height(64.dp)
                                )
                                Text(
                                    text = "MPH",
                                    color = if (isSpeedingViolation) Color.Yellow else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isSpeedingViolation) {
                                    Text(
                                        text = "⚡ SPEED LIMIT EXCEEDED (+8 MPH)",
                                        color = Color.Yellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Onboard Accelerometer Dynamic G-Force HUD & speed limit configurator
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(130.dp)
                            .graphicsLayer { scaleX = if (isMirrorMode) -1f else 1f },
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Max target speed limit controller card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.0f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.setTargetSpeedLimit((targetSpeedLimit - 5).coerceAtLeast(15)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", tint = Color.White)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("LIMIT", color = Color.Gray, fontSize = 9.sp)
                                    Text(
                                        text = "$targetSpeedLimit",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("MPH", color = Color.Gray, fontSize = 8.sp)
                                }

                                IconButton(
                                    onClick = { viewModel.setTargetSpeedLimit((targetSpeedLimit + 5).coerceAtMost(90)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = Color.White)
                                }
                            }
                        }

                        // Real time Shock G-meter card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.0f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (gForce > 1.5) Color(0xFF7C2D12) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$gForce G",
                                    color = if (gForce > 1.5) Color.Red else Color.Cyan,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (gForce > 1.5) "HEAVY FORCE SHOCK" else "Onboard G-Sensor",
                                    color = Color.LightGray,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                RiskIntelligenceCard(riskLevel = riskLevel)

                Spacer(modifier = Modifier.height(8.dp))

                // ── PRO TOOLS quick-launch (Drive Score / Emergency SOS / Evidence Timeline) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo("drive_score") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Drive Score", tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                            Text("SCORE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo("sos") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Sos, contentDescription = "Emergency SOS", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Text("SOS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo("timeline") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Timeline, contentDescription = "Evidence Timeline", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Text("TIMELINE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── PRO TOOLS row 2 (Parking Sentry / Live Telemetry / Trip History) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo("sentry") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Shield, contentDescription = "Parking Sentry", tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                            Text("SENTRY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo("telemetry") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShowChart, contentDescription = "Live Telemetry", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Text("GRAPH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo("trips") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Route, contentDescription = "Trip History", tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                            Text("TRIPS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── PRO TOOLS row 3 (Collision Guard / AR HUD / Achievements) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo("impact") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, contentDescription = "Collision Guard", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Text("IMPACT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo("hud") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Dashboard, contentDescription = "AR HUD", tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp))
                            Text("AR HUD", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo("badges") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements", tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                            Text("BADGES", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── ROW 2: Core Action Controls ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // START / STOP TRIP
                    Button(
                        onClick = { viewModel.toggleRecording() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFDC2626) else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop Trip" else "Start Trip",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isRecording) "STOP TRIP" else "START TRIP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = if (isRecording) "Tap to end session" else "Auto-motion ready",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }

                    // SWITCH CAMERA
                    Button(
                        onClick = { viewModel.toggleCameraSensor() },
                        modifier = Modifier.weight(0.65f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SwapCalls,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isFrontActive) "CABIN" else "ROAD",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // AUTO TRIGGER
                    Button(
                        onClick = { viewModel.toggleAutoCapture() },
                        modifier = Modifier.weight(0.65f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (autoCapture) Color(0xFF059669) else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (autoCapture) Icons.Default.DirectionsRun else Icons.Default.MotionPhotosPaused,
                                contentDescription = "Auto Capture",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (autoCapture) "AUTO" else "MANUAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── LOG INCIDENT NOW (full-width big orange button) ──
                Button(
                    onClick = { viewModel.recordIncidentNow() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFB923C))
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = "Log Incident",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "⚠  LOG INCIDENT NOW",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Snap + 10s video + telemetry locked instantly",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 8.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── WITNESS VIOLATOR + STEALTH side by side ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Witness Violator — detail form
                    Button(
                        onClick = {
                            if (viewModel.isPro) {
                                showReportDialog = true
                            } else {
                                viewModel.speakText("Suit Writer Witness Form is a Pro feature. Opening paywall.")
                                viewModel.navigateTo("upgrade")
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Witness Violator",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WITNESS VIOLATOR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    // Stealth Mode
                    Button(
                        onClick = {
                            if (viewModel.isPro) {
                                viewModel.toggleStealthMode()
                            } else {
                                viewModel.speakText("Stealth mode is a Pro feature. Opening paywall.")
                                viewModel.navigateTo("upgrade")
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStealthMode) Color(0xFF15803D) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, if (isStealthMode) Color(0xFF22C55E) else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isStealthMode) Icons.Default.Security else Icons.Default.VisibilityOff,
                            contentDescription = "Stealth",
                            tint = if (isStealthMode) Color(0xFF22C55E) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStealthMode) "STEALTH ON" else "STEALTH",
                            color = if (isStealthMode) Color(0xFF22C55E) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CONSTITUTIONAL & LEGAL RIGHTS SHIELD BUTTON
                Button(
                    onClick = { showRightsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7) // Sleek Premium Cyber Blue
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.2.dp, Color.Cyan.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Legal Shield",
                        tint = Color.Cyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONSTITUTIONAL & LEGAL RIGHTS SHIELD",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // PREMIUM CYBER-NEON "VIEW LAST RECORDING (IN-APP PREVIEW)" BUTTON
                Button(
                    onClick = {
                        // In-app preview only — this also fired an external "open folder"
                        // intent whose picker could cover the very preview the label promises.
                        showLastRecordingDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF701A75) // Dark Fuchsia/Purple Cyberpunk
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.2.dp, Color.Magenta.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "View Last Recording",
                        tint = Color.Magenta,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIEW LAST RECORDING (IN-APP PREVIEW)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // THE EVIDENCE TELEMETRY CHROME BANNER (MANDATORY SEPARATION FROM CAMERA VIDEO STAMP AREA)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = if (isMirrorMode) -1f else 1f },
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "OFFICIAL TELEMETRY CHAIN-OF-CUSTODY STAMP",
                            color = Color.Red,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Stamped line details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Hemisphere derived from sign (was hardcoded "° N"/"° W",
                                // rendering double-signed/wrong values outside the NW quadrant);
                                // Locale.US keeps the decimal point on comma-locale devices.
                                Text(
                                    "LAT: ${String.format(Locale.US, "%.6f", kotlin.math.abs(latitude))}° ${if (latitude >= 0) "N" else "S"}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "LON: ${String.format(Locale.US, "%.6f", kotlin.math.abs(longitude))}° ${if (longitude >= 0) "E" else "W"}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1.3f),
                                horizontalAlignment = Alignment.End
                            ) {
                                val currentStreet = manualStreet.ifEmpty { street }
                                val currentCounty = manualCounty.ifEmpty { county }
                                Text(
                                    "LOC: ${currentStreet.take(24)}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    "CTR: ${if (currentCounty.isNotEmpty() && !currentCounty.contains("Unknown")) currentCounty else "WiFi / GPS Warmup"}",
                                    color = Color.Cyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        LiveStampTimeText(hasGpsFix)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // PREMIUM CALIBRATION & CAPTURE SETTINGS CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🛡️ PREMIUM SENSOR & CAPTURE CALIBRATION",
                            color = Color(0xFFFBBF24),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )

                        // Setting 1: Speed Warning Threshold Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Speed Warning Buffer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Triggers TTS warning above limit", color = Color.Gray, fontSize = 9.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val thresholds = listOf(5, 8, 12, 15)
                                thresholds.forEach { valSec ->
                                    val isSelected = speedWarningThreshold == valSec
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF22C55E) else Color.Black.copy(alpha = 0.4f))
                                            .clickable { viewModel.setSpeedWarningThreshold(valSec) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "+$valSec",
                                            color = if (isSelected) Color.Black else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // Setting 2: Frame Rate FPS Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Viewfinder Frame Rate", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Telemetry stamp frequency", color = Color.Gray, fontSize = 9.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val rates = listOf(4, 8, 12)
                                rates.forEach { rate ->
                                    val isSelected = frameRateFps == rate
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF22C55E) else Color.Black.copy(alpha = 0.4f))
                                            .clickable { viewModel.setFrameRate(rate) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "$rate FPS",
                                            color = if (isSelected) Color.Black else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // Setting 3: Recording Mode Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Telemetry Capture Mode", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Format of offline logging", color = Color.Gray, fontSize = 9.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val modes = listOf("Video Clip Stream", "Stamped Frames")
                                modes.forEach { md ->
                                    val isSelected = recordingMode == md
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF22C55E) else Color.Black.copy(alpha = 0.4f))
                                            .clickable { viewModel.setRecordingMode(md) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (md == "Video Clip Stream") "VIDEO" else "FRAMES",
                                            color = if (isSelected) Color.Black else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // QUICK RECKLESS VIOLATOR INCIDENT RECORDER DIALOG
        if (showReportDialog) {
            // Saveable: an auto-rotation in a car mount used to close the dialog and
            // discard everything the user had typed about the violator.
            var plateNumber by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
            var vehicleDetails by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
            var selectedBehavior by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Reckless Cut-off / Lane Change") }
            var extraNotes by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

            val behaviors = listOf(
                "Tailgating / Dangerous Proximity",
                "Reckless Cut-off / Lane Change",
                "Street Racing / Extreme Speeding",
                "Passed on Solid Double Line / Shoulder",
                "Ran Red Light / Stop Sign",
                "Failure to Yield / Aggressive Weaving"
            )

            Dialog(onDismissRequest = { showReportDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "WITNESS RECKLESS DEFENDANT",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Red,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "This matches live telemetries (speed, coordinates, G-shock sensors) to compile a small-claims evidence outline or report package.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = plateNumber,
                            onValueChange = { plateNumber = it.uppercase() },
                            label = { Text("License Plate (e.g. 7XYZ99)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = vehicleDetails,
                            onValueChange = { vehicleDetails = it },
                            label = { Text("Vehicle Make/Model/Color (e.g., Red Toyota Prius)") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Offensive Violation", color = Color.Gray, fontSize = 12.sp)
                        Column(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            behaviors.forEach { behavior ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBehavior = behavior }
                                        .padding(10.dp, 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedBehavior == behavior,
                                        onClick = { selectedBehavior = behavior }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(behavior, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = extraNotes,
                            onValueChange = { extraNotes = it },
                            label = { Text("Plaintiff Custom Notes") },
                            maxLines = 3,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showReportDialog = false }) {
                                Text("Cancel", color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.createIncidentReport(
                                        defendantPlate = plateNumber.ifEmpty { "UNKNOWN PLATE" },
                                        defendantColorModel = vehicleDetails.ifEmpty { "UNKNOWN CAR" },
                                        behaviorObserved = selectedBehavior,
                                        extraNotes = extraNotes
                                    )
                                    showReportDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Sign & Log Evidence")
                            }
                        }
                    }
                }
            }
        }

        // OFFLINE OVERRIDE LOCATION SETTER (Allows using the app smoothly in fully offline zones)
        if (showOverrideDialog) {
            var tempOverrideStreet by remember { mutableStateOf(manualStreet) }
            var tempOverrideCounty by remember { mutableStateOf(manualCounty) }

            Dialog(onDismissRequest = { showOverrideDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Offline Route Presets",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "To build a robust small claim proof chain even with zero cell service or WiFi, preset the current driving Highway / County below:",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = tempOverrideStreet,
                            onValueChange = { tempOverrideStreet = it },
                            label = { Text("Highway / Local Road (e.g., Highway 101)") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tempOverrideCounty,
                            onValueChange = { tempOverrideCounty = it },
                            label = { Text("Current County (e.g., King County)") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                viewModel.setManualLocationOverrides("", "")
                                showOverrideDialog = false
                            }) {
                                Text("Clear Presets", color = Color.Red)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                viewModel.setManualLocationOverrides(tempOverrideStreet, tempOverrideCounty)
                                showOverrideDialog = false
                            }) {
                                Text("Save Overrides", color = Color.Green)
                            }
                        }
                    }
                }
            }
        }

        if (showRightsDialog) {
            Dialog(onDismissRequest = { showRightsDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, Color.Cyan.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Rights Protection Shield",
                                    tint = Color.Cyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LEGAL SHIELD & CHECKLISTS",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Cyan,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            IconButton(
                                onClick = { showRightsDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Scrollable content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section 1: OFFICIAL CONSTITUTIONAL DEFENSE SHIELD
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Constitutional Safeguard",
                                            tint = Color.Cyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "OFFICIAL CONSTITUTIONAL DEFENSE SHIELD",
                                            color = Color.Cyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Select a legal statement to play via Text-To-Speech (TTS) speaker to assert your constitutional rights during a traffic stop:",
                                        color = Color.LightGray,
                                        fontSize = 10.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val rightsStatements = remember { listOf(
                                        "Under the Fifth Amendment, I am choosing to remain silent. I will not answer any questions without an attorney present.",
                                        "Officer, I do not consent to any search of my vehicle, my trunk, my glove compartment, or my personal property.",
                                        "Officer, am I free to go? If not, please state the reasonable suspicion or probable cause for my detention."
                                    ) }

                                    var selectedStatementIndex by remember { mutableStateOf(0) }
                                    var dropdownExpanded by remember { mutableStateOf(false) }

                                    // Dropdown selector container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .clickable { dropdownExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (selectedStatementIndex) {
                                                    0 -> "5th Amendment: Right to Remain Silent"
                                                    1 -> "4th Amendment: Refuse Vehicle Search"
                                                    else -> "Right to Liberty: Am I Free to Go?"
                                                },
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Dropdown Arrow",
                                                tint = Color.White
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier
                                                .background(Color(0xFF1E293B))
                                                .border(1.dp, Color.Cyan.copy(alpha = 0.5f))
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("5th Amendment: Right to Remain Silent", color = Color.White, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedStatementIndex = 0
                                                    dropdownExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("4th Amendment: Refuse Vehicle Search", color = Color.White, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedStatementIndex = 1
                                                    dropdownExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Right to Liberty: Am I Free to Go?", color = Color.White, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedStatementIndex = 2
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Display statement detail
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = rightsStatements[selectedStatementIndex],
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // TTS Broadcast Button
                                    Button(
                                        onClick = { viewModel.speakText(rightsStatements[selectedStatementIndex]) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Speak Statement",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PLAY SELECTED STATEMENT (TTS) 🇺🇸",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Section 2: WARRANTLESS HOME ENTRY DEFENDER (Anti-ICE/Police residency checklist)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home Defender Safeguard",
                                            tint = Color.Cyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "WARRANTLESS HOME ENTRY DEFENDER",
                                            color = Color.Cyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Critical legal checklists when agents (ICE or Police) approach your residence without permission:",
                                        color = Color.LightGray,
                                        fontSize = 10.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    var step1Checked by remember { mutableStateOf(false) }
                                    var step2Checked by remember { mutableStateOf(false) }
                                    var step3Checked by remember { mutableStateOf(false) }
                                    var step4Checked by remember { mutableStateOf(false) }

                                    // Checklist item 1
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { step1Checked = !step1Checked },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = step1Checked,
                                            onCheckedChange = { step1Checked = it }
                                        )
                                        Text(
                                            text = "Keep door closed. Ask: 'Who is it?'",
                                            color = if (step1Checked) Color.Green else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (step1Checked) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    // Checklist item 2
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { step2Checked = !step2Checked },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = step2Checked,
                                            onCheckedChange = { step2Checked = it }
                                        )
                                        Text(
                                            text = "Ask for warrant under the door or at window.",
                                            color = if (step2Checked) Color.Green else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (step2Checked) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    // Checklist item 3
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { step3Checked = !step3Checked },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = step3Checked,
                                            onCheckedChange = { step3Checked = it }
                                        )
                                        Text(
                                            text = "Verify signed by a judge with exact address.",
                                            color = if (step3Checked) Color.Green else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (step3Checked) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    // Checklist item 4
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { step4Checked = !step4Checked },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = step4Checked,
                                            onCheckedChange = { step4Checked = it }
                                        )
                                        Text(
                                            text = "Declare refusal if no valid signed warrant.",
                                            color = if (step4Checked) Color.Green else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (step4Checked) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // TTS broadcast buttons (English and Spanish)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.speakText("I do not consent to entry. Do not enter my home without a search warrant signed by a judge. Under the Fourth Amendment, I am choosing to remain inside.")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Speak English warning",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PLAY ENGLISH 🇺🇸",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.speakText("No consiento la entrada. No entren a mi casa sin una orden de registro firmada por un juez. Bajo la Cuarta Enmienda, elijo permanecer adentro.")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Speak Spanish warning",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PLAY SPANISH 🇪🇸",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dismiss button
                        Button(
                            onClick = { showRightsDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("DISMISS RIGHTS SHIELD", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // INTERACTIVE IN-APP PLAYBACK PREVIEW DIALOG FOR LAST RECORDING
        if (showLastRecordingDialog) {
            // Loaded off the main thread — getLastRecordingInfo() walks the evidence dir
            // (listFiles + stat), which previously ran synchronously during composition.
            var lastInfoState by remember { mutableStateOf<com.example.viewmodel.LastRecordingInfo?>(null) }
            LaunchedEffect(showLastRecordingDialog) {
                lastInfoState = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    viewModel.getLastRecordingInfo()
                }
            }
            // Local snapshot: a delegated property can't smart-cast to non-null at the
            // dozen use sites below; an immutable local can.
            val lastInfo = lastInfoState
            val matchingIncidents = remember(lastInfo, incidents) {
                if (lastInfo != null) {
                    incidents.filter { it.sessionFrameFolder == lastInfo.tripId }
                } else {
                    emptyList()
                }
            }
            val mainIncident = matchingIncidents.firstOrNull()

            Dialog(onDismissRequest = { showLastRecordingDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, Color.Magenta.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = "Secured Preview",
                                    tint = Color.Magenta,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SECURED TRIP PREVIEW",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Magenta,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            IconButton(
                                onClick = { showLastRecordingDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        if (lastInfo == null) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOff,
                                        contentDescription = "No Evidence",
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "NO SECURED TRIP RECORDINGS FOUND YET",
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Drive or tap witness log to generate evidentiary data.",
                                        color = Color.DarkGray,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Subtitle Date
                                Text(
                                    text = "Trip: ${lastInfo.formattedDate}",
                                    color = Color.Yellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // 1. SNAPSHOT CONTAINER
                                val snapFile = lastInfo.snapshotFiles.firstOrNull()
                                val imageBitmap = remember(snapFile) {
                                    if (snapFile != null && snapFile.exists()) {
                                        try {
                                            BitmapFactory.decodeFile(snapFile.absolutePath)?.asImageBitmap()
                                        } catch (e: Exception) {
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    if (imageBitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = imageBitmap,
                                            contentDescription = "Witness Snapshot Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = "No snapshot",
                                                    tint = Color.DarkGray,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "SECURED FRONT CAMERA STREAM LOCKED",
                                                    color = Color.DarkGray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. AUDIO PLAYER CONSOLE
                                val audioFile = lastInfo.audioFile
                                if (audioFile != null && audioFile.exists()) {
                                    var isPlaying by remember { mutableStateOf(false) }
                                    var audioProgress by remember { mutableStateOf(0f) }
                                    var audioDuration by remember { mutableStateOf(0) }
                                    var currentPosition by remember { mutableStateOf(0) }
                                    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

                                    LaunchedEffect(showLastRecordingDialog, audioFile) {
                                        if (showLastRecordingDialog) {
                                            try {
                                                val mp = MediaPlayer().apply {
                                                    setDataSource(audioFile.absolutePath)
                                                    prepare()
                                                }
                                                mediaPlayer = mp
                                                audioDuration = mp.duration
                                                
                                                while (true) {
                                                    if (mp.isPlaying) {
                                                        currentPosition = mp.currentPosition
                                                        audioProgress = if (audioDuration > 0) currentPosition.toFloat() / audioDuration else 0f
                                                    }
                                                    delay(200L)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }

                                    DisposableEffect(Unit) {
                                        onDispose {
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                        }
                                    }

                                    fun formatMs(ms: Int): String {
                                        val seconds = (ms / 1000) % 60
                                        val minutes = (ms / (1000 * 60)) % 60
                                        return String.format(Locale.US, "%d:%02d", minutes, seconds)
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, Color.Magenta.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "🎙️ WITNESS AUDIO RECORDING PLAYBACK",
                                                color = Color.Magenta,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        mediaPlayer?.let { mp ->
                                                            if (mp.isPlaying) {
                                                                mp.pause()
                                                                isPlaying = false
                                                            } else {
                                                                mp.start()
                                                                isPlaying = true
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(Color.Magenta.copy(alpha = 0.2f), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                                        tint = Color.Magenta,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Slider(
                                                    value = audioProgress,
                                                    onValueChange = { progress ->
                                                        audioProgress = progress
                                                        mediaPlayer?.let { mp ->
                                                            val seekToMs = (progress * audioDuration).toInt()
                                                            mp.seekTo(seekToMs)
                                                            currentPosition = seekToMs
                                                        }
                                                    },
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.Magenta,
                                                        activeTrackColor = Color.Magenta,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Text(
                                                    text = "${formatMs(currentPosition)} / ${formatMs(audioDuration)}",
                                                    color = Color.LightGray,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. TELEMETRY DETAILS
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "📊 STAMPED METADATA SUMMARY",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            color = Color.Yellow
                                        )

                                        if (mainIncident != null) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Recorded Velocity", color = Color.Gray, fontSize = 10.sp)
                                                Text("${mainIncident.speedMph.toInt()} MPH (Posted limit: ${mainIncident.speedLimitMph} mph)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Peak Shock Value", color = Color.Gray, fontSize = 10.sp)
                                                Text("${String.format("%.2f", mainIncident.maxGForce)} Gs", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("GPS Coordinates", color = Color.Gray, fontSize = 10.sp)
                                                Text("${mainIncident.latitude}, ${mainIncident.longitude}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Location Jurisdiction", color = Color.Gray, fontSize = 10.sp)
                                                Text("${mainIncident.streetOrHighway}, ${mainIncident.county}", color = Color.White, fontSize = 10.sp, maxLines = 1)
                                            }
                                            if (mainIncident.defendantPlate.isNotEmpty()) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Reckless Violator ID", color = Color.Gray, fontSize = 10.sp)
                                                    Text("${mainIncident.recklessBehaviorObserved} [Plate: ${mainIncident.defendantPlate}]", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Trip Session ID", color = Color.Gray, fontSize = 10.sp)
                                                Text(lastInfo.tripId, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            Text(
                                                text = "No custom violations witnessed or logged for this session yet. Recording running in background streams.",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }
                                }

                                // 4. FORMAT EXPORTERS / ACTIONS
                                Text(
                                    text = "📥 EVIDENCE FORMAT SHARING & EXPORTS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Color.Cyan,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Share JPG
                                    OutlinedButton(
                                        onClick = { mainIncident?.let { viewModel.shareSingleSnapshot(context, it.id) } },
                                        enabled = mainIncident != null,
                                        border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share JPG", fontSize = 9.sp, maxLines = 1)
                                    }
                                    // Share M4A
                                    OutlinedButton(
                                        onClick = { viewModel.shareSingleAudio(context, lastInfo.tripId) },
                                        border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share M4A", fontSize = 9.sp, maxLines = 1)
                                    }
                                    // Share CSV
                                    OutlinedButton(
                                        onClick = { viewModel.shareSingleTelemetryCsv(context, lastInfo.tripId) },
                                        border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share CSV", fontSize = 9.sp, maxLines = 1)
                                    }
                                    // Share MP4 video clip
                                    if (lastInfo.videoFiles.isNotEmpty() && mainIncident != null) {
                                        OutlinedButton(
                                            onClick = { viewModel.shareVideo(context, mainIncident.id) },
                                            border = BorderStroke(1.dp, Color.Magenta.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Magenta, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share MP4", fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Save JPG
                                    OutlinedButton(
                                        onClick = { mainIncident?.let { viewModel.downloadSingleSnapshot(context, it.id) } },
                                        enabled = mainIncident != null,
                                        border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save JPG", fontSize = 9.sp, maxLines = 1)
                                    }
                                    // Save M4A
                                    OutlinedButton(
                                        onClick = { viewModel.downloadSingleAudio(context, lastInfo.tripId) },
                                        border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save M4A", fontSize = 9.sp, maxLines = 1)
                                    }
                                    // Save CSV
                                    OutlinedButton(
                                        onClick = { viewModel.downloadSingleTelemetryCsv(context, lastInfo.tripId) },
                                        border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save CSV", fontSize = 9.sp, maxLines = 1)
                                    }
                                    // Save MP4 video clip
                                    if (lastInfo.videoFiles.isNotEmpty() && mainIncident != null) {
                                        OutlinedButton(
                                            onClick = { viewModel.downloadSingleVideo(context, mainIncident.id) },
                                            border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.7f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save MP4", fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showLastRecordingDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("DISMISS PREVIEW", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
