package com.example.data

/**
 * Pure gamification math for the Achievements screen — driver XP, level, and
 * progress to the next level. Kept out of the Composable so it can be unit-tested
 * and reused without drift.
 */

const val XP_PER_LEVEL = 500

/** XP from logged activity: each incident 100, each auto-capture +50, each unlocked badge +75. */
fun driverXp(totalIncidents: Int, autoCaptured: Int, unlockedBadges: Int): Int =
    totalIncidents * 100 + autoCaptured * 50 + unlockedBadges * 75

/** 1-based driver level: every [XP_PER_LEVEL] XP is one level. */
fun driverLevel(xp: Int): Int = xp / XP_PER_LEVEL + 1

/** Fraction (0f..1f) of progress through the current level. */
fun levelProgress(xp: Int): Float = (xp % XP_PER_LEVEL) / XP_PER_LEVEL.toFloat()
