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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.Collectors
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.pixel.PixelCraft
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Which side of the body a layer draws.
 *
 * An orbit seen from the side passes behind the thing it goes round for half of every revolution,
 * and until now nothing did: every satellite was drawn over the planet, including the ones that
 * were meant to be on the far side of it. That reads as stickers on the glass rather than as a
 * system, and it is the single thing that makes the difference between the two.
 *
 * So the layer is drawn twice, once under the body and once over it, and each pass keeps only the
 * half it is responsible for. Two canvases and one comparison, which is a great deal cheaper than
 * sorting anything.
 */
enum class OrbitSide {
    /** The far half, drawn under the body so the body hides it. */
    BEHIND,

    /** The near half, drawn over the body. */
    INFRONT,
}

/** One thing in orbit: which ring it sits on, where it started, how fast it goes. */
private class Orbiter(
    val ring: Int,
    val phase: Float,
    val speed: Float,
    val shape: Int,
    val accent: Color,
)

/**
 * The collectors, in orbit around the body.
 *
 * In Cookie Clicker you can see your farms; here the entire fleet was a list of numbers in a shop,
 * and nothing on the screen changed however much of it you owned.
 *
 * ## What was wrong with the first version
 *
 * Two things, and both were arithmetic rather than taste.
 *
 * Every collector got a ring of its own at `1.18 + index × 0.16` body radii. That was written when
 * there were six collectors; there are twenty-three. The outermost ring landed at four and a half
 * body radii — several screens out, so most of the fleet was drawn outside the canvas and simply
 * never appeared. Worse, the radius was measured against `spriteFraction × 0.31`, while the body
 * is actually drawn at `spriteFraction × 0.5` — so the first four rings, the ones a player owns
 * first, orbited *inside* the planet.
 *
 * Now the rings are laid out between the body's edge and the edge of the box, however many kinds
 * are owned, and the body itself is drawn a little smaller to leave them a lane. What a ring
 * *means* has changed with it: rings are handed out in the order machines are bought rather than
 * by catalogue position, so an early fleet fills the inner lanes instead of scattering across
 * twenty-three of them.
 */
@Composable
fun Satellites(
    state: GameState,
    tier: CelestialTier,
    side: OrbitSide,
    modifier: Modifier = Modifier,
) {
    val orbiters = remember(state.collectors) { buildOrbiters(state) }
    if (orbiters.isEmpty()) return

    val factory = LocalSpriteFactory.current
    // One bitmap per silhouette and colour, not per machine: a ring of nine identical craft is
    // one image drawn nine times.
    val craft = remember(orbiters, factory) {
        orbiters.associate { it.shape to it.accent }
            .mapValues { (shape, accent) ->
                factory.bitmap(PixelCraft.pixels(shape, accent.argb()), PixelCraft.SIDE)
            }
    }

    val quiet = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "orbit")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ORBIT_MILLIS, easing = LinearEasing)),
        label = "turn",
    )
    val advance = if (quiet) 0f else turn

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val block = floor(3.dp.toPx()).coerceAtLeast(2f)
        val centreX = size.width / 2f
        val centreY = size.height / 2f
        val edge = (block * PixelCraft.SIDE).roundToInt()

        val lanes = laneRadii(size.width, size.height, tier, orbiters, edge.toFloat())

        for (orbiter in orbiters) {
            val angle = (orbiter.phase + advance * orbiter.speed) * TWO_PI
            // Positive sine is the near half. The flattening below is what makes the ring read
            // as an orbit seen from slightly above rather than as a circle drawn around a disc.
            val front = sin(angle) >= 0f
            if (front != (side == OrbitSide.INFRONT)) continue

            val radius = lanes[orbiter.ring]
            val x = centreX + cos(angle) * radius
            val y = centreY + sin(angle) * radius * FLATTEN

            val image = craft[orbiter.shape] ?: continue
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(PixelCraft.SIDE, PixelCraft.SIDE),
                // Snapped to the block grid, so a craft never lands on a half pixel and softens
                // its own edges — the one thing this whole style cannot survive.
                dstOffset = IntOffset(
                    snap(x - edge / 2f, block).roundToInt(),
                    snap(y - edge / 2f, block).roundToInt(),
                ),
                dstSize = IntSize(edge, edge),
                filterQuality = FilterQuality.None,
            )
        }
    }
}

/**
 * Where each lane sits, in pixels from the centre.
 *
 * The band runs from just outside the body to just inside the box, and the lanes share it evenly.
 * Both ends matter: the inner one is what stopped the fleet orbiting through the planet, and the
 * outer one is what stopped it flying off the screen.
 *
 * The half-width of a craft is taken off the outer end, because a lane is the path of a *centre*
 * and a machine standing on the last lane still has three pixels of itself further out.
 */
private fun laneRadii(
    width: Float,
    height: Float,
    tier: CelestialTier,
    orbiters: List<Orbiter>,
    craftEdge: Float,
): FloatArray {
    val lanes = (orbiters.maxOf { it.ring } + 1).coerceAtLeast(1)
    val shorter = minOf(width, height)
    val body = bodyRadius(shorter, tier)

    val inner = body + craftEdge * 0.6f
    // Horizontally, because that is where the widest point of a flattened ring is; the vertical
    // extreme is [FLATTEN] of this and has room to spare on any shape of box.
    val outer = (width / 2f - craftEdge * 0.6f).coerceAtLeast(inner)

    if (lanes == 1) return floatArrayOf((inner + outer) / 2f)
    val step = (outer - inner) / (lanes - 1)
    return FloatArray(lanes) { inner + it * step }
}

/**
 * The rings a state deserves, innermost first.
 *
 * Ordered by what is owned rather than by catalogue position: a player with three kinds of machine
 * gets three lanes next to the body, not lanes four, nine and seventeen with nothing in between.
 */
private fun buildOrbiters(state: GameState): List<Orbiter> {
    val owned = Collectors.all.filter { state.ownedOf(it.id) > 0 }
    if (owned.isEmpty()) return emptyList()

    val result = ArrayList<Orbiter>()
    owned.forEachIndexed { position, collector ->
        // More kinds than lanes means the later ones share the outer lanes rather than being
        // given lanes that do not fit. Twenty-three rings around one planet was never a picture
        // of anything.
        val ring = position * MAX_RINGS / owned.size
        val count = state.ownedOf(collector.id)

        // Counts reach the thousands; drawing one craft each would be a solid ring. Every
        // doubling adds one, so the ring keeps growing without ever becoming a smear.
        val craft = (1 + log2(count.toFloat())).toInt().coerceIn(1, MAX_PER_RING)
        // The catalogue index rather than the position, so a machine keeps the same silhouette
        // and colour as the fleet around it grows.
        val catalogue = Collectors.all.indexOf(collector)
        for (index in 0 until craft) {
            result += Orbiter(
                ring = ring,
                // Spread around the lane, offset per kind so two lanes never march in step.
                phase = ((index.toFloat() / craft) + catalogue * 0.13f) % 1f,
                speed = if (ring % 2 == 0) 1f else -0.78f,
                shape = catalogue % PixelCraft.COUNT,
                accent = RING_COLOURS[catalogue % RING_COLOURS.size],
            )
        }
    }
    return result
}

/**
 * How much of the shorter side the body itself covers, as a radius.
 *
 * Kept next to the orbits rather than imported from the renderer, because the number that matters
 * here is what ends up *on screen*: [CelestialBody] draws its sprite at `spriteFraction` of the
 * shorter side, scaled by whatever the caller asked for, and the fleet has to clear that and not
 * the sprite's own resolution.
 */
internal fun bodyRadius(shorter: Float, tier: CelestialTier): Float =
    shorter * PixelPlanet.spriteFraction(tier) * BODY_SCALE_IN_FIELD / 2f

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

/**
 * The colour as one packed integer, for handing to the sprite renderer.
 *
 * Written out rather than taken from `androidx.compose.ui.graphics.toArgb`, whose availability
 * differs between the Android artefacts and the multiplatform ones this file is also compiled
 * against — and four multiplications are not worth a conditional import.
 */
private fun Color.argb(): Int {
    val a = (alpha * 255f + 0.5f).toInt() and 0xFF
    val r = (red * 255f + 0.5f).toInt() and 0xFF
    val g = (green * 255f + 0.5f).toInt() and 0xFF
    val b = (blue * 255f + 0.5f).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private val RING_COLOURS = listOf(
    Color(0xFF8E95C4),
    Color(0xFF5CE1A6),
    Color(0xFFFFB74D),
    Color(0xFF7C5CFF),
    Color(0xFFE9ECFF),
    Color(0xFF6EC6FF),
)

/**
 * How much of its own field the body is drawn at when it has a fleet around it.
 *
 * The one number that had to give. A body drawn at its full `spriteFraction` fills the shorter
 * side of the box outright at the top of the ladder, and a ring outside something that already
 * touches both edges has nowhere to be — every arrangement of orbits either crossed the planet or
 * left the screen. Four fifths leaves a lane wide enough for two craft, and at the sizes the body
 * is actually drawn at that is a difference nobody looks at twice.
 *
 * Only in the field. The tier celebration draws the same body at its full size, because there is
 * nothing in orbit there to make room for.
 */
internal const val BODY_SCALE_IN_FIELD = 0.8f

/** How flat the rings are: the vertical half-axis as a share of the horizontal one. */
private const val FLATTEN = 0.34f

private const val MAX_RINGS = 5
private const val MAX_PER_RING = 6
private const val ORBIT_MILLIS = 22_000
private const val TWO_PI = 6.2831855f
