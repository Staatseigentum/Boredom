package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The day's allowance on the contracts that can be repeated. */
class ContractLimitTest {

    private val day = 1_700_000_000_000L
    private val nextDay = day + 86_400_000L

    /** Deep enough that collapses come quickly — the state the press was found in. */
    private fun endgame(): GameState = GameState.new(day).copy(
        collapses = 400,
        bigBangs = Multiverse.SLOTS,
        universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it, bestTier = 24, collapses = 20) },
        contractDay = Contract.dayOf(day),
        aeons = 0.0,
    )

    private fun onlyCollapses(state: GameState): GameState = state.copy(
        contracts = listOf("ct_collapses"),
        contractMarks = mapOf("ct_collapses" to state.collapses.toDouble()),
    )

    @Test
    fun `five collapses can be handed in five times a day and not a sixth`() {
        var state = onlyCollapses(endgame())
        val contract = Contract.byId("ct_collapses")!!
        assertEquals(5, contract.dailyLimit)

        var paid = 0
        repeat(8) {
            state = state.copy(collapses = state.collapses + 5)
            if (contract.isMetBy(state) && !Contract.isSpentToday(state, contract)) {
                val after = GameEngine.claimContract(state, "ct_collapses")
                if (after.aeons > state.aeons) paid++
                state = after.copy(
                    contracts = listOf("ct_collapses"),
                    contractMarks = mapOf("ct_collapses" to after.collapses.toDouble()),
                )
            }
        }
        assertEquals(5, paid, "Der Auftrag wurde $paid mal bezahlt statt fünfmal")
        assertEquals(contract.reward * 5, state.aeons, 1e-9)
    }

    @Test
    fun `a spent contract leaves the table instead of sitting there`() {
        val spent = endgame().copy(
            contracts = listOf("ct_collapses"),
            contractsToday = mapOf("ct_collapses" to 5),
        )
        assertTrue("ct_collapses" !in Contract.refilled(spent), "Er bleibt liegen")
        // And the table is filled up with something that can still be done.
        val dealt = GameEngine.tick(spent, 1.0)
        assertTrue("ct_collapses" !in dealt.contracts)
        assertEquals(Contract.SLOTS, dealt.contracts.size, "Der Tisch wurde nicht aufgefüllt")
    }

    @Test
    fun `the allowance comes back the next day`() {
        val spent = endgame().copy(contractsToday = mapOf("ct_collapses" to 5))
        assertTrue(Contract.isSpentToday(spent, Contract.byId("ct_collapses")!!))

        val tomorrow = GameEngine.onWallClock(spent, nextDay)
        assertEquals(Contract.dayOf(nextDay), tomorrow.contractDay)
        assertTrue(tomorrow.contractsToday.isEmpty(), "Das Kontingent wurde nicht zurückgesetzt")
        assertFalse(Contract.isSpentToday(tomorrow, Contract.byId("ct_collapses")!!))
    }

    @Test
    fun `a clock that runs backwards also resets rather than sticking`() {
        val spent = endgame().copy(contractsToday = mapOf("ct_collapses" to 5))
        val yesterday = GameEngine.onWallClock(spent, day - 86_400_000L)
        assertTrue(yesterday.contractsToday.isEmpty(), "Eine zurückgestellte Uhr sperrt für immer")
    }

    @Test
    fun `the rule is enforced where it is decided, not only where it is shown`() {
        val spent = endgame().copy(
            contracts = listOf("ct_collapses"),
            contractMarks = mapOf("ct_collapses" to 0.0),
            contractsToday = mapOf("ct_collapses" to 5),
        )
        assertTrue(Contract.byId("ct_collapses")!!.isMetBy(spent))
        assertEquals(spent.aeons, GameEngine.claimContract(spent, "ct_collapses").aeons)
    }

    @Test
    fun `only the contracts that can repeat carry a limit`() {
        // The rest finish themselves: five alloys, sixteen challenges, fourteen projects, and a
        // pile of gold that only grows. A limit on those would be a rule with nothing to rule.
        val limited = Contract.all.filter { it.dailyLimit != null }.map { it.id }.toSet()
        assertEquals(setOf("ct_rungs", "ct_collapses", "ct_finds", "ct_sky"), limited)

        for (contract in Contract.all) {
            contract.dailyLimit?.let {
                assertTrue(it in 5..10, "${contract.title} erlaubt $it am Tag")
            }
        }
    }

    @Test
    fun `the day map never grows past the limited contracts`() {
        var state = endgame().copy(contracts = Contract.all.map { it.id })
        for (contract in Contract.all) {
            state = state.copy(
                contractMarks = state.contractMarks + (contract.id to 0.0),
                collapses = 10_000,
                alloys = Alloy.entries.map { it.id }.toSet(),
                collectors = mapOf("dust" to 2_000),
                orbits = 8,
                satellites = (0 until 8).associateWith { 1e20 },
                findsAnswered = 100,
                challengesDone = Challenge.entries.map { it.id }.toSet(),
                research = ResearchTree.all.map { it.id }.toSet(),
                heavy = mapOf("au" to 1e6),
                runMass = Tiers.last.threshold * 1e10,
            )
            state = GameEngine.claimContract(state, contract.id)
        }
        assertTrue(
            state.contractsToday.keys.all { Contract.byId(it)?.dailyLimit != null },
            "Es wird für Aufträge mitgezählt, die kein Limit haben: ${state.contractsToday.keys}",
        )
    }

    @Test
    fun `the allowance survives both resets`() {
        val spent = endgame().copy(
            contractsToday = mapOf("ct_collapses" to 3),
            runMass = 1e30,
            mass = 1e30,
            bestTier = Tiers.last.index,
        )
        assertEquals(spent.contractsToday, GameEngine.collapse(spent, day).contractsToday)
        assertEquals(spent.contractsToday, GameEngine.bigBang(spent, day, Path.HAND.id).contractsToday)
    }
}
