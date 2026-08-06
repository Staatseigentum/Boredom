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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    // Rendering the sheet costs tens of milliseconds, so it happens off the main thread and the
    // body appears once it is ready. The cache matters here: the tap area and the tier
    // celebration are two separate composables showing the same body at the same moment, and
    // without it they would each pay for their own copy.
    val frames by produceState<List<ImageBitmap>?>(initialValue = SpriteCache.ready(tier), tier.index) {
        if (value == null) value = SpriteCache.sheet(tier)
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

    val side = PixelPlanet.size(tier)

    Canvas(modifier = modifier) {
        val sheet = frames ?: return@Canvas
        val index = (phase * PixelPlanet.FRAMES).toInt().coerceIn(0, sheet.lastIndex)

        val available = min(size.width, size.height) * PixelPlanet.spriteFraction(tier)
        // Whole-number scaling is what keeps the pixels square.
        val scale = (available / side).toInt().coerceAtLeast(1)
        val drawn = side * scale

        drawImage(
            image = sheet[index],
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(side, side),
            dstOffset = IntOffset(
                ((size.width - drawn) / 2f).roundToInt(),
                ((size.height - drawn) / 2f).roundToInt(),
            ),
            dstSize = IntSize(drawn, drawn),
            filterQuality = FilterQuality.None,
        )
    }
}

/**
 * Keeps the last couple of sprite sheets around.
 *
 * Two is enough on purpose: the game only ever shows the current body, and the sheet for the
 * largest tier is several megabytes. Holding all eighteen would cost far more memory than the
 * rendering it saves is worth.
 */
private object SpriteCache {

    private const val KEEP = 2

    private val lock = Mutex()
    private val sheets = LinkedHashMap<Int, List<ImageBitmap>>()

    /** The sheet if it has already been built, for showing a body without a blank frame first. */
    fun ready(tier: CelestialTier): List<ImageBitmap>? = synchronized(sheets) { sheets[tier.index] }

    suspend fun sheet(tier: CelestialTier): List<ImageBitmap> {
        ready(tier)?.let { return it }
        return lock.withLock {
            // Another caller may have finished it while this one waited for the lock.
            ready(tier) ?: withContext(Dispatchers.Default) {
                val side = PixelPlanet.size(tier)
                PixelPlanet.frames(tier).map { it.toImageBitmap(side) }
            }.also { built ->
                synchronized(sheets) {
                    sheets[tier.index] = built
                    while (sheets.size > KEEP) {
                        val oldest = sheets.keys.first()
                        sheets.remove(oldest)
                    }
                }
            }
        }
    }
}

private fun IntArray.toImageBitmap(side: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(this, 0, side, 0, 0, side, side)
    return bitmap.asImageBitmap()
}
