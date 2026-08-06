package com.staatseigentum.kollaps.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The body the player taps, drawn as a pixel art sprite.
 *
 * The sprite sheet comes out of [PixelPlanet] as raw pixel buffers and is only turned into
 * bitmaps here. Scaling uses [FilterQuality.None] and a whole-number factor, so every sprite
 * pixel lands on an exact block of screen pixels and nothing goes soft at the edges.
 */
@Composable
fun CelestialBody(
    tier: CelestialTier,
    modifier: Modifier = Modifier,
) {
    // Rendering 24 frames costs a few milliseconds, so it happens off the main thread and the
    // body simply appears once it is ready.
    val frames by produceState<List<ImageBitmap>?>(initialValue = null, tier.index) {
        value = withContext(Dispatchers.Default) {
            PixelPlanet.frames(tier).map { it.toImageBitmap() }
        }
    }

    val transition = rememberInfiniteTransition(label = "body-${tier.index}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(PixelPlanet.spinMillis(tier.kind), easing = LinearEasing),
        ),
        label = "spin",
    )

    Canvas(modifier = modifier) {
        val sheet = frames ?: return@Canvas
        val index = (phase * PixelPlanet.FRAMES).toInt().coerceIn(0, sheet.lastIndex)

        val available = min(size.width, size.height) * PixelPlanet.spriteFraction(tier)
        // Whole-number scaling is what keeps the pixels square.
        val scale = (available / PixelPlanet.SIZE).toInt().coerceAtLeast(1)
        val side = PixelPlanet.SIZE * scale

        drawImage(
            image = sheet[index],
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(PixelPlanet.SIZE, PixelPlanet.SIZE),
            dstOffset = IntOffset(
                ((size.width - side) / 2f).roundToInt(),
                ((size.height - side) / 2f).roundToInt(),
            ),
            dstSize = IntSize(side, side),
            filterQuality = FilterQuality.None,
        )
    }
}

private fun IntArray.toImageBitmap(): ImageBitmap {
    val bitmap = Bitmap.createBitmap(PixelPlanet.SIZE, PixelPlanet.SIZE, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(this, 0, PixelPlanet.SIZE, 0, 0, PixelPlanet.SIZE, PixelPlanet.SIZE)
    return bitmap.asImageBitmap()
}
