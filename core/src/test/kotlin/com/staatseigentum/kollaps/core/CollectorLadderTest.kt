package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The collector ladder as a whole.
 *
 * `ContentTest` already checks that prices and rates climb and that efficiency degrades gently;
 * this is about the shape the ladder has to keep as it grows — that every rung is reachable, that
 * each one has something to buy for it, and that the chain of synergies has no gap in it.
 */
class CollectorLadderTest {

    @Test
    fun `the last collector is affordable before the ladder runs out`() {
        // A collector nobody can ever buy is a row in the shop that only ever says no. The final
        // body's threshold is the most mass a run will ever hold, so the priciest machine has to
        // sit comfortably under it.
        val priciest = Collectors.all.last()
        assertTrue(
            priciest.baseCost < Tiers.last.threshold / 10.0,
            "${priciest.name} kostet ${priciest.baseCost}, die Leiter endet bei ${Tiers.last.threshold}",
        )
    }

    @Test
    fun `every collector becomes visible before it becomes affordable`() {
        // Visibility keys off lifetime mass and affordability off the purse, so a collector that
        // only appeared once it was already payable would pop into the shop pre-bought.
        var state = GameState.new(0).copy(totalMass = 0.0, mass = 0.0)
        for (collector in Collectors.all) {
            state = state.copy(totalMass = collector.baseCost, mass = collector.baseCost)
            val offer = GameEngine.collectorOffers(state, BuyAmount.ONE)
                .first { it.collector.id == collector.id }
            assertTrue(offer.visible, "${collector.name} ist bei eigenem Preis noch unsichtbar")
        }
    }

    @Test
    fun `the synergy chain reaches the end of the ladder`() {
        // Each collector but the first should be the target of some synergy, otherwise a new
        // machine quietly arrives without the upgrade type that makes the shop interesting.
        val targets = Upgrades.all
            .mapNotNull { (it.effect as? UpgradeEffect.CollectorSynergy)?.targetId }
            .toSet()

        for (collector in Collectors.all.drop(1)) {
            assertTrue(
                collector.id in targets,
                "${collector.name} bekommt von niemandem eine Synergie",
            )
        }
    }

    @Test
    fun `every synergy points at a later collector than its source`() {
        // Forwards only: a synergy that fed a cheaper machine from a dearer one would be a bonus
        // that arrives long after the thing it improves has stopped mattering.
        val order = Collectors.all.withIndex().associate { (index, it) -> it.id to index }
        for (upgrade in Upgrades.all) {
            val effect = upgrade.effect as? UpgradeEffect.CollectorSynergy ?: continue
            assertTrue(
                order.getValue(effect.sourceId) < order.getValue(effect.targetId),
                "${upgrade.id} zeigt rückwärts",
            )
        }
    }

    @Test
    fun `every collector carries a full set of marks`() {
        for (collector in Collectors.all) {
            val marks = Upgrades.all.count {
                (it.effect as? UpgradeEffect.CollectorMultiplier)?.collectorId == collector.id
            }
            assertEquals(5, marks, "${collector.name} hat $marks Mk-Stufen")
        }
    }

    @Test
    fun `names and ids are unique and nothing is blank`() {
        assertEquals(Collectors.all.size, Collectors.all.map { it.id }.toSet().size)
        assertEquals(Collectors.all.size, Collectors.all.map { it.name }.toSet().size)
        for (collector in Collectors.all) {
            assertTrue(collector.name.isNotBlank())
            assertTrue(collector.flavor.isNotBlank())
            assertNotNull(Collectors.byId(collector.id))
        }
    }

    @Test
    fun `a save from before the new collectors keeps everything it had`() {
        val old = GameState.new(0).copy(collectors = mapOf("dust" to 40, "echo" to 3))
        val restored = SaveCodec.decode(SaveCodec.encode(old))!!
        assertEquals(old.collectors, restored.collectors)
        assertEquals(0, restored.ownedOf("omega"))
    }
}
