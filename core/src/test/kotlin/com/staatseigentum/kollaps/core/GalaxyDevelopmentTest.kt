package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Visiting an old galaxy and building it out. */
class GalaxyDevelopmentTest {

    private val now = 1_700_000_000_000L

    private fun sky(aeons: Double = 1_000.0): GameState = GameState.new(now).copy(
        bigBangs = Multiverse.SLOTS,
        aeons = aeons,
        universes = (0 until Multiverse.SLOTS).map {
            ParkedUniverse(slot = it, pathId = Path.MASCHINE.id, bestTier = 20, collapses = 12)
        },
    )

    @Test
    fun `a level costs aeons and is worth something for good`() {
        val before = sky()
        val cost = Multiverse.costOfNextLevel(before.universes.first())!!
        val after = GameEngine.developGalaxy(before, 0)

        assertEquals(before.aeons - cost, after.aeons, 1e-9)
        assertEquals(1, after.universes.first { it.slot == 0 }.level)
        assertTrue(
            Multiverse.yieldOf(after.universes.first { it.slot == 0 }) >
                Multiverse.yieldOf(before.universes.first { it.slot == 0 }),
            "Der Ausbau bringt nichts",
        )
        assertTrue(Multiverse.multiplier(after) > Multiverse.multiplier(before))
        assertTrue(Multiverse.aeonsPerSecond(after) > Multiverse.aeonsPerSecond(before))
    }

    @Test
    fun `nothing is built without the aeons for it`() {
        val broke = sky(aeons = 0.0)
        assertFalse(Multiverse.canDevelop(broke, 0))
        assertEquals(broke.aeons, GameEngine.developGalaxy(broke, 0).aeons)
        assertEquals(broke.universes, GameEngine.developGalaxy(broke, 0).universes)

        // Nor onto a slot that is not there. Compared field by field rather than whole, because
        // the call goes through `award` and a rich sky earns achievements just by being looked at.
        val rich = sky()
        val missed = GameEngine.developGalaxy(rich, 99)
        assertEquals(rich.aeons, missed.aeons, "Ein Platz, den es nicht gibt, hat Äonen gekostet")
        assertEquals(rich.universes, missed.universes)
    }

    @Test
    fun `the price climbs and the track ends`() {
        var state = sky(aeons = 1e9)
        val prices = mutableListOf<Double>()
        repeat(Multiverse.MAX_LEVEL) {
            prices += Multiverse.costOfNextLevel(state.universes.first { it.slot == 0 })!!
            state = GameEngine.developGalaxy(state, 0)
        }

        assertEquals(Multiverse.MAX_LEVEL, state.universes.first { it.slot == 0 }.level)
        assertTrue(prices.zipWithNext().all { (a, b) -> b > a }, "Der Preis steigt nicht: $prices")
        assertTrue(prices.last() > prices.first() * 5, "Die letzte Stufe ist zu billig")

        // Finished means finished — no level sixteen, at any price.
        assertNull(Multiverse.costOfNextLevel(state.universes.first { it.slot == 0 }))
        assertFalse(Multiverse.canDevelop(state, 0))
        assertEquals(state.aeons, GameEngine.developGalaxy(state, 0).aeons)
    }

    @Test
    fun `a shallow galaxy is the cheap one to lift`() {
        // The decision the price is shaped for: the sky's weakest slot is a question — weld it
        // away, or spend on it — and it would not be one if improving it cost more.
        val mixed = sky().copy(
            universes = listOf(
                ParkedUniverse(slot = 0, bestTier = 3, collapses = 2),
                ParkedUniverse(slot = 1, bestTier = 24, collapses = 40),
            ),
        )
        assertEquals(
            Multiverse.costOfNextLevel(mixed.universes[0]),
            Multiverse.costOfNextLevel(mixed.universes[1]),
            "Der Preis hängt an der Tiefe der Galaxie",
        )
    }

    @Test
    fun `a weld keeps the better build-out and not the sum`() {
        var state = sky(aeons = 1e6)
        repeat(5) { state = GameEngine.developGalaxy(state, 0) }
        repeat(3) { state = GameEngine.developGalaxy(state, 1) }

        val merged = GameEngine.mergeGalaxies(state, keepSlot = 0, absorbSlot = 1)
            .universes.first { it.slot == 0 }
        assertEquals(5, merged.level, "Zwei halb ausgebaute Galaxien ergeben eine ganze")
    }

    @Test
    fun `building out is allowed while the galaxy is changing over`() {
        // The ramp is about what a galaxy is doing; this is about what it is.
        val switching = GameEngine.assignGalaxy(sky(), 0, GalaxyJob.RECHNEN.id)
        assertTrue(switching.universes.first { it.slot == 0 }.isRamping)
        assertTrue(Multiverse.canDevelop(switching, 0))
        assertEquals(1, GameEngine.developGalaxy(switching, 0).universes.first { it.slot == 0 }.level)
    }

    @Test
    fun `the build-out survives every reset and the save`() {
        var state = sky(aeons = 1e6)
        repeat(4) { state = GameEngine.developGalaxy(state, 0) }
        state = state.copy(runMass = 1e30, mass = 1e30, collapses = 60, bestTier = Tiers.last.index)

        assertEquals(4, GameEngine.collapse(state, now).universes.first { it.slot == 0 }.level)
        assertEquals(4, GameEngine.bigBang(state, now, Path.HAND.id).universes.first { it.slot == 0 }.level)

        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertTrue(back != null)
        assertEquals(4, back.universes.first { it.slot == 0 }.level)
    }

    @Test
    fun `a save from before this reads as unbuilt rather than breaking`() {
        val old = ParkedUniverse(slot = 0, bestTier = 20, collapses = 10)
        assertEquals(0, old.level)
        assertEquals(Multiverse.LEVEL_BASE_COST, Multiverse.costOfNextLevel(old))
    }
}
