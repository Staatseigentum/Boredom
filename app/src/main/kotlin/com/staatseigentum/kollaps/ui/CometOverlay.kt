package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.core.Comets
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

/** A comet on its way across the sky, and where it has got to. */
private class Flight(val comet: Comet, val fromTop: Float, val toTop: Float, val leftToRight: Boolean)

/**
 * The comet that drifts past every few minutes and pays out only if it is caught.
 *
 * Owns its own clock rather than riding the game tick, because it is about the player being
 * present: it should keep moving smoothly between ticks, and it should stop entirely when the
 * screen is gone, which is exactly what a frame callback already does.
 */
@Composable
fun CometOverlay(
    state: GameState,
    frequency: Double,
    onCatch: (Comet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val random = remember { Random(System.nanoTime()) }
    var flight by remember { mutableStateOf<Flight?>(null) }
    val travel = remember { Animatable(0f) }
    val appears = Comets.appearsAt(state)

    LaunchedEffect(appears, frequency) {
        if (!appears) return@LaunchedEffect
        while (true) {
            waitSeconds(Comets.nextDelay(random, frequency))
            flight = Flight(
                comet = Comets.pick(random),
                fromTop = 0.15f + random.nextFloat() * 0.5f,
                toTop = 0.15f + random.nextFloat() * 0.5f,
                leftToRight = random.nextBoolean(),
            )
            travel.snapTo(0f)
            // Crossing the screen is the whole window in which it can be caught.
            travel.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(
                durationMillis = (Comets.VISIBLE_SECONDS * 1000).toInt(),
                easing = androidx.compose.animation.core.LinearEasing,
            ))
            flight = null
        }
    }

    val current = flight ?: return
    val progress = travel.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(current) {
                detectTapGestures { tap ->
                    val head = headOf(current, progress, size.width.toFloat(), size.height.toFloat())
                    // A generous radius: it is moving, it is small, and missing a comet the
                    // player did see is far more annoying than catching one they nearly missed.
                    val reach = 44.dp.toPx()
                    if (abs(tap.x - head.x) < reach && abs(tap.y - head.y) < reach) {
                        onCatch(current.comet)
                        flight = null
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val head = headOf(current, progress, size.width, size.height)
            val block = floor(3.dp.toPx()).coerceAtLeast(2f)

            // A tail of blocks trailing behind, thinning out — drawn on the grid so it stays
            // part of the same picture as the planets.
            val backwards = if (current.leftToRight) -1f else 1f
            for (step in TAIL_BLOCKS downTo 1) {
                val distance = step * block * 1.6f
                val fade = 1f - step.toFloat() / TAIL_BLOCKS
                drawRect(
                    color = Ember.copy(alpha = 0.10f + 0.5f * fade * fade),
                    topLeft = Offset(
                        snap(head.x + backwards * distance, block),
                        snap(head.y - distance * 0.22f, block),
                    ),
                    size = Size(block, block),
                )
            }

            // The head: a two by two block with a lighter core, so it reads as solid.
            drawRect(
                color = Ember,
                topLeft = Offset(snap(head.x - block, block), snap(head.y - block, block)),
                size = Size(block * 2, block * 2),
            )
            drawRect(
                color = Starlight,
                topLeft = Offset(snap(head.x, block), snap(head.y - block, block)),
                size = Size(block, block),
            )
        }
    }
}

/** Where the head is right now, in pixels. */
private fun headOf(flight: Flight, progress: Float, width: Float, height: Float): Offset {
    // Starts and ends off screen, so it drifts in rather than blinking into existence.
    val from = if (flight.leftToRight) -0.1f else 1.1f
    val to = if (flight.leftToRight) 1.1f else -0.1f
    val x = (from + (to - from) * progress) * width
    val y = (flight.fromTop + (flight.toTop - flight.fromTop) * progress) * height
    return Offset(x, y)
}

private fun snap(value: Float, block: Float): Float = floor(value / block) * block

private suspend fun waitSeconds(seconds: Double) {
    var left = seconds * 1_000_000_000.0
    var previous = 0L
    while (left > 0) {
        withFrameNanos { now ->
            if (previous != 0L) left -= (now - previous).toDouble()
            previous = now
        }
    }
}

private const val TAIL_BLOCKS = 9

/** Colour is not enough to tell the three comets apart, so the catch says which it was. */
val Comet.accent: Color
    get() = when (this) {
        Comet.WINDFALL -> Ember
        Comet.SURGE -> Color(0xFF5CE1A6)
        Comet.FRENZY -> Color(0xFF7C5CFF)
    }
