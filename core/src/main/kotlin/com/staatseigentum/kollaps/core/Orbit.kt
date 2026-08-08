package com.staatseigentum.kollaps.core

import kotlin.math.pow

/**
 * One slot around the body, and everything that depends on how far out it is.
 *
 * The whole system is a trade along this one axis. Close in, a body is fed fast by whatever the
 * central mass sweeps up and is worth more, because it is deeper in the well — but the tides pull
 * it apart just as fast, so it settles at a mass and stays there. Far out it is fed slowly and
 * counts for less, and nothing takes it away again, so it keeps growing for as long as the run
 * lasts.
 *
 * There is no correct answer, which is the point: a short session wants the inner orbits and a
 * run left going overnight wants the outer ones.
 */
data class Orbit(val index: Int) {

    /** Share of the screen radius this orbit is drawn at, in `0f..1f`. */
    val radius: Float get() = 0.42f + index * 0.075f

    /** Mass fed in per second, as a share of the body's own production. Inner orbits sweep more. */
    val feed: Double get() = FEED_SHARE / (1.0 + index * 0.35)

    /** Share of its own mass a body here loses every second. Zero on the outermost orbits. */
    val tide: Double get() = (TIDE_BASE - index * TIDE_STEP).coerceAtLeast(0.0)

    /** How much a body here counts towards production. */
    val yield: Double get() = 1.0 + (Orbits.MAX - index) * 0.15

    /** Mass at which feeding and the tides cancel out, given the body's production. */
    fun equilibrium(perSecond: Double): Double =
        if (tide <= 0.0) Double.POSITIVE_INFINITY else perSecond * feed / tide

    /** What it costs to open this slot. */
    val cost: Double get() = Orbits.FIRST_COST * Orbits.COST_GROWTH.pow(index)

    /** What it costs to put a body on it, once the slot is open. */
    val seedCost: Double get() = cost * Orbits.SEED_SHARE

    companion object {
        private const val FEED_SHARE = 0.055
        private const val TIDE_BASE = 0.0075
        private const val TIDE_STEP = 0.0013
    }
}

object Orbits {

    /** The body at which the first slot can be opened. */
    const val UNLOCK_TIER = "Saturn"

    /** Eight slots. Enough for every resonance worth having, few enough to read at a glance. */
    const val MAX = 8

    const val FIRST_COST = 1.2e10
    const val COST_GROWTH = 14.0

    /** A body costs this share of its slot's price on top of the slot. */
    const val SEED_SHARE = 0.4

    /**
     * What one rung of a satellite's tier is worth towards production.
     *
     * Counted in rungs rather than in the tier's own production multiplier, which was the first
     * attempt and was a runaway by construction: that multiplier spans one to twelve hundred
     * across the ladder, so eight satellites near the top came to a hundred and sixty times
     * everything and the last seven bodies fell inside a minute. Rungs span nought to
     * twenty-four, so the system grows with the run instead of overtaking it.
     */
    const val YIELD_PER_TIER = 0.007

    /** What a resonant pair is worth, to both of them. */
    const val RESONANCE_BONUS = 0.25

    /** Mass kept when two bodies merge, over and above the sum. */
    const val MERGE_BONUS = 1.12

    val all: List<Orbit> = (0 until MAX).map { Orbit(it) }

    fun at(index: Int): Orbit? = all.getOrNull(index)

    /**
     * The orbit-count ratios that lock two bodies together.
     *
     * Real resonances, and the reason Jupiter's inner moons keep their orbits: when two periods
     * divide evenly, the same nudge arrives at the same place every time round instead of
     * averaging out. Written as slot numbers rather than periods because the slots are what the
     * player moves things between.
     */
    private val RATIOS = setOf(1 to 2, 2 to 3, 1 to 3, 3 to 4, 2 to 5)

    /** True once the player can open a slot at all. Owning one keeps it open. */
    fun isUnlocked(state: GameState): Boolean =
        state.bestTier >= Tiers.indexOf(UNLOCK_TIER) || state.orbits > 0

    /** Slots opened so far, as objects. */
    fun opened(state: GameState): List<Orbit> = all.take(state.orbits.coerceIn(0, MAX))

    /** The next slot to open, or `null` when they are all open. */
    fun next(state: GameState): Orbit? = at(state.orbits)

    fun massOn(state: GameState, orbit: Orbit): Double = state.satellites[orbit.index] ?: 0.0

    fun isOccupied(state: GameState, orbit: Orbit): Boolean = massOn(state, orbit) > 0.0

    /**
     * The tier a body on this orbit counts as.
     *
     * Capped one rung below the central body, however much mass it has gathered. A moon larger
     * than its planet is not a moon, and without the cap the fastest route up the ladder would be
     * to feed a satellite instead of the thing the player is actually playing.
     */
    fun tierOn(state: GameState, orbit: Orbit): CelestialTier {
        val own = Tiers.forMass(massOn(state, orbit))
        val ceiling = (GameEngine.tierOf(state).index - 1).coerceAtLeast(0)
        return if (own.index <= ceiling) own else Tiers.byIndex(ceiling)
    }

    /** Whether two occupied slots are locked together. */
    fun isResonant(a: Orbit, b: Orbit): Boolean {
        val low = minOf(a.index, b.index) + 1
        val high = maxOf(a.index, b.index) + 1
        if (low == high) return false
        val divisor = gcd(low, high)
        return (low / divisor to high / divisor) in RATIOS
    }

    /** Every slot this one is locked to, occupied or not. */
    fun resonantWith(state: GameState, orbit: Orbit): List<Orbit> =
        opened(state).filter { it.index != orbit.index && isResonant(orbit, it) && isOccupied(state, it) }

    /**
     * What one body adds to production, before the resonance bonus.
     *
     * Additive rather than multiplicative, for the same reason the investments are: eight bodies
     * each multiplying would turn the system into the only thing that matters by the third one.
     */
    fun yieldOf(state: GameState, orbit: Orbit): Double {
        if (!isOccupied(state, orbit)) return 0.0
        return YIELD_PER_TIER * (tierOn(state, orbit).index + 1) * orbit.yield
    }

    /** What one body adds, including whatever it is locked to. */
    fun totalYieldOf(state: GameState, orbit: Orbit): Double {
        val base = yieldOf(state, orbit)
        if (base <= 0.0) return 0.0
        val partners = resonantWith(state, orbit).size
        return base * (1.0 + RESONANCE_BONUS * partners)
    }

    /** The multiplier the whole system is worth. One when nothing is in orbit. */
    fun multiplier(state: GameState): Double =
        1.0 + opened(state).sumOf { totalYieldOf(state, it) }

    /** How many bodies are up there. */
    fun occupiedCount(state: GameState): Int = opened(state).count { isOccupied(state, it) }

    /**
     * Feeds and erodes every body for [seconds].
     *
     * Both are proportional to what the central body produces, so the system scales with the run
     * instead of mattering enormously at the moment it unlocks and never again.
     */
    fun advance(state: GameState, seconds: Double, perSecond: Double): GameState {
        if (seconds <= 0.0) return state
        if (state.satellites.isEmpty()) return state

        var changed = false
        val next = HashMap<Int, Double>(state.satellites.size)
        for ((index, mass) in state.satellites) {
            val orbit = at(index)
            if (orbit == null || mass <= 0.0) {
                changed = true
                continue
            }
            val fed = perSecond * orbit.feed * seconds
            val lost = mass * orbit.tide * seconds
            val settled = (mass + fed - lost).coerceAtLeast(0.0)
            if (settled != mass) changed = true
            next[index] = settled
        }
        return if (changed) state.copy(satellites = next) else state
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
