package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutomationTest {

    private val now = 1_700_000_000_000L

    /** The research project that unlocks the automatic buyer, and with it every rule. */
    private val unlockProject =
        ResearchTree.all.first { it.effect == PrestigeEffect.AutoBuy }.id

    private fun automated(mass: Double = 1e9): GameState = GameState(
        mass = mass,
        research = setOf(unlockProject),
        bestTier = Tiers.indexOf(ResearchTree.UNLOCK_TIER),
    )

    private fun on(state: GameState, rule: AutomationRule, option: Int = 0): GameState =
        Automation.set(state, rule, option)

    @Test
    fun `nothing is automatic until it is switched on`() {
        val idle = automated().copy(collectors = mapOf("dust" to 1))
        assertTrue(Automation.isUnlocked(idle))
        assertFalse(Automation.isOn(idle, AutomationRule.COLLECTORS))

        val ticked = GameEngine.tick(idle, 1.0)
        assertEquals(idle.collectors, ticked.collectors, "Es wurde ungefragt gekauft")
    }

    @Test
    fun `no rule runs before the buyer is unlocked`() {
        val locked = on(GameState(mass = 1e9), AutomationRule.COLLECTORS, 0)
        assertFalse(Automation.isUnlocked(locked))

        assertTrue(GameEngine.tick(locked, 1.0).collectors.isEmpty())
    }

    // ------------------------------------------------------------------ the old switch

    @Test
    fun `an old save keeps buying at the reserve it always used`() {
        val legacy = automated().copy(autoBuyOn = true)

        assertTrue(Automation.isOn(legacy, AutomationRule.COLLECTORS))
        assertEquals(
            GameEngine.AUTO_BUY_RESERVE,
            Automation.valueOf(legacy, AutomationRule.COLLECTORS),
        )
        assertTrue(GameEngine.tick(legacy, 1.0).collectors.isNotEmpty())
    }

    @Test
    fun `switching the old toggle off really switches it off`() {
        val legacy = automated().copy(autoBuyOn = true)
        val off = GameEngine.setAutoBuy(legacy, false)

        assertFalse(Automation.isOn(off, AutomationRule.COLLECTORS))
        assertFalse(off.autoBuyOn, "Der alte Schalter fällt hinter der Regel wieder durch")
        assertTrue(GameEngine.tick(off, 1.0).collectors.isEmpty())
    }

    // ------------------------------------------------------------------ the dial

    @Test
    fun `tapping walks through every setting and then off`() {
        val rule = AutomationRule.COLLECTORS
        var state = automated()

        for (expected in rule.options.indices) {
            state = Automation.cycle(state, rule)
            assertEquals(expected, Automation.settingOf(state, rule))
        }

        state = Automation.cycle(state, rule)
        assertNull(Automation.settingOf(state, rule), "Nach der letzten Stufe bleibt sie an")
    }

    @Test
    fun `a rule that is not available yet cannot be switched on`() {
        val noLab = GameState(mass = 1e9, prestigeUpgrades = autoBuyPrestige())
        assertTrue(Automation.isUnlocked(noLab))
        assertFalse(Automation.isAvailable(noLab, AutomationRule.RESEARCH))

        val tried = GameEngine.cycleAutomation(noLab, AutomationRule.RESEARCH.id)
        assertEquals(noLab, tried)
    }

    /** Any prestige upgrade that unlocks the buyer, so the test does not name one by id. */
    private fun autoBuyPrestige(): Set<String> =
        PrestigeUpgrades.all.filter { it.effect == PrestigeEffect.AutoBuy }.map { it.id }.toSet()

    // ------------------------------------------------------------------ what each rule does

    @Test
    fun `the collector rule keeps the reserve it is set to`() {
        val cheapest = Collectors.all.first()
        // Exactly enough for a copy, and nowhere near the fiftyfold reserve.
        val tight = on(automated(mass = cheapest.baseCost * 3), AutomationRule.COLLECTORS, 3)
        assertTrue(GameEngine.tick(tight, 1.0).collectors.isEmpty(), "Die Rücklage wurde ignoriert")

        val loose = on(tight, AutomationRule.COLLECTORS, 0)
        assertTrue(GameEngine.tick(loose, 1.0).collectors.isNotEmpty())
    }

    @Test
    fun `the upgrade rule only takes what is small against the pile`() {
        // Upgrades unlock off collectors owned and mass collected, so an empty run has an empty
        // shop and the rule would have nothing to prove itself against.
        val played = automated(mass = 1e12).copy(
            runMass = 1e7,
            collectors = mapOf("dust" to 25, "net" to 10),
            taps = 500,
        )
        val cheapest = GameEngine.upgradeOffers(played).first().upgrade
        val state = played.copy(mass = cheapest.cost * 20)

        // A hundredth of twenty times the price is not enough.
        val strict = on(state, AutomationRule.UPGRADES, 2)
        assertTrue(GameEngine.tick(strict, 1.0).upgrades.isEmpty())

        val relaxed = on(state, AutomationRule.UPGRADES, 0)
        assertTrue(GameEngine.tick(relaxed, 1.0).upgrades.isNotEmpty())
    }

    @Test
    fun `the fusion rule builds the cheapest stage first`() {
        val state = on(
            automated(mass = 1e18).copy(runMass = Tiers.byName(Fusion.UNLOCK_TIER).threshold),
            AutomationRule.FUSION,
            0,
        )
        assertTrue(Automation.isAvailable(state, AutomationRule.FUSION))

        val after = GameEngine.tick(state, 1.0)
        assertEquals(
            mapOf(Fusion.stages.first().id to 1),
            after.fusers,
            "Es wurde nicht unten angefangen",
        )
    }

    @Test
    fun `the research rule fills the bench`() {
        val cheapest = ResearchTree.all.filter { it.requires.isEmpty() }.minBy { it.cost }
        val dearest = ResearchTree.all.filter { it.requires.isEmpty() }.maxBy { it.cost }
        val state = automated(mass = dearest.cost * 2)

        val thrifty = GameEngine.onWallClock(on(state, AutomationRule.RESEARCH, 0), now)
        assertEquals(cheapest.id, thrifty.activeResearch)

        val greedy = GameEngine.onWallClock(on(state, AutomationRule.RESEARCH, 1), now)
        assertEquals(dearest.id, greedy.activeResearch)
    }

    @Test
    fun `the research rule leaves a running project alone`() {
        val state = on(automated(mass = 1e12), AutomationRule.RESEARCH, 0)
        val started = GameEngine.onWallClock(state, now)
        assertNotNull(started.activeResearch)

        val again = GameEngine.onWallClock(started, now + 1_000)
        assertEquals(started.activeResearch, again.activeResearch)
        assertEquals(started.researchDoneAt, again.researchDoneAt)
    }

    @Test
    fun `the collapse rule waits for the payout it was told to wait for`() {
        val ready = automated().copy(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            collapses = 1,
        )
        assertTrue(GameEngine.canCollapse(ready))
        assertTrue(GameEngine.pendingSingularities(ready) < 500.0)

        val patient = on(ready, AutomationRule.COLLAPSE, 3)
        assertEquals(0, GameEngine.onWallClock(patient, now).collapses - 1)

        val eager = on(ready, AutomationRule.COLLAPSE, 0)
        assertEquals(2, GameEngine.onWallClock(eager, now).collapses)
    }

    @Test
    fun `the collapse rule never interrupts a challenge`() {
        val challenge = Challenge.entries.first()
        val running = automated().copy(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            collapses = 1,
            activeChallenge = challenge.id,
        )
        val state = on(running, AutomationRule.COLLAPSE, 0)

        assertEquals(1, GameEngine.onWallClock(state, now).collapses)
        assertEquals(challenge.id, GameEngine.onWallClock(state, now).activeChallenge)
    }

    // ------------------------------------------------------------------ persistence

    @Test
    fun `rules survive every reset`() {
        val ready = on(
            automated().copy(runMass = Tiers.last.threshold, bestTier = Tiers.last.index),
            AutomationRule.UPGRADES,
            1,
        )
        val expected = ready.automation

        assertEquals(expected, GameEngine.collapse(ready, now).automation)

        val banging = ready.copy(collapses = BigBang.REQUIRED_COLLAPSES)
        assertEquals(expected, GameEngine.bigBang(banging, now).automation)
    }

    @Test
    fun `a save keeps the rules`() {
        val state = on(automated(), AutomationRule.COLLECTORS, 2)
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(2, Automation.settingOf(back, AutomationRule.COLLECTORS))
    }

    @Test
    fun `a save drops rules that no longer exist`() {
        val state = automated().copy(
            automation = mapOf(AutomationRule.COLLECTORS.id to 1, "au_teleport" to 0),
        )
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(mapOf(AutomationRule.COLLECTORS.id to 1), back.automation)
    }

    @Test
    fun `a setting out of range is read as the nearest one`() {
        val state = automated().copy(automation = mapOf(AutomationRule.COLLECTORS.id to 99))
        assertEquals(
            AutomationRule.COLLECTORS.options.lastIndex,
            Automation.settingOf(state, AutomationRule.COLLECTORS),
        )
    }

    @Test
    fun `every rule offers something to choose between`() {
        for (rule in AutomationRule.entries) {
            assertTrue(rule.options.size >= 2, "${rule.label} hat nichts einzustellen")
            assertTrue(rule.label.isNotBlank() && rule.flavor.isNotBlank())
            assertTrue(rule.options.all { it.label.isNotBlank() })
        }
        assertEquals(
            AutomationRule.entries.size,
            AutomationRule.entries.map { it.id }.toSet().size,
            "Doppelte Regel-ID",
        )
    }
}
