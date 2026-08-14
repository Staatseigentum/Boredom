package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Welding two galaxies, and what the orbits do for the sky. */
class GalaxyMergeTest {

    private val now = 1_700_000_000_000L

    private fun fullSky(): GameState = GameState.new(now).copy(
        bigBangs = Multiverse.SLOTS,
        collapses = 60,
        universes = (0 until Multiverse.SLOTS).map {
            ParkedUniverse(
                slot = it,
                pathId = if (it % 2 == 0) Path.MASCHINE.id else Path.HAND.id,
                bestTier = 20 + it,
                collapses = 10 + it,
                singularities = 100.0 * (it + 1),
            )
        },
    )

    @Test
    fun `merging frees a slot and keeps the deeper of the two`() {
        val before = fullSky()
        assertFalse(Multiverse.hasRoom(before))

        val after = GameEngine.mergeGalaxies(before, keepSlot = 7, absorbSlot = 0)
        assertEquals(Multiverse.SLOTS - 1, after.universes.size)
        assertTrue(Multiverse.hasRoom(after), "Es ist kein Platz frei geworden")

        val merged = after.universes.first { it.slot == 7 }
        assertEquals(maxOf(before.universes[7].bestTier, before.universes[0].bestTier), merged.bestTier)
        assertTrue(merged.isMerged, "Die verschmolzene Galaxie weiß nichts davon")
    }

    @Test
    fun `a weld carries both leans and is worth less than the two it was made of`() {
        val before = fullSky()
        val after = GameEngine.mergeGalaxies(before, keepSlot = 7, absorbSlot = 0)
        val merged = after.universes.first { it.slot == 7 }

        assertEquals(setOf(Path.HAND, Path.MASCHINE), merged.paths.toSet())
        // Deliberately not the sum: a sum would make merging strictly better than not merging, and
        // the whole decision is that a merge buys a slot and costs some of what stood in it.
        val apart = Multiverse.yieldOf(before.universes[7]) + Multiverse.yieldOf(before.universes[0])
        assertTrue(Multiverse.yieldOf(merged) < apart, "Verschmelzen kostet nichts")
        assertTrue(Multiverse.yieldOf(merged) > Multiverse.yieldOf(before.universes[7]))
    }

    @Test
    fun `nothing is welded while there is still room, or twice, or with itself`() {
        val roomy = fullSky().let { it.copy(universes = it.universes.drop(1)) }
        assertFalse(Multiverse.canMerge(roomy, 7, 2), "Mit freiem Platz wäre Verschmelzen reiner Verlust")
        assertEquals(roomy, GameEngine.mergeGalaxies(roomy, 7, 2))

        val full = fullSky()
        assertFalse(Multiverse.canMerge(full, 3, 3))
        assertEquals(full, GameEngine.mergeGalaxies(full, 3, 3))

        val once = GameEngine.mergeGalaxies(full, 7, 0)
        // The sky has room again, so a second weld is refused for that reason alone.
        assertFalse(Multiverse.canMerge(once, 7, 1))
    }

    @Test
    fun `a galaxy mid changeover cannot be welded`() {
        val switching = GameEngine.assignGalaxy(fullSky(), 0, GalaxyJob.RECHNEN.id)
        assertFalse(Multiverse.canMerge(switching, 7, 0), "Eine umstellende Galaxie lässt sich verschweißen")
        assertFalse(Multiverse.canMerge(switching, 0, 7))
    }

    @Test
    fun `a satellite lends its weight to the galaxy in the matching slot`() {
        val bare = fullSky()
        val orbiting = bare.copy(
            orbits = Multiverse.SLOTS,
            satellites = mapOf(3 to 1e20),
            runMass = 1e24,
        )

        val plain = Multiverse.weightedYieldOf(bare, bare.universes.first { it.slot == 3 })
        val lent = Multiverse.weightedYieldOf(orbiting, orbiting.universes.first { it.slot == 3 })
        assertTrue(lent > plain, "Der Trabant auf Bahn drei tut für Galaxie drei nichts")

        // And only for its own slot.
        val neighbour = Multiverse.weightedYieldOf(orbiting, orbiting.universes.first { it.slot == 4 })
        val neighbourBare = Multiverse.weightedYieldOf(bare, bare.universes.first { it.slot == 4 })
        assertEquals(neighbourBare, neighbour, 1e-9)
    }

    @Test
    fun `an unopened or empty orbit lends nothing`() {
        val sky = fullSky()
        assertEquals(0.0, Multiverse.orbitBonusFor(sky, sky.universes.first { it.slot == 3 }))

        val opened = sky.copy(orbits = Multiverse.SLOTS)
        assertEquals(0.0, Multiverse.orbitBonusFor(opened, opened.universes.first { it.slot == 3 }))
    }
}
