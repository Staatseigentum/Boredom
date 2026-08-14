package com.staatseigentum.kollaps.core

import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rule that ends runs, and the two promises that make it allowed to exist.
 *
 * It must not fire early — a collapse the moment it becomes possible is worth one singularity and
 * throws the run away — and it must stop by itself after exactly the number of runs that were
 * ordered. Everything else about it is a detail; these two are the feature.
 */
class AutoCollapseTest {

    private val now = 1_700_000_000_000L

    private val unlockProject =
        ResearchTree.all.first { it.effect == PrestigeEffect.AutoBuy }.id

    /** A save that has collapsed by hand once, so the rule is allowed at all. */
    private fun ready(runMass: Double, running: Boolean = false): GameState = GameState(
        research = setOf(unlockProject),
        bestTier = Tiers.indexOf(ResearchTree.UNLOCK_TIER),
        collapses = 1,
        runMass = runMass,
        // Production has to be read off a fleet, because that is what the look-ahead reads — and it
        // has to be on the scale of the black hole, or a minute of it rounds away against a run
        // that is already there. Sized from the ladder rather than typed in, so it stays true.
        collectors = if (running) mapOf(fastest.id to fleetForATenthPerMinute) else emptyMap(),
    )

    private val fastest = Collectors.all.maxBy { it.baseRate }

    /** Enough of them that a minute adds about a tenth of the black hole's threshold. */
    private val fleetForATenthPerMinute =
        ceil(Tiers.last.threshold / 10.0 / 60.0 / fastest.baseRate).toInt()

    private fun armed(state: GameState, option: Int = 0) =
        Automation.set(state, AutomationRule.COLLAPSE, option)

    @Test
    fun `nothing happens before the player has collapsed once by hand`() {
        val fresh = GameState(research = setOf(unlockProject), collapses = 0)
        assertFalse(Automation.isAvailable(fresh, AutomationRule.COLLAPSE))

        val armed = armed(fresh.copy(runMass = Tiers.last.threshold * 100.0))
        assertEquals(0, GameEngine.onWallClock(armed, now).collapses, "Der erste Kollaps wurde geklaut")
    }

    @Test
    fun `choosing a count places the order`() {
        val state = armed(ready(0.0), option = 1)
        assertEquals(
            AutomationRule.COLLAPSE.optionAt(1).value.toInt(),
            state.collapseBudget,
        )

        // And picking a different count replaces it rather than adding to it.
        val bigger = armed(state.copy(collapseBudget = 2), option = 3)
        assertEquals(AutomationRule.COLLAPSE.optionAt(3).value.toInt(), bigger.collapseBudget)

        // Switching it off clears it.
        assertEquals(0, Automation.set(bigger, AutomationRule.COLLAPSE, null).collapseBudget)
    }

    @Test
    fun `it waits while the run is still going somewhere`() {
        // Barely over the line and producing hard: another minute is worth a great deal here.
        val climbing = armed(ready(Tiers.last.threshold * 1.05, running = true))
        assertTrue(GameEngine.canCollapse(climbing), "Der Aufbau taugt nicht für den Test")
        assertTrue(
            GameEngine.singularitiesIn(climbing, 60.0) > GameEngine.pendingSingularities(climbing),
            "Eine Minute bringt hier nichts mehr — der Test misst das Falsche",
        )

        assertEquals(1, GameEngine.onWallClock(climbing, now).collapses, "Zu früh kollabiert")
    }

    @Test
    fun `it fires once the run has stopped paying`() {
        // Deep, and nothing left running: a further minute adds exactly nothing.
        val stalled = armed(ready(Tiers.last.threshold * 1e6))
        assertTrue(GameEngine.canCollapse(stalled))

        val after = GameEngine.onWallClock(stalled, now)
        assertEquals(2, after.collapses, "Es wurde nicht kollabiert")
        assertTrue(after.singularities > 0.0)
        assertEquals(stalled.collapseBudget - 1, after.collapseBudget)
    }

    @Test
    fun `it switches itself off after exactly the number of runs ordered`() {
        val ordered = AutomationRule.COLLAPSE.optionAt(0).value.toInt()
        var state = armed(ready(Tiers.last.threshold * 1e6), option = 0)
        assertEquals(ordered, state.collapseBudget)

        repeat(ordered) {
            // Each run is put back where it was, because the collapse resets the mass.
            state = GameEngine.onWallClock(state.copy(runMass = Tiers.last.threshold * 1e6), now)
        }

        assertEquals(1 + ordered, state.collapses, "Es wurden nicht genau $ordered Läufe kollabiert")
        assertEquals(0, state.collapseBudget)
        assertFalse(Automation.isOn(state, AutomationRule.COLLAPSE), "Die Regel läuft weiter")

        // And it stays off: one more pass must not end another run.
        val later = GameEngine.onWallClock(state.copy(runMass = Tiers.last.threshold * 1e6), now)
        assertEquals(1 + ordered, later.collapses, "Die Regel hat nach dem Auslaufen weitergemacht")
    }

    @Test
    fun `a challenge is never cut short by it`() {
        val running = GameEngine.startChallenge(
            armed(ready(Tiers.last.threshold * 1e6).copy(collapses = 5)),
            "c_hand",
            now,
        )
        assertTrue(running.runningChallengeIds.isNotEmpty(), "Die Herausforderung läuft gar nicht")

        val after = GameEngine.onWallClock(running, now)
        assertEquals(running.collapses, after.collapses, "Die Herausforderung wurde abgewürgt")
        assertEquals(running.collapseBudget, after.collapseBudget, "Der Auftrag wurde trotzdem verbraucht")
    }

    @Test
    fun `an old save carrying the removed rule is not switched on by it`() {
        // The id the rule used before it was taken out. Nothing may answer to it.
        val legacy = ready(Tiers.last.threshold * 1e6).copy(automation = mapOf("au_collapse" to 3))
        assertFalse(Automation.isOn(legacy, AutomationRule.COLLAPSE))
        assertEquals(1, GameEngine.onWallClock(legacy, now).collapses)
    }
}
