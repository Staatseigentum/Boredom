package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * How deep in the interface an element sits, which decides when and how hard it is pulled.
 *
 * The interface is nested — lines sit inside cards, cards sit inside panels — and everything
 * falling at one rate would look like a photograph being scaled down. Three rates make it read as
 * a collapse working its way outwards: the lines implode into their card, the cards into their
 * panel, and the panel itself goes into the hole last.
 *
 * The three levels are not just taste. A card lives inside a scrolling list, and a scrolling list
 * clips whatever leaves it — so a card that flew the whole way to the body would be cut off at
 * the edge of the shop. Cards therefore mostly implode where they stand and break into blocks,
 * which are drawn on the canvas over everything and are clipped by nothing; the panel around them,
 * which nothing clips, is what actually makes the journey.
 */
enum class SogDepth(
    /** How much of the distance to the centre is closed at full strength. */
    val pull: Float,
    /** Turns of spiral on the way in. */
    val spin: Float,
    /** How much of the size is given up. */
    val shrink: Float,
    /** Held back this long before it starts moving at all. */
    val delayMillis: Float,
    /** Share of grid points that become a block. */
    val debrisChance: Float,
) {
    /** A line of text, a bar, a button. Goes first, and goes nowhere: into its own card. */
    CONTENT(pull = 0.62f, spin = 0.6f, shrink = 0.75f, delayMillis = 0f, debrisChance = 0.55f),

    /**
     * A card, a tab strip, a row of chips.
     *
     * Pulled only halfway, because most of these sit in a list that clips them. What sells it is
     * the shrink and the debris, not the travel.
     */
    CONTAINER(pull = 0.55f, spin = 1.1f, shrink = 0.9f, delayMillis = 340f, debrisChance = 0.5f),

    /** The header, the whole shop panel. Nothing clips these, so they go all the way in. */
    SHELL(pull = 1.0f, spin = 1.0f, shrink = 0.96f, delayMillis = 1_000f, debrisChance = 0.3f),
}

/** Where the sequence is on its one clock. */
enum class CollapsePhase { SOG, CRUSH, BLAST, RETURN }

/*
 * The clock, in milliseconds.
 *
 * The pull runs the full length of the rumble that plays under it — [Chiptune.COLLAPSE_RUMBLE_SECONDS] —
 * because the sound stopping is what announces that the screen is about to be empty. Change one
 * without the other and the rumble either dies in the middle of the fall or plays over the bang.
 */
private const val SOG_END = 2_600f
private const val CRUSH_END = 3_000f
private const val RETURN_START = 3_760f
private const val TOTAL = 6_000f

/**
 * How long one element takes to fall, once its own delay has run out.
 *
 * Longer than the delays it stacks on top of, so the three levels overlap rather than taking
 * turns: while the panel is still gathering itself the cards inside it are already gone.
 */
private const val FALL_MILLIS = 1_150f

/** And to fly back. */
private const val RETURN_MILLIS = 620f

/** Positions are snapped to this many pixels, so nothing lands between blocks. */
private const val SNAP = 3f

/**
 * How long one turn of a body takes, near enough, in milliseconds.
 *
 * The spin-up is expressed as a multiple of the body's own rate, and only [CelestialBody] knows
 * what that rate actually is for the body on screen. Rather than thread the real period down here
 * for the sake of an effect that lasts a second and a half, the extra turns are counted against a
 * typical one — the disc has to look like it is running away with itself, not keep a schedule.
 */
private const val SPIN_REFERENCE = 4_000f

/**
 * The whole collapse, on one clock.
 *
 * Everything that moves — every element of the interface, the sky, the body, the debris — reads
 * its position from [elapsed]. One clock rather than an animation per part is what keeps them
 * agreeing: a card and the blocks it broke into cannot drift apart if both are a function of the
 * same number.
 *
 * It is presentation and nothing else. The collapse has already happened in the rules by the time
 * this starts; closing the app halfway through loses the animation and nothing else.
 */
class CollapseSequence {

    /** Milliseconds since the collapse. Null when nothing is running. */
    var elapsed by mutableStateOf<Float?>(null)
        private set

    /** Middle of the tap area — everything falls towards this, not towards the middle of the screen. */
    var centre by mutableStateOf(Offset.Zero)

    /** Extra turns of the body, accumulated during the pull so the disc visibly spins up. */
    var extraSpin by mutableFloatStateOf(0f)
        private set

    private val nodes = mutableListOf<SogNode>()
    private val debris = mutableListOf<Block>()

    /** How many blocks of the element being shattered have been handed over, for the ripple. */
    private var staggered = 0
    private val random = Random(0x5A17)

    val running: Boolean get() = elapsed != null

    val phase: CollapsePhase?
        get() = elapsed?.let {
            when {
                it < SOG_END -> CollapsePhase.SOG
                it < CRUSH_END -> CollapsePhase.CRUSH
                it < RETURN_START -> CollapsePhase.BLAST
                else -> CollapsePhase.RETURN
            }
        }

    /** Progress through the pull, `0f..1f`. Zero outside it. */
    private val pullProgress: Float
        get() {
            val now = elapsed ?: return 0f
            return (now / SOG_END).coerceIn(0f, 1f)
        }

    /** How far into the crush, `0f..1f`. */
    private val crushProgress: Float
        get() {
            val now = elapsed ?: return 0f
            if (now < SOG_END) return 0f
            return ((now - SOG_END) / (CRUSH_END - SOG_END)).coerceIn(0f, 1f)
        }

    /** What the sky is doing: wound up through the pull, unwound over the first part of the blast. */
    val warp: Float
        get() {
            val now = elapsed ?: return 0f
            if (now <= SOG_END) {
                val p = pullProgress
                return p * p
            }
            val back = ((now - CRUSH_END) / 900f).coerceIn(0f, 1f)
            return 1f - back
        }

    /**
     * How large the body is drawn: swelling as it eats the screen, then crushed to nothing.
     *
     * Back to one for the whole of the blast and afterwards — what comes back is the meteorite of
     * the new run at its ordinary size, fading in rather than growing.
     */
    val bodyScale: Float
        get() {
            val now = elapsed ?: return 1f
            if (now <= SOG_END) return 1f + pullProgress * 0.30f
            if (now < CRUSH_END) {
                val k = crushProgress
                return 1.30f * (1f - k) * (1f - k)
            }
            return 1f
        }

    /**
     * How visible the body is: solid through the pull, gone by the end of the crush, and then
     * faded back in over half a second once the return has begun.
     */
    val bodyAlpha: Float
        get() {
            val now = elapsed ?: return 1f
            if (now <= SOG_END) return 1f
            if (now < CRUSH_END) return 1f - crushProgress
            return ((now - RETURN_START) / 500f).coerceIn(0f, 1f)
        }

    /** Whether the blast should be on screen. Hung on the same clock as everything else. */
    val blastDue: Boolean
        get() {
            val now = elapsed ?: return false
            return now >= CRUSH_END
        }

    /**
     * Whether the interface should be showing the state from *before* the collapse.
     *
     * True for exactly as long as the old screen is still being pulled in. After that the new run
     * is what flies back out, which is the point of the whole thing.
     */
    val showingOld: Boolean
        get() {
            val now = elapsed ?: return false
            return now < CRUSH_END
        }

    internal fun register(node: SogNode) {
        nodes += node
    }

    internal fun unregister(node: SogNode) {
        nodes -= node
    }

    /**
     * Runs the clock, calling [onBlast] once at the moment the screen is empty.
     *
     * Cancelling at any point simply stops the picture — nothing here owns any part of the game,
     * so a screen that goes away halfway through leaves a correctly collapsed save behind.
     */
    suspend fun run(onBlast: () -> Unit) {
        elapsed = 0f
        extraSpin = 0f
        debris.clear()
        staggered = 0
        nodes.forEach { it.reset() }

        var blasted = false
        var previous = 0L
        try {
            while ((elapsed ?: 0f) < TOTAL) {
                withFrameNanos { now ->
                    val delta = if (previous == 0L) 0f else (now - previous) / 1_000_000f
                    previous = now
                    elapsed = (elapsed ?: 0f) + delta
                    advance(delta)
                }
                // Fired from the timeline rather than from the collapse itself: the whole point is
                // that it goes off over an empty screen, a second and three quarters later.
                if (!blasted && blastDue) {
                    blasted = true
                    onBlast()
                }
            }
        } finally {
            // Also on the way out of a cancellation, so a screen that leaves mid-fall does not
            // come back with half its interface parked somewhere near the middle.
            elapsed = null
            debris.clear()
        staggered = 0
            nodes.forEach { it.reset() }
            if (!blasted) onBlast()
        }
    }

    private fun advance(deltaMillis: Float) {
        // The body's own rotation is left alone; this is the extra piled on top, so the accretion
        // disc runs up to thirty-five times its usual speed and comes back down without a jump.
        // Accumulated rather than set, because a rate that changes cannot be turned into a phase
        // any other way — and a phase that jumped would show as the disc skipping a beat.
        if (phase == CollapsePhase.SOG) {
            val p = pullProgress
            extraSpin += deltaMillis / SPIN_REFERENCE * (p * p * 34f)
        }

        // Each element breaks apart once, on its own way past the point of no return, so the blocks
        // appear where the element still was rather than all at one moment.
        // Over a copy: the list is added to and taken from as the interface changes around the
        // sequence, and a frame callback is a bad place to find that out.
        for (node in nodes.toList()) {
            // Reset per element: a panel ripples apart over its own blocks, not behind every block
            // that has ever come off anything.
            staggered = 0
            for (spec in node.shatter(this)) {
                debris += blockOf(spec)
                staggered++
            }
        }

        val step = deltaMillis / 16.6f
        val iterator = debris.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()
            if (block.waiting > 0f) {
                block.waiting -= deltaMillis
                continue
            }
            block.advance(step)
            if (block.radius <= 4f) iterator.remove()
        }
    }

    /**
     * Turns a block from where the element saw it into where the sequence draws it.
     *
     * Elements measure themselves in window coordinates; debris moves on a spiral around the body,
     * so it is kept in polar coordinates from there on. Converting once at birth rather than every
     * frame is also what lets the block forget which element it came from.
     */
    private fun blockOf(spec: BlockSpec): Block {
        val offset = Offset(spec.x - centre.x, spec.y - centre.y)
        val radius = hypot(offset.x, offset.y)
        return Block(
            radius = radius,
            angle = atan2(offset.y, offset.x),
            side = spec.side,
            fall = spec.fall,
            spin = spec.spin,
            color = spec.color,
            birth = radius.coerceAtLeast(1f),
            waiting = staggered * DEBRIS_STAGGER_MILLIS,
        )
    }

    internal fun nextRandom(): Float = random.nextFloat()

    /**
     * Draws whatever is still falling, on the one canvas that lies over everything.
     *
     * Each block is snapped to its own edge length rather than to a shared grid — the same rule
     * the supernova draws by, and the reason a field of three- and six-pixel blocks still reads as
     * one picture instead of two.
     */
    fun drawDebris(scope: DrawScope) {
        // Whatever is still in the air when the body is crushed goes with it. The timeline says
        // the screen is empty before the bang, and that has to be true rather than nearly true —
        // a couple of stray blocks over an empty screen read as a bug, not as debris.
        val fade = 1f - crushProgress
        if (fade <= 0f) return

        for (block in debris) {
            if (block.waiting > 0f) continue
            // Sixteen stops on the way in rather than a slide.
            //
            // The position was already snapped to the grid — that part of the picture was right
            // long before this pass. What was not is *when* it moves: a block that changes place
            // every frame travels smoothly however cleanly each frame is drawn, and next to a body
            // that turns in forty-eight discrete steps it is the one thing sliding. Quantising the
            // radius makes the fall a sequence of positions, which is what the rest of the screen
            // is doing.
            val stride = (block.birth / DEBRIS_STEPS).coerceAtLeast(1f)
            val radius = floor(block.radius / stride) * stride
            val x = centre.x + cos(block.angle) * radius
            val y = centre.y + sin(block.angle) * radius
            val side = block.side
            scope.drawRect(
                color = block.color.copy(alpha = block.alpha * fade),
                topLeft = Offset(floor(x / side) * side, floor(y / side) * side),
                size = Size(side, side),
            )
        }
    }
}

/** One piece of a shattered element, on its way down the drain. */
private class Block(
    var radius: Float,
    var angle: Float,
    val side: Float,
    val fall: Float,
    val spin: Float,
    val color: Color,
    /** Where it started, so its sixteen steps are steps of its own journey and not of a fixed one. */
    val birth: Float,
    /**
     * Milliseconds still to wait before it moves at all.
     *
     * A shattered element used to hand over its blocks all at once, so a panel came apart as a
     * single sheet drifting inwards. Sixty milliseconds apart they come off in a ripple, which is
     * what breaking looks like.
     */
    var waiting: Float,
) {
    val alpha: Float get() = min(1f, radius / 90f)

    fun advance(step: Float) {
        /*
         * Three terms, and the first one is not decoration.
         *
         * A rate that depends only on how close a block is leaves the ones that came off the far
         * edge of the screen crawling: eight hundred pixels at a couple of pixels a frame is
         * eleven seconds, and the sequence is over in three. So the distance itself drives the
         * first term — far blocks come streaming in — while the last term keeps the original
         * idea, that the final stretch into the horizon is the fastest part of the trip.
         */
        radius -= fall * (radius * 0.03f + 3.0f + (300f - min(300f, radius)) / 18f) * step
        angle += spin * 0.035f * step
    }
}

/**
 * One element taking part, and the geometry it had when the sequence began.
 *
 * Measured once at the start rather than every frame, and for a reason: the element is being moved
 * by this very animation, so measuring it again would feed its own displacement back in and it
 * would chase itself into the centre far too fast.
 */
internal class SogNode(private val depth: SogDepth, private val accent: Color) {

    /** Kept up to date while nothing is running, so the start geometry is there the moment it is. */
    var bounds: Bounds? = null

    private var shattered = false

    fun reset() {
        shattered = false
    }

    fun measured(position: Offset, size: Size) {
        bounds = Bounds(position, size)
    }

    /**
     * How far this element's own fall has got, `0f..1f`.
     *
     * The geometry is passed in rather than read from [bounds], because an element that has not
     * been measured still has to take part — and taking part as an unmoved, fully opaque thing in
     * the middle of the blast is exactly the failure this avoids.
     */
    private fun progress(sequence: CollapseSequence, here: Bounds): Float {
        val now = sequence.elapsed ?: return 0f
        val distance = (here.centre - sequence.centre).getDistance()
        // Nearest falls first: the pull reaches what is close to it before what is out at the edge,
        // which is what makes it read as one thing collapsing rather than a screen fading out.
        val delay = depth.delayMillis + distance * 0.55f
        return ((now - delay) / FALL_MILLIS).coerceIn(0f, 1f)
    }

    fun applyTo(scope: GraphicsLayerScope, sequence: CollapseSequence) {
        val now = sequence.elapsed
        if (now == null) {
            scope.translationX = 0f
            scope.translationY = 0f
            scope.scaleX = 1f
            scope.scaleY = 1f
            scope.alpha = 1f
            return
        }

        // An element that appeared only once the new run was on screen has nothing measured yet.
        // Treating it as a speck at the centre is what the rest of the timeline expects of it: it
        // stays invisible through the blast and flies out with everything else, rather than
        // popping in at full strength over an empty screen.
        val here = bounds ?: Bounds(sequence.centre, Size.Zero)

        if (sequence.phase == CollapsePhase.RETURN) {
            applyReturn(scope, sequence, here, now)
            return
        }

        val k = progress(sequence, here)
        // Cubed rather than linear: gravity does not pull at a constant rate, and a constant rate
        // reads as a slide rather than a fall.
        val e = k * k * k

        val offset = here.centre - sequence.centre
        val distance = offset.getDistance()
        val start = atan2(offset.y, offset.x)

        val radius = distance * (1f - e * depth.pull)
        val angle = start + e * 1.7f * depth.spin
        val target = sequence.centre + Offset(cos(angle) * radius, sin(angle) * radius)
        val shift = target - here.centre

        scope.translationX = snap(shift.x)
        scope.translationY = snap(shift.y)
        val scale = kotlin.math.max(0.04f, 1f - e * depth.shrink)
        scope.scaleX = scale
        scope.scaleY = scale
        scope.alpha = if (k > 0.72f) (1f - (k - 0.72f) / 0.28f).coerceIn(0f, 1f) else 1f
    }

    private fun applyReturn(
        scope: GraphicsLayerScope,
        sequence: CollapseSequence,
        here: Bounds,
        now: Float,
    ) {
        val offset = here.centre - sequence.centre
        val distance = offset.getDistance()
        val delay = distance * 0.5f
        val k = ((now - RETURN_START - delay) / RETURN_MILLIS).coerceIn(0f, 1f)

        if (k >= 1f) {
            // Left exactly as found, so nothing afterwards is still hanging off the animation.
            scope.translationX = 0f
            scope.translationY = 0f
            scope.scaleX = 1f
            scope.scaleY = 1f
            scope.alpha = 1f
            return
        }

        // Eased out rather than in: coming back should settle, not accelerate into place.
        val e = 1f - (1f - k) * (1f - k) * (1f - k)
        val angle = atan2(offset.y, offset.x) + (1f - e) * 1.1f
        val radius = distance * e
        val target = sequence.centre + Offset(cos(angle) * radius, sin(angle) * radius)
        val shift = target - here.centre

        scope.translationX = snap(shift.x)
        scope.translationY = snap(shift.y)
        scope.scaleX = 1f
        scope.scaleY = 1f
        scope.alpha = k
    }

    /** The grid of blocks this element breaks into. Empty until it is far enough in. */
    fun shatter(sequence: CollapseSequence): List<BlockSpec> {
        if (shattered) return emptyList()
        val here = bounds ?: return emptyList()
        if (progress(sequence, here) <= 0.42f) return emptyList()
        shattered = true

        val out = mutableListOf<BlockSpec>()
        val columns = min(26, (here.size.width / 6f).toInt().coerceAtLeast(1))
        val rows = min(10, (here.size.height / 6f).toInt().coerceAtLeast(1))

        for (column in 0 until columns) {
            for (row in 0 until rows) {
                // Not every grid point: a complete grid looks like a picture cut into squares,
                // which is the opposite of something falling apart.
                if (sequence.nextRandom() > depth.debrisChance) continue
                val x = here.position.x + here.size.width * (column + 0.5f) / columns
                val y = here.position.y + here.size.height * (row + 0.5f) / rows
                out += BlockSpec(
                    x = x,
                    y = y,
                    side = if (sequence.nextRandom() < 0.5f) 3f else 6f,
                    fall = 0.55f + sequence.nextRandom() * 0.7f,
                    spin = 1.4f + sequence.nextRandom() * 1.6f,
                    color = accent,
                )
            }
        }
        return out
    }
}

/** A block as the element describes it, before it is placed in the sequence's own coordinates. */
internal class BlockSpec(
    val x: Float,
    val y: Float,
    val side: Float,
    val fall: Float,
    val spin: Float,
    val color: Color,
)

/** Position and size in window coordinates. */
internal class Bounds(val position: Offset, val size: Size) {
    val centre: Offset get() = Offset(position.x + size.width / 2f, position.y + size.height / 2f)
}

/** Stops a block makes on its way in. See [CollapseSequence.drawDebris]. */
private const val DEBRIS_STEPS = 16

/** And how far apart the blocks of one element come off. */
private const val DEBRIS_STAGGER_MILLIS = 60f

private fun snap(value: Float): Float = (value / SNAP).roundToInt() * SNAP

/** Reaches every element without a parameter on half the screen. Null when nothing is running. */
val LocalCollapse = staticCompositionLocalOf<CollapseSequence?> { null }

/**
 * Whether the player has asked the system for less movement.
 *
 * A seam rather than a lookup, because how that is asked differs per platform and none of the ways
 * of asking can appear in here — this file is compiled for the desktop build too. False by default,
 * so a platform that has no such setting simply plays the whole thing.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * Marks an element as something the hole can take.
 *
 * The geometry is collected here rather than being declared, so nothing has to be kept in step by
 * hand: an element that moves, or one added later, brings its own position along.
 */
fun Modifier.sog(depth: SogDepth, accent: Color): Modifier = composed {
    val sequence = LocalCollapse.current ?: return@composed this
    val node = remember(depth, accent) { SogNode(depth, accent) }

    DisposableEffect(sequence, node) {
        sequence.register(node)
        onDispose { sequence.unregister(node) }
    }

    this
        .onGloballyPositioned { coordinates ->
            // Only while nothing is running: once the pull starts, this element is being moved by
            // it, and re-measuring would feed its own displacement back into the sum.
            if (!sequence.running || node.bounds == null) {
                node.measured(
                    coordinates.positionInWindow(),
                    Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()),
                )
            }
        }
        .graphicsLayer { node.applyTo(this, sequence) }
}

/** Marks the point everything falls towards: the middle of the body. */
fun Modifier.collapseCentre(): Modifier = composed {
    val sequence = LocalCollapse.current ?: return@composed this
    onGloballyPositioned { coordinates ->
        if (sequence.running) return@onGloballyPositioned
        val position = coordinates.positionInWindow()
        sequence.centre = Offset(
            position.x + coordinates.size.width / 2f,
            position.y + coordinates.size.height / 2f,
        )
    }
}

/** The falling blocks, drawn over everything else. */
@Composable
fun CollapseDebris(sequence: CollapseSequence, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Read so the canvas redraws every frame the clock moves.
        sequence.elapsed ?: return@Canvas
        sequence.drawDebris(this)
    }
}

/** Wraps the screen so [sog] can find the sequence. */
@Composable
fun WithCollapse(sequence: CollapseSequence?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCollapse provides sequence, content = content)
}
