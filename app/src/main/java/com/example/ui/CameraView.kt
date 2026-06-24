package com.example.ui

import java.text.SimpleDateFormat
import java.util.Locale

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraView(
    isFrontCamera: Boolean,
    useHardware: Boolean,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    speedMph: Double = 0.0,
    isStealthMode: Boolean = false,
    onImageCaptureReady: ((ImageCapture?) -> Unit)? = null,
    onVideoCaptureReady: ((VideoCapture<Recorder>?) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState("android.permission.CAMERA")

    var cameraBindingFailed by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(isFrontCamera, cameraPermissionState.status.isGranted, useHardware) {
        if (useHardware && !cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(useHardware, isFrontCamera, cameraPermissionState.status.isGranted, previewViewRef) {
        val currentPreviewView = previewViewRef
        if (useHardware && cameraPermissionState.status.isGranted && currentPreviewView != null) {
            cameraBindingFailed = false
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()

                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(currentPreviewView.surfaceProvider)

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    // Build VideoCapture with Recorder for 10-second evidentiary MP4 clips
                    val qualitySelector = QualitySelector.fromOrderedList(
                        listOf(Quality.HD, Quality.SD, Quality.FHD),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                    val recorder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build()
                    val videoCapture = VideoCapture.withOutput(recorder)

                    // Try binding all three use cases; fall back to Preview + ImageCapture only
                    // if the device chipset cannot support 3 concurrent use cases.
                    try {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            videoCapture
                        )
                        cameraBindingFailed = false
                        onImageCaptureReady?.invoke(imageCapture)
                        onVideoCaptureReady?.invoke(videoCapture)
                    } catch (tripleBindEx: Exception) {
                        // Fallback: Prioritize VideoCapture over ImageCapture for critical evidentiary recording!
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                videoCapture
                            )
                            cameraBindingFailed = false
                            onImageCaptureReady?.invoke(null)
                            onVideoCaptureReady?.invoke(videoCapture)
                        } catch (doubleBindEx: Exception) {
                            // Ultimate fallback: bind Preview and ImageCapture if VideoCapture is completely unsupported by device
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                                cameraBindingFailed = false
                                onImageCaptureReady?.invoke(imageCapture)
                                onVideoCaptureReady?.invoke(null)
                            } catch (e: Exception) {
                                cameraBindingFailed = true
                                onImageCaptureReady?.invoke(null)
                                onVideoCaptureReady?.invoke(null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    cameraBindingFailed = true
                    onImageCaptureReady?.invoke(null)
                    onVideoCaptureReady?.invoke(null)
                }
            }, ContextCompat.getMainExecutor(context))
        } else {
            onImageCaptureReady?.invoke(null)
            onVideoCaptureReady?.invoke(null)
        }
    }

    DisposableEffect(useHardware, isFrontCamera) {
        onDispose {
            if (useHardware) {
                onImageCaptureReady?.invoke(null)
                onVideoCaptureReady?.invoke(null)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isStealthMode) {
            // Convincing dim system screensaver clock to fully conceal and turn off both camera previews visually
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020617)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US) }
                    var timeText by remember { mutableStateOf(timeFormat.format(java.util.Date())) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            timeText = timeFormat.format(java.util.Date())
                            kotlinx.coroutines.delay(1000L)
                        }
                    }
                    Text(
                        text = timeText,
                        color = Color.Gray.copy(alpha = 0.4f), // dim gray
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            ) {
                if (useHardware && cameraPermissionState.status.isGranted && !cameraBindingFailed) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                previewViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = {}
                    )
                } else {
                    // Emulate a driving highway when camera is unavailable or permission is denied
                    // Helps test perfectly in emulators/AI Studio without hardware cameras!
                    DrivingSimulationView(
                        speedMph = speedMph,
                        isRecording = isRecording,
                        isFrontCamera = isFrontCamera
                    )
                }
            }
        }
    }
}

@Composable
fun DrivingSimulationView(
    speedMph: Double,
    isRecording: Boolean,
    isFrontCamera: Boolean
) {
    // Continuous infinite progress to animate lanes moving
    val speedFactor = (speedMph.coerceAtLeast(1.0) / 60.0).toFloat().coerceIn(0.1f, 3.0f)
    val infiniteTransition = rememberInfiniteTransition(label = "road")
    
    val animationState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / speedFactor).toInt().coerceIn(150, 4000),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_scroll"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isFrontCamera) {
            // Front camera simulation shows a stylized cockpit/user outline and cabin lighting
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                // Draw deep midnight cosmic cockpit gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )

                // Draw cabin seats & cockpit dashboard silhouette
                val cockpitPath = Path().apply {
                    moveTo(0f, canvasHeight * 0.7f)
                    quadraticTo(
                        canvasWidth * 0.25f, canvasHeight * 0.65f,
                        canvasWidth * 0.5f, canvasHeight * 0.8f
                    )
                    quadraticTo(
                        canvasWidth * 0.75f, canvasHeight * 0.65f,
                        canvasWidth, canvasHeight * 0.7f
                    )
                    lineTo(canvasWidth, canvasHeight)
                    lineTo(0f, canvasHeight)
                    close()
                }
                drawPath(cockpitPath, color = Color(0xFF020617))

                // Active telemetry glowing HUD target
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.2f),
                    radius = 250f,
                    center = Offset(canvasWidth / 2, canvasHeight * 0.4f),
                    style = Stroke(width = 4f)
                )

                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.4f),
                    radius = 120f,
                    center = Offset(canvasWidth / 2, canvasHeight * 0.4f),
                    style = Stroke(width = 8f)
                )

                // Face guide for cabin camera placement
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 180f,
                    center = Offset(canvasWidth / 2, canvasHeight * 0.38f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Front View",
                        tint = Color.Cyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "FRONT COCKPIT CAMERA SIMULATOR",
                        color = Color.Cyan.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Align devices to capture driver posture / proof of non-distraction",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // Rear camera simulation shows live moving highway roads and stars
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Sky Night background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF020617), Color(0xFF1E1B4B))
                    )
                )

                // Horizon glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF312E81).copy(alpha = 0.4f)),
                        startY = height * 0.3f,
                        endY = height * 0.5f
                    )
                )

                // Draw hills in background
                val hillPath = Path().apply {
                    moveTo(0f, height * 0.55f)
                    quadraticTo(width * 0.25f, height * 0.5f, width * 0.5f, height * 0.54f)
                    quadraticTo(width * 0.75f, height * 0.48f, width, height * 0.55f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(hillPath, color = Color(0xFF0B0F19))

                // Road outline (perspective triangle pointing to center)
                val roadPath = Path().apply {
                    moveTo(width * 0.48f, height * 0.52f)
                    lineTo(width * 0.52f, height * 0.52f)
                    lineTo(width * 1.1f, height)
                    lineTo(-width * 0.1f, height)
                    close()
                }
                drawPath(roadPath, color = Color(0xFF1E293B))

                // Side road barrier glowing lines
                drawLine(
                    color = Color.Magenta.copy(alpha = 0.6f),
                    start = Offset(width * 0.48f, height * 0.52f),
                    end = Offset(-width * 0.1f, height),
                    strokeWidth = 10f
                )
                drawLine(
                    color = Color.Magenta.copy(alpha = 0.6f),
                    start = Offset(width * 0.52f, height * 0.52f),
                    end = Offset(width * 1.1f, height),
                    strokeWidth = 10f
                )

                // Moving lane dashed lines based on animationState
                val linesCount = 6
                for (i in 0 until linesCount) {
                    val progress = ((i * 20f + animationState) % 100f) / 100f
                    // Non-linear projection to give perspective spacing
                    val y = height * 0.52f + (height * 0.48f) * progress
                    val laneLength = 40f * progress
                    val nextY = y + laneLength

                    val centerX = width / 2
                    // Let's keep it dead center for straight highway lines
                    drawLine(
                        color = Color.Yellow.copy(alpha = 0.7f),
                        start = Offset(centerX, y),
                        end = Offset(centerX, nextY),
                        strokeWidth = (2f + 12f * progress)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Demo Mode",
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Camera Simulated Route • Speed ${speedMph.toInt()} mph",
                        color = Color.Yellow,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
