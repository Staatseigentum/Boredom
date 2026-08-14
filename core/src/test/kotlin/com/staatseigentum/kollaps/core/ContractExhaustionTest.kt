package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Contracts whose counter runs out.
 *
 * Three of the ten point at a finite pool — sixteen challenges, five alloys, fourteen research
 * projects. Once a player owns all of one, that counter never moves again, and the contract sat on
 * the table for ever asking for a seventeenth. Same family as the mark bug: a contract is only a
 * direction if what it points at is still ahead.
 */
class ContractExhaustionTest {

    private val now = 1_700_000_000_000L

    private fun completionist(): GameState = GameState.new(now).copy(
        collapses = 60,
        bigBangs = Multiverse.SLOTS,
        universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it, bestTier = 24, collapses = 20) },
        challengesDone = Challenge.entries.map { it.id }.toSet(),
        alloys = Alloy.entries.map { it.id }.toSet(),
        research = ResearchTree.all.map { it.id }.toSet(),
        contractDay = Contract.dayOf(now),
    )

    @Test
    fun `an exhausted counter is never dealt`() {
        val dealt = GameEngine.tick(completionist(), 1.0)
        for (id in listOf("ct_challenge", "ct_alloys", "ct_research")) {
            assertTrue(id !in dealt.contracts, "$id wurde ausgeteilt, obwohl er nicht mehr geht")
            assertFalse(Contract.byId(id)!!.isPossibleFor(dealt))
        }
        // The table is still full of things that can be done.
        assertEquals(Contract.SLOTS, dealt.contracts.size)
        assertTrue(dealt.contracts.all { Contract.byId(it)!!.isPossibleFor(dealt) })
    }

    @Test
    fun `one already on the table leaves it`() {
        // Exactly the report: everything done, and the row still sitting there.
        val stuck = completionist().copy(
            contracts = listOf("ct_challenge"),
            contractMarks = mapOf("ct_challenge" to Challenge.entries.size.toDouble()),
        )
        assertFalse(Contract.byId("ct_challenge")!!.isMetBy(stuck), "Er gilt fälschlich als erfüllt")

        val after = GameEngine.tick(stuck, 1.0)
        assertTrue("ct_challenge" !in after.contracts, "Er klebt weiter am Tisch")
    }

    @Test
    fun `it is dealt again while there is still room`() {
        val nearly = completionist().copy(
            challengesDone = Challenge.entries.dropLast(1).map { it.id }.toSet(),
            contracts = emptyList(),
        )
        val contract = Contract.byId("ct_challenge")!!
        assertTrue(contract.isPossibleFor(nearly), "Eine offene Herausforderung reicht nicht")

        // And the moment the last one is taken, it stops being dealt.
        val done = nearly.copy(challengesDone = Challenge.entries.map { it.id }.toSet())
        assertFalse(contract.isPossibleFor(done))
    }

    @Test
    fun `the ceiling is measured against the contract's own zero`() {
        // Twelve of fourteen projects done: three more do not fit, so it must not be dealt.
        val late = completionist().copy(
            research = ResearchTree.all.take(12).map { it.id }.toSet(),
            contractMarks = mapOf("ct_research" to 12.0),
        )
        assertFalse(Contract.byId("ct_research")!!.isPossibleFor(late), "Drei weitere Projekte gibt es nicht")

        val early = late.copy(
            research = ResearchTree.all.take(6).map { it.id }.toSet(),
            contractMarks = mapOf("ct_research" to 6.0),
        )
        assertTrue(Contract.byId("ct_research")!!.isPossibleFor(early))
    }

    @Test
    fun `a sky too small for four thinkers is not asked for four`() {
        val small = completionist().copy(
            universes = (0 until 3).map { ParkedUniverse(slot = it, bestTier = 24) },
        )
        assertFalse(Contract.byId("ct_sky")!!.isPossibleFor(small), "Drei Galaxien sollen vier rechnen")
        assertTrue("ct_sky" !in GameEngine.tick(small, 1.0).contracts)
    }

    @Test
    fun `the endless counters keep no ceiling at all`() {
        val deep = completionist()
        for (id in listOf("ct_collapses", "ct_rungs", "ct_finds", "ct_fleet", "ct_orbits", "ct_metal")) {
            assertTrue(Contract.byId(id)!!.isPossibleFor(deep), "$id gilt als unmöglich")
        }
    }

    @Test
    fun `a completionist still gets a table rather than an empty screen`() {
        // Everything finite is done and every daily allowance is spent: there must still be
        // something to do, or the screen whose job is "what next" answers "nothing".
        val spent = completionist().copy(
            contractsToday = Contract.all.mapNotNull { c -> c.dailyLimit?.let { c.id to it } }.toMap(),
        )
        val dealt = GameEngine.tick(spent, 1.0)
        assertTrue(dealt.contracts.isNotEmpty(), "Der Tisch ist völlig leer")
    }
}
