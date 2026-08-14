package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The two things a player actually hit, and the sweep that came out of looking for more of them.
 *
 * Both reported bugs had the same shape: two rules that were each individually right, disagreeing
 * about the same question. So most of what is here is a check that the rules which *must* agree
 * still do.
 */
class ReportedBugsTest {

    private val now = 1_700_000_000_000L

    /** The reported save: twelve collapses, one big bang, the head start bought. */
    private fun reported(): GameState = GameState.new(now).copy(
        collapses = 12,
        bigBangs = 1,
        universes = listOf(ParkedUniverse(slot = 0, bestTier = 24, collapses = 11)),
        singularities = 6.39e11,
        prestigeUpgrades = setOf("p_collectors_1", "p_collectors_2"),
        investments = mapOf("i_fleet" to 40),
        research = ResearchTree.all.take(6).map { it.id }.toSet(),
        runMass = 1e30,
        mass = 1e30,
        bestTier = Tiers.last.index,
    )

    // ------------------------------------------------- gemeldet: Kaufknopf tut nichts

    @Test
    fun `the head start never hands over a machine the shop refuses to sell`() {
        val fresh = GameEngine.collapse(reported(), now)
        assertFalse(Designations.isUnlocked(fresh), "Der Testaufbau hat die Leiter schon offen")

        for (collector in Collectors.all.filter { it.catalogueOnly }) {
            assertEquals(
                0,
                fresh.ownedOf(collector.id),
                "${collector.name} wurde verschenkt, obwohl der Laden sie nicht verkauft",
            )
        }
    }

    @Test
    fun `what the shop shows and what the rules sell are the same set`() {
        // This is the invariant the dead button broke: a visible row whose buy does nothing.
        for (state in listOf(GameEngine.collapse(reported(), now), openLadder(), GameState.new(now))) {
            for (offer in GameEngine.collectorOffers(state.copy(mass = 1e60), BuyAmount.ONE)) {
                if (!offer.visible) continue
                if (offer.owned >= Collector.MAX_OWNED) continue
                val after = GameEngine.buyCollector(state.copy(mass = 1e60), offer.collector.id, BuyAmount.ONE)
                assertTrue(
                    after.ownedOf(offer.collector.id) > state.ownedOf(offer.collector.id),
                    "${offer.collector.name} steht im Laden, lässt sich aber nicht kaufen",
                )
            }
        }
    }

    private fun openLadder(): GameState = reported().copy(
        bigBangs = Multiverse.SLOTS,
        universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it, bestTier = 24) },
    )

    @Test
    fun `the automatic buyer is never offered something it cannot buy`() {
        val state = GameEngine.collapse(reported(), now).copy(
            mass = 1e60,
            automation = mapOf(AutomationRule.COLLECTORS.id to 3),
        )
        val candidates = GameEngine.collectorOffers(state, BuyAmount.ONE).filter { it.visible && it.amount > 0 }
        for (offer in candidates) {
            assertFalse(
                offer.collector.catalogueOnly && !Designations.isUnlocked(state),
                "Der Auto-Kauf bekommt ${offer.collector.name} angeboten und würde abprallen",
            )
        }
    }

    // ------------------------------------------- gemeldet: Aufträge aktualisieren sich nicht

    @Test
    fun `every dealt contract is finishable from where it was dealt`() {
        val dealt = GameEngine.tick(GameEngine.collapse(reported(), now).copy(mass = 1e30), 1.0)
        assertTrue(dealt.contracts.isNotEmpty(), "Es wurde gar nichts ausgeteilt")

        for (id in dealt.contracts) {
            val contract = Contract.byId(id)!!
            val mark = dealt.contractMarks[id] ?: 0.0
            assertTrue(
                mark <= contract.counter(dealt),
                "${contract.title} startet über dem eigenen Zähler: $mark gegen ${contract.counter(dealt)}",
            )
            assertTrue(contract.progressOf(dealt) >= 0.0)
            assertTrue(contract.fractionOf(dealt) in 0.0..1.0)
        }
    }

    @Test
    fun `finishing a challenge moves the challenge contract`() {
        // The exact report: a contract that stayed at 0 / 1 after the thing was done.
        var state = GameEngine.tick(GameEngine.collapse(reported(), now).copy(mass = 1e30), 1.0)
        state = state.copy(
            contracts = listOf("ct_challenge"),
            contractMarks = mapOf("ct_challenge" to state.challengesDone.size.toDouble()),
        )
        val contract = Contract.byId("ct_challenge")!!
        assertEquals(0.0, contract.progressOf(state))

        val done = state.copy(challengesDone = state.challengesDone + "c_hand")
        assertEquals(1.0, contract.progressOf(done), "Die bestandene Herausforderung zählt nicht")
        assertTrue(contract.isMetBy(done))
    }

    @Test
    fun `a research contract can be finished inside the tree that exists`() {
        val deep = GameEngine.tick(
            GameEngine.collapse(reported(), now).copy(mass = 1e30, research = ResearchTree.all.take(11).map { it.id }.toSet()),
            1.0,
        )
        val contract = Contract.byId("ct_research")!!
        val marked = deep.copy(
            contracts = listOf("ct_research"),
            contractMarks = mapOf("ct_research" to deep.research.size.toDouble()),
        )
        assertTrue(
            deep.research.size + contract.target <= ResearchTree.all.size.toDouble(),
            "Der Auftrag verlangt mehr Projekte, als es gibt",
        )
        val finished = marked.copy(research = ResearchTree.all.take(deep.research.size + 3).map { it.id }.toSet())
        assertTrue(contract.isMetBy(finished))
    }

    // ------------------------------------------------------------------ der Rundumschlag

    @Test
    fun `merging never silently drops a lean`() {
        val full = GameState.new(now).copy(
            bigBangs = Multiverse.SLOTS,
            universes = (0 until Multiverse.SLOTS).map {
                ParkedUniverse(slot = it, pathId = Path.entries[it % Path.entries.size].id, bestTier = 20, collapses = 10)
            },
        )
        val once = GameEngine.mergeGalaxies(full, keepSlot = 0, absorbSlot = 1)
        val merged = once.universes.first { it.slot == 0 }
        assertEquals(2, merged.paths.size)

        // A weld may not be welded again, from either side — the second lean would vanish.
        val refilled = once.copy(universes = once.universes + ParkedUniverse(slot = 1, pathId = Path.KERN.id, bestTier = 20))
        assertFalse(Multiverse.canMerge(refilled, 0, 1), "Eine verschweißte Galaxie nimmt noch eine auf")
        assertFalse(Multiverse.canMerge(refilled, 1, 0), "Eine verschweißte Galaxie lässt sich einsaugen")
    }

    @Test
    fun `merging removes exactly one galaxy and keeps the slots unique`() {
        val full = GameState.new(now).copy(
            bigBangs = Multiverse.SLOTS,
            // Deliberately identical apart from the slot, which is what breaks a removal by value.
            universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it, bestTier = 20, collapses = 10) },
        )
        val once = GameEngine.mergeGalaxies(full, keepSlot = 3, absorbSlot = 5)
        assertEquals(Multiverse.SLOTS - 1, once.universes.size, "Es wurde die falsche Galaxie entfernt")
        assertEquals(once.universes.size, once.universes.map { it.slot }.distinct().size)
        assertTrue(once.universes.any { it.slot == 3 }, "Die behaltene Galaxie ist verschwunden")
        assertFalse(once.universes.any { it.slot == 5 })
    }

    @Test
    fun `nothing in the whole state machine throws on a deep save`() {
        var state = openLadder().copy(
            collectors = Collectors.all.associate { it.id to Collector.MAX_OWNED },
            alloys = Alloy.entries.map { it.id }.toSet(),
            heavy = HeavyElement.entries.associate { it.id to 1e6 },
            runMass = Tiers.last.threshold * 1e40,
        )
        repeat(60) { state = GameEngine.tick(state, 60.0) }
        state = GameEngine.applyOffline(state.copy(lastSeenAt = now - 48 * 3_600_000L), now).state

        assertTrue(GameEngine.massPerSecond(state).isFinite())
        assertTrue(GameEngine.massPerTap(state).isFinite())
        assertTrue(GameEngine.pendingSingularities(state).isFinite())
        assertTrue(BigBang.pending(state).isFinite())
        assertTrue(Statistics.lines(state).isNotEmpty())
        assertTrue(GameEngine.collectorOffers(state, BuyAmount.ONE).isNotEmpty())
        assertTrue(state.aeons.isFinite() && state.aeons >= 0.0)
    }

    @Test
    fun `a save survives the whole round trip after all of it`() {
        var state = openLadder()
        repeat(30) { state = GameEngine.tick(state, 120.0) }
        state = GameEngine.assignGalaxy(state, 0, GalaxyJob.RECHNEN.id)
        state = GameEngine.mergeGalaxies(state, 2, 3)

        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertTrue(back != null, "Der Spielstand lässt sich nicht mehr laden")
        assertEquals(state.contracts, back.contracts)
        assertEquals(state.contractMarks, back.contractMarks)
        assertEquals(state.universes.map { it.slot }.sorted(), back.universes.map { it.slot }.sorted())
    }
}

/** A shop row must never refuse without saying why. */
class ShopHonestyTest {

    private val now = 1_700_000_000_000L

    /** A save from before the fix: it already holds machines the ladder has not unlocked. */
    private fun carriedOver(): GameState = GameState.new(now).copy(
        collapses = 12,
        bigBangs = 1,
        universes = listOf(ParkedUniverse(slot = 0, bestTier = 24, collapses = 11)),
        collectors = Collectors.all.associate { it.id to 145 },
        mass = 1e60,
        totalMass = 1e60,
        runMass = 1e30,
    )

    @Test
    fun `a locked row says so instead of going quiet`() {
        val state = carriedOver()
        val offers = GameEngine.collectorOffers(state, BuyAmount.ONE)

        val locked = offers.filter { it.collector.catalogueOnly }
        assertTrue(locked.isNotEmpty(), "Der Testaufbau hat keine gesperrten Zeilen")
        for (offer in locked) {
            assertTrue(offer.visible, "Eine besessene Maschine verschwindet aus dem Laden")
            assertFalse(offer.affordable, "Die Zeile behauptet, kaufbar zu sein")
            assertTrue(offer.lockedReason != null, "${offer.collector.name} schweigt zum Grund")
        }
    }

    @Test
    fun `nothing else in the shop claims to be locked`() {
        for (offer in GameEngine.collectorOffers(carriedOver(), BuyAmount.ONE)) {
            if (offer.collector.catalogueOnly) continue
            assertEquals(null, offer.lockedReason, "${offer.collector.name} ist grundlos gesperrt")
        }
    }

    @Test
    fun `with the ladder open nothing is locked at all`() {
        val open = carriedOver().copy(
            bigBangs = Multiverse.SLOTS,
            universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it, bestTier = 24) },
        )
        for (offer in GameEngine.collectorOffers(open, BuyAmount.ONE)) {
            assertEquals(null, offer.lockedReason, "${offer.collector.name} bleibt gesperrt")
        }
    }

    @Test
    fun `the free machines go away at the next collapse rather than being taken now`() {
        // Nothing is confiscated on load — the save keeps what it has. The head start simply
        // stops handing them out, so the next collapse is where it corrects itself.
        val state = carriedOver()
        assertEquals(145, state.ownedOf("entropie"))

        val after = GameEngine.collapse(
            state.copy(runMass = Tiers.last.threshold * 10, prestigeUpgrades = setOf("p_collectors_1")),
            now,
        )
        assertEquals(0, after.ownedOf("entropie"), "Die verschenkten Maschinen kommen wieder")
        assertTrue(after.ownedOf("dust") > 0, "Die reguläre Starthilfe ist mit weggefallen")
    }
}
