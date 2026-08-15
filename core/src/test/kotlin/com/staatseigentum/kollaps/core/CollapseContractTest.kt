package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The contract that asks you to collapse, walked the way a player walks it.
 *
 * Reported as not working at all, and it is the one contract whose counter the *reset itself*
 * touches — so if any of the three things it depends on failed to survive a collapse, this is
 * exactly the contract that would show it and the only one that would.
 */
class CollapseContractTest {

    /** A save deep enough to collapse, with the table already dealt. */
    private fun readyToCollapse(): GameState = GameEngine.tick(
        GameState(
            collapses = 1,
            runMass = Tiers.last.threshold,
            totalMass = Tiers.last.threshold,
            mass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
        ),
        0.1,
    )

    @Test
    fun `the table is dealt and carries a mark for the collapse contract`() {
        val state = readyToCollapse()
        assertTrue(Contract.isUnlocked(state), "Aufträge sind gar nicht freigeschaltet")
        assertTrue(state.contracts.isNotEmpty(), "Es liegt nichts auf dem Tisch")
    }

    /**
     * The whole point of it: five collapses, and the row is finished.
     *
     * Driven through the engine rather than by editing the state, because what is being doubted
     * here is precisely whether the engine's own reset keeps what the contract counts against.
     */
    @Test
    fun `five collapses finish the contract`() {
        val contract = Contract.byId("ct_collapses")!!
        var state = readyToCollapse().let { dealt ->
            // Put it on the table by hand, with the mark the engine would have written. A random
            // deal will not always include it and this test is about this row in particular.
            dealt.copy(
                contracts = listOf(contract.id),
                contractMarks = mapOf(contract.id to contract.counter(dealt)),
            )
        }
        val started = state.collapses

        repeat(5) {
            state = GameEngine.collapse(state, NOW)
            // Back up to collapsing weight, the way a run does.
            state = state.copy(
                runMass = Tiers.last.threshold,
                mass = Tiers.last.threshold,
                bestTier = Tiers.last.index,
            )
        }

        assertEquals(started + 5, state.collapses, "Die Kollapse wurden nicht gezählt")
        assertEquals(
            contract.id,
            state.contracts.firstOrNull(),
            "Der Auftrag ist vom Tisch verschwunden",
        )
        assertEquals(5.0, contract.progressOf(state), "Der Fortschritt zählt nicht mit")
        assertTrue(contract.isMetBy(state), "Fünf Kollapse reichen dem Auftrag nicht")
    }

    /** And handing it in has to pay, and take the row off the table. */
    @Test
    fun `it can be handed in once it is met`() {
        val contract = Contract.byId("ct_collapses")!!
        val met = readyToCollapse().copy(
            collapses = 20,
            contracts = listOf(contract.id),
            contractMarks = mapOf(contract.id to 15.0),
        )
        assertTrue(contract.isMetBy(met), "Zwanzig minus fünfzehn ist nicht fünf")

        val claimed = GameEngine.claimContract(met, contract.id)
        assertTrue(claimed.aeons > met.aeons, "Es gab keine Äonen")
        assertTrue(contract.id !in claimed.contracts, "Der Auftrag liegt noch auf dem Tisch")
        assertEquals(met.contractsDone + 1, claimed.contractsDone)
    }

    /**
     * The mark must not move under the row while it is being worked on.
     *
     * This is the failure that would look exactly like "the contract does not work": every
     * collapse counts, and every collapse also resets the zero it counts from, so the bar sits at
     * nought for ever no matter how many you do.
     */
    @Test
    fun `a collapse does not rewrite the mark`() {
        val contract = Contract.byId("ct_collapses")!!
        var state = readyToCollapse().copy(
            contracts = listOf(contract.id),
            contractMarks = mapOf(contract.id to 1.0),
        )

        repeat(3) {
            state = GameEngine.collapse(state, NOW)
            state = state.copy(runMass = Tiers.last.threshold, bestTier = Tiers.last.index)
            // A tick between collapses, because that is where the table is refilled — and
            // refilling is the step that could overwrite a mark.
            state = GameEngine.tick(state, 0.1)
        }

        assertEquals(1.0, state.contractMarks[contract.id], "Die Marke wurde verschoben")
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
