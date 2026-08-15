package com.staatseigentum.kollaps.ios

import com.staatseigentum.kollaps.core.audio.Chiptune
import com.staatseigentum.kollaps.core.audio.Cue
import com.staatseigentum.kollaps.core.audio.Mood
import com.staatseigentum.kollaps.core.audio.Score
import com.staatseigentum.kollaps.ui.Music
import com.staatseigentum.kollaps.ui.Sounds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

/**
 * The sound the iPhone build shipped without.
 *
 * It cost almost nothing in the end, and that is worth saying plainly: none of the *sound* is
 * written here. Both generators live in `:core` and are ordinary Kotlin — [Chiptune] builds the
 * short cues note by note, [Score] builds the sixteen second loop the music runs on, and both hand
 * back a finished WAV file as bytes. Every platform's job is only to make bytes audible.
 *
 * On the desktop that is `javax.sound`, on Android `SoundPool`, and here it is `AVAudioPlayer`,
 * which takes exactly what those two produce: a `NSData` of a RIFF file. So the iPhone makes the
 * same noises as the phone and the PC, note for note, rather than approximations of them.
 *
 * Everything is wrapped against failure. A device in silent mode, a session another app holds, a
 * player that will not open — none of those is a reason for the game not to run. The worst outcome
 * allowed here is silence, which is what this build had anyway.
 */

/** ByteArray to `NSData`, copied — the player keeps its own reference to the bytes. */
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData? {
    if (isEmpty()) return null
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

/** A player over WAV bytes, or `null` if this device will not open one. */
private fun playerOf(wav: ByteArray): AVAudioPlayer? {
    val data = wav.toNSData() ?: return null
    return runCatching {
        AVAudioPlayer(data = data, error = null).takeIf { it.prepareToPlay() }
    }.getOrNull()
}

/**
 * The audio session, set once.
 *
 * `Ambient` on purpose, and it is the difference between a game and a nuisance: it mixes with
 * whatever the player already had running instead of stopping their music, and it honours the
 * ring switch — a phone on silent stays silent. Both are what somebody expects of a game they
 * opened on a train.
 */
private fun openSession() {
    runCatching {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, null)
        session.setActive(true, null)
    }
}

/**
 * The cues, rendered once and rewound on every play.
 *
 * One player per cue rather than one per playback: building a player means decoding a file, and a
 * tap must never wait on that. The cost is that a cue restarts rather than overlapping itself,
 * which at these lengths is what it would sound like either way.
 *
 * Rendered off the main thread. Thirteen cues is a few hundred thousand samples of arithmetic, and
 * the first frame of the game should not be waiting behind it — until it is done the game is quiet,
 * which is a second at most and no worse than what it replaced.
 */
private class IosSounds(scope: CoroutineScope) : Sounds {

    private val players = mutableMapOf<Cue, AVAudioPlayer>()

    init {
        scope.launch {
            for (cue in Cue.entries) {
                val built = withContext(Dispatchers.Default) { playerOf(Chiptune.wav(cue)) }
                if (built != null) players[cue] = built
            }
        }
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

    // Under the bang that follows it, on purpose: this one has two and a half seconds to make its
    // point and the explosion has none. Same balance as the other two platforms.
    override fun collapse() = play(Cue.COLLAPSE, CUE_VOLUME * 0.8f)

    override fun explosion() = play(Cue.EXPLOSION, CUE_VOLUME * 0.5f)

    override fun flatten() = play(Cue.FLATTEN, CUE_VOLUME * 0.7f)

    private fun play(cue: Cue, volume: Float) {
        val player = players[cue] ?: return
        runCatching {
            player.stop()
            player.currentTime = 0.0
            player.volume = volume
            player.play()
        }
    }

    /**
     * Called when the app leaves the screen.
     *
     * A cue caught mid-play would otherwise be resumed on the way back in, seconds later and with
     * nothing on screen to explain it.
     */
    fun silence() {
        for (player in players.values) runCatching { player.stop() }
    }

    private companion object {
        const val VOLUME = 0.7f
        const val CUE_VOLUME = 0.5f
    }
}

/**
 * The background loop.
 *
 * `numberOfLoops = -1` rather than restarting it on completion: the loop is a whole number of
 * cycles by construction — that is the entire reason [Score] renders sixteen seconds — so the
 * player can repeat it from memory with no seam, and nothing has to watch for the end.
 */
private class IosMusic(private val scope: CoroutineScope) : Music {

    private var player: AVAudioPlayer? = null
    private var playing: Mood? = null

    override fun play(mood: Mood) {
        if (playing == mood) return
        // Claimed before the samples exist, so two quick tier changes cannot both start one.
        playing = mood
        scope.launch {
            val built = withContext(Dispatchers.Default) { playerOf(Score.wav(mood)) } ?: return@launch
            // A newer mood won while this one was being rendered; it owns the loop now.
            if (playing != mood) return@launch
            stopPlayer()
            player = built.apply {
                numberOfLoops = -1
                volume = VOLUME
                runCatching { play() }
            }
        }
    }

    override fun stop() {
        playing = null
        stopPlayer()
    }

    private fun stopPlayer() {
        player?.let { open -> runCatching { open.stop() } }
        player = null
    }

    private companion object {
        const val VOLUME = 0.35f
    }
}

/**
 * Everything the iPhone can make a noise with, built once and handed to the screen.
 *
 * Its own scope rather than the composition's: the cues are rendered once for the life of the app
 * and a composition that goes away mid-render would cancel work nothing else is going to redo.
 */
object IosAudio {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val sounds by lazy {
        openSession()
        IosSounds(scope)
    }

    private val music by lazy { IosMusic(scope) }

    val cues: Sounds get() = sounds

    val loop: Music get() = music

    /** Everything quiet, for the moment the app is put away. */
    fun silence() {
        music.stop()
        sounds.silence()
    }
}
