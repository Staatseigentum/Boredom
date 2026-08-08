package com.staatseigentum.kollaps.core.audio

import com.staatseigentum.kollaps.core.BodyKind
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A piece of music, one per kind of body.
 *
 * Tied to [BodyKind] rather than to the tier because that is where the music has something to say:
 * twenty-five separate pieces would be twenty-four changes nobody notices, while the seven changes
 * here all land on a moment that already matters — rock becoming a world, a world becoming a gas
 * giant, a giant catching fire, a star collapsing into what is left of it.
 */
enum class Mood(
    val label: String,
    /** Hertz of the tonic, low: this is the drone the rest is built over. */
    val root: Double,
    /** Semitones above the root that the melody may use. */
    val scale: IntArray,
    /** Share of the sixteen steps that sound at all. Sparse is the default; this is background. */
    val density: Double,
    /** How many octaves above the root the melody reaches. */
    val octaves: Int,
    /** How loud the drone sits under everything. */
    val droneGain: Double,
    /** How square the melody is. Zero is a pure sine, one is a full square wave. */
    val edge: Double,
    /** Length of a melody note as a share of one step. */
    val sustain: Double,
) {
    /** Rock: almost nothing. A low fifth and the occasional stone falling. */
    STAUB(
        label = "Staub",
        root = 65.41, // C2
        scale = intArrayOf(0, 3, 5, 7, 10),
        density = 0.30,
        octaves = 2,
        droneGain = 0.45,
        edge = 0.15,
        sustain = 0.8,
    ),

    /** A world with weather on it: warmer, and the first thing here that sounds like a tune. */
    WASSER(
        label = "Wasser",
        root = 73.42, // D2
        scale = intArrayOf(0, 2, 4, 7, 9),
        density = 0.45,
        octaves = 2,
        droneGain = 0.40,
        edge = 0.25,
        sustain = 1.1,
    ),

    /** Gas giants: wide intervals, a lydian fourth, everything in slow motion. */
    STURM(
        label = "Sturm",
        root = 61.74, // B1
        scale = intArrayOf(0, 2, 6, 7, 11),
        density = 0.40,
        octaves = 3,
        droneGain = 0.50,
        edge = 0.35,
        sustain = 1.4,
    ),

    /** A star: bright, busy, and the loudest the game gets. */
    FEUER(
        label = "Feuer",
        root = 82.41, // E2
        scale = intArrayOf(0, 4, 7, 9, 11),
        density = 0.62,
        octaves = 3,
        droneGain = 0.35,
        edge = 0.55,
        sustain = 0.7,
    ),

    /** What is left of one: high, thin, long gaps. */
    ASCHE(
        label = "Asche",
        root = 98.00, // G2
        scale = intArrayOf(0, 5, 7, 12),
        density = 0.22,
        octaves = 3,
        droneGain = 0.25,
        edge = 0.10,
        sustain = 2.2,
    ),

    /** Neutron stars and magnetars: a beat rather than a melody. */
    PULS(
        label = "Puls",
        root = 55.00, // A1
        scale = intArrayOf(0, 1, 7, 8),
        density = 0.70,
        octaves = 2,
        droneGain = 0.55,
        edge = 0.75,
        sustain = 0.35,
    ),

    /** The end of the ladder: two notes that do not agree, and a lot of room. */
    HORIZONT(
        label = "Horizont",
        root = 49.00, // G1
        scale = intArrayOf(0, 6, 11),
        density = 0.20,
        octaves = 2,
        droneGain = 0.65,
        edge = 0.20,
        sustain = 2.6,
    ),
    ;

    companion object {
        fun of(kind: BodyKind): Mood = when (kind) {
            BodyKind.ROCK -> STAUB
            BodyKind.TERRESTRIAL -> WASSER
            BodyKind.GAS -> STURM
            BodyKind.STAR -> FEUER
            BodyKind.REMNANT -> ASCHE
            BodyKind.EXOTIC -> PULS
            BodyKind.SINGULARITY -> HORIZONT
        }
    }
}

/**
 * The background music, generated the same way everything else here is.
 *
 * A single loop per mood rather than an endless stream: a stream would need a synthesiser running
 * on a thread for as long as the app is open, to produce something the listener cannot tell apart
 * from a loop of the same material. The loop is rendered once, handed to the platform, and
 * repeated until the body changes kind.
 *
 * Seamlessness is the one thing a loop has to get right, and it is got right twice over here. The
 * drone is quantised to a whole number of cycles in the loop, so its waveform meets itself at the
 * seam; and melody notes are written with the sample index wrapped, so a note that starts near the
 * end continues over the beginning rather than being cut off.
 */
object Score {

    const val SAMPLE_RATE = Chiptune.SAMPLE_RATE

    /** Long enough not to feel like a loop, short enough to render in a blink and hold in memory. */
    const val LOOP_SECONDS = 16.0

    /** Melody positions in one loop. Sixteen over sixteen seconds is one note a second at most. */
    const val STEPS = 16

    /** Well below the cues: this plays for hours and they do not. */
    private const val PEAK = 9_000.0

    private const val ATTACK_SECONDS = 0.04

    fun samplesPerLoop(): Int = (LOOP_SECONDS * SAMPLE_RATE).toInt()

    /** The mood as a playable, loopable WAV file. */
    fun wav(mood: Mood): ByteArray = Wav.riff(render(mood), SAMPLE_RATE)

    /** Raw samples, exposed so a test can look at the waveform without parsing a header. */
    fun render(mood: Mood): ShortArray {
        val total = samplesPerLoop()
        val out = DoubleArray(total)

        drone(out, mood, total)
        melody(out, mood, total)

        return Wav.normalise(out, PEAK)
    }

    /**
     * The root and its fifth, held for the whole loop.
     *
     * Both are quantised so that a whole number of cycles fits the loop exactly. Without that the
     * waveform arrives at the seam part way through a cycle and jumps to wherever it started —
     * a step in the signal, which is a click, once every sixteen seconds, forever.
     */
    private fun drone(out: DoubleArray, mood: Mood, total: Int) {
        // The swell is on a whole number of cycles too, so it meets itself at the seam as well.
        val swellStep = 2.0 * PI * quantise(SWELL_HERTZ, total) / SAMPLE_RATE

        for ((partial, gain) in listOf(1.0 to 1.0, 1.5 to 0.5, 2.0 to 0.25)) {
            val step = 2.0 * PI * quantise(mood.root * partial, total) / SAMPLE_RATE
            for (i in 0 until total) {
                val swell = 0.75 + 0.25 * sin(swellStep * i)
                out[i] += sin(step * i) * gain * mood.droneGain * swell
            }
        }
    }

    /**
     * The notes over the top, picked by a generator seeded from the mood.
     *
     * Seeded rather than random so that the same body always sounds the same way — a mood the
     * player half recognises on the way back up the ladder is the whole point of having seven of
     * them, and that cannot survive a fresh shuffle on every launch.
     */
    private fun melody(out: DoubleArray, mood: Mood, total: Int) {
        var seed = 0x5EEDL + mood.ordinal * 0x9E3779B9L
        fun next(): Double {
            // xorshift64: small, deterministic, and good enough to pick notes with. The top
            // fifty-three bits are the ones with any quality, which is also exactly how many a
            // double can hold without rounding.
            seed = seed xor (seed shl 13)
            seed = seed xor (seed ushr 7)
            seed = seed xor (seed shl 17)
            return (seed ushr 11).toDouble() / (1L shl 53).toDouble()
        }

        val stepSamples = total / STEPS
        val noteSamples = (stepSamples * mood.sustain).toInt().coerceAtLeast(1)

        for (step in 0 until STEPS) {
            if (next() >= mood.density) continue

            val degree = mood.scale[(next() * mood.scale.size).toInt() % mood.scale.size]
            val octave = 2 + (next() * mood.octaves).toInt()
            val hertz = mood.root * 2.0.pow(octave + degree / 12.0)
            val gain = 0.4 + next() * 0.35

            writeNote(out, total, step * stepSamples, noteSamples, hertz, gain, mood.edge)
        }
    }

    /**
     * One note, written with the index wrapped around the end of the loop.
     *
     * The wrap is what lets a note start on the last step: it simply continues over the beginning,
     * where the loop will be when it gets there. Clamping instead would cut the note dead at the
     * seam, which is audible in exactly the way a loop must not be.
     */
    private fun writeNote(
        out: DoubleArray,
        total: Int,
        start: Int,
        length: Int,
        hertz: Double,
        gain: Double,
        edge: Double,
    ) {
        val attack = (ATTACK_SECONDS * SAMPLE_RATE).coerceAtMost(length / 3.0).coerceAtLeast(1.0)
        val phaseStep = 2.0 * PI * hertz / SAMPLE_RATE

        for (i in 0 until length) {
            val phase = phaseStep * i
            val square = if (sin(phase) >= 0.0) 1.0 else -1.0
            val voice = square * edge + sin(phase) * (1.0 - edge)

            val shape = (i / attack).coerceAtMost(1.0) * exp(-DECAY * i.toDouble() / length)
            out[(start + i) % total] += voice * gain * shape
        }
    }

    /**
     * The nearest frequency to [hertz] that completes a whole number of cycles in [total] samples.
     *
     * At sixteen seconds the spacing between allowed frequencies is a sixteenth of a hertz, which
     * is far below what anybody can hear as being out of tune — and it buys a seam that does not
     * click.
     */
    internal fun quantise(hertz: Double, total: Int): Double {
        val cycles = (hertz * total / SAMPLE_RATE).roundToInt().coerceAtLeast(1)
        return cycles.toDouble() * SAMPLE_RATE / total
    }

    private const val DECAY = 3.2
    private const val SWELL_HERTZ = 0.125
}
