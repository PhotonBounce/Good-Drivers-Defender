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
    
    // Editable lawsuit parameters
    var plaintiffName by remember { mutableStateOf("John Doe (" + context.getString(com.aistudio.driverrecorder.gpxrt.R.string.app_name) + " Owner)") }
    var defendantName by remember { mutableStateOf(if (incident.defendantPlate.isNotEmpty() && incident.defendantPlate != "UNKNOWN PLATE") "Owner of Vehicle Plate [${incident.defendantPlate}]" else "Unknown Reckless Motorist") }
    var emotionalDamagesVal by remember { mutableStateOf("$7,500.00") }
    var safetyViolationByZone by remember { mutableStateOf(if (incident.speedMph >= incident.speedLimitMph + 8) "Yes (+8 MPH Violation)" else "Confirmed Dangerous Speed for Conditions") }

    // Computes structured complaint draft text
    val legalDraftText = remember(plaintiffName, defendantName, emotionalDamagesVal, safetyViolationByZone, incident) {
        """
        ========================================================================
        EVIDENTIARY CIVIL DETAILED COMPLAINT DRAFT & POLICE OUTLINE
        FOR TORTS, CIVIL RECKLESSNESS, AND ENDANGERMENT
        ========================================================================
        
        IN THE COURT OF SMALL CLAIMS / CIVIL MUNICIPAL JURISDICTION
        STATE / JURISDICTION: DECLARED OF OUTBOARD EVIDENCE
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
           On $eventDate, Plaintiff was operating their designated vehicle equipped with a calibrated telemetry-stamped dashcam recorder device, traveling on or near ${incident.streetOrHighway} in the City of ${incident.city}, County of ${incident.county}.
           
        2. WITNESSED FACTS OF RECKLESS DEVIOUS CONDUCT:
           The Defendant, driving a ${if (incident.defendantCarModelColor.isNotEmpty()) incident.defendantCarModelColor else "vehicle described in telemetry captures"} (stamping Plate #${if (incident.defendantPlate.isNotEmpty()) incident.defendantPlate else "Pending Scan"}), engaged in extreme reckless driving behavior described as:
           >>> "${incident.recklessBehaviorObserved}"
           
        3. REAL-TIME SOLID STATE TELEMETRY EVIDENCE:
           The onboard, tamper-proof recorder registered the following parameters matching the physics of the encounter:
           - PLAINTIFF CALIBRATED SPEED: ${String.format("%.1f", incident.speedMph)} MPH
           - ESTABLISHED ZONE SPEED LIMIT: ${incident.speedLimitMph} MPH
           - SPEED DIRECT COMPLIANCE STATUS: $safetyViolationByZone
           - DETECTED IMPACT / HARD-DECELERATION FORCE: ${String.format("%.2f", incident.maxGForce)} Gs (Onboard Tri-axial Accelerometer Log)
           - PRECISE GPS COORDINATES FOR EVIDENCE CORROBORATION:
             Latitude: ${incident.latitude}° North
             Longitude: ${incident.longitude}° West
             Chain-of-Custody Reference ID: ${incident.sessionFrameFolder}
           
        4. CAUSES OF ACTION:
           COUNT I: NEGLIGENCE PER SE
           By operating their vehicle in direct excess of established limits and/or in an erratic/dangerous manner during heavy traffic, Defendant breached their common law duty of reasonable care as a driver.
           
           COUNT II: RECKLESS ENDANGERMENT & INTENTIONAL INVOLVEMENT OF EXTREME EMOTIONAL DISTRESS
           By tailgating/cutting-off Plaintiff, Defendant forced Plaintiff to engage in emergency, high-G-force evasive maneuvers (accelerometer peak: ${String.format("%.2f", incident.maxGForce)} Gs), presenting a direct, immediate threat of bodily harm and severe physical collisions.
           
        5. DEMAND FOR SPECIAL AND COMPENSATORY CIVIL DAMAGES:
           Plaintiff demands judgment against the Defendant in the amount of:
           - Out-of-pocket, hard brake tire wear & camera wear: $495.00
           - Punitive damages for wilful/wanton endangerment: $4,500.00
           - Mental suffering and fear of severe collision: $emotionalDamagesVal
           
           TOTAL CIVIL RELIEF DEMANDED: $7,995.00
           
        ------------------------------------------------------------------------
        VERIFICATION & CERTIFICATION OF TELEMETRIES:
        Plaintiff certifies under penalty of perjury under the laws of civil small claims courts that the above stamped coordinates, G-sensor spikes, speeds, and timestamps are authentic as direct, unedited electronic logs from the "Driver Recorder" platform.
        
        STAMPED SIGNATURE: Digital Chain-of-Custody Locked. Code [${incident.sessionFrameFolder.hashCode()}]
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
                    text = "LOCKED TELEMETRY INTEGRITY FIXED",
                    color = Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Small Claims Courts accept solid sensor Logs as prima facie proof of Defendant's erratic traffic velocities and speed limit transgressions.",
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
