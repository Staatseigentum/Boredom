package com.staatseigentum.kollaps.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.staatseigentum.kollaps.ui.LocalSpriteFactory
import com.staatseigentum.kollaps.ui.SpriteFactory
import com.staatseigentum.kollaps.ui.theme.KollapsTheme
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.io.File

/**
 * What the harness has to supply that Android otherwise would: pixels into a bitmap, and the
 * fonts. Everything else about the interface is the app's own code.
 */

/**
 * Raw sprite pixels into a Skia bitmap.
 *
 * The renderer hands out ARGB in the order Android wants. Skia on this platform reads BGRA, so
 * the two middle channels have to be swapped — get this wrong and every planet comes out with
 * red and blue traded, which is exactly the kind of thing the harness exists to catch.
 */
val DesktopSprites = SpriteFactory { pixels, side ->
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

private fun family(vararg files: Pair<String, FontWeight>): FontFamily {
    val fonts = files.mapNotNull { (name, weight) ->
        val file = File("app/src/main/res/font/$name.ttf")
        if (file.exists()) Font(file, weight) else null
    }
    // Falls back to the system monospace so the harness still runs from an odd working directory.
    return if (fonts.isEmpty()) FontFamily.Monospace else FontFamily(fonts)
}

private val DisplayFamily by lazy {
    family("silkscreen_regular" to FontWeight.Normal, "silkscreen_bold" to FontWeight.Bold)
}

private val TextFamily by lazy { family("vt323_regular" to FontWeight.Normal) }

/** Wraps the game in the pieces only this platform can provide. */
@Composable
fun DesktopPlatform(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpriteFactory provides DesktopSprites) {
        KollapsTheme(display = DisplayFamily, text = TextFamily, content = content)
    }
}
