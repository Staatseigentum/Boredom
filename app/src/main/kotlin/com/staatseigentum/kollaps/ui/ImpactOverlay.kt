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
import com.staatseigentum.kollaps.core.Accretion
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Impact
import com.staatseigentum.kollaps.core.Material
import com.staatseigentum.kollaps.core.Shell
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** One fragment on its way in, and the line it is falling along. */
private class Fall(val impact: Impact, val angle: Float, val spin: Float)

/**
 * How long until the next fragment — held *outside* the screen that draws it.
 *
 * This exists because of where the phone puts the body. The six phone screens are a `when`, so
 * leaving the body throws the whole subtree away, [ImpactOverlay] with it, and coming back builds
 * a fresh one whose wait starts at zero again. Past Mars that wait is some twenty seconds, and the
 * building panel is a *different screen* — so the ordinary loop of the feature it belongs to
 * (look at the shells, go back to the body, wait) reset the clock every single time. Fragments
 * were not rare on a phone, they were unreachable.
 *
 * Remembered above the `when` in [GameScreen] and handed down, so the wait carries on across a
 * screen switch instead of starting over. Exactly the shape of the achievement card in 5.0.1, and
 * the wide layout never showed either fault, because there the body is always up.
 *
 * The wait *pauses* while the body is off screen rather than running on. That is deliberate: a
 * fragment that fell into an unwatched screen would have to be counted as missed, and a missed
 * heavy one costs mass — taken behind the player's back, for a thing they were never shown.
 */
class ImpactSchedule {
    /** Seconds still to wait. Negative means "roll a fresh one". Not Compose state: only the
     *  scheduling coroutine ever reads it, and a per-frame write would recompose the screen. */
    internal var dueIn: Double = -1.0
}

/**
 * The things that hit you, and the first hour finally having something in it.
 *
 * The comets and this look alike and are opposites. A comet *crosses* the sky every few minutes,
 * pays a windfall measured in quarter hours, and is a thing to chase. A fragment falls *towards*
 * the body every few seconds, pays a handful of seconds, and leaves behind the one thing that
 * cannot be bought: material. Catching every one of them is not the point — the point is that
 * something is happening at all while the first four collectors are being saved up for.
 *
 * Owns its own clock, exactly like the comet overlay and for the same reason: it is about the
 * player being present, so it should stop dead when the screen goes away, which a frame callback
 * already does for free.
 */
@Composable
fun ImpactOverlay(
    state: GameState,
    /** The wait, kept alive across the phone's six screens. See [ImpactSchedule]. */
    schedule: ImpactSchedule,
    onAbsorb: (Impact) -> Unit,
    onMiss: (Impact) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Its own stream, seeded from the shared one — two runs must not see the same sky in the same
    // order. Seeded from a number and not from the clock, because this file is compiled for three
    // platforms and `System.nanoTime` is a JVM word.
    val random = remember { Random(Random.nextLong()) }
    var falling by remember { mutableStateOf<Fall?>(null) }
    val travel = remember { Animatable(0f) }
    val active = Accretion.isActive(state)
    val sfx by rememberUpdatedState(LocalSfx.current)

    // Read fresh on every arrival rather than captured into the loop: the interval shortens as the
    // body grows and as the mantle thickens, and a captured one would keep the schedule of
    // whatever the body was when the screen was first composed.
    val current by rememberUpdatedState(state)

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (true) {
            // Jittered around the interval rather than exactly on it. A metronome is a thing to
            // tune out; something that arrives roughly every nine seconds is a thing to watch for.
            //
            // Rolled only when the schedule has none left over, so returning to the body screen
            // resumes the wait it was in the middle of instead of beginning a new one.
            if (schedule.dueIn < 0.0) {
                schedule.dueIn = Accretion.interval(current) * (0.7 + random.nextDouble() * 0.6)
            }
            countDown(schedule)

            val landing = Fall(
                impact = Accretion.pick(random),
                angle = random.nextFloat() * TWO_PI,
                spin = if (random.nextBoolean()) 1f else -1f,
            )
            falling = landing
            travel.snapTo(0f)
            travel.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = (Accretion.APPROACH_SECONDS * 1000).toInt(),
                    easing = LinearEasing,
                ),
            )
            // Still there when it arrives means nobody took it: it strikes at an angle and most of
            // it is thrown straight back out. Absorbing clears the field, which is the only way a
            // catch can be told from a miss — the fall runs to the end either way.
            if (falling === landing) {
                onMiss(landing.impact)
                if (landing.impact.heavy) sfx?.missed()
                falling = null
            }
            // Spent: the next turn of the loop rolls a fresh wait. Written here and not before the
            // fall, so a screen switch *during* a fall leaves a zero behind and the fragment that
            // was interrupted arrives as soon as the body is up again.
            schedule.dueIn = -1.0
        }
    }

    val fall = falling ?: return
    val progress = travel.value
    val quietMotion = LocalReduceMotion.current

    Box(modifier = modifier.fillMaxSize()) {
        /*
         * The catcher follows the head rather than covering the screen.
         *
         * Learned from the comet, which used to swallow every tap that missed it — for the eleven
         * seconds one was crossing, tapping the body quietly did nothing. Here it would be far
         * worse: something is falling most of the time.
         */
        val reach = 40.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    val side = (reach * 2).roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val head = headOf(
                            fall,
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
                .pointerInput(fall) {
                    detectTapGestures {
                        sfx?.comet()
                        onAbsorb(fall.impact)
                        falling = null
                    }
                },
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val head = headOf(fall, progress, size.width, size.height)
            val block = floor(3.dp.toPx()).coerceAtLeast(2f)
            val accent = fall.impact.material.accent

            // The trail points back the way it came, which is outward — so it is drawn from the
            // head along the reversed approach rather than at a fixed angle like the comet's.
            val back = Offset(cos(fall.angle), sin(fall.angle))
            for ((distance, thickness, alpha) in TRAIL) {
                val out = block * distance
                drawRect(
                    color = accent.copy(alpha = alpha),
                    topLeft = Offset(
                        snap(head.x + back.x * out - block * thickness / 2f, block),
                        snap(head.y + back.y * out - block * thickness / 2f, block),
                    ),
                    size = Size(block * thickness, block * thickness),
                )
            }

            /*
             * The fragment itself: a lump of blocks in the colour of what it is made of, with a
             * lighter face on one side so it reads as a solid thing rather than a coloured square.
             * Heavy ones are a step bigger and carry a bright core, because they are the two the
             * player is meant to want and the only two that cost anything to ignore.
             */
            val side = block * (if (fall.impact.heavy) 4f else 3f)
            val turn = if (quietMotion) 0f else quantise(progress, SPIN_STEPS) * fall.spin * TWO_PI
            val lean = Offset(cos(turn), sin(turn)) * (block * 0.5f)

            drawRect(
                color = accent,
                topLeft = Offset(snap(head.x - side / 2f, block), snap(head.y - side / 2f, block)),
                size = Size(side, side),
            )
            drawRect(
                color = accent.copy(alpha = 0.45f),
                topLeft = Offset(
                    snap(head.x - side / 2f + lean.x, block),
                    snap(head.y - side / 2f + lean.y, block),
                ),
                size = Size(block, block),
            )
            if (fall.impact.heavy) {
                drawRect(
                    color = Starlight,
                    topLeft = Offset(snap(head.x - block / 2f, block), snap(head.y - block / 2f, block)),
                    size = Size(block, block),
                )
            }
        }
    }
}

/**
 * Where the fragment is right now.
 *
 * It falls along a straight line from off the edge to the middle, because the middle is where the
 * body is drawn — this overlay is laid over the tap area and nothing else, so "the centre" needs no
 * measuring. The distance is taken from the diagonal so a fragment coming in at a corner starts as
 * far outside as one coming in from the side.
 */
private fun headOf(fall: Fall, progress: Float, width: Float, height: Float): Offset {
    val centre = Offset(width / 2f, height / 2f)
    val reach = maxOf(width, height) * START_REACH
    val from = centre + Offset(cos(fall.angle), sin(fall.angle)) * reach
    return from + (centre - from) * progress
}

/**
 * How far outside the middle a fragment starts, as a share of the longer side.
 *
 * It was 0.75, and three quarters of the *longer* side from the centre is well outside the area on
 * every phone: measured on a 360×430 field, a fragment coming in from the side spent the first
 * forty-four per cent of its fall off screen, and one coming in diagonally a fifth. The fall was
 * nominally four seconds and visibly two and a half.
 *
 * At 0.55 it starts just past the edge — still off screen, so it drifts in rather than appearing,
 * and nearly all of the fall is now something the player can actually see and hit.
 */
private const val START_REACH = 0.55f

private fun snap(value: Float, block: Float): Float = floor(value / block) * block

/** Runs [schedule] down to zero, a frame at a time. Stops dead when the screen goes away. */
private suspend fun countDown(schedule: ImpactSchedule) {
    var previous = 0L
    while (schedule.dueIn > 0.0) {
        withFrameNanos { now ->
            if (previous != 0L) schedule.dueIn -= (now - previous) / 1_000_000_000.0
            previous = now
        }
    }
}

/** How far behind the head, how big, and how visible — all in blocks. Nearest first. */
private val TRAIL = listOf(
    Triple(3f, 1.6f, 0.55f),
    Triple(6f, 1f, 0.3f),
    Triple(9f, 1f, 0.15f),
)

/** How many steps a full turn is cut into, so the tumble reads as sprite frames and not a spin. */
private const val SPIN_STEPS = 8
private const val TWO_PI = 6.2831855f

/**
 * The colour a material is drawn in, taken straight from the rules so nothing can disagree.
 *
 * The four colours are in `:core` rather than in the theme because they are read by two things that
 * are not the interface: the sprite tint that bends the whole palette towards what the body is made
 * of, and the panel that has to agree with it. One definition, three readers.
 */
val Material.accent: Color get() = Color(colour)

/** A shell wears the colour of what it is mostly made of. */
val Shell.accent: Color get() = wants.accent
