package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathTest {

    private val now = 1_700_000_000_000L

    private fun readyToBang(): GameState = GameState(
        collapses = BigBang.REQUIRED_COLLAPSES,
        runMass = Tiers.last.threshold,
        bestTier = Tiers.last.index,
    )

    @Test
    fun `a universe has no lean until one is chosen`() {
        assertNull(Path.of(GameState()))
        assertNull(GameState().path)
    }

    @Test
    fun `the big bang takes the choice`() {
        val ready = readyToBang()
        assertTrue(BigBang.canBang(ready))

        val after = GameEngine.bigBang(ready, now, Path.LABOR.id)
        assertEquals(Path.LABOR, Path.of(after))
    }

    /**
     * A typo in a save file must not be able to eat a ten-collapse reset.
     *
     * The press still goes through, the universe simply comes out unaligned — which is a bad
     * outcome, but a far smaller one than refusing the button and leaving the player wondering
     * why nothing happened.
     */
    @Test
    fun `an unknown choice leaves the universe unaligned rather than refusing the press`() {
        val after = GameEngine.bigBang(readyToBang(), now, "path_wormhole")

        assertEquals(1, after.bigBangs)
        assertNull(after.path)
    }

    @Test
    fun `the lean lasts until the next big bang`() {
        val handed = GameEngine.bigBang(readyToBang(), now, Path.HAND.id)

        // A collapse and a challenge both happen inside the same universe.
        val collapsing = handed.copy(runMass = Tiers.last.threshold, bestTier = Tiers.last.index)
        assertEquals(Path.HAND.id, GameEngine.collapse(collapsing, now).path)

        val challenge = Challenge.entries.first()
        val started = GameEngine.startChallenge(
            handed.copy(collapses = 9),
            challenge.id,
            now,
        )
        assertEquals(Path.HAND.id, started.path)

        // The next big bang replaces it.
        val again = GameEngine.bigBang(
            handed.copy(
                collapses = BigBang.REQUIRED_COLLAPSES,
                runMass = Tiers.last.threshold,
                bestTier = Tiers.last.index,
            ),
            now,
            Path.KERN.id,
        )
        assertEquals(Path.KERN, Path.of(again))
    }

    @Test
    fun `each path leans the way it says`() {
        val fleet = GameState(collectors = mapOf("dust" to 100))

        val handed = fleet.copy(path = Path.HAND.id)
        assertTrue(GameEngine.massPerTap(handed) > GameEngine.massPerTap(fleet))

        val machined = fleet.copy(path = Path.MASCHINE.id)
        assertTrue(GameEngine.massPerSecond(machined) > GameEngine.massPerSecond(fleet))
        assertTrue(GameEngine.hasAutoBuy(machined), "Die Maschine kauft nicht selbst")

        val labbed = fleet.copy(path = Path.LABOR.id)
        assertTrue(GameEngine.researchSpeed(labbed) > GameEngine.researchSpeed(fleet))
        assertTrue(GameEngine.stats(labbed).offlineEfficiency > GameEngine.stats(fleet).offlineEfficiency)

        val cored = fleet.copy(path = Path.KERN.id)
        assertTrue(GameEngine.fusionRate(cored) > GameEngine.fusionRate(fleet))
    }

    @Test
    fun `no path is simply better than another at everything`() {
        val fleet = GameState(collectors = mapOf("dust" to 100), runMass = 1e12)

        // Every path has to be beaten by some other path at something, or it is a trap.
        for (path in Path.entries) {
            val mine = fleet.copy(path = path.id)
            val beatenSomewhere = Path.entries.any { other ->
                if (other == path) return@any false
                val theirs = fleet.copy(path = other.id)
                GameEngine.massPerSecond(theirs) > GameEngine.massPerSecond(mine) ||
                    GameEngine.massPerTap(theirs) > GameEngine.massPerTap(mine) ||
                    GameEngine.researchSpeed(theirs) > GameEngine.researchSpeed(mine) ||
                    GameEngine.fusionRate(theirs) > GameEngine.fusionRate(mine)
            }
            assertTrue(beatenSomewhere, "${path.label} ist nirgends schlechter als die anderen")
        }
    }

    @Test
    fun `a save keeps the lean`() {
        val state = GameState(path = Path.MASCHINE.id, bigBangs = 3)
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertEquals(Path.MASCHINE, Path.of(back))
    }

    @Test
    fun `a save drops a path that no longer exists`() {
        val state = GameState(path = "path_wormhole", bigBangs = 3)
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertNull(back.path, "Ein unbekannter Weg bleibt im Spielstand stehen")
    }

    @Test
    fun `every path says what it is and what it does`() {
        for (path in Path.entries) {
            assertTrue(path.label.isNotBlank(), path.id)
            assertTrue(path.flavor.isNotBlank(), path.id)
            assertTrue(path.effects.isNotEmpty(), "${path.label} tut nichts")
            assertTrue(path.effectTexts.all { it.isNotBlank() }, path.label)
        }
        assertEquals(
            Path.entries.size,
            Path.entries.map { it.id }.toSet().size,
            "Doppelte Weg-ID",
        )
    }

    /** Nothing here starts a run; a lean that only showed up at a reset would be invisible. */
    @Test
    fun `no path hands out a starting bonus`() {
        for (path in Path.entries) {
            for (effect in path.effects) {
                assertTrue(
                    effect !is PrestigeEffect.StartingMass &&
                        effect !is PrestigeEffect.StartingCollectors,
                    "${path.label} verteilt einen Startbonus statt einer Ausrichtung",
                )
            }
        }
    }
}
