package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTest {

    @Test
    fun `case and umlauts do not decide whether something is found`() {
        assertEquals("erdol", Search.fold("Erdöl"))
        assertEquals("erdol", Search.fold("  ERDÖL  "))
        assertEquals("strasse", Search.fold("Straße"))
        assertEquals("uber uns", Search.fold("Über uns"))
    }

    @Test
    fun `an empty search finds everything`() {
        val upgrade = Upgrades.all.first()
        assertTrue(upgrade.matches(""))
        assertTrue(upgrade.matches("   "))
    }

    @Test
    fun `an upgrade is found by its own name`() {
        for (upgrade in Upgrades.all) {
            assertTrue(upgrade.matches(upgrade.name), "${upgrade.id} findet sich selbst nicht")
        }
    }

    /**
     * The reason the effect line is searched at all.
     *
     * "Offline" is a word the player knows from the effect text; it appears in no upgrade name.
     * A search over names alone would answer "nichts gefunden" to the most obvious query in the
     * whole shop.
     */
    @Test
    fun `a word that only appears in the effect still finds the upgrade`() {
        val offline = Upgrades.all.filter { it.group == UpgradeGroup.OFFLINE }
        assertTrue(offline.isNotEmpty(), "Keine Offline-Upgrades zum Prüfen")
        for (upgrade in offline) {
            assertTrue(upgrade.matches("offline"), "${upgrade.id} nicht über die Wirkung findbar")
        }
    }

    @Test
    fun `a search nobody could mean finds nothing`() {
        val nonsense = "qzxwvyjkq"
        assertTrue(Upgrades.all.none { it.matches(nonsense) })
    }

    @Test
    fun `searching narrows rather than reorders`() {
        val all = Upgrades.all
        val hits = all.filter { it.matches("tipp") }
        assertTrue(hits.size < all.size, "Die Suche hat nichts ausgeschlossen")
        assertTrue(hits.isNotEmpty(), "Die Suche hat alles ausgeschlossen")
        assertEquals(hits, all.filter { it in hits }, "Die Reihenfolge hat sich verschoben")
    }

    @Test
    fun `typing without the umlaut still finds the word with it`() {
        val withUmlaut = Upgrades.all.filter { upgrade ->
            listOf(upgrade.name, upgrade.flavor, upgrade.effectText)
                .any { text -> text.any { it in "äöüß" } }
        }
        assertTrue(withUmlaut.isNotEmpty(), "Kein Upgrade mit Umlaut — der Test prüft nichts")

        for (upgrade in withUmlaut) {
            val plain = Search.fold(upgrade.name)
            assertTrue(upgrade.matches(plain), "${upgrade.id} nicht ohne Umlaut findbar")
        }
    }

    @Test
    fun `a group and a search can be combined`() {
        val tapHits = Upgrades.all.filter { it.group == UpgradeGroup.TAP && it.matches("e") }
        assertTrue(tapHits.all { it.group == UpgradeGroup.TAP })
        assertFalse(tapHits.isEmpty(), "Kein Tipp-Upgrade enthält ein E")
    }
}
