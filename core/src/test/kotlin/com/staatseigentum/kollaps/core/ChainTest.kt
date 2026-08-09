package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChainTest {

    /** A state sitting on a station, as the tick would leave it. */
    private fun at(chain: EventChain, station: String) = GameState(
        activeChain = chain.id,
        chainStation = station,
        pendingEvent = station,
        // A windfall pays a stretch of production, so the fixture needs something producing.
        collectors = mapOf("dust" to 5),
    )

    /**
     * The one mistake this structure invites.
     *
     * Stations are wired by id, so a typo is not a compile error — it is a story that stops dead
     * in the middle with no question on the table and no way to finish the chain.
     */
    @Test
    fun `every onward step points at a station that exists`() {
        for (chain in Chains.all) {
            assertNotNull(chain.station(chain.start), "${chain.id}: Anfang fehlt")
            for (stop in chain.stations) {
                for (option in listOf(stop.first, stop.second)) {
                    val next = option.next ?: continue
                    assertNotNull(
                        chain.station(next),
                        "${chain.id}/${stop.id}: Antwort ${option.label} fuehrt ins Leere ($next)",
                    )
                }
            }
        }
    }

    @Test
    fun `every station is reachable from the start`() {
        for (chain in Chains.all) {
            val seen = mutableSetOf<String>()
            fun walk(id: String?) {
                val stop = chain.station(id) ?: return
                if (!seen.add(stop.id)) return
                walk(stop.first.next)
                walk(stop.second.next)
            }
            walk(chain.start)
            val orphans = chain.stations.map { it.id }.toSet() - seen
            assertTrue(orphans.isEmpty(), "${chain.id}: nie erreichbar — $orphans")
        }
    }

    /** A story that cannot end is a dialog that never stops coming back. */
    @Test
    fun `every path through a chain reaches an ending`() {
        for (chain in Chains.all) {
            for (stop in chain.stations) {
                for (option in listOf(stop.first, stop.second)) {
                    var steps = 0
                    var here: String? = option.next
                    while (here != null) {
                        val next = chain.station(here)
                        assertNotNull(next, "${chain.id}: $here fehlt")
                        here = next.first.next
                        steps++
                        assertTrue(steps < 20, "${chain.id}: Schleife ab ${stop.id}")
                    }
                }
            }
        }
    }

    @Test
    fun `a chain is three to five answers deep`() {
        for (chain in Chains.all) {
            assertTrue(chain.depth in 2..5, "${chain.id} ist ${chain.depth} Stationen tief")
        }
    }

    @Test
    fun `both answers at the first station lead somewhere different`() {
        for (chain in Chains.all) {
            val start = chain.station(chain.start)!!
            assertTrue(
                start.first.next != start.second.next,
                "${chain.id}: Die erste Antwort ändert nichts",
            )
        }
    }

    @Test
    fun `answering moves the story on`() {
        val chain = Chains.all.first()
        val start = chain.station(chain.start)!!
        val after = GameEngine.chooseEvent(at(chain, chain.start), 0)

        assertEquals(start.first.next, after.chainStation)
        assertEquals(chain.id, after.activeChain, "Die Kette wurde mitten drin abgebrochen")
        assertNull(after.pendingEvent, "Die Frage steht noch auf dem Tisch")
        assertTrue(chain.id !in after.chainsDone, "Die Kette gilt schon als erzählt")
    }

    @Test
    fun `an answer that leads nowhere finishes the chain`() {
        val chain = Chains.all.first()
        val ending = chain.stations.first { it.first.next == null }
        val after = GameEngine.chooseEvent(at(chain, ending.id), 0)

        assertNull(after.activeChain)
        assertNull(after.chainStation)
        assertTrue(chain.id in after.chainsDone)
    }

    @Test
    fun `a finished chain never starts again`() {
        val far = GameState(
            runMass = Tiers.last.threshold,
            chainsDone = Chains.all.map { it.id }.toSet(),
        )
        assertNull(Chains.startable(far))
    }

    @Test
    fun `a chain waits for the body it was written for`() {
        val early = GameState(runMass = Tiers.all[1].threshold)
        val late = GameState(runMass = Tiers.last.threshold)

        val first = Chains.startable(early)
        assertTrue(
            first == null || Tiers.indexOf(first.unlockTier) <= Tiers.forMass(early.runMass).index,
            "Eine Kette taucht vor ihrem Himmelskörper auf",
        )
        assertNotNull(Chains.startable(late), "Ganz oben gibt es keine Kette mehr")
    }

    /**
     * A mis-tap on the way to the shop must not silently end something three answers deep.
     */
    @Test
    fun `turning a station down keeps the story`() {
        val chain = Chains.all.first()
        val after = GameEngine.dismissEvent(at(chain, chain.start))

        assertNull(after.pendingEvent, "Der Dialog bleibt offen")
        assertEquals(chain.id, after.activeChain)
        assertEquals(chain.start, after.chainStation, "Die Kette hat ihre Stelle verloren")
    }

    @Test
    fun `a collapse does not un-tell a story`() {
        val chain = Chains.all.first()
        val running = at(chain, chain.start).copy(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            chainsDone = setOf("chain_irgendwas"),
        )
        val after = GameEngine.collapse(running, nowMillis = 1)

        assertEquals(chain.id, after.activeChain)
        assertEquals(chain.start, after.chainStation)
        assertTrue("chain_irgendwas" in after.chainsDone)
    }

    @Test
    fun `a station is what the dialog shows, and it says which story it belongs to`() {
        val chain = Chains.all.first()
        val prompt = at(chain, chain.start).prompt

        assertNotNull(prompt)
        assertEquals(chain.station(chain.start)!!.title, prompt.title)
        assertEquals(chain.title, prompt.chain)
    }

    @Test
    fun `a one-off event says it belongs to no story`() {
        val single = GameState(pendingEvent = CosmicEvent.entries.first().id)
        val prompt = single.prompt

        assertNotNull(prompt)
        assertNull(prompt.chain)
    }

    @Test
    fun `answering a station pays the same way a one-off does`() {
        val chain = Chains.all.first()
        val start = chain.station(chain.start)!!
        val windfall = listOf(start.first, start.second)
            .indexOfFirst { it.reward is CometReward.Windfall }
        assertTrue(windfall >= 0, "Keine Antwort mit Sofortgewinn zum Prüfen")

        val before = at(chain, chain.start)
        val after = GameEngine.chooseEvent(before, windfall)
        assertTrue(after.mass > before.mass, "Die Antwort hat nichts eingebracht")
    }

    @Test
    fun `no two stations anywhere share an id`() {
        val ids = Chains.all.flatMap { chain -> chain.stations.map { it.id } }
        assertEquals(ids.size, ids.toSet().size, "Doppelte Stationskennung")
    }

    @Test
    fun `every station reads as something, not as a placeholder`() {
        for (chain in Chains.all) {
            for (stop in chain.stations) {
                assertTrue(stop.title.isNotBlank(), "${chain.id}/${stop.id}")
                assertTrue(stop.flavor.length > 20, "${chain.id}/${stop.id}: zu wenig Text")
                for (option in listOf(stop.first, stop.second)) {
                    assertTrue(option.label.isNotBlank(), "${chain.id}/${stop.id}")
                    assertTrue(option.flavor.isNotBlank(), "${chain.id}/${stop.id}")
                }
            }
        }
    }

    @Test
    fun `the two answers at a station are never the same`() {
        for (chain in Chains.all) {
            for (stop in chain.stations) {
                assertFalse(
                    stop.first.label == stop.second.label,
                    "${chain.id}/${stop.id}: zweimal dieselbe Antwort",
                )
            }
        }
    }
}
