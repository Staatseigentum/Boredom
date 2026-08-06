package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumbersTest {

    @Test
    fun `renders whole numbers below a thousand without decimals`() {
        assertEquals("0", Numbers.format(0.0))
        assertEquals("15", Numbers.format(15.0))
        assertEquals("999", Numbers.format(999.0))
    }

    @Test
    fun `renders fractions with a German decimal comma`() {
        assertEquals("0,10", Numbers.format(0.1))
        assertEquals("12,5", Numbers.format(12.5))
    }

    @Test
    fun `groups large numbers with short scale suffixes`() {
        assertEquals("1,00 K", Numbers.format(1_000.0))
        assertEquals("12,3 K", Numbers.format(12_345.0))
        assertEquals("1,00 Mio", Numbers.format(1e6))
        assertEquals("1,00 Mrd", Numbers.format(1e9))
        assertEquals("8,00 Bio", Numbers.format(8e12))
    }

    @Test
    fun `falls back to scientific notation beyond the suffix table`() {
        val rendered = Numbers.format(1.23e70)
        assertTrue(rendered.contains("e70"), "erwartet wissenschaftliche Notation, war: $rendered")
    }

    @Test
    fun `never renders negatives or NaN as garbage`() {
        assertEquals("0", Numbers.format(-5.0))
        assertEquals("0", Numbers.format(Double.NaN))
        assertEquals("∞", Numbers.format(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `formats durations in German units`() {
        assertEquals("30 Sek", Numbers.formatDuration(30))
        assertEquals("5 Min", Numbers.formatDuration(300))
        assertEquals("2 Std", Numbers.formatDuration(7_200))
        assertEquals("2 Std 30 Min", Numbers.formatDuration(9_000))
        assertEquals("1 Tage 2 Std", Numbers.formatDuration(93_600))
    }

    @Test
    fun `formats multipliers and percentages`() {
        assertEquals("×2", Numbers.formatMultiplier(2.0))
        assertEquals("×2,50", Numbers.formatMultiplier(2.5))
        assertEquals("15 %", Numbers.formatPercent(0.15))
        assertEquals("1,5 %", Numbers.formatPercent(0.015))
    }
}
