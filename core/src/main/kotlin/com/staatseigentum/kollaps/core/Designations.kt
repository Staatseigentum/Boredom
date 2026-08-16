package com.staatseigentum.kollaps.core

import kotlin.concurrent.Volatile
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * The ladder above the black hole: every body again, six hundred and seventy-six times each.
 *
 * The old ladder stopped at the black hole and so did the game. Everything past it was a number
 * going up with nothing to reach, which is a strange thing for a game whose whole loop is reaching
 * the next thing. So it keeps going — but not by inventing new bodies, because there are only so
 * many objects in a sky and the twenty-sixth would be a made-up word.
 *
 * Instead the ladder starts over with catalogue designations, the way real surveys number what
 * they find once they have run out of names: Meteorit AA, Meteorit AB, all the way to Meteorit ZZ,
 * then Asteroid AA, and so on through **every one** of the twenty-five bodies. Sixteen thousand
 * nine hundred rungs above the black hole — not a number anybody finishes, which is the point: a
 * ladder that does not run out before the player does.
 *
 * ## Why it opens on a full sky
 *
 * It is endgame, and it has to read as endgame. Opened earlier it would be *the* ladder rather
 * than a thing above it, and a new player would spend their first evening clicking through
 * catalogue numbers instead of watching a rock turn into a moon.
 *
 * The condition is every galaxy occupied rather than a count of big bangs, which is the same thing
 * in practice and a different thing to read. A number is arbitrary — why four, why six — and it
 * sits in the interface as a counter nobody can see the end of. A full sky is a picture: eight
 * universes standing where there were none, all of them still working, and the ladder above the
 * black hole opening as the thing that was behind the last empty slot. It is also honest about the
 * dependency, because the sky is what makes the climb survivable in the first place.
 *
 * ## Why this is computed and not a list
 *
 * Sixteen thousand [CelestialTier] objects is a couple of megabytes of near-identical data held
 * for ever, nearly all of it for rungs nobody will see. Every one is derivable from its position —
 * the body is the position divided by [PER_BODY], the designation is the remainder, and both the
 * threshold and the multiplier are one geometric step past the rung below — so they are worked out
 * on demand and only the ones actually visited are kept.
 *
 * ## Why the numbers are what they are
 *
 * The whole ladder has to fit inside a `Double`, which stops at about 1e308. The black hole sits
 * at 2.7e24, leaving room for roughly 283 further orders of magnitude; spread across 16,900 rungs
 * that allows about 3.7 % more mass per rung, and [THRESHOLD_GROWTH] sits just under it.
 * Production grows a little slower than the threshold on purpose, so each rung is a hair harder
 * than the last and the climb stays a climb. What actually carries a player up it is the collapses
 * and the sky, not the rungs themselves.
 */
object Designations {

    /**
     * Galaxies that have to be standing before the ladder continues past the black hole.
     *
     * The whole sky, so this is [Multiverse.SLOTS] by definition rather than a number of its own —
     * "all of them" cannot drift out of step with how many there are.
     */
    val REQUIRED_GALAXIES: Int get() = Multiverse.SLOTS

    /** Big bangs that amounts to, for anything that wants to say so in words. */
    val REQUIRED_BIG_BANGS: Int get() = REQUIRED_GALAXIES

    /** Letters per position. Two positions, so AA through ZZ. */
    private const val LETTERS = 26

    /** Designations one body carries before the next body starts. */
    const val PER_BODY = LETTERS * LETTERS

    /**
     * What the catalogue costs to set foot on at all, as a multiple of the black hole.
     *
     * The ladder used to start a single 3.65 % step above the black hole, and that was the whole
     * problem with it: anybody who has earned the eight galaxies it takes to unlock is producing
     * many orders of magnitude past 2.7e24 kg, so the first hundreds of rungs went by in the time
     * it took to read them. A million times over is the price of the first designation, and it
     * turns entering the catalogue into something that happens rather than something that has
     * already happened.
     */
    const val ENTRY_STEP = 1e6

    /**
     * How much more mass each rung above the black hole costs than the one below it.
     *
     * Close to as steep as this ladder can be made, and that is an arithmetic fact rather than a
     * balance decision. There are [COUNT] rungs, the threshold is a geometric series over all of
     * them, and a `Double` stops at about 1e308 — so the growth, the [ENTRY_STEP] and the
     * anchor together have only about 283 decades to spend. At this rate the last rung lands
     * near 1e301, which is as much headroom as is safe to leave. Raising it further would not
     * make the climb harder; it would make the top of the ladder infinite.
     */
    const val THRESHOLD_GROWTH = 1.0375

    /**
     * And how much more it produces.
     *
     * Below [THRESHOLD_GROWTH], which is the only reason this lands anywhere useful: every rung is
     * harder than the last, so the climb slows as it goes.
     *
     * The gap between the two is the whole difficulty of the catalogue, and it is the one lever
     * the `Double` ceiling does not cap — which is why *this* number moved further than the one
     * above it. At 3.30 % against 3.65 % each rung was 0.34 % harder than the last and the ladder
     * barely slowed at all; at 3.00 % against 3.75 % each rung is 0.73 %, and because that
     * compounds, the two-thousandth rung went from being nine hundred times the first to two
     * million times it.
     */
    const val PRODUCTION_GROWTH = 1.0300

    /**
     * Whether this save has earned the ladder above the black hole.
     *
     * Read off the sky itself, not off the big-bang counter. They agree for anybody who played
     * their way here — but a save carried over from before the multiverse has a counter and a
     * backfilled sky, and the sky is the one that is actually standing there.
     */
    fun isUnlocked(state: GameState): Boolean = Multiverse.count(state) >= REQUIRED_GALAXIES

    /** Rungs above the black hole. */
    val COUNT: Int get() = Tiers.all.size * PER_BODY

    /** The first rung of the designated ladder, one past the black hole. */
    val FIRST_INDEX: Int get() = Tiers.all.size

    /** The whole ladder, named bodies and designations together. */
    val TOTAL: Int get() = FIRST_INDEX + COUNT

    /** Whether [index] is a designated rung rather than one of the twenty-five named bodies. */
    fun isDesignated(index: Int): Boolean = index >= FIRST_INDEX

    /**
     * The designation at [position], as the player reads it: `AA`, `AB`, … `ZZ`.
     *
     * Plain base-26 with both letters always written, which is what a catalogue does — `AB` is the
     * second entry, not the twenty-eighth, and nobody has to be told that.
     */
    fun label(position: Int): String {
        val wrapped = position.mod(PER_BODY)
        return "${'A' + wrapped / LETTERS}${'A' + wrapped % LETTERS}"
    }

    /**
     * The rung at [index], built from its position.
     *
     * Remembers exactly one rung, and that is deliberate on two counts.
     *
     * The first attempt was a `HashMap` keyed by index, which is the obvious shape and wrong twice
     * over. It is a mutable map on a singleton, written from whichever thread happens to ask —
     * the tick and the composition both do, and a `HashMap` torn between two threads does not
     * merely lose an entry, it can spin for ever inside `get`. And it grows without a ceiling that
     * anything enforces: sixteen thousand rungs of held objects, for a saving that was never
     * measured.
     *
     * A single rung is enough because of how this is actually asked. Mass climbs, so consecutive
     * questions are overwhelmingly about the same rung, and the answer costs two `pow` calls and a
     * `copy` when it misses. A stale or half-written memo is harmless here: the worst case is that
     * a rung gets built twice, and every rung is a pure function of its index.
     */
    fun at(index: Int): CelestialTier {
        require(isDesignated(index)) { "Stufe $index gehört nicht zur Kennungsleiter" }
        val clamped = index.coerceAtMost(TOTAL - 1)
        val remembered = memo
        if (remembered != null && remembered.index == clamped) return remembered
        return build(clamped).also { memo = it }
    }

    @Volatile
    private var memo: CelestialTier? = null

    private fun build(index: Int): CelestialTier {
        val position = index - FIRST_INDEX
        val body = Tiers.all[(position / PER_BODY).coerceIn(Tiers.all.indices)]
        val step = position + 1
        val anchor = Tiers.last

        return body.copy(
            index = index,
            // The base body's own name stays the key, so `Tiers.byName("Saturn")` still finds the
            // Saturn that every unlock, challenge and event chain points at, rather than one of
            // its six hundred and seventy-six catalogue entries. Only what is drawn changes.
            designation = position.mod(PER_BODY),
            threshold = anchor.threshold * ENTRY_STEP * THRESHOLD_GROWTH.pow(step),
            productionMultiplier = anchor.productionMultiplier * PRODUCTION_GROWTH.pow(step),
        )
    }

    /**
     * The rung [mass] has reached, for any mass at or above the black hole.
     *
     * Closed form rather than a walk. Half a dozen call sites ask this on every frame, and a loop
     * over sixteen thousand rungs to answer it would be absurd when the thresholds are a geometric
     * series and the answer is one logarithm.
     */
    fun forMass(mass: Double): CelestialTier {
        val anchor = Tiers.last
        // Everything between the black hole and the price of the first designation is still the
        // black hole. That stretch is [ENTRY_STEP] wide and it is the point of the entry step.
        val entry = anchor.threshold * ENTRY_STEP
        if (mass < entry) return anchor
        /*
         * The nudge is not cosmetic.
         *
         * A rung's threshold is `anchor * growth^n`, so asking which rung that exact number
         * reaches divides `ln(growth^n)` by `ln(growth)` and should land on `n`. In binary it
         * lands on 1.9999999999999998, and `floor` then hands back the rung below — so standing
         * on a rung's threshold to the kilogram would show the previous rung. One part in a
         * billion is far below any gap between rungs (they are 3.65 % apart) and far above the
         * error being corrected.
         */
        val steps = floor(ln(mass / entry) / ln(THRESHOLD_GROWTH) + 1e-9).toInt()
        if (steps < 1) return anchor
        return at(FIRST_INDEX + steps - 1)
    }
}
