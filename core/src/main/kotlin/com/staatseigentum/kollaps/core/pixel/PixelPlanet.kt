package com.staatseigentum.kollaps.core.pixel

import com.staatseigentum.kollaps.core.BodyKind
import com.staatseigentum.kollaps.core.CelestialTier
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Draws the celestial bodies as pixel art sprites.
 *
 * Everything is rendered into a small integer pixel buffer and only then blown up with
 * nearest-neighbour scaling, which is what gives the chunky look. Each body gets a rotating
 * sprite sheet: the surface lives in an equirectangular texture that is sampled per pixel
 * through a sphere projection, so features travel around the body instead of sliding across it.
 *
 * Shading is quantised to a handful of palette steps and the transitions between them are
 * dithered with a Bayer matrix, the way a sprite artist would.
 */
object PixelPlanet {

    /** Edge length of a sprite in sprite pixels. */
    const val SIZE = 96

    /** Rotation frames per body. */
    const val FRAMES = 24

    /**
     * Renders the full sprite sheet for a tier as raw ARGB buffers, each [SIZE] by [SIZE].
     * Cheap enough to do on a background thread when the player reaches a new tier.
     */
    fun frames(tier: CelestialTier): List<IntArray> {
        val palette = Palette.of(tier)
        val texture = Texture.of(tier)
        return List(FRAMES) { frame ->
            val pixels = IntArray(SIZE * SIZE)
            renderFrame(tier, palette, texture, frame.toFloat() / FRAMES, pixels)
            pixels
        }
    }

    /** A single frame, for previews and tests. */
    fun frame(tier: CelestialTier, index: Int): IntArray {
        val pixels = IntArray(SIZE * SIZE)
        val phase = index.toFloat() / FRAMES
        renderFrame(tier, Palette.of(tier), Texture.of(tier), phase, pixels)
        return pixels
    }

    /** How much of the available space the whole sprite should cover for this tier. */
    fun spriteFraction(tier: CelestialTier): Float =
        (tier.relativeSize * 1.45f).coerceIn(0.25f, 1f)

    /** One full turn in milliseconds. */
    fun spinMillis(kind: BodyKind): Int = when (kind) {
        BodyKind.ROCK -> 7_000
        BodyKind.TERRESTRIAL -> 8_000
        BodyKind.GAS -> 6_000
        BodyKind.STAR -> 9_000
        BodyKind.EXOTIC -> 1_600
        BodyKind.SINGULARITY -> 2_800
    }

    // ------------------------------------------------------------------ frame rendering

    private fun renderFrame(
        tier: CelestialTier,
        palette: Palette,
        texture: Texture,
        phase: Float,
        out: IntArray,
    ) {
        when (tier.kind) {
            BodyKind.SINGULARITY -> renderBlackHole(palette, phase, out)
            BodyKind.EXOTIC -> renderNeutronStar(palette, phase, out)
            else -> renderSphere(tier, palette, texture, phase, out)
        }
    }

    private fun renderSphere(
        tier: CelestialTier,
        palette: Palette,
        texture: Texture,
        phase: Float,
        out: IntArray,
    ) {
        val centre = SIZE / 2f
        val bodyRadius = SIZE / 2f * BODY_FRACTION
        val glowRadius = bodyRadius * if (tier.kind == BodyKind.STAR) 1.5f else 1.18f
        val emissive = tier.kind == BodyKind.STAR
        val spin = phase * TWO_PI

        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val nx = (x + 0.5f - centre) / bodyRadius
                val ny = (y + 0.5f - centre) / bodyRadius
                val distance = sqrt(nx * nx + ny * ny)
                var colour = 0

                if (tier.hasRing && ny < 0f) colour = ringColour(nx, ny, distance, palette, x, y)

                if (distance <= 1f) {
                    colour = spherePixel(nx, ny, distance, spin, palette, texture, emissive, x, y)
                } else if (colour == 0) {
                    colour = glowPixel(distance, bodyRadius, glowRadius, palette, x, y)
                }

                if (tier.hasRing && ny >= 0f) {
                    val ring = ringColour(nx, ny, distance, palette, x, y)
                    if (ring != 0) colour = ring
                }

                out[y * SIZE + x] = colour
            }
        }
    }

    private fun spherePixel(
        nx: Float,
        ny: Float,
        distance: Float,
        spin: Float,
        palette: Palette,
        texture: Texture,
        emissive: Boolean,
        x: Int,
        y: Int,
    ): Int {
        val nz = sqrt((1f - distance * distance).coerceAtLeast(0f))

        // Surface coordinates, so the texture turns with the body instead of sliding over it.
        val latitude = asin(ny.coerceIn(-1f, 1f))
        val longitude = atan2(nx, nz) + spin
        val texel = texture.texelAt(latitude, longitude)
        val material = texture.materialAt(texel)
        val tone = texture.toneAt(texel)

        val lambert = (nx * LIGHT_X + ny * LIGHT_Y + nz * LIGHT_Z).coerceAtLeast(0f)
        var light = if (emissive) {
            // A star is lit from within; only a slight falloff towards the limb.
            0.72f + 0.28f * nz + 0.12f * tone
        } else {
            0.12f + 0.88f * lambert + 0.18f * (tone - 0.5f)
        }
        // Rim light, the classic trick that makes a pixel sphere read as round.
        if (distance > 0.88f) light += 0.25f * (distance - 0.88f) / 0.12f

        return palette.shade(material, light, x, y)
    }

    private fun glowPixel(
        distance: Float,
        bodyRadius: Float,
        glowRadius: Float,
        palette: Palette,
        x: Int,
        y: Int,
    ): Int {
        val reach = glowRadius / bodyRadius
        if (distance >= reach) return 0
        val strength = 1f - (distance - 1f) / (reach - 1f)
        // Dithered instead of smoothly faded, so the halo stays made of pixels.
        return if (strength > bayer(x, y)) palette.glow else 0
    }

    private fun ringColour(
        nx: Float,
        ny: Float,
        distance: Float,
        palette: Palette,
        x: Int,
        y: Int,
    ): Int {
        val rx = nx / RING_OUTER
        val ry = ny / (RING_OUTER * RING_FLATTEN)
        val ring = sqrt(rx * rx + ry * ry)
        if (ring > 1f || ring < RING_INNER) return 0
        // Hide the part that passes behind the body.
        if (distance <= 1f && ny < 0f) return 0

        val band = ((ring - RING_INNER) / (1f - RING_INNER) * 3f).toInt().coerceIn(0, 2)
        if (band == 1 && bayer(x, y) > 0.55f) return 0
        return palette.ring[band]
    }

    private fun renderNeutronStar(palette: Palette, phase: Float, out: IntArray) {
        val centre = SIZE / 2f
        val core = SIZE / 2f * 0.13f
        val reach = SIZE / 2f * 0.96f
        // Two opposite jets look the same after half a turn, so the cycle only covers 180° —
        // otherwise half the sprite sheet would be duplicates.
        val sweep = phase * PI_F
        val jetX = sin(sweep)
        val jetY = -cos(sweep)

        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val dx = x + 0.5f - centre
                val dy = y + 0.5f - centre
                val distance = sqrt(dx * dx + dy * dy)
                var colour = 0

                // Two tight jets sweeping around like a lighthouse beam.
                val along = abs(dx * jetX + dy * jetY)
                val across = abs(dx * -jetY + dy * jetX)
                val width = core * 0.55f + along * 0.13f
                if (across < width && along < reach) {
                    val fade = 1f - along / reach
                    if (fade > bayer(x, y) * 0.55f) {
                        colour = if (across < width * 0.5f) palette.ring[0] else palette.ring[1]
                    }
                }

                // A tight halo, so the star reads as brutally bright rather than fuzzy.
                if (distance < core * 2.6f) {
                    val halo = 1f - distance / (core * 2.6f)
                    if (halo > bayer(x, y) * 0.8f) colour = palette.glow
                }
                if (distance < core) colour = palette.shade(0, 1.4f - distance / core, x, y)

                out[y * SIZE + x] = colour
            }
        }
    }

    private fun renderBlackHole(palette: Palette, phase: Float, out: IntArray) {
        val centre = SIZE / 2f
        val horizon = SIZE / 2f * 0.30f
        val spin = phase * TWO_PI

        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val dx = x + 0.5f - centre
                val dy = y + 0.5f - centre
                val distance = sqrt(dx * dx + dy * dy)
                var colour = 0

                // The disk seen almost edge on: a thin flattened annulus, not a slab.
                val ringX = dx / (SIZE / 2f * 0.95f)
                val ringY = dy / (SIZE / 2f * 0.30f)
                val ring = sqrt(ringX * ringX + ringY * ringY)
                val diskColour = if (ring in DISK_INNER..1f) {
                    // Brightest at the inner edge, with a hot spot travelling around the ring.
                    val inner = 1f - (ring - DISK_INNER) / (1f - DISK_INNER)
                    val travel = 0.5f + 0.5f * sin(atan2(ringY, ringX) - spin)
                    val heat = inner * 0.6f + travel * 0.4f
                    val step = (heat * 2f + bayer(x, y) - 0.5f).roundToInt().coerceIn(0, 2)
                    palette.ring[2 - step]
                } else {
                    0
                }

                // Far half of the disk, then the horizon, then the near half over the top of it.
                if (dy < 0f) colour = diskColour

                // A faint glow above the hole, so it does not sit on the background flat.
                if (colour == 0 && dy < 0f && distance < horizon * 1.9f) {
                    val halo = 1f - distance / (horizon * 1.9f)
                    if (halo > bayer(x, y) * 1.6f) colour = palette.glow
                }

                if (distance < horizon) {
                    colour = HORIZON
                } else if (distance < horizon * 1.16f) {
                    // Photon ring: the light that orbits just before it falls in.
                    colour = palette.ring[0]
                }

                if (dy >= 0f && diskColour != 0) colour = diskColour

                // The far side of the disk, bent up and over the horizon. Without this arc a
                // black hole seen edge on just reads as a dark blob between two wings.
                if (dy < 0f && abs(ringX) <= 1f) {
                    val arc = -horizon * (0.15f + 1.35f * sqrt(1f - ringX * ringX))
                    if (abs(dy - arc) < LENS_THICKNESS) colour = palette.ring[0]
                }

                out[y * SIZE + x] = colour
            }
        }
    }

    // ------------------------------------------------------------------ palette

    /**
     * A tier's colours: three materials, each quantised into [LEVELS] shading steps. Everything
     * the sprite draws comes out of this table, which is what keeps a body looking like one
     * coherent piece of art rather than a gradient.
     */
    private class Palette(
        val ramps: Array<IntArray>,
        val glow: Int,
        val ring: IntArray,
    ) {
        fun shade(material: Int, light: Float, x: Int, y: Int): Int {
            val dithered = light * (LEVELS - 1) + (bayer(x, y) - 0.5f) * DITHER_STRENGTH
            val level = dithered.roundToInt().coerceIn(0, LEVELS - 1)
            return ramps[material][level]
        }

        companion object {
            fun of(tier: CelestialTier): Palette {
                val primary = tier.primaryColor.toInt()
                val secondary = tier.secondaryColor.toInt()
                val glow = tier.glowColor.toInt()

                val accent = tier.accentColor?.toInt() ?: secondary
                val materials = when (tier.kind) {
                    BodyKind.TERRESTRIAL -> intArrayOf(primary, accent, mix(primary, WHITE, 0.75f))
                    BodyKind.GAS -> intArrayOf(primary, secondary, mix(accent, glow, 0.45f))
                    BodyKind.STAR -> intArrayOf(primary, mix(secondary, primary, 0.4f), WHITE)
                    BodyKind.EXOTIC -> intArrayOf(WHITE, primary, glow)
                    BodyKind.SINGULARITY -> intArrayOf(HORIZON, secondary, glow)
                    BodyKind.ROCK -> intArrayOf(primary, secondary, mix(primary, WHITE, 0.35f))
                }

                // The accretion disk needs real contrast between its three steps, otherwise it
                // reads as one flat orange lens instead of glowing matter falling inwards.
                val ring = if (tier.kind == BodyKind.SINGULARITY) {
                    intArrayOf(mix(glow, WHITE, 0.7f), glow, mix(secondary, SHADOW, 0.45f))
                } else {
                    intArrayOf(mix(glow, WHITE, 0.35f), glow, mix(glow, secondary, 0.5f))
                }

                return Palette(
                    ramps = Array(materials.size) { rampOf(materials[it], glow) },
                    glow = withAlpha(mix(glow, SHADOW, 0.35f), 0xB0),
                    ring = ring,
                )
            }

            /** Dark to light, shadows tinted cool and highlights pulled towards the glow. */
            private fun rampOf(base: Int, glow: Int): IntArray {
                val steps = floatArrayOf(-0.62f, -0.34f, 0f, 0.24f, 0.48f)
                return IntArray(LEVELS) { index ->
                    val amount = steps[index]
                    if (amount < 0f) mix(base, SHADOW, -amount) else mix(base, glow, amount)
                }
            }
        }
    }

    // ------------------------------------------------------------------ surface texture

    /**
     * An equirectangular map of the surface. Sampled with nearest neighbour on purpose — that
     * is what keeps craters and cloud bands looking hand placed.
     */
    private class Texture(private val materials: ByteArray, private val tones: FloatArray) {

        /** Texel under a surface point. Split from the reads so sampling allocates nothing. */
        fun texelAt(latitude: Float, longitude: Float): Int {
            val u = (longitude / TWO_PI).mod(1f)
            val v = ((latitude / PI_F) + 0.5f).coerceIn(0f, 0.999f)
            val tx = (u * TEXTURE_WIDTH).toInt().coerceIn(0, TEXTURE_WIDTH - 1)
            val ty = (v * TEXTURE_HEIGHT).toInt().coerceIn(0, TEXTURE_HEIGHT - 1)
            return ty * TEXTURE_WIDTH + tx
        }

        fun materialAt(texel: Int): Int = materials[texel].toInt()

        fun toneAt(texel: Int): Float = tones[texel]

        companion object {
            fun of(tier: CelestialTier): Texture {
                val materials = ByteArray(TEXTURE_WIDTH * TEXTURE_HEIGHT)
                val tones = FloatArray(TEXTURE_WIDTH * TEXTURE_HEIGHT)
                val random = Random(tier.index * 6_151L + 17L)

                for (index in tones.indices) tones[index] = 0.5f

                when (tier.kind) {
                    BodyKind.ROCK -> craters(materials, tones, random)
                    BodyKind.TERRESTRIAL -> continents(materials, tones, random)
                    BodyKind.GAS -> bands(materials, tones, random)
                    else -> mottle(materials, tones, random)
                }
                return Texture(materials, tones)
            }

            private fun craters(materials: ByteArray, tones: FloatArray, random: Random) {
                repeat(22) {
                    val cx = random.nextInt(TEXTURE_WIDTH)
                    val cy = 3 + random.nextInt(TEXTURE_HEIGHT - 6)
                    val radius = 2 + random.nextInt(4)
                    forEachTexel(cx, cy, radius + 1) { x, y, distance ->
                        val index = y * TEXTURE_WIDTH + x
                        when {
                            distance <= radius - 1 -> {
                                materials[index] = 1
                                tones[index] = 0.28f
                            }

                            distance <= radius + 1 -> {
                                materials[index] = 2
                                tones[index] = 0.72f
                            }
                        }
                    }
                }
            }

            private fun continents(materials: ByteArray, tones: FloatArray, random: Random) {
                repeat(9) {
                    val cx = random.nextInt(TEXTURE_WIDTH)
                    val cy = 6 + random.nextInt(TEXTURE_HEIGHT - 12)
                    val radius = 4 + random.nextInt(6)
                    forEachTexel(cx, cy, radius) { x, y, distance ->
                        // A wobbly edge reads as coastline instead of a circle.
                        if (distance < radius - random.nextInt(3)) {
                            val index = y * TEXTURE_WIDTH + x
                            materials[index] = 1
                            tones[index] = 0.45f + random.nextFloat() * 0.3f
                        }
                    }
                }
                // Ice caps.
                for (y in 0 until TEXTURE_HEIGHT) {
                    val polar = abs(y - (TEXTURE_HEIGHT - 1) / 2f) / (TEXTURE_HEIGHT / 2f)
                    if (polar < 0.82f) continue
                    for (x in 0 until TEXTURE_WIDTH) {
                        val index = y * TEXTURE_WIDTH + x
                        materials[index] = 2
                        tones[index] = 0.8f
                    }
                }
            }

            private fun bands(materials: ByteArray, tones: FloatArray, random: Random) {
                val bandTone = FloatArray(TEXTURE_HEIGHT)
                val bandMaterial = ByteArray(TEXTURE_HEIGHT)
                var y = 0
                while (y < TEXTURE_HEIGHT) {
                    val height = 2 + random.nextInt(4)
                    val material = if (random.nextFloat() < 0.45f) 1.toByte() else 0.toByte()
                    val tone = 0.3f + random.nextFloat() * 0.5f
                    for (offset in 0 until height) {
                        val row = y + offset
                        if (row >= TEXTURE_HEIGHT) break
                        bandMaterial[row] = material
                        bandTone[row] = tone
                    }
                    y += height
                }
                for (row in 0 until TEXTURE_HEIGHT) {
                    for (x in 0 until TEXTURE_WIDTH) {
                        // Waviness so the bands are not perfectly straight lines.
                        val wobble = (sin(x * 0.22f + row) * 1.4f).toInt()
                        val source = (row + wobble).coerceIn(0, TEXTURE_HEIGHT - 1)
                        val index = row * TEXTURE_WIDTH + x
                        materials[index] = bandMaterial[source]
                        tones[index] = bandTone[source]
                    }
                }
                // The one big storm every gas giant deserves — an oval, stretched with the bands.
                val stormX = random.nextInt(TEXTURE_WIDTH)
                val stormY = TEXTURE_HEIGHT / 2 + random.nextInt(8) - 4
                val stormWidth = 7
                val stormHeight = 3
                for (dy in -stormHeight..stormHeight) {
                    val y = stormY + dy
                    if (y !in 0 until TEXTURE_HEIGHT) continue
                    for (dx in -stormWidth..stormWidth) {
                        val fx = dx.toFloat() / stormWidth
                        val fy = dy.toFloat() / stormHeight
                        if (fx * fx + fy * fy > 1f) continue
                        val index = y * TEXTURE_WIDTH + (stormX + dx).mod(TEXTURE_WIDTH)
                        materials[index] = 2
                        tones[index] = 0.72f
                    }
                }
            }

            private fun mottle(materials: ByteArray, tones: FloatArray, random: Random) {
                for (index in materials.indices) {
                    val roll = random.nextFloat()
                    materials[index] = if (roll > 0.78f) 2 else if (roll > 0.4f) 1 else 0
                    tones[index] = 0.35f + random.nextFloat() * 0.5f
                }
            }

            /** Walks a disc in texture space, wrapping around the seam in longitude. */
            private inline fun forEachTexel(
                centreX: Int,
                centreY: Int,
                radius: Int,
                block: (x: Int, y: Int, distance: Float) -> Unit,
            ) {
                for (dy in -radius..radius) {
                    val y = centreY + dy
                    if (y !in 0 until TEXTURE_HEIGHT) continue
                    for (dx in -radius..radius) {
                        val distance = sqrt((dx * dx + dy * dy).toFloat())
                        if (distance > radius) continue
                        val x = (centreX + dx).mod(TEXTURE_WIDTH)
                        block(x, y, distance)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun mix(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        val a = 0xFF
        val r = lerpChannel(from shr 16 and 0xFF, to shr 16 and 0xFF, t)
        val g = lerpChannel(from shr 8 and 0xFF, to shr 8 and 0xFF, t)
        val b = lerpChannel(from and 0xFF, to and 0xFF, t)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerpChannel(from: Int, to: Int, t: Float): Int =
        (from + (to - from) * t).roundToInt().coerceIn(0, 255)

    private fun withAlpha(colour: Int, alpha: Int): Int =
        (alpha shl 24) or (colour and 0x00FFFFFF)

    private fun bayer(x: Int, y: Int): Float = BAYER[(y and 3) * 4 + (x and 3)] * (1f / 16f)

    private const val LEVELS = 5
    private const val DITHER_STRENGTH = 0.9f
    private const val BODY_FRACTION = 0.62f
    private const val RING_OUTER = 1.62f
    private const val RING_INNER = 0.72f
    private const val RING_FLATTEN = 0.30f
    private const val DISK_INNER = 0.55f
    private const val LENS_THICKNESS = 2.2f
    private const val TEXTURE_WIDTH = 96
    private const val TEXTURE_HEIGHT = 48
    private const val TWO_PI = 6.2831855f
    private const val PI_F = 3.1415927f

    // Light comes from the upper left, the convention pixel artists shade to.
    private const val LIGHT_X = -0.52f
    private const val LIGHT_Y = -0.58f
    private const val LIGHT_Z = 0.63f

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val SHADOW = 0xFF161029.toInt()
    private const val LAND = 0xFF2F7A3A.toInt()
    private const val HORIZON = 0xFF05030A.toInt()

    /** Ordered dithering matrix — the reason the shading steps blend instead of banding. */
    private val BAYER = intArrayOf(
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5,
    )
}
