package com.example.data

/**
 * Picks the single most impactful, actionable coaching tip from a trip's stats,
 * so the Drive Score screen gives the driver a "what to improve next" nudge
 * instead of just numbers. Pure + deterministic so it can be unit-tested.
 *
 * Priority order: a clean record first, then whichever factor is hurting the
 * score most (hard braking weighs most heavily), then speed, then g-force.
 */
fun coachingTip(hardBrakes: Int, minorIncidents: Int, topSpeedMph: Int, maxG: Double): String {
    if (hardBrakes == 0 && minorIncidents == 0) {
        return "Flawless record — keep your smooth, steady driving going."
    }
    val brakeWeight = hardBrakes * 8
    val minorWeight = minorIncidents * 3
    return when {
        hardBrakes > 0 && brakeWeight >= minorWeight ->
            "Hard braking is your biggest factor — start slowing earlier to smooth out stops."
        topSpeedMph >= 75 ->
            "High top speeds add risk — easing off raises your score the fastest."
        maxG >= 2.0 ->
            "Sharp g-forces detected — smoother cornering and acceleration will help."
        else ->
            "Mostly minor events — a little extra following distance keeps them down."
    }
}
