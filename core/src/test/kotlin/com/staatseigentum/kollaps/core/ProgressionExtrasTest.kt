package com.staatseigentum.kollaps.core

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Comets, the prestige shop and achievements — everything added on top of the plain ladder. */
class ProgressionExtrasTest {

    /** A state with real production, so the effects have something to act on. */
    private fun producing(): GameState = GameState.new(NOW).copy(
        mass = 1e9,
        runMass = 1e9,
        collectors = mapOf("dust" to 50, "net" to 40, "drone" to 20),
    )

    // ------------------------------------------------------------------ comets

    @Test
    fun `a windfall pays what the collectors would earn in that time`() {
        val before = producing()
        val perSecond = GameEngine.massPerSecond(before)
        val after = GameEngine.catchComet(before, Comet.WINDFALL)

        val reward = Comet.WINDFALL.reward as CometReward.Windfall
        val expected = perSecond * reward.secondsOfProduction
        assertTrue(
            abs((after.mass - before.mass) - expected) < expected * 1e-6,
            "Erwartet ${expected}, bekommen ${after.mass - before.mass}",
        )
        assertEquals(1, after.cometsCaught.toInt())
    }

    /** The same state with the buff taken off, so a comparison measures the buff and nothing else. */
    private fun GameState.withoutBuff(): GameState = copy(buffId = null, buffSecondsLeft = 0.0)

    @Test
    fun `a surge multiplies production and a frenzy does not`() {
        val plain = producing()

        // Compared against itself minus the buff: catching a comet also earns the "first comet"
        // achievement, and that is worth a per cent of its own.
        val surged = GameEngine.catchComet(plain, Comet.SURGE)
        assertEquals(Buff.SURGE, surged.buff)
        val surgeBase = GameEngine.massPerSecond(surged.withoutBuff())
        assertEquals(
            surgeBase * Buff.SURGE.factor,
            GameEngine.massPerSecond(surged),
            absoluteTolerance = surgeBase * 1e-6,
            message = "Der Schub greift nicht auf die Produktion",
        )

        val frenzied = GameEngine.catchComet(plain, Comet.FRENZY)
        val frenzyBase = GameEngine.massPerSecond(frenzied.withoutBuff())
        assertEquals(
            frenzyBase,
            GameEngine.massPerSecond(frenzied),
            absoluteTolerance = frenzyBase * 1e-6,
            message = "Der Klickrausch fasst die Produktion an, soll er aber nicht",
        )
        assertTrue(
            GameEngine.massPerTap(frenzied) > GameEngine.massPerTap(frenzied.withoutBuff()) * 50,
            "Der Klickrausch greift nicht auf das Tippen",
        )
    }

    @Test
    fun `a buff runs out on the tick and not on the wall clock`() {
        val buffed = GameEngine.catchComet(producing(), Comet.SURGE)
        assertEquals(Buff.SURGE.seconds, buffed.buffSecondsLeft)

        // Putting the phone away must not burn it: only ticking counts down.
        val touched = GameEngine.touch(buffed, NOW + 60_000)
        assertEquals(Buff.SURGE, touched.buff)

        val halfway = GameEngine.tick(buffed, Buff.SURGE.seconds / 2)
        assertEquals(Buff.SURGE, halfway.buff)

        val expired = GameEngine.tick(halfway, Buff.SURGE.seconds)
        assertNull(expired.buff, "Der Buff läuft nicht ab")
        assertNull(expired.buffId)
    }

    @Test
    fun `catching a second comet restarts the buff instead of stacking it`() {
        val once = GameEngine.catchComet(producing(), Comet.SURGE)
        val spent = GameEngine.tick(once, 20.0)
        val twice = GameEngine.catchComet(spent, Comet.SURGE)
        assertEquals(Buff.SURGE.seconds, twice.buffSecondsLeft)
    }

    @Test
    fun `the schedule keeps comets inside their window and picks every kind`() {
        val random = Random(7)
        repeat(200) {
            val delay = Comets.nextDelay(random)
            assertTrue(
                delay in Comets.MIN_SECONDS..Comets.MAX_SECONDS,
                "Wartezeit $delay liegt außerhalb des Fensters",
            )
        }
        val seen = (1..400).map { Comets.pick(random) }.toSet()
        assertEquals(Comet.entries.toSet(), seen, "Nicht jede Kometenart kommt vor")

        // Prestige is supposed to shorten the wait, not lengthen it.
        val quick = Comets.nextDelay(Random(1), frequency = 2.0)
        assertTrue(quick <= Comets.MAX_SECONDS / 2.0)
    }

    @Test
    fun `comets stay away until there is production worth multiplying`() {
        assertTrue(!Comets.appearsAt(GameState.new(NOW)), "Komet direkt beim Meteoriten")
        val later = GameState.new(NOW).copy(runMass = Tiers.all[Comets.FIRST_TIER].threshold)
        assertTrue(Comets.appearsAt(later))
    }

    // ------------------------------------------------------------------ prestige

    @Test
    fun `a prestige upgrade costs singularities and is only sold once`() {
        val upgrade = PrestigeUpgrades.byId("p_offline_1")!!
        val rich = GameState.new(NOW).copy(singularities = upgrade.cost)

        val bought = GameEngine.buyPrestigeUpgrade(rich, upgrade.id)
        assertTrue(bought.ownsPrestige(upgrade.id))
        assertEquals(0.0, bought.singularities)

        assertSame(bought, GameEngine.buyPrestigeUpgrade(bought, upgrade.id).let { bought })
        val again = GameEngine.buyPrestigeUpgrade(bought, upgrade.id)
        assertEquals(0.0, again.singularities, "Ein zweiter Kauf hat nochmal abgebucht")
    }

    @Test
    fun `a prestige upgrade the player cannot afford changes nothing`() {
        val poor = GameState.new(NOW).copy(singularities = 1.0)
        val after = GameEngine.buyPrestigeUpgrade(poor, "p_global_2")
        assertTrue(after.prestigeUpgrades.isEmpty())
        assertEquals(1.0, after.singularities)
    }

    @Test
    fun `upgrades gated behind collapses are neither offered nor sold`() {
        val fresh = GameState.new(NOW).copy(singularities = 1_000.0)
        val gated = PrestigeUpgrades.all.first { it.requiredCollapses > 0 }

        assertTrue(PrestigeUpgrades.offered(fresh).none { it.id == gated.id })
        assertTrue(GameEngine.buyPrestigeUpgrade(fresh, gated.id).prestigeUpgrades.isEmpty())

        val seasoned = fresh.copy(collapses = gated.requiredCollapses)
        assertTrue(PrestigeUpgrades.offered(seasoned).any { it.id == gated.id })
        assertTrue(GameEngine.buyPrestigeUpgrade(seasoned, gated.id).ownsPrestige(gated.id))
    }

    @Test
    fun `collapsing keeps what prestige is for and clears the rest`() {
        val ready = GameState.new(NOW).copy(
            mass = Tiers.last.threshold,
            runMass = Tiers.last.threshold,
            totalMass = Tiers.last.threshold,
            collectors = mapOf("dust" to 100),
            upgrades = setOf("tap_1", "tap_2"),
            prestigeUpgrades = setOf("p_offline_1", "p_start_mass"),
            achievements = setOf("a_taps_100"),
            cometsCaught = 9,
            playedSeconds = 1_234.0,
            soundOn = false,
        )

        val after = GameEngine.collapse(ready, NOW)

        assertTrue(after.singularities > 0, "Kein Ertrag aus dem Kollaps")
        assertEquals(setOf("p_offline_1", "p_start_mass"), after.prestigeUpgrades)
        assertTrue("a_taps_100" in after.achievements)
        assertEquals(9, after.cometsCaught.toInt())
        assertEquals(1_234.0, after.playedSeconds)
        assertTrue(!after.soundOn, "Die Toneinstellung hat den Kollaps nicht überlebt")

        assertEquals(emptySet(), after.upgrades, "Lauf-Upgrades bleiben nach dem Kollaps")
        assertEquals(0.0, after.runMass)
    }

    @Test
    fun `starting collectors and mass come out of prestige`() {
        val base = GameState.new(NOW).copy(
            mass = Tiers.last.threshold,
            runMass = Tiers.last.threshold,
        )
        val plain = GameEngine.collapse(base, NOW)
        assertEquals(0.0, plain.mass)
        assertTrue(plain.collectors.values.all { it == 0 })

        val prepared = GameEngine.collapse(
            base.copy(prestigeUpgrades = setOf("p_start_mass", "p_collectors_1")),
            NOW,
        )
        assertEquals(50_000.0, prepared.mass)
        assertTrue(
            Collectors.all.all { prepared.ownedOf(it.id) == 5 },
            "Die Startkollektoren fehlen",
        )
    }

    @Test
    fun `the singularity upgrade pays out more for the same run`() {
        val ready = GameState.new(NOW).copy(
            runMass = Tiers.last.threshold * 4,
        )
        val plain = GameEngine.pendingSingularities(ready)
        val boosted = GameEngine.pendingSingularities(
            ready.copy(prestigeUpgrades = setOf("p_singularity")),
        )
        assertTrue(boosted > plain, "$boosted ist nicht mehr als $plain")
    }

    // ------------------------------------------------------------------ achievements

    @Test
    fun `achievements are recorded once and are worth something`() {
        val tapped = GameState.new(NOW).copy(taps = 100)
        val awarded = GameEngine.award(tapped)
        assertTrue("a_taps_100" in awarded.achievements)

        val again = GameEngine.award(awarded)
        assertEquals(awarded.achievements, again.achievements)

        assertEquals(
            1.0 + Achievements.BONUS_EACH,
            Achievements.multiplier(awarded),
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun `an achievement raises production the moment it lands`() {
        val before = producing()
        val after = GameEngine.award(before.copy(taps = 100))
        assertTrue(
            GameEngine.massPerSecond(after) > GameEngine.massPerSecond(before),
            "Der Erfolg wirkt nicht auf die Produktion",
        )
    }

    @Test
    fun `playing awards the achievements that describe playing`() {
        var state = producing()
        repeat(120) { state = GameEngine.tick(state, 60.0) }
        assertTrue("a_patient" in state.achievements, "Eine Stunde Spielzeit zählt nicht")
        assertTrue(state.playedSeconds >= 3_600)
    }

    @Test
    fun `every achievement is reachable in principle`() {
        // Not a simulation — just that nothing throws and no id is duplicated, which is the sort
        // of thing a hand written catalogue of thirty entries gets wrong quietly.
        val state = producing()
        for (achievement in Achievements.all) {
            achievement.earned(state)
            assertNotNull(Achievements.byId(achievement.id))
        }
        assertEquals(Achievements.all.size, Achievements.all.map { it.id }.toSet().size)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
