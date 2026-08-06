package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import com.staatseigentum.kollaps.core.BodyKind
import com.staatseigentum.kollaps.core.CelestialTier
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws the body the player taps. Everything is generated from the tier definition — colours,
 * craters, cloud bands, the accretion disk — so there is not a single image asset in the app and
 * every step of the ladder looks distinct.
 */
@Composable
fun CelestialBody(
    tier: CelestialTier,
    modifier: Modifier = Modifier,
) {
    val surface = remember(tier.index) { SurfaceFeatures.of(tier) }

    val transition = rememberInfiniteTransition(label = "body-${tier.index}")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(spinMillis(tier), easing = LinearEasing)),
        label = "spin",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(2_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f * tier.relativeSize
        // A layout pass can hand us a zero sized canvas, and a gradient with radius zero throws.
        if (radius <= 0f) return@Canvas
        drawTier(tier, surface, center, radius, spin, pulse)
    }
}

// ---------------------------------------------------------------------- generated surfaces

private data class Blob(val lat: Float, val lon: Float, val size: Float, val tone: Float)

private data class Band(val offset: Float, val thickness: Float, val tone: Float)

private class SurfaceFeatures(
    val blobs: List<Blob>,
    val bands: List<Band>,
) {
    companion object {
        fun of(tier: CelestialTier): SurfaceFeatures {
            val random = Random(tier.index * 7_919L + 13L)
            val blobCount = when (tier.kind) {
                BodyKind.ROCK -> 16
                BodyKind.TERRESTRIAL -> 22
                BodyKind.GAS -> 4
                BodyKind.STAR -> 26
                else -> 0
            }
            val blobs = List(blobCount) {
                Blob(
                    lat = (random.nextFloat() - 0.5f) * 2.4f,
                    lon = random.nextFloat() * TWO_PI,
                    size = 0.08f + random.nextFloat() * 0.24f,
                    tone = random.nextFloat(),
                )
            }
            val bands = if (tier.kind == BodyKind.GAS) {
                List(9) { index ->
                    Band(
                        offset = -0.85f + index * 0.21f,
                        thickness = 0.07f + random.nextFloat() * 0.09f,
                        tone = random.nextFloat(),
                    )
                }
            } else {
                emptyList()
            }
            return SurfaceFeatures(blobs, bands)
        }
    }
}

// ---------------------------------------------------------------------- drawing

private fun DrawScope.drawTier(
    tier: CelestialTier,
    surface: SurfaceFeatures,
    center: Offset,
    radius: Float,
    spin: Float,
    pulse: Float,
) {
    val primary = Color(tier.primaryColor)
    val secondary = Color(tier.secondaryColor)
    val glow = Color(tier.glowColor)

    when (tier.kind) {
        BodyKind.SINGULARITY -> {
            drawBlackHole(center, radius, glow, secondary, spin, pulse)
            return
        }

        BodyKind.EXOTIC -> {
            drawNeutronStar(center, radius, primary, glow, spin, pulse)
            return
        }

        else -> Unit
    }

    val isStar = tier.kind == BodyKind.STAR
    val haloRadius = radius * if (isStar) 2.4f * pulse else 1.7f
    drawHalo(center, haloRadius, glow, if (isStar) 1f else 0.45f)

    if (tier.hasRing) drawRing(center, radius, glow, secondary, front = false)

    val light = Offset(center.x - radius * 0.34f, center.y - radius * 0.36f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to lerp(primary, Color.White, if (isStar) 0.55f else 0.28f),
            0.55f to primary,
            1f to secondary,
            center = light,
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )

    clipPath(circlePath(center, radius)) {
        when (tier.kind) {
            BodyKind.ROCK -> drawCraters(surface, center, radius, spin, secondary)
            BodyKind.TERRESTRIAL -> drawContinents(surface, center, radius, spin, primary, secondary)
            BodyKind.GAS -> drawBands(surface, center, radius, spin, primary, secondary)
            BodyKind.STAR -> drawGranulation(surface, center, radius, spin, glow)
            else -> Unit
        }
    }

    if (!isStar) {
        // Terminator: the far side of the sphere falls into shadow.
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.58f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.6f),
                center = light,
                radius = radius * 1.55f,
            ),
            radius = radius,
            center = center,
        )
    }

    drawCircle(
        color = glow.copy(alpha = if (isStar) 0.7f else 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.05f),
    )

    if (isStar) drawFlares(center, radius, glow, spin, pulse)
    if (tier.hasRing) drawRing(center, radius, glow, secondary, front = true)
}

private fun DrawScope.drawHalo(center: Offset, radius: Float, color: Color, intensity: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = 0.34f * intensity),
            0.35f to color.copy(alpha = 0.14f * intensity),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawCraters(
    surface: SurfaceFeatures,
    center: Offset,
    radius: Float,
    spin: Float,
    shadow: Color,
) {
    for (blob in surface.blobs) {
        val point = project(blob.lat, blob.lon + spin * TWO_PI, radius, center) ?: continue
        val scale = point.depth * (1f - 0.25f * blob.tone)
        val size = radius * blob.size * scale
        if (size < 1f) continue
        drawCircle(
            color = shadow.copy(alpha = 0.32f + 0.2f * blob.tone),
            radius = size,
            center = point.position,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = size,
            center = point.position.copy(
                x = point.position.x - size * 0.18f,
                y = point.position.y - size * 0.18f,
            ),
        )
    }
}

private fun DrawScope.drawContinents(
    surface: SurfaceFeatures,
    center: Offset,
    radius: Float,
    spin: Float,
    primary: Color,
    secondary: Color,
) {
    val land = lerp(secondary, Color(0xFF2F7A3A), 0.45f)
    for (blob in surface.blobs) {
        val point = project(blob.lat, blob.lon + spin * TWO_PI, radius, center) ?: continue
        val size = radius * blob.size * point.depth
        if (size < 1f) continue
        drawCircle(
            color = land.copy(alpha = 0.55f + 0.25f * blob.tone),
            radius = size,
            center = point.position,
        )
    }
    // Polar caps.
    val capColor = lerp(primary, Color.White, 0.7f).copy(alpha = 0.5f)
    drawOvalAt(center.copy(y = center.y - radius * 0.92f), radius * 1.1f, radius * 0.42f, capColor)
    drawOvalAt(center.copy(y = center.y + radius * 0.92f), radius * 1.1f, radius * 0.42f, capColor)
}

private fun DrawScope.drawBands(
    surface: SurfaceFeatures,
    center: Offset,
    radius: Float,
    spin: Float,
    primary: Color,
    secondary: Color,
) {
    for (band in surface.bands) {
        val y = center.y + band.offset * radius
        val half = radius * band.thickness
        val shade = if (band.tone > 0.5f) {
            lerp(primary, Color.White, 0.22f * band.tone)
        } else {
            lerp(secondary, Color.Black, 0.18f)
        }
        drawRect(
            color = shade.copy(alpha = 0.55f),
            topLeft = Offset(center.x - radius, y - half),
            size = Size(radius * 2f, half * 2f),
        )
    }
    // One big storm that travels with the rotation.
    val storm = surface.blobs.firstOrNull() ?: return
    val point = project(storm.lat * 0.5f, storm.lon + spin * TWO_PI, radius, center) ?: return
    drawOvalAt(
        point.position,
        radius * 0.42f * point.depth,
        radius * 0.20f * point.depth,
        lerp(secondary, Color(0xFFD1462F), 0.55f).copy(alpha = 0.8f),
    )
}

private fun DrawScope.drawGranulation(
    surface: SurfaceFeatures,
    center: Offset,
    radius: Float,
    spin: Float,
    glow: Color,
) {
    for (blob in surface.blobs) {
        val point = project(blob.lat, blob.lon + spin * TWO_PI, radius, center) ?: continue
        val size = radius * blob.size * point.depth
        if (size < 1f) continue
        drawCircle(
            brush = Brush.radialGradient(
                0f to glow.copy(alpha = 0.35f + 0.3f * blob.tone),
                1f to Color.Transparent,
                center = point.position,
                radius = size,
            ),
            radius = size,
            center = point.position,
        )
    }
}

private fun DrawScope.drawFlares(
    center: Offset,
    radius: Float,
    glow: Color,
    spin: Float,
    pulse: Float,
) {
    val diameter = radius * 2.2f * pulse
    val topLeft = Offset(center.x - diameter / 2f, center.y - diameter / 2f)
    rotate(degrees = spin * 360f, pivot = center) {
        for (index in 0 until 5) {
            drawArc(
                color = glow.copy(alpha = 0.20f),
                startAngle = index * 72f,
                sweepAngle = 34f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = radius * 0.09f),
            )
        }
    }
}

private fun DrawScope.drawRing(
    center: Offset,
    radius: Float,
    glow: Color,
    secondary: Color,
    front: Boolean,
) {
    val width = radius * 3.1f
    val height = radius * 0.9f
    val topLeft = Offset(center.x - width / 2f, center.y - height / 2f)
    rotate(degrees = -16f, pivot = center) {
        for (pass in 0 until 3) {
            val inset = pass * radius * 0.22f
            drawArc(
                color = lerp(glow, secondary, pass / 3f).copy(alpha = if (front) 0.75f else 0.4f),
                startAngle = if (front) 0f else 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(topLeft.x + inset, topLeft.y + inset * height / width),
                size = Size(width - inset * 2f, height - inset * 2f * height / width),
                style = Stroke(width = radius * 0.075f),
            )
        }
    }
}

private fun DrawScope.drawNeutronStar(
    center: Offset,
    radius: Float,
    primary: Color,
    glow: Color,
    spin: Float,
    pulse: Float,
) {
    drawHalo(center, radius * 4.5f * pulse, glow, 0.9f)

    // Two relativistic jets sweeping around like a lighthouse.
    rotate(degrees = spin * 360f, pivot = center) {
        for (direction in listOf(-1f, 1f)) {
            val tip = Offset(center.x, center.y + direction * radius * 4.5f)
            drawPath(
                path = Path().apply {
                    moveTo(center.x - radius * 0.5f, center.y)
                    lineTo(center.x + radius * 0.5f, center.y)
                    lineTo(tip.x + radius * 1.6f, tip.y)
                    lineTo(tip.x - radius * 1.6f, tip.y)
                    close()
                },
                brush = Brush.radialGradient(
                    0f to glow.copy(alpha = 0.55f),
                    1f to Color.Transparent,
                    center = center,
                    radius = radius * 4.5f,
                ),
            )
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White,
            0.6f to primary,
            1f to glow.copy(alpha = 0.4f),
            center = center,
            radius = radius * 1.4f,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawBlackHole(
    center: Offset,
    radius: Float,
    glow: Color,
    ember: Color,
    spin: Float,
    pulse: Float,
) {
    val diskColors = listOf(
        ember.copy(alpha = 0.15f),
        glow,
        Color.White,
        glow,
        ember.copy(alpha = 0.55f),
        ember.copy(alpha = 0.15f),
        ember.copy(alpha = 0.15f),
    )

    drawHalo(center, radius * 2.6f * pulse, glow, 0.7f)

    // The disk, seen almost edge on: a flattened ring that keeps turning.
    scale(scaleX = 1f, scaleY = 0.34f, pivot = center) {
        rotate(degrees = spin * 360f, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(colors = diskColors, center = center),
                radius = radius * 1.35f,
                center = center,
                style = Stroke(width = radius * 0.62f),
            )
        }
    }

    // Event horizon. Drawn over the disk so its near half disappears behind the hole.
    drawCircle(color = Color.Black, radius = radius * 0.62f, center = center)

    // Light from the far side of the disk, bent up and over the horizon.
    val lensWidth = radius * 2.5f
    val lensHeight = radius * 1.5f
    drawArc(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to glow.copy(alpha = 0.9f),
            1f to Color.Transparent,
            startX = center.x - lensWidth / 2f,
            endX = center.x + lensWidth / 2f,
        ),
        startAngle = 185f,
        sweepAngle = 170f,
        useCenter = false,
        topLeft = Offset(center.x - lensWidth / 2f, center.y - lensHeight / 2f),
        size = Size(lensWidth, lensHeight),
        style = Stroke(width = radius * 0.13f),
    )

    // Photon ring hugging the horizon.
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = radius * 0.66f,
        center = center,
        style = Stroke(width = radius * 0.035f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            0.82f to Color.Transparent,
            1f to glow.copy(alpha = 0.6f),
            center = center,
            radius = radius * 0.9f,
        ),
        radius = radius * 0.9f,
        center = center,
    )
}

// ---------------------------------------------------------------------- helpers

private class Projected(val position: Offset, val depth: Float)

/** Maps a latitude/longitude on the sphere to the screen, or `null` on the hidden side. */
private fun project(lat: Float, lon: Float, radius: Float, center: Offset): Projected? {
    val cosLat = cos(lat)
    val z = cosLat * cos(lon)
    if (z <= 0.05f) return null
    val x = cosLat * sin(lon)
    val y = sin(lat)
    return Projected(
        position = Offset(center.x + x * radius, center.y - y * radius),
        depth = z,
    )
}

private fun circlePath(center: Offset, radius: Float): Path = Path().apply {
    addOval(
        Rect(
            offset = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
        ),
    )
}

private fun DrawScope.drawOvalAt(center: Offset, halfWidth: Float, halfHeight: Float, color: Color) {
    drawOval(
        color = color,
        topLeft = Offset(center.x - halfWidth, center.y - halfHeight),
        size = Size(halfWidth * 2f, halfHeight * 2f),
    )
}

private fun spinMillis(tier: CelestialTier): Int = when (tier.kind) {
    BodyKind.ROCK -> 26_000
    BodyKind.TERRESTRIAL -> 30_000
    BodyKind.GAS -> 20_000
    BodyKind.STAR -> 40_000
    BodyKind.EXOTIC -> 2_400
    BodyKind.SINGULARITY -> 9_000
}

private const val TWO_PI = 6.2831855f
