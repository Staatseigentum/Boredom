package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tapping the sky instead of the body. */
class EmptyTapTest {

    @Test
    fun `a miss pays nothing`() {
        val before = GameState(mass = 500.0, collectors = mapOf("dust" to 5))
        val after = GameEngine.tapEmpty(before)

        assertEquals(before.mass, after.mass, "Danebentippen hat Masse gebracht")
        assertEquals(before.runMass, after.runMass)
        assertEquals(before.totalMass, after.totalMass)
    }

    /**
     * A miss is not a tap.
     *
     * Rolling it into the tap counter would make every tapping achievement — up to fifty thousand
     * — claimable by flailing at the background, which is the opposite of what they are for.
     */
    @Test
    fun `a miss does not count as a tap`() {
        val after = GameEngine.tapEmpty(GameState())
        assertEquals(0, after.taps)
        assertEquals(1, after.missedTaps)
    }

    @Test
    fun `the first miss earns the achievement, and only once`() {
        val once = GameEngine.tapEmpty(GameState.new(0))
        assertTrue("a_leer" in once.achievements, "Der Erfolg blieb aus")

        val twice = GameEngine.tapEmpty(once)
        assertEquals(once.achievements, twice.achievements)
        assertEquals(2, twice.missedTaps)
    }

    @Test
    fun `a fresh save has not earned it`() {
        assertTrue("a_leer" !in GameState.new(0).achievements)
        assertTrue("a_leer" !in GameEngine.tap(GameState.new(0)).achievements, "Ein Treffer zählt")
    }

    @Test
    fun `the achievement says what it should`() {
        val leer = Achievements.all.first { it.id == "a_leer" }
        assertEquals("Was war dat jetze?", leer.name)
        assertEquals("Das war genau so wenig wert wie eh und je", leer.flavor)
    }

    @Test
    fun `the counter is a lifetime one and survives every reset`() {
        val missed = GameState(
            missedTaps = 7,
            collapses = 40,
            singularities = 1e9,
            runMass = Tiers.last.threshold,
        )
        assertEquals(7, GameEngine.collapse(missed, 1).missedTaps)
        assertEquals(7, GameEngine.bigBang(missed, 1).missedTaps)
    }

    /** It is worth the same as every other achievement — a tick that adds up, not a joke prize. */
    @Test
    fun `it pays the same bonus as the rest`() {
        val fleet = mapOf("dust" to 20)
        val without = GameState(collectors = fleet)
        val with = GameEngine.tapEmpty(without)

        assertTrue(GameEngine.massPerSecond(with) > GameEngine.massPerSecond(without))
    }
}
