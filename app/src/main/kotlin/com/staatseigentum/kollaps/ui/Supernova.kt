package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/** How big a blast is, and what colour it burns. */
enum class BlastKind(
    val seconds: Int,
    val rings: Int,
    val flash: Float,
    val core: Color,
    val edge: Color,
) {
    /** A collapse: the black hole falls in on itself and throws the rest outward. */
    KOLLAPS(
        seconds = 1_400,
        rings = 4,
        flash = 0.85f,
        core = Color(0xFFFFF6E0),
        edge = Color(0xFFFF9A2E),
    ),

    /** A big bang: bigger, slower, and the wrong colour for anything that ever lived. */
    URKNALL(
        seconds = 2_200,
        rings = 6,
        flash = 1f,
        core = Color(0xFFFFFFFF),
        edge = Color(0xFFB88AFF),
    ),
}

/**
 * The blast that goes off when a run ends.
 *
 * Pressing the collapse button used to do nothing visible: a dialog closed, and the numbers were
 * suddenly small again. It is the single biggest moment in the game — four hours of play cashed in
 * — and it looked exactly like nothing happening.
 *
 * Built from the same blocks everything else here is: rings of squares flying outward on a pixel
 * grid, not a smooth glow. The flash is a flat white rectangle over the whole screen, because a
 * gradient would be the one soft thing in a game made of hard edges.
 *
 * @param trigger anything that changes when a new blast should start. Null means no blast.
 */
@Composable
fun Blast(
    trigger: Any?,
    kind: BlastKind,
    modifier: Modifier = Modifier,
    /**
     * Where it goes off. Unspecified means the middle of the screen.
     *
     * The big bang squeezes the universe onto the middle of the *tap area*, which on a phone sits
     * well above the middle of the screen — and rings that then bloomed from somewhere else would
     * say the explosion had nothing to do with the point the player had just been staring at.
     */
    centre: Offset = Offset.Unspecified,
    onFinished: () -> Unit = {},
) {
    if (trigger == null) return

    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(kind.seconds, easing = LinearEasing))
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val time = progress.value
        if (time >= 1f) return@Canvas

        val block = BLOCK.toPx()
        // Far enough to clear a corner from the middle of the screen, whatever the shape.
        val reach = sqrt(size.width * size.width + size.height * size.height) / 2f
        val origin = if (centre.isSpecified) centre else Offset(size.width / 2f, size.height / 2f)

        // The flash first and underneath: it is the light, the rings are what the light is made of.
        val flash = kind.flash * (1f - time * FLASH_FADE).coerceAtLeast(0f)
        if (flash > 0f) {
            drawRect(color = kind.core.copy(alpha = flash * flash))
        }

        for (ring in 0 until kind.rings) {
            // Each ring sets off a little after the one before it, so the blast has a front and
            // a wake rather than being one expanding line.
            val delay = ring.toFloat() / (kind.rings * 2f)
            val life = (time - delay) / (1f - delay)
            if (life <= 0f || life >= 1f) continue

            val radius = reach * life * OVERSHOOT
            val alpha = (1f - life) * (1f - life)
            val colour = if (ring % 2 == 0) kind.core else kind.edge

            // More blocks further out, so the ring stays a ring instead of thinning into dots.
            val count = (radius / block * 1.6f).toInt().coerceIn(12, 220)
            val side = block * (2f - life)

            for (index in 0 until count) {
                val angle = index.toFloat() / count * TWO_PI + ring * 0.4f
                drawBlock(
                    x = origin.x + cos(angle) * radius,
                    y = origin.y + sin(angle) * radius,
                    size = side,
                    color = colour.copy(alpha = alpha * 0.9f),
                )
            }
        }
    }
}

/**
 * A single ring, thrown off the body when it reaches a new rung.
 *
 * Much smaller than a [Blast] and drawn inside the tap area rather than over the whole screen: a
 * step up the ladder happens twenty-four times a run, and anything that covered the screen that
 * often would stop being an event and start being an interruption.
 */
@Composable
fun TierPulse(
    trigger: Any?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (trigger == null) return
    // Nothing here carries information — reaching a rung already announces itself in the HUD, the
    // ladder column and the celebration. So under reduced motion it simply does not happen, rather
    // than happening more gently.
    if (LocalReduceMotion.current) return

    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(900, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val time = progress.value
        if (time >= 1f) return@Canvas

        val block = BLOCK.toPx()
        val centre = Offset(size.width / 2f, size.height / 2f)
        val reach = minOf(size.width, size.height) / 2f

        for (ring in 0 until 2) {
            // A hundred and twenty milliseconds of the nine hundred, so the second ring is a wake
            // behind the first rather than a second event.
            val offset = ring * (120f / 900f)
            val raw = (time - offset) / (1f - offset)
            if (raw <= 0f || raw >= 1f) continue
            // Stepped, for the same reason the tap's rings are: this expands past a sprite made of
            // hard blocks, and the one smooth thing in that picture is the one that looks wrong.
            val life = quantise(raw, PULSE_STEPS)

            val radius = reach * (0.55f + life * 0.75f)
            val alpha = (1f - life) * 0.9f
            val count = (radius / block * 1.4f).toInt().coerceIn(10, 140)
            val side = block * (1.6f - life * 0.8f)

            for (index in 0 until count) {
                val angle = index.toFloat() / count * TWO_PI
                drawBlock(
                    x = centre.x + cos(angle) * radius,
                    y = centre.y + sin(angle) * radius,
                    size = side,
                    color = color.copy(alpha = alpha),
                )
            }
        }
    }
}

/** Steps the celebration ring expands in. See [quantise]. */
private const val PULSE_STEPS = 12

/** Snaps to the pixel grid, the same way the sky and the sprites do. */
private fun DrawScope.drawBlock(x: Float, y: Float, size: Float, color: Color) {
    if (size <= 0f) return
    val snappedX = floor(x / size) * size
    val snappedY = floor(y / size) * size
    drawRect(color = color, topLeft = Offset(snappedX, snappedY), size = Size(size, size))
}

/** How far past the corner the last ring travels, so nothing stops mid-screen. */
private const val OVERSHOOT = 1.25f

/** The flash is gone well before the rings are: it is the bang, not the debris. */
private const val FLASH_FADE = 2.6f

private const val TWO_PI = 6.2831855f
private val BLOCK = 4.dp
