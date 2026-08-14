package com.staatseigentum.kollaps.core

import kotlinx.serialization.Serializable
import kotlin.math.ln
import kotlin.math.pow

/**
 * What a parked universe is doing with itself.
 *
 * The sky arrived as eight numbers that went up, which is a strange thing to hand somebody who has
 * spent a hundred and sixty collapses earning them. Eight things you own and no decision about any
 * of them is a scoreboard wearing the clothes of a system.
 *
 * So each galaxy is put to work at one of four things. None of them is best: production is what the
 * run in front of you needs, Äonen are what the shelf needs, comets are what an active session
 * wants and metal is what the forge wants — and which of those you are short of changes across an
 * evening. That is the decision.
 */
enum class GalaxyJob(val id: String, val label: String, val flavor: String) {
    /** Feeds the universe that is actually being played. What every galaxy does by default. */
    FOERDERN(
        id = "job_produce",
        label = "Fördern",
        flavor = "Schickt herüber, was sie herstellt. Das aktive Universum merkt es sofort.",
    ),

    /** Äonen instead of production, and considerably more of them. */
    RECHNEN(
        id = "job_aeons",
        label = "Rechnen",
        flavor = "Stellt die Produktion ein und rechnet stattdessen. Zahlt in Äonen.",
    ),

    /** Throws comets at the active universe. */
    SUCHEN(
        id = "job_comets",
        label = "Suchen",
        flavor = "Durchkämmt sich selbst nach Losem und wirft es herüber.",
    ),

    /** The first way to heavy elements that does not go through a collapse. */
    GRABEN(
        id = "job_metal",
        label = "Graben",
        flavor = "Holt schwere Kerne aus dem eigenen Kern. Langsam, aber ohne Kollaps.",
    ),
    ;

    companion object {
        val DEFAULT = FOERDERN

        fun byId(id: String?): GalaxyJob = entries.firstOrNull { it.id == id } ?: DEFAULT

    }
}

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
    /**
     * The universe itself, paused, so it can be played again. See [UniverseRun].
     *
     * `null` for the galaxies of a save from before this existed — those were only ever a record,
     * and there is nothing to load. They keep contributing exactly as they did; they simply cannot
     * be visited.
     */
    val run: UniverseRun? = null,
    /**
     * A second lean, from a galaxy welded into this one. See [Multiverse.merge].
     *
     * Only ever set by a merge, and only once — a galaxy that carried three paths would be worth
     * more than three galaxies and the sky would collapse into one entry.
     */
    val secondPathId: String? = null,
    /**
     * How far this galaxy has been built out since it was parked. See [Multiverse.develop].
     *
     * A parked universe used to be frozen at whatever it was on the day it ended, which made the
     * sky a shelf of trophies: eight things you look at and cannot touch. This is the one number
     * about a galaxy the player can still move, and it is what makes visiting one worth doing.
     */
    val level: Int = 0,
    /** What it has been put to work at. See [GalaxyJob]; absent means the default. */
    val jobId: String? = null,
    /**
     * Seconds of changeover still to run before the current job takes effect.
     *
     * Counted down rather than derived from a wall-clock stamp, and that was a correction. The
     * first version stored the moment the job was chosen and compared it against `lastSeenAt` —
     * but `lastSeenAt` is written when the game is *saved*, not while it is played, so the
     * changeover would have stood still for as long as anybody was watching it and then completed
     * itself the instant they looked away. This is counted by the same tick that counts everything
     * else, and by the offline credit for the time in between, so it means the same thing whether
     * or not the app is open.
     */
    val rampSeconds: Double = 0.0,
) {
    val job: GalaxyJob get() = GalaxyJob.byId(jobId)

    /** Whether the changeover is still running, and the galaxy is therefore doing nothing. */
    val isRamping: Boolean get() = rampSeconds > 0.0

    /** The path's lean, or `null` where there was none. */
    val path: Path? get() = Path.byId(pathId)

    /** The second lean, where this galaxy is a weld of two. */
    val secondPath: Path? get() = Path.byId(secondPathId)

    /** Both leans, in a fixed order, with nothing repeated. */
    val paths: List<Path> get() = listOfNotNull(path, secondPath).distinct()

    /** Whether this galaxy is a weld of two. */
    val isMerged: Boolean get() = secondPathId != null

    /**
     * Whether the universe in here is the one that was actually played.
     *
     * `false` for a galaxy parked before universes were kept. Those are still enterable — see
     * [UniverseRun.reconstruct] — but what you walk into is rebuilt from the chronicle rather than
     * restored, so it is worth saying so before somebody goes looking for a fleet that was never
     * written down. Once such a galaxy has been visited and left, its universe is real and this
     * turns true for good.
     */
    val isRestored: Boolean get() = run != null

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
        val built = YIELD_PER_LEVEL * universe.level.coerceIn(0, MAX_LEVEL)
        return depth + history + hoard + built
    }

    /**
     * Building a galaxy out, one level at a time, paid in Äonen.
     *
     * The sky earns Äonen and Äonen build the sky, which is a loop that feeds itself — so it is
     * bounded twice over. [MAX_LEVEL] is a finish line each galaxy actually reaches, and the price
     * climbs at [COST_GROWTH] so the last level of one galaxy costs about what the first ten did.
     * The same care the singularity accounts get, and for the same reason: the one posting there
     * that pays in its own currency is also the shortest list on the shelf.
     *
     * The price does *not* scale with how good the galaxy already is, and that is the interesting
     * part. It means a shallow galaxy is the cheap one to improve — so the sky's weakest slot is
     * now a question rather than an obvious weld. Merge it away and free a place, or spend the
     * Äonen and keep the lean it carries.
     */
    const val MAX_LEVEL = 15

    /** What one level adds to a galaxy's yield. */
    const val YIELD_PER_LEVEL = 0.06

    /** Äonen for the first level. */
    const val LEVEL_BASE_COST = 2.0

    /** And what each further one multiplies that by. */
    const val COST_GROWTH = 1.18

    /** What the next level of [universe] costs, or `null` when it is finished. */
    fun costOfNextLevel(universe: ParkedUniverse): Double? {
        if (universe.level >= MAX_LEVEL) return null
        return LEVEL_BASE_COST * COST_GROWTH.pow(universe.level)
    }

    fun canDevelop(state: GameState, slot: Int): Boolean {
        val universe = state.universes.firstOrNull { it.slot == slot } ?: return false
        val cost = costOfNextLevel(universe) ?: return false
        return state.aeons >= cost
    }

    /**
     * Builds one level onto the galaxy in [slot].
     *
     * A galaxy mid-changeover can still be built out: the ramp is about what it is *doing*, and
     * this is about what it *is*. Refusing here would only make the player wait for a timer that
     * has nothing to do with the purchase.
     */
    fun develop(state: GameState, slot: Int): GameState {
        val universe = state.universes.firstOrNull { it.slot == slot } ?: return state
        val cost = costOfNextLevel(universe) ?: return state
        if (state.aeons < cost) return state

        return state.copy(
            aeons = state.aeons - cost,
            universes = state.universes.map {
                if (it.slot == slot) it.copy(level = it.level + 1) else it
            },
        )
    }

    /** The same, weighted by how many better galaxies are already standing. */
    fun weightedYieldOf(state: GameState, universe: ParkedUniverse): Double {
        val ranked = parked(state).sortedByDescending { yieldOf(it) }
        val rank = ranked.indexOfFirst { it.slot == universe.slot }.coerceAtLeast(0)
        // The orbit bonus is added before the falloff, not after, so tending the satellite over a
        // strong galaxy is worth more than tending the one over a weak galaxy. Otherwise the right
        // play would be to feed whichever body is cheapest and ignore where it sits.
        return (yieldOf(universe) + orbitBonusFor(state, universe)) * SLOT_FALLOFF.pow(rank)
    }

    /**
     * What a galaxy is contributing right now, which is nothing at all while it is changing over.
     *
     * The single gate every job goes through. Put in each of the four separately it would be four
     * places to forget it, and a galaxy that kept paying through its own changeover would make the
     * changeover free — which is exactly the cost the ramp exists to charge.
     */
    private fun workingYield(state: GameState, universe: ParkedUniverse, job: GalaxyJob): Double =
        if (universe.job != job || universe.isRamping) {
            0.0
        } else {
            weightedYieldOf(state, universe)
        }

    /**
     * How much harder a galaxy's weight pulls on production than it does on anything else.
     *
     * The sky pays four ways, and three of them are in currencies that stay small: Äonen are
     * counted in ones, comets in how often they come, metal in grams. Production is the one that
     * does not — by the time the sky is full, the active universe is multiplied by the designation
     * ladder, the orbits, the path tree and the singularity shelf, all of which have grown by
     * orders of magnitude since the sky's numbers were chosen. A flat share of yield keeps pace
     * with the small currencies and quietly falls off the bottom of the big one.
     *
     * So Fördern gets leverage the other three jobs do not. It only touches [multiplier], which is
     * the deliberate part: the yields themselves are untouched, so Äonen, comets and metal all pay
     * exactly what they paid before and nothing else in the economy moves.
     */
    const val FOERDERN_LEVERAGE = 2.4

    /**
     * And how much more a sky that has actually been finished is worth than a sky merely filled.
     *
     * Measured against the whole sky rather than the galaxies standing in it: with three galaxies,
     * all of them at [MAX_LEVEL], this sits at three eighths, not at one. Filling the last five
     * slots is the largest single thing left to do in the game, and the multiplier should say so
     * instead of being indifferent to it.
     */
    const val COMPLETION_BONUS = 1.6

    /**
     * How far along the sky as a whole is, from nothing at all to eight finished galaxies.
     *
     * Levels rather than slots, because slots are filled by playing the game anyway and levels are
     * bought — this is the part of the sky that is a decision.
     */
    fun completion(state: GameState): Double {
        val built = parked(state).sumOf { it.level.coerceIn(0, MAX_LEVEL) }
        return 1.0 + COMPLETION_BONUS * built.toDouble() / (SLOTS * MAX_LEVEL)
    }

    /** What one galaxy is adding to production, which is nothing unless it is out there farming. */
    fun productionShareOf(state: GameState, universe: ParkedUniverse): Double =
        workingYield(state, universe, GalaxyJob.FOERDERN) * FOERDERN_LEVERAGE * completion(state)

    /** The multiplier the sky is worth to the active universe. Only the galaxies on [GalaxyJob.FOERDERN]. */
    fun multiplier(state: GameState): Double =
        1.0 + parked(state).sumOf { productionShareOf(state, it) }

    /**
     * Äonen every parked universe together earns per second.
     *
     * Zero before anything is parked, which is what keeps this from being a currency the player
     * has to understand before they have seen a single big bang.
     *
     * A galaxy on [GalaxyJob.RECHNEN] earns [AEON_FOCUS] times what one merely standing there does.
     * That is the trade the whole system is built on: it stops helping the run in front of you
     * entirely, and pays the deep currency for it.
     */
    fun aeonsPerSecond(state: GameState): Double {
        if (state.universes.isEmpty()) return 0.0
        val perHour = parked(state).sumOf { universe ->
            when {
                universe.isRamping -> 0.0
                universe.job == GalaxyJob.RECHNEN -> weightedYieldOf(state, universe) * AEON_FOCUS
                // Everything else still ticks over on the side, because a sky that only paid Äonen
                // when told to would make the first thirty minutes of every galaxy a dead loss.
                else -> weightedYieldOf(state, universe) * AEON_IDLE
            }
        } * AEONS_PER_HOUR
        return perHour / 3_600.0
    }

    /** What a galaxy told to think earns, against one that is doing something else. */
    const val AEON_FOCUS = 4.0

    /** And what the others still bring in on the side. */
    const val AEON_IDLE = 0.35

    /** What the galaxies on [GalaxyJob.SUCHEN] multiply comet frequency by. */
    fun cometFactor(state: GameState): Double =
        1.0 + parked(state).sumOf { workingYield(state, it, GalaxyJob.SUCHEN) } * COMET_PER_YIELD

    /** How hard one point of yield pushes the comets. */
    const val COMET_PER_YIELD = 0.5

    /**
     * Heavy elements a digging galaxy turns up per second, keyed by element id.
     *
     * The first route to metal that does not go through a collapse, and deliberately a slow one:
     * the forge is supposed to be the reason to keep collapsing, and a dig that outpaced a collapse
     * would quietly replace it rather than supplement it. What this buys is the ability to top up
     * the last few hundred grams of a rare metal without a whole further universe.
     */
    fun metalPerSecond(state: GameState): Map<String, Double> {
        val digging = parked(state).sumOf { workingYield(state, it, GalaxyJob.GRABEN) }
        if (digging <= 0.0) return emptyMap()
        return HeavyElement.entries.associate { metal ->
            metal.id to digging * metal.perRoot * METAL_PER_HOUR / 3_600.0
        }
    }

    /** Grams of the commonest metal one point of yield digs up in an hour. */
    const val METAL_PER_HOUR = 6.0

    /** Credits [seconds] of digging. Nothing at all when no galaxy is on it. */
    fun advanceMetal(state: GameState, seconds: Double): GameState {
        if (seconds <= 0.0) return state
        val rates = metalPerSecond(state)
        if (rates.isEmpty()) return state

        val dug = state.heavy.toMutableMap()
        for ((id, perSecond) in rates) {
            dug[id] = (dug[id] ?: 0.0) + perSecond * seconds
        }
        return state.copy(heavy = dug)
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
            // Through the same gate as the multiplier: a galaxy that has been told to think, look
            // or dig is not also still leaning on the run it left behind.
            val strength = workingYield(state, universe, GalaxyJob.FOERDERN)
            // Nothing rather than a list of neutral multipliers. At zero share every `taper` lands
            // on exactly one, so the old version handed the modifier pass a handful of
            // multiply-by-one entries per idle galaxy — harmless arithmetic, but it made "this
            // galaxy contributes nothing" indistinguishable from "this galaxy contributes" to
            // anything looking at the list.
            if (strength <= 0.0) {
                emptyList()
            } else {
                val share = leanShare(strength)
                // Both leans where there are two, each at the same share. That is what a merged
                // galaxy is *for*: it is worth less than the two it was made of, and it does two
                // things at once.
                universe.paths.flatMap { it.effects }.mapNotNull { softened(it, share) }
            }
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
     * Welds two galaxies into one, freeing the slot the second one stood in.
     *
     * A full sky used to be an ending: the ninth big bang displaced the weakest galaxy, and that
     * galaxy was simply gone. Nothing a player spent thirty collapses on should evaporate because
     * they pressed the button that the game had spent thirty collapses telling them to press.
     *
     * So the sky becomes a puzzle instead. Two galaxies merge into one that keeps the deeper of
     * the two everywhere it matters and carries both leans at once — the only object in the game
     * that does — and the slot the other one held opens up for the universe you are about to
     * finish. Filling all eight is no longer the last thing that happens up there; it is the point
     * at which the sky starts asking questions.
     *
     * The merged galaxy is deliberately *not* the sum. Two galaxies of ten collapses do not make
     * one of twenty: a sum would make merging strictly better than not merging, and the whole
     * decision is that a merge buys a slot and costs some of what was standing in it.
     */
    fun merge(state: GameState, keepSlot: Int, absorbSlot: Int): GameState {
        if (keepSlot == absorbSlot) return state
        val keep = state.universes.firstOrNull { it.slot == keepSlot } ?: return state
        val absorb = state.universes.firstOrNull { it.slot == absorbSlot } ?: return state
        // Neither may be mid-changeover: a galaxy that is doing nothing is not a galaxy anybody
        // can judge the worth of, and merging one away would hide the cost of the ramp inside the
        // cost of the merge.
        if (keep.isRamping || absorb.isRamping) return state

        val merged = keep.copy(
            bestTier = maxOf(keep.bestTier, absorb.bestTier),
            collapses = maxOf(keep.collapses, absorb.collapses) +
                (minOf(keep.collapses, absorb.collapses) * MERGE_SHARE).toInt(),
            singularities = maxOf(keep.singularities, absorb.singularities) +
                minOf(keep.singularities, absorb.singularities) * MERGE_SHARE,
            // The second lean is remembered separately, because a galaxy has one path and this one
            // has two. Everything downstream reads both.
            secondPathId = absorb.pathId ?: keep.secondPathId,
            // The better of the two build-outs, not the sum. Äonen spent on the absorbed galaxy
            // are not simply lost — but two half-built galaxies do not weld into a finished one,
            // or the cheapest route to a maximum level would be to build two and merge them.
            level = maxOf(keep.level, absorb.level),
        )
        // Both removals by slot, and neither by value. `List - element` removes the first entry
        // that compares equal, and `ParkedUniverse` is a data class — two galaxies that happened
        // to be identical in every field would make it take the wrong one. Slots are unique, so
        // slots are what identifies a galaxy.
        return state.copy(
            universes = state.universes.filterNot { it.slot == absorbSlot || it.slot == keepSlot } + merged,
        )
    }

    /** How much of the weaker galaxy survives the weld. */
    const val MERGE_SHARE = 0.5

    /**
     * What an occupied orbit does for the galaxy standing in the matching slot.
     *
     * The two systems had nothing to do with each other. Orbits were the mid-game's puzzle and the
     * sky quietly took the endgame's attention, which left eight slots up there and eight slots
     * down here that never once looked at one another — and the orbit system slowly became the
     * thing you set up once and stopped thinking about.
     *
     * Slot for slot, then: a body in orbit four lends its weight to the galaxy in slot four. It
     * costs nothing to discover, because both systems already number their slots the same way and
     * the numbering was always visible; what it costs is the choice of *which* orbits to occupy,
     * which the tides already make into a real decision.
     */
    fun orbitBonusFor(state: GameState, universe: ParkedUniverse): Double {
        val orbit = Orbits.at(universe.slot) ?: return 0.0
        if (universe.slot >= state.orbits) return 0.0
        if (!Orbits.isOccupied(state, orbit)) return 0.0
        // Counted off the satellite's rung rather than its mass, for the same reason the orbit
        // system's own yield is: mass up there spans thirty orders of magnitude and rungs span
        // twenty-four.
        return ORBIT_PER_TIER * (Orbits.tierOn(state, orbit).index + 1)
    }

    /** What one rung of a matching satellite is worth to its galaxy. */
    const val ORBIT_PER_TIER = 0.012

    /** Whether two galaxies can be welded right now. */
    fun canMerge(state: GameState, keepSlot: Int, absorbSlot: Int): Boolean {
        if (keepSlot == absorbSlot) return false
        val keep = state.universes.firstOrNull { it.slot == keepSlot } ?: return false
        val absorb = state.universes.firstOrNull { it.slot == absorbSlot } ?: return false
        // Only ever with a full sky. With a slot free there is nothing to buy, and a merge would
        // be pure loss dressed up as a choice.
        // Neither side may already be a weld. Only `keep` was checked, which let an *absorbed*
        // galaxy carry a second lean into the merge — where it was silently dropped, because a
        // merged galaxy has room for exactly two and one of those is already spoken for. Losing a
        // lean without saying so is worse than refusing the merge.
        return !hasRoom(state) &&
            !keep.isRamping && !absorb.isRamping &&
            !keep.isMerged && !absorb.isMerged
    }

    /**
     * How long one visit to an old universe lasts, in seconds.
     *
     * An hour, and then it hands the player back to the newest universe. A visit is a session with
     * a clock on it rather than a second home — without the clock the newest universe becomes the
     * one you *left*, and eight parallel games all being nudged along is a different game from
     * this one.
     *
     * The clock runs on the tick, so it is an hour of *play* and not an hour of wall time. Closing
     * the app in the middle of a visit does not spend it.
     */
    const val VISIT_SECONDS = 60.0 * 60.0

    /** The galaxy currently being played, or `null` in the newest universe. */
    fun visited(state: GameState): ParkedUniverse? =
        state.visiting?.let { slot -> state.universes.firstOrNull { it.slot == slot } }

    /** Whether [slot] can be dropped into right now. */
    fun canVisit(state: GameState, slot: Int): Boolean {
        if (state.visiting != null) return false
        // Not out of a challenge. A challenge is a set of rules on the run in front of you, and
        // walking out of the run to somewhere the rules do not apply is not beating it.
        if (state.runningChallengeIds.isNotEmpty()) return false
        // Every galaxy can be entered. The ones from before this existed get a universe built from
        // their chronicle on the way in, so having a full sky is no longer a reason to be locked
        // out of the sky.
        return state.universes.any { it.slot == slot }
    }

    /**
     * Drops into the universe parked in [slot].
     *
     * The universe being left is stowed exactly where a visited one is stowed, so coming back is
     * the same operation in the other direction and there is only one piece of code to be wrong.
     */
    fun visit(state: GameState, slot: Int): GameState {
        if (!canVisit(state, slot)) return state
        val galaxy = state.universes.first { it.slot == slot }
        val loaded = galaxy.run ?: UniverseRun.reconstruct(galaxy)

        return loaded.applyTo(state).copy(
            homeRun = UniverseRun.of(state),
            visiting = slot,
            visitSecondsLeft = VISIT_SECONDS,
        )
    }

    /**
     * Puts the visited universe back in its galaxy and returns to the newest one.
     *
     * Whatever was done during the visit is kept: the galaxy is stowed as it stands now, not as it
     * was when the visit began. That is the whole point of going.
     */
    fun leave(state: GameState): GameState {
        val slot = state.visiting ?: return state
        val home = state.homeRun ?: return state
        val stowed = UniverseRun.of(state)

        return home.applyTo(state).copy(
            universes = state.universes.map {
                if (it.slot == slot) it.copy(run = stowed, bestTier = maxOf(it.bestTier, stowed.bestTierOf())) else it
            },
            homeRun = null,
            visiting = null,
            visitSecondsLeft = 0.0,
        )
    }

    /** Counts the visit down, and shows the player out when it runs out. */
    fun advanceVisit(state: GameState, seconds: Double): GameState {
        if (state.visiting == null || seconds <= 0.0) return state
        val left = state.visitSecondsLeft - seconds
        return if (left > 0.0) state.copy(visitSecondsLeft = left) else leave(state)
    }

    /**
     * Puts a galaxy on a job, starting its changeover.
     *
     * Setting the job it is already on does nothing at all, rather than restarting the ramp — a
     * mis-tap must never cost half an hour.
     */
    fun assign(state: GameState, slot: Int, job: GalaxyJob): GameState {
        val universe = state.universes.firstOrNull { it.slot == slot } ?: return state
        if (universe.job == job) return state
        return state.copy(
            universes = state.universes.map {
                if (it.slot == slot) it.copy(jobId = job.id, rampSeconds = RAMP_SECONDS) else it
            },
        )
    }

    /**
     * How long a galaxy is idle after being put on something else, in seconds.
     *
     * Half an hour, and it contributes *nothing* while it runs. Without a cost the right play is
     * to switch to whichever job pays for the next purchase and switch straight back, which is not
     * a decision, it is an errand. With one, choosing is choosing.
     */
    const val RAMP_SECONDS = 30.0 * 60.0

    /** Counts every running changeover down by [seconds]. */
    fun advanceRamps(state: GameState, seconds: Double): GameState {
        if (seconds <= 0.0) return state
        if (state.universes.none { it.isRamping }) return state
        return state.copy(
            universes = state.universes.map {
                if (it.isRamping) it.copy(rampSeconds = (it.rampSeconds - seconds).coerceAtLeast(0.0)) else it
            },
        )
    }

    /**
     * Turns the universe about to end into the record that outlives it.
     *
     * Reads the state as it stands the moment before the big bang, so `bestTier` here is the
     * deepest rung this universe ever stood on rather than the one it happens to be on.
     */
    fun park(state: GameState, slot: Int, nowMillis: Long): ParkedUniverse = ParkedUniverse(
        slot = slot,
        // The universe itself, paused rather than discarded — this is what makes it visitable.
        run = UniverseRun.of(state),
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
