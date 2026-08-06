package com.staatseigentum.kollaps.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.staatseigentum.kollaps.R

/**
 * The click.
 *
 * One short sample played through a [SoundPool], which is the Android audio path built for firing
 * the same tiny sound over and over: it decodes once up front, so a tap never waits on a decoder.
 * An idle game gets tapped faster than the sample is long, so several streams are allowed to
 * overlap rather than cutting each other off.
 */
class Sfx(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /** Written on the loader thread, read on the UI thread. */
    @Volatile
    private var loaded = false

    private val sample: Int

    init {
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        sample = pool.load(context.applicationContext, R.raw.click, PRIORITY)
    }

    /** Plays the click. Silent until the sample is decoded — a tap is never worth blocking on. */
    fun click() {
        if (!loaded) return
        pool.play(sample, VOLUME, VOLUME, PRIORITY, 0, 1f)
    }

    fun release() {
        loaded = false
        pool.release()
    }

    private companion object {
        const val MAX_STREAMS = 6
        const val PRIORITY = 1
        const val VOLUME = 0.7f
    }
}

/**
 * Reaches every button and row without threading a parameter through the whole tree. Null until
 * [ProvideSfx] is in place, so previews and tests stay silent instead of needing a player.
 */
val LocalSfx = staticCompositionLocalOf<Sfx?> { null }

/** Owns the player for as long as the interface is on screen. */
@Composable
fun ProvideSfx(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val sfx = remember(context.applicationContext) { Sfx(context) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    CompositionLocalProvider(LocalSfx provides sfx, content = content)
}
