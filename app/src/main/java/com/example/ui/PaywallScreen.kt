package com.example.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RecorderViewModel
import com.aistudio.driverrecorder.gpxrt.BuildConfig
import kotlinx.coroutines.delay

// Feature comparison row data
private data class FeatureRow(
    val label: String,
    val freeValue: String,
    val proValue: String,
    val proHighlight: Boolean = false
)

private val FEATURES = listOf(
    FeatureRow("Camera preview (Road + Cabin)", "✓", "✓"),
    FeatureRow("Speed & G-Force HUD", "✓", "✓"),
    FeatureRow("GPS location tracking", "✓", "✓"),
    FeatureRow("Rights Shield (TTS)", "✓", "✓"),
    FeatureRow("Trip recording duration", "30 min", "Unlimited", proHighlight = true),
    FeatureRow("LOG INCIDENT (snapshot)", "3 / day", "Unlimited", proHighlight = true),
    FeatureRow("10-second video clip", "✗", "✓", proHighlight = true),
    FeatureRow("Background audio (M4A)", "✗", "✓", proHighlight = true),
    FeatureRow("Auto-motion recording", "✗", "✓", proHighlight = true),
    FeatureRow("Stealth mode", "✗", "✓", proHighlight = true),
    FeatureRow("Witness Violator form", "✗", "✓", proHighlight = true),
    FeatureRow("CSV telemetry export", "✗", "✓", proHighlight = true),
    FeatureRow("Share MP4 / M4A / CSV", "✗", "✓", proHighlight = true),
    FeatureRow("Evidence ZIP bundle", "✗", "✓", proHighlight = true),
    FeatureRow("Trip history (all sessions)", "Last 1", "Unlimited", proHighlight = true),
)

@Composable
fun PaywallScreen(
    viewModel: RecorderViewModel,
    activity: ComponentActivity,
    onDismiss: () -> Unit
) {
    val subState by viewModel.subscriptionState.collectAsState()
    val trialActive by viewModel.trialActive.collectAsState()
    val trialDaysLeft by viewModel.trialDaysRemaining.collectAsState()

    // Real, localized prices from Google Play (falls back to defaults until the
    // product details load). Reading subState above means this recomposes — and
    // re-reads the live prices — once BillingManager reports loaded state.
    val billing = viewModel.getBillingManager()
    val monthlyPrice = billing?.getMonthlyPriceString() ?: "$4.99"
    val annualPrice = billing?.getAnnualPriceString() ?: "$34.99"

    // Animated gradient shimmer on header
    val shimmerTranslate by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue = -600f, targetValue = 1200f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
            label = "shimmerX"
        )

    // Pulsing glow on the Pro button
    val glowAlpha by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "glowA"
        )

    var selectedPlan by remember { mutableStateOf("annual") } // "monthly" or "annual"

    val bgGradient = Brush.verticalGradient(
        listOf(Color(0xFF0A0A1A), Color(0xFF0F0F2A), Color(0xFF0D0D1A))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── HEADER ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A0533), Color(0xFF2D1B69),
                                Color(0xFF1A0533)
                            ),
                            start = Offset(shimmerTranslate - 600f, 0f),
                            end = Offset(shimmerTranslate, 300f)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Crown icon
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Pro",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (subState.isPro) {
                        Text(
                            text = "✓  YOU'RE A DEFENDER PRO",
                            color = Color(0xFF22C55E),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All features unlocked. Thank you for supporting road safety!",
                            color = Color(0xFF86EFAC),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "DEFENDER PRO",
                            color = Color(0xFFFBBF24),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unlock the full evidence machine.\nProtect yourself without limits.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                // Developer debug override — DEBUG BUILDS ONLY. Never ships in release,
                // so production users cannot unlock Pro for free.
                if (BuildConfig.DEBUG) {
                    IconButton(
                        onClick = { viewModel.toggleDebugPro() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug Override",
                            tint = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }

                // Close button top-right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── FREE VIP TRIAL BANNER (new installs, not yet subscribed) ─────
            if (trialActive && !subState.isPro) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(Color(0xFF143020), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Free VIP trial active",
                            color = Color(0xFF22C55E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$trialDaysLeft ${if (trialDaysLeft == 1) "day" else "days"} left — every feature unlocked. Subscribe any time to keep VIP after it ends.",
                            color = Color(0xFF86EFAC),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── PLAN SELECTOR (only show if not Pro) ─────────────────────────
            if (!subState.isPro) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose Your Plan",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Monthly plan card
                        PlanCard(
                            modifier = Modifier.weight(1f),
                            title = "MONTHLY",
                            price = monthlyPrice,
                            period = "per month",
                            badge = null,
                            isSelected = selectedPlan == "monthly",
                            onClick = { selectedPlan = "monthly" }
                        )

                        // Annual plan card — highlighted as best value
                        PlanCard(
                            modifier = Modifier.weight(1f),
                            title = "ANNUAL",
                            price = annualPrice,
                            period = "per year",
                            badge = "BEST VALUE",
                            isSelected = selectedPlan == "annual",
                            onClick = { selectedPlan = "annual" }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Subscribe CTA button
                    Button(
                        onClick = {
                            if (selectedPlan == "monthly") {
                                viewModel.getBillingManager()?.launchMonthlyPurchase(activity)
                            } else {
                                viewModel.getBillingManager()?.launchAnnualPurchase(activity)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                shadowElevation = 24f
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            2.dp,
                            Color(0xFFFDE68A).copy(alpha = glowAlpha)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF0A0A1A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (selectedPlan == "annual") "UNLOCK DEFENDER PRO — $annualPrice/yr" else "UNLOCK DEFENDER PRO — $monthlyPrice/mo",
                                color = Color(0xFF0A0A1A),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Cancel anytime · Billed via Google Play",
                                color = Color(0xFF0A0A1A).copy(alpha = 0.65f),
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Restore purchases
                    TextButton(onClick = { viewModel.getBillingManager()?.refreshPurchases() }) {
                        Text(
                            "Restore Purchase",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── FEATURE COMPARISON TABLE ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Table header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color(0xFF1E1B4B))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("Feature", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                    Text("FREE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                    Text("PRO ⭐", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                }

                FEATURES.forEachIndexed { i, row ->
                    val bg = if (i % 2 == 0) Color(0xFF0F0F1F) else Color(0xFF131325)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .then(
                                if (i == FEATURES.lastIndex)
                                    Modifier.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                                else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.label,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1.8f),
                            lineHeight = 14.sp
                        )
                        Text(
                            text = row.freeValue,
                            color = if (row.freeValue == "✗") Color(0xFF475569) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = row.proValue,
                            color = if (row.proHighlight) Color(0xFF22C55E) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (row.proHighlight) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── TRUST SIGNALS ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrustBadge(icon = Icons.Default.Security,   text = "Evidence-grade recording stays fully on-device. We never see your footage.")
                TrustBadge(icon = Icons.Default.CreditCard, text = "Billed via Google Play. Cancel anytime from Play Store subscriptions.")
                TrustBadge(icon = Icons.Default.Lock,       text = "No account needed. Anonymized. Your data is yours.")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlanCard(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    period: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155),
        label = "planBorder"
    )
    val bgColor by animateColorAsState(
        if (isSelected) Color(0xFF1C1410) else Color(0xFF0F172A),
        label = "planBg"
    )

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(2.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = price,
                    color = if (isSelected) Color(0xFFFBBF24) else Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = period,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFFFBBF24), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF0A0A1A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // "BEST VALUE" badge
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-10).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun TrustBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF22C55E),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
