package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
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
    // Its own stream, seeded from the shared one so that two runs of the game do not see the same
    // comets in the same order. Seeded from a number rather than from the clock: `System.nanoTime`
    // is a JVM word, and this file is compiled for three platforms now.
    val random = remember { Random(Random.nextLong()) }
    var flight by remember { mutableStateOf<Flight?>(null) }
    val travel = remember { Animatable(0f) }
    val appears = Comets.appearsAt(state)
    // Read fresh rather than captured: the loop below outlives many recompositions, and a
    // captured sound object goes on making noise long after the setting was switched off.
    val sfx by rememberUpdatedState(LocalSfx.current)

    /*
     * The rate is read, not keyed on.
     *
     * It used to be part of the key, and a key is a thing that tears the loop down and starts it
     * over. The rate is a product of five prestige multipliers and moves whenever any of them
     * does, so on a built-up save every purchase threw away a comet that was halfway across the
     * screen and began the wait again. Read through [rememberUpdatedState] the schedule simply
     * carries on and the next wait uses the new rate, which is what "comets come more often" was
     * always supposed to mean.
     */
    val rate by rememberUpdatedState(frequency)

    LaunchedEffect(appears) {
        if (!appears) return@LaunchedEffect
        while (true) {
            waitSeconds(Comets.nextDelay(random, rate))
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
                // Silent once the sky is busy. The sound means "that was a quarter of an hour of
                // production and you were looking elsewhere", which is worth saying twice an hour
                // and is nagging two hundred times an hour — and a player with the frequency
                // fully built is not meant to be catching all of them. See [Comets.isBusySky].
                if (!Comets.isBusySky(rate)) sfx?.missed()
                flight = null
            }
        }
    }

    val current = flight ?: return
    val progress = travel.value

    // Reset for every flight, so a core that got away half broken arrives whole next time.
    var struck by remember(current) { mutableIntStateOf(0) }

    /*
     * Chips knocked off the crust.
     *
     * A hard core used to be hit and simply carry on with a slightly smaller square. The hit
     * counter was in the code and nowhere on the screen, so a player who struck one and did not
     * catch it had no way to tell they had done anything at all.
     *
     * Keyed on the strike count: every hit restarts it, and it runs to nothing on its own.
     */
    val quietMotion = LocalReduceMotion.current
    val chips = remember(current) { Animatable(1f) }
    LaunchedEffect(struck) {
        if (struck == 0) return@LaunchedEffect
        chips.snapTo(0f)
        chips.animateTo(1f, tween(CHIP_MILLIS, easing = LinearEasing))
    }

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

            val accent = current.comet.accent
            val backwards = if (current.leftToRight) -1f else 1f

            /*
             * Two tail blocks rather than nine.
             *
             * Nine fading squares are a gradient built out of rectangles — the shape it makes is a
             * smooth trail, and reading it as one is the point at which the effect stops belonging
             * to this art. Two hard slabs at fixed opacities, the near one short and thick and the
             * far one long and thin, say "moving fast" in the language of the sprites: a shape, not
             * a fade.
             */
            for ((length, thickness, alpha) in TAIL) {
                val far = block * (length + 2f)
                drawRect(
                    color = accent.copy(alpha = alpha),
                    topLeft = Offset(
                        snap(head.x + backwards * far, block),
                        snap(head.y - far * 0.22f - block * thickness / 2f, block),
                    ),
                    size = Size(block * length, block * thickness),
                )
            }

            // The head: a light core with a ring in the colour of what it is carrying. Three
            // comets that all looked the same was three the player could not tell apart until
            // after catching one — and the ring is also the crust, one step thinner per hit, so a
            // strike that did not catch it is still visibly a strike.
            val crust = (current.comet.hits - struck).coerceAtLeast(0)
            val ring = block * (1f + crust)
            drawRect(
                color = accent,
                topLeft = Offset(snap(head.x - ring, block), snap(head.y - ring, block)),
                size = Size(ring * 2f, ring * 2f),
            )
            val core = block * 2f
            drawRect(
                color = Starlight,
                topLeft = Offset(snap(head.x - core / 2f, block), snap(head.y - core / 2f, block)),
                size = Size(core, core),
            )

            // And the pieces that came off, if one just did. Stepped like everything else in this
            // pass, so they travel in visible jumps rather than sliding.
            //
            // Skipped under reduced motion — but the ring above still loses a step per hit, so a
            // strike that did not catch the comet is still visible. That was the whole point of
            // adding these, and it must survive the setting that removes them.
            val life = if (quietMotion) 1f else chips.value
            if (life < 1f) {
                val flung = quantise(life, CHIP_STEPS)
                for (index in 0 until CHIP_COUNT) {
                    val angle = index.toFloat() / CHIP_COUNT * TWO_PI
                    val distance = block * 2f + flung * block * 7f
                    drawRect(
                        color = accent.copy(alpha = 1f - flung),
                        topLeft = Offset(
                            snap(head.x + cos(angle) * distance, block),
                            snap(head.y + sin(angle) * distance, block),
                        ),
                        size = Size(block * 1.4f, block * 1.4f),
                    )
                }
            }
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

/**
 * The two slabs behind the head: how long, how thick, how visible — all in blocks.
 *
 * Near one first, so the far one is drawn under it where they overlap.
 */
private val TAIL = listOf(
    Triple(4f, 1.6f, 0.60f),
    Triple(8f, 1f, 0.35f),
)

/** How many pieces come off a crust, and over how long, in how many steps. */
private const val CHIP_COUNT = 6
private const val CHIP_MILLIS = 260
private const val CHIP_STEPS = 8
private const val TWO_PI = 6.2831855f

/** Colour is not enough to tell the three comets apart, so the catch says which it was. */
val Comet.accent: Color
    get() = when (this) {
        Comet.WINDFALL -> Ember
        Comet.SURGE -> Color(0xFF5CE1A6)
        Comet.FRENZY -> Color(0xFF7C5CFF)
        Comet.ICE_CORE -> Color(0xFF9FD8FF)
        Comet.EMBER_CORE -> Color(0xFFFF7A3D)
    }
