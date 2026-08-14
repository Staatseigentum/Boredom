package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Eight galaxies, four jobs, and the half hour it costs to change your mind. */
class GalaxyJobTest {

    private val now = 1_700_000_000_000L

    private fun sky(job: GalaxyJob = GalaxyJob.FOERDERN): GameState = GameState.new(now).copy(
        bigBangs = Multiverse.SLOTS,
        universes = (0 until Multiverse.SLOTS).map {
            ParkedUniverse(slot = it, pathId = Path.MASCHINE.id, bestTier = 20, collapses = 15, jobId = job.id)
        },
        collectors = mapOf("dust" to 100),
        runMass = 1e9,
        mass = 1e9,
        collapses = 20,
    )

    @Test
    fun `producing is what a galaxy does unless told otherwise`() {
        // No job on the record at all — a save from before this existed — has to read as producing.
        val old = GameState.new(now).copy(universes = listOf(ParkedUniverse(slot = 0, bestTier = 15)))
        assertEquals(GalaxyJob.FOERDERN, old.universes.single().job)
        assertTrue(Multiverse.multiplier(old) > 1.0, "Eine Galaxie ohne Auftrag fördert nicht")
    }

    @Test
    fun `each job pays in its own currency and stops paying in the others`() {
        val producing = sky(GalaxyJob.FOERDERN)
        val thinking = sky(GalaxyJob.RECHNEN)
        val looking = sky(GalaxyJob.SUCHEN)
        val digging = sky(GalaxyJob.GRABEN)

        // Production: only the producers.
        assertTrue(Multiverse.multiplier(producing) > 1.0)
        assertEquals(1.0, Multiverse.multiplier(thinking), "Rechnende Galaxien fördern trotzdem")
        assertEquals(1.0, Multiverse.multiplier(digging))

        // Äonen: everyone a little, the thinkers a lot.
        assertTrue(Multiverse.aeonsPerSecond(thinking) > Multiverse.aeonsPerSecond(producing) * 2.0)
        assertTrue(Multiverse.aeonsPerSecond(producing) > 0.0, "Fördern zahlt gar keine Äonen")

        // Comets: only the searchers.
        assertTrue(Multiverse.cometFactor(looking) > 1.0)
        assertEquals(1.0, Multiverse.cometFactor(producing), "Fördernde Galaxien suchen mit")

        // Metal: only the diggers.
        assertTrue(Multiverse.metalPerSecond(digging).values.any { it > 0.0 })
        assertTrue(Multiverse.metalPerSecond(producing).isEmpty())
    }

    @Test
    fun `a galaxy contributes nothing at all while it changes over`() {
        val before = sky(GalaxyJob.FOERDERN)
        val switching = GameEngine.assignGalaxy(before, 0, GalaxyJob.RECHNEN.id)
        val moved = switching.universes.first { it.slot == 0 }

        assertTrue(moved.isRamping, "Die Umstellung läuft gar nicht")
        assertEquals(GalaxyJob.RECHNEN, moved.job)
        // Not producing any more, and not yet thinking either. That is the cost.
        assertTrue(
            Multiverse.multiplier(switching) < Multiverse.multiplier(before),
            "Sie fördert während der Umstellung weiter",
        )

        val settled = Multiverse.advanceRamps(switching, Multiverse.RAMP_SECONDS)
        assertTrue(!settled.universes.first { it.slot == 0 }.isRamping)
        assertTrue(
            Multiverse.aeonsPerSecond(settled) > Multiverse.aeonsPerSecond(switching),
            "Nach der Umstellung rechnet sie immer noch nicht",
        )
    }

    @Test
    fun `the ramp is counted by the tick and by the time away`() {
        val switching = GameEngine.assignGalaxy(sky(), 0, GalaxyJob.GRABEN.id)

        val ticked = GameEngine.tick(switching, 600.0)
        val left = ticked.universes.first { it.slot == 0 }.rampSeconds
        assertTrue(left < Multiverse.RAMP_SECONDS, "Der Tick zählt die Umstellung nicht herunter")
        assertTrue(left > 0.0, "Zehn Minuten haben eine halbe Stunde beendet")

        // And an hour with the app shut finishes it, uncapped by the offline rules.
        val away = GameEngine.applyOffline(ticked.copy(lastSeenAt = now - 3_600_000L), now).state
        assertTrue(!away.universes.first { it.slot == 0 }.isRamping, "Die Zeit weg zählt nicht mit")
    }

    @Test
    fun `choosing the job it is already on costs nothing`() {
        val producing = sky(GalaxyJob.FOERDERN)
        // The sky itself, not the whole state: the call goes through `award`, and a full sky earns
        // achievements the moment anything looks at it.
        assertEquals(
            producing.universes,
            GameEngine.assignGalaxy(producing, 0, GalaxyJob.FOERDERN.id).universes,
        )
        // A mis-tap must never cost half an hour, and neither must a slot that is not there.
        assertEquals(
            producing.universes,
            GameEngine.assignGalaxy(producing, 99, GalaxyJob.RECHNEN.id).universes,
        )
    }

    @Test
    fun `digging fills the piles the forge wants`() {
        val digging = sky(GalaxyJob.GRABEN)
        val after = GameEngine.tick(digging, 8 * 3_600.0)

        for (metal in HeavyElement.entries) {
            assertTrue(
                Heavy.amountOf(after, metal) > Heavy.amountOf(digging, metal),
                "${metal.label} wächst beim Graben nicht",
            )
        }
        // Slowly, on purpose: a dig that outpaced a collapse would replace it rather than
        // supplement it, and the forge is meant to be a reason to keep collapsing.
        val collapseYield = Heavy.yieldFrom(1e6).values.sum()
        assertTrue(
            Heavy.amountOf(after, HeavyElement.GOLD) < collapseYield,
            "Acht Stunden Graben schlagen einen Kollaps",
        )
    }

    @Test
    fun `a galaxy told to think stops handing on its path`() {
        val producing = sky(GalaxyJob.FOERDERN)
        val thinking = Multiverse.advanceRamps(
            GameEngine.assignGalaxy(producing, 0, GalaxyJob.RECHNEN.id),
            Multiverse.RAMP_SECONDS,
        )
        assertTrue(
            Multiverse.effects(thinking).size < Multiverse.effects(producing).size,
            "Sie lehnt weiter auf den Lauf, den sie verlassen hat",
        )
    }
}
