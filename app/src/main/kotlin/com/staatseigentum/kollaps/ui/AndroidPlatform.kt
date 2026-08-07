package com.staatseigentum.kollaps.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.staatseigentum.kollaps.R
import com.staatseigentum.kollaps.ui.theme.KollapsTheme

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
 * One short sample played through a [SoundPool], the Android audio path built for firing the same
 * tiny sound over and over: it decodes once up front, so a tap never waits on a decoder, and it
 * allows several overlapping streams because an idle game gets tapped faster than the sample is
 * long.
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

    /** Written on the loader thread, read on the UI thread. */
    @Volatile
    private var loaded = false

    private val sample: Int

    init {
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        sample = pool.load(context.applicationContext, R.raw.click, PRIORITY)
    }

    /** Silent until the sample is decoded — a tap is never worth blocking on. */
    override fun click() {
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

    CompositionLocalProvider(
        LocalSfx provides sounds,
        LocalSpriteFactory provides AndroidSprites,
    ) {
        KollapsTheme(display = DisplayFamily, text = TextFamily, content = content)
    }
}
