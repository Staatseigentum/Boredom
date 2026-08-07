package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The upgrades whose strength depends on what else is in the fleet. */
class SynergyTest {

    private fun withFleet(vararg fleet: Pair<String, Int>): GameState =
        GameState.new(NOW).copy(collectors = fleet.toMap())

    private fun synergy(id: String): Upgrade = assertNotNull(Upgrades.byId(id), "$id fehlt")

    @Test
    fun `a pair synergy scales with how many of the source are owned`() {
        val upgrade = synergy("syn_dust_net")
        val effect = upgrade.effect as UpgradeEffect.CollectorSynergy

        val fleet = withFleet("dust" to 100, "net" to 10)
        val without = GameEngine.massPerSecond(fleet)
        val with = GameEngine.massPerSecond(fleet.copy(upgrades = setOf(upgrade.id)))

        // Only the target gains, so the difference is the net's output times the bonus.
        val netOutput = GameEngine.collectorOutput(fleet, Collectors.byId("net")!!)
        assertEquals(without + netOutput * effect.perUnit * 100, with, with * 1e-9)
    }

    @Test
    fun `a pair synergy does nothing while the source is unowned`() {
        val fleet = withFleet("net" to 10)
        assertEquals(
            GameEngine.massPerSecond(fleet),
            GameEngine.massPerSecond(fleet.copy(upgrades = setOf("syn_dust_net"))),
        )
    }

    @Test
    fun `a fleet synergy lifts every collector, not just one`() {
        val upgrade = synergy("syn_dyson_alle")
        val effect = upgrade.effect as UpgradeEffect.FleetSynergy

        val fleet = withFleet("dust" to 40, "net" to 40, "dyson" to 50)
        val without = GameEngine.massPerSecond(fleet)
        val with = GameEngine.massPerSecond(fleet.copy(upgrades = setOf(upgrade.id)))

        assertEquals(without * (1.0 + effect.perUnit * 50), with, with * 1e-9)
    }

    @Test
    fun `synergies stack with the flat doublings instead of replacing them`() {
        // Measured on the net's own output, not on the total: the dust nets are in the total
        // too and the synergy does not touch them, so the totals would not factor.
        val net = Collectors.byId("net")!!
        val fleet = withFleet("dust" to 100, "net" to 25)
        val doubling = (Upgrades.byId("net_25")!!.effect as UpgradeEffect.CollectorMultiplier).factor
        val perUnit = (synergy("syn_dust_net").effect as UpgradeEffect.CollectorSynergy).perUnit

        val plain = GameEngine.collectorOutput(fleet, net)
        val both = GameEngine.collectorOutput(
            fleet.copy(upgrades = setOf("syn_dust_net", "net_25")),
            net,
        )

        val expected = plain * doubling * (1.0 + perUnit * 100)
        assertEquals(expected, both, expected * 1e-9)
    }

    @Test
    fun `every synergy names collectors that exist and reads as a sentence`() {
        val synergies = Upgrades.all.filter {
            it.effect is UpgradeEffect.CollectorSynergy || it.effect is UpgradeEffect.FleetSynergy
        }
        assertTrue(synergies.size >= 12, "Nur ${synergies.size} Synergien im Katalog")

        for (upgrade in synergies) {
            when (val effect = upgrade.effect) {
                is UpgradeEffect.CollectorSynergy -> {
                    assertNotNull(Collectors.byId(effect.sourceId), "${upgrade.id}: Quelle fehlt")
                    assertNotNull(Collectors.byId(effect.targetId), "${upgrade.id}: Ziel fehlt")
                    assertTrue(effect.perUnit > 0.0)
                }

                is UpgradeEffect.FleetSynergy -> {
                    assertNotNull(Collectors.byId(effect.sourceId), "${upgrade.id}: Quelle fehlt")
                    assertTrue(effect.perUnit > 0.0)
                }

                else -> Unit
            }
            // A per-unit value that rounds to "0 %" would describe itself as doing nothing.
            assertTrue(
                "0 %" !in upgrade.effectText,
                "${upgrade.id} beschreibt sich als wirkungslos: ${upgrade.effectText}",
            )
        }
    }

    @Test
    fun `a challenge without collectors switches the synergies off too`() {
        val fleet = withFleet("dust" to 100, "net" to 20)
            .copy(upgrades = setOf("syn_dust_net"), activeChallenge = "c_hand")
        assertEquals(0.0, GameEngine.massPerSecond(fleet))
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
