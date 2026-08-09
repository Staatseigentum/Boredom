package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The small tree hanging off each of the four universes. */
class PathTreeTest {

    private val now = 1_700_000_000_000L

    private fun aligned(path: Path, aeons: Double = 50.0) = GameState(
        path = path.id,
        aeons = aeons,
        bigBangs = 1,
        collectors = mapOf("dust" to 20),
    )

    @Test
    fun `every path has a tree, and every tree has a root`() {
        for (path in Path.entries) {
            val nodes = PathTrees.of(path.id)
            assertTrue(nodes.size >= 3, "${path.id} hat nur ${nodes.size} Knoten")
            assertEquals(1, nodes.count { it.requires == null }, "${path.id}: nicht genau eine Wurzel")
        }
    }

    @Test
    fun `the root comes first in the list the panel draws`() {
        for (path in Path.entries) {
            assertEquals(null, PathTrees.of(path.id).first().requires, path.id)
        }
    }

    @Test
    fun `no node hangs on a parent from another path`() {
        for (node in PathTrees.all) {
            val parent = PathTrees.byId(node.requires ?: continue)
            assertNotNull(parent, node.id)
            assertEquals(node.pathId, parent.pathId, node.id)
        }
    }

    @Test
    fun `a leaf cannot be bought before its root`() {
        val path = Path.HAND
        val leaf = PathTrees.of(path.id).first { it.requires != null }
        val state = aligned(path)

        assertFalse(PathTrees.canBuy(state, leaf))
        assertEquals(state, GameEngine.buyPathNode(state, leaf.id), "Das Blatt ging ohne Wurzel")

        val withRoot = GameEngine.buyPathNode(state, leaf.requires!!)
        assertTrue(PathTrees.canBuy(withRoot, leaf))
    }

    @Test
    fun `a node of another path cannot be bought`() {
        val state = aligned(Path.HAND)
        val foreign = PathTrees.of(Path.KERN.id).first()

        assertFalse(PathTrees.canBuy(state, foreign))
        assertEquals(state, GameEngine.buyPathNode(state, foreign.id))
    }

    @Test
    fun `buying takes the aeons and records the node`() {
        val path = Path.MASCHINE
        val root = PathTrees.of(path.id).first()
        val state = aligned(path, aeons = 10.0)
        val after = GameEngine.buyPathNode(state, root.id)

        assertTrue(root.id in after.pathNodes)
        assertEquals(10.0 - root.cost, after.aeons, 1e-9)
    }

    @Test
    fun `a node without the aeons behind it is refused`() {
        val path = Path.LABOR
        val root = PathTrees.of(path.id).first()
        val broke = aligned(path, aeons = root.cost - 0.5)

        assertEquals(broke, GameEngine.buyPathNode(broke, root.id))
    }

    @Test
    fun `nothing can be bought twice`() {
        val path = Path.KERN
        val root = PathTrees.of(path.id).first()
        val once = GameEngine.buyPathNode(aligned(path), root.id)
        val twice = GameEngine.buyPathNode(once, root.id)

        assertEquals(once.aeons, twice.aeons, 1e-9, "Der Knoten wurde ein zweites Mal bezahlt")
    }

    /** The whole point: bought stays bought, but says nothing under another alignment. */
    @Test
    fun `a node is silent under a different path`() {
        val path = Path.MASCHINE
        val root = PathTrees.of(path.id).first()
        val bought = GameEngine.buyPathNode(aligned(path), root.id)

        assertTrue(PathTrees.effects(bought).isNotEmpty())
        assertTrue(PathTrees.effects(bought.copy(path = Path.HAND.id)).isEmpty())

        val here = GameEngine.massPerSecond(bought)
        val elsewhere = GameEngine.massPerSecond(bought.copy(path = Path.HAND.id))
        assertTrue(here > elsewhere, "Der Knoten wirkt auch unter einem fremden Pfad")
    }

    @Test
    fun `a big bang keeps the nodes it did not pay for`() {
        val path = Path.KERN
        val root = PathTrees.of(path.id).first()
        val bought = GameEngine.buyPathNode(aligned(path), root.id)

        val ready = bought.copy(
            collapses = 60,
            runMass = Tiers.last.threshold,
            singularities = 1e9,
        )
        val banged = GameEngine.bigBang(ready, now, Path.HAND.id)

        assertTrue(root.id in banged.pathNodes, "Der Urknall hat den Baum abgeräumt")
        assertEquals(Path.HAND.id, banged.path)
        assertTrue(PathTrees.effects(banged).isEmpty(), "Er wirkt unter der neuen Ausrichtung")
    }

    @Test
    fun `a collapse does not touch the tree`() {
        val path = Path.LABOR
        val root = PathTrees.of(path.id).first()
        val bought = GameEngine.buyPathNode(aligned(path), root.id)
        val after = GameEngine.collapse(bought.copy(runMass = Tiers.last.threshold), now)

        assertEquals(bought.pathNodes, after.pathNodes)
    }

    @Test
    fun `there is no tree before the first big bang`() {
        assertTrue(PathTrees.current(GameState.new(now)).isEmpty())
    }

    @Test
    fun `no two nodes share an id and every effect says something`() {
        val ids = PathTrees.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Doppelte Knoten-ID")
        for (node in PathTrees.all) {
            assertTrue(node.name.isNotBlank(), node.id)
            assertTrue(node.flavor.length > 15, "${node.id}: zu wenig Text")
            assertTrue(node.effectText.isNotBlank(), node.id)
            assertTrue(node.cost > 0.0, node.id)
        }
    }

    /** A root that costs more than a leaf would make the order of the tree a lie. */
    @Test
    fun `every leaf costs at least as much as its root`() {
        for (node in PathTrees.all) {
            val parent = PathTrees.byId(node.requires ?: continue) ?: continue
            assertTrue(node.cost >= parent.cost, "${node.id} ist billiger als seine Wurzel")
        }
    }

    @Test
    fun `the counter on the header only counts the running path`() {
        val path = Path.HAND
        val mixed = aligned(path).copy(
            pathNodes = setOf(
                PathTrees.of(Path.HAND.id).first().id,
                PathTrees.of(Path.KERN.id).first().id,
            ),
        )
        assertEquals(1, PathTrees.ownedIn(mixed, path.id))
        assertEquals(1, PathTrees.ownedIn(mixed, Path.KERN.id))
    }
}
