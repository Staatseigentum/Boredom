package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
import com.staatseigentum.kollaps.core.i18n.Language
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The numbers, which is where a translation gets caught out.
 *
 * Everything else in this game is prose, and prose that is still German merely looks unfinished.
 * A number that is still German looks *wrong*: "1,23" reads as one and a bit to a German and as a
 * list to everybody else, and a "Billion" is out by a factor of a thousand.
 */
class NumbersLanguageTest {

    @AfterTest
    fun german() {
        Lang.current = Language.DE
        Numbers.format = NumberFormat.NAMEN
    }

    private fun <T> inEnglish(block: () -> T): T {
        Lang.current = Language.EN
        try {
            return block()
        } finally {
            Lang.current = Language.DE
        }
    }

    @Test
    fun `the decimal separator follows the language`() {
        // Two decimals below ten, which is what [Numbers.formatBelowThousand] does — the point of
        // this test is the character between them, not how many there are.
        Lang.current = Language.DE
        assertEquals("1,50", Numbers.format(1.5))
        inEnglish { assertEquals("1.50", Numbers.format(1.5)) }
    }

    /**
     * The false friend, written down so it cannot come back.
     *
     * German counts in the long scale — Million, Milliarde, Billion — and English in the short
     * one. 10⁹ is a Milliarde and a billion; 10¹² is a Billion and a trillion. Translating the
     * word instead of the system would make every large number in the game overstate itself by a
     * factor of a thousand, and it would look plausible the whole way.
     */
    @Test
    fun `a German Billion is an English trillion, not a billion`() {
        Lang.current = Language.DE
        assertTrue(Numbers.format(1e9).endsWith("Mrd"), Numbers.format(1e9))
        assertTrue(Numbers.format(1e12).endsWith("Bio"), Numbers.format(1e12))

        inEnglish {
            assertTrue(Numbers.format(1e9).endsWith("B"), Numbers.format(1e9))
            assertTrue(Numbers.format(1e12).endsWith("T"), Numbers.format(1e12))
            // The one that would be wrong if the words had been translated one for one.
            assertTrue(
                !Numbers.format(1e9).contains("T"),
                "10⁹ wird als Trillion geschrieben — die Skalen wurden übersetzt statt ersetzt",
            )
        }
    }

    @Test
    fun `the suffix is spaced in German and joined in English`() {
        Lang.current = Language.DE
        assertEquals("1,23 Mrd", Numbers.format(1.23e9))
        inEnglish { assertEquals("1.23B", Numbers.format(1.23e9)) }
    }

    @Test
    fun `percentages follow the local spacing`() {
        Lang.current = Language.DE
        assertEquals("15 %", Numbers.formatPercent(0.15))
        inEnglish { assertEquals("15%", Numbers.formatPercent(0.15)) }
    }

    @Test
    fun `durations are words in German and symbols in English`() {
        Lang.current = Language.DE
        assertEquals("2 Std 14 Min", Numbers.formatDuration(2 * 3600 + 14 * 60))
        inEnglish { assertEquals("2 h 14 min", Numbers.formatDuration(2 * 3600 + 14 * 60)) }
    }

    /** The unit of mass is SI and belongs to nobody's language. */
    @Test
    fun `kilograms stay kilograms`() = inEnglish {
        assertTrue(Numbers.formatMass(1000.0).endsWith(" kg"), Numbers.formatMass(1000.0))
        assertTrue(Numbers.formatRate(1000.0).endsWith(" kg/s"), Numbers.formatRate(1000.0))
    }

    /**
     * The example under each setting is generated, so it cannot disagree with the setting.
     *
     * It used to be a hand-written string, which was right in German and would have stayed right
     * in German for ever, next to a number that had stopped looking like it.
     */
    @Test
    fun `each format shows itself correctly in both languages`() {
        Lang.current = Language.DE
        assertEquals("1,23 Mrd", NumberFormat.NAMEN.example)
        assertEquals("1,23e9", NumberFormat.WISSENSCHAFTLICH.example)

        inEnglish {
            assertEquals("1.23B", NumberFormat.NAMEN.example)
            assertEquals("1.23e9", NumberFormat.WISSENSCHAFTLICH.example)
        }
    }

    /** And the scientific formats are the same everywhere except for the separator. */
    @Test
    fun `the exponent formats do not change their marker`() = inEnglish {
        assertEquals("1.23e9", Numbers.format(1.23e9, NumberFormat.WISSENSCHAFTLICH))
        assertEquals("1.23E9", Numbers.format(1.23e9, NumberFormat.KURZ))
    }

    /** Every rung of the naming ladder has to exist in both, or a big number falls back to `e`. */
    @Test
    fun `both scales are the same length`() {
        var power = 1e3
        val german = mutableListOf<String>()
        val english = mutableListOf<String>()
        repeat(19) {
            Lang.current = Language.DE
            german += Numbers.format(power, NumberFormat.NAMEN)
            Lang.current = Language.EN
            english += Numbers.format(power, NumberFormat.NAMEN)
            power *= 1000.0
        }
        Lang.current = Language.DE
        // Not `contains("e")`: two of the German names are "Sep" and "Sex". What marks a fallback
        // is an `e` with the exponent's digits behind it.
        val fellBack = Regex("""e\d+$""")
        assertTrue(german.none { fellBack.containsMatchIn(it) }, "Deutsch fällt zurück: $german")
        assertTrue(english.none { fellBack.containsMatchIn(it) }, "Englisch fällt zurück: $english")
        assertEquals(english.size, english.toSet().size, "Zwei Stufen teilen sich ein Kürzel")
    }
}
