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
        /*
         * Measured against the tree rather than against a number.
         *
         * This used to say "twelve of fourteen", which was true when it was written and stopped
         * being true the moment the lab grew a second storey — the test then failed for a reason
         * that had nothing to do with what it is about. The contract's ceiling was always the size
         * of the catalogue; the test should ask the catalogue too.
         */
        val contract = Contract.byId("ct_research")!!
        val total = ResearchTree.all.size
        val target = contract.target.toInt()

        // Near the top, with fewer than one target's worth of projects left: it cannot be finished.
        val leftOver = total - target + 1
        val late = completionist().copy(
            research = ResearchTree.all.take(leftOver).map { it.id }.toSet(),
            contractMarks = mapOf("ct_research" to leftOver.toDouble()),
        )
        assertFalse(contract.isPossibleFor(late), "Es sind weniger als $target Projekte übrig")

        val early = late.copy(
            research = emptySet(),
            contractMarks = mapOf("ct_research" to 0.0),
        )
        assertTrue(contract.isPossibleFor(early), "Von vorn muss es zu schaffen sein")
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
        /*
         * Everything finite is done and every daily allowance is spent.
         *
         * This used to demand a non-empty table, and that was satisfiable only because three
         * contracts had no allowance at all — the same three the Äonen audit found could be farmed
         * for ever. Closing that hole made this state reachable, which means the demand has to
         * change rather than the fix.
         *
         * What actually has to hold is that the player is never left with *nothing coming back*.
         * An empty table because today is finished is a different thing from an empty table because
         * the game has run out, and the difference has to be visible — so the state has a name and
         * the panel says it out loud.
         */
        val spent = completionist().copy(
            contractsToday = Contract.all.mapNotNull { c -> c.dailyLimit?.let { c.id to it } }.toMap(),
        )
        val dealt = GameEngine.tick(spent, 1.0)
        if (dealt.contracts.isEmpty()) {
            assertTrue(
                Contract.restingUntilTomorrow(dealt),
                "Der Tisch ist leer und das Spiel sagt nicht, dass es am Tag liegt",
            )
        }

        // And the day turning has to actually hand it back — otherwise "morgen wieder" is a lie.
        val tomorrow = GameEngine.onWallClock(dealt, now + 2 * 24 * 3_600_000L)
        assertTrue(
            GameEngine.tick(tomorrow, 1.0).contracts.isNotEmpty(),
            "Am nächsten Tag liegt immer noch nichts auf dem Tisch",
        )
    }
}
