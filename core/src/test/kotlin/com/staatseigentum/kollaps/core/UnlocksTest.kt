package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnlocksTest {

    /** A card nobody can dismiss would sit on the screen for ever. */
    @Test
    fun `every introduction says something and no two share an id`() {
        val ids = Unlocks.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Doppelte Kennung")
        for (intro in Unlocks.all) {
            assertTrue(intro.title.isNotBlank(), intro.id)
            assertTrue(intro.where.isNotBlank(), "${intro.id}: kein Ort")
            assertTrue(intro.text.length > 40, "${intro.id}: zu wenig Text")
        }
    }

    /**
     * The load-bearing property of the whole design.
     *
     * Every introduction is gated behind a system that has to be unlocked, which is what makes it
     * safe to mark "everything already reached" as read on the first tick of a loaded save: for a
     * brand new game that set is empty, so a new player still gets every card in turn.
     */
    @Test
    fun `a new game has reached none of them`() {
        val fresh = GameState.new(0)
        assertTrue(
            Unlocks.applicable(fresh).isEmpty(),
            "Ein frisches Spiel hat schon ${Unlocks.applicable(fresh).map { it.id }} erreicht",
        )
        assertNull(Unlocks.pending(fresh))
    }

    @Test
    fun `an unlocked system offers its card exactly once`() {
        // Orbits unlock on their own condition; reach it and the card is waiting.
        var state = GameState(collapses = 1, bestTier = 12, runMass = Tiers.last.threshold)
        val first = Unlocks.pending(state)
        assertNotNull(first, "Nichts vorzustellen, obwohl mehrere Systeme offen sind")

        state = GameEngine.acknowledgeIntro(state)
        assertTrue(first.id in state.seenIntros)
        assertTrue(Unlocks.pending(state)?.id != first.id, "Dieselbe Karte kommt noch einmal")
    }

    /** Read is read, whatever the run does afterwards. */
    @Test
    fun `what has been read survives every reset`() {
        val read = GameEngine.acknowledgeIntro(
            GameState(collapses = 40, singularities = 1e9, runMass = Tiers.last.threshold),
        )
        assertTrue(read.seenIntros.isNotEmpty())
        assertEquals(read.seenIntros, GameEngine.collapse(read, 1).seenIntros)
        assertEquals(read.seenIntros, GameEngine.bigBang(read, 1).seenIntros)
    }

    /**
     * A save from before the introductions existed must not be handed the backlog.
     *
     * The worst possible version of this feature is eleven cards in a row about systems somebody
     * has been using for hours, so the first tick after loading marks everything already reached
     * as read.
     */
    @Test
    fun `a deep save is not buried in cards it does not need`() {
        val veteran = GameState(
            collapses = 12,
            bigBangs = 2,
            bestTier = Tiers.last.index,
            runMass = Tiers.last.threshold,
        )
        assertTrue(Unlocks.applicable(veteran).size > 4, "Der Test misst nichts")

        val settled = GameEngine.tick(veteran, 0.1)
        assertTrue(settled.introsSeeded, "Der Abgleich hat nicht stattgefunden")
        assertNull(Unlocks.pending(settled), "Der Veteran bekommt doch noch Karten")
    }

    /** And a new player still gets them, one at a time, as the systems arrive. */
    @Test
    fun `a new game still learns about each system when it turns up`() {
        val started = GameEngine.tick(GameState.new(0), 0.1)
        assertTrue(started.introsSeeded)
        assertNull(Unlocks.pending(started), "Ein frisches Spiel bekommt sofort eine Karte")

        // Far enough along to have unlocked something, without having been told about it.
        val later = started.copy(
            collapses = 1,
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
        )
        assertNotNull(Unlocks.pending(later), "Nach dem ersten Kollaps kommt keine Karte")
    }
}
