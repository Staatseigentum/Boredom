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
     * What the first designation costs, in seconds of play.
     *
     * The catalogue used to be priced in kilograms and that could not work. A fixed ladder of
     * masses is crossed at whatever speed production happens to be, and by the time eight
     * galaxies are standing that speed is not a number anybody has intuitions about: a bot built
     * to that point walked the whole AA–ZZ round — two thousand seven hundred rungs — in *three
     * seconds*, its mass doubling every twenty-one milliseconds. Raising the thresholds cannot
     * fix it. They are a geometric series bounded by what a `Double` holds; production is bounded
     * by nothing, because singularities, investments, fusion and the multiverse all multiply and
     * every kilogram in hand buys more fleet.
     *
     * So a rung is not a quantity, it is a *wait* — [GameState.runSeconds] of it. Production
     * cancels out entirely: a player making a hundred times more climbs at exactly the same rate,
     * which is the only way an endless ladder can be paced at all.
     *
     * Read off the run clock rather than off production, and that is not a detail. Production is
     * a function of the rung (a designation pays more), so pricing a rung in production would ask
     * the tier to know itself — it did, briefly, and the recursion went all the way down.
     */
    const val ENTRY_SECONDS = 60.0

    /**
     * How much longer each rung takes than the one below it.
     *
     * The wait for rung *n* is `ENTRY_SECONDS × TIME_GROWTH^(n-1)`, so the whole climb to rung
     * *n* is that series summed — see [secondsFor]. At this rate one full round of designations,
     * AA through ZZ, is six hundred and seventy-six rungs and about five days of play: a minute
     * for the first, some forty minutes for the last.
     */
    const val TIME_GROWTH = 1.0055

    /**
     * And how much more each rung produces.
     *
     * Unchanged in spirit and now free of the job it used to do. It no longer has to stay below a
     * threshold growth to keep the ladder honest, because the threshold is not a mass any more —
     * a rung that pays more simply buys the same wait sooner in kilograms and not at all in
     * seconds.
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
            // A stand-in: the real price of a rung depends on the run that is climbing it,
            // and [forRun] stamps that on. This is what a caller sees who has an index and
            // no game — the ladder screen's pictures, mostly.
            threshold = massFor(step),
            productionMultiplier = anchor.productionMultiplier * PRODUCTION_GROWTH.pow(step),
        )
    }

    /**
     * Seconds of play needed to *reach* rung [step], counted from the black hole.
     *
     * The waits are geometric, so the climb is their sum. Closed form because half a dozen call
     * sites ask this on every frame and a loop over sixteen thousand rungs would be absurd.
     */
    fun secondsFor(step: Int): Double =
        ENTRY_SECONDS * (TIME_GROWTH.pow(step) - 1.0) / (TIME_GROWTH - 1.0)

    /**
     * The mass rung [step] asks for, on top of the wait.
     *
     * A second gate, and a loose one. Time is what binds for anybody actually climbing; this only
     * catches the case time cannot see — a run holding far more mass than it could have made,
     * which is what an imported save or a fixture looks like rather than a played one.
     */
    fun massFor(step: Int): Double = Tiers.last.threshold * MASS_GROWTH.pow(step)

    /** How much more mass each rung asks than the one below. The looser of the two gates. */
    const val MASS_GROWTH = 1.0375

    /**
     * The rung a run stands on: it has to have put in the time *and* have the mass.
     *
     * Both, and the lower of the two readings. Neither on its own is a rule — time alone would
     * hand rungs to a save that sat still with a huge fleet, and mass alone is what let the whole
     * ladder go by in three seconds.
     */
    fun forRun(runMass: Double, runSeconds: Double): CelestialTier {
        val anchor = Tiers.last
        if (runMass < anchor.threshold || runSeconds < ENTRY_SECONDS) return anchor

        // Inverse of [secondsFor]. The nudge covers the last bit of binary error, which would
        // otherwise park a run one rung below the wait it has actually served.
        val grown = 1.0 + runSeconds * (TIME_GROWTH - 1.0) / ENTRY_SECONDS
        val byTime = floor(ln(grown) / ln(TIME_GROWTH) + 1e-9).toInt()
        val byMass = floor(ln(runMass / anchor.threshold) / ln(MASS_GROWTH) + 1e-9).toInt()

        val steps = minOf(byTime, byMass)
        if (steps < 1) return anchor
        val index = (FIRST_INDEX + steps - 1).coerceAtMost(TOTAL - 1)
        return at(index)
    }

    /**
     * The rung a bare mass reads as, with no fleet to price it against.
     *
     * A weaker question than [forRun] and it exists for the two callers that can only ask it: the
     * record kept of a parked universe, and the achievements that compare a mass to a rung. Both
     * are looking at something finished rather than pricing a climb, so the stand-in thresholds
     * from [build] are the right answer and the fleet is none of their business.
     */
    fun forNominalMass(mass: Double): CelestialTier {
        val anchor = Tiers.last
        if (mass < anchor.threshold) return anchor
        val steps = floor(ln(mass / anchor.threshold) / ln(MASS_GROWTH) + 1e-9).toInt()
        if (steps < 1) return anchor
        return at((FIRST_INDEX + steps - 1).coerceAtMost(TOTAL - 1))
    }

    /** The rung above [tier], or `null` at the very top. */
    fun above(tier: CelestialTier): CelestialTier? {
        val index = tier.index + 1
        return if (index >= TOTAL) null else at(index)
    }
}
