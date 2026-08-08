package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.pixel.Sky
import kotlin.math.floor

/**
 * The background: a sky with depth in it.
 *
 * Three layers of stars drifting at three speeds, nebulae built from overlapping lobes rather than
 * circles, and a dust lane cutting across the whole thing. Everything is snapped to the same pixel
 * grid the sprites use and drawn as flat blocks — a smooth gradient here would be the one soft
 * thing on a screen made entirely of hard edges.
 *
 * What each block *is* comes from [Sky], which is arithmetic and can be tested; what is left here
 * is where it lands and what colour it takes, which is a loop over rectangles. The fields are
 * worked out once and kept, because deciding whether a block belongs to a cloud costs far more
 * than drawing it and none of it changes between frames. What changes is only where the whole
 * field sits, which is one addition per block.
 */
@Composable
fun Starfield(
    tint: Color,
    modifier: Modifier = Modifier,
    /** How far up the ladder the player is, in `0f..1f`. Deeper skies for later bodies. */
    depth: Float = 0f,
) {
    val stars = remember { Sky.stars() }
    // Rebuilt when the body changes rung, which is a few times an hour.
    val clouds = remember(depth) { Sky.clouds(depth) }
    val dust = remember { Sky.dust() }

    val transition = rememberInfiniteTransition(label = "starfield")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "twinkle",
    )
    // Four minutes for one pass. Slow enough that nobody catches it moving, fast enough that
    // looking up after a while shows a different sky.
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(240_000, easing = LinearEasing)),
        label = "drift",
    )

    val accent = lerp(tint, NebulaAccent, 0.65f)

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val block = PIXEL.toPx()

        // Furthest first, so a near star is never hidden behind something that is behind it.
        for (cloud in clouds) {
            drawBlock(
                x = Sky.wrap(cloud.x + drift * CLOUD_DRIFT) * size.width,
                y = cloud.y * size.height,
                size = block * if (cloud.wide) 3f else 2f,
                color = lerp(tint, accent, cloud.warmth).copy(alpha = cloud.alpha),
            )
        }

        for (grain in dust) {
            drawBlock(
                x = Sky.wrap(grain.x + drift * DUST_DRIFT) * size.width,
                y = grain.y * size.height,
                size = block,
                color = DustColor.copy(alpha = grain.alpha),
            )
        }

        for (star in stars) {
            val layer = star.layer
            val alpha = if (layer.twinkles) {
                // Four brightness steps, switched rather than faded.
                val wave = ((twinkle * star.speed + star.offset) * 4f) % 1f
                when (floor(wave * 4f).toInt()) {
                    0 -> 0.35f
                    1 -> 0.7f
                    2 -> 1f
                    else -> 0.55f
                }
            } else {
                // Still varied, but fixed per star: a hundred and fifty blinking specks is noise.
                0.55f + star.offset * 0.45f
            } * layer.brightness

            drawBlock(
                x = Sky.wrap(star.x + drift * layer.drift) * size.width,
                y = star.y * size.height,
                size = block * layer.blocks,
                color = (if (star.warm) WarmStar else Color.White).copy(alpha = alpha),
            )
        }
    }
}

/** Snaps to the pixel grid so every block sits whole. */
private fun DrawScope.drawBlock(x: Float, y: Float, size: Float, color: Color) {
    val snappedX = floor(x / size) * size
    val snappedY = floor(y / size) * size
    drawRect(color = color, topLeft = Offset(snappedX, snappedY), size = Size(size, size))
}

private const val CLOUD_DRIFT = 0.05f
private const val DUST_DRIFT = 0.08f

private val PIXEL: Dp = 3.dp
private val WarmStar = Color(0xFFFFD7A8)

/** What the tint is mixed towards, so a nebula has two colours in it rather than one. */
private val NebulaAccent = Color(0xFF7A4ACF)
private val DustColor = Color(0xFFBFA9E0)
