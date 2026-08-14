package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the big bang actually asks for after the first one.
 *
 * The regression this exists for was in the card and not in the rules: the panel printed
 * [BigBang.REQUIRED_COLLAPSES], which is what the *first* big bang costs, while the button used
 * [BigBang.requiredNow]. After one big bang the card read "Ab 10 Kollapsen. Du bist bei 12" next
 * to a button that refused. The rules were right and the sentence was wrong, which is the worse
 * way round — the player believes the sentence.
 */
class BigBangRequirementTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `the requirement rises with every big bang`() {
        assertEquals(BigBang.REQUIRED_COLLAPSES, BigBang.requiredFor(0))
        for (n in 0 until 8) {
            assertTrue(
                BigBang.requiredFor(n + 1) > BigBang.requiredFor(n),
                "Der ${n + 2}. Urknall verlangt nicht mehr als der ${n + 1}.",
            )
        }
    }

    @Test
    fun `twelve collapses after one big bang is not enough, and the number says so`() {
        // Exactly the state in the screenshot.
        val state = GameState.new(now).copy(
            bigBangs = 1,
            collapses = 12,
            bestTier = Tiers.last.index,
            runMass = Tiers.last.threshold * 100,
        )

        assertEquals(13, BigBang.requiredNow(state), "Nach einem Urknall sind es nicht 13")
        assertFalse(BigBang.canBang(state), "Zwölf Kollapse reichen plötzlich doch")
        // The figure the card prints has to be the one the button uses.
        assertTrue(
            BigBang.requiredNow(state) > state.collapses,
            "Die Karte würde eine Zahl nennen, die schon erreicht ist",
        )

        val enough = state.copy(collapses = 13)
        assertTrue(BigBang.canBang(enough), "Dreizehn Kollapse reichen immer noch nicht")
    }

    @Test
    fun `the base constant is only ever right for the first one`() {
        val fresh = GameState.new(now).copy(collapses = BigBang.REQUIRED_COLLAPSES)
        assertEquals(BigBang.REQUIRED_COLLAPSES, BigBang.requiredNow(fresh))
        assertTrue(BigBang.canBang(fresh))

        // And from the second onwards the constant undersells it, which is what the card did.
        for (n in 1..5) {
            val deeper = fresh.copy(bigBangs = n)
            assertTrue(
                BigBang.requiredNow(deeper) > BigBang.REQUIRED_COLLAPSES,
                "Nach $n Urknallen wäre die Grundzahl noch korrekt",
            )
        }
    }
}
