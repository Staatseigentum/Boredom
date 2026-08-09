package com.staatseigentum.kollaps.core.i18n

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The translation mechanism, not the translation.
 *
 * What matters here is that nothing can *break* because of it: German is untouched, a missing
 * entry falls back rather than showing a key, and a template keeps its values.
 */
class LangTest {

    @AfterTest
    fun reset() {
        Lang.current = Language.DE
    }

    @Test
    fun `german is handed straight back`() {
        Lang.current = Language.DE
        val sentence = "Ein Klumpen Gestein, der niemandem gehört."
        assertEquals(sentence, Lang.t(sentence))
    }

    @Test
    fun `a missing translation falls back to german instead of a key`() {
        Lang.current = Language.EN
        val untranslated = "Ein Satz, den niemand übersetzt hat."
        assertEquals(untranslated, Lang.t(untranslated))
    }

    @Test
    fun `a translation that exists is used`() {
        Lang.current = Language.EN
        assertEquals("Collectors", Lang.t("Kollektoren"))
    }

    @Test
    fun `a template keeps its values in both languages`() {
        for (language in Language.entries) {
            Lang.current = language
            val rendered = Lang.t("Alle %s Stück: %s mehr", 25, "15 %")
            assertTrue("25" in rendered, "$language: die Zahl fehlt")
            assertTrue("15 %" in rendered, "$language: der Wert fehlt")
        }
    }

    @Test
    fun `an unknown language id is german`() {
        assertEquals(Language.DE, Language.byId(null))
        assertEquals(Language.DE, Language.byId("kl"))
        assertEquals(Language.EN, Language.byId("en"))
    }

    @Test
    fun `no translation is left blank`() {
        for ((german, english) in Translations.EN) {
            assertTrue(english.isNotBlank(), "Leere Übersetzung für: $german")
        }
    }

    @Test
    fun `the coverage check reports what is missing`() {
        val texts = listOf("Kollektoren", "Etwas völlig Unübersetztes")
        assertEquals(listOf("Etwas völlig Unübersetztes"), Lang.missing(texts))
        assertTrue(Lang.isComplete(listOf("Kollektoren")))
    }
}
