package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Going back into an old universe and playing it for an hour. */
class GalaxyVisitTest {

    private val now = 1_700_000_000_000L

    /** A universe worth parking, then parked. */
    private fun afterBigBang(): GameState {
        val lived = GameState.new(now).copy(
            collapses = 14,
            singularities = 250.0,
            bestTier = Tiers.last.index,
            runMass = 1e26,
            mass = 5e25,
            collectors = mapOf("dust" to 400, "net" to 220),
            upgrades = Upgrades.all.filter { it.cost < 1e9 }.map { it.id }.toSet(),
            orbits = 4,
            satellites = mapOf(0 to 1e18),
            aeons = 40.0,
            achievements = setOf("a_tier_erde"),
            path = Path.MASCHINE.id,
        )
        return GameEngine.bigBang(lived, now, Path.HAND.id)
    }

    @Test
    fun `the big bang stows the universe instead of dropping it`() {
        val after = afterBigBang()
        val galaxy = after.universes.single()

        assertTrue(galaxy.isPlayable, "Die Galaxie ist leer, es gibt nichts zu besuchen")
        val run = assertNotNull(galaxy.run)
        assertEquals(400, run.collectors["dust"])
        assertEquals(250.0, run.singularities)
        assertEquals(14, run.collapses)
        assertEquals(4, run.orbits)

        // And the new universe still starts from nothing.
        assertEquals(0.0, after.runMass)
        assertTrue(after.collectors.isEmpty())
    }

    @Test
    fun `visiting swaps the universe and leaves the player alone`() {
        val home = afterBigBang().copy(mass = 123.0, runMass = 456.0)
        val visit = GameEngine.visitGalaxy(home, 0)

        assertEquals(0, visit.visiting)
        assertEquals(Multiverse.VISIT_SECONDS, visit.visitSecondsLeft)
        // The old universe is on.
        assertEquals(400, visit.ownedOf("dust"))
        assertEquals(250.0, visit.singularities)
        assertEquals(Path.MASCHINE.id, visit.path)
        // What belongs to the player did not roll back.
        assertEquals(home.aeons, visit.aeons)
        // Containment, not equality. Achievements belong to the player and only ever grow —
        // standing in a deep universe again genuinely earns the ones for its rungs, and nothing
        // that was earned is ever handed back.
        assertTrue(visit.achievements.containsAll(home.achievements), "Ein Erfolg ging verloren")
        assertEquals(home.bigBangs, visit.bigBangs)
        assertEquals(home.universes.size, visit.universes.size)
    }

    @Test
    fun `leaving puts back what was done there and restores the newest universe`() {
        val home = afterBigBang().copy(mass = 123.0, runMass = 456.0, collectors = mapOf("dust" to 7))
        var visit = GameEngine.visitGalaxy(home, 0)
        visit = GameEngine.buyCollector(visit.copy(mass = 1e30), "dust", BuyAmount.ONE)
        val boughtTo = visit.ownedOf("dust")

        val back = GameEngine.leaveGalaxy(visit)
        assertNull(back.visiting)
        assertEquals(0.0, back.visitSecondsLeft)
        // The newest universe is back, exactly as it was left.
        assertEquals(7, back.ownedOf("dust"))
        assertEquals(456.0, back.runMass)
        // And the visited one kept the purchase.
        assertEquals(boughtTo, back.universes.single().run!!.collectors["dust"])
    }

    @Test
    fun `the hour runs on play time and hands the player back`() {
        val visit = GameEngine.visitGalaxy(afterBigBang(), 0)

        val partway = GameEngine.tick(visit, 600.0)
        assertEquals(0, partway.visiting, "Zehn Minuten haben den Besuch beendet")
        assertTrue(partway.visitSecondsLeft < Multiverse.VISIT_SECONDS)

        val over = GameEngine.tick(partway, Multiverse.VISIT_SECONDS)
        assertNull(over.visiting, "Die Stunde ist um und der Besuch läuft weiter")
        assertNotNull(over.universes.single().run)
    }

    @Test
    fun `nothing can be visited from inside a visit, a challenge, or an empty galaxy`() {
        val visit = GameEngine.visitGalaxy(afterBigBang(), 0)
        assertFalse(Multiverse.canVisit(visit, 0), "Aus einem Besuch heraus lässt sich besuchen")

        val challenged = GameEngine.startChallenge(afterBigBang().copy(collapses = 5), "c_hand", now)
        assertFalse(Multiverse.canVisit(challenged, 0), "Aus einer Herausforderung heraus")

        // A galaxy from before this existed carries no universe.
        val old = afterBigBang().let { it.copy(universes = it.universes.map { g -> g.copy(run = null) }) }
        assertFalse(old.universes.single().isPlayable)
        assertFalse(Multiverse.canVisit(old, 0))
        assertEquals(old.universes, GameEngine.visitGalaxy(old, 0).universes)
    }

    @Test
    fun `a visit survives the save and comes back where it was`() {
        val visit = GameEngine.tick(GameEngine.visitGalaxy(afterBigBang(), 0), 300.0)
        val back = assertNotNull(SaveCodec.decode(SaveCodec.encode(visit)))

        assertEquals(visit.visiting, back.visiting)
        assertEquals(visit.visitSecondsLeft, back.visitSecondsLeft, 1e-6)
        assertEquals(visit.ownedOf("dust"), back.ownedOf("dust"))
        assertNotNull(back.homeRun)
        assertEquals(0, GameEngine.leaveGalaxy(back).ownedOf("dust"), "Die Rückkehr bringt nicht das neueste Universum")
    }

    @Test
    fun `collapsing inside a visit works and stays there`() {
        val visit = GameEngine.visitGalaxy(afterBigBang(), 0)
            .let { it.copy(runMass = Tiers.last.threshold * 4, mass = 1e26) }
        assertTrue(GameEngine.canCollapse(visit), "Im alten Universum lässt sich nicht kollabieren")

        val collapsed = GameEngine.collapse(visit, now)
        assertEquals(0, collapsed.visiting, "Der Kollaps hat den Besuch beendet")
        assertTrue(collapsed.collapses > visit.collapses)

        // And it stays in that universe when the visit ends.
        val back = GameEngine.leaveGalaxy(collapsed)
        assertEquals(collapsed.collapses, back.universes.single().run!!.collapses)
        assertEquals(14, back.universes.single().run!!.collapses - 1)
    }
}
