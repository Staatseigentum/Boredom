package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
import com.staatseigentum.kollaps.core.i18n.Language
import com.staatseigentum.kollaps.core.i18n.Texts
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How far the English version has got, said as a number rather than as a feeling.
 *
 * A translation is the one kind of work where "I clicked through it and saw nothing German" is
 * indistinguishable from "I did not open that panel". This test opens all of them: [Texts] walks
 * every content catalogue the game owns and hands over every string it can draw, and what is
 * measured here is how much of that has an English side.
 *
 * ## Why it reports rather than fails, for now
 *
 * The bar is deliberately a floor that ratchets up rather than "everything, or red". A test that
 * failed on the first untranslated string would have been red from the moment the catalogue
 * existed and would have taught everyone to ignore it — and the useful signal during the work is
 * not "not finished" but *how much is left, and which*. [LEAST_TRANSLATED] is raised as batches
 * land; when it reaches every text, the switch in the settings goes in and the floor becomes a
 * ceiling. See `Lang.isComplete`, which is what the settings screen will ask.
 */
class LanguageCoverageTest {

    /** No test may leave the game speaking something the next one does not expect. */
    @AfterTest
    fun german() {
        Lang.current = Language.DE
    }

    /**
     * The floor, raised as translation batches land. Never lowered.
     *
     * Lowering it would mean a text stopped being translated — which happens exactly one way, by
     * somebody changing a German string and not its English side. That is the failure this number
     * is here to catch, and it is silent in every other way: the fallback quietly serves the German.
     */
    private val LEAST_TRANSLATED = 90

    @Test
    fun `the catalogues are actually being read`() {
        // A collector that returned nothing would make every other test in this file pass.
        assertTrue(Texts.all.size > 500, "Nur ${Texts.all.size} Texte gefunden — der Sammler greift zu kurz")
    }

    @Test
    fun `no text is listed twice with two meanings`() {
        // The German is the key, so two identical German strings share one English one. That is
        // the cost of keying on the text itself and it is only safe while it stays deliberate.
        assertEquals(Texts.all.size, Texts.all.toSet().size, "Doppelte Texte im Sammler")
    }

    @Test
    fun `the translation only ever grows`() {
        val translated = Texts.all.size - Texts.missing.size
        assertTrue(
            translated >= LEAST_TRANSLATED,
            "Übersetzt sind $translated von ${Texts.all.size}; die Untergrenze ist " +
                "$LEAST_TRANSLATED. Wenn das nach einer Änderung fällt, wurde ein deutscher " +
                "Text angefasst und seine englische Seite nicht.",
        )
    }

    /** Everything that is translated has to actually come out in English. */
    @Test
    fun `switching the language changes what is read`() {
        val german = Texts.all.first { it !in Texts.missing }
        Lang.current = Language.EN
        assertTrue(Lang.t(german) != german, "»$german« ist übersetzt, kommt aber deutsch heraus")
    }

    /** And anything not yet translated has to fall back rather than disappear. */
    @Test
    fun `an untranslated text falls back to German instead of breaking`() {
        Lang.current = Language.EN
        for (text in Texts.missing.take(50)) {
            assertEquals(text, Lang.t(text), "Ein fehlender Text kam nicht als Deutsch zurück")
        }
    }

    /**
     * What is left, printed rather than asserted.
     *
     * The list is the work order. Run this test alone to see it — it is why the batches can be
     * done in any order without anybody keeping a spreadsheet.
     */
    @Test
    fun `report what is left`() {
        val missing = Texts.missing
        println("--- Übersetzung: ${Texts.all.size - missing.size} von ${Texts.all.size}")
        println("--- Es fehlen ${missing.size}. Noch nicht angebunden: ${Texts.PENDING.size} Kataloge")
        for (line in Texts.PENDING) println("    offen: $line")
        for (text in missing.take(20)) println("    fehlt: $text")
        assertTrue(true)
    }
}
