package com.staatseigentum.kollaps.core.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** A sound the game needs to make, beyond the click that ships as a recording. */
enum class Cue {
    /** A new body on the ladder. Rising, and the longest of them. */
    LEVEL_UP,

    /** A comet caught in time. */
    COMET,

    /** Something bought in the shop. Short enough to fire on every row. */
    PURCHASE,

    /** A challenge handed in. */
    SUCCESS,

    /** A research project come due. Quiet: it arrives while the player is doing something else. */
    RESEARCH,

    /** The core catching light for the first time in a run. */
    IGNITION,

    /** A comet that drifted off the screen uncaught. */
    MISSED,

    /** Something crossing the sky. Quiet, and the only cue nobody has to react to. */
    FLYBY,

    /** An achievement earned. */
    UNLOCK,

    /**
     * Any tap.
     *
     * Android plays a recording for this and never asks for the cue; the desktop has no such file
     * and would otherwise be the one platform where tapping the body is silent. Synthesising it
     * means both platforms make a noise without a sound file having to be carried into an
     * installer that already bundles a Java runtime.
     */
    CLICK,

    /**
     * The interface being dragged into the hole.
     *
     * The odd one out in every respect: two and a half seconds where the rest are fractions of
     * one, no notes, and it ends in silence rather than in a decay. It is not a reaction to
     * anything the player did — it runs alongside the collapse sequence and is over when the
     * screen is empty. The silence at the end is the point: what follows it is the bang.
     */
    COLLAPSE,
}

/**
 * The sound effects, synthesised rather than recorded.
 *
 * Everything else in this game is generated — the planets, the launcher icon, the surface
 * textures — and the sounds had no reason to be the exception. Square waves with a short decay
 * are also exactly what the pixel art is asking for, and they cost a few hundred bytes of code
 * instead of a few hundred kilobytes of assets.
 *
 * Output is 16 bit mono PCM in a RIFF container, which is what Android's `SoundPool` wants and
 * what a plain file on disk can be.
 */
object Chiptune {

    const val SAMPLE_RATE = 22_050

    /** A note in a cue: pitch in hertz, when it starts, how long it lasts, how loud. */
    private class Note(
        val hertz: Double,
        val startSeconds: Double,
        val seconds: Double,
        val gain: Double = 1.0,
        /** Pitch at the end, for the ones that slide. */
        val toHertz: Double = hertz,
    )

    // A minor-ish set of pitches, so the cues sit together instead of clashing.
    private const val C5 = 523.25
    private const val E5 = 659.25
    private const val G5 = 783.99
    private const val C6 = 1046.50
    private const val E6 = 1318.51
    private const val G4 = 392.00
    private const val C4 = 261.63

    private fun notesOf(cue: Cue): List<Note> = when (cue) {
        // Four steps up, the last one held: the ladder, in one second.
        Cue.LEVEL_UP -> listOf(
            Note(C5, 0.00, 0.10),
            Note(E5, 0.09, 0.10),
            Note(G5, 0.18, 0.10),
            Note(C6, 0.27, 0.34),
            Note(E6, 0.30, 0.30, gain = 0.5),
        )

        // A downward slide, like something passing overhead and gone.
        Cue.COMET -> listOf(
            Note(hertz = E6, startSeconds = 0.0, seconds = 0.26, toHertz = G5),
            Note(hertz = C6, startSeconds = 0.02, seconds = 0.22, gain = 0.4, toHertz = E5),
        )

        // Two notes, eighty milliseconds, because this one fires on every row of the shop.
        Cue.PURCHASE -> listOf(
            Note(G5, 0.00, 0.04, gain = 0.7),
            Note(C6, 0.035, 0.05, gain = 0.7),
        )

        // Two soft notes a fifth apart, no attack to speak of. This one interrupts nothing.
        Cue.RESEARCH -> listOf(
            Note(C5, 0.00, 0.14, gain = 0.5),
            Note(G5, 0.10, 0.22, gain = 0.45),
        )

        // Low to high and held: something starting rather than something finishing.
        Cue.IGNITION -> listOf(
            Note(hertz = G4, startSeconds = 0.00, seconds = 0.30, toHertz = C5),
            Note(hertz = C5, startSeconds = 0.14, seconds = 0.34, gain = 0.6, toHertz = G5),
            Note(hertz = E5, startSeconds = 0.28, seconds = 0.30, gain = 0.4),
        )

        // A long slide downward, quiet: something passing overhead that was never coming to you.
        // The one cue whose job is to make the player *look*, so it has to be noticeable without
        // being an alarm — it fires every few minutes whether or not anybody wants it to.
        Cue.FLYBY -> listOf(
            Note(hertz = E6, startSeconds = 0.00, seconds = 0.34, toHertz = C5, gain = 0.55),
            Note(hertz = G5, startSeconds = 0.06, seconds = 0.26, toHertz = G4, gain = 0.30),
        )

        // Up a fourth and held, with the octave under it. Shorter than the tier fanfare, because
        // an achievement is a nod and a new body is an event — and a player can earn three of
        // these inside a second, so it must not queue up into a chord.
        Cue.UNLOCK -> listOf(
            Note(G5, 0.00, 0.10),
            Note(C6, 0.08, 0.26),
            Note(C5, 0.08, 0.24, gain = 0.4),
        )

        // One note and gone. Fired more often than everything else put together, so it is the one
        // cue where length is the whole design: long enough to hear, short enough that two taps
        // in quick succession are two sounds rather than a smear.
        Cue.CLICK -> listOf(
            Note(hertz = C6, startSeconds = 0.0, seconds = 0.05, toHertz = G5),
        )

        // Two steps down and done, low and short.
        //
        // Deliberately the least interesting sound in the game. A miss is the player's own doing
        // and it happens every few minutes; anything with a shape to it would turn a small "oh"
        // into a telling-off, and the one thing this cue must not become is a reason to stop
        // leaving the screen alone.
        Cue.MISSED -> listOf(
            Note(G4, 0.00, 0.09, gain = 0.6),
            Note(C4, 0.07, 0.13, gain = 0.45),
        )

        // A triad that lands on the octave and holds.
        Cue.SUCCESS -> listOf(
            Note(G4, 0.00, 0.12),
            Note(C5, 0.10, 0.12),
            Note(E5, 0.20, 0.12),
            Note(G5, 0.30, 0.40),
            Note(C6, 0.32, 0.38, gain = 0.6),
        )

        // Not a list of notes at all — see [renderCollapse].
        Cue.COLLAPSE -> emptyList()
    }

    /** The cue as a playable WAV file. */
    fun wav(cue: Cue): ByteArray = Wav.riff(render(cue), SAMPLE_RATE)

    /** Raw samples, exposed so a test can look at the waveform without parsing a header. */
    fun render(cue: Cue): ShortArray {
        if (cue == Cue.COLLAPSE) return Wav.normalise(renderCollapse(), PEAK)

        val notes = notesOf(cue)
        val totalSeconds = notes.maxOf { it.startSeconds + it.seconds } + TAIL_SECONDS
        val out = DoubleArray((totalSeconds * SAMPLE_RATE).toInt())

        for (note in notes) {
            val start = (note.startSeconds * SAMPLE_RATE).toInt()
            val length = (note.seconds * SAMPLE_RATE).toInt()
            var phase = 0.0

            for (i in 0 until length) {
                val index = start + i
                if (index >= out.size) break

                val progress = i.toDouble() / length
                val hertz = note.hertz + (note.toHertz - note.hertz) * progress
                phase += 2.0 * PI * hertz / SAMPLE_RATE

                // Square rather than sine: it is the sound the picture is drawn in. Softened by
                // a sine at the same pitch so it is bright without being a buzzsaw.
                val square = if (sin(phase) >= 0.0) 1.0 else -1.0
                val voice = square * 0.6 + sin(phase) * 0.4

                out[index] += voice * note.gain * envelope(i, length)
            }
        }
        return Wav.normalise(out, PEAK)
    }

    /**
     * The collapse: a rising rumble that stops dead.
     *
     * Written by hand rather than as notes, because everything the note renderer is good at is
     * wrong here. A note has a pitch, an attack and a decay; this has a pitch that slides for two
     * and a half seconds, no attack worth the name, and no decay at all. It also has to be
     * *noise* as much as tone — a collapse is not a chord.
     *
     * Three layers, all following the same curve the picture follows:
     *
     * - a low voice sliding from 38 to 116 Hz on `p²`, the same acceleration the elements fall on,
     *   so the sound speeds up exactly when the screen does;
     * - a fifth above it that drifts sharp as it goes, so the two beat against each other harder
     *   and harder — the sound of something being pulled out of tune rather than played;
     * - filtered noise that only arrives in the second half, growing on `p²` like the sky's warp.
     *
     * And then it simply stops, thirty-four hundredths of a second before the bang. The silence is
     * doing the work: a rumble that swells into the explosion sounds like volume, a rumble that is
     * cut off sounds like the air being taken out of the room.
     */
    private fun renderCollapse(): DoubleArray {
        val rumble = (COLLAPSE_RUMBLE_SECONDS * SAMPLE_RATE).toInt()
        val total = ((COLLAPSE_RUMBLE_SECONDS + COLLAPSE_SILENCE_SECONDS) * SAMPLE_RATE).toInt()
        val out = DoubleArray(total)

        var lowPhase = 0.0
        var fifthPhase = 0.0
        var filtered = 0.0

        // Its own counter rather than `Random`: the cue is rendered once and cached, and a test
        // insists that two renders are byte for byte the same file. A seeded sequence written out
        // here is the cheapest way to be certain of that on every platform.
        var seed = 0x9E3779B97F4A7C15uL.toLong()

        // Long enough not to click, short enough to still be a cut rather than a fade.
        val cutSamples = 0.05 * SAMPLE_RATE

        for (i in 0 until rumble) {
            val p = i.toDouble() / rumble

            val low = 38.0 + 78.0 * p * p
            lowPhase += 2.0 * PI * low / SAMPLE_RATE
            // The fifth goes sharp as it climbs: 1.5 is in tune, 1.56 is audibly not.
            fifthPhase += 2.0 * PI * (low * (1.5 + 0.06 * p)) / SAMPLE_RATE

            seed = seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
            val white = ((seed ushr 40).toDouble() / (1 shl 23).toDouble()) - 1.0
            // One pole of low pass. White noise on its own is a hiss; this is wind.
            filtered += (white - filtered) * 0.05

            val square = if (sin(lowPhase) >= 0.0) 1.0 else -1.0
            val voice = square * 0.5 + sin(lowPhase) * 0.5
            val fifth = sin(fifthPhase) * 0.35 * p

            val swell = p.pow(1.4)
            val cut = ((rumble - i) / cutSamples).coerceAtMost(1.0)

            out[i] = (voice + fifth + filtered * 1.2 * p * p) * swell * cut
        }
        return out
    }

    /**
     * A fast attack and an exponential decay.
     *
     * The attack is a fixed number of milliseconds rather than a share of the note, because the
     * shortest cue is forty milliseconds long and a proportional ramp there would be over in
     * under one. Starting a square wave at full amplitude puts a step in the signal, and a step
     * is a click on top of the note — which is the one artefact that would make every one of
     * these sound broken.
     */
    private fun envelope(sample: Int, length: Int): Double {
        val attackSamples = minOf(ATTACK_SECONDS * SAMPLE_RATE, length / 4.0).coerceAtLeast(1.0)
        val attack = (sample / attackSamples).coerceAtMost(1.0)
        return attack * exp(-DECAY * sample.toDouble() / length)
    }

    /** Silence after the last note, so nothing is cut off mid-decay. */
    private const val TAIL_SECONDS = 0.05
    private const val ATTACK_SECONDS = 0.006
    private const val DECAY = 4.5
    private const val PEAK = 26_000.0

    /**
     * How long the collapse rumble runs, and how long the hole in the sound is after it.
     *
     * The first number is the length of the sequence's pull phase and has to stay that way: the
     * sound stopping is what tells the player the screen is about to be empty. The second is the
     * gap before the bang.
     */
    const val COLLAPSE_RUMBLE_SECONDS = 2.6
    private const val COLLAPSE_SILENCE_SECONDS = 0.34
}
