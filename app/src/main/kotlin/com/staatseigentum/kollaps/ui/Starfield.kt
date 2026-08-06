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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float,
    val warm: Boolean,
)

/**
 * The background: a fixed star pattern that twinkles, plus two soft nebulae tinted in the colour
 * of the body the player is currently on, so the whole screen shifts mood as they progress.
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
                radius = 0.4f + random.nextFloat() * 1.6f,
                phase = random.nextFloat(),
                speed = 0.4f + random.nextFloat() * 1.4f,
                warm = random.nextFloat() < 0.25f,
            )
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
        val width = size.width
        val height = size.height

        drawCircle(
            brush = Brush.radialGradient(
                0f to tint.copy(alpha = 0.16f),
                1f to Color.Transparent,
                center = Offset(width * 0.18f, height * 0.22f),
                radius = width * 0.75f,
            ),
            radius = width * 0.75f,
            center = Offset(width * 0.18f, height * 0.22f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to tint.copy(alpha = 0.10f),
                1f to Color.Transparent,
                center = Offset(width * 0.88f, height * 0.72f),
                radius = width * 0.65f,
            ),
            radius = width * 0.65f,
            center = Offset(width * 0.88f, height * 0.72f),
        )

        for (star in stars) {
            val wave = abs(sin(((time * star.speed + star.phase) * 2f * PI).toFloat()))
            val alpha = 0.25f + 0.75f * wave
            drawCircle(
                color = if (star.warm) WarmStar else Color.White,
                radius = star.radius,
                center = Offset(star.x * width, star.y * height),
                alpha = alpha,
            )
        }
    }
}

private const val STAR_COUNT = 150
private val WarmStar = Color(0xFFFFD7A8)
