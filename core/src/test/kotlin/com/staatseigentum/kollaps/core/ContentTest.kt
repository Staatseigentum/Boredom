package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContentTest {

    @Test
    fun `tier ladder starts at zero and climbs to the black hole`() {
        assertEquals(0.0, Tiers.first.threshold)
        assertEquals("Meteorit", Tiers.first.name)
        assertEquals("Schwarzes Loch", Tiers.last.name)
        assertEquals(BodyKind.SINGULARITY, Tiers.last.kind)
        assertTrue(Tiers.last.isFinal)
    }

    @Test
    fun `tier thresholds and multipliers grow strictly`() {
        Tiers.all.zipWithNext { a, b ->
            assertTrue(b.threshold > a.threshold, "${b.name} liegt nicht über ${a.name}")
            assertTrue(
                b.productionMultiplier > a.productionMultiplier,
                "${b.name} bringt keinen Fortschritt gegenüber ${a.name}",
            )
            assertEquals(a.index + 1, b.index)
        }
    }

    @Test
    fun `tier lookup picks the highest reached tier`() {
        assertEquals(Tiers.first, Tiers.forMass(0.0))
        assertEquals(Tiers.first, Tiers.forMass(Tiers.all[1].threshold - 1))
        assertEquals(Tiers.all[1], Tiers.forMass(Tiers.all[1].threshold))
        assertEquals(Tiers.last, Tiers.forMass(Double.MAX_VALUE))
        assertEquals(null, Tiers.next(Tiers.last))
    }

    @Test
    fun `collector ids are unique and prices as well as rates increase`() {
        assertEquals(Collectors.all.size, Collectors.all.map { it.id }.toSet().size)
        Collectors.all.zipWithNext { a, b ->
            assertTrue(b.baseCost > a.baseCost, "${b.name} ist nicht teurer als ${a.name}")
            assertTrue(b.baseRate > a.baseRate, "${b.name} produziert nicht mehr als ${a.name}")
        }
    }

    @Test
    fun `collector efficiency degrades gently along the ladder`() {
        // Like in Cookie Clicker later buildings buy less rate per kg up front — they only pay
        // off because the cheap ones inflate away. The step between two of them still has to
        // stay within a sane band, otherwise a typo in the price table goes unnoticed.
        Collectors.all.zipWithNext { a, b ->
            val ratio = (b.baseRate / b.baseCost) / (a.baseRate / a.baseCost)
            assertTrue(
                ratio in 0.25..2.0,
                "Effizienzsprung von ${a.name} zu ${b.name} ist $ratio",
            )
        }
    }

    @Test
    fun `upgrade ids are unique and reference existing content`() {
        assertEquals(Upgrades.all.size, Upgrades.all.map { it.id }.toSet().size)
        for (upgrade in Upgrades.all) {
            assertTrue(upgrade.cost > 0.0, "${upgrade.name} kostet nichts")
            assertTrue(upgrade.name.isNotBlank())
            assertTrue(upgrade.flavor.isNotBlank())
            when (val effect = upgrade.effect) {
                is UpgradeEffect.CollectorMultiplier ->
                    assertNotNull(
                        Collectors.byId(effect.collectorId),
                        "${upgrade.id} verweist auf unbekannten Kollektor",
                    )

                else -> Unit
            }
            when (val unlock = upgrade.unlock) {
                is UnlockCondition.CollectorsOwned ->
                    assertNotNull(
                        Collectors.byId(unlock.collectorId),
                        "${upgrade.id} wird von unbekanntem Kollektor freigeschaltet",
                    )

                is UnlockCondition.TierReached ->
                    assertTrue(unlock.tierIndex in Tiers.all.indices)

                else -> Unit
            }
        }
    }

    @Test
    fun `every upgrade renders a non empty effect text`() {
        for (upgrade in Upgrades.all) {
            assertTrue(upgrade.effectText.isNotBlank(), "${upgrade.id} hat keinen Effekttext")
        }
    }

    @Test
    fun `every collector has upgrades attached to it`() {
        for (collector in Collectors.all) {
            val count = Upgrades.all.count {
                (it.effect as? UpgradeEffect.CollectorMultiplier)?.collectorId == collector.id
            }
            assertTrue(count >= 4, "${collector.name} hat nur $count Upgrades")
        }
    }
}
