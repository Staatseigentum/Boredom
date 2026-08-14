package com.staatseigentum.kollaps.core

import kotlinx.serialization.Serializable
import kotlin.math.ln
import kotlin.math.pow

/**
 * A universe that has been through its big bang and did not stop existing.
 *
 * The big bang used to be a delete key with a good story attached: press it, lose everything the
 * collapses built, start the same four hours again with larger numbers. That is a fine second
 * prestige layer exactly once, and after the third one the deepest button in the game means
 * "faster" and nothing else.
 *
 * So it parks instead of wiping. The finished universe keeps its shape — how deep it got, how
 * hard it was running, which way it leaned — moves into a galaxy of its own, and goes on working
 * while the player builds the next one. Two universes earning at once, then three, then eight.
 *
 * Deliberately a record and not a running simulation. Ticking eight full games in the background
 * would be eight times the work every frame for a number nobody can watch, and would make a save
 * file that grows without limit. What is kept is what a universe *was* at its end, and everything
 * it contributes is worked out from that — which is stable, cheap, and reads the same after an
 * hour away as after a second.
 */
@Serializable
data class ParkedUniverse(
    /** Which galaxy it sits in, `0 until Multiverse.SLOTS`. */
    val slot: Int,
    /** The path it ran under, or `null` for the unaligned universes from before paths existed. */
    val pathId: String? = null,
    /** The deepest rung it ever reached. The main measure of what it was worth. */
    val bestTier: Int = 0,
    /** Collapses it had behind it when the big bang came. */
    val collapses: Int = 0,
    /** Singularities it had accumulated. Kept for the chronicle, and a small part of its yield. */
    val singularities: Double = 0.0,
    /** Wall-clock milliseconds at which it was parked, for the "läuft seit" line. */
    val parkedAt: Long = 0L,
) {
    /** The path's lean, or `null` where there was none. */
    val path: Path? get() = Path.byId(pathId)

    /**
     * What this galaxy is called.
     *
     * Derived rather than stored: a name in the save is a name that has to be migrated, and there
     * is nothing here a player would want to rename. The slot fixes the constellation and the
     * depth fixes the number, so a galaxy that got further reads as a bigger one.
     */
    val name: String
        get() = "${CONSTELLATIONS[slot % CONSTELLATIONS.size]}-${(bestTier + 1) * 100 + collapses}"

    private companion object {
        val CONSTELLATIONS = listOf(
            "Andromeda", "Bootes", "Carina", "Draco",
            "Eridanus", "Fornax", "Grus", "Hydra",
        )
    }
}

/**
 * Every universe the player has finished, and what they are worth together.
 *
 * The rules here are the same shape as [Orbits] on purpose: each galaxy contributes *additively*
 * to one multiplier rather than multiplying on its own. Eight things that each multiply is a
 * runaway by the third one — the orbit system learned that the expensive way, and there is no
 * reason to learn it twice.
 */
object Multiverse {

    /** Galaxies, which is also the most universes that can be parked at once. */
    const val SLOTS = 8

    /**
     * What one rung of a parked universe's depth is worth to the active one.
     *
     * Counted in rungs rather than in the production it was making at the end, which was the
     * obvious first attempt and is unplayable: a universe parked at the black hole was running at
     * something like 1e14 kg/s, and a trickle of any visible share of that would hand the next
     * universe its first three hours in the opening second. Rungs are a small, bounded number, so
     * a parked universe is a lasting advantage rather than a skip button.
     */
    const val YIELD_PER_TIER = 0.05

    /** And what each collapse it had behind it adds, on the same scale. */
    const val YIELD_PER_COLLAPSE = 0.012

    /**
     * Äonen a galaxy generates per hour, per point of [yieldOf].
     *
     * This is the half of the feature the player actually asked for: the old universe does not
     * merely make the new one faster, it goes on earning the deep currency by itself, in real
     * time, whether or not anybody is watching. It pays in Äonen rather than mass because mass
     * belongs to a run and would either be swamped by the active universe or swamp it, while
     * Äonen are permanent, spent on a shelf of their own, and worth waiting for.
     */
    const val AEONS_PER_HOUR = 0.020

    /**
     * How much less each further galaxy is worth than the one before it.
     *
     * Without this the eighth parked universe is worth as much as the first, and since each one
     * is deeper than the last the total grows faster than the game it is multiplying. With it the
     * sum converges: eight galaxies are worth about four of the best one, so filling the sky stays
     * a real gain and never becomes the only thing that matters.
     */
    const val SLOT_FALLOFF = 0.82

    fun parked(state: GameState): List<ParkedUniverse> =
        state.universes.filter { it.slot in 0 until SLOTS }.sortedBy { it.slot }

    fun count(state: GameState): Int = parked(state).size

    /** Whether there is anywhere left to put one. */
    fun hasRoom(state: GameState): Boolean = count(state) < SLOTS

    /** The lowest free galaxy, or `null` when the sky is full. */
    fun nextSlot(state: GameState): Int? {
        val taken = state.universes.map { it.slot }.toSet()
        return (0 until SLOTS).firstOrNull { it !in taken }
    }

    /** True once there is anything to show. */
    fun isUnlocked(state: GameState): Boolean = state.bigBangs > 0 || state.universes.isNotEmpty()

    /**
     * What one parked universe is worth, before the falloff of where it sits.
     *
     * Singularities enter through a logarithm rather than linearly: a universe that was collapsed
     * forty times had a hundred times the singularities of one collapsed ten times, and paying
     * that out straight would make every galaxy but the deepest a rounding error.
     */
    fun yieldOf(universe: ParkedUniverse): Double {
        val depth = YIELD_PER_TIER * (universe.bestTier + 1)
        val history = YIELD_PER_COLLAPSE * universe.collapses
        val hoard = 0.02 * ln(1.0 + universe.singularities.coerceAtLeast(0.0))
        return depth + history + hoard
    }

    /** The same, weighted by how many better galaxies are already standing. */
    fun weightedYieldOf(state: GameState, universe: ParkedUniverse): Double {
        val ranked = parked(state).sortedByDescending { yieldOf(it) }
        val rank = ranked.indexOfFirst { it.slot == universe.slot }.coerceAtLeast(0)
        return yieldOf(universe) * SLOT_FALLOFF.pow(rank)
    }

    /** The multiplier the whole sky is worth to the active universe. One when it is empty. */
    fun multiplier(state: GameState): Double =
        1.0 + parked(state).sumOf { weightedYieldOf(state, it) }

    /**
     * Äonen every parked universe together earns per second.
     *
     * Zero before anything is parked, which is what keeps this from being a currency the player
     * has to understand before they have seen a single big bang.
     */
    fun aeonsPerSecond(state: GameState): Double {
        if (state.universes.isEmpty()) return 0.0
        val perHour = parked(state).sumOf { weightedYieldOf(state, it) } * AEONS_PER_HOUR
        return perHour / 3_600.0
    }

    /**
     * The leans of every parked universe, to be folded in alongside the active one's.
     *
     * A parked universe keeps contributing what it *was*, at a fraction of the strength — which is
     * what makes choosing a different path at each big bang a portfolio rather than a mood. Eight
     * universes all down the same path is a legitimate, and legitimately narrow, way to play.
     */
    fun effects(state: GameState): List<PrestigeEffect> =
        parked(state).flatMap { universe ->
            val share = leanShare(weightedYieldOf(state, universe))
            universe.path?.effects.orEmpty().mapNotNull { softened(it, share) }
        }

    /**
     * How much of its path a galaxy of a given strength hands on, in `0f..1f`.
     *
     * Saturating rather than clamped, and that distinction is the whole of it. Clamping was the
     * first attempt: a deep universe scores well above one, so `coerceAtMost(1.0)` handed over the
     * path at *full* strength — a single parked galaxy was worth exactly as much as being aligned
     * to it, which made the alignment of the universe the player is actually in worth nothing.
     * This approaches one and never arrives, so every galaxy is a real share of a path and no
     * number of them is ever the same as living in one.
     */
    private fun leanShare(strength: Double): Double {
        val positive = strength.coerceAtLeast(0.0)
        return positive / (positive + 1.0)
    }

    /**
     * A path effect at [share] of its strength.
     *
     * Multipliers are pulled towards one and flat additions are scaled. Three kinds are dropped
     * instead of softened, each for its own reason: [PrestigeEffect.AutoBuy] is a switch, and
     * there is no such thing as three quarters of a button — a parked universe handing it over for
     * ever would quietly delete a prestige upgrade somebody paid for. [PrestigeEffect.StartingMass]
     * and [PrestigeEffect.StartingCollectors] are read once when a run begins and would turn the
     * sky into a head start rather than a rate. And [PrestigeEffect.SingularityBonus] *replaces*
     * what a singularity is worth rather than adding to it, so a scaled copy of it is not a weaker
     * version of the same thing, it is a different and probably smaller number winning a
     * comparison it should not have been in.
     */
    private fun softened(effect: PrestigeEffect, share: Double): PrestigeEffect? = when (effect) {
        is PrestigeEffect.GlobalMultiplier -> PrestigeEffect.GlobalMultiplier(taper(effect.factor, share))
        is PrestigeEffect.TapMultiplier -> PrestigeEffect.TapMultiplier(taper(effect.factor, share))
        is PrestigeEffect.FusionRate -> PrestigeEffect.FusionRate(taper(effect.factor, share))
        is PrestigeEffect.ResearchSpeed -> PrestigeEffect.ResearchSpeed(taper(effect.factor, share))
        is PrestigeEffect.CometFrequency -> PrestigeEffect.CometFrequency(taper(effect.factor, share))
        is PrestigeEffect.SingularityGain -> PrestigeEffect.SingularityGain(taper(effect.factor, share))
        is PrestigeEffect.AutoTap -> PrestigeEffect.AutoTap(effect.perSecond * share)
        is PrestigeEffect.MilestoneBonus -> PrestigeEffect.MilestoneBonus(effect.extra * share)
        is PrestigeEffect.OfflineEfficiency -> effect
        is PrestigeEffect.OfflineCapHours -> effect
        is PrestigeEffect.AutoBuy,
        is PrestigeEffect.StartingMass,
        is PrestigeEffect.StartingCollectors,
        is PrestigeEffect.SingularityBonus,
        -> null
    }

    /** A multiplier at [share] strength: 1 at nothing, the full factor at everything. */
    private fun taper(factor: Double, share: Double): Double = 1.0 + (factor - 1.0) * share

    /**
     * Turns the universe about to end into the record that outlives it.
     *
     * Reads the state as it stands the moment before the big bang, so `bestTier` here is the
     * deepest rung this universe ever stood on rather than the one it happens to be on.
     */
    fun park(state: GameState, slot: Int, nowMillis: Long): ParkedUniverse = ParkedUniverse(
        slot = slot,
        pathId = state.path,
        bestTier = maxOf(state.bestTier, GameEngine.tierOf(state).index),
        collapses = state.collapses,
        singularities = state.singularities,
        parkedAt = nowMillis,
    )

    /**
     * Credits [seconds] of Äonen from the sky.
     *
     * The fraction is carried in the save rather than rounded away: at the opening rate a galaxy
     * earns an Äon every couple of days, and a version of this that dropped everything below one
     * would pay out nothing, for ever, while looking like it worked.
     */
    fun advance(state: GameState, seconds: Double): GameState {
        if (seconds <= 0.0) return state
        val earned = aeonsPerSecond(state) * seconds
        if (earned <= 0.0) return state

        val pot = state.aeonFraction + earned
        val whole = kotlin.math.floor(pot)
        return if (whole >= 1.0) {
            state.copy(aeons = state.aeons + whole, aeonFraction = pot - whole)
        } else {
            state.copy(aeonFraction = pot)
        }
    }
}
