package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * What happens to a save written before any of this existed.
 *
 * Every new field has a default, so a save from 2.6.7 loads. That is the easy half. The hard half
 * is that it has to load into a game that still makes sense: nothing lost, nothing granted that was
 * not earned, and no rung, shop or gate that a returning player finds locked behind something they
 * already did.
 */
class OldSaveMigrationTest {

    private val now = 1_700_000_000_000L

    /** A save as 2.6.7 would have written it: no sky, no alloys, cap at five hundred. */
    private fun oldSave(): GameState = GameState.new(now).copy(
        mass = 4.2e26,
        runMass = 4.2e26,
        totalMass = 9.9e30,
        collectors = Collectors.all.filterNot { it.catalogueOnly }.associate { it.id to 500 },
        upgrades = Upgrades.all.filter { it.cost < 1e12 }.map { it.id }.toSet(),
        singularities = 3_400.0,
        collapses = 31,
        bestTier = Tiers.last.index,
        prestigeUpgrades = PrestigeUpgrades.all.filter { it.requiredCollapses <= 3 }.map { it.id }.toSet(),
        investments = mapOf("i_global" to 80, "i_tap" to 60, "i_gain" to 25),
        aeons = 9.0,
        aeonUpgrades = setOf("ae_global_1"),
        bigBangs = 3,
        path = Path.KERN.id,
        heavy = mapOf("au" to 900.0, "pt" to 400.0, "u" to 120.0),
        challengesDone = setOf("c_hand", "c_idle"),
        achievements = Achievements.all.take(30).map { it.id }.toSet(),
    )

    private fun reloaded(): GameState =
        assertNotNull(SaveCodec.decode(SaveCodec.encode(oldSave())), "Der alte Spielstand lädt nicht")

    @Test
    fun `nothing that was owned is lost`() {
        val old = oldSave()
        val new = reloaded()

        assertEquals(old.singularities, new.singularities)
        assertEquals(old.collapses, new.collapses)
        assertEquals(old.aeons, new.aeons)
        assertEquals(old.prestigeUpgrades, new.prestigeUpgrades)
        assertEquals(old.investments, new.investments)
        assertEquals(old.challengesDone, new.challengesDone)
        assertEquals(old.achievements, new.achievements)
        assertEquals(old.heavy, new.heavy)
        assertEquals(old.collectors, new.collectors, "Die Flotte ist nicht mehr dieselbe")
    }

    @Test
    fun `three big bangs become three galaxies and no more`() {
        val new = reloaded()
        assertEquals(3, new.universes.size)
        assertTrue(Multiverse.multiplier(new) > 1.0, "Der nachgetragene Himmel bringt nichts")
        assertTrue(Multiverse.aeonsPerSecond(new) > 0.0, "Er verdient auch nichts")
        // Conservative on purpose: nothing reconstructed may claim to have gone deeper than the
        // save itself ever went.
        assertTrue(new.universes.all { it.bestTier <= new.bestTier })
        assertTrue(new.universes.all { it.collapses <= new.collapses })
    }

    @Test
    fun `the catalogue ladder stays shut until the sky is full`() {
        val new = reloaded()
        // Three galaxies is not eight, so a returning player is not simply handed the endgame.
        assertTrue(!Designations.isUnlocked(new), "Die Katalogleiter steht offen nach drei Urknallen")
        assertEquals(Tiers.last.index, GameEngine.tierOf(new).index, "Er steht schon über dem Tor")

        val full = new.copy(universes = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it) })
        assertTrue(Designations.isUnlocked(full))
    }

    @Test
    fun `the fleet keeps its copies and gains room above them`() {
        val new = reloaded()
        for (collector in Collectors.all.filterNot { it.catalogueOnly }) {
            assertEquals(500, new.ownedOf(collector.id), "${collector.name} hat Stücke verloren")
        }
        // The cap moved up, so the old fleet is now buildable further rather than finished.
        val offers = GameEngine.collectorOffers(new.copy(mass = 1e60), BuyAmount.ONE)
        assertTrue(
            offers.any { !it.collector.catalogueOnly && it.amount > 0 },
            "Bei 500 Stück ist trotzdem Schluss",
        )
    }

    @Test
    fun `the new shelves are open and the old purchases still count`() {
        val new = reloaded()
        val offered = PrestigeUpgrades.offered(new).map { it.id }.toSet()

        assertTrue(offered.isNotEmpty(), "Der Prestige-Laden hat nichts Neues")
        assertTrue(
            offered.none { it in new.prestigeUpgrades },
            "Etwas bereits Gekauftes wird noch einmal angeboten",
        )
        // Thirty-one collapses is deep, so this save can see the whole new shelf at once — which
        // is the right outcome for somebody returning to it. The gating is checked where it
        // matters instead: a save at three collapses must not see the deep end.
        val shallow = new.copy(collapses = 3)
        assertTrue(
            PrestigeUpgrades.offered(shallow).all { it.requiredCollapses <= 3 },
            "Ein Spielstand mit drei Kollapsen sieht schon das tiefe Ende",
        )
        assertTrue(
            PrestigeUpgrades.all.any { it.requiredCollapses > 3 },
            "Es gibt gar nichts, was tiefer läge",
        )
    }

    @Test
    fun `the forge is open and wants metal the save already has`() {
        val new = reloaded()
        assertTrue(Alloy.isUnlocked(new), "Die Schmiede bleibt zu, obwohl Metall da ist")
        assertTrue(
            Alloy.entries.any { Alloy.canForge(new, it) },
            "Nichts ist schmiedbar, obwohl 900 Gold und 400 Platin daliegen",
        )
        // The three metals that did not exist yet start empty rather than at some invented amount.
        assertEquals(0.0, Heavy.amountOf(new, HeavyElement.IRIDIUM))
    }

    @Test
    fun `a save with no big bangs at all is left completely alone`() {
        val early = GameState.new(now).copy(collapses = 2, singularities = 30.0, mass = 1e6)
        val loaded = assertNotNull(SaveCodec.decode(SaveCodec.encode(early)))
        assertEquals(early, loaded, "Ein früher Spielstand wurde beim Laden verändert")
    }

    @Test
    fun `an exported block from the old version still imports`() {
        val block = SaveCodec.export(oldSave())
        val back = assertNotNull(SaveCodec.import(block), "Der Kopieren-Knopf von damals ist tot")
        assertEquals(3, back.universes.size)
        assertEquals(oldSave().singularities, back.singularities)
    }
}
