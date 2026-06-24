package com.example.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.driverrecorder.gpxrt.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Show for 2 seconds then call onTimeout
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000L)
        onTimeout()
    }

    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }
    // SimpleDateFormat works on every API level. java.time requires API 26+ and core
    // library desugaring is not enabled, so using it here crashed the app on launch
    // for minSdk-24 devices (Android 7.0 / 7.1).
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Good Drivers Defender",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (c${BuildConfig.VERSION_CODE})",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = formatter.format(Date(now.value)),
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
