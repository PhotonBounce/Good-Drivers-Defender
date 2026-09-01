package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.aistudio.driverrecorder.gpxrt.R
import com.example.data.IncidentRecord
import java.text.SimpleDateFormat
import java.util.*

import com.example.viewmodel.RecorderViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Lock

@Composable
fun ReportGeneratorScreen(
    viewModel: RecorderViewModel,
    incident: IncidentRecord?,
    onBackToLocker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    // Observe so the gate reacts if Pro status changes while this screen is composed.
    val isPro by viewModel.isProFlow.collectAsState()

    if (!isPro) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Locked Gavel",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "⚖️ CIVIL COMPLAINT WRITER IS LOCKED",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Drafting small claims complaint drafts and police report outlines is a premium Defender Pro feature. Get unlimited access today.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { viewModel.navigateTo("upgrade") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text("UPGRADE TO PRO TO DRAFT COMPLAINTS", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(onClick = onBackToLocker) {
                        Text("Return to Storage Locker", color = Color.Gray)
                    }
                }
            }
        }
        return
    }

    if (incident == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select an incident from the Storage Locker tab to draft a complaint.", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackToLocker, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Go to Storage Locker")
                }
            }
        }
        return
    }

    val eventDate = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm:ss a z", Locale.US).format(Date(incident.timestamp))
    
    // Editable complaint parameters (saveable: rotation used to wipe a mid-edit draft)
    var plaintiffName by rememberSaveable { mutableStateOf("John Doe (" + context.getString(com.aistudio.driverrecorder.gpxrt.R.string.app_name) + " Owner)") }
    var defendantName by rememberSaveable { mutableStateOf(if (incident.defendantPlate.isNotEmpty() && incident.defendantPlate != "UNKNOWN PLATE") "Owner of Vehicle Plate [${incident.defendantPlate}]" else "Unknown Motorist") }
    var emotionalDamagesVal by rememberSaveable { mutableStateOf("$7,500.00") }
    var safetyViolationByZone by rememberSaveable { mutableStateOf(if (incident.speedMph >= incident.speedLimitMph + 8) "Yes (+8 MPH Violation)" else "Speed recorded for conditions") }

    // Computes structured complaint draft text.
    // Deliberately factual: earlier versions asserted "calibrated"/"tamper-proof"
    // hardware, auto-drafted a penalty-of-perjury certification, stamped a Java
    // hashCode() as a "digital signature", and printed a hardcoded total that did
    // not match its own line items — all removed.
    val legalDraftText = remember(plaintiffName, defendantName, emotionalDamagesVal, safetyViolationByZone, incident) {
        val outOfPocket = 495.00
        val punitive = 4500.00
        val emotional = emotionalDamagesVal.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        val totalDemand = String.format(Locale.US, "%,.2f", outOfPocket + punitive + emotional)
        val latHem = if (incident.latitude >= 0) "N" else "S"
        val lonHem = if (incident.longitude >= 0) "E" else "W"
        """
        ========================================================================
        DRAFT CIVIL COMPLAINT OUTLINE (SMALL CLAIMS) — PREPARED FROM APP LOGS
        REVIEW, VERIFY, AND COMPLETE BEFORE ANY FILING. NOT LEGAL ADVICE.
        ========================================================================

        IN THE COURT OF SMALL CLAIMS / CIVIL MUNICIPAL JURISDICTION
        STATE / JURISDICTION: ____________________ (fill in before filing)
        COUNTY OF FILING: ${incident.county.uppercase()}
        CITY OF OCCURRENCE: ${incident.city.uppercase()}

        ------------------------------------------------------------------------
        ${plaintiffName.uppercase()}
            Plaintiff,

        vs.

        ${defendantName.uppercase()}
            Defendant.
        ------------------------------------------------------------------------

        CLAIM AND BILL OF PARTICULARS OUTLINE OF EVIDENCE:

        1. STATEMENT OF JURISDICTION & VENUE:
           On $eventDate, Plaintiff was operating their vehicle with a dashcam app recording GPS speed, accelerometer readings, and timestamps, traveling on or near ${incident.streetOrHighway} in the City of ${incident.city}, County of ${incident.county}.

        2. FACTS OBSERVED BY PLAINTIFF:
           The Defendant, driving a ${if (incident.defendantCarModelColor.isNotEmpty()) incident.defendantCarModelColor else "vehicle (description to be completed)"} (Plate: ${if (incident.defendantPlate.isNotEmpty()) incident.defendantPlate else "not captured"}), was observed driving in a manner described as:
           >>> "${incident.recklessBehaviorObserved}"

        3. TELEMETRY RECORDED BY PLAINTIFF'S DEVICE:
           The app recorded the following at the time of the incident (consumer-device readings; not independently calibrated):
           - RECORDED SPEED (GPS-derived): ${String.format(Locale.US, "%.1f", incident.speedMph)} MPH
           - ZONE SPEED LIMIT (as configured by user): ${incident.speedLimitMph} MPH
           - SPEED STATUS: $safetyViolationByZone
           - PEAK ACCELEROMETER READING: ${String.format(Locale.US, "%.2f", incident.maxGForce)} G
           - GPS COORDINATES:
             Latitude: ${String.format(Locale.US, "%.6f", kotlin.math.abs(incident.latitude))}° $latHem
             Longitude: ${String.format(Locale.US, "%.6f", kotlin.math.abs(incident.longitude))}° $lonHem
             Session Reference ID: ${incident.sessionFrameFolder}

        4. CAUSES OF ACTION (VERIFY ELEMENTS FOR YOUR JURISDICTION):
           COUNT I: NEGLIGENCE
           By operating their vehicle in excess of posted limits and/or in an erratic or dangerous manner, Defendant breached their duty of reasonable care as a driver.

           COUNT II: RECKLESS ENDANGERMENT / EMOTIONAL DISTRESS
           By tailgating or cutting off Plaintiff, Defendant forced Plaintiff into emergency evasive maneuvers (peak accelerometer reading: ${String.format(Locale.US, "%.2f", incident.maxGForce)} G), presenting an immediate threat of bodily harm.

        5. DAMAGES DEMANDED (EDIT AMOUNTS TO YOUR ACTUAL LOSSES):
           Plaintiff demands judgment against the Defendant in the amount of:
           - Out-of-pocket costs: ${'$'}${String.format(Locale.US, "%,.2f", outOfPocket)}
           - Punitive damages (where permitted): ${'$'}${String.format(Locale.US, "%,.2f", punitive)}
           - Emotional distress: $emotionalDamagesVal

           TOTAL CIVIL RELIEF DEMANDED: ${'$'}$totalDemand

        ------------------------------------------------------------------------
        ABOUT THE ATTACHED LOGS:
        The speeds, coordinates, accelerometer readings, and timestamps above were
        recorded by the Good Drivers Defender app on the Plaintiff's own device at
        the time of the incident. They are consumer-device readings provided as
        supporting documentation; the Plaintiff should verify all statements
        personally before signing or filing anything.

        THIS DRAFT WAS GENERATED BY AN APP AND IS NOT LEGAL ADVICE. CONSULT A
        LICENSED ATTORNEY OR YOUR COURT'S SELF-HELP RESOURCES BEFORE FILING.
        ------------------------------------------------------------------------
        """.trimIndent()
    }

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToLocker) {
                Icon(imageVector = Icons.Default.Gavel, contentDescription = "Back", tint = Color.Red)
            }
            Text(
                text = "CIVIL COMPLAINT WRITER",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.Red, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color.Red)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "DRAFT FROM YOUR RECORDED TELEMETRY",
                    color = Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Builds a complaint outline from this incident's logs. Review and verify everything before filing — this is a starting draft, not legal advice, and courts weigh evidence at their own discretion.",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
        }

        // Editable fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = plaintiffName,
                onValueChange = { plaintiffName = it },
                label = { Text("Plaintiff Name", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.Red,
                    unfocusedLabelColor = Color.Gray
                )
            )

            OutlinedTextField(
                value = defendantName,
                onValueChange = { defendantName = it },
                label = { Text("Defendant / Violator Info", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.Red,
                    unfocusedLabelColor = Color.Gray
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = emotionalDamagesVal,
                onValueChange = { emotionalDamagesVal = it },
                label = { Text("Emotional Tort Demand", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.Red,
                    unfocusedLabelColor = Color.Gray
                )
            )

            OutlinedTextField(
                value = safetyViolationByZone,
                onValueChange = { safetyViolationByZone = it },
                label = { Text("Zone Code Violation Stat", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.Red,
                    unfocusedLabelColor = Color.Gray
                )
            )
        }

        // The pre-rendered letter document body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = legalDraftText,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Quick Copy and Transfer Action
        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Incident Report Draft", legalDraftText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Complaint copied! Ready to paste and file.", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "COPY EVIDENCE LAWSUIT PACKED TEXT",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
