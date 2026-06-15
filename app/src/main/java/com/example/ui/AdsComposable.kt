package com.example.ui

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdManager
import com.example.data.BillingManager

/**
 * Compose wrapper for a banner ad. Shows nothing for premium users.
 */
@Composable
fun BannerAd(activity: Activity, billingManager: BillingManager) {
    val context = LocalContext.current
    val isPremium = billingManager.isUserPremium()
    if (isPremium) {
        // No ad for premium users
        return
    }
    AndroidView(factory = { ctx ->
        val container = FrameLayout(ctx)
        // Load the banner into the container
        AdManager.loadBanner(ctx, container)
        container
    })
}
