package com.staatseigentum.kollaps.core.audio

import com.staatseigentum.kollaps.core.BodyKind
import com.staatseigentum.kollaps.core.Tiers
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreTest {

    @Test
    fun `every body has something to sound like`() {
        for (kind in BodyKind.entries) {
            // Would throw on a kind the mapping forgot, which is the point of asking.
            Mood.of(kind)
        }
        val used = BodyKind.entries.map { Mood.of(it) }.toSet()
        assertEquals(
            Mood.entries.toSet(),
            used,
            "Eine Stimmung gehört zu keinem Himmelskörper und ist damit unerreichbar",
        )
    }

    @Test
    fun `every rung of the ladder has music`() {
        for (tier in Tiers.all) {
            val mood = Mood.of(tier.kind)
            assertTrue(mood.label.isNotBlank(), "${tier.name} klingt nach nichts")
        }
    }

    @Test
    fun `the loop is exactly as long as it says`() {
        for (mood in Mood.entries) {
            assertEquals(Score.samplesPerLoop(), Score.render(mood).size, "${mood.label}")
        }
        assertEquals(
            (Score.LOOP_SECONDS * Score.SAMPLE_RATE).toInt(),
            Score.samplesPerLoop(),
        )
    }

    @Test
    fun `rendering the same mood twice gives the same music`() {
        for (mood in Mood.entries) {
            assertTrue(
                Score.render(mood).contentEquals(Score.render(mood)),
                "${mood.label} klingt jedes Mal anders",
            )
        }
    }

    @Test
    fun `two moods are actually two pieces`() {
        val rendered = Mood.entries.map { it to Score.render(it) }
        for ((a, first) in rendered) {
            for ((b, second) in rendered) {
                if (a >= b) continue
                assertTrue(
                    !first.contentEquals(second),
                    "${a.label} und ${b.label} sind dieselbe Aufnahme",
                )
            }
        }
    }

    /**
     * The one thing a loop has to get right.
     *
     * A step at the seam is a click, once every sixteen seconds, for as long as the game is open —
     * the single most noticeable way generated music can be broken. Measured against the loop's
     * own steepest interior moves rather than against an absolute number, because a square wave
     * jumps hard on purpose and a fixed threshold would either fail on that or prove nothing.
     */
    @Test
    fun `the loop meets itself without a step`() {
        for (mood in Mood.entries) {
            val samples = Score.render(mood)
            val interior = IntArray(samples.size - 1) {
                abs(samples[it + 1] - samples[it])
            }
            interior.sort()
            val steepest = interior[(interior.size * 0.999).toInt()]
            val seam = abs(samples.first() - samples.last())

            assertTrue(
                seam <= steepest,
                "${mood.label}: Sprung an der Naht ist $seam, im Stück höchstens $steepest",
            )
        }
    }

    @Test
    fun `a quantised pitch fits the loop a whole number of times`() {
        val total = Score.samplesPerLoop()
        for (hertz in listOf(49.0, 55.0, 65.41, 82.41, 440.0, 1_760.0)) {
            val fitted = Score.quantise(hertz, total)
            val cycles = fitted * total / Score.SAMPLE_RATE

            assertEquals(
                cycles.roundToInt().toDouble(),
                cycles,
                1e-9,
                "$hertz passt nicht ganzzahlig in die Schleife",
            )
            // A sixteenth of a hertz at the low end is about two thousandths of a semitone.
            assertTrue(abs(fitted - hertz) < 0.1, "$hertz wurde auf $fitted verstimmt")
        }
    }

    @Test
    fun `nothing clips and nothing is silent`() {
        for (mood in Mood.entries) {
            val samples = Score.render(mood)
            val peak = samples.maxOf { abs(it.toInt()) }

            assertTrue(peak > 1_000, "${mood.label} ist praktisch still")
            assertTrue(peak <= Short.MAX_VALUE.toInt(), "${mood.label} übersteuert")
            // Well below the cues, which are normalised far higher: this plays for hours.
            assertTrue(peak < 16_000, "${mood.label} ist zu laut fürs Dauerlaufen")
        }
    }

    @Test
    fun `the music is quieter than the sound effects`() {
        val loudestCue = Cue.entries.maxOf { cue -> Chiptune.render(cue).maxOf { abs(it.toInt()) } }
        val loudestMood = Mood.entries.maxOf { mood -> Score.render(mood).maxOf { abs(it.toInt()) } }

        assertTrue(loudestMood < loudestCue, "Die Musik übertönt die Geräusche")
    }

    @Test
    fun `the wav says how many bytes it carries`() {
        for (mood in Mood.entries) {
            val wav = Score.wav(mood)
            assertEquals(
                Wav.HEADER_BYTES + Score.samplesPerLoop() * 2,
                wav.size,
                "${mood.label}",
            )
            assertEquals("RIFF", wav.decodeToString(0, 4))
            assertEquals("WAVE", wav.decodeToString(8, 12))

            val declared = (wav[40].toInt() and 0xFF) or
                ((wav[41].toInt() and 0xFF) shl 8) or
                ((wav[42].toInt() and 0xFF) shl 16) or
                ((wav[43].toInt() and 0xFF) shl 24)
            assertEquals(wav.size - Wav.HEADER_BYTES, declared, "Der Header lügt über die Länge")
        }
    }

    @Test
    fun `every mood is built from something playable`() {
        for (mood in Mood.entries) {
            assertTrue(mood.root in 20.0..200.0, "${mood.label}: Grundton ${mood.root} Hz")
            assertTrue(mood.scale.isNotEmpty(), "${mood.label} hat keine Tonleiter")
            assertTrue(mood.scale.first() == 0, "${mood.label} enthält den Grundton nicht")
            assertTrue(mood.scale.all { it in 0..12 }, "${mood.label} verlässt die Oktave")
            assertTrue(mood.density in 0.0..1.0)
            assertTrue(mood.edge in 0.0..1.0)
            assertTrue(mood.octaves >= 1)
        }
    }
}
