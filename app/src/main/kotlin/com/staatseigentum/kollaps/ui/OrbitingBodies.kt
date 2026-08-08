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
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Orbit
import com.staatseigentum.kollaps.core.Orbits
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Outline
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/** One body up there, worked out once per state change rather than every frame. */
private class Body(
    val orbit: Orbit,
    val radius: Float,
    /** Turns per animation cycle. Inner orbits go round more often, as they should. */
    val speed: Float,
    val phase: Float,
    /** Edge length in grid blocks, from the tier it counts as. */
    val blocks: Int,
    val colour: Color,
    val resonant: Boolean,
)

/**
 * The system, drawn: the slots as faint rings and whatever stands on them going round.
 *
 * Periods follow Kepler rather than being picked to look busy — the time to go round scales with
 * the radius to the power of one and a half, so the inner bodies visibly race and the outer ones
 * barely move. It costs one `pow` per body per state change and it is the difference between a
 * system and a set of spinning dots.
 *
 * A resonant pair lights its rings, which is the only place in the game where the mechanic and
 * the picture are the same thing: two bodies whose rings glow really are the two whose periods
 * divide evenly.
 */
@Composable
fun OrbitingBodies(
    state: GameState,
    modifier: Modifier = Modifier,
) {
    val bodies = remember(state.orbits, state.satellites, state.runMass) { build(state) }
    if (bodies.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "system")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CYCLE_MILLIS, easing = LinearEasing)),
        label = "revolution",
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val block = floor(3.dp.toPx()).coerceAtLeast(2f)
        val centre = Offset(size.width / 2f, size.height / 2f)
        val reach = minOf(size.width, size.height) / 2f

        // The rings first, all of them, so a body is never drawn under the path of a wider one.
        for (body in bodies) {
            val ring = if (body.resonant) Ember.copy(alpha = 0.35f) else Outline.copy(alpha = 0.5f)
            val radius = body.radius * reach
            val dots = (radius / block * 0.5f).toInt().coerceIn(16, 90)
            for (index in 0 until dots) {
                val angle = index.toFloat() / dots * TWO_PI
                drawBlock(
                    x = centre.x + cos(angle) * radius,
                    y = centre.y + sin(angle) * radius,
                    size = block,
                    color = ring,
                )
            }
        }

        for (body in bodies) {
            if (body.blocks <= 0) continue
            val angle = (turn * body.speed + body.phase) * TWO_PI
            val radius = body.radius * reach
            drawDisc(
                centre = Offset(centre.x + cos(angle) * radius, centre.y + sin(angle) * radius),
                blocks = body.blocks,
                block = block,
                colour = body.colour,
            )
        }
    }
}

private fun build(state: GameState): List<Body> {
    val opened = Orbits.opened(state)
    if (opened.isEmpty()) return emptyList()

    return opened.map { orbit ->
        val occupied = Orbits.isOccupied(state, orbit)
        val tier = Orbits.tierOn(state, orbit)
        Body(
            orbit = orbit,
            radius = orbit.radius,
            // Kepler: the period goes as the radius to the three halves, so the rate is the
            // inverse of that. Scaled off the innermost slot so the fastest one is one turn.
            speed = (Orbits.all.first().radius / orbit.radius).pow(1.5f),
            // Spread so nothing starts in a line, and stable between frames.
            phase = (orbit.index * 0.37f) % 1f,
            blocks = if (!occupied) 0 else 2 + tier.index / 5,
            colour = lerp(Color(tier.primaryColor), Color(tier.glowColor), 0.35f),
            resonant = occupied && Orbits.resonantWith(state, orbit).isNotEmpty(),
        )
    }
}

/** A small round body, built out of whole grid blocks like everything else on screen. */
private fun DrawScope.drawDisc(centre: Offset, blocks: Int, block: Float, colour: Color) {
    val reach = blocks / 2f
    for (row in -blocks / 2..blocks / 2) {
        for (column in -blocks / 2..blocks / 2) {
            // The +0.4 rounds the silhouette off: without it a three-block body is a square.
            if (row * row + column * column > (reach + 0.4f) * (reach + 0.4f)) continue
            drawBlock(
                x = centre.x + column * block,
                y = centre.y + row * block,
                size = block,
                color = colour,
            )
        }
    }
}

private fun DrawScope.drawBlock(x: Float, y: Float, size: Float, color: Color) {
    val snappedX = floor(x / size) * size
    val snappedY = floor(y / size) * size
    drawRect(color = color, topLeft = Offset(snappedX, snappedY), size = Size(size, size))
}

/** One full turn of the innermost slot. Slow enough to watch, fast enough to notice. */
private const val CYCLE_MILLIS = 42_000
private const val TWO_PI = 6.2831855f
