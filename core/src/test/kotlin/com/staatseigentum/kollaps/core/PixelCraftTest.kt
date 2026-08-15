package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.pixel.PixelCraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The fleet sprites are written as pictures — seven rows of seven characters each — and a picture
 * typed by hand is a picture with a hole in it sooner or later. Nothing on screen would say so: a
 * short row silently becomes a transparent column down one side of a craft, which reads as art.
 */
class PixelCraftTest {

    @Test
    fun `every shape is square and complete`() {
        for (shape in 0 until PixelCraft.COUNT) {
            val pixels = PixelCraft.pixels(shape, ACCENT)
            assertEquals(
                PixelCraft.SIDE * PixelCraft.SIDE,
                pixels.size,
                "Form $shape hat die falsche Größe",
            )
        }
    }

    @Test
    fun `every shape actually draws something`() {
        for (shape in 0 until PixelCraft.COUNT) {
            val opaque = PixelCraft.pixels(shape, ACCENT).count { it ushr 24 != 0 }
            // A third of the grid. Below that it is a speck, which is exactly what these replace.
            assertTrue(
                opaque > PixelCraft.SIDE * PixelCraft.SIDE / 3,
                "Form $shape ist mit $opaque Pixeln zu leer",
            )
        }
    }

    /** The hull is steel on all of them; the colour a player can name comes from the accent. */
    @Test
    fun `the accent reaches every shape`() {
        for (shape in 0 until PixelCraft.COUNT) {
            val pixels = PixelCraft.pixels(shape, ACCENT)
            assertTrue(
                pixels.any { it == ACCENT },
                "Form $shape trägt die Kennfarbe nirgends",
            )
        }
    }

    /** Two collectors far apart in the catalogue must not arrive as the same picture. */
    @Test
    fun `the shapes differ from one another`() {
        val seen = (0 until PixelCraft.COUNT)
            .map { PixelCraft.pixels(it, ACCENT).toList() }
            .toSet()
        assertEquals(PixelCraft.COUNT, seen.size, "zwei Formen sind identisch")
    }

    /** Asking for a shape past the end wraps rather than throwing: there are more machines. */
    @Test
    fun `an index past the end still draws a craft`() {
        val wrapped = PixelCraft.pixels(PixelCraft.COUNT + 2, ACCENT).toList()
        assertEquals(PixelCraft.pixels(2, ACCENT).toList(), wrapped)
    }

    private companion object {
        const val ACCENT = 0xFF5CE1A6.toInt()
    }
}
