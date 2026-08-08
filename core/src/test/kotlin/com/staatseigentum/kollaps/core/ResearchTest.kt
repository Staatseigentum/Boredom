package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResearchTest {

    private val start = 1_700_000_000_000L

    /** A save with the lab open and mass to burn. */
    private fun withLab(mass: Double = 1e22): GameState = GameState(
        bestTier = Tiers.indexOf(ResearchTree.UNLOCK_TIER),
        mass = mass,
    )

    private fun project(id: String): Research =
        ResearchTree.byId(id) ?: error("Projekt fehlt: $id")

    private val first = ResearchTree.all.first { it.requires.isEmpty() }

    @Test
    fun `the lab stays shut until the earth`() {
        val early = GameState(bestTier = 0, mass = 1e22)
        assertFalse(ResearchTree.isUnlocked(early))
        assertEquals(early, GameEngine.startResearch(early, first.id, start))
    }

    @Test
    fun `finished work keeps the lab open even after a reset`() {
        val reset = GameState(bestTier = 0, research = setOf(first.id))
        assertTrue(ResearchTree.isUnlocked(reset))
    }

    @Test
    fun `starting takes the mass and sets the clock`() {
        val state = withLab(mass = first.cost * 3)
        val started = GameEngine.startResearch(state, first.id, start)

        assertEquals(first.id, started.activeResearch)
        assertEquals(first.cost * 2, started.mass, first.cost * 1e-9)
        assertEquals(start + (first.seconds * 1_000).toLong(), started.researchDoneAt)
    }

    @Test
    fun `the bench holds one project at a time`() {
        val busy = GameEngine.startResearch(withLab(), first.id, start)
        val other = ResearchTree.all.first { it.requires.isEmpty() && it.id != first.id }

        assertEquals(busy, GameEngine.startResearch(busy, other.id, start))
    }

    @Test
    fun `starting is refused without the mass`() {
        val broke = withLab(mass = first.cost - 1.0)
        assertEquals(broke, GameEngine.startResearch(broke, first.id, start))
    }

    @Test
    fun `a project with unmet requirements cannot be started`() {
        val gated = ResearchTree.all.first { it.requires.isNotEmpty() }
        val state = withLab()

        assertFalse(ResearchTree.isOpen(state, gated))
        assertEquals(state, GameEngine.startResearch(state, gated.id, start))

        val opened = state.copy(research = gated.requires.toSet())
        assertTrue(ResearchTree.isOpen(opened, gated))
        assertNotNull(GameEngine.startResearch(opened, gated.id, start).activeResearch)
    }

    @Test
    fun `nothing is booked before the time is up`() {
        val running = GameEngine.startResearch(withLab(), first.id, start)
        val early = start + (first.seconds * 1_000).toLong() - 1

        val settled = GameEngine.settleResearch(running, early)
        assertEquals(first.id, settled.activeResearch)
        assertTrue(settled.research.isEmpty())
        assertTrue(ResearchTree.progress(running, early) < 1f)
    }

    @Test
    fun `the project is booked once the time is up`() {
        val running = GameEngine.startResearch(withLab(), first.id, start)
        val due = start + (first.seconds * 1_000).toLong()

        val settled = GameEngine.settleResearch(running, due)
        assertNull(settled.activeResearch)
        assertTrue(first.id in settled.research)
        assertEquals(0L, settled.researchDoneAt)
    }

    /** The lab has to survive the phone being off for a week, not only for the exact duration. */
    @Test
    fun `a project that came due days ago still books`() {
        val running = GameEngine.startResearch(withLab(), first.id, start)
        val muchLater = start + 7L * 24 * 3_600 * 1_000

        assertTrue(first.id in GameEngine.settleResearch(running, muchLater).research)
    }

    @Test
    fun `cancelling frees the bench and keeps the mass spent`() {
        val running = GameEngine.startResearch(withLab(mass = first.cost), first.id, start)
        val cancelled = GameEngine.cancelResearch(running)

        assertNull(cancelled.activeResearch)
        assertEquals(0.0, cancelled.mass, 1e-9)
        assertTrue(cancelled.research.isEmpty())
    }

    @Test
    fun `an effect only applies once the project is booked`() {
        val boost = ResearchTree.all.first { it.effect is PrestigeEffect.GlobalMultiplier }
        val factor = (boost.effect as PrestigeEffect.GlobalMultiplier).factor
        val base = withLab().copy(collectors = mapOf("dust" to 50))

        val running = base.copy(activeResearch = boost.id, researchDoneAt = start)
        assertEquals(
            GameEngine.massPerSecond(base),
            GameEngine.massPerSecond(running),
            GameEngine.massPerSecond(base) * 1e-9,
        )

        val done = base.copy(research = setOf(boost.id))
        assertEquals(
            GameEngine.massPerSecond(base) * factor,
            GameEngine.massPerSecond(done),
            GameEngine.massPerSecond(base) * 1e-9,
        )
    }

    @Test
    fun `research survives every reset`() {
        val done = setOf(first.id)
        val collapsing = GameState(
            runMass = Tiers.last.threshold,
            research = done,
            bestTier = Tiers.last.index,
        )
        assertEquals(done, GameEngine.collapse(collapsing, start).research)

        val banging = collapsing.copy(collapses = BigBang.REQUIRED_COLLAPSES)
        assertTrue(BigBang.canBang(banging))
        assertEquals(done, GameEngine.bigBang(banging, start).research)
    }

    /** A project running when the universe ends should still be running afterwards. */
    @Test
    fun `a running project survives a collapse`() {
        val running = GameEngine.startResearch(
            withLab().copy(runMass = Tiers.last.threshold, bestTier = Tiers.last.index),
            first.id,
            start,
        )
        val after = GameEngine.collapse(running, start)

        assertEquals(first.id, after.activeResearch)
        assertEquals(running.researchDoneAt, after.researchDoneAt)
    }

    @Test
    fun `a speed bonus shortens the wait`() {
        val speedUp = ResearchTree.all.first { it.effect is PrestigeEffect.ResearchSpeed }
        val factor = (speedUp.effect as PrestigeEffect.ResearchSpeed).factor
        val fast = withLab().copy(research = setOf(speedUp.id))

        assertEquals(factor, GameEngine.researchSpeed(fast), 1e-9)
        assertEquals(first.seconds / factor, ResearchTree.duration(fast, first), 1e-9)

        val started = GameEngine.startResearch(fast, first.id, start)
        assertEquals(start + (first.seconds / factor * 1_000).toLong(), started.researchDoneAt)
    }

    @Test
    fun `a fusion bonus speeds up the furnaces`() {
        val boost = ResearchTree.all.first { it.effect is PrestigeEffect.FusionRate }
        val factor = (boost.effect as PrestigeEffect.FusionRate).factor

        val base = GameState(
            runMass = Tiers.byName(Fusion.UNLOCK_TIER).threshold,
            fusers = mapOf("intake" to 2),
        )
        val boosted = base.copy(research = setOf(boost.id))

        val plain = Fusion.amountOf(Fusion.advance(base, 10.0), Element.WASSERSTOFF)
        val quick = Fusion.amountOf(Fusion.advance(boosted, 10.0), Element.WASSERSTOFF)

        assertEquals(plain * factor, quick, plain * 1e-9)
    }

    @Test
    fun `the catalogue only offers what is reachable`() {
        val fresh = withLab()
        val offered = ResearchTree.offered(fresh)

        assertTrue(offered.isNotEmpty())
        assertTrue(offered.all { it.requires.isEmpty() }, "Ein gesperrtes Projekt wird angeboten")

        val advanced = fresh.copy(research = setOf("r_optics"))
        assertTrue(ResearchTree.offered(advanced).any { it.id == "r_telemetry" })
    }

    @Test
    fun `a save keeps the lab`() {
        val running = GameEngine.startResearch(withLab(), first.id, start)
            .copy(research = setOf("r_optics"))
        val back = SaveCodec.decode(SaveCodec.encode(running))
        assertNotNull(back)

        assertEquals(setOf("r_optics"), back.research)
        assertEquals(first.id, back.activeResearch)
        assertEquals(running.researchDoneAt, back.researchDoneAt)
    }

    @Test
    fun `a save drops research that no longer exists`() {
        val state = withLab().copy(
            research = setOf("r_optics", "r_perpetuum"),
            activeResearch = "r_perpetuum",
            researchDoneAt = start,
        )
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(setOf("r_optics"), back.research)
        assertNull(back.activeResearch, "Ein unbekanntes Projekt blockiert die Bank")
    }

    // ------------------------------------------------------------------ the catalogue itself

    @Test
    fun `every requirement can actually be met first`() {
        val done = mutableSetOf<String>()
        var progressed = true
        while (progressed) {
            progressed = false
            for (project in ResearchTree.all) {
                if (project.id !in done && done.containsAll(project.requires)) {
                    done += project.id
                    progressed = true
                }
            }
        }
        assertEquals(
            ResearchTree.all.map { it.id }.toSet(),
            done,
            "Unerreichbare Forschung — die Voraussetzungen laufen im Kreis",
        )
    }

    @Test
    fun `a project never comes before the one it needs`() {
        for (project in ResearchTree.all) {
            for (required in project.requires) {
                val needed = project(required)
                assertTrue(
                    needed.cost <= project.cost,
                    "${project.name} ist billiger als seine Voraussetzung ${needed.name}",
                )
            }
        }
    }

    @Test
    fun `every project says what it does`() {
        for (project in ResearchTree.all) {
            assertTrue(project.effectText.isNotBlank(), "${project.name} beschreibt sich nicht")
            assertTrue(project.flavor.isNotBlank())
            assertTrue(project.seconds > 0.0)
            assertTrue(project.cost > 0.0)
        }
    }
}
