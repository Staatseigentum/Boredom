package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.core.Comets
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlin.math.floor
import kotlin.math.roundToInt
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
    // Read fresh rather than captured: the loop below outlives many recompositions, and a
    // captured sound object goes on making noise long after the setting was switched off.
    val sfx by rememberUpdatedState(LocalSfx.current)

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
            // Said as it enters, not as it is caught: the whole difficulty of a comet is
            // noticing one at all, and the screen is usually not what the player is looking at.
            sfx?.flyby()
            travel.snapTo(0f)
            // Crossing the screen is the whole window in which it can be caught.
            travel.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(
                durationMillis = (Comets.VISIBLE_SECONDS * 1000).toInt(),
                easing = androidx.compose.animation.core.LinearEasing,
            ))
            // Still there at the far edge means it was never caught — catching clears it from
            // the tap handler. That is the only place a miss can be told from a catch, because
            // the animation runs to the end either way.
            if (flight != null) {
                sfx?.missed()
                flight = null
            }
        }
    }

    val current = flight ?: return
    val progress = travel.value

    // Reset for every flight, so a core that got away half broken arrives whole next time.
    var struck by remember(current) { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        // The catcher is a small box that follows the head, not a sheet over the whole screen.
        // A full-size tap catcher swallowed every tap that missed, so for the eleven seconds a
        // comet was crossing, tapping the planet quietly did nothing.
        val reach = 44.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    val side = (reach * 2).roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val head = headOf(
                            current,
                            progress,
                            constraints.maxWidth.toFloat(),
                            constraints.maxHeight.toFloat(),
                        )
                        placeable.place(
                            (head.x - side / 2f).roundToInt(),
                            (head.y - side / 2f).roundToInt(),
                        )
                    }
                }
                .pointerInput(current) {
                    // Anywhere in the box counts: it is moving, it is small, and missing a comet
                    // the player did see is worse than catching one they nearly missed.
                    detectTapGestures {
                        // A hard core needs hitting again while it is still moving, which is a
                        // different skill from spotting it in the first place.
                        if (struck + 1 >= current.comet.hits) {
                            sfx?.comet()
                            onCatch(current.comet)
                            flight = null
                        } else {
                            struck++
                            sfx?.click()
                        }
                    }
                },
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val head = headOf(current, progress, size.width, size.height)
            val block = floor(3.dp.toPx()).coerceAtLeast(2f)

            // A tail of blocks trailing behind, thinning out — drawn on the grid so it stays
            // part of the same picture as the planets.
            val accent = current.comet.accent
            val backwards = if (current.leftToRight) -1f else 1f
            for (step in TAIL_BLOCKS downTo 1) {
                val distance = step * block * 1.6f
                val fade = 1f - step.toFloat() / TAIL_BLOCKS
                drawRect(
                    color = accent.copy(alpha = 0.10f + 0.5f * fade * fade),
                    topLeft = Offset(
                        snap(head.x + backwards * distance, block),
                        snap(head.y - distance * 0.22f, block),
                    ),
                    size = Size(block, block),
                )
            }

            // The head, in the colour of what it is carrying: three comets that all looked the
            // same was three comets the player could not tell apart until after catching one.
            // A hard core is drawn wider and loses a ring with every hit, so the crust visibly
            // comes off rather than the count living only in the tap handler.
            val crust = current.comet.hits - struck
            val side = block * (1 + crust)
            drawRect(
                color = accent,
                topLeft = Offset(snap(head.x - side / 2f, block), snap(head.y - side / 2f, block)),
                size = Size(side, side),
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
        Comet.ICE_CORE -> Color(0xFF9FD8FF)
        Comet.EMBER_CORE -> Color(0xFFFF7A3D)
    }
