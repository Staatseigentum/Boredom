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
    fun `every collector is affordable somewhere on the ladder it belongs to`() {
        // A collector nobody can ever buy is a row in the shop that only ever says no.
        //
        // Two ladders now, and each machine has to sit on the one it belongs to. The named ladder
        // ends at the black hole, so everything offered during it has to fit under that. The
        // catalogue machines are allowed to need the ladder above it — but its opening rungs, not
        // rung sixteen thousand, or they are shop rows nobody will ever see either.
        val earlyCatalogue = Designations.at(Designations.FIRST_INDEX + 499).threshold

        for (collector in Collectors.all) {
            val ceiling = if (collector.catalogueOnly) earlyCatalogue else Tiers.last.threshold
            assertTrue(
                collector.baseCost < ceiling / 10.0,
                "${collector.name} kostet ${collector.baseCost}, erreichbar ist bis $ceiling",
            )
        }

        // And the named ladder must keep a fleet of its own rather than becoming a prologue to
        // the shop: most of the machines still have to be buyable before the black hole.
        val named = Collectors.all.count { !it.catalogueOnly }
        assertTrue(named > Collectors.all.size / 2, "Nur $named Kollektoren vor dem Schwarzen Loch")
    }

    @Test
    fun `the catalogue fleet stays shut until the ladder opens`() {
        val rich = GameState.new(0).copy(mass = 1e40, totalMass = 1e40, runMass = 1e40)
        val catalogue = Collectors.all.first { it.catalogueOnly }

        // Not merely hidden — refused. The shop is a view; this is the rule.
        assertEquals(rich, GameEngine.buyCollector(rich, catalogue.id, BuyAmount.ONE))
        assertTrue(
            GameEngine.collectorOffers(rich, BuyAmount.ONE)
                .none { it.collector.catalogueOnly && it.visible },
            "Die Katalogflotte steht schon im Laden",
        )

        val opened = rich.copy(
            bigBangs = Multiverse.SLOTS,
            universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it) },
        )
        assertTrue(GameEngine.buyCollector(opened, catalogue.id, BuyAmount.ONE).ownedOf(catalogue.id) > 0)
    }

    @Test
    fun `every collector becomes visible before it becomes affordable`() {
        // Visibility keys off lifetime mass and affordability off the purse, so a collector that
        // only appeared once it was already payable would pop into the shop pre-bought.
        //
        // With the catalogue ladder open, because that is the state in which every machine in the
        // game is on sale — the gate is tested on its own in the test above, and mixing the two
        // questions here would only ask the gate twice.
        var state = GameState.new(0).copy(
            totalMass = 0.0,
            mass = 0.0,
            bigBangs = Multiverse.SLOTS,
            universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it) },
        )
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
            assertEquals(
                Upgrades.MARKS_PER_COLLECTOR,
                marks,
                "${collector.name} hat $marks Mk-Stufen",
            )
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
