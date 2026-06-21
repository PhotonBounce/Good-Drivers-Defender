package com.example.data

import android.content.Context

/**
 * 7-day free VIP trial for new installs. On first launch we stamp the install
 * time; for the next [TRIAL_DURATION_MS] every user gets full VIP/Pro access,
 * after which they drop to the limited free tier unless they subscribe.
 *
 * The time math ([isInTrial] / [daysRemaining]) is pure and takes an injectable
 * `nowMs`, so it is unit-testable; only first-launch persistence touches the platform.
 */
class TrialManager(context: Context) {

    private val prefs = context.getSharedPreferences("trial_v1", Context.MODE_PRIVATE)

    /** Stamps the first-launch time exactly once; safe to call on every startup. Returns the stamp. */
    fun ensureStarted(nowMs: Long = System.currentTimeMillis()): Long {
        val existing = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        if (existing > 0L) return existing
        prefs.edit().putLong(KEY_FIRST_LAUNCH, nowMs).apply()
        return nowMs
    }

    fun firstLaunchMs(): Long = prefs.getLong(KEY_FIRST_LAUNCH, 0L)

    /** True while the install is still inside its free VIP window. */
    fun isInTrial(nowMs: Long = System.currentTimeMillis()): Boolean {
        val start = firstLaunchMs()
        return start > 0L && nowMs < start + TRIAL_DURATION_MS
    }

    /** Whole days left in the trial (0 once expired), rounded up, for UI display. */
    fun daysRemaining(nowMs: Long = System.currentTimeMillis()): Int {
        val start = firstLaunchMs()
        if (start <= 0L) return TRIAL_DAYS
        val remaining = (start + TRIAL_DURATION_MS) - nowMs
        if (remaining <= 0L) return 0
        return ((remaining + DAY_MS - 1) / DAY_MS).toInt().coerceIn(0, TRIAL_DAYS)
    }

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch_ms"
        const val TRIAL_DAYS = 7
        private const val DAY_MS = 24L * 60 * 60 * 1000
        const val TRIAL_DURATION_MS = TRIAL_DAYS * DAY_MS
    }
}
