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

    /** Edge length of the largest sprite, in sprite pixels. */
    const val BASE_SIZE = 288

    /** Rotation frames per body. */
    const val FRAMES = 24

    /**
     * Edge length of this tier's sprite, in sprite pixels.
     *
     * Resolution follows the tier rather than being one number for everybody. Sprites are blown
     * up by a whole-number factor, so a single high resolution would leave the small bodies at
     * factor one — no visible pixel blocks at all — while the big ones would have to shrink to
     * keep a factor of two. Scaling the buffer with the body instead keeps every tier at the same
     * on-screen block size, and the detail grows with how large the body actually appears.
     */
    fun size(tier: CelestialTier): Int =
        ((BASE_SIZE * spriteFraction(tier) / GRID).roundToInt() * GRID).coerceAtLeast(MIN_SIZE)

    /**
     * Renders the full sprite sheet for a tier as raw ARGB buffers, each [size] by [size].
     * Costs tens of milliseconds, so it belongs on a background thread.
     */
    fun frames(tier: CelestialTier, skin: Skin = Skins.ORIGINAL): List<IntArray> {
        val side = size(tier)
        val palette = Palette.of(tier, skin)
        val texture = Texture.of(tier, side)
        return List(FRAMES) { frame ->
            val pixels = IntArray(side * side)
            renderFrame(tier, side, palette, texture, frame.toFloat() / FRAMES, pixels)
            pixels
        }
    }

    /** A single frame, for previews and tests. */
    fun frame(tier: CelestialTier, index: Int): IntArray = frame(tier, index, size(tier))

    /**
     * A single frame at an explicit resolution. The launcher icon is generated from the black
     * hole at whatever edge length each density folder wants, and asking for that size directly
     * beats scaling a sprite that happens to be a different size.
     */
    fun frame(
        tier: CelestialTier,
        index: Int,
        size: Int,
        skin: Skin = Skins.ORIGINAL,
    ): IntArray {
        val pixels = IntArray(size * size)
        val phase = index.toFloat() / FRAMES
        renderFrame(tier, size, Palette.of(tier, skin), Texture.of(tier, size), phase, pixels)
        return pixels
    }

    /**
     * How much of the tap area the sprite fills. The tier says so itself — there is deliberately
     * no factor in between, because a factor large enough to make the small bodies look right
     * pushed the top of the ladder into the clamp, and everything from Jupiter upwards came out
     * exactly the same size.
     */
    fun spriteFraction(tier: CelestialTier): Float = tier.relativeSize.coerceIn(0.25f, 1f)

    /** One full turn in milliseconds. */
    fun spinMillis(kind: BodyKind): Int = when (kind) {
        BodyKind.ROCK -> 7_000
        BodyKind.TERRESTRIAL -> 8_000
        BodyKind.GAS -> 6_000
        BodyKind.STAR -> 9_000
        BodyKind.EXOTIC -> 1_600
        // Slower than a pulsar and faster than a star: it is dense and small, but nothing about
        // it is spinning hundreds of times a second.
        BodyKind.REMNANT -> 4_000
        BodyKind.SINGULARITY -> 2_800
    }

    // ------------------------------------------------------------------ frame rendering

    private fun renderFrame(
        tier: CelestialTier,
        size: Int,
        palette: Palette,
        texture: Texture,
        phase: Float,
        out: IntArray,
    ) {
        when (tier.kind) {
            BodyKind.SINGULARITY -> renderBlackHole(size, palette, phase, out)
            BodyKind.EXOTIC -> renderNeutronStar(size, palette, phase, out)
            else -> renderSphere(tier, size, palette, texture, phase, out)
        }
    }

    private fun renderSphere(
        tier: CelestialTier,
        size: Int,
        palette: Palette,
        texture: Texture,
        phase: Float,
        out: IntArray,
    ) {
        val centre = size / 2f
        val bodyRadius = size / 2f * BODY_FRACTION
        val glowRadius = bodyRadius * when (tier.kind) {
            BodyKind.STAR -> 1.5f
            // Tight and fierce rather than broad: the light comes off a body the size of a
            // planet, so it does not spill the way a giant's corona does.
            BodyKind.REMNANT -> 1.34f
            else -> 1.18f
        }
        val emissive = tier.kind == BodyKind.STAR || tier.kind == BodyKind.REMNANT
        val spin = phase * TWO_PI

        for (y in 0 until size) {
            for (x in 0 until size) {
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

                out[y * size + x] = colour
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

    private fun renderNeutronStar(size: Int, palette: Palette, phase: Float, out: IntArray) {
        val centre = size / 2f
        val core = size / 2f * 0.13f
        val reach = size / 2f * 0.96f
        // Two opposite jets look the same after half a turn, so the cycle only covers 180° —
        // otherwise half the sprite sheet would be duplicates.
        val sweep = phase * PI_F
        val jetX = sin(sweep)
        val jetY = -cos(sweep)

        for (y in 0 until size) {
            for (x in 0 until size) {
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

                out[y * size + x] = colour
            }
        }
    }

    private fun renderBlackHole(size: Int, palette: Palette, phase: Float, out: IntArray) {
        val centre = size / 2f
        val horizon = size / 2f * 0.30f
        val spin = phase * TWO_PI
        // The lensing arc is a drawn line, so its width has to follow the buffer or it would
        // thin out to a hairline as the resolution goes up.
        val lensThickness = size / 96f * LENS_THICKNESS

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x + 0.5f - centre
                val dy = y + 0.5f - centre
                val distance = sqrt(dx * dx + dy * dy)
                var colour = 0

                // The disk seen almost edge on: a thin flattened annulus, not a slab.
                val ringX = dx / (size / 2f * 0.95f)
                val ringY = dy / (size / 2f * 0.30f)
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
                } else if (distance < horizon * 1.07f) {
                    // Photon ring: the light that orbits just before it falls in. Kept thin and
                    // no brighter than the disk — drawn in the lightest colour it read as an
                    // outline around the horizon, which turned the whole sprite into an eye.
                    colour = palette.ring[1]
                }

                if (dy >= 0f && diskColour != 0) colour = diskColour

                // The far side of the disk, bent up and over the horizon. Without this arc a
                // black hole seen edge on just reads as a dark blob between two wings.
                if (dy < 0f && abs(ringX) <= 1f) {
                    val arc = -horizon * (0.15f + 1.35f * sqrt(1f - ringX * ringX))
                    // Same colours as the disk it is a bent image of, brightest in the middle
                    // where the material is closest to the hole.
                    if (abs(dy - arc) < lensThickness) {
                        val heat = 1f - abs(ringX)
                        colour = if (heat + bayer(x, y) * 0.5f > 0.75f) palette.ring[0] else palette.ring[1]
                    }
                }

                out[y * size + x] = colour
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
            /**
             * The ramps for a tier, put through a scheme.
             *
             * The skin is applied to the four colours the tier declares and to nothing else, so
             * every shade, dither step and ring below is built out of already-transformed colours
             * — one place to change the look rather than one per material.
             */
            fun of(tier: CelestialTier, skin: Skin = Skins.ORIGINAL): Palette {
                val primary = skin.apply(tier.primaryColor.toInt())
                val secondary = skin.apply(tier.secondaryColor.toInt())
                val glow = skin.apply(tier.glowColor.toInt())

                val accent = tier.accentColor?.let { skin.apply(it.toInt()) } ?: secondary
                val materials = when (tier.kind) {
                    BodyKind.TERRESTRIAL -> intArrayOf(primary, accent, mix(primary, WHITE, 0.75f))
                    BodyKind.GAS -> intArrayOf(primary, secondary, mix(accent, glow, 0.45f))
                    BodyKind.STAR -> intArrayOf(primary, mix(secondary, primary, 0.4f), WHITE)
                    BodyKind.EXOTIC -> intArrayOf(WHITE, primary, glow)
                    // Barely any spread: a white dwarf is one temperature all over, and giving
                    // it three distinct materials would paint continents onto degenerate matter.
                    BodyKind.REMNANT -> intArrayOf(
                        WHITE,
                        mix(WHITE, primary, 0.5f),
                        mix(primary, secondary, 0.35f),
                    )
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

            /**
             * Dark to light, shadows tinted cool and highlights pulled towards the glow. One
             * more step than the sprites used to have: at this resolution the bands between
             * shading levels are wide enough to be read as bands rather than as shape.
             */
            private fun rampOf(base: Int, glow: Int): IntArray {
                val steps = floatArrayOf(-0.62f, -0.42f, -0.20f, 0f, 0.24f, 0.48f)
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
     *
     * The map is sized against the sprite it will be wrapped around, at roughly one texel per
     * sprite pixel. Too coarse and the surface stays blocky while the sphere gets smooth; too
     * fine and features shrink below a pixel and turn into noise. Every feature is measured in
     * [detail] rather than in texels, so the composition holds at any resolution and only the
     * fineness changes.
     */
    private class Texture(
        private val width: Int,
        private val height: Int,
        private val materials: ByteArray,
        private val tones: FloatArray,
    ) {

        /** Texel under a surface point. Split from the reads so sampling allocates nothing. */
        fun texelAt(latitude: Float, longitude: Float): Int {
            val u = (longitude / TWO_PI).mod(1f)
            val v = ((latitude / PI_F) + 0.5f).coerceIn(0f, 0.999f)
            val tx = (u * width).toInt().coerceIn(0, width - 1)
            val ty = (v * height).toInt().coerceIn(0, height - 1)
            return ty * width + tx
        }

        fun materialAt(texel: Int): Int = materials[texel].toInt()

        fun toneAt(texel: Int): Float = tones[texel]

        companion object {

            fun of(tier: CelestialTier, spriteSize: Int): Texture {
                val width = ((spriteSize * TEXELS_PER_PIXEL).roundToInt() / 2) * 2
                val height = width / 2
                val detail = height / REFERENCE_HEIGHT
                val materials = ByteArray(width * height)
                val tones = FloatArray(width * height)
                val random = Random(tier.index * 6_151L + 17L)

                for (index in tones.indices) tones[index] = 0.5f

                val map = Canvas(width, height, materials, tones)
                when (tier.kind) {
                    BodyKind.ROCK -> craters(map, detail, random)
                    BodyKind.TERRESTRIAL -> continents(map, detail, random)
                    BodyKind.GAS -> bands(map, detail, random)
                    else -> mottle(map, detail, random)
                }
                return Texture(width, height, materials, tones)
            }

            /** The map being painted, so the builders below do not juggle four parameters each. */
            private class Canvas(
                val width: Int,
                val height: Int,
                val materials: ByteArray,
                val tones: FloatArray,
            ) {
                fun paint(x: Int, y: Int, material: Int, tone: Float) {
                    val index = y * width + x.mod(width)
                    materials[index] = material.toByte()
                    tones[index] = tone
                }
            }

            private fun craters(map: Canvas, detail: Float, random: Random) {
                // The big ones carry the composition and are the craters that were always there.
                repeat(22) {
                    crater(map, random, (2f + random.nextInt(4)) * detail)
                }
                // The small ones are what the extra resolution buys: pitting between the large
                // craters that simply had nowhere to live on a coarse map.
                repeat((26 * detail).roundToInt()) {
                    crater(map, random, 1.2f * detail + random.nextFloat() * detail)
                }
            }

            private fun crater(map: Canvas, random: Random, radius: Float) {
                val margin = (radius.toInt() + 1).coerceAtMost(map.height / 2 - 1)
                val cx = random.nextInt(map.width)
                val cy = margin + random.nextInt((map.height - 2 * margin).coerceAtLeast(1))
                val wall = (radius * 0.28f).coerceAtLeast(1f)
                val rim = radius + wall
                forEachTexel(map, cx, cy, rim.toInt() + 1) { x, y, distance ->
                    when {
                        distance <= radius - wall -> map.paint(x, y, 1, 0.28f)
                        distance <= rim -> map.paint(x, y, 2, 0.72f)
                    }
                }
            }

            /** Rim thickness: thin craters at low resolution, a real raised edge at high. */
            private fun detailStep(radius: Float): Float = (radius * 0.28f).coerceAtLeast(1f)

            private fun continents(map: Canvas, detail: Float, random: Random) {
                // Landmasses are built from a few overlapping lobes each, so a continent has a
                // coastline instead of being one circle — the extra resolution is spent on the
                // shape of the land rather than on scattering more dots across the ocean.
                repeat(11) {
                    val cx = random.nextInt(map.width)
                    val cy = map.height / 5 + random.nextInt(map.height * 3 / 5)
                    val scale = (5f + random.nextInt(5)) * detail
                    repeat(3 + random.nextInt(3)) {
                        val offset = scale * 0.7f
                        landmass(
                            map,
                            random,
                            scale * (0.55f + random.nextFloat() * 0.6f),
                            (cx + ((random.nextFloat() - 0.5f) * 2f * offset).toInt()).mod(map.width),
                            (cy + ((random.nextFloat() - 0.5f) * 2f * offset).toInt())
                                .coerceIn(2, map.height - 3),
                        )
                    }
                }
                // A handful of islands, enough to break up the open ocean without speckling it.
                repeat((4 * detail).roundToInt()) {
                    landmass(
                        map,
                        random,
                        1.2f * detail + random.nextFloat() * detail,
                        random.nextInt(map.width),
                        2 + random.nextInt(map.height - 4),
                    )
                }

                // Ice caps.
                for (y in 0 until map.height) {
                    val polar = abs(y - (map.height - 1) / 2f) / (map.height / 2f)
                    if (polar < 0.82f) continue
                    for (x in 0 until map.width) map.paint(x, y, 2, 0.8f)
                }
            }

            private fun landmass(map: Canvas, random: Random, radius: Float, cx: Int, cy: Int) {
                // A wobbly edge reads as coastline instead of a circle; the wobble is a fraction
                // of the radius so a big continent is not merely a scaled up island.
                val wobble = radius * 0.3f
                forEachTexel(map, cx, cy, radius.toInt() + 1) { x, y, distance ->
                    if (distance < radius - random.nextFloat() * wobble) {
                        map.paint(x, y, 1, 0.45f + random.nextFloat() * 0.3f)
                    }
                }
            }

            private fun bands(map: Canvas, detail: Float, random: Random) {
                val bandTone = FloatArray(map.height)
                val bandMaterial = ByteArray(map.height)
                var y = 0
                while (y < map.height) {
                    val height = ((2 + random.nextInt(4)) * detail).roundToInt().coerceAtLeast(1)
                    val material = if (random.nextFloat() < 0.45f) 1.toByte() else 0.toByte()
                    val tone = 0.3f + random.nextFloat() * 0.5f
                    for (offset in 0 until height) {
                        val row = y + offset
                        if (row >= map.height) break
                        bandMaterial[row] = material
                        bandTone[row] = tone
                    }
                    y += height
                }
                for (row in 0 until map.height) {
                    for (x in 0 until map.width) {
                        // Waviness so the bands are not perfectly straight lines. Both the
                        // wavelength and the amplitude are in texels, so they follow the map.
                        val wobble = (sin(x * 0.22f / detail + row / detail) * 1.4f * detail).toInt()
                        val source = (row + wobble).coerceIn(0, map.height - 1)
                        map.paint(x, row, bandMaterial[source].toInt(), bandTone[source])
                    }
                }

                // The one big storm every gas giant deserves — an oval, stretched with the bands.
                storm(map, random, 7f * detail, 3f * detail, 2, 0.72f)
                // Smaller eddies trailing in the same latitudes.
                repeat((5 * detail).roundToInt()) {
                    storm(map, random, 1.5f * detail, 0.8f * detail, 2, 0.66f)
                }
            }

            private fun storm(
                map: Canvas,
                random: Random,
                halfWidth: Float,
                halfHeight: Float,
                material: Int,
                tone: Float,
            ) {
                val cx = random.nextInt(map.width)
                val spread = (8 * halfHeight).toInt().coerceAtLeast(2)
                val cy = map.height / 2 + random.nextInt(spread) - spread / 2
                val h = halfHeight.toInt().coerceAtLeast(1)
                val w = halfWidth.toInt().coerceAtLeast(1)
                for (dy in -h..h) {
                    val y = cy + dy
                    if (y !in 0 until map.height) continue
                    for (dx in -w..w) {
                        val fx = dx.toFloat() / w
                        val fy = dy.toFloat() / h
                        if (fx * fx + fy * fy > 1f) continue
                        map.paint(cx + dx, y, material, tone)
                    }
                }
            }

            private fun mottle(map: Canvas, detail: Float, random: Random) {
                // Granulation, not noise. A random draw per texel — or per square cell — turns
                // into television static as soon as the resolution goes up, because nothing in
                // it has a shape. Convection cells do: bright grains with darker lanes between
                // them. So the surface is laid down as overlapping round grains instead.
                // The base is the darker material: it survives only as the lanes between grains,
                // the way the gaps between convection cells are the dark part of a star.
                for (index in map.tones.indices) {
                    map.materials[index] = 1
                    map.tones[index] = 0.28f + random.nextFloat() * 0.08f
                }

                val area = map.width * map.height
                val grainRadius = 1.6f * detail
                // Enough grains to cover the surface several times over, so what is left of the
                // base reads as thin lanes rather than as blotches.
                val grains = (area / (grainRadius * grainRadius) * 0.5f).roundToInt()
                repeat(grains.coerceAtLeast(48)) {
                    val radius = grainRadius * (0.6f + random.nextFloat() * 0.8f)
                    val cx = random.nextInt(map.width)
                    val cy = random.nextInt(map.height)
                    val bright = random.nextFloat()
                    val material = if (bright > 0.9f) 2 else 0
                    val tone = 0.6f + bright * 0.35f
                    forEachTexel(map, cx, cy, radius.toInt() + 1) { x, y, distance ->
                        // Softer at the edge, so grains merge into each other rather than
                        // stamping hard circles.
                        if (distance < radius * (0.75f + random.nextFloat() * 0.25f)) {
                            map.paint(x, y, material, tone)
                        }
                    }
                }
            }

            /** Walks a disc in texture space, wrapping around the seam in longitude. */
            private inline fun forEachTexel(
                map: Canvas,
                centreX: Int,
                centreY: Int,
                radius: Int,
                block: (x: Int, y: Int, distance: Float) -> Unit,
            ) {
                for (dy in -radius..radius) {
                    val y = centreY + dy
                    if (y !in 0 until map.height) continue
                    for (dx in -radius..radius) {
                        val distance = sqrt((dx * dx + dy * dy).toFloat())
                        if (distance > radius) continue
                        block((centreX + dx).mod(map.width), y, distance)
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

    /**
     * Ordered dithering, on an 8 by 8 matrix rather than 4 by 4. The finer grid has 64 thresholds
     * instead of 16, which is what keeps a shading transition looking like a gradient of dots at
     * this resolution instead of a repeating checkerboard.
     */
    private fun bayer(x: Int, y: Int): Float = BAYER[(y and 7) * 8 + (x and 7)] * (1f / 64f)

    private const val LEVELS = 6
    private const val DITHER_STRENGTH = 0.9f
    /** Sprite sizes snap to this grid so a scaled sprite never lands on a half pixel. */
    private const val GRID = 8
    private const val MIN_SIZE = 64
    /** Texels per sprite pixel across the visible face — about one, so features stay hand placed. */
    private const val TEXELS_PER_PIXEL = 1.25f
    /** The map height the feature sizes were originally tuned against. */
    private const val REFERENCE_HEIGHT = 48f
    private const val BODY_FRACTION = 0.62f
    private const val RING_OUTER = 1.62f
    private const val RING_INNER = 0.72f
    private const val RING_FLATTEN = 0.30f
    private const val DISK_INNER = 0.55f
    /** Width of the lensing arc, measured against a 96 pixel sprite and scaled from there. */
    private const val LENS_THICKNESS = 2.2f
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
        0, 32, 8, 40, 2, 34, 10, 42,
        48, 16, 56, 24, 50, 18, 58, 26,
        12, 44, 4, 36, 14, 46, 6, 38,
        60, 28, 52, 20, 62, 30, 54, 22,
        3, 35, 11, 43, 1, 33, 9, 41,
        51, 19, 59, 27, 49, 17, 57, 25,
        15, 47, 7, 39, 13, 45, 5, 37,
        63, 31, 55, 23, 61, 29, 53, 21,
    )
}
