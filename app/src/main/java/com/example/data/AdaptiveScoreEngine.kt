package com.example.data

import android.content.Context

enum class ScoreTrend { IMPROVING, STABLE, DECLINING }

/**
 * Canonical Drive-Score formula, shared by the live trip-end calculation
 * (RecorderViewModel.stopRecordingSession) and the Drive Score screen
 * (TripScoreScreen.computeDriveScore) so the two can never drift apart.
 * Each hard brake costs 8 points, each minor incident 3, clamped to 0..100.
 */
fun rawTripScore(hardBrakes: Int, minorIncidents: Int): Int =
    (100 - hardBrakes * 8 - minorIncidents * 3).coerceIn(0, 100)


/**
 * Q-learning-inspired adaptive driver score engine.
 * Tracks trip score history using an Exponential Moving Average (α=0.3) so recent trips
 * are weighted more heavily than older ones. Also computes a real-time risk level from
 * speed, G-force, and session hard-brake events.
 */
class AdaptiveScoreEngine(context: Context) {

    private val prefs = context.getSharedPreferences("adaptive_score_v1", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EMA = "ema_score"
        private const val KEY_SCORES = "trip_scores"
        private const val ALPHA = 0.3f
        private const val MAX_HISTORY = 20
    }

    fun getAdaptiveScore(): Float = prefs.getFloat(KEY_EMA, 100f)

    fun updateWithTripScore(rawScore: Int) {
        val updated = ALPHA * rawScore + (1f - ALPHA) * getAdaptiveScore()
        val history = getRecentScores(MAX_HISTORY).toMutableList().also { it.add(rawScore) }
        prefs.edit()
            .putFloat(KEY_EMA, updated)
            .putString(KEY_SCORES, history.takeLast(MAX_HISTORY).joinToString(","))
            .apply()
    }

    fun getTrend(): ScoreTrend {
        val recent = getRecentScores(6)
        if (recent.size < 4) return ScoreTrend.STABLE
        val older = recent.take(recent.size / 2).average()
        val newer = recent.drop(recent.size / 2).average()
        return when {
            newer - older > 5 -> ScoreTrend.IMPROVING
            older - newer > 5 -> ScoreTrend.DECLINING
            else -> ScoreTrend.STABLE
        }
    }

    fun getRecentScores(n: Int): List<Int> =
        (prefs.getString(KEY_SCORES, "") ?: "")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .takeLast(n)

    fun computeRiskLevel(
        speedMph: Double,
        speedLimitMph: Int,
        gForce: Double,
        sessionHardBrakes: Int,
        sessionMinutes: Double
    ): Float {
        val limit = speedLimitMph.takeIf { it > 0 } ?: 35
        val speedRisk = (speedMph / limit).toFloat().coerceIn(0f, 2f) / 2f
        val gRisk = (gForce / 2.0).toFloat().coerceIn(0f, 1f)
        val brakeRisk = if (sessionMinutes > 0.1)
            (sessionHardBrakes / sessionMinutes).toFloat().coerceIn(0f, 1f)
        else 0f
        return (0.4f * speedRisk + 0.4f * gRisk + 0.2f * brakeRisk).coerceIn(0f, 1f)
    }

    /**
     * EMA-smooths the live risk level so the dashboard halo glides between states
     * instead of flickering on momentary sensor spikes. alpha = how much the newest
     * raw reading moves the displayed value (0.25 = gentle glide).
     */
    fun smoothRisk(previous: Float, raw: Float, alpha: Float = 0.25f): Float =
        (alpha * raw + (1f - alpha) * previous).coerceIn(0f, 1f)
}
