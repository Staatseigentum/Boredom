package com.staatseigentum.kollaps.desktop

import com.staatseigentum.kollaps.core.audio.Chiptune
import com.staatseigentum.kollaps.core.audio.Cue
import com.staatseigentum.kollaps.core.audio.Mood
import com.staatseigentum.kollaps.core.audio.Score
import com.staatseigentum.kollaps.ui.Music
import com.staatseigentum.kollaps.ui.Sounds
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineUnavailableException

/**
 * The sound the PC version was missing.
 *
 * Android has `SoundPool` and `AudioTrack`; the desktop has `javax.sound.sampled`, which is part
 * of the Java runtime the installer already bundles — so this costs nothing to ship and needs no
 * library. Both kinds of sound come from the same generators the phone uses, so the two versions
 * make exactly the same noises.
 *
 * Every call is wrapped against failure. A machine with no sound card, a card already held
 * exclusively by something else, or a stripped runtime are all real and none of them are a reason
 * for the game not to start — the worst outcome allowed here is silence.
 */

/** Opens a clip over WAV bytes, or `null` if this machine cannot play it. */
private fun clipOf(wav: ByteArray): Clip? = try {
    val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(wav))
    (AudioSystem.getLine(javax.sound.sampled.DataLine.Info(Clip::class.java, stream.format)) as Clip)
        .apply { open(stream) }
} catch (_: LineUnavailableException) {
    null
} catch (_: Exception) {
    null
}

/** Sets a clip's volume in the same 0..1 terms the Android side uses. */
private fun Clip.setGain(volume: Float) {
    if (!isControlSupported(FloatControl.Type.MASTER_GAIN)) return
    val control = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
    // Decibels, not a share: -20 dB is roughly a tenth as loud, and the floor keeps a volume of
    // zero from asking for negative infinity.
    val decibels = (20.0 * kotlin.math.log10(volume.coerceIn(0.0001f, 1f).toDouble())).toFloat()
    control.value = decibels.coerceIn(control.minimum, control.maximum)
}

/**
 * The cues, decoded once at startup and rewound on every play.
 *
 * One clip per cue rather than one per playback: opening a line costs milliseconds, and a tap must
 * never wait on one. The cost is that the same cue restarts rather than overlapping itself, which
 * for sounds this short is what it would sound like anyway.
 */
private class DesktopSounds : Sounds {

    private val clips = mutableMapOf<Cue, Clip>()

    init {
        // On a background thread: rendering seven cues and opening seven lines is not work the
        // first frame should wait for, and until it is done the game is simply quiet.
        Thread {
            for (cue in Cue.entries) {
                clipOf(Chiptune.wav(cue))?.let { clip -> synchronized(clips) { clips[cue] = clip } }
            }
        }.apply { isDaemon = true }.start()
    }

    override fun click() = play(Cue.CLICK, VOLUME)

    override fun levelUp() = play(Cue.LEVEL_UP, CUE_VOLUME)

    override fun comet() = play(Cue.COMET, CUE_VOLUME)

    override fun missed() = play(Cue.MISSED, CUE_VOLUME * 0.45f)

    override fun flyby() = play(Cue.FLYBY, CUE_VOLUME * 0.55f)

    override fun unlock() = play(Cue.UNLOCK, CUE_VOLUME)

    override fun purchase() = play(Cue.PURCHASE, CUE_VOLUME * 0.7f)

    override fun success() = play(Cue.SUCCESS, CUE_VOLUME)

    override fun research() = play(Cue.RESEARCH, CUE_VOLUME * 0.8f)

    override fun ignition() = play(Cue.IGNITION, CUE_VOLUME)

    // Under the bang that follows it, on purpose: this one has two and a half seconds to make
    // its point and the explosion has none.
    override fun collapse() = play(Cue.COLLAPSE, CUE_VOLUME * 0.8f)

    private fun play(cue: Cue, volume: Float) {
        val clip = synchronized(clips) { clips[cue] } ?: return
        runCatching {
            clip.stop()
            clip.framePosition = 0
            clip.setGain(volume)
            clip.start()
        }
    }

    fun release() {
        synchronized(clips) {
            clips.values.forEach { runCatching { it.close() } }
            clips.clear()
        }
    }

    private companion object {
        const val VOLUME = 0.7f
        const val CUE_VOLUME = 0.5f
    }
}

/**
 * The background loop.
 *
 * A `Clip` with `LOOP_CONTINUOUSLY` rather than a streaming line: the loop is already a finished
 * block of samples, and a clip repeats it from memory with no seam — which is the entire reason
 * [Score] renders whole cycles in the first place.
 */
private class DesktopMusic : Music {

    private var clip: Clip? = null
    private var playing: Mood? = null
    private val lock = Any()

    override fun play(mood: Mood) {
        synchronized(lock) {
            if (playing == mood) return
            // Claimed before the samples exist, so two quick tier changes cannot both start.
            playing = mood
        }
        Thread {
            val built = clipOf(Score.wav(mood)) ?: return@Thread
            synchronized(lock) {
                // A newer mood won while this one was rendering; its thread owns the line now.
                if (playing != mood) {
                    runCatching { built.close() }
                    return@Thread
                }
                stopLocked()
                clip = built.apply {
                    setGain(VOLUME)
                    runCatching { loop(Clip.LOOP_CONTINUOUSLY) }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    override fun stop() {
        synchronized(lock) {
            playing = null
            stopLocked()
        }
    }

    private fun stopLocked() {
        clip?.let { open ->
            runCatching { open.stop() }
            runCatching { open.close() }
        }
        clip = null
    }

    private companion object {
        const val VOLUME = 0.35f
    }
}

/** Everything the desktop can make a noise with, built once and handed to the screen. */
class DesktopAudio {
    private val sounds = DesktopSounds()
    private val music = DesktopMusic()

    val cues: Sounds get() = sounds
    val loop: Music get() = music

    fun release() {
        music.stop()
        sounds.release()
    }
}
