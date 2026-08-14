package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The four rules added for the long road to a full sky. */
class NewChallengeRuleTest {

    private val now = 1_700_000_000_000L

    /** Deep enough that every challenge in the catalogue is on offer, with a sky behind them. */
    private fun veteran(): GameState = GameState.new(now).copy(
        collapses = Challenge.entries.maxOf { it.requiredCollapses },
        singularities = 500.0,
        bestTier = Tiers.indexOf("Schwarzes Loch"),
        collectors = mapOf("dust" to 200, "net" to 100),
        universes = (0 until Multiverse.SLOTS).map {
            ParkedUniverse(slot = it, pathId = Path.MASCHINE.id, bestTier = 20, collapses = 10)
        },
        bigBangs = Multiverse.SLOTS,
    )

    private fun running(id: String): GameState =
        GameEngine.startChallenge(veteran(), id, now).copy(
            collectors = mapOf("dust" to 200, "net" to 100),
            runMass = 1e9,
            mass = 1e9,
        )

    @Test
    fun `the milestone rule takes the serial bonus away and nothing else`() {
        val plain = veteran().copy(runMass = 1e9, mass = 1e9)
        val stopped = running("c_nomiles")

        assertTrue(Milestones.reached(200) > 0, "Der Testaufbau hat gar keine Meilensteine")
        assertTrue(
            GameEngine.massPerSecond(stopped) < GameEngine.massPerSecond(plain),
            "Serienstopp kostet nichts",
        )
        // The fleet still works, and so does the finger. Only the bonus for numbers is off.
        assertTrue(GameEngine.massPerSecond(stopped) > 0.0)
        assertTrue(GameEngine.massPerTap(stopped) > 0.0)
    }

    @Test
    fun `the offline rule pays nothing for a closed app`() {
        val watching = running("c_nooffline")
        val away = GameEngine.applyOffline(watching.copy(lastSeenAt = now - 6 * 3_600_000L), now)

        assertEquals(0.0, away.gained, "Wachdienst hat für die Nacht bezahlt")
        // And the run is otherwise untouched: production while somebody is looking is normal.
        assertTrue(GameEngine.massPerSecond(watching) > 0.0)
    }

    @Test
    fun `the fusion rule leaves the chain cold`() {
        val cold = running("c_nofusion")
        assertEquals(0.0, GameEngine.fusionRate(cold), "Die Öfen brennen weiter")
        assertEquals(
            Fusion.amountOf(cold, Element.entries.first()),
            Fusion.amountOf(GameEngine.tick(cold, 600.0), Element.entries.first()),
            "In zehn Minuten ist trotzdem etwas fusioniert",
        )
    }

    @Test
    fun `the sky rule silences the galaxies`() {
        val alone = running("c_nosky")
        val withSky = veteran().copy(runMass = 1e9, mass = 1e9)

        assertTrue(Multiverse.multiplier(withSky) > 1.0, "Der Testaufbau hat gar keinen Himmel")
        assertTrue(
            GameEngine.massPerSecond(alone) < GameEngine.massPerSecond(withSky),
            "Einsames Universum merkt den Himmel nicht",
        )
        // The galaxies still exist and still earn Äonen — the rule takes their help away from this
        // run, it does not unpark them.
        assertEquals(Multiverse.SLOTS, alone.universes.size)
        assertTrue(Multiverse.aeonsPerSecond(alone) > 0.0, "Die Herausforderung hat den Himmel abgeschaltet")
    }

    @Test
    fun `every challenge names a rule the engine actually reads`() {
        // A rule the modifier pass does not handle is a card that promises something and does
        // nothing. Every one of them has to change *some* number.
        //
        // The fixture carries bought upgrades and satellites as well as a fleet, because two of
        // the rules only bite on things a bare run does not have. `NoUpgrades` was the one that
        // caught this: a challenge run starts with the shop wiped, so switching the shop off
        // changed nothing measurable and the rule looked broken when it was the test that was.
        val furnished = { state: GameState ->
            state.copy(
                collectors = mapOf("dust" to 200, "net" to 100),
                upgrades = Upgrades.all.filter { it.cost < 1e6 }.map { it.id }.toSet(),
                orbits = 3,
                satellites = mapOf(0 to 1e8, 1 to 1e8, 2 to 1e8),
                runMass = 1e9,
                mass = 1e9,
            )
        }
        val base = furnished(veteran())
        val reference = GameEngine.massPerSecond(base)

        for (challenge in Challenge.entries) {
            if (challenge.rule == ChallengeRule.Handicap(1.0)) continue // the timed ones, by design
            val under = furnished(GameEngine.startChallenge(veteran(), challenge.id, now))
            val changed = GameEngine.massPerSecond(under) != reference ||
                GameEngine.massPerTap(under) != GameEngine.massPerTap(base) ||
                GameEngine.fusionRate(under) != GameEngine.fusionRate(base) ||
                GameEngine.applyOffline(under.copy(lastSeenAt = now - 3_600_000L), now).gained == 0.0
            assertTrue(changed, "${challenge.title} ändert nichts")
        }
    }

    @Test
    fun `every rule says what it does`() {
        for (challenge in Challenge.entries) {
            assertTrue(challenge.ruleText.isNotBlank(), "${challenge.title} hat keinen Regeltext")
            assertTrue(challenge.goalText.isNotBlank(), "${challenge.title} hat keinen Zieltext")
        }
    }
}
