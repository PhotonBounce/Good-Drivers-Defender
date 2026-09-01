package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TrialManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Coverage for the 7-day free VIP trial window math and one-time first-launch stamping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrialManagerTest {

    private lateinit var trial: TrialManager
    private val day = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("trial_v1", Context.MODE_PRIVATE).edit().clear().commit()
        trial = TrialManager(ctx)
    }

    @Test
    fun `ensureStarted stamps once and is idempotent`() {
        val t0 = 1_000_000L
        val first = trial.ensureStarted(t0)
        val second = trial.ensureStarted(t0 + 5 * day) // a later call must not move the stamp
        assertEquals(t0, first)
        assertEquals(t0, second)
        assertEquals(t0, trial.firstLaunchMs())
    }

    @Test
    fun `not in trial before first launch is stamped`() {
        assertFalse(trial.isInTrial(1_000_000L))
        assertEquals(7, trial.daysRemaining(1_000_000L)) // full window shown pre-stamp
    }

    @Test
    fun `trial is active within the window and expires after 7 days`() {
        val t0 = 1_000_000L
        trial.ensureStarted(t0)
        assertTrue(trial.isInTrial(t0))
        assertTrue(trial.isInTrial(t0 + 6 * day))
        assertTrue(trial.isInTrial(t0 + 7 * day - 1))
        assertFalse(trial.isInTrial(t0 + 7 * day))
        assertFalse(trial.isInTrial(t0 + 30 * day))
    }

    @Test
    fun `clock rollback cannot revive an expired trial`() {
        val t0 = 1_000_000L
        trial.ensureStarted(t0)
        assertFalse(trial.isInTrial(t0 + 8 * day))  // expired; records the high-water mark
        assertFalse(trial.isInTrial(t0 + 1 * day))  // clock rolled back — still expired
        assertEquals(0, trial.daysRemaining(t0 + 1 * day))
    }

    @Test
    fun `days remaining counts down and floors at zero`() {
        val t0 = 1_000_000L
        trial.ensureStarted(t0)
        assertEquals(7, trial.daysRemaining(t0))
        assertEquals(1, trial.daysRemaining(t0 + 6 * day))
        assertEquals(1, trial.daysRemaining(t0 + 7 * day - 1))
        assertEquals(0, trial.daysRemaining(t0 + 7 * day))
        assertEquals(0, trial.daysRemaining(t0 + 100 * day))
    }
}
