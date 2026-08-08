package com.staatseigentum.kollaps.core.pixel

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * How far away a star is.
 *
 * The whole illusion rests on this: three sets of stars drifting at three speeds read as depth,
 * where one set drifting at one speed reads as a texture sliding across the screen. The near layer
 * is also the only one that twinkles, because a star you can barely make out does not need to.
 */
enum class SkyLayer(
    val count: Int,
    /** Edge length in grid blocks. */
    val blocks: Int,
    /** Share of the screen width the layer travels in one pass. */
    val drift: Float,
    val brightness: Float,
    val twinkles: Boolean,
) {
    FERN(count = 150, blocks = 1, drift = 0.10f, brightness = 0.45f, twinkles = false),
    MITTE(count = 70, blocks = 1, drift = 0.28f, brightness = 0.75f, twinkles = true),
    NAH(count = 26, blocks = 2, drift = 0.62f, brightness = 1.0f, twinkles = true),
}

/** Positions are fractions of the screen, so the same sky fits a phone and a tablet. */
data class SkyStar(
    val x: Float,
    val y: Float,
    val layer: SkyLayer,
    /** Where in the twinkle cycle this star starts, and how fast it runs through it. */
    val offset: Float,
    val speed: Float,
    val warm: Boolean,
)

/** A block of nebula: where it sits, how solid it is, and how far it leans from tint to accent. */
data class SkyCloud(
    val x: Float,
    val y: Float,
    val alpha: Float,
    val warmth: Float,
    /** The densest blocks are drawn fatter, so a cloud has a body and not a speckled outline. */
    val wide: Boolean,
)

/** A block of the dust lane that runs across the sky. */
data class SkyDust(val x: Float, val y: Float, val alpha: Float)

/**
 * The background sky, worked out as numbers.
 *
 * Here rather than in the drawing code for the same reason [PixelPlanet] is: deciding what the
 * sky looks like is arithmetic that can be checked, and painting it is a loop over rectangles that
 * cannot. Everything below is deterministic — the same seed every time — so the sky a player sees
 * on Tuesday is the sky they saw on Monday, and so a test can say what it expects.
 */
object Sky {

    val LANE_INTERCEPT = 0.18f
    val LANE_SLOPE = 0.5f
    val LANE_WIDTH = 0.17f

    /** How hard a block has to argue to be part of a cloud. Higher is sparser. */
    private const val DITHER = 1.35f

    private const val LOBES = 5
    private const val CLOUD_CANDIDATES = 2_600
    private const val DUST_CANDIDATES = 1_400

    private const val TWO_PI = (2.0 * PI).toFloat()

    /** Every star, all three layers, furthest first. */
    fun stars(): List<SkyStar> {
        val random = Random(0x5EED)
        return SkyLayer.entries.flatMap { layer ->
            List(layer.count) {
                SkyStar(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    layer = layer,
                    offset = random.nextFloat(),
                    speed = 0.5f + random.nextFloat() * 1.5f,
                    warm = random.nextFloat() < 0.25f,
                )
            }
        }
    }

    /**
     * The nebulae, as the blocks that survived the density test.
     *
     * Each lobe is a circle whose radius wobbles with the angle, which is the cheapest way to get
     * something that does not read as a circle. A block is kept when the strongest lobe over it
     * beats that block's own random threshold — that comparison is the dither, and it is what
     * makes the edges break into single pixels instead of ending on a line.
     *
     * @param depth how far up the ladder the player is, in `0f..1f`. A meteorite hangs in a thin
     *   sky and a black hole in a thick one, and that is a different field rather than the same
     *   one at a different opacity.
     */
    fun clouds(depth: Float): List<SkyCloud> {
        val random = Random(0xC10D)
        val lobes = List(LOBES) {
            Lobe(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.16f + random.nextFloat() * 0.22f,
                wobble = 2 + random.nextInt(3),
                phase = random.nextFloat() * TWO_PI,
                warmth = random.nextFloat(),
            )
        }
        val reach = 0.55f + depth.coerceIn(0f, 1f) * 0.75f

        return buildList {
            // A plain loop rather than `repeat`, because the dither works by skipping candidates
            // and `continue` needs a loop to belong to.
            for (candidate in 0 until CLOUD_CANDIDATES) {
                val x = random.nextFloat()
                val y = random.nextFloat()
                val threshold = random.nextFloat()

                var best = 0f
                var warmth = 0f
                for (lobe in lobes) {
                    val strength = lobe.strengthAt(x, y)
                    if (strength > best) {
                        best = strength
                        warmth = lobe.warmth
                    }
                }

                val density = best * reach
                if (density <= threshold * DITHER) continue
                add(
                    SkyCloud(
                        x = x,
                        y = y,
                        alpha = (0.05f + 0.13f * density).coerceAtMost(0.22f),
                        warmth = warmth,
                        wide = density > 0.75f,
                    ),
                )
            }
        }
    }

    /**
     * The dust lane: a diagonal band, densest along its spine and fraying at both edges.
     *
     * Built from the same dither the clouds use, so the two belong to one sky rather than looking
     * like two effects that happen to share a screen.
     */
    fun dust(): List<SkyDust> {
        val random = Random(0xD057)
        return buildList {
            for (candidate in 0 until DUST_CANDIDATES) {
                val x = random.nextFloat()
                val y = random.nextFloat()

                val offset = (y - spineAt(x)) / LANE_WIDTH
                val density = 1f - offset * offset
                if (density <= 0f) continue
                if (density <= random.nextFloat()) continue

                add(SkyDust(x = x, y = y, alpha = 0.06f + 0.16f * density))
            }
        }
    }

    /** Where the middle of the dust lane sits at this fraction of the width. */
    fun spineAt(x: Float): Float = LANE_INTERCEPT + x * LANE_SLOPE

    /** Keeps a drifting position inside `0f..1f`, so the field wraps instead of running out. */
    fun wrap(value: Float): Float {
        val fraction = value % 1f
        return if (fraction < 0f) fraction + 1f else fraction
    }

    private class Lobe(
        val x: Float,
        val y: Float,
        val radius: Float,
        val wobble: Int,
        val phase: Float,
        val warmth: Float,
    ) {
        /** One at the centre, zero at the edge, and nothing beyond it. */
        fun strengthAt(px: Float, py: Float): Float {
            val dx = px - x
            val dy = py - y
            val distance = sqrt(dx * dx + dy * dy)
            val edge = radius * (0.72f + 0.28f * sin(wobble * atan2(dy, dx) + phase))
            if (distance >= edge) return 0f
            return 1f - distance / edge
        }
    }
}
