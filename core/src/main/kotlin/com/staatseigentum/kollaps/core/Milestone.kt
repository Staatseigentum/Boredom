package com.staatseigentum.kollaps.core

/**
 * The bonus a collector earns simply for existing in numbers.
 *
 * Buying the four hundredth dust net used to feel like nothing: the price had climbed 15 % four
 * hundred times over and the output was still one flat rate per unit. A milestone every twenty-five
 * gives the counter somewhere to be going, and it is the one bonus in the game that costs nothing
 * beyond what the player was buying anyway.
 */
object Milestones {

    /** Copies between two milestones. */
    const val STEP = 25

    /** What each milestone multiplies that collector by. */
    const val FACTOR = 1.15

    /** How many milestones [owned] copies have passed. */
    fun reached(owned: Int): Int = if (owned <= 0) 0 else owned / STEP

    /** The count at which the next milestone lands, or `null` once nothing is left to count to. */
    fun nextAt(owned: Int): Int? = if (owned < 0) STEP else (reached(owned) + 1) * STEP

    /**
     * What the milestones passed so far are worth together.
     *
     * [each] is what one milestone multiplies by; it is a parameter because an Äonen upgrade can
     * raise it, and the shop has to be able to show the raised number rather than the base one.
     */
    fun factor(owned: Int, each: Double = FACTOR): Double {
        var result = 1.0
        repeat(reached(owned)) { result *= each }
        return result
    }
}
