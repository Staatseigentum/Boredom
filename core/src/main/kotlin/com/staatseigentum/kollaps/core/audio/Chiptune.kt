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

    /**
     * The bang itself, over the empty screen.
     *
     * The blast used to borrow the challenge fanfare, which is a triad landing on the octave —
     * a well-done, not an explosion. This is noise rather than notes, and deliberately quiet:
     * it lands in the silence the rumble leaves behind, and silence is what makes it loud.
     */
    EXPLOSION,

    /**
     * The universe being pressed flat, under the first phase of the big bang.
     *
     * The opposite of [COLLAPSE] in every way that can be heard. That one falls: it slides *down*
     * from thirty-eight hertz and gets thicker as it goes. This one rises — a whine climbing out
     * of the top of its range, thinning rather than thickening — and then stops dead, leaving the
     * four hundred and sixty milliseconds of nothing that the bang goes off in.
     */
    FLATTEN,
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

        // Neither of these is a list of notes — see [renderCollapse] and [renderExplosion].
        Cue.COLLAPSE -> emptyList()
        Cue.EXPLOSION -> emptyList()
        Cue.FLATTEN -> emptyList()
    }

    /** The cue as a playable WAV file. */
    fun wav(cue: Cue): ByteArray = Wav.riff(render(cue), SAMPLE_RATE)

    /** Raw samples, exposed so a test can look at the waveform without parsing a header. */
    fun render(cue: Cue): ShortArray {
        if (cue == Cue.COLLAPSE) return Wav.normalise(renderCollapse(), PEAK)
        if (cue == Cue.EXPLOSION) return Wav.normalise(renderExplosion(), PEAK)
        if (cue == Cue.FLATTEN) return Wav.normalise(renderFlatten(), PEAK)

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
     * The flattening: a whine climbing out of the top of its range, cut off at the point.
     *
     * Written against [renderCollapse] rather than alongside it. The two resets are half a screen
     * apart in the same tab, and if they sounded alike the player would stop hearing which one
     * they had triggered. So every choice here is the other one's mirror: the pitch climbs where
     * the collapse's falls, from 210 up past 2000 hertz; the noise *thins* over time instead of
     * growing, filtered harder and harder until only a hiss is left; and the two voices converge
     * on the same note rather than drifting apart, so it tightens instead of souring.
     *
     * It ends where the line becomes a point, and nothing follows it for almost half a second.
     * That gap is the loudest thing in the sequence.
     */
    private fun renderFlatten(): DoubleArray {
        val body = (FLATTEN_SECONDS * SAMPLE_RATE).toInt()
        val total = ((FLATTEN_SECONDS + TAIL_SECONDS) * SAMPLE_RATE).toInt()
        val out = DoubleArray(total)

        var risePhase = 0.0
        var beatPhase = 0.0
        var filtered = 0.0
        var seed = 0x0DDB1A5E5BAD5EEDuL.toLong()

        for (i in 0 until body) {
            val p = i.toDouble() / body

            // Accelerating upward: a linear climb sounds like a siren, which is a warning. This
            // is something being forced, and forcing gets harder the further it goes.
            val rise = 210.0 + 1_900.0 * p.pow(1.8)
            risePhase += 2.0 * PI * rise / SAMPLE_RATE
            // Starts a fifth below and closes on the main voice, so the interval narrows to
            // nothing exactly as the picture narrows to a line.
            beatPhase += 2.0 * PI * (rise * (0.66 + 0.34 * p)) / SAMPLE_RATE

            seed = seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
            val white = ((seed ushr 40).toDouble() / (1 shl 23).toDouble()) - 1.0
            filtered += (white - filtered) * (0.02 + 0.5 * p)

            val square = if (sin(risePhase) >= 0.0) 1.0 else -1.0
            val voice = square * 0.35 + sin(risePhase) * 0.65
            val beat = sin(beatPhase) * 0.4 * (1.0 - p * 0.5)

            // Six milliseconds in, and out over the last forty: enough not to click at either
            // end, far too little to sound like a fade.
            val attack = (i / (0.006 * SAMPLE_RATE)).coerceAtMost(1.0)
            val release = ((body - i) / (0.04 * SAMPLE_RATE)).coerceAtMost(1.0)
            val swell = 0.25 + 0.75 * p

            out[i] = (voice + beat + filtered * 0.5 * (1.0 - p)) * swell * attack * release
        }
        return out
    }

    /**
     * The bang: noise that starts bright and goes dull, over a sub that falls away.
     *
     * An explosion is not a chord, so this is not notes either. Two things make it read as one
     * rather than as a burst of static:
     *
     * - the filter *opens* for a tenth of a second and then closes for the rest of the second.
     *   That is the whole trick — the crack at the front is the high end being let through
     *   briefly, and everything after it is the same noise with the top taken off, which is what
     *   distance and air actually do to a loud sound;
     * - a sine underneath sliding from 90 Hz down to 28, so there is a body under the noise
     *   rather than only hiss.
     *
     * It is kept short and it is played quietly. The bang lands in the silence the collapse
     * rumble leaves behind, and a sound in silence does not need volume to be an event — asking
     * it to be loud as well would only make it a clipped mess on a phone speaker.
     */
    private fun renderExplosion(): DoubleArray {
        val body = (EXPLOSION_SECONDS * SAMPLE_RATE).toInt()
        val total = ((EXPLOSION_SECONDS + TAIL_SECONDS) * SAMPLE_RATE).toInt()
        val out = DoubleArray(total)

        var subPhase = 0.0
        var filtered = 0.0
        var seed = 0x2545F4914F6CDD1DuL.toLong()

        // Where the filter turns around: bright on the way up, dull for the whole way down.
        val opening = 0.09 * SAMPLE_RATE

        for (i in 0 until body) {
            val p = i.toDouble() / body

            seed = seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
            val white = ((seed ushr 40).toDouble() / (1 shl 23).toDouble()) - 1.0

            // 0.9 lets almost everything through, 0.05 is a dull thud. One pole either way.
            val cutoff = if (i < opening) 0.12 + 0.78 * (i / opening) else 0.9 - 0.85 * p
            filtered += (white - filtered) * cutoff.coerceIn(0.03, 0.95)

            val sub = 90.0 - 62.0 * p
            subPhase += 2.0 * PI * sub / SAMPLE_RATE

            // Six milliseconds of attack: enough not to be a step in the signal, short enough
            // that it still hits rather than swells.
            val attack = (i / (0.006 * SAMPLE_RATE)).coerceAtMost(1.0)
            val decay = exp(-3.4 * p)

            out[i] = (filtered * 0.75 + sin(subPhase) * 0.55) * attack * decay
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

    /** How long the bang rings out. Short: it is an impact, not a wash. */
    private const val EXPLOSION_SECONDS = 1.0

    /**
     * How long the flattening whine runs.
     *
     * The big bang sequence presses the interface flat for 980 ms and then squeezes the line to a
     * point over another 340. The sound covers both and stops with the point, which is what makes
     * the silence after it land as an event rather than as a gap.
     */
    const val FLATTEN_SECONDS = 1.32
}
