package com.example

import com.example.data.coachingTip
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM coverage for the Drive Score coaching tip selector and its priority order.
 */
class CoachingTest {

    @Test
    fun `clean record is praised`() {
        assertTrue(coachingTip(0, 0, 40, 1.0).contains("Flawless"))
    }

    @Test
    fun `hard braking takes priority when it dominates the penalty`() {
        // brakeWeight 16 >= minorWeight 0
        assertTrue(coachingTip(2, 0, 50, 1.0).contains("Hard braking"))
        // brakeWeight 8 >= minorWeight 6
        assertTrue(coachingTip(1, 2, 50, 1.0).contains("Hard braking"))
    }

    @Test
    fun `high speed surfaces when braking is not the dominant factor`() {
        assertTrue(coachingTip(0, 2, 80, 1.0).contains("top speed"))
    }

    @Test
    fun `sharp g-force surfaces when speed is moderate`() {
        assertTrue(coachingTip(0, 2, 50, 2.5).contains("g-force"))
    }

    @Test
    fun `minor events are the fallback advice`() {
        assertTrue(coachingTip(0, 1, 50, 1.0).contains("minor events"))
        // braking present but minors dominate the weight, speed/g low -> minor advice
        assertTrue(coachingTip(1, 5, 50, 1.0).contains("minor events"))
    }
}
