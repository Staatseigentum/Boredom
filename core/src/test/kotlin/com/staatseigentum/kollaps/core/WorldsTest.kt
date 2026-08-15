package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.pixel.Skins
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the body turns out to be, and what is left of it afterwards.
 *
 * The pillar the other two stand on. Impacts without this are a slot machine and shells are a shop;
 * the reason to build an ice world rather than the metal one that pays better is that the ice world
 * is one this save has not been — and that only holds if the record survives the collapse that
 * takes everything else.
 */
class WorldsTest {

    @AfterTest
    fun restore() = assertTrue(Rollout.accretion, "Der Schalter wurde nicht zurückgestellt")

    /** A body built out of one thing is named after it. */
    @Test
    fun `the leading shell names the world`() {
        val metal = GameState(shells = mapOf(Shell.KERN.id to 8))
        assertEquals(Lane.METALL, Worlds.laneOf(metal))

        val rock = GameState(shells = mapOf(Shell.MANTEL.id to 8))
        assertEquals(Lane.GESTEIN, Worlds.laneOf(rock))

        val ice = GameState(shells = mapOf(Shell.KRUSTE.id to 8))
        assertEquals(Lane.EIS, Worlds.laneOf(ice))
    }

    /**
     * And a body built evenly is a world of its own rather than a failed one.
     *
     * Three shells at three levels each has a leader — `maxByOrNull` always finds one — and if the
     * lane were simply "whichever is largest", an even body would be named after whichever of the
     * three happened to be checked first. It has to fall through to layered instead.
     */
    @Test
    fun `an evenly built body is layered`() {
        val even = GameState(
            shells = mapOf(Shell.KERN.id to 3, Shell.MANTEL.id to 3, Shell.KRUSTE.id to 3),
        )
        assertEquals(Lane.GESCHICHTET, Worlds.laneOf(even))
        assertEquals(Lane.GESCHICHTET, Worlds.current(even)?.lane)
    }

    @Test
    fun `a body too shallow to be anything is nothing`() {
        assertNull(Worlds.current(GameState()), "Ein leerer Körper hat schon einen Typ")
        val thin = GameState(shells = mapOf(Shell.KERN.id to Depth.JUNG.atLeast - 1))
        assertNull(Worlds.current(thin), "Ein zu flacher Körper hat einen Typ")

        val enough = GameState(shells = mapOf(Shell.KERN.id to Depth.JUNG.atLeast))
        assertEquals(Depth.JUNG, Worlds.current(enough)?.depth)
    }

    @Test
    fun `depth is read as the deepest band reached`() {
        assertNull(Depth.of(0))
        assertEquals(Depth.JUNG, Depth.of(Depth.JUNG.atLeast))
        assertEquals(Depth.GEREIFT, Depth.of(Depth.GEREIFT.atLeast))
        assertEquals(Depth.VOLLENDET, Depth.of(Depth.VOLLENDET.atLeast + 40))
    }

    /** Twelve of them, each reachable, and none of them sharing a name or an id. */
    @Test
    fun `the grid is complete and unambiguous`() {
        assertEquals(Lane.entries.size * Depth.entries.size, Worlds.all.size)
        assertEquals(Worlds.all.size, Worlds.all.map { it.id }.toSet().size, "Doppelte Kennung")
        assertEquals(Worlds.all.size, Worlds.all.map { it.label }.toSet().size, "Doppelter Name")
        for (world in Worlds.all) {
            assertEquals(world, Worlds.byId(world.id))
            assertEquals(world, Worlds.of(world.lane, world.depth))
            assertTrue(world.flavor.isNotBlank(), "${world.label} hat keinen Text")
        }
    }

    /** Building is what writes the record, on the press rather than a tick later. */
    @Test
    fun `the record is written the moment the body qualifies`() {
        var state = GameState(materials = Material.entries.associate { it.id to 1e6 })
        repeat(Depth.JUNG.atLeast - 1) { state = GameEngine.buildShell(state, Shell.KRUSTE.id) }
        assertTrue(state.worldTypes.isEmpty(), "Zu früh eingetragen")

        state = GameEngine.buildShell(state, Shell.KRUSTE.id)
        assertEquals(setOf(Worlds.of(Lane.EIS, Depth.JUNG).id), state.worldTypes)
    }

    /** Passing through a band still records it — nothing is skipped by building on. */
    @Test
    fun `every band on the way is kept`() {
        var state = GameState(materials = Material.entries.associate { it.id to 1e9 })
        repeat(Depth.GEREIFT.atLeast) { state = GameEngine.buildShell(state, Shell.KERN.id) }

        assertTrue(Worlds.of(Lane.METALL, Depth.JUNG).id in state.worldTypes, "Die junge Welt fehlt")
        assertTrue(Worlds.of(Lane.METALL, Depth.GEREIFT).id in state.worldTypes)
        assertEquals(2, state.worldTypes.size, "Es wurde mehr eingetragen als erreicht")
    }

    /** The tick is the second way in, for a save that was already deep before the record existed. */
    @Test
    fun `a save that is already a world records itself on the next tick`() {
        val deep = GameState(shells = mapOf(Shell.MANTEL.id to Depth.GEREIFT.atLeast))
        assertTrue(deep.worldTypes.isEmpty())
        val ticked = GameEngine.tick(deep, 0.1)
        assertTrue(
            Worlds.of(Lane.GESTEIN, Depth.GEREIFT).id in ticked.worldTypes,
            "Der Tick trägt nichts nach",
        )
    }

    /**
     * The whole point, in one test.
     *
     * The collapse takes the shells and the loose material — that is what makes the next run a
     * decision rather than a continuation — and leaves the record, which is what makes the decision
     * mean anything.
     */
    @Test
    fun `a collapse takes the body and leaves the record`() {
        val built = GameState(
            runMass = Tiers.last.threshold,
            mass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            materials = mapOf(Material.EIS.id to 40.0),
            shells = mapOf(Shell.KRUSTE.id to Depth.JUNG.atLeast),
            impactsAbsorbed = 17,
        )
        val recorded = GameEngine.tick(built, 0.1)
        val expected = Worlds.of(Lane.EIS, Depth.JUNG).id
        assertTrue(expected in recorded.worldTypes, "Der Typ wurde gar nicht erst eingetragen")

        val after = GameEngine.collapse(recorded, NOW)
        assertEquals(recorded.collapses + 1, after.collapses, "Es wurde nicht kollabiert")
        assertTrue(after.shells.isEmpty(), "Die Schichten haben den Kollaps überlebt")
        assertTrue(after.materials.isEmpty(), "Das Material hat den Kollaps überlebt")
        assertTrue(expected in after.worldTypes, "Der Weltentyp ist verloren gegangen")
        assertEquals(17L, after.impactsAbsorbed, "Der Zähler wurde zurückgesetzt")
    }

    /** And the big bang, which takes strictly more, leaves it too. */
    @Test
    fun `a big bang leaves the record`() {
        val world = Worlds.of(Lane.METALL, Depth.VOLLENDET).id
        val ready = GameState(
            // The button reads the collapse counter, not the singularities; see [BigBang.pending].
            collapses = 200,
            singularities = 1e12,
            runMass = Tiers.last.threshold,
            mass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            worldTypes = setOf(world),
            shells = mapOf(Shell.KERN.id to 12),
        )
        val after = GameEngine.bigBang(ready, NOW)
        assertEquals(ready.bigBangs + 1, after.bigBangs, "Es gab keinen Urknall")
        assertTrue(after.shells.isEmpty(), "Die Schichten haben den Urknall überlebt")
        assertTrue(world in after.worldTypes, "Der Weltentyp ist verloren gegangen")
    }

    @Test
    fun `the record pays, a little, and only for real types`() {
        val none = GameState()
        assertEquals(1.0, Worlds.multiplier(none))

        val two = GameState(worldTypes = setOf(Worlds.all[0].id, Worlds.all[1].id))
        assertEquals(1.0 + 2 * Worlds.BONUS_EACH, Worlds.multiplier(two))

        // A save carrying an id from a future version, or a broken one, must not be paid for it.
        val bogus = GameState(worldTypes = setOf("wt_gibt_es_nicht"))
        assertEquals(1.0, Worlds.multiplier(bogus), "Ein unbekannter Typ zählt mit")
    }

    @Test
    fun `the whole record is worth having and no more`() {
        val everything = GameState(worldTypes = Worlds.all.map { it.id }.toSet())
        assertEquals(1.0 + Worlds.all.size * Worlds.BONUS_EACH, Worlds.multiplier(everything))
        // A quarter more production for a collection that takes a dozen runs is a nice thing to
        // have finished. Anything much past that would be a wall rather than a collection.
        assertTrue(Worlds.multiplier(everything) < 1.35, "Die Sammlung ist zu stark geworden")
    }

    // ------------------------------------------------------------------ the picture

    @Test
    fun `a fresh body wears the palette it was given`() {
        val plain = Skins.ORIGINAL
        assertEquals(plain, Shells.tintOver(plain, GameState()), "Ein leerer Körper wird eingefärbt")

        val even = GameState(
            shells = mapOf(Shell.KERN.id to 5, Shell.MANTEL.id to 5, Shell.KRUSTE.id to 5),
        )
        assertEquals(plain, Shells.tintOver(plain, even), "Ein gleichmäßiger Körper wird eingefärbt")
    }

    @Test
    fun `a one-sided body bends the palette towards what it is made of`() {
        val icy = GameState(shells = mapOf(Shell.KRUSTE.id to 15))
        val tinted = Shells.tintOver(Skins.ORIGINAL, icy)
        assertTrue(tinted.tintStrength > 0f, "Die Zusammensetzung ist nicht zu sehen")
        assertNotEquals(Skins.ORIGINAL.tint, tinted.tint)

        // Never far enough to lose the picture: a body is tinted, not repainted.
        assertTrue(tinted.tintStrength < 0.5f, "Der Körper wird übermalt statt eingefärbt")

        // A deeper crust bends it further than a shallow one.
        val thin = Shells.tintOver(Skins.ORIGINAL, GameState(shells = mapOf(Shell.KRUSTE.id to 15, Shell.KERN.id to 10)))
        assertTrue(thin.tintStrength < tinted.tintStrength, "Der Anteil ändert die Färbung nicht")
    }

    /** And a palette the player chose is bent, never replaced. */
    @Test
    fun `a chosen scheme survives the composition`() {
        val chosen = Skins.all.first { it.desaturation > 0f }
        val over = Shells.tintOver(chosen, GameState(shells = mapOf(Shell.KERN.id to 15)))
        assertEquals(chosen.id, over.id, "Das Schema wurde ausgetauscht")
        assertEquals(chosen.desaturation, over.desaturation, "Die Entsättigung ging verloren")
        assertTrue(over.tintStrength > chosen.tintStrength, "Die Zusammensetzung wurde ignoriert")
        assertTrue(over.tintStrength <= 1f)
    }

    /** And with the update switched back off, the body is drawn exactly as it was before it. */
    @Test
    fun `the picture goes back to normal if the update is withdrawn`() =
        Rollout.accretion(live = false) {
            val built = GameState(shells = mapOf(Shell.KRUSTE.id to 15))
            assertEquals(Skins.ORIGINAL, Shells.tintOver(Skins.ORIGINAL, built))
            assertFalse(Worlds.isUnlocked(built))
        }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
