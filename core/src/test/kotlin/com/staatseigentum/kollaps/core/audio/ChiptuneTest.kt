package com.staatseigentum.kollaps.core.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The synthesised sound effects.
 *
 * Nobody can listen to a unit test, so these check the things that make a sound file broken
 * rather than the things that make it good: a header a decoder will accept, a length in the right
 * range, an amplitude that neither clips nor whispers, and no step at the start that would turn
 * every note into a click.
 */
class ChiptuneTest {

    private fun header(wav: ByteArray, at: Int, length: Int): String =
        wav.copyOfRange(at, at + length).map { it.toInt().toChar() }.joinToString("")

    private fun int32(wav: ByteArray, at: Int): Int =
        (wav[at].toInt() and 0xFF) or
            ((wav[at + 1].toInt() and 0xFF) shl 8) or
            ((wav[at + 2].toInt() and 0xFF) shl 16) or
            ((wav[at + 3].toInt() and 0xFF) shl 24)

    @Test
    fun `every cue produces a well formed wav file`() {
        for (cue in Cue.entries) {
            val wav = Chiptune.wav(cue)

            assertEquals("RIFF", header(wav, 0, 4), "$cue")
            assertEquals("WAVE", header(wav, 8, 4), "$cue")
            assertEquals("fmt ", header(wav, 12, 4), "$cue")
            assertEquals("data", header(wav, 36, 4), "$cue")

            // The two sizes in the header have to describe the file that is actually there,
            // or a decoder either truncates the sound or reads past the end of it.
            assertEquals(wav.size - 8, int32(wav, 4), "$cue: RIFF-Größe")
            assertEquals(wav.size - 44, int32(wav, 40), "$cue: Datengröße")
            assertEquals(Chiptune.SAMPLE_RATE, int32(wav, 24), "$cue: Abtastrate")
        }
    }

    @Test
    fun `cues are short enough to fire during play`() {
        for (cue in Cue.entries) {
            val seconds = Chiptune.render(cue).size.toDouble() / Chiptune.SAMPLE_RATE
            assertTrue(seconds > 0.05, "$cue ist mit $seconds s zu kurz zum Hören")
            assertTrue(seconds < 1.2, "$cue blockiert mit $seconds s zu lange")
        }
        // The one that fires on every shop row has to be the shortest by a clear margin.
        assertTrue(
            Chiptune.render(Cue.PURCHASE).size < Chiptune.render(Cue.LEVEL_UP).size / 3,
            "Der Kaufton ist nicht deutlich kürzer als der Stufenaufstieg",
        )
    }

    @Test
    fun `cues use the range without clipping it`() {
        for (cue in Cue.entries) {
            val peak = Chiptune.render(cue).maxOf { abs(it.toInt()) }
            assertTrue(peak > 20_000, "$cue ist mit Spitze $peak zu leise")
            assertTrue(peak < 32_000, "$cue übersteuert mit Spitze $peak")
        }
    }

    @Test
    fun `no cue starts with a step, which would be a click on top of the note`() {
        for (cue in Cue.entries) {
            val samples = Chiptune.render(cue)
            val firstMillisecond = Chiptune.SAMPLE_RATE / 1_000
            val opening = samples.take(firstMillisecond).maxOf { abs(it.toInt()) }
            val peak = samples.maxOf { abs(it.toInt()) }
            assertTrue(
                opening < peak / 3,
                "$cue setzt mit $opening von $peak ein — das knackt",
            )
        }
    }

    @Test
    fun `every cue ends near silence instead of being cut off`() {
        for (cue in Cue.entries) {
            val samples = Chiptune.render(cue)
            val lastMillisecond = Chiptune.SAMPLE_RATE / 1_000
            val ending = samples.takeLast(lastMillisecond).maxOf { abs(it.toInt()) }
            val peak = samples.maxOf { abs(it.toInt()) }
            assertTrue(ending < peak / 4, "$cue bricht bei $ending von $peak ab")
        }
    }

    @Test
    fun `rendering is deterministic, so the cached file never goes stale`() {
        for (cue in Cue.entries) {
            assertTrue(
                Chiptune.wav(cue).contentEquals(Chiptune.wav(cue)),
                "$cue klingt bei jedem Aufruf anders",
            )
        }
    }
}
