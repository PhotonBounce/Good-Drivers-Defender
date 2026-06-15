package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Singleton manager for AdMob banner and interstitial ads.
 *
 * Usage:
 *   AdManager.initialize(this)           // typically in Application or MainActivity
 *   AdManager.loadBanner(this, adContainer) // pass a ViewGroup where the banner will be added
 *   AdManager.loadInterstitial(this)      // pre‑load an interstitial
 *   AdManager.showInterstitial(this)      // show if loaded and user is not premium
 */
object AdManager {
    private const val TAG = "AdManager"
    // TODO: replace with your actual AdMob unit IDs
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // sample ID
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // sample ID

    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false

    /**
     * Initialise the Mobile Ads SDK. Call once, preferably from Application#onCreate or the first Activity.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context, OnInitializationCompleteListener { _: InitializationStatus? ->
            Log.d(TAG, "MobileAds SDK initialized")
            isInitialized = true
        })
    }

    /**
     * Load and display a banner ad inside the provided [container]. The container should be a
     * ViewGroup (e.g., FrameLayout) sized to wrap content. The method adds an [AdView] to the
     * container and starts loading the ad.
     */
    fun loadBanner(context: Context, container: ViewGroup) {
        if (!isInitialized) {
            Log.w(TAG, "AdManager not initialized – calling initialize() first")
            initialize(context)
        }
        // Remove any existing AdView to avoid duplicates
        container.removeAllViews()
        val adView = AdView(context)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = BANNER_AD_UNIT_ID
        container.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        Log.d(TAG, "Banner ad request started")
    }

    /**
     * Pre‑load an interstitial ad. Call this early (e.g., after app start) so the ad is ready when you
     * need to show it.
     */
    fun loadInterstitial(context: Context) {
        if (!isInitialized) {
            Log.w(TAG, "AdManager not initialized – calling initialize() first")
            initialize(context)
        }
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial ad loaded")
                interstitialAd = ad
            }

            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                Log.e(TAG, "Failed to load interstitial ad: ${error.message}")
                interstitialAd = null
            }
        })
    }

    /**
     * Show the interstitial ad if it has been loaded and the user is not a premium subscriber.
     * The [activity] parameter is used to display the ad.
     */
    fun showInterstitial(activity: Activity, isPremium: Boolean) {
        if (isPremium) {
            Log.d(TAG, "User is premium – skipping interstitial ad")
            return
        }
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    // Load the next interstitial for future use
                    loadInterstitial(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Failed to show interstitial ad: ${adError.message}")
                    // Attempt to load another ad
                    loadInterstitial(activity)
                }
            }
            ad.show(activity)
            interstitialAd = null
        } else {
            Log.d(TAG, "Interstitial ad not ready – loading now")
            loadInterstitial(activity)
        }
    }
}
