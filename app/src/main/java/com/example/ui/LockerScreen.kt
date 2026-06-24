package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IncidentRecord
import com.example.viewmodel.RecorderViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LockerScreen(
    viewModel: RecorderViewModel,
    onSelectIncidentForSuit: (IncidentRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val incidents by viewModel.allIncidents.collectAsState()
    var expandedIncidentId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "EVIDENTIARY LOCKER",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${incidents.size} incidents recorded safely in offline memory",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(32.dp)
            )
        }

        if (incidents.isNotEmpty()) {
            var selectedTimeframe by remember { mutableStateOf("All Logs") } // "All Logs", "Last 24 Hours", "Last 1 Hour", "Custom Range"
            var isExporting by remember { mutableStateOf(false) }
            var exportTimeframeExpanded by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current

            var showCustomTimeframeDialog by remember { mutableStateOf(false) }
            val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
            var customStartText by remember { mutableStateOf(df.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))) }
            var customEndText by remember { mutableStateOf(df.format(Date(System.currentTimeMillis()))) }
            var customStartMillis by remember { mutableStateOf<Long?>(null) }
            var customEndMillis by remember { mutableStateOf<Long?>(null) }

            if (showCustomTimeframeDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomTimeframeDialog = false },
                    title = {
                        Text(
                            text = "CUSTOM EVIDENCE TIMEFRAME",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    },
                    containerColor = Color(0xFF0F172A),
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Define a precise custom window of time to filter and package evidence files. Format: YYYY-MM-DD HH:mm",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            
                            OutlinedTextField(
                                value = customStartText,
                                onValueChange = { customStartText = it },
                                label = { Text("Start Time", color = Color.Cyan) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Red,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = customEndText,
                                onValueChange = { customEndText = it },
                                label = { Text("End Time", color = Color.Cyan) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Red,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    val startD = df.parse(customStartText.trim())
                                    val endD = df.parse(customEndText.trim())
                                    if (startD != null && endD != null) {
                                        customStartMillis = startD.time
                                        customEndMillis = endD.time
                                        selectedTimeframe = "Custom Range"
                                        showCustomTimeframeDialog = false
                                        viewModel.speakText("Custom range applied.")
                                    } else {
                                        viewModel.speakText("Invalid inputs.")
                                    }
                                } catch (e: Exception) {
                                    viewModel.speakText("Check your date format.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Apply Range", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomTimeframeDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }

            // Observe so the locker reveals all incidents the moment the user upgrades,
            // without needing a navigation event to force recomposition.
            val isPro by viewModel.isProFlow.collectAsState()
            val visibleIncidents = if (isPro) incidents else incidents.take(1)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "POLICE & LAWYER COOPERATIVE PORTAL",
                                color = Color.Yellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Compile GPS coordinates, chain-of-custody hashes, audio witnesses, and dual camera overlays into a secure, encrypted ZIP package.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Timeframe Dropdown trigger
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { exportTimeframeExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(selectedTimeframe, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                                if (selectedTimeframe == "Custom Range" && customStartMillis != null) {
                                                    Text(
                                                        text = "$customStartText to $customEndText",
                                                        color = Color.Yellow,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Cyan)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = exportTimeframeExpanded,
                                        onDismissRequest = { exportTimeframeExpanded = false },
                                        modifier = Modifier.background(Color(0xFF0F172A))
                                    ) {
                                        val options = listOf("All Logs", "Last 24 Hours", "Last 1 Hour", "Custom Timeframe")
                                        options.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, color = Color.White, fontSize = 11.sp) },
                                                onClick = {
                                                    if (option == "Custom Timeframe") {
                                                        showCustomTimeframeDialog = true
                                                    } else {
                                                        selectedTimeframe = option
                                                    }
                                                    exportTimeframeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Button 1: Share ZIP
                                    Button(
                                        onClick = {
                                            if (!viewModel.isPro) {
                                                viewModel.speakText("Certified Evidence ZIP packaging is a Pro feature. Opening paywall.")
                                                viewModel.navigateTo("upgrade")
                                            } else {
                                                isExporting = true
                                                val now = System.currentTimeMillis()
                                                val targetIncidents = when (selectedTimeframe) {
                                                    "Last 1 Hour" -> incidents.filter { now - it.timestamp <= 60 * 60 * 1000L }
                                                    "Last 24 Hours" -> incidents.filter { now - it.timestamp <= 24 * 60 * 60 * 1000L }
                                                    "Custom Range" -> {
                                                        val s = customStartMillis ?: 0L
                                                        val e = customEndMillis ?: Long.MAX_VALUE
                                                        incidents.filter { it.timestamp in s..e }
                                                    }
                                                    else -> incidents
                                                }

                                                if (targetIncidents.isEmpty()) {
                                                    viewModel.speakText("No incidents logged in the chosen timeframe.")
                                                    isExporting = false
                                                } else {
                                                    viewModel.speakText("Compiling chain of custody track records. Packaging ZIP.")
                                                    viewModel.exportEvidenceZip(context, targetIncidents) { zipPath ->
                                                        isExporting = false
                                                        if (zipPath != null) {
                                                            viewModel.speakText("Zip package successfully compiled! Sharing.")
                                                            try {
                                                                val file = java.io.File(zipPath)
                                                                val authority = "${context.packageName}.fileprovider"
                                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                                                                
                                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                    type = "application/zip"
                                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Good Drivers Defender - Evidence Package")
                                                                    putExtra(android.content.Intent.EXTRA_TEXT, "Enclosed is a Good Drivers Defender evidence bundle containing timestamped telemetry and camera manifests.")
                                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                }
                                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Deliver Evidence ZIP"))
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        } else {
                                                            viewModel.speakText("Failed to compile zip evidence packet.")
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isExporting,
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share ZIP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    // Button 2: Save ZIP to Downloads
                                    Button(
                                        onClick = {
                                            if (!viewModel.isPro) {
                                                viewModel.speakText("Certified Evidence ZIP packaging is a Pro feature. Opening paywall.")
                                                viewModel.navigateTo("upgrade")
                                            } else {
                                                isExporting = true
                                                val now = System.currentTimeMillis()
                                                val targetIncidents = when (selectedTimeframe) {
                                                    "Last 1 Hour" -> incidents.filter { now - it.timestamp <= 60 * 60 * 1000L }
                                                    "Last 24 Hours" -> incidents.filter { now - it.timestamp <= 24 * 60 * 60 * 1000L }
                                                    "Custom Range" -> {
                                                        val s = customStartMillis ?: 0L
                                                        val e = customEndMillis ?: Long.MAX_VALUE
                                                        incidents.filter { it.timestamp in s..e }
                                                    }
                                                    else -> incidents
                                                }

                                                if (targetIncidents.isEmpty()) {
                                                    viewModel.speakText("No incidents logged in the chosen timeframe.")
                                                    isExporting = false
                                                } else {
                                                    viewModel.downloadEvidenceZip(context, targetIncidents) { success ->
                                                        isExporting = false
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isExporting,
                                        modifier = Modifier.weight(1.2f).height(38.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save ZIP to Downloads", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                items(visibleIncidents, key = { it.id }) { incident ->
                    val isExpanded = expandedIncidentId == incident.id
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(incident.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedIncidentId = if (isExpanded) null else incident.id },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (incident.recklessBehaviorObserved.contains("Hard Braking")) Color(0xFF1E293B) else Color(0xFF1B2A3E)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (incident.recklessBehaviorObserved.contains("Hard Braking")) Color.Yellow.copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = incident.recklessBehaviorObserved,
                                        color = if (incident.recklessBehaviorObserved.contains("Hard Braking")) Color.Yellow else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Stamped: $dateFormatted",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }

                                // Max speed at time
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${incident.speedMph.toInt()} MPH",
                                        color = if (incident.speedMph >= incident.speedLimitMph + 8) Color.Red else Color.Green,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Limit: ${incident.speedLimitMph} mph",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Compact metadata badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = incident.streetOrHighway.take(18),
                                        color = Color.Cyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Landscape, contentDescription = null, tint = Color.Green, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = incident.county.take(18),
                                        color = Color.Green,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ElectricCar, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = incident.defendantPlate.ifEmpty { "NO PLATE STAMP" },
                                        color = Color.Yellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Expanded Section
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp)
                                ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(bottom = 12.dp))

                                    // Detailed Telemetry Rows
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🛰️ ENHANCED EVIDENTIARY ANALYSIS",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = Color.Red
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Latitude / Longitude", color = Color.Gray, fontSize = 11.sp)
                                            Text("${incident.latitude}, ${incident.longitude}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Maximum G-Sensor Spike", color = Color.Gray, fontSize = 11.sp)
                                            Text("${String.format("%.2f", incident.maxGForce)} Gs (Threshold: 1.8G)", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Defendant Vehicle Info", color = Color.Gray, fontSize = 11.sp)
                                            Text(incident.defendantCarModelColor.ifEmpty { "Not entered" }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }

                                        if (incident.extraNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Plaintiff Custody Notes:", color = Color.Gray, fontSize = 11.sp)
                                            Text(incident.extraNotes, color = Color.LightGray, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "📥 SHARE SINGLE EVIDENCE FORMATS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = Color.Cyan,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        
                                        // Share Snapshot (JPG)
                                        OutlinedButton(
                                            onClick = { viewModel.shareSingleSnapshot(context, incident.id) },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Share JPG", tint = Color.Cyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Snapshot JPG", fontSize = 10.sp, maxLines = 1)
                                        }

                                        // Share Audio (M4A)
                                        OutlinedButton(
                                            onClick = {
                                                if (viewModel.isPro) {
                                                    viewModel.shareSingleAudio(context, incident.sessionFrameFolder)
                                                } else {
                                                    viewModel.speakText("Audio sharing is a Pro feature. Opening paywall.")
                                                    viewModel.navigateTo("upgrade")
                                                }
                                            },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Share M4A", tint = Color.Cyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Witness M4A", fontSize = 10.sp, maxLines = 1)
                                        }

                                        // Share CSV Track Logs
                                        OutlinedButton(
                                            onClick = {
                                                if (viewModel.isPro) {
                                                    viewModel.shareSingleTelemetryCsv(context, incident.sessionFrameFolder)
                                                } else {
                                                    viewModel.speakText("GPS CSV export is a Pro feature. Opening paywall.")
                                                    viewModel.navigateTo("upgrade")
                                                }
                                            },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Share CSV", tint = Color.Cyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Track CSV", fontSize = 10.sp, maxLines = 1)
                                        }

                                        // Share Video (MP4)
                                        OutlinedButton(
                                            onClick = {
                                                if (viewModel.isPro) {
                                                    viewModel.shareVideo(context, incident.id)
                                                } else {
                                                    viewModel.speakText("Evidentiary video sharing is a Pro feature. Opening paywall.")
                                                    viewModel.navigateTo("upgrade")
                                                }
                                            },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Magenta.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share MP4", tint = Color.Magenta, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Video MP4", fontSize = 10.sp, maxLines = 1)
                                        }
                                    }

                                    Text(
                                        text = "📥 DOWNLOAD TO PUBLIC DOWNLOADS FOLDER",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = Color.Green,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        
                                        // Save Snapshot (JPG)
                                        OutlinedButton(
                                            onClick = { viewModel.downloadSingleSnapshot(context, incident.id) },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save JPG", tint = Color.Green, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save JPG", fontSize = 10.sp, maxLines = 1)
                                        }

                                        // Save Audio (M4A)
                                        OutlinedButton(
                                            onClick = {
                                                if (viewModel.isPro) {
                                                    viewModel.downloadSingleAudio(context, incident.sessionFrameFolder)
                                                } else {
                                                    viewModel.speakText("Audio download is a Pro feature. Opening paywall.")
                                                    viewModel.navigateTo("upgrade")
                                                }
                                            },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Save M4A", tint = Color.Green, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save M4A", fontSize = 10.sp, maxLines = 1)
                                        }

                                        // Save CSV Track Logs
                                        OutlinedButton(
                                            onClick = {
                                                if (viewModel.isPro) {
                                                    viewModel.downloadSingleTelemetryCsv(context, incident.sessionFrameFolder)
                                                } else {
                                                    viewModel.speakText("GPS CSV download is a Pro feature. Opening paywall.")
                                                    viewModel.navigateTo("upgrade")
                                                }
                                            },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = "Save CSV", tint = Color.Green, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save CSV", fontSize = 10.sp, maxLines = 1)
                                        }

                                        // Save Video (MP4)
                                        OutlinedButton(
                                            onClick = {
                                                if (viewModel.isPro) {
                                                    viewModel.downloadSingleVideo(context, incident.id)
                                                } else {
                                                    viewModel.speakText("Evidentiary video download is a Pro feature. Opening paywall.")
                                                    viewModel.navigateTo("upgrade")
                                                }
                                            },
                                            modifier = Modifier.weight(1.0f),
                                            border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.7f)),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "Save MP4", tint = Color.Green, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save MP4", fontSize = 10.sp, maxLines = 1)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action buttons for single expanded item
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Delete Evidence
                                        Button(
                                            onClick = { viewModel.deleteIncident(incident.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF451A03)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Discard", color = Color.Red, fontSize = 12.sp)
                                        }

                                        // Navigate into Legal Suit Builder!
                                        Button(
                                            onClick = {
                                                if (viewModel.isPro) {
                                                    onSelectIncidentForSuit(incident)
                                                } else {
                                                    viewModel.speakText("Certified Suit Writer is a Pro feature. Opening paywall.")
                                                    viewModel.navigateTo("upgrade")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Gavel, contentDescription = "Gavel", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Draft Civil Suit", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isPro && incidents.size > 1) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.speakText("Unlock all ${incidents.size} trip logs. Opening paywall.")
                                    viewModel.navigateTo("upgrade")
                                }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                            border = BorderStroke(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked Logs",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "⭐ PRO SHIELD LOG UNLOCK",
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Upgrade to Pro to unlock remaining ${incidents.size - 1} trip sessions, ZIP evidence packaging, and video/audio downloads.",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}