package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the two resets pay, and why staying past the gate is now a real option. */
class RewardingResetsTest {

    private val now = 1_700_000_000_000L

    /** A save with the catalogue ladder open, so overshooting is possible at all. */
    private fun deep(runMass: Double): GameState = GameState.new(now).copy(
        runMass = runMass,
        mass = runMass,
        collapses = 40,
        bestTier = Tiers.last.index,
        universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it, bestTier = 20, collapses = 10) },
        bigBangs = Multiverse.SLOTS,
    )

    @Test
    fun `collapsing exactly at the gate still pays the base`() {
        val gate = deep(Tiers.last.threshold)
        assertEquals(0, GameEngine.tierOf(gate).index - Tiers.last.index, "Am Tor gibt es keine Sprossen")
        assertTrue(GameEngine.pendingSingularities(gate) >= GameEngine.SINGULARITY_SCALE)
    }

    @Test
    fun `climbing the catalogue pays more than the mass alone would`() {
        val gate = deep(Tiers.last.threshold)
        val climbed = deep(Tiers.last.threshold * Math.pow(Designations.THRESHOLD_GROWTH, 100.0))

        val rungs = GameEngine.tierOf(climbed).index - Tiers.last.index
        assertTrue(rungs >= 99, "Der Aufbau steht nur $rungs Sprossen höher")

        // What the mass alone would have been worth, without the bonus for having climbed.
        val massOnly = GameEngine.pendingSingularities(gate) *
            Math.sqrt(climbed.runMass / gate.runMass)
        assertTrue(
            GameEngine.pendingSingularities(climbed) > massOnly * 1.5,
            "Hundert Sprossen bringen kaum mehr als die Masse allein",
        )
    }

    @Test
    fun `the overshoot bonus grows but never runs away`() {
        val rungs = listOf(1, 10, 100, 1_000, 10_000)
        val paid = rungs.map { step ->
            GameEngine.pendingSingularities(
                deep(Tiers.last.threshold * Math.pow(Designations.THRESHOLD_GROWTH, step + 0.5)),
            )
        }
        assertTrue(paid.zipWithNext().all { (a, b) -> b > a }, "Höher steigen bringt nicht mehr: $paid")
        assertTrue(paid.all { it.isFinite() }, "Die Ausschüttung entgleist: $paid")
    }

    @Test
    fun `a deeper universe is worth more aeons than a shallow one`() {
        val shallow = GameState.new(now).copy(
            collapses = BigBang.requiredFor(0),
            bestTier = Tiers.last.index,
        )
        val pushed = shallow.copy(bestTier = Tiers.last.index + 500)

        assertTrue(BigBang.pending(shallow) >= 1.0, "Der flache Urknall zahlt gar nichts")
        assertTrue(
            BigBang.pending(pushed) > BigBang.pending(shallow),
            "Fünfhundert Sprossen tiefer bringt beim Urknall nichts",
        )
        // Gentler than the collapse's, because the collapse is pressed far more often.
        assertTrue(BigBang.DEPTH_BONUS < GameEngine.OVERSHOOT_BONUS)
    }

    @Test
    fun `neither reward applies below the gate`() {
        val below = GameState.new(now).copy(runMass = Tiers.last.threshold / 2.0, collapses = 40)
        assertEquals(0.0, GameEngine.pendingSingularities(below))
        assertEquals(1.0, BigBang.depthBonus(below), "Unter dem Tor gibt es schon einen Tiefenbonus")
    }
}
