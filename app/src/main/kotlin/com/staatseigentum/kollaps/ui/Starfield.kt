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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.pixel.Sky
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

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
    /**
     * How hard the sky is being wound into the hole, `0f` for not at all.
     *
     * The sky takes part in the collapse rather than sitting behind it. Anything less looks like a
     * photograph of space with an animation played on top; this way the whole picture goes down
     * the drain together.
     *
     * A lambda for the same reason the twinkle and the drift are read inside the canvas below: it
     * changes every frame, and read as a parameter it would recompose rather than redraw.
     */
    warp: () -> Float = { 0f },
    /** Where it is being wound into. Unspecified means the middle of the canvas. */
    warpCentre: () -> Offset = { Offset.Unspecified },
    /**
     * The big bang's three, and the reason it does not look like the collapse.
     *
     * [crush] squashes the sky onto the horizontal line through the centre, [pinch] then squeezes
     * that line sideways onto a single point, and [birth] is how much of the sky exists at all —
     * zero through the silence, then opening back out of the point. Nothing here turns: the
     * collapse winds the sky up, this one folds it in half and then in half again.
     */
    crush: () -> Float = { 0f },
    pinch: () -> Float = { 0f },
    birth: () -> Float = { 1f },
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
        val pull = warp().coerceIn(0f, 1f)
        val centre = warpCentre()
        val eye = if (centre.isSpecified) centre else Offset(size.width / 2f, size.height / 2f)

        val flat = crush().coerceIn(0f, 1f)
        val squeeze = pinch().coerceIn(0f, 1f)
        val born = birth().coerceIn(0f, 1f)
        // Nothing to draw at all, which is what the silence before the bang is made of.
        if (born <= 0f) return@Canvas
        val fold = Fold(eye, flat, squeeze, born)

        // Furthest first, so a near star is never hidden behind something that is behind it.
        for (cloud in clouds) {
            drawBlock(
                point = wound(
                    Offset(
                        Sky.wrap(cloud.x + drift * CLOUD_DRIFT) * size.width,
                        cloud.y * size.height,
                    ),
                    pull,
                    eye,
                ),
                fold = fold,
                size = block * if (cloud.wide) 3f else 2f,
                color = lerp(tint, accent, cloud.warmth).copy(alpha = cloud.alpha * born),
            )
        }

        for (grain in dust) {
            drawBlock(
                point = wound(
                    Offset(
                        Sky.wrap(grain.x + drift * DUST_DRIFT) * size.width,
                        grain.y * size.height,
                    ),
                    pull,
                    eye,
                ),
                fold = fold,
                size = block,
                color = DustColor.copy(alpha = grain.alpha * born),
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
                point = wound(
                    Offset(
                        Sky.wrap(star.x + drift * layer.drift) * size.width,
                        star.y * size.height,
                    ),
                    pull,
                    eye,
                ),
                fold = fold,
                size = block * layer.blocks,
                color = (if (star.warm) WarmStar else Color.White).copy(alpha = alpha * born),
            )
        }
    }
}

/**
 * Where a block of sky ends up once the hole has hold of it.
 *
 * Polar around the same centre everything else falls towards: pulled most of the way in and turned
 * through a third of a circle at full strength. The field itself is never rebuilt — it is the same
 * hundred and fifty stars, read through a different lens for a second and a half.
 */
private fun wound(point: Offset, warp: Float, centre: Offset): Offset {
    if (warp <= 0f) return point
    val dx = point.x - centre.x
    val dy = point.y - centre.y
    val radius = hypot(dx, dy) * (1f - warp * 0.92f)
    val angle = atan2(dy, dx) + warp * 2.3f
    return Offset(centre.x + cos(angle) * radius, centre.y + sin(angle) * radius)
}

/**
 * How the big bang folds the sky, as one small object rather than four loose numbers.
 *
 * Y first and X second, in that order and never together: the sky is pressed onto a horizontal
 * line, and only once it is a line is the line squeezed sideways onto a point. Doing both at once
 * would give a diagonal collapse into the middle, which is a different — and much duller — event.
 * [born] runs the whole thing backwards out of the point afterwards.
 */
private class Fold(
    val centre: Offset,
    val crush: Float,
    val pinch: Float,
    val born: Float,
) {
    val idle: Boolean get() = crush <= 0f && pinch <= 0f && born >= 1f

    operator fun invoke(point: Offset): Offset {
        if (idle) return point
        var x = centre.x + (point.x - centre.x) * (1f - pinch)
        var y = centre.y + (point.y - centre.y) * (1f - crush)
        if (born < 1f) {
            x = centre.x + (x - centre.x) * born
            y = centre.y + (y - centre.y) * born
        }
        return Offset(x, y)
    }
}

/** Snaps to the pixel grid so every block sits whole. */
private fun DrawScope.drawBlock(point: Offset, fold: Fold, size: Float, color: Color) {
    val placed = fold(point)
    val snappedX = floor(placed.x / size) * size
    val snappedY = floor(placed.y / size) * size
    drawRect(color = color, topLeft = Offset(snappedX, snappedY), size = Size(size, size))
}

private const val CLOUD_DRIFT = 0.05f
private const val DUST_DRIFT = 0.08f

private val PIXEL: Dp = 3.dp
private val WarmStar = Color(0xFFFFD7A8)

/** What the tint is mixed towards, so a nebula has two colours in it rather than one. */
private val NebulaAccent = Color(0xFF7A4ACF)
private val DustColor = Color(0xFFBFA9E0)
