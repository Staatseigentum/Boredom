package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Where Äonen come from, and how many of them a day can possibly produce.
 *
 * This exists because of a question that took an hour to answer: a player reported waking up to two
 * hundred and thirty-five Äonen and nobody could say whether that was a bug. Answering it meant
 * finding every source by hand, working out each one's maximum, and adding them up — and the answer
 * turned out to be an accumulation nobody had bounded, through a contract that could be farmed
 * without limit.
 *
 * So the sum is written down here instead. Four sources, each with a ceiling, and a total. The
 * numbers are deliberately generous: this is not a balance test and it should not go red because
 * something got ten per cent better. It goes red when a *new* source appears without a ceiling, or
 * when an existing one loses the one it had — which is exactly what happened, and is the only class
 * of fault that produces a number nobody can explain.
 */
class AeonBudgetTest {

    private val now = 1_700_000_000_000L

    /** A deep save with a full, finished sky — the most Äonen-productive state the game has. */
    private fun endgame(): GameState = GameState(
        bigBangs = 8,
        collapses = 60,
        singularities = 5_000.0,
        runMass = Tiers.last.threshold * 1e6,
        universes = (0 until Multiverse.SLOTS).map { slot ->
            ParkedUniverse(
                slot = slot,
                bestTier = Tiers.indexOf("Schwarzes Loch"),
                collapses = 30,
                singularities = 5_000.0,
                level = Multiverse.MAX_LEVEL,
                jobId = GalaxyJob.RECHNEN.id,
            )
        },
    )

    @Test
    fun `the sky is the only source that pays while nobody is playing, and it is capped`() {
        val state = endgame()
        val perDay = Multiverse.aeonsPerSecond(state) * 86_400.0
        assertTrue(perDay > 0.0, "Ein voller Himmel verdient gar nichts")
        assertTrue(
            perDay < SKY_PER_DAY_CEILING,
            "Der Himmel zahlt $perDay Äonen am Tag — über der Schranke $SKY_PER_DAY_CEILING",
        )

        /*
         * And the offline credit is bounded — through the path the game actually uses.
         *
         * [Multiverse.advance] is deliberately uncapped: the tick calls it with a fraction of a
         * second and capping there would be nonsense. The cap belongs to the caller that can be
         * handed a year at once, which is [GameEngine.applyOffline], and that is what this asks.
         * Testing the inner function instead would have "found" a hole that is not one and missed
         * whether the real door is locked.
         */
        val away = state.copy(lastSeenAt = now - 400L * 86_400_000L)
        val credited = GameEngine.applyOffline(away, now).state.let {
            it.aeons - away.aeons + (it.aeonFraction - away.aeonFraction)
        }
        val cappedDays = GameEngine.SKY_OFFLINE_CAP_SECONDS / 86_400.0
        assertTrue(
            credited <= perDay * cappedDays + 1.0,
            "Vierhundert Tage Abwesenheit zahlen $credited Äonen aus, " +
                "erlaubt sind ${perDay * cappedDays}",
        )
    }

    @Test
    fun `every contract is bounded, one way or the other`() {
        /*
         * The source that actually caused the report, stated as a rule rather than a sum.
         *
         * A contract pays in the permanent currency, so it needs a reason it cannot be repeated for
         * ever. There are exactly two acceptable reasons: a [Contract.dailyLimit], or a
         * [Contract.ceiling] that says the pool it counts from is finite and will run out.
         *
         * Three had neither when this test was written — the fleet, the orbits and the gold. All
         * three count the *current* state, which a collapse resets, so all three came back around
         * every run. That is the same shape as the collapse contract that was farmed to death, and
         * it was sitting there unnoticed because nothing had ever asked the question in general.
         */
        val open = Contract.all.filter { it.dailyLimit == null && it.ceiling == null }
        assertTrue(
            open.isEmpty(),
            "Unbegrenzt wiederholbar: ${open.joinToString { it.id }}",
        )

        val perDay = Contract.all.sumOf { it.reward * (it.dailyLimit ?: 1) }
        assertTrue(
            perDay < CONTRACTS_PER_DAY_CEILING,
            "Aufträge zahlen bis zu $perDay Äonen am Tag — über der Schranke",
        )
    }

    @Test
    fun `a catalogue find pays a single Äon and only when answered`() {
        // The smallest source, and the one most likely to be quietly scaled up later.
        assertEquals(1.0, CatalogueFind.AEON_REWARD)
    }

    @Test
    fun `the whole day, from every source at once`() {
        val state = endgame()
        val sky = Multiverse.aeonsPerSecond(state) * 86_400.0
        val contracts = Contract.all.sumOf { it.reward * (it.dailyLimit ?: 1) }
        // A find can appear a handful of times a day at most; being generous costs nothing here.
        val finds = CatalogueFind.AEON_REWARD * 12
        // And the presses: a run to the black hole takes hours, so a day holds a few big bangs.
        val bangs = BigBang.pending(state) * 4

        val total = sky + contracts + finds + bangs
        assertTrue(
            total < TOTAL_PER_DAY_CEILING,
            "Ein Tag kann $total Äonen bringen (Himmel $sky, Aufträge $contracts, " +
                "Funde $finds, Urknalle $bangs) — über der Schranke $TOTAL_PER_DAY_CEILING",
        )
    }

    private companion object {
        /**
         * Ceilings, in Äonen per day.
         *
         * Round numbers with room above what the game currently produces. They are tripwires, not
         * targets — the failure they are here to catch is a *category* change, not a tuning change.
         */
        const val SKY_PER_DAY_CEILING = 40.0
        const val CONTRACTS_PER_DAY_CEILING = 120.0
        const val TOTAL_PER_DAY_CEILING = 400.0

    }
}
