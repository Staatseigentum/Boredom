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
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.random.Random

private class Star(
    val x: Float,
    val y: Float,
    /** 1 for a single block, 2 for a fatter one. */
    val blocks: Int,
    val offset: Float,
    val speed: Float,
    val warm: Boolean,
)

/**
 * The background: stars as square blocks snapped to a pixel grid, blinking between a handful of
 * fixed brightnesses instead of fading smoothly, plus two dithered nebula clouds tinted in the
 * colour of the body the player is currently on.
 */
@Composable
fun Starfield(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val stars = remember {
        val random = Random(0x5EED)
        List(STAR_COUNT) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                blocks = if (random.nextFloat() < 0.22f) 2 else 1,
                offset = random.nextFloat(),
                speed = 0.5f + random.nextFloat() * 1.5f,
                warm = random.nextFloat() < 0.25f,
            )
        }
    }
    val clouds = remember {
        val random = Random(0xC10D)
        List(CLOUD_BLOCKS) {
            Offset(random.nextFloat(), random.nextFloat()) to random.nextFloat()
        }
    }

    val transition = rememberInfiniteTransition(label = "starfield")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "twinkle",
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val block = PIXEL.toPx()

        // Two soft clouds, drawn as scattered blocks so they stay part of the pixel grid.
        for ((position, weight) in clouds) {
            val nearFirst = 1f - ((position.x - 0.2f) * (position.x - 0.2f) +
                (position.y - 0.22f) * (position.y - 0.22f)) * 3.2f
            val nearSecond = 1f - ((position.x - 0.85f) * (position.x - 0.85f) +
                (position.y - 0.74f) * (position.y - 0.74f)) * 3.6f
            val density = maxOf(nearFirst, nearSecond)
            if (density <= weight) continue
            drawBlock(
                position.x * size.width,
                position.y * size.height,
                block * 2f,
                tint.copy(alpha = 0.12f + 0.10f * density),
            )
        }

        for (star in stars) {
            // Four brightness steps, switched rather than faded.
            val wave = ((time * star.speed + star.offset) * 4f) % 1f
            val step = floor(wave * 4f).toInt()
            val alpha = when (step) {
                0 -> 0.35f
                1 -> 0.7f
                2 -> 1f
                else -> 0.55f
            }
            drawBlock(
                star.x * size.width,
                star.y * size.height,
                block * star.blocks,
                (if (star.warm) WarmStar else Color.White).copy(alpha = alpha),
            )
        }
    }
}

/** Snaps to the pixel grid so every star sits on a whole block. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlock(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
) {
    val snappedX = floor(x / size) * size
    val snappedY = floor(y / size) * size
    drawRect(color = color, topLeft = Offset(snappedX, snappedY), size = Size(size, size))
}

private const val STAR_COUNT = 130
private const val CLOUD_BLOCKS = 320
private val PIXEL = 3.dp
private val WarmStar = Color(0xFFFFD7A8)
