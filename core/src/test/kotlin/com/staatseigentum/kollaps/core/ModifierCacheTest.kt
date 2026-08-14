package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.system.measureNanoTime

/**
 * The folded modifiers are remembered per state, and the two things that could go wrong with that.
 *
 * A cache is only ever as good as its invalidation, and this one is keyed on object identity — so
 * the test that matters is not "is it fast" but "does a changed state still get a changed answer".
 * Both are here, because a cache that is correct and never hits is a cache that does nothing.
 */
class ModifierCacheTest {

    /** A save with something in every layer of the fold, so the walk is not a formality. */
    private fun loaded(): GameState = GameState(
        mass = 1e12,
        runMass = 1e12,
        collectors = Collectors.all.associate { it.id to 40 },
        upgrades = Upgrades.all.take(12).map { it.id }.toSet(),
        prestigeUpgrades = PrestigeUpgrades.all.take(4).map { it.id }.toSet(),
        aeonUpgrades = AeonUpgrades.all.take(3).map { it.id }.toSet(),
        investments = Investments.all.associate { it.id to 5 },
        research = ResearchTree.all.take(6).map { it.id }.toSet(),
        singularities = 500.0,
        collapses = 12,
    )

    @Test
    fun `a changed state gets a changed answer`() {
        val base = loaded()
        val before = GameEngine.massPerSecond(base)

        // Buying something has to be visible immediately. This is the failure a stale cache would
        // produce, and it would look like a purchase that did nothing.
        val richer = GameEngine.buyUpgrade(
            base.copy(mass = 1e30),
            GameEngine.upgradeOffers(base).first { it.upgrade.id !in base.upgrades }.upgrade.id,
        )
        assertNotEquals(base.upgrades, richer.upgrades, "Der Kauf ging gar nicht durch")
        assertTrue(
            GameEngine.massPerSecond(richer) > before,
            "Ein gekauftes Upgrade ändert die Produktion nicht — der Cache ist abgestanden",
        )
    }

    @Test
    fun `a state that is equal but not identical is still answered correctly`() {
        // Identity keying means a copy is a miss, not a wrong answer. Guards the version of this
        // that keyed on equality and would have had to compare forty fields to find out.
        val base = loaded()
        val twin = base.copy()
        assertEquals(base, twin)
        assertEquals(GameEngine.massPerSecond(base), GameEngine.massPerSecond(twin), 1e-6)
    }

    @Test
    fun `the tick's own state is not re-folded on every read`() {
        val state = loaded()
        // Warm, so the first fold is not being timed against the rest.
        GameEngine.massPerSecond(state)

        val cold = measureNanoTime {
            // A fresh object every time: every one of these is a miss and pays for a full fold.
            repeat(REPEATS) { GameEngine.massPerSecond(state.copy(mass = state.mass + it)) }
        }
        val warm = measureNanoTime {
            repeat(REPEATS) { GameEngine.massPerSecond(state) }
        }

        // Deliberately a loose bound. The point is not a speed figure — it is that the second loop
        // is doing categorically less work than the first, which is only true if the cache is
        // being hit at all. A tighter number would fail on a busy build machine and teach nobody
        // anything.
        assertTrue(
            warm * 2 < cold,
            "Der Cache greift nicht: warm ${warm / 1_000}µs gegen kalt ${cold / 1_000}µs",
        )
    }

    private companion object {
        const val REPEATS = 400
    }
}
