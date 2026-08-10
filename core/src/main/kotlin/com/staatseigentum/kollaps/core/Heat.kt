package com.staatseigentum.kollaps.core

import kotlin.math.exp

/**
 * How hot the body gets from being tapped, and how fast it cools again.
 *
 * The problem this solves is specific. The game asks the player to tap a celestial body, and for
 * the first ten rungs that is most of the income — but a tap is a flat amount plus a share of
 * production, while production itself multiplies with every collector, upgrade and milestone. By
 * the middle of a run the central verb of the game is worth less than a rounding error, and the
 * only thing left to do is watch a number.
 *
 * So heat does not make a tap pay better. It makes *everything* pay better for as long as somebody
 * is tapping, which is the only formulation that still means something at the top of the ladder.
 *
 * Three numbers decide whether this is a good idea or a bad one:
 *
 * - [BONUS] is how much it is worth at full heat. Thirty per cent is on purpose. A heat that
 *   doubled output would make putting the phone down a mistake, and an idle game that punishes
 *   you for idling has misunderstood itself. This is worth staying a minute for and not worth
 *   feeling guilty about.
 * - [PER_TAP] puts full heat about twenty-five taps away — six or seven seconds of real tapping,
 *   so it is reachable in one sitting rather than being a stamina test.
 * - [HALF_LIFE_SECONDS] takes it away again in well under a minute. It has to be visibly gone by
 *   the time somebody comes back to the screen, or it stops being about being present.
 *
 * Exponential decay rather than a straight line, because the same fraction leaving every second is
 * what "cooling" actually is — and because it means a player who taps once every few seconds sits
 * at a stable middle rather than sawing between full and nothing.
 */
object Heat {

    /** What full heat multiplies production by, over and above one. */
    const val BONUS = 0.30

    /** How much one tap adds. */
    const val PER_TAP = 0.04

    /** How long it takes to lose half of whatever is there. */
    const val HALF_LIFE_SECONDS = 6.0

    /** What production is multiplied by at this heat. */
    fun factor(heat: Double): Double = 1.0 + BONUS * heat.coerceIn(0.0, 1.0)

    /** The heat after one tap. */
    fun afterTap(heat: Double): Double = (heat + PER_TAP).coerceIn(0.0, 1.0)

    /**
     * The heat after [seconds] without one.
     *
     * Framed as a half-life so the constant means something a person can check: after six seconds
     * of not tapping, half of it is gone, whatever it was.
     */
    fun cooled(heat: Double, seconds: Double): Double {
        if (heat <= 0.0 || seconds <= 0.0) return heat.coerceIn(0.0, 1.0)
        val left = heat * exp(-LN_TWO * seconds / HALF_LIFE_SECONDS)
        // Below a hundredth it is worth nothing and would otherwise never quite reach zero,
        // leaving the bar on screen forever showing a bonus of nought per cent.
        return if (left < 0.01) 0.0 else left
    }

    private const val LN_TWO = 0.6931471805599453
}
