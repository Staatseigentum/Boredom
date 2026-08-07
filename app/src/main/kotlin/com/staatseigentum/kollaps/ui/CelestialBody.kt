package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The body the player taps, drawn as a pixel art sprite.
 *
 * The sprite sheet comes out of [PixelPlanet] as raw pixel buffers and is only turned into
 * bitmaps here. Scaling uses [FilterQuality.None] and a whole-number factor, so every sprite
 * pixel lands on an exact block of screen pixels and nothing goes soft at the edges.
 */
@Composable
fun CelestialBody(
    tier: CelestialTier,
    modifier: Modifier = Modifier,
) {
    val factory = LocalSpriteFactory.current

    // Keyed on the tier, which is the whole point: this used to be a produceState, whose backing
    // remember carries no key, so on a tier change the state kept the previous body's sheet and
    // the producer — seeing a non-null value — never loaded the new one.
    var sheet by remember(tier.index) { mutableStateOf(SpriteCache.ready(tier)) }
    LaunchedEffect(tier.index) {
        // Rendering costs tens of milliseconds, so it happens off the main thread and the body
        // appears once it is ready. The cache matters: the tap area and the tier celebration are
        // two composables showing the same body at the same moment.
        if (sheet == null) sheet = SpriteCache.sheet(tier, factory)
    }

    val transition = rememberInfiniteTransition(label = "body-${tier.index}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(PixelPlanet.spinMillis(tier.kind), easing = LinearEasing),
        ),
        label = "spin",
    )

    Canvas(modifier = modifier) {
        val current = sheet ?: return@Canvas
        // The edge length comes off the sheet rather than from the tier, so the source rectangle
        // can never be a different size than the bitmap it is read from.
        val side = current.side
        val index = (phase * PixelPlanet.FRAMES).toInt().coerceIn(0, current.frames.lastIndex)

        val available = min(size.width, size.height) * PixelPlanet.spriteFraction(tier)
        // Whole-number scaling is what keeps the pixels square.
        val scale = (available / side).toInt().coerceAtLeast(1)
        val drawn = side * scale

        drawImage(
            image = current.frames[index],
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(side, side),
            dstOffset = IntOffset(
                ((size.width - drawn) / 2f).roundToInt(),
                ((size.height - drawn) / 2f).roundToInt(),
            ),
            dstSize = IntSize(drawn, drawn),
            filterQuality = FilterQuality.None,
        )
    }
}
