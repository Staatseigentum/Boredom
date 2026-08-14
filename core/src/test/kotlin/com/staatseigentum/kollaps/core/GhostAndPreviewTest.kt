package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The record's shape, and the figures the collapse button shows before it is pressed. */
class GhostAndPreviewTest {

    private val now = 1_700_000_000_000L

    private fun finished(seconds: Double, curve: List<Double>): GameState = GameState.new(now).copy(
        runMass = Tiers.last.threshold * 4,
        mass = 1e26,
        runSeconds = seconds,
        history = curve,
        collectors = mapOf("dust" to 300, "net" to 200),
        collapses = 3,
    )

    @Test
    fun `the first finished run becomes the ghost`() {
        val run = finished(4_000.0, listOf(1.0, 10.0, 100.0))
        val after = GameEngine.collapse(run, now)

        assertEquals(run.history, after.bestHistory, "Der erste Lauf hat keine Geisterkurve hinterlassen")
        assertEquals(run.runSeconds, after.bestRunSeconds)
    }

    @Test
    fun `only a faster run replaces the ghost`() {
        val first = GameEngine.collapse(finished(4_000.0, listOf(1.0, 5.0)), now)

        val slower = GameEngine.collapse(
            finished(9_000.0, listOf(2.0, 9.0)).copy(
                bestRunSeconds = first.bestRunSeconds,
                bestHistory = first.bestHistory,
            ),
            now,
        )
        assertEquals(first.bestHistory, slower.bestHistory, "Ein langsamerer Lauf hat den Geist überschrieben")

        val faster = GameEngine.collapse(
            finished(2_000.0, listOf(3.0, 30.0, 300.0)).copy(
                bestRunSeconds = first.bestRunSeconds,
                bestHistory = first.bestHistory,
            ),
            now,
        )
        assertEquals(listOf(3.0, 30.0, 300.0), faster.bestHistory, "Der schnellere Lauf zählt nicht")
        // The curve and the time always describe the same run.
        assertEquals(2_000.0, faster.bestRunSeconds)
    }

    @Test
    fun `the ghost survives a big bang and a challenge`() {
        val withGhost = GameEngine.collapse(finished(3_000.0, listOf(1.0, 8.0)), now)
            .copy(collapses = 60, runMass = 1e30, mass = 1e30, bestTier = Tiers.last.index)

        assertEquals(withGhost.bestHistory, GameEngine.bigBang(withGhost, now, Path.HAND.id).bestHistory)
        assertEquals(withGhost.bestHistory, GameEngine.startChallenge(withGhost, "c_hand", now).bestHistory)
    }

    /**
     * A run standing at the gate with a fleet behind it.
     *
     * The plain fixture deliberately does not work here, and that is worth knowing rather than
     * papering over: at four times the threshold with a modest fleet, twenty minutes adds so
     * little mass that both figures floor to the same number — which is exactly the case the card
     * hides the line for. The projection is only ever advice when production is a real fraction of
     * the run's own mass.
     */
    private fun atTheGate(): GameState = GameState.new(now).copy(
        runMass = Tiers.last.threshold,
        mass = Tiers.last.threshold,
        runSeconds = 3_000.0,
        collectors = Collectors.all.filterNot { it.catalogueOnly }.associate { it.id to 400 },
        singularities = 500.0,
        collapses = 20,
    )

    @Test
    fun `waiting is projected to pay more, and never less`() {
        val run = atTheGate()
        val horizons = listOf(0.0, 20 * 60.0, 60 * 60.0, 6 * 3_600.0, 24 * 3_600.0)
        val paid = horizons.map { GameEngine.singularitiesIn(run, it) }

        assertTrue(paid.zipWithNext().all { (a, b) -> b >= a }, "Länger warten zahlt weniger: $paid")
        assertTrue(paid.last() > paid.first(), "Warten zahlt nie mehr: $paid")

        // Twenty minutes at the gate is deliberately *not* asserted to differ, and that is the
        // interesting part. It adds about nine per cent to the run's mass, the payout goes as the
        // square root of that, and at twelve singularities four per cent does not cross an integer
        // — so both figures floor to the same number. The card hides the line in exactly that case
        // rather than printing "in zwanzig Minuten: dasselbe".
        assertEquals(
            GameEngine.pendingSingularities(run),
            GameEngine.singularitiesIn(run, 20 * 60.0),
        )
    }

    @Test
    fun `the projection never promises more than actually arrives`() {
        val run = atTheGate()
        val horizon = 6 * 3_600.0
        val promised = GameEngine.singularitiesIn(run, horizon)
        // Deliberately an underestimate: it projects from the production of *now*, and production
        // climbs across those hours. A figure that flattered and then disappointed would be worse
        // than none at all; one that undersells is a quiet promise kept.
        val arrived = GameEngine.pendingSingularities(GameEngine.tick(run, horizon))
        assertTrue(arrived >= promised, "Versprochen $promised, gekommen $arrived")
    }

    @Test
    fun `the projection is the plain payout when no time passes`() {
        val run = atTheGate()
        assertEquals(GameEngine.pendingSingularities(run), GameEngine.singularitiesIn(run, 0.0))
        assertEquals(GameEngine.pendingSingularities(run), GameEngine.singularitiesIn(run, -5.0))
    }

    @Test
    fun `the last payout is recorded for the next card to compare against`() {
        val run = finished(3_000.0, emptyList())
        val paid = GameEngine.pendingSingularities(run)
        val after = GameEngine.collapse(run, now)

        assertEquals(paid, after.lastRunSingularities, "Was der Kollaps zahlte, wurde nicht notiert")
        // And it is not wiped by the next run starting.
        assertEquals(paid, GameEngine.startChallenge(after.copy(collapses = 4), "c_hand", now).lastRunSingularities)
    }
}
