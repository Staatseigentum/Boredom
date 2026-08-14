package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The survey turns something up every fifty rungs, and asks what it is for. */
class CatalogueFindTest {

    private val now = 1_700_000_000_000L

    private fun climbing(rungs: Int): GameState = GameState.new(now).copy(
        bigBangs = Multiverse.SLOTS,
        universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it, bestTier = 20) },
        runMass = Tiers.last.threshold * Math.pow(Designations.THRESHOLD_GROWTH, rungs + 0.5),
        mass = 1e30,
        collapses = 40,
        collectors = mapOf("dust" to 100),
    )

    @Test
    fun `nothing is found below the ladder or below the first fifty rungs`() {
        val below = GameState.new(now).copy(runMass = Tiers.last.threshold / 2.0)
        assertTrue(!CatalogueFind.isUnlocked(below))
        assertEquals(0, CatalogueFind.earnedBy(below))

        val early = climbing(CatalogueFind.EVERY_RUNGS - 5)
        assertEquals(0, CatalogueFind.earnedBy(early))
        assertNull(GameEngine.tick(early, 1.0).pendingFind, "Ein Fund kam zu früh")
    }

    @Test
    fun `climbing turns finds up and the tick puts them on the table`() {
        val high = climbing(CatalogueFind.EVERY_RUNGS * 3)
        assertEquals(3, CatalogueFind.earnedBy(high))

        val ticked = GameEngine.tick(high, 1.0)
        assertNotNull(ticked.pendingFind, "Drei verdiente Funde und keiner liegt auf dem Tisch")
        // One at a time: two questions at once is a dialog on top of a dialog.
        assertEquals(ticked.pendingFind, GameEngine.tick(ticked, 1.0).pendingFind)
    }

    @Test
    fun `each answer pays in its own currency`() {
        val waiting = GameEngine.tick(climbing(CatalogueFind.EVERY_RUNGS * 2), 1.0)

        val studied = GameEngine.answerFind(waiting, FindAnswer.AUSWERTEN.id)
        assertEquals(waiting.aeons + CatalogueFind.AEON_REWARD, studied.aeons)
        assertNull(studied.pendingFind)

        val tapped = GameEngine.answerFind(waiting, FindAnswer.ANZAPFEN.id)
        assertEquals(1, tapped.findsTapped)
        assertTrue(
            GameEngine.massPerSecond(tapped) > GameEngine.massPerSecond(waiting),
            "Anzapfen bringt nichts",
        )

        val left = GameEngine.answerFind(waiting, FindAnswer.RUHEN.id)
        assertTrue(left.findFragments.isNotEmpty(), "Es wurde nichts notiert")
        // Every answer counts as answered, or the same find comes straight back.
        assertEquals(waiting.findsAnswered + 1, left.findsAnswered)
    }

    @Test
    fun `the tapped bonus is capped and belongs to the run`() {
        var state = climbing(CatalogueFind.EVERY_RUNGS * 200)
        repeat(200) {
            state = GameEngine.tick(state, 1.0)
            if (state.pendingFind != null) state = GameEngine.answerFind(state, FindAnswer.ANZAPFEN.id)
        }
        assertTrue(state.findsTapped > 20, "Nur ${state.findsTapped} Funde angezapft")
        assertEquals(CatalogueFind.TAP_CAP, CatalogueFind.tapMultiplier(state), 1e-9)

        // And it is gone at the collapse, which is the whole trade.
        val after = GameEngine.collapse(state.copy(runMass = 1e30, mass = 1e30), now)
        assertEquals(0, after.findsTapped, "Der Anzapf-Bonus hat den Kollaps überlebt")
        assertEquals(1.0, CatalogueFind.tapMultiplier(after))
    }

    @Test
    fun `the record survives both resets and the fragments with it`() {
        val answered = GameEngine.answerFind(
            GameEngine.tick(climbing(CatalogueFind.EVERY_RUNGS * 2), 1.0),
            FindAnswer.RUHEN.id,
        ).copy(runMass = 1e30, mass = 1e30, collapses = 60)

        val collapsed = GameEngine.collapse(answered, now)
        assertEquals(answered.findsAnswered, collapsed.findsAnswered)
        assertEquals(answered.findFragments, collapsed.findFragments)

        val banged = GameEngine.bigBang(answered, now, Path.HAND.id)
        assertEquals(answered.findFragments, banged.findFragments, "Der Urknall hat die Notizen verbrannt")
    }

    @Test
    fun `which find turns up is decided by the record and not by chance`() {
        val high = climbing(CatalogueFind.EVERY_RUNGS * 4)
        assertEquals(CatalogueFind.next(high).id, CatalogueFind.next(high).id)
        // Reloading must not re-roll a question into a better one.
        val reloaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(high)))
        assertEquals(CatalogueFind.next(high).id, CatalogueFind.next(reloaded).id)
    }

    @Test
    fun `every find says something and nothing is duplicated`() {
        assertEquals(CatalogueFind.all.size, CatalogueFind.all.map { it.id }.toSet().size)
        for (find in CatalogueFind.all) {
            assertTrue(find.title.isNotBlank() && find.flavor.isNotBlank() && find.fragment.isNotBlank())
        }
        for (answer in FindAnswer.entries) {
            assertTrue(answer.label.isNotBlank() && answer.flavor.isNotBlank())
        }
    }
}
