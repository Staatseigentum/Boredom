package com.staatseigentum.kollaps.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The three ways of writing a very large number.
 *
 * [Numbers.format] is ambient, so every test here puts it back afterwards — a formatting
 * preference leaking into the next test would be exactly the failure this design invites.
 */
class NumberFormatTest {

    @AfterTest
    fun reset() {
        Numbers.format = NumberFormat.NAMEN
    }

    @Test
    fun `every format agrees below a thousand`() {
        for (value in listOf(0.0, 1.0, 42.0, 420.5, 999.0)) {
            val rendered = NumberFormat.entries.map { Numbers.format(value, it) }
            assertEquals(1, rendered.toSet().size, "$value wird verschieden geschrieben: $rendered")
        }
    }

    @Test
    fun `the names format keeps the names`() {
        assertEquals("1,23 Mrd", Numbers.format(1.23e9, NumberFormat.NAMEN))
        assertEquals("1,00 Mio", Numbers.format(1e6, NumberFormat.NAMEN))
    }

    @Test
    fun `the scientific formats say the exponent`() {
        assertEquals("1,23e9", Numbers.format(1.23e9, NumberFormat.WISSENSCHAFTLICH))
        assertEquals("1,23E9", Numbers.format(1.23e9, NumberFormat.KURZ))
    }

    /** The reason the scientific formats exist: they do not run out of words. */
    @Test
    fun `no format gives up on a number the game can actually reach`() {
        val enormous = 1e63
        for (format in NumberFormat.entries) {
            val rendered = Numbers.format(enormous, format)
            assertTrue(rendered.isNotBlank(), format.name)
            assertTrue(rendered.length < 20, "$format schreibt $rendered")
        }
    }

    @Test
    fun `past the names even the names format falls back to the exponent`() {
        val past = 1e63
        assertTrue(
            Numbers.format(past, NumberFormat.NAMEN).contains("e"),
            "Jenseits der Namen wird etwas anderes behauptet",
        )
    }

    @Test
    fun `the ambient setting is what the plain call uses`() {
        Numbers.format = NumberFormat.KURZ
        assertEquals(Numbers.format(1.23e9, NumberFormat.KURZ), Numbers.format(1.23e9))
        assertNotEquals(Numbers.format(1.23e9, NumberFormat.NAMEN), Numbers.format(1.23e9))
    }

    @Test
    fun `the unit tags follow the setting`() {
        Numbers.format = NumberFormat.WISSENSCHAFTLICH
        assertEquals("1,23e9 kg", Numbers.formatMass(1.23e9))
        assertEquals("1,23e9 kg/s", Numbers.formatRate(1.23e9))
    }

    @Test
    fun `the engine records the choice and applies it at once`() {
        val after = GameEngine.setNumberFormat(GameState.new(0), NumberFormat.KURZ)

        assertEquals(NumberFormat.KURZ.name, after.numberFormat)
        assertEquals(NumberFormat.KURZ, Numbers.format, "Erst beim nächsten Start wirksam")
    }

    @Test
    fun `an unknown or missing name is the German one`() {
        assertEquals(NumberFormat.NAMEN, NumberFormat.byName(null))
        assertEquals(NumberFormat.NAMEN, NumberFormat.byName("GIBTSNICHT"))
    }

    @Test
    fun `the choice survives every reset`() {
        val chosen = GameEngine.setNumberFormat(
            GameState(collapses = 40, singularities = 1e9, runMass = Tiers.last.threshold),
            NumberFormat.KURZ,
        )
        assertEquals(chosen.numberFormat, GameEngine.collapse(chosen, 1).numberFormat)
        assertEquals(chosen.numberFormat, GameEngine.bigBang(chosen, 1).numberFormat)
    }

    @Test
    fun `every option shows an example that is what it does`() {
        for (format in NumberFormat.entries) {
            assertTrue(format.label.isNotBlank(), format.name)
            assertEquals(
                Numbers.format(1.23e9, format),
                format.example,
                "${format.name}: das Beispiel stimmt nicht mit dem Format überein",
            )
        }
    }
}
