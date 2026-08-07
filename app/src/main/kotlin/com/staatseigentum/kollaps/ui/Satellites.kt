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
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.Collectors
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** One thing in orbit: which ring it sits on, where it started, how fast it goes. */
private class Orbiter(val ring: Int, val phase: Float, val speed: Float, val colour: Color)

/**
 * The collectors, in orbit around the body.
 *
 * In Cookie Clicker you can see your farms; here the entire fleet was a list of numbers in a
 * shop, and nothing on the screen changed however much of it you owned. Each kind of collector
 * gets its own ring and its own colour, and the number of blocks on a ring grows with the count
 * — slowly, because the counts run to hundreds and a ring of hundreds is a smear.
 */
@Composable
fun Satellites(
    state: GameState,
    tier: CelestialTier,
    modifier: Modifier = Modifier,
) {
    val orbiters = remember(state.collectors, tier.index) { buildOrbiters(state) }
    if (orbiters.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "orbit")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ORBIT_MILLIS, easing = LinearEasing)),
        label = "turn",
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val block = floor(3.dp.toPx()).coerceAtLeast(2f)
        val centreX = size.width / 2f
        val centreY = size.height / 2f
        // Just outside the body itself, so nothing orbits through the planet.
        val bodyRadius = min(size.width, size.height) * PixelPlanet.spriteFraction(tier) * 0.31f

        for (orbiter in orbiters) {
            val radius = bodyRadius * (1.18f + orbiter.ring * 0.16f)
            val angle = (orbiter.phase + turn * orbiter.speed) * TWO_PI
            // Flattened, so the rings read as orbits seen at an angle rather than as circles.
            val x = centreX + cos(angle) * radius
            val y = centreY + sin(angle) * radius * 0.42f

            drawRect(
                color = orbiter.colour,
                topLeft = Offset(snap(x, block), snap(y, block)),
                size = Size(block, block),
            )
        }
    }
}

private fun buildOrbiters(state: GameState): List<Orbiter> {
    val result = ArrayList<Orbiter>()
    Collectors.all.forEachIndexed { ring, collector ->
        val owned = state.ownedOf(collector.id)
        if (owned <= 0) return@forEachIndexed

        // Counts reach the hundreds; drawing one block each would be a solid ring. Every
        // doubling adds one, so the ring keeps growing without ever becoming a smear.
        val blocks = (1 + log2(owned.toFloat())).toInt().coerceIn(1, MAX_PER_RING)
        val colour = RING_COLOURS[ring % RING_COLOURS.size]
        // A prime-ish stride so the blocks on a ring never line up into a single clump.
        for (index in 0 until blocks) {
            result += Orbiter(
                ring = ring,
                phase = (index * 0.61803f) % 1f,
                speed = if (ring % 2 == 0) 1f else -0.78f,
                colour = colour,
            )
        }
    }
    return result
}

private fun log2(value: Float): Float {
    var result = 0f
    var current = value
    while (current > 1f) {
        current /= 2f
        result++
    }
    return result
}

private fun snap(value: Float, block: Float): Float = floor(value / block) * block

private val RING_COLOURS = listOf(
    Color(0xFF8E95C4),
    Color(0xFF5CE1A6),
    Color(0xFFFFB74D),
    Color(0xFF7C5CFF),
    Color(0xFFE9ECFF),
    Color(0xFF6EC6FF),
)

private const val MAX_PER_RING = 9
private const val ORBIT_MILLIS = 22_000
private const val TWO_PI = 6.2831855f
