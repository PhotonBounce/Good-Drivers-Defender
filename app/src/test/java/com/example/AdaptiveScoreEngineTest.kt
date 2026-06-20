package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdaptiveScoreEngine
import com.example.data.ScoreTrend
import com.example.data.rawTripScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit coverage for the adaptive Q-scoring engine: EMA smoothing, trend detection,
 * the bounded real-time risk blend, and the shared raw-score formula.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdaptiveScoreEngineTest {

    private lateinit var engine: AdaptiveScoreEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Clean prefs so each test is deterministic regardless of ordering.
        context.getSharedPreferences("adaptive_score_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        engine = AdaptiveScoreEngine(context)
    }

    @Test
    fun `fresh engine starts at a perfect score with no history`() {
        assertEquals(100f, engine.getAdaptiveScore(), 0.001f)
        assertTrue(engine.getRecentScores(5).isEmpty())
        assertEquals(ScoreTrend.STABLE, engine.getTrend())
    }

    @Test
    fun `EMA weights the latest trip with alpha 0_3`() {
        engine.updateWithTripScore(50)
        // 0.3*50 + 0.7*100 = 85
        assertEquals(85f, engine.getAdaptiveScore(), 0.001f)
        engine.updateWithTripScore(50)
        // 0.3*50 + 0.7*85 = 74.5
        assertEquals(74.5f, engine.getAdaptiveScore(), 0.001f)
    }

    @Test
    fun `recent scores preserve order and cap at the history limit`() {
        repeat(25) { engine.updateWithTripScore(it) } // 0..24
        val recent = engine.getRecentScores(100)
        assertEquals(20, recent.size)  // capped at MAX_HISTORY
        assertEquals(24, recent.last())
        assertEquals(5, recent.first()) // 25 - 20 = 5
    }

    @Test
    fun `trend stays stable with too few trips`() {
        engine.updateWithTripScore(40)
        engine.updateWithTripScore(95)
        assertEquals(ScoreTrend.STABLE, engine.getTrend())
    }

    @Test
    fun `trend detects improvement`() {
        listOf(60, 60, 60, 90, 90, 90).forEach { engine.updateWithTripScore(it) }
        assertEquals(ScoreTrend.IMPROVING, engine.getTrend())
    }

    @Test
    fun `trend detects decline`() {
        listOf(90, 90, 90, 60, 60, 60).forEach { engine.updateWithTripScore(it) }
        assertEquals(ScoreTrend.DECLINING, engine.getTrend())
    }

    @Test
    fun `trend stays stable for flat scores`() {
        listOf(80, 80, 80, 80, 80, 80).forEach { engine.updateWithTripScore(it) }
        assertEquals(ScoreTrend.STABLE, engine.getTrend())
    }

    @Test
    fun `risk is zero when stationary and calm`() {
        assertEquals(0f, engine.computeRiskLevel(0.0, 35, 0.0, 0, 0.0), 0.001f)
    }

    @Test
    fun `risk rises with speed and g-force`() {
        // speed double the limit -> speedRisk 1.0 (*0.4); g 2.0 -> gRisk 1.0 (*0.4)
        assertEquals(0.8f, engine.computeRiskLevel(70.0, 35, 2.0, 0, 0.0), 0.001f)
    }

    @Test
    fun `risk is clamped to one`() {
        assertEquals(1f, engine.computeRiskLevel(200.0, 35, 9.0, 50, 1.0), 0.001f)
    }

    @Test
    fun `risk falls back to default limit when limit is zero`() {
        // limit 0 -> 35; speed 35 -> ratio 1 -> speedRisk 0.5 -> *0.4 = 0.2
        assertEquals(0.2f, engine.computeRiskLevel(35.0, 0, 0.0, 0, 0.0), 0.001f)
    }

    @Test
    fun `raw trip score penalizes hard brakes and minors and clamps`() {
        assertEquals(100, rawTripScore(0, 0))
        assertEquals(89, rawTripScore(1, 1)) // 100 - 8 - 3
        assertEquals(0, rawTripScore(20, 0)) // clamped at 0
    }
}
