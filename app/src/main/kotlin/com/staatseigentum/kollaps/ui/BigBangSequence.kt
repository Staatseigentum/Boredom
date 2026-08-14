package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/*
 * The big bang, and why none of it looks like the collapse.
 *
 * The two resets sit next to each other in the same tab and had better not be the same animation
 * with different colours. So they are built on opposite ideas, and every value here is chosen to
 * be the opposite of the corresponding one in [CollapseSequence]:
 *
 * - the collapse spirals; this does not rotate at all, ever. Everything moves in a straight line,
 *   square onto the middle. If a rotation ever turns up in here it is a bug.
 * - the collapse pulls the nearest things in first; this starts at the edges and finishes in the
 *   middle, because the middle is the target and the target moves least.
 * - the collapse gets louder until it is crushed; this has four hundred and sixty milliseconds of
 *   total silence in the middle of it, one pixel on a black screen. That pause is the whole point
 *   of the sequence — the bang after it only lands because nothing at all happened first.
 * - the collapse keeps its colours; this washes out to white on the way down, because a screen
 *   that folds up with its colours intact reads as a window closing rather than as a burn-out.
 */

/** Where the sequence is on its one clock. */
enum class BigBangPhase { FLAT, PINCH, SILENCE, BANG, RETURN }

/*
 * The clock, in milliseconds. Taken exactly from the prototype; none of it is rounded off.
 */

/** The interface is pressed down to a line and washed white. */
private const val FLAT_END = 980f

/** The line and the sky, already flat, are squeezed sideways onto a point. */
private const val PINCH_END = 1_320f

/** Then nothing happens for 460 ms, which is the longest anything in this game does nothing. */
private const val BANG_AT = 1_780f

/** How long [BlastKind.URKNALL] runs, which is what the blast's own clock is set to. */
private const val BLAST_MILLIS = 2_200f

/** When the state after the big bang is put on screen and the interface unfolds again. */
private const val RETURN_AT = BANG_AT + 620f

/**
 * The end.
 *
 * Seven hundred and twenty milliseconds after the blast has finished, because the matter thrown
 * out by it is still in the air and cutting the sequence at the blast would delete it mid-flight.
 */
private const val TOTAL = 4_700f

/** How long one element takes to be pressed flat, once its own delay has run out. */
private const val FLAT_FALL = 760f

/** And to unfold again. */
private const val RETURN_MILLIS = 560f

/** Screen positions are snapped to this many pixels. */
private const val SNAP = 3f

/** How far up or down the screen the start delay is spread, in dp. */
private const val DELAY_REACH = 480f

/** How long a body takes for one turn, near enough — see [CollapseSequence]'s note on the same. */
private const val SPIN_REFERENCE = 4_000f

/** How many pieces of matter the bang throws out. */
private const val MATTER = 260

private const val TWO_PI = 6.2831855f

/**
 * The whole big bang, on one clock.
 *
 * Presentation only. The universe has already been thrown away in the rules by the time this
 * starts; closing the app halfway through loses the animation and nothing else.
 */
class BigBangSequence {

    /** Milliseconds since the path was chosen. Null when nothing is running. */
    var elapsed by mutableStateOf<Float?>(null)
        private set

    /**
     * Where everything converges, kept as two numbers because they come from two places.
     *
     * X is the middle of the screen and Y is the middle of the tap area — not the middle of the
     * screen, and not the middle of the sprite. The point the universe is squeezed onto has to be
     * where the body was, or the bang goes off somewhere the player was not looking. Held apart
     * so that whichever of the two is measured first cannot wipe out the other.
     */
    var centreX by mutableFloatStateOf(0f)
    var centreY by mutableFloatStateOf(0f)

    val centre: Offset get() = Offset(centreX, centreY)

    /** How wide the screen is, for the line that shrinks across it. */
    var screen by mutableStateOf(Size.Zero)

    /** Pixels per dp, so the prototype's dp-space measurements land on the right pixels. */
    var density by mutableFloatStateOf(1f)

    /** Extra turns of the body, accumulated as the disc spins up on the way down. */
    var extraSpin by mutableFloatStateOf(0f)
        private set

    /**
     * Bumped when the interface has to be measured again.
     *
     * The return is not the reverse of a journey — it is a *different* screen unfolding, with a
     * different number of rows in it. Its elements are in new places, so the geometry collected
     * before the bang is worthless to them and everything re-measures once.
     */
    var generation by mutableIntStateOf(0)
        private set

    /** How far the screen is shaken, in pixels, already snapped to the grid. */
    var shake by mutableStateOf(Offset.Zero)
        private set

    private val nodes = mutableListOf<FlatNode>()
    private val debris = mutableListOf<Shard>()
    private val matter = mutableListOf<Chunk>()
    private val random = Random(Random.nextLong())

    /** Set once the bang has thrown its matter, so it is thrown exactly once. */
    private var scattered = false

    /** Jitter of the core pixel during the silence: one block or none, redrawn every frame. */
    private var coreJitter by mutableFloatStateOf(0f)

    val running: Boolean get() = elapsed != null

    val phase: BigBangPhase?
        get() = elapsed?.let {
            when {
                it < FLAT_END -> BigBangPhase.FLAT
                it < PINCH_END -> BigBangPhase.PINCH
                it < BANG_AT -> BigBangPhase.SILENCE
                it < RETURN_AT -> BigBangPhase.BANG
                else -> BigBangPhase.RETURN
            }
        }

    /** The blast's own clock: milliseconds since the bang, negative before it. */
    private val sinceBang: Float get() = (elapsed ?: 0f) - BANG_AT

    /** Whether the blast should be on screen. Hung on the same clock as everything else. */
    val blastDue: Boolean
        get() {
            val now = elapsed ?: return false
            return now >= BANG_AT
        }

    /**
     * Whether the interface should still be showing the universe that was thrown away.
     *
     * It is invisible for most of that time — from 980 ms there is nothing on screen but the line
     * — but it still has to be the *old* one, because the geometry the flattening is working from
     * was measured off it.
     */
    val showingOld: Boolean
        get() {
            val now = elapsed ?: return false
            return now < RETURN_AT
        }

    // ------------------------------------------------------------------ the sky

    /** How far the sky has been squashed onto the horizontal line. */
    val skyCrush: Float
        get() {
            val now = elapsed ?: return 0f
            if (now >= BANG_AT) return 0f
            if (now >= FLAT_END) return 1f
            val p = now / FLAT_END
            return p * p
        }

    /** And how far that line has then been squeezed sideways onto the point. */
    val skyPinch: Float
        get() {
            val now = elapsed ?: return 0f
            if (now >= BANG_AT) return 0f
            if (now >= PINCH_END) return 1f
            if (now < FLAT_END) return 0f
            val k = (now - FLAT_END) / (PINCH_END - FLAT_END)
            return k * k
        }

    /**
     * How much of the sky exists at all.
     *
     * Zero through the silence, which is what makes the silence work: squeezing the sky to a point
     * still leaves a bright smear where the point is, and the screen has to be *empty* apart from
     * the one pixel. So for those 460 ms the sky is simply not drawn.
     */
    val skyBirth: Float
        get() {
            val now = elapsed ?: return 1f
            if (now < PINCH_END) return 1f
            if (now < BANG_AT) return 0f
            val k = ((sinceBang - 120f) / 1_500f).coerceIn(0f, 1f)
            val left = 1f - k
            return 1f - left * left * left
        }

    // ------------------------------------------------------------------ the body

    /** How large the body is drawn. */
    val bodyScale: Float
        get() {
            val now = elapsed ?: return 1f
            if (now < FLAT_END) return 1f - (now / FLAT_END) * 0.9f
            if (now < BANG_AT) return 0f
            val k = ((sinceBang - 900f) / 700f).coerceIn(0f, 1f)
            return 0.3f + k * 0.7f
        }

    /** And how visible. Gone entirely through the pinch, the silence and the front of the bang. */
    val bodyAlpha: Float
        get() {
            val now = elapsed ?: return 1f
            if (now < FLAT_END) {
                val p = now / FLAT_END
                return 1f - p * p
            }
            if (now < BANG_AT) return 0f
            return ((sinceBang - 900f) / 700f).coerceIn(0f, 1f)
        }

    // ------------------------------------------------------------------ running it

    internal fun register(node: FlatNode) {
        nodes += node
    }

    internal fun unregister(node: FlatNode) {
        nodes -= node
    }

    internal fun nextRandom(): Float = random.nextFloat()

    /**
     * Runs the clock, calling [onBang] once at the moment the point lets go.
     *
     * Cancelling at any point simply stops the picture — nothing here owns any part of the game,
     * so a screen that goes away halfway through leaves a correctly reset save behind.
     */
    suspend fun run(onBang: () -> Unit) {
        elapsed = 0f
        extraSpin = 0f
        shake = Offset.Zero
        scattered = false
        debris.clear()
        matter.clear()
        nodes.forEach { it.reset() }

        var banged = false
        var returned = false
        var previous = 0L
        try {
            while ((elapsed ?: 0f) < TOTAL) {
                withFrameNanos { now ->
                    val delta = if (previous == 0L) 0f else (now - previous) / 1_000_000f
                    previous = now
                    elapsed = (elapsed ?: 0f) + delta
                    advance(delta)
                }
                if (!banged && blastDue) {
                    banged = true
                    onBang()
                }
                // One bump, at the moment the new universe is put on screen: everything measures
                // itself again, because it is a different screen and not the old one coming back.
                if (!returned && (elapsed ?: 0f) >= RETURN_AT) {
                    returned = true
                    generation++
                }
            }
        } finally {
            elapsed = null
            shake = Offset.Zero
            debris.clear()
            matter.clear()
            nodes.forEach { it.reset() }
            if (!banged) onBang()
        }
    }

    private fun advance(deltaMillis: Float) {
        /*
         * Everything below is per *frame* in the prototype, which is only correct at sixty of them
         * a second. On a 120 Hz phone the same code would run the whole sequence at double speed
         * while the timeline above stayed put, and the matter would be gone before the rings were.
         * So each per-frame step is scaled by how long the frame actually took.
         */
        val step = deltaMillis / 16.667f
        val now = elapsed ?: return

        if (now < FLAT_END) {
            val p = now / FLAT_END
            extraSpin += deltaMillis / SPIN_REFERENCE * (p * 8f)
        }

        // Over a copy: the list is added to and taken from as the interface changes around the
        // sequence, and a frame callback is a bad place to find that out.
        for (node in nodes.toList()) {
            for (spec in node.shatter(this)) debris += Shard(spec)
        }

        val falling = debris.iterator()
        while (falling.hasNext()) {
            val shard = falling.next()
            shard.advance(step)
            if (shard.done) falling.remove()
        }

        // One block of jitter or none, and a fresh throw every frame — this is what turns the
        // single pixel from a dot into something straining to get out.
        coreJitter = if (skyPinch >= 1f && random.nextBoolean()) SNAP else 0f

        if (now >= BANG_AT) {
            val since = sinceBang
            if (!scattered && since < BLAST_MILLIS * 0.08f) {
                scattered = true
                repeat(MATTER) { matter += Chunk(this) }
            }

            val shaking = max(0f, 1f - since / 620f)
            shake = if (shaking <= 0f) {
                Offset.Zero
            } else {
                // Snapped to the block grid: a smooth shake turns every hard edge on screen into
                // a blurred one for half a second, which is the whole picture undone.
                Offset(
                    ((random.nextFloat() - 0.5f) * 10f * shaking / SNAP).roundToInt() * SNAP,
                    ((random.nextFloat() - 0.5f) * 10f * shaking / SNAP).roundToInt() * SNAP,
                )
            }

            val flying = matter.iterator()
            while (flying.hasNext()) {
                val chunk = flying.next()
                chunk.advance(step, centre, screen)
                if (chunk.done) flying.remove()
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    /**
     * Everything the sequence draws itself: the shards, the line, the point, and the matter.
     *
     * One canvas rather than three, because they are layered — matter over the point over the
     * shards — and three canvases would leave the order to whoever edits the screen next.
     */
    fun draw(scope: DrawScope) {
        val now = elapsed ?: return

        for (shard in debris) shard.draw(scope, centre)

        if (now in FLAT_END..BANG_AT) drawPoint(scope, now)

        for (chunk in matter) chunk.draw(scope, centre)
    }

    /**
     * The line, and then the point it becomes.
     *
     * The half-transparent block behind the core is the only soft thing in the sequence and it
     * earns its place: a single six-pixel square on a black screen reads as a dead pixel, and the
     * block behind it is what says the thing is under pressure rather than switched off.
     */
    private fun drawPoint(scope: DrawScope, now: Float) {
        val pinch = skyPinch
        val cx = centre.x
        val half = cx * (1f - pinch)
        val y = floor(centre.y / SNAP) * SNAP

        if (half > SNAP) {
            scope.drawRect(
                color = Color.White,
                topLeft = Offset(floor((cx - half) / SNAP) * SNAP, y),
                size = Size(ceil(half * 2f / SNAP) * SNAP, SNAP),
            )
        }

        val core = if (pinch >= 1f) 6f else 9f
        val jitter = if (pinch >= 1f) coreJitter else 0f
        scope.drawRect(
            color = Color.White,
            topLeft = Offset(cx - core / 2f, y - jitter),
            size = Size(core, core),
        )
        scope.drawRect(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(cx - core, y - core / 2f),
            size = Size(core * 2f, core),
        )
    }
}

// ---------------------------------------------------------------------- the pieces

/** A block broken off an element, on its way straight to the point. */
private class Shard(spec: ShardSpec) {
    private val fromX = spec.x
    private val fromY = spec.y
    private val side = spec.side
    private val speed = spec.speed
    private val color = spec.color
    private var travel = 0f

    val done: Boolean get() = travel >= 1f

    fun advance(step: Float) {
        travel += speed * step
    }

    fun draw(scope: DrawScope, centre: Offset) {
        // Squared, so it accelerates into the point rather than sliding to it — and in a straight
        // line, because nothing in the big bang turns.
        val e = travel * travel
        val x = fromX + (centre.x - fromX) * e
        val y = fromY + (centre.y - fromY) * e
        scope.drawRect(
            color = color.copy(alpha = min(1f, (1f - travel) * 1.6f).coerceAtLeast(0f)),
            topLeft = Offset(floor(x / side) * side, floor(y / side) * side),
            size = Size(side, side),
        )
    }
}

/** A block as the element describes it, before the sequence takes it over. */
internal class ShardSpec(
    val x: Float,
    val y: Float,
    val side: Float,
    val speed: Float,
    val color: Color,
)

/** One piece of matter thrown out by the bang. */
private class Chunk(sequence: BigBangSequence) {
    private val angle = sequence.nextRandom() * TWO_PI
    private var speed = 1.4f + sequence.nextRandom() * 7.5f
    private var radius = 2f + sequence.nextRandom() * 10f
    private val side = if (sequence.nextRandom() < 0.5f) 3f else 6f
    private val drag = 0.972f + sequence.nextRandom() * 0.02f
    private val color = MATTER_COLOURS[(sequence.nextRandom() * MATTER_COLOURS.size).toInt()
        .coerceAtMost(MATTER_COLOURS.lastIndex)]
    private var life = 1f
    private var gone = false

    val done: Boolean get() = gone || life <= 0f

    fun advance(step: Float, centre: Offset, screen: Size) {
        radius += speed * step
        // Raised to the step rather than multiplied by it: drag compounds per frame, and a frame
        // that took twice as long has to lose twice as much speed, not the same amount again.
        speed *= drag.pow(step)
        life -= 0.006f * step

        val x = centre.x + cos(angle) * radius
        val y = centre.y + sin(angle) * radius
        gone = x < -20f || x > screen.width + 20f || y < -20f || y > screen.height + 20f
    }

    fun draw(scope: DrawScope, centre: Offset) {
        val x = centre.x + cos(angle) * radius
        val y = centre.y + sin(angle) * radius
        scope.drawRect(
            color = color.copy(alpha = min(1f, life * 1.4f).coerceAtLeast(0f)),
            topLeft = Offset(floor(x / side) * side, floor(y / side) * side),
            size = Size(side, side),
        )
    }
}

/** White, near-white, two purples and the collapse's amber: the old universe in five colours. */
private val MATTER_COLOURS = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFE9ECFF),
    Color(0xFFB88AFF),
    Color(0xFF7C5CFF),
    Color(0xFFFFB74D),
)

// ---------------------------------------------------------------------- the elements

/** Position and size in window coordinates. */
internal class FlatBounds(val position: Offset, val size: Size) {
    val centreY: Float get() = position.y + size.height / 2f
}

/**
 * One element being pressed flat, and the geometry it had when the sequence began.
 *
 * Measured once per generation rather than every frame: the element is being moved by this very
 * animation, and measuring it again would feed its own displacement back into the sum.
 */
internal class FlatNode(private val accent: Color) {

    private var bounds: FlatBounds? = null
    private var measuredAt = -1
    private var shattered = false

    fun reset() {
        shattered = false
    }

    fun measure(sequence: BigBangSequence, position: Offset, size: Size) {
        // Nothing without a box: an element that has not been laid out yet, or one that collapsed
        // to nothing, would otherwise take part as a line of debris coming from the top left.
        if (size.width <= 0f || size.height <= 0f) return
        if (sequence.running && measuredAt == sequence.generation) return
        bounds = FlatBounds(position, size)
        measuredAt = sequence.generation
    }

    /**
     * How far this element's own flattening has got, `0f..1f`.
     *
     * The delay is the opposite way round from the collapse: nothing at the edges of the screen,
     * the full hundred and twenty milliseconds in the middle. The middle is where everything is
     * going, so it is the last thing to leave.
     */
    private fun progress(sequence: BigBangSequence, here: FlatBounds): Float {
        val now = sequence.elapsed ?: return 0f
        val awayDp = abs(here.centreY - sequence.centre.y) / sequence.density
        val delay = (1f - min(1f, awayDp / DELAY_REACH)) * 120f
        return ((now - delay) / FLAT_FALL).coerceIn(0f, 1f)
    }

    fun applyTo(scope: GraphicsLayerScope, sequence: BigBangSequence) {
        val now = sequence.elapsed
        if (now == null) {
            scope.reset()
            return
        }

        val here = bounds ?: FlatBounds(sequence.centre, Size.Zero)

        // From the moment the line exists until the new universe is put up, the whole interface
        // is simply not there. Not faded — gone. The canvas carries the picture on its own.
        if (now in FLAT_END..RETURN_AT) {
            scope.reset()
            scope.alpha = 0f
            return
        }

        if (now > RETURN_AT) {
            applyReturn(scope, sequence, here, now - RETURN_AT)
            return
        }

        val k = progress(sequence, here)
        // Squared, not cubed as in the collapse: this is a press rather than a fall, and it wants
        // to be moving noticeably from the first frame.
        val e = k * k

        scope.translationX = 0f
        scope.translationY = snap((sequence.centre.y - here.centreY) * e)
        // Wider as it goes flatter, which is what makes it read as squashed rather than shrunk.
        scope.scaleX = 1f + e * 0.4f
        scope.scaleY = max(0.02f, 1f - e)
        scope.alpha = if (k > 0.8f) max(0f, 1f - (k - 0.8f) / 0.2f) else 1f
    }

    private fun applyReturn(
        scope: GraphicsLayerScope,
        sequence: BigBangSequence,
        here: FlatBounds,
        since: Float,
    ) {
        val awayDp = abs(here.centreY - sequence.centre.y) / sequence.density
        val k = ((since - awayDp * 0.55f) / RETURN_MILLIS).coerceIn(0f, 1f)

        if (k >= 1f) {
            scope.reset()
            return
        }

        // Eased out rather than in: unfolding should settle, not accelerate into place. And the
        // delay runs the other way again — the middle opens first, the edges last.
        val left = 1f - k
        val e = 1f - left * left * left

        scope.translationX = 0f
        scope.translationY = snap((sequence.centre.y - here.centreY) * (1f - e))
        scope.scaleX = 1f + (1f - e) * 0.4f
        scope.scaleY = max(0.02f, e)
        scope.alpha = min(1f, k * 1.6f)
    }

    /**
     * The colour filter this element is seen through, or null while it is simply itself.
     *
     * On the way down the colour is drained out and the brightness driven up, so the interface
     * burns to white as it is pressed flat; on the way back the white drains out of it again.
     * Without this the phase reads as a window being closed rather than as something igniting.
     */
    fun filterFor(sequence: BigBangSequence): ColorFilter? {
        val now = sequence.elapsed ?: return null
        val here = bounds ?: return null

        val saturation: Float
        val brightness: Float
        if (now <= FLAT_END) {
            val k = progress(sequence, here)
            // Twenty stops between colour and white, and saturation and brightness take them
            // together — quantising one and not the other would give a colour that is half drained
            // at a brightness meant for fully drained, which is a shade nothing else on screen has.
            //
            // Stepped for the reason everything in this pass is: this is the whole interface
            // changing colour, and it is the largest smooth transition left in the game. Twenty
            // steps over the best part of a second is a visible ratchet, which is what an interface
            // made of hard-edged blocks should do when it burns.
            val e = quantise(k * k, WASH_STEPS)
            if (e <= 0f) return null
            saturation = 1f - e
            brightness = 1f + e * 2.2f
        } else if (now > RETURN_AT) {
            val awayDp = abs(here.centreY - sequence.centre.y) / sequence.density
            val k = ((now - RETURN_AT - awayDp * 0.55f) / RETURN_MILLIS).coerceIn(0f, 1f)
            if (k >= 1f) return null
            val left = 1f - k
            val e = quantise(1f - left * left * left, WASH_STEPS)
            saturation = 1f
            brightness = 1f + (1f - e) * 1.8f
        } else {
            return null
        }

        val matrix = ColorMatrix().apply { setToSaturation(saturation) }
        // Brightness on top of saturation, applied to the three colour rows only: scaling the
        // alpha row as well would make the element opaque rather than bright.
        for (row in 0..2) {
            for (column in 0..4) matrix.values[row * 5 + column] *= brightness
        }
        return ColorFilter.colorMatrix(matrix)
    }

    /** The blocks this element breaks into. Empty until it is far enough down. */
    fun shatter(sequence: BigBangSequence): List<ShardSpec> {
        if (shattered) return emptyList()
        val here = bounds ?: return emptyList()
        val now = sequence.elapsed ?: return emptyList()
        if (now > FLAT_END) return emptyList()
        if (progress(sequence, here) <= 0.3f) return emptyList()
        shattered = true

        val out = mutableListOf<ShardSpec>()
        val step = 7f * sequence.density
        val columns = (here.size.width / step).roundToInt().coerceIn(2, 24)
        val rows = (here.size.height / step).roundToInt().coerceIn(1, 8)

        for (column in 0 until columns) {
            for (row in 0 until rows) {
                // Not every grid point: a complete grid looks like a picture cut into squares,
                // which is the opposite of something coming apart.
                if (sequence.nextRandom() > 0.42f) continue
                out += ShardSpec(
                    x = here.position.x + here.size.width * (column + 0.5f) / columns,
                    y = here.position.y + here.size.height * (row + 0.5f) / rows,
                    side = if (sequence.nextRandom() < 0.5f) 3f else 6f,
                    speed = 0.020f + sequence.nextRandom() * 0.020f,
                    color = accent,
                )
            }
        }
        return out
    }
}

private fun GraphicsLayerScope.reset() {
    translationX = 0f
    translationY = 0f
    scaleX = 1f
    scaleY = 1f
    alpha = 1f
}

private fun snap(value: Float): Float = (value / SNAP).roundToInt() * SNAP

// ---------------------------------------------------------------------- the seam

/** Reaches every element without a parameter on half the screen. Null when nothing is running. */
val LocalBigBang = staticCompositionLocalOf<BigBangSequence?> { null }

/**
 * Marks an element as something the big bang flattens.
 *
 * Only an accent colour, and no depth: unlike the collapse, every element here runs on exactly the
 * same formula. The difference between a card and the line of text inside it comes entirely from
 * the transforms being *nested* — the card is pressed, and its contents are pressed again inside
 * the already-pressed card — so a depth parameter would be a second way of saying something the
 * layout tree is already saying. What the colour is for is the debris.
 */
fun Modifier.urknall(accent: Color): Modifier = composed {
    val sequence = LocalBigBang.current ?: return@composed this
    val node = remember(accent) { FlatNode(accent) }

    DisposableEffect(sequence, node) {
        sequence.register(node)
        onDispose { sequence.unregister(node) }
    }

    this
        .onGloballyPositioned { coordinates ->
            node.measure(
                sequence,
                coordinates.positionInWindow(),
                Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()),
            )
        }
        .drawWithCache {
            // Built once and reused: a Paint per frame per element is a lot of rubbish to make
            // for something whose only changing field is the filter.
            val paint = Paint()
            onDrawWithContent {
                val filter = node.filterFor(sequence)
                if (filter == null) {
                    drawContent()
                    return@onDrawWithContent
                }
                // A layer, because a colour matrix applies to what has already been drawn: the
                // content goes onto its own surface and the filter is applied as it comes back.
                paint.colorFilter = filter
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(Offset.Zero, size), paint)
                    drawContent()
                    canvas.restore()
                }
            }
        }
        .graphicsLayer { node.applyTo(this, sequence) }
}

/**
 * Marks the point everything is pressed onto.
 *
 * Only the Y of it: the X is the middle of the screen and stays there, because the line the
 * interface becomes has to run across the whole width and be squeezed from both ends evenly.
 */
fun Modifier.bigBangCentre(): Modifier = composed {
    val sequence = LocalBigBang.current ?: return@composed this
    onGloballyPositioned { coordinates ->
        if (sequence.running) return@onGloballyPositioned
        sequence.centreY = coordinates.positionInWindow().y + coordinates.size.height / 2f
    }
}

/**
 * Marks the sheet everything is measured against: how wide the screen is, and where its middle is.
 *
 * Separate from [bigBangCentre] because the two halves of the centre come from different places in
 * the tree, and neither may be waiting on the other — whichever is laid out first simply writes
 * its own number.
 */
fun Modifier.bigBangStage(): Modifier = composed {
    val sequence = LocalBigBang.current ?: return@composed this
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    onGloballyPositioned { coordinates ->
        if (sequence.running) return@onGloballyPositioned
        val size = coordinates.size
        sequence.screen = Size(size.width.toFloat(), size.height.toFloat())
        sequence.centreX = size.width / 2f
        sequence.density = density
    }
}

/** The shards, the point and the matter, drawn over everything else. */
@Composable
fun BigBangCanvas(sequence: BigBangSequence, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Read so the canvas redraws every frame the clock moves.
        sequence.elapsed ?: return@Canvas
        sequence.draw(this)
    }
}

/** Wraps the screen so [urknall] can find the sequence. */
@Composable
fun WithBigBang(sequence: BigBangSequence?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBigBang provides sequence, content = content)
}

/** Steps between full colour and white as the universe is pressed flat. */
private const val WASH_STEPS = 20
