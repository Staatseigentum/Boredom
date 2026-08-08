package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoreTest {

    /**
     * The ladder has grown twice already.
     *
     * Both times a body was inserted into the middle, and both times everything indexed by tier
     * had to be looked at. This is the check that says whether that was done here too.
     */
    @Test
    fun `every rung of the ladder has a line`() {
        for (tier in Tiers.all) {
            val fragment = Lore.forTier(tier.index)
            assertNotNull(fragment, "${tier.name} hat keine Zeile")
            assertTrue(fragment.text.isNotBlank(), tier.name)
            assertEquals(tier.name, fragment.source)
        }
    }

    @Test
    fun `nothing is written for a rung that does not exist`() {
        assertNull(Lore.forTier(Tiers.all.size))
        assertNull(Lore.forTier(-1))
    }

    @Test
    fun `no two rungs say the same thing`() {
        val lines = Tiers.all.mapNotNull { Lore.forTier(it.index)?.text }
        assertEquals(lines.size, lines.toSet().size, "Zwei Stufen sagen dasselbe")
    }

    @Test
    fun `every fragment has its own id`() {
        val everything = GameState(
            bestTier = Tiers.last.index,
            collapses = 99,
            bigBangs = 99,
        )
        val ids = Lore.unlocked(everything).map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Doppelte Fragment-Kennung")
    }

    @Test
    fun `the collapses and big bangs keep talking after the list runs out`() {
        for (count in 1..40) {
            assertNotNull(Lore.forCollapse(count), "Kollaps $count schweigt")
            assertNotNull(Lore.forBigBang(count), "Urknall $count schweigt")
        }
        assertNull(Lore.forCollapse(0))
        assertNull(Lore.forBigBang(0))
    }

    @Test
    fun `a fresh save has nothing to read`() {
        val fresh = GameState.new(0)
        assertFalse(Lore.isWorthShowing(fresh))
        assertEquals(1, Lore.unlocked(fresh).size, "Auf der ersten Stufe steht mehr als eine Zeile")
    }

    /** Reading is a record, not a possession: nothing may take a line back. */
    @Test
    fun `a collapse never un-reads a line`() {
        val far = GameState(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
        )
        val before = Lore.unlocked(far).size
        val after = Lore.unlocked(GameEngine.collapse(far, nowMillis = 1)).size

        assertTrue(after >= before, "Der Kollaps hat Zeilen verschluckt: $before auf $after")
    }

    @Test
    fun `the chronicle grows as the game does`() {
        val early = GameState(bestTier = 3)
        val later = GameState(bestTier = 12, collapses = 4, bigBangs = 1)

        assertTrue(Lore.unlocked(later).size > Lore.unlocked(early).size)
        assertTrue(Lore.unlocked(later).size <= Lore.total)
    }

    @Test
    fun `the count on the header is reachable`() {
        val everything = GameState(
            bestTier = Tiers.last.index,
            collapses = 999,
            bigBangs = 999,
        )
        assertEquals(Lore.total, Lore.unlocked(everything).size)
    }
}
