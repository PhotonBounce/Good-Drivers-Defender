package com.example

import com.example.data.driverLevel
import com.example.data.driverXp
import com.example.data.levelProgress
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM coverage for the Achievements gamification math (no Android deps).
 */
class GamificationTest {

    @Test
    fun `xp sums incidents auto-captures and badges with correct weights`() {
        assertEquals(0, driverXp(0, 0, 0))
        assertEquals(225, driverXp(1, 1, 1)) // 100 + 50 + 75
        assertEquals(1325, driverXp(10, 5, 1)) // 1000 + 250 + 75
    }

    @Test
    fun `level is one-based and increments every 500 xp`() {
        assertEquals(1, driverLevel(0))
        assertEquals(1, driverLevel(499))
        assertEquals(2, driverLevel(500))
        assertEquals(2, driverLevel(999))
        assertEquals(3, driverLevel(1000))
    }

    @Test
    fun `level progress is the fraction through the current level`() {
        assertEquals(0f, levelProgress(0), 0.001f)
        assertEquals(0.5f, levelProgress(250), 0.001f)
        assertEquals(0f, levelProgress(500), 0.001f)
        assertEquals(0.5f, levelProgress(750), 0.001f)
    }
}
