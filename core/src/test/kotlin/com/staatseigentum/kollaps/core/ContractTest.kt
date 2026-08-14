package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Three things worth doing next, dealt from a deck. */
class ContractTest {

    private val now = 1_700_000_000_000L

    private fun player(collapses: Int = 6, bigBangs: Int = 2): GameState = GameState.new(now).copy(
        collapses = collapses,
        bigBangs = bigBangs,
        universes = (0 until bigBangs).map { ParkedUniverse(slot = it, bestTier = 20) },
        collectors = mapOf("dust" to 200),
        runMass = 1e9,
        mass = 1e9,
    )

    @Test
    fun `the table fills itself and holds exactly three`() {
        val fresh = player()
        assertTrue(fresh.contracts.isEmpty())

        val dealt = GameEngine.tick(fresh, 1.0)
        assertEquals(Contract.SLOTS, dealt.contracts.size, "Der Tisch hat ${dealt.contracts.size} Aufträge")
        assertEquals(Contract.SLOTS, Contract.offered(dealt).size)
        // Dealt by the tick, so somebody who has never opened the tab still finds three waiting.
        assertEquals(dealt.contracts, GameEngine.tick(dealt, 1.0).contracts)
    }

    @Test
    fun `nothing is dealt that the player is not deep enough for`() {
        val shallow = GameEngine.tick(player(collapses = 1, bigBangs = 0), 1.0)
        for (id in shallow.contracts) {
            assertEquals(0, assertNotNull(Contract.byId(id)).requiredBigBangs, "$id ist zu tief für diesen Stand")
        }
    }

    @Test
    fun `handing one in pays and deals another`() {
        var state = GameEngine.tick(player(), 1.0)
        val fleet = assertNotNull(Contract.byId("ct_fleet"))
        state = state.copy(
            // Deliberately built without duplicates: the deal never produces one, and a hand that
            // held the same contract twice would be testing the dedupe rather than the payout.
            contracts = (listOf("ct_fleet") + state.contracts).distinct().take(Contract.SLOTS),
            // The fixture's two hundred machines are deliberately short of the thousand this asks
            // for, so the fleet is built up here rather than the contract being made smaller.
            collectors = mapOf("dust" to 800, "net" to 400),
        )

        assertTrue(fleet.isMetBy(state), "Zwölfhundert Maschinen reichen nicht für tausend")
        val paid = GameEngine.claimContract(state, "ct_fleet")
        assertEquals(state.aeons + fleet.reward, paid.aeons)
        assertEquals(1, paid.contractsDone)

        val refilled = GameEngine.tick(paid, 1.0)
        assertEquals(Contract.SLOTS, refilled.contracts.size, "Es wurde nicht nachgelegt")
        assertTrue("ct_fleet" !in refilled.contracts, "Derselbe Auftrag wurde noch einmal ausgeteilt")
    }

    @Test
    fun `an unmet contract cannot be handed in`() {
        val state = GameEngine.tick(player(), 1.0).copy(contracts = listOf("ct_orbits"))
        assertEquals(0, Orbits.occupiedCount(state))
        assertEquals(state, GameEngine.claimContract(state, "ct_orbits"))
        // Nor one that is not on the table at all.
        assertEquals(state, GameEngine.claimContract(state, "ct_fleet"))
        assertEquals(state, GameEngine.claimContract(state, "gibtsnicht"))
    }

    @Test
    fun `the more-of contracts count from where that contract was dealt`() {
        // A deep save must not finish "five further collapses" by opening the tab.
        val veteran = GameEngine.tick(player(collapses = 40), 1.0)
            .let { it.copy(contracts = listOf("ct_collapses"), contractMarks = mapOf("ct_collapses" to 40.0)) }
        val five = assertNotNull(Contract.byId("ct_collapses"))
        assertTrue(!five.isMetBy(veteran), "Vierzig Kollapse haben fünf weitere sofort erfüllt")

        val later = veteran.copy(collapses = veteran.collapses + 5)
        assertTrue(five.isMetBy(later), "Fünf weitere Kollapse reichen nicht")
    }

    /**
     * The bug the per-contract marks exist for.
     *
     * One shared mark set to the maximum of four unrelated counters asked a save with thirteen
     * collapses for sixteen research projects. There are fourteen in the tree, so the contract
     * could not be finished, could not be replaced, and sat there for ever showing 0 / 3.
     */
    @Test
    fun `a contract never asks for more than its own counter can reach`() {
        val deep = GameEngine.tick(
            player(collapses = 40, bigBangs = Multiverse.SLOTS).copy(
                research = ResearchTree.all.take(6).map { it.id }.toSet(),
                challengesDone = setOf("c_hand"),
                findsAnswered = 2,
            ),
            1.0,
        )

        for (id in deep.contracts) {
            val contract = assertNotNull(Contract.byId(id))
            val mark = deep.contractMarks[id] ?: 0.0
            assertTrue(
                mark <= contract.counter(deep),
                "${contract.title} startet bei $mark, der Zähler steht aber bei ${contract.counter(deep)}",
            )
        }

        // Specifically: the research contract must be reachable inside the tree that exists.
        val research = assertNotNull(Contract.byId("ct_research"))
        val dealt = deep.copy(
            contracts = listOf("ct_research"),
            contractMarks = mapOf("ct_research" to deep.research.size.toDouble()),
        )
        val needed = (deep.contractMarks["ct_research"] ?: deep.research.size.toDouble()) + research.target
        assertTrue(
            deep.research.size + research.target <= ResearchTree.all.size.toDouble(),
            "Der Auftrag verlangt $needed Projekte, es gibt ${ResearchTree.all.size}",
        )
        val finished = dealt.copy(research = ResearchTree.all.take(deep.research.size + 3).map { it.id }.toSet())
        assertTrue(research.isMetBy(finished), "Drei weitere Projekte erfüllen ihn nicht")
    }

    @Test
    fun `a mark belongs to its contract and survives its neighbours being replaced`() {
        var state = GameEngine.tick(player(collapses = 40), 1.0)
        val marked = state.contracts.filter { Contract.byId(it)?.fromHere == true }
        val before = state.contractMarks.filterKeys { it in marked }
        assertTrue(before.isNotEmpty(), "Kein einziger Auftrag misst von hier aus")

        // Hand in something else; the untouched contracts keep their zero.
        state = state.copy(collectors = mapOf("dust" to 1_200), contracts = listOf("ct_fleet") + state.contracts)
        state = GameEngine.tick(GameEngine.claimContract(state, "ct_fleet"), 1.0)

        for ((id, mark) in before) {
            if (id !in state.contracts) continue
            assertEquals(mark, state.contractMarks[id], "$id hat seine Marke verloren")
        }
    }

    @Test
    fun `the hand is dealt from the record and not from chance`() {
        val a = GameEngine.tick(player(), 1.0)
        val b = GameEngine.tick(player(), 1.0)
        assertEquals(a.contracts, b.contracts, "Zwei gleiche Spielstände bekommen verschiedene Hände")

        val reloaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(a)))
        assertEquals(a.contracts, reloaded.contracts, "Neu laden würfelt die Hand neu")
    }

    @Test
    fun `the table survives both resets`() {
        val dealt = GameEngine.tick(player(collapses = 40), 1.0)
            .copy(runMass = 1e30, mass = 1e30, bestTier = Tiers.last.index)

        assertEquals(dealt.contracts, GameEngine.collapse(dealt, now).contracts)
        assertEquals(dealt.contractsDone, GameEngine.bigBang(dealt.copy(collapses = 60), now, Path.HAND.id).contractsDone)
    }

    @Test
    fun `every contract is finishable and says where it stands`() {
        val state = GameEngine.tick(player(bigBangs = Multiverse.SLOTS), 1.0)
        assertEquals(Contract.all.size, Contract.all.map { it.id }.toSet().size)
        for (contract in Contract.all) {
            assertTrue(contract.title.isNotBlank())
            assertTrue(contract.reward > 0.0, "${contract.title} zahlt nichts")
            assertTrue(contract.target > 0.0, "${contract.title} verlangt nichts")
            assertTrue(contract.fractionOf(state) in 0.0..1.0)
            assertTrue(contract.statusOf(state).isNotBlank())
        }
    }
}
