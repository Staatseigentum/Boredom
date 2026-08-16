package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The ladder grew from twenty-five rungs to nearly seventeen thousand, and `bestTier` grew with it.
 *
 * Everything that reads a tier index had been written when twenty-five was the whole world. These
 * are the regressions for the places that turned out to still believe it.
 */
class LadderSafetyTest {

    private val now = 1_700_000_000_000L

    /** Somebody standing a long way up the catalogue ladder. */
    private fun high(rung: Int): GameState = GameState.new(now).copy(
        bestTier = Tiers.last.index + rung,
        runMass = Tiers.last.threshold * Designations.ENTRY_STEP *
            Math.pow(Designations.THRESHOLD_GROWTH, rung.toDouble() + 0.5),
        universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it) },
        bigBangs = Multiverse.SLOTS,
        collapses = 40,
    )

    @Test
    fun `the statistics screen survives a catalogue rung`() {
        // This threw. `Tiers.all[state.bestTier]` is a raw index into a twenty-five entry list, so
        // the statistics screen crashed for exactly the players who had climbed past the gate.
        for (rung in listOf(1, 100, Designations.PER_BODY, Designations.COUNT - 1)) {
            val lines = Statistics.lines(high(rung))
            assertTrue(lines.isNotEmpty(), "Keine Statistik bei Sprosse $rung")
            val best = lines.firstOrNull { it.label == "Höchste Stufe" }
            assertNotNull(best, "Die höchste Stufe fehlt bei Sprosse $rung")
            assertTrue(best.value.isNotBlank())
        }
    }

    @Test
    fun `every index the game can hold resolves to a drawable rung`() {
        val edges = listOf(
            0,
            Tiers.last.index,
            Designations.FIRST_INDEX,
            Designations.TOTAL - 1,
            // Past the end and below the start: both have to clamp rather than throw, because a
            // save file is a thing that can be edited.
            Designations.TOTAL + 5_000,
            -3,
        )
        for (index in edges) {
            val tier = Tiers.byIndex(index)
            assertTrue(tier.label.isNotBlank(), "Stufe $index hat keine Beschriftung")
            assertTrue(tier.threshold.isFinite(), "Stufe $index hat eine kaputte Schwelle")
            assertTrue(tier.productionMultiplier.isFinite() && tier.productionMultiplier > 0.0)
        }
    }

    @Test
    fun `a designated rung shows its designation and not just the body`() {
        val tier = Tiers.byIndex(Designations.FIRST_INDEX + 27)
        assertTrue(tier.isDesignated)
        assertTrue(tier.label != tier.name, "Die Kennung fehlt in der Beschriftung: ${tier.label}")
        assertTrue(tier.label.startsWith(tier.name), "Die Beschriftung hat den Körper verloren")
        // And the name stays the key, so everything that points at a rung by name still finds it.
        assertEquals(Tiers.byName("Meteorit").name, Tiers.byIndex(Designations.FIRST_INDEX).name)
    }

    @Test
    fun `nothing on the whole ladder overflows a double`() {
        val top = Tiers.byIndex(Designations.TOTAL - 1)
        assertTrue(top.threshold.isFinite(), "Die oberste Schwelle ist ${top.threshold}")
        assertTrue(top.productionMultiplier.isFinite(), "Der oberste Multiplikator entgleist")
        // And production worked out on it stays a number.
        val standing = high(Designations.COUNT - 1).copy(
            collectors = Collectors.all.associate { it.id to Collector.MAX_OWNED },
            singularities = 1e6,
        )
        assertTrue(GameEngine.massPerSecond(standing).isFinite(), "Produktion ganz oben entgleist")
        assertTrue(GameEngine.pendingSingularities(standing).isFinite(), "Die Ausschüttung entgleist")
    }

    @Test
    fun `the collapse gate stays at the black hole however high the ladder goes`() {
        assertTrue(Tiers.last.isFinal, "Das Schwarze Loch ist nicht mehr das Tor")
        assertTrue(
            Tiers.byIndex(Designations.FIRST_INDEX + 500).isFinal.not(),
            "Eine Katalogsprosse behauptet, das Tor zu sein",
        )
        // Which is what keeps collapsing possible at all up there.
        assertTrue(GameEngine.canCollapse(high(500)), "Ganz oben lässt sich nicht mehr kollabieren")
    }
}
