package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Google Play product IDs — must match exactly what you set in Play Console */
object DefenderProducts {
    const val PRO_MONTHLY = "defender_pro_monthly"
    const val PRO_ANNUAL  = "defender_pro_annual"
    val ALL = listOf(PRO_MONTHLY, PRO_ANNUAL)
}

data class SubscriptionState(
    val isPro: Boolean = false,
    val planId: String? = null,         // which product is active
    val isLoading: Boolean = true,      // still querying Play
    val billingAvailable: Boolean = true
)

class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener {

    private val TAG = "BillingManager"

    private val _subscriptionState = MutableStateFlow(SubscriptionState(isLoading = true))
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    // Cached product details for launching purchase flows
    private var monthlyDetails: ProductDetails? = null
    private var annualDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        connect()
    }

    // ─── Connection ───────────────────────────────────────────────────────────

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    scope.launch {
                        queryProductDetails()
                        queryExistingPurchases()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                    _subscriptionState.value = SubscriptionState(
                        isPro = false, isLoading = false, billingAvailable = false
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected — retrying in 3s")
                scope.launch {
                    delay(3_000)
                    connect()
                }
            }
        })
    }

    // ─── Query active subscriptions ───────────────────────────────────────────

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)
        handlePurchaseList(result.purchasesList)
    }

    private fun handlePurchaseList(purchases: List<Purchase>) {
        val activePro = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.any { it in DefenderProducts.ALL }
        }

        if (activePro != null) {
            // Acknowledge if needed
            if (!activePro.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(activePro.purchaseToken)
                    .build()
                scope.launch {
                    billingClient.acknowledgePurchase(ackParams)
                }
            }
            val planId = activePro.products.firstOrNull { it in DefenderProducts.ALL }
            _subscriptionState.value = SubscriptionState(
                isPro = true, planId = planId, isLoading = false, billingAvailable = true
            )
        } else {
            _subscriptionState.value = SubscriptionState(
                isPro = false, isLoading = false, billingAvailable = true
            )
        }
    }

    // ─── Query product details (for price display) ─────────────────────────────

    private suspend fun queryProductDetails() {
        val productList = DefenderProducts.ALL.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.productDetailsList?.forEach { details ->
                when (details.productId) {
                    DefenderProducts.PRO_MONTHLY -> monthlyDetails = details
                    DefenderProducts.PRO_ANNUAL  -> annualDetails  = details
                }
            }
        }
    }

    // ─── Launch purchase flow ─────────────────────────────────────────────────

    fun launchMonthlyPurchase(activity: Activity) = launchPurchase(activity, monthlyDetails)
    fun launchAnnualPurchase(activity: Activity)  = launchPurchase(activity, annualDetails)

    private fun launchPurchase(activity: Activity, details: ProductDetails?) {
        if (details == null) {
            Log.w(TAG, "Product details not loaded yet")
            return
        }

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return

        val productDetailsParams = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParams)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    // ─── Purchase updates callback ─────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchaseList(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${result.debugMessage}")
            }
        }
    }

    // ─── Price display helpers ─────────────────────────────────────────────────

    fun getMonthlyPriceString(): String {
        return monthlyDetails
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            ?: "$4.99"
    }

    fun getAnnualPriceString(): String {
        return annualDetails
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            ?: "$34.99"
    }

    /** Force-refresh purchase state (call on app resume) */
    fun refreshPurchases() {
        if (billingClient.isReady) {
            scope.launch { queryExistingPurchases() }
        }
    }

    /**
     * Returns true if the user currently has an active Pro subscription.
     */
    fun isUserPremium(): Boolean = _subscriptionState.value.isPro

    fun destroy() {
        billingClient.endConnection()
    }
}
