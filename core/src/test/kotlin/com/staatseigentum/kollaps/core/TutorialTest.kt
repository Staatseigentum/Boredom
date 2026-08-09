package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TutorialTest {

    @Test
    fun `a fresh save is told to tap`() {
        val step = Tutorial.current(GameState.new(0))
        assertNotNull(step)
        assertEquals("tut_tap", step.id)
    }

    @Test
    fun `each step gives way to the next as it is met`() {
        var state = GameState.new(0)
        val seen = mutableListOf<String>()

        for (step in Tutorial.steps) {
            val current = Tutorial.current(state)
            assertNotNull(current, "Nach ${seen.lastOrNull()} kam nichts mehr")
            assertEquals(step.id, current.id, "Die Reihenfolge stimmt nicht")
            seen += current.id
            state = satisfy(state, step)
        }
        assertNull(Tutorial.current(state), "Nach dem letzten Schritt kommt noch etwas")
    }

    /** Puts the state into the shape the step asks for, without going through the engine. */
    private fun satisfy(state: GameState, step: TutorialStep): GameState = when (step.id) {
        "tut_tap" -> state.copy(taps = 10)
        "tut_collector" -> state.copy(collectors = mapOf("dust" to 1))
        "tut_more" -> state.copy(collectors = mapOf("dust" to 10))
        "tut_upgrade" -> state.copy(upgrades = setOf(Upgrades.all.first().id))
        "tut_tier" -> state.copy(bestTier = 1)
        "tut_cosmos" -> state.copy(bestTier = 3)
        else -> error("Unbekannter Schritt ${step.id}")
    }

    /**
     * The reason the conditions read the state rather than counting button presses.
     *
     * A player who buys ten collectors before reading anything has done the first three steps and
     * must not be walked back through them.
     */
    @Test
    fun `doing the thing first skips the step`() {
        val ahead = GameState.new(0).copy(taps = 50, collectors = mapOf("dust" to 12))
        val step = Tutorial.current(ahead)

        assertNotNull(step)
        assertEquals("tut_upgrade", step.id)
    }

    @Test
    fun `sending it away silences it for good`() {
        val fresh = GameState.new(0)
        assertNotNull(Tutorial.current(fresh))

        val dismissed = GameEngine.dismissTutorial(fresh)
        assertNull(Tutorial.current(dismissed))
        assertFalse(Tutorial.appliesTo(dismissed))
    }

    @Test
    fun `a veteran is never shown a word of it`() {
        val veteran = GameState(collapses = 3, bestTier = 12, taps = 5_000)
        assertFalse(Tutorial.appliesTo(veteran))
    }

    @Test
    fun `the dismissal survives every reset`() {
        val dismissed = GameEngine.dismissTutorial(
            GameState(collapses = 40, singularities = 1e9, runMass = Tiers.last.threshold),
        )
        assertTrue(GameEngine.collapse(dismissed, 1).tutorialDone)
        assertTrue(GameEngine.bigBang(dismissed, 1).tutorialDone)
    }

    @Test
    fun `the counter matches the step being shown`() {
        var state = GameState.new(0)
        for ((index, step) in Tutorial.steps.withIndex()) {
            assertEquals(index + 1, Tutorial.position(state), step.id)
            state = satisfy(state, step)
        }
        assertEquals(Tutorial.total, Tutorial.position(state))
    }

    @Test
    fun `every step says something and no two share an id`() {
        val ids = Tutorial.steps.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Doppelte Schritt-Kennung")
        for (step in Tutorial.steps) {
            assertTrue(step.title.isNotBlank(), step.id)
            assertTrue(step.text.length > 30, "${step.id}: zu wenig Text")
        }
    }

    /** A step nothing can satisfy would leave the nudge on screen for good. */
    @Test
    fun `every step is reachable by playing`() {
        val far = GameState(
            taps = 100_000,
            collectors = mapOf("dust" to 500),
            upgrades = Upgrades.all.take(5).map { it.id }.toSet(),
            bestTier = Tiers.last.index,
        )
        assertTrue(Tutorial.steps.all { it.isDone(far) }, "Ein Schritt ist nie zu erfüllen")
    }
}
