package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The workshop above the heavy elements: the one place metal is ever spent. */
class AlloyTest {

    private val now = 1_700_000_000_000L

    private fun withMetal(amount: Double): GameState = GameState.new(now).copy(
        collapses = 5,
        heavy = HeavyElement.entries.associate { it.id to amount },
    )

    @Test
    fun `forging takes both metals and hands over the effect for good`() {
        val alloy = Alloy.ELEKTRUM
        val before = withMetal(alloy.cost * 2)
        assertTrue(Alloy.canForge(before, alloy))

        val after = GameEngine.forgeAlloy(before, alloy.id)
        assertTrue(Alloy.isForged(after, alloy))
        assertEquals(alloy.cost, Heavy.amountOf(after, alloy.first), 1e-9)
        assertEquals(alloy.cost, Heavy.amountOf(after, alloy.second), 1e-9)
        assertTrue(Alloy.effects(after).isNotEmpty())

        // And it is worth something: the same fold every other permanent bonus goes through.
        val busy = { s: GameState -> GameEngine.massPerSecond(s.copy(collectors = mapOf("dust" to 50), runMass = 1e6)) }
        assertTrue(busy(after) > busy(before), "Elektrum bringt nichts")
    }

    @Test
    fun `a short pile is refused outright rather than half charged`() {
        val alloy = Alloy.ELEKTRUM
        val short = withMetal(alloy.cost).copy(
            heavy = mapOf(alloy.first.id to alloy.cost, alloy.second.id to alloy.cost - 1.0),
        )
        assertFalse(Alloy.canForge(short, alloy))
        assertEquals(short, GameEngine.forgeAlloy(short, alloy.id), "Es wurde trotzdem Metall genommen")
    }

    @Test
    fun `nothing is forged twice and nothing unknown is forged at all`() {
        val alloy = Alloy.SCHWERGUSS
        val once = GameEngine.forgeAlloy(withMetal(alloy.cost * 5), alloy.id)
        assertEquals(once, GameEngine.forgeAlloy(once, alloy.id), "Zweimal geschmiedet")
        assertEquals(once, GameEngine.forgeAlloy(once, "al_gibtsnicht"))
    }

    @Test
    fun `alloys survive both resets`() {
        val alloy = Alloy.ZUENDKERN
        val forged = GameEngine.forgeAlloy(withMetal(alloy.cost * 3), alloy.id)
            .copy(runMass = 1e30, mass = 1e30, collapses = 40, bestTier = Tiers.last.index)

        assertTrue(Alloy.isForged(GameEngine.collapse(forged, now), alloy), "Der Kollaps hat sie eingeschmolzen")
        assertTrue(
            Alloy.isForged(GameEngine.bigBang(forged.copy(collapses = 60), now, Path.HAND.id), alloy),
            "Der Urknall hat sie eingeschmolzen",
        )
        assertTrue(
            Alloy.isForged(GameEngine.startChallenge(forged, "c_hand", now), alloy),
            "Eine Herausforderung hat sie eingeschmolzen",
        )
    }

    @Test
    fun `the workshop is shut before the first collapse`() {
        val fresh = GameState.new(now)
        assertTrue(Alloy.offered(fresh).isEmpty(), "Die Schmiede steht schon offen")
        // No metal exists before a collapse either, so this is belt and braces rather than a
        // second gate — but a shop that appears with nothing buyable in it is worse than no shop.
        assertTrue(Alloy.entries.none { Alloy.canForge(fresh, it) })
    }

    @Test
    fun `every alloy is reachable and says what it costs and does`() {
        for (alloy in Alloy.entries) {
            assertTrue(alloy.cost > 0.0, "${alloy.label} ist umsonst")
            assertTrue(alloy.first != alloy.second, "${alloy.label} besteht aus sich selbst")
            assertTrue(alloy.costText.isNotBlank())
            assertTrue(alloy.effectText.isNotBlank(), "${alloy.label} sagt nicht, was sie tut")
        }
        assertEquals(Alloy.entries.size, Alloy.entries.map { it.id }.toSet().size)
        // Every heavy element has to be wanted by something, or a pile grows for no reason at all.
        val used = Alloy.entries.flatMap { listOf(it.first, it.second) }.toSet()
        for (metal in HeavyElement.entries) {
            assertTrue(metal in used, "${metal.label} wird von keiner Legierung gebraucht")
        }
    }

    @Test
    fun `the new heavy elements pull levers that were empty`() {
        val rich = withMetal(1e6)
        assertTrue(Heavy.factorFor(rich, FusionBonus.FUSION) > 1.0, "Iridium tut nichts")
        assertTrue(Heavy.factorFor(rich, FusionBonus.RESEARCH) > 1.0, "Osmium tut nichts")
        assertTrue(Heavy.factorFor(rich, FusionBonus.COMETS) > 1.0, "Plutonium tut nichts")
    }
}
