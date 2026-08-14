package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The lab's second storey, and the thing that makes a new effect worth adding: that it does something.
 *
 * A `PrestigeEffect` is a data class, a branch in a fold and a line of text. All three can be
 * written, compile, ship and change no number at all — the fold branch can be dropped, or the field
 * it writes can be read by nobody. Every one of these asks the same question in the same shape:
 * take a save, measure, grant the project, measure again, and require the two to differ in the
 * direction the card promises.
 */
class LabSecondStoreyTest {

    private val now = 1_700_000_000_000L

    /** A save deep enough that all four systems are actually running. */
    private fun deep(): GameState = GameState(
        mass = 1e24,
        runMass = Tiers.last.threshold * 10,
        bestTier = Tiers.last.index,
        collectors = Collectors.all.take(6).associate { it.id to 200 },
        collapses = 40,
        bigBangs = Multiverse.SLOTS,
        singularities = 4_000.0,
        orbits = 6,
        satellites = (0 until 6).associateWith { 1e20 },
        universes = (0 until Multiverse.SLOTS).map { slot ->
            ParkedUniverse(
                slot = slot,
                bestTier = Tiers.indexOf("Schwarzes Loch"),
                collapses = 25,
                singularities = 3_000.0,
                level = Multiverse.MAX_LEVEL,
            )
        },
        fusers = Fusion.stages.associate { it.id to 20 },
        // Iron in the tanks, not merely furnaces that could make some: the forge reads the pile,
        // and a state with ovens and nothing in them forges exactly nothing.
        elements = mapOf(Element.EISEN.id to 250_000.0),
        contractDay = Contract.dayOf(now),
    )

    private fun withProject(id: String): GameState {
        // Everything it stands on as well: a project whose requirements are missing is not offered,
        // and one granted straight into the set would be testing an unreachable state.
        val chain = mutableSetOf<String>()
        fun pull(current: String) {
            val project = ResearchTree.byId(current) ?: return
            project.requires.forEach(::pull)
            chain += project.id
        }
        pull(id)
        return deep().copy(research = chain)
    }

    @Test
    fun `the sky weighs more`() {
        val before = GameEngine.massPerSecond(deep())
        val after = GameEngine.massPerSecond(withProject("r_cartography"))
        assertTrue(after > before, "Himmelskartierung ändert nichts: $before zu $after")
    }

    @Test
    fun `the orbits deliver more`() {
        val before = GameEngine.massPerSecond(deep())
        val after = GameEngine.massPerSecond(withProject("r_ephemeris"))
        assertTrue(after > before, "Ephemeriden ändern nichts: $before zu $after")
    }

    @Test
    fun `a collapse forges more metal`() {
        val plain = GameEngine.collapse(deep(), now)
        val learned = GameEngine.collapse(withProject("r_transmutation"), now)
        val before = plain.heavy.values.sum()
        val after = learned.heavy.values.sum()
        assertTrue(before > 0.0, "Der Aufbau schmiedet gar nichts")
        assertTrue(after > before, "Transmutation ändert nichts: $before zu $after")
    }

    @Test
    fun `a contract pays the handling fee`() {
        val contract = Contract.all.first { it.dailyLimit != null }
        // Placed on the table and already finished, so claiming is the only step left.
        fun ready(state: GameState) = state.copy(
            contracts = listOf(contract.id),
            contractMarks = mapOf(contract.id to -contract.target),
        )

        val plain = GameEngine.claimContract(ready(deep()), contract.id)
        val learned = GameEngine.claimContract(ready(withProject("r_bureau")), contract.id)
        assertEquals(contract.reward, plain.aeons - deep().aeons, 1e-9)
        assertTrue(
            learned.aeons > plain.aeons,
            "Der Verwaltungsapparat nimmt keine Gebühr: ${plain.aeons} zu ${learned.aeons}",
        )
    }

    @Test
    fun `the new storey stands on the old one and nothing dangles`() {
        val added = listOf(
            "r_cartography", "r_ephemeris", "r_transmutation",
            "r_bureau", "r_survey", "r_recursion",
        )
        for (id in added) {
            val project = ResearchTree.byId(id)
            assertTrue(project != null, "$id fehlt im Katalog")
            assertTrue(project.requires.isNotEmpty(), "$id hängt an nichts und erscheint sofort")
        }

        // Nothing new is reachable before the first storey is finished — otherwise the tree would
        // offer a project about the sky to somebody who has never seen one.
        val fresh = GameState()
        val open = ResearchTree.offered(fresh).map { it.id }
        assertTrue(added.none { it in open }, "Ein Projekt der zweiten Etage liegt sofort offen")
    }

    @Test
    fun `every effect the catalogue uses says what it does`() {
        // A card with an empty line under the name is a card nobody can judge.
        for (project in ResearchTree.all) {
            assertTrue(
                project.effectText.isNotBlank(),
                "${project.name} beschreibt seine Wirkung nicht",
            )
        }
    }
}
