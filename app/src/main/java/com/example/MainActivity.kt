package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.BillingManager
import com.example.data.IncidentRecord
import com.example.ui.DashboardScreen
import com.example.ui.LockerScreen

import com.example.ui.PaywallScreen
import com.example.ui.ReportGeneratorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.SplashScreen
import com.example.ui.VideoGalleryScreen
import com.example.ui.TripScoreScreen
import com.example.ui.EmergencySosScreen
import com.example.ui.EvidenceTimelineScreen
import com.example.viewmodel.RecorderViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

import android.view.WindowManager
import android.os.Build

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: RecorderViewModel
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the screen on continuously when this security app is foregrounded
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Show activity over lock screen so accidental power button clicks do not interrupt
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        viewModel = ViewModelProvider(this)[RecorderViewModel::class.java]

        // Initialize Play Billing and wire subscription state into ViewModel
        billingManager = BillingManager(applicationContext, lifecycleScope)
        viewModel.setBillingManager(billingManager)

        setContent {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        SplashScreen {
            showSplash = false
        }
    } else {
        MyApplicationTheme {
            AppPermissionAndOnboardingWrapper(viewModel = viewModel, activity = this)
        }
    }
}
    }

    override fun onResume() {
        super.onResume()
        // Re-validate subscription on every resume (handles side-loads and cancellations)
        if (::billingManager.isInitialized) billingManager.refreshPurchases()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::billingManager.isInitialized) billingManager.destroy()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppPermissionAndOnboardingWrapper(viewModel: RecorderViewModel, activity: ComponentActivity) {
    // Collect permissions needed for comprehensive driving telemetries
    val diagnosticPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )

    var showOnboardingInfo by remember { mutableStateOf(!diagnosticPermissionsState.allPermissionsGranted) }

    LaunchedEffect(Unit) {
        if (!diagnosticPermissionsState.allPermissionsGranted) {
            diagnosticPermissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!showOnboardingInfo) {
                MainAppBottomBar(viewModel = viewModel)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Keep track of which incident is currently being drafted in the Civil Suit Form
            var activeSuitIncident by remember { mutableStateOf<IncidentRecord?>(null) }
            val currentRoute by viewModel.currentRoute.collectAsState()

            if (showOnboardingInfo) {
                // Friendly high-contrast onboarding layout suitable for tablet/mobile mount
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Legal Tool",
                                tint = Color.Red,
                                modifier = Modifier.size(64.dp)
                            )

                            Text(
                                text = "DRIVER RECORDERS & EVIDENCE ACTIVE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "This dedicated driver safety app tracks vehicle telemetry metrics relative to the current highway zone and logs sudden deceleration events to compile solid evidence packets for civil recovery claims or police reporting.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Divider(color = Color.White.copy(alpha = 0.1f))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text("💡 CRITICAL STEPS FOR PROOF CHAIN:", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("GPS & WiFi Geo tracker logs speeds and counties", color = Color.White, fontSize = 10.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Accelerometer registers evasive hard braking force", color = Color.White, fontSize = 10.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Custom overlay timestamps are printed for chain of custody", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    diagnosticPermissionsState.launchMultiplePermissionRequest()
                                    showOnboardingInfo = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Engage Telemetries & Drive Sensors")
                            }
                        }
                    }
                }
            } else {
                // Cross routing screens
                when (currentRoute) {
            "dashboard" -> {
                DashboardScreen(viewModel = viewModel)
            }
            "locker" -> {
                LockerScreen(
                    viewModel = viewModel,
                    onSelectIncidentForSuit = { selected ->
                        activeSuitIncident = selected
                        viewModel.navigateTo("report_builder")
                    }
                )
            }
            "report_builder" -> {
                ReportGeneratorScreen(
                    viewModel = viewModel,
                    incident = activeSuitIncident,
                    onBackToLocker = { viewModel.navigateTo("locker") }
                )
            }
            "upgrade" -> {
                PaywallScreen(
                    viewModel = viewModel,
                    activity = activity,
                    onDismiss = { viewModel.navigateTo("dashboard") }
                )
            }
            "videos" -> {
                VideoGalleryScreen(viewModel = viewModel)
            }
            "drive_score" -> {
                TripScoreScreen(viewModel = viewModel)
            }
            "sos" -> {
                EmergencySosScreen(viewModel = viewModel)
            }
            "timeline" -> {
                EvidenceTimelineScreen(viewModel = viewModel)
            }
            else -> {
                DashboardScreen(viewModel = viewModel)
            }
        }
            }
        }
    }
}

@Composable
fun MainAppBottomBar(viewModel: RecorderViewModel) {
    val currentRoute by viewModel.currentRoute.collectAsState()
    val subState by viewModel.subscriptionState.collectAsState()

    NavigationBar(
        containerColor = Color(0xFF020617),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = { viewModel.navigateTo("dashboard") },
            label = { Text("Dashcam", fontSize = 10.sp) },
            icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "HUD") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White, selectedTextColor = Color.White,
                indicatorColor = Color.Red.copy(alpha = 0.8f),
                unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentRoute == "locker",
            onClick = { viewModel.navigateTo("locker") },
            label = { Text("Evidence", fontSize = 10.sp) },
            icon = { Icon(imageVector = Icons.Default.FolderSpecial, contentDescription = "Locker") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White, selectedTextColor = Color.White,
                indicatorColor = Color.Red.copy(alpha = 0.8f),
                unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentRoute == "report_builder",
            onClick = { viewModel.navigateTo("report_builder") },
            label = { Text("Suit Writer", fontSize = 10.sp) },
            icon = { Icon(imageVector = Icons.Default.Gavel, contentDescription = "Suit") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White, selectedTextColor = Color.White,
                indicatorColor = Color.Red.copy(alpha = 0.8f),
                unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray
            )
        )

        // Pro upgrade tab — glows gold for free users, checkmark for Pro
        NavigationBarItem(
            selected = currentRoute == "upgrade",
            onClick = { viewModel.navigateTo("upgrade") },
            label = {
                Text(
                    text = if (subState.isPro) "PRO ✓" else "Go Pro",
                    fontSize = 10.sp,
                    color = if (subState.isPro) Color(0xFF22C55E) else Color(0xFFFBBF24)
                )
            },
            icon = {
                Icon(
                    imageVector = if (subState.isPro) Icons.Default.WorkspacePremium else Icons.Default.Star,
                    contentDescription = "Upgrade",
                    tint = if (subState.isPro) Color(0xFF22C55E) else Color(0xFFFBBF24)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFBBF24), selectedTextColor = Color(0xFFFBBF24),
                indicatorColor = Color(0xFF78350F),
                unselectedIconColor = Color(0xFFFBBF24), unselectedTextColor = Color(0xFFFBBF24)
            )
        )

        NavigationBarItem(
            selected = currentRoute == "videos",
            onClick = { viewModel.navigateTo("videos") },
            label = { Text("Videos", fontSize = 10.sp) },
            icon = { Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = "Videos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF3B82F6),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}
