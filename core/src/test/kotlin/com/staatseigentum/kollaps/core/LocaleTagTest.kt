package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Language
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The tag a platform hands over, in every spelling the three of them use.
 *
 * Android gives "en", the JVM gives "en", and iOS gives a whole identifier like "en_US" — and
 * that difference cost a round of CI, because the property that would have given the bare code on
 * iOS is not in Kotlin/Native's Foundation bindings at all. Taking the letters off the front of
 * whatever arrives is what makes all three the same problem.
 */
class LocaleTagTest {

    @Test
    fun `every spelling a platform uses lands on the same language`() {
        for (tag in listOf("en", "EN", "en_US", "en-GB", "en_GB.UTF-8")) {
            assertEquals(Language.EN, Language.ofLocale(tag), tag)
        }
        for (tag in listOf("de", "de_DE", "de-AT", "de_CH")) {
            assertEquals(Language.DE, Language.ofLocale(tag), tag)
        }
    }

    /** Anything the game does not speak gets the original rather than a language nobody chose. */
    @Test
    fun `an unknown language falls back to German`() {
        for (tag in listOf("nb_NO", "pt-BR", "ja", "", null, "  ")) {
            assertEquals(Language.DE, Language.ofLocale(tag), tag ?: "null")
        }
    }
}
