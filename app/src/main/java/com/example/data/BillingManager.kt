package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume

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
        // Only trust the answer when Play actually answered. On a transient error
        // (Play Store updating, service disconnected) purchasesList comes back
        // empty — overwriting state from it would silently downgrade a paying
        // subscriber on the next resume. Keep the previous entitlement instead.
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchaseList(result.purchasesList)
        } else {
            Log.w(TAG, "queryPurchases failed (${result.billingResult.responseCode}) — keeping previous entitlement")
            _subscriptionState.value = _subscriptionState.value.copy(isLoading = false)
        }
    }

    private fun handlePurchaseList(purchases: List<Purchase>) {
        val activePro = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.any { it in DefenderProducts.ALL }
        }

        if (activePro != null) {
            // Acknowledge if needed — Google auto-refunds unacknowledged purchases
            // after 3 days, so a silently dropped ack costs the user their Pro AND
            // the sale. Check the result and retry with backoff; the resume-time
            // re-query provides a further safety net on later launches.
            if (!activePro.isAcknowledged) {
                acknowledgeWithRetry(activePro.purchaseToken)
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

    private fun acknowledgeWithRetry(purchaseToken: String) {
        scope.launch {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            repeat(3) { attempt ->
                val result = billingClient.acknowledgePurchase(ackParams)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged")
                    return@launch
                }
                Log.w(TAG, "acknowledgePurchase attempt ${attempt + 1} failed: ${result.debugMessage}")
                delay(2_000L * (attempt + 1))
            }
            Log.e(TAG, "acknowledgePurchase failed after retries — will retry on next refresh")
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

        // Billing 8.x delivers QueryProductDetailsResult: the fetched details plus
        // the products Play could NOT serve (unknown ID / no eligible offer) — the
        // #1 clue when a price fails to appear in the paywall. The callback API is
        // wrapped in a coroutine here because the KTX helper's result shape changed
        // across 7.x → 8.x.
        val (billingResult, detailsResult) =
            suspendCancellableCoroutine<Pair<BillingResult, QueryProductDetailsResult>> { cont ->
                billingClient.queryProductDetailsAsync(params) { br, dr ->
                    if (cont.isActive) cont.resume(br to dr)
                }
            }
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            detailsResult.productDetailsList.forEach { details ->
                when (details.productId) {
                    DefenderProducts.PRO_MONTHLY -> monthlyDetails = details
                    DefenderProducts.PRO_ANNUAL  -> annualDetails  = details
                }
            }
            detailsResult.unfetchedProductList.forEach { unfetched ->
                Log.w(TAG, "Play could not fetch product: $unfetched")
            }
        } else {
            Log.w(TAG, "queryProductDetails failed: ${billingResult.debugMessage}")
        }
    }

    // ─── Launch purchase flow ─────────────────────────────────────────────────

    /** Returns false when the flow could not even be launched (no Play / details not loaded). */
    fun launchMonthlyPurchase(activity: Activity): Boolean = launchPurchase(activity, monthlyDetails)
    fun launchAnnualPurchase(activity: Activity): Boolean  = launchPurchase(activity, annualDetails)

    private fun launchPurchase(activity: Activity, details: ProductDetails?): Boolean {
        if (details == null) {
            Log.w(TAG, "Product details not loaded yet")
            return false
        }

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false

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
        return true
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
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Local state got out of sync (e.g. a transient query failure earlier):
                // the user owns the product but the app thinks they don't. Re-sync.
                Log.w(TAG, "Purchase reports already-owned — resyncing entitlement")
                scope.launch { queryExistingPurchases() }
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
            // Use the LAST phase = the recurring base price. firstOrNull() would show
            // the $0 free-trial / intro phase as "the price" when an offer is configured.
            ?.lastOrNull()
            ?.formattedPrice
            ?: "$4.99"
    }

    fun getAnnualPriceString(): String {
        return annualDetails
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
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
