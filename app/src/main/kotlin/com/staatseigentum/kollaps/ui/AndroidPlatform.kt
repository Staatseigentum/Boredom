package com.staatseigentum.kollaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.staatseigentum.kollaps.R
import com.staatseigentum.kollaps.core.audio.Chiptune
import com.staatseigentum.kollaps.core.audio.Cue
import com.staatseigentum.kollaps.core.audio.Mood
import com.staatseigentum.kollaps.core.audio.Score
import com.staatseigentum.kollaps.ui.theme.KollapsTheme
import java.io.File
import java.util.Collections

/**
 * Everything the interface needs that only Android can supply. Kept in one file so the rest of
 * the screen stays free of `android.*` imports and can be compiled for the desktop harness.
 */

/** Raw sprite pixels into an Android bitmap. */
private val AndroidSprites = SpriteFactory { pixels, side ->
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, side, 0, 0, side, side)
    bitmap.asImageBitmap()
}

/**
 * Short samples played through a [SoundPool], the Android audio path built for firing the same
 * tiny sound over and over: it decodes once up front, so a tap never waits on a decoder, and it
 * allows several overlapping streams because an idle game gets tapped faster than the sample is
 * long.
 *
 * The click is a recording that ships with the app; everything else is synthesised by
 * [Chiptune] and written to the cache on first launch, because `SoundPool` will only load from a
 * file or a resource and a generated sound has to become one of the two.
 */
private class AndroidSounds(context: Context) : Sounds {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /** Sound ids that have finished decoding. Written on the loader thread, read on the UI one. */
    private val ready = Collections.synchronizedSet(mutableSetOf<Int>())

    private val clickSample: Int

    /** Filled from a background thread as the generated files appear. */
    private val cueSamples = Collections.synchronizedMap(mutableMapOf<Cue, Int>())

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        clickSample = pool.load(context.applicationContext, R.raw.click, PRIORITY)

        // Synthesis and a file write are not main thread work, and the first frame of the game
        // must not wait on a sound nobody has asked for yet.
        Thread {
            for (cue in Cue.entries) {
                val file = cacheFile(context, cue)
                cueSamples[cue] = pool.load(file.absolutePath, PRIORITY)
            }
        }.apply { isDaemon = true }.start()
    }

    /**
     * The cue as a file, written once and reused.
     *
     * Rendering costs a few milliseconds and the result never changes, so it is worth keeping —
     * but only in the cache, where the system may reclaim it and the next launch simply writes
     * it again.
     */
    private fun cacheFile(context: Context, cue: Cue): File {
        val file = File(context.applicationContext.cacheDir, "cue-${cue.name.lowercase()}.wav")
        if (!file.exists() || file.length() == 0L) {
            file.writeBytes(Chiptune.wav(cue))
        }
        return file
    }

    /** Silent until the sample is decoded — a tap is never worth blocking on. */
    override fun click() = play(clickSample, VOLUME)

    override fun levelUp() = play(cueSamples[Cue.LEVEL_UP], CUE_VOLUME)

    override fun comet() = play(cueSamples[Cue.COMET], CUE_VOLUME)

    override fun purchase() = play(cueSamples[Cue.PURCHASE], CUE_VOLUME * 0.7f)

    override fun success() = play(cueSamples[Cue.SUCCESS], CUE_VOLUME)

    private fun play(sample: Int?, volume: Float) {
        if (sample == null || sample !in ready) return
        pool.play(sample, volume, volume, PRIORITY, 0, 1f)
    }

    fun release() {
        ready.clear()
        pool.release()
    }

    private companion object {
        const val MAX_STREAMS = 6
        const val PRIORITY = 1
        const val VOLUME = 0.7f

        /** The synthesised cues are normalised to full scale, so they sit lower than the click. */
        const val CUE_VOLUME = 0.5f
    }
}

/**
 * The background loop, played straight out of memory.
 *
 * An `AudioTrack` in static mode rather than a `MediaPlayer` over a file: the loop is already a
 * block of PCM samples, and static mode is the one path that takes exactly that and repeats it in
 * hardware. No file to write, no decoder to wait for, and — the part that matters — a loop point
 * the hardware honours sample for sample, where a `MediaPlayer` set to loop leaves an audible gap
 * at the wrap.
 *
 * Rendering sixteen seconds of audio takes long enough to be worth keeping off the main thread,
 * so a mood change hands the work to a background thread and only touches the track once the
 * samples are ready.
 */
private class AndroidMusic : Music {

    private var track: AudioTrack? = null
    private var playing: Mood? = null

    /** True while the app is off screen, so a track built in the meantime does not start. */
    private var paused = false

    /** Guards [track] and [playing], which the loader thread and the UI thread both reach. */
    private val lock = Any()

    override fun play(mood: Mood) {
        synchronized(lock) {
            if (playing == mood) return
            // Claimed before the samples exist, so two quick tier changes cannot both start.
            playing = mood
        }
        Thread {
            val samples = Score.render(mood)
            synchronized(lock) {
                // A newer mood won while this one was rendering; its thread owns the track now.
                if (playing != mood) return@Thread
                stopLocked()
                track = build(samples).apply { if (!paused) play() }
            }
        }.apply { isDaemon = true }.start()
    }

    override fun stop() {
        synchronized(lock) {
            playing = null
            stopLocked()
        }
    }

    /**
     * Holds the loop where it is while the app is off screen.
     *
     * Not [stop], because stopping throws away samples that would only have to be synthesised
     * again to arrive at the identical loop. Pausing keeps both the track and the position, so
     * returning to the game picks the bar back up where it left off.
     */
    fun pause() {
        synchronized(lock) {
            paused = true
            track?.runCatching { pause() }
        }
    }

    fun resume() {
        synchronized(lock) {
            paused = false
            track?.runCatching { play() }
        }
    }

    private fun stopLocked() {
        track?.run {
            // Paused before release: releasing a running track is what leaves a click behind.
            runCatching { pause() }
            runCatching { flush() }
            release()
        }
        track = null
    }

    private fun build(samples: ShortArray): AudioTrack {
        val bytes = samples.size * 2
        val built = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Score.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        built.write(samples, 0, samples.size)
        built.setLoopPoints(0, samples.size, LOOP_FOREVER)
        built.setVolume(VOLUME)
        return built
    }

    private companion object {
        /** Negative one is the platform's word for "keep going". */
        const val LOOP_FOREVER = -1
        const val VOLUME = 0.35f
    }
}

private val DisplayFamily = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold),
)

private val TextFamily = FontFamily(Font(R.font.vt323_regular))

/** Wraps the game in the pieces only this platform can provide. */
@Composable
fun AndroidPlatform(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val sounds = remember(context.applicationContext) { AndroidSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }

    // The loop must not carry on playing behind whatever the player switched to. Held rather
    // than stopped, so coming back resumes the same bar instead of rebuilding it.
    val music = remember { AndroidMusic() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(music, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> music.resume()
                Lifecycle.Event.ON_STOP -> music.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            music.stop()
        }
    }

    CompositionLocalProvider(
        LocalSfx provides sounds,
        LocalMusic provides music,
        LocalSpriteFactory provides AndroidSprites,
    ) {
        KollapsTheme(display = DisplayFamily, text = TextFamily, content = content)
    }
}
