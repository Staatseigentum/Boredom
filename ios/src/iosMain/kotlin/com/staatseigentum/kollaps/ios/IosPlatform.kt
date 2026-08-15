package com.staatseigentum.kollaps.ios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.staatseigentum.kollaps.ui.LocalMusic
import com.staatseigentum.kollaps.ui.LocalSfx
import com.staatseigentum.kollaps.ui.LocalSpriteFactory
import com.staatseigentum.kollaps.ui.SpriteFactory
import com.staatseigentum.kollaps.ui.theme.KollapsTheme
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * The two things this platform has to supply that the shared sources cannot: pixels into a
 * bitmap, and the fonts. Everything else on screen is the app's own code, compiled as it stands.
 */

/**
 * Raw sprite pixels into a Skia bitmap.
 *
 * Character for character what the desktop does, and for the same reason: the renderer hands out
 * ARGB in the order Android wants, and Skia is told BGRA explicitly rather than being trusted to
 * agree. Naming the layout is what makes this the same code on both — get it wrong and every
 * planet arrives with red and blue traded, which is a bug you can only see by looking.
 */
val IosSprites = SpriteFactory { pixels, side ->
    val bytes = ByteArray(side * side * 4)
    for (index in pixels.indices) {
        val argb = pixels[index]
        val at = index * 4
        bytes[at] = (argb and 0xFF).toByte()
        bytes[at + 1] = (argb shr 8 and 0xFF).toByte()
        bytes[at + 2] = (argb shr 16 and 0xFF).toByte()
        bytes[at + 3] = (argb ushr 24).toByte()
    }
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(side, side, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(bytes)
    Image.makeFromBitmap(bitmap).toComposeImageBitmap()
}

/**
 * The pixel fonts, out of the app bundle.
 *
 * There is no classpath here — a Kotlin/Native binary has no resources of its own — so the `.ttf`
 * files travel as plain files inside the `.app`, put there by `paket.sh`, and are read back by
 * name. Falls back to the system monospace rather than failing: a readable game in the wrong face
 * beats a black screen, and the desktop learned that the hard way when a packaged build shipped
 * without its fonts and nothing said so.
 */
private fun family(vararg files: Pair<String, FontWeight>): FontFamily {
    val fonts = files.mapNotNull { (name, weight) ->
        val bytes = fontBytes(name) ?: return@mapNotNull null
        Font(identity = name, data = bytes, weight = weight)
    }
    return if (fonts.isEmpty()) FontFamily.Monospace else FontFamily(fonts)
}

@OptIn(ExperimentalForeignApi::class)
private fun fontBytes(name: String): ByteArray? {
    val path = NSBundle.mainBundle.pathForResource(name, "ttf") ?: return null
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size == 0) return null
    val out = ByteArray(size)
    out.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
    return out
}

private val DisplayFamily by lazy {
    family("silkscreen_regular" to FontWeight.Normal, "silkscreen_bold" to FontWeight.Bold)
}

private val TextFamily by lazy { family("vt323_regular" to FontWeight.Normal) }

/**
 * Wraps the game in the pieces only this platform can provide.
 *
 * Sound and music come from the same two generators the phone and the PC use — they live in
 * `:core` and produce finished WAV bytes, so the only thing this platform had to supply was a way
 * to play them. See [IosAudio].
 *
 * Handed over without asking whether the player wants to hear anything, which is deliberate and is
 * what the other two do: the setting is enforced once, in `GameScreen`, by taking the player away
 * from everything below it. Checking here as well would be a second place for the two answers to
 * disagree.
 */
@Composable
fun IosPlatform(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpriteFactory provides IosSprites,
        LocalSfx provides IosAudio.cues,
        LocalMusic provides IosAudio.loop,
    ) {
        KollapsTheme(display = DisplayFamily, text = TextFamily, content = content)
    }
}
