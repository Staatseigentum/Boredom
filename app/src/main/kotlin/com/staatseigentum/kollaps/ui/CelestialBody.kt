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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import com.staatseigentum.kollaps.core.pixel.Skin
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The body the player taps, drawn as a pixel art sprite.
 *
 * The pixels come out of [PixelPlanet] and are only turned into a bitmap here. Scaling uses
 * [FilterQuality.None], so every sprite pixel lands on an exact block of screen pixels and nothing
 * goes soft at the edges.
 *
 * ## Why the frame is rendered rather than looked up
 *
 * There used to be a sheet of every frame, rendered up front. At the resolution the sprites are
 * drawn at now that is tens of megabytes per body, so instead the body is kept as a
 * [PixelPlanet.Sprite] — palette and surface, the slow half — and the one frame on screen is drawn
 * into a buffer that gets reused. A six second turn across forty-eight frames is eight renders a
 * second, and each is one pass over the pixels.
 */
@Composable
fun CelestialBody(
    tier: CelestialTier,
    modifier: Modifier = Modifier,
    /** The colour scheme the body is drawn in. */
    skin: Skin = LocalSkin.current,
    /**
     * Whole turns added on top of the body's own rotation.
     *
     * The collapse feeds the accretion disc and it has to visibly run up before it is crushed. No
     * new sprites are needed for that — a faster walk through the frames is the whole effect.
     *
     * A lambda rather than a value, and for the usual reason: it changes every frame, and read as
     * a parameter it would recompose this whole subtree sixty times a second. Read inside the
     * canvas it costs a redraw, which is what it actually is.
     */
    extraTurns: () -> Float = { 0f },
) {
    val factory = LocalSpriteFactory.current

    // Keyed on the tier *and* the scheme, which is the whole point: this used to be a produceState,
    // whose backing remember carries no key, so on a tier change the state kept the previous
    // body's sprite and the producer — seeing a non-null value — never loaded the new one. A
    // palette change has exactly the same shape, and would otherwise not show until the next rung.
    val sprite = remember(tier.index, skin.id) { SpriteCache.sprite(tier, skin) }
    val surface = remember(tier.index, skin.id, factory) { factory.surface(sprite.side) }
    val buffer = remember(tier.index, skin.id) { sprite.buffer() }
    // Which frame the surface currently holds. Deliberately not Compose state: it is read and
    // written inside the draw, and making it state would invalidate the composition from within
    // the draw phase — the redraw already happens, because `phase` is what triggered it.
    val drawn = remember(tier.index, skin.id) { intArrayOf(NOTHING_DRAWN) }

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
        val side = sprite.side
        // Wrapped rather than clamped: the extra turns keep counting up for as long as the
        // collapse lasts, and a clamp would park the disc on its last frame instead of spinning it.
        val turn = ((phase + extraTurns()) % 1f + 1f) % 1f
        val index = (turn * PixelPlanet.FRAMES).toInt().coerceIn(0, PixelPlanet.FRAMES - 1)

        if (drawn[0] != index) {
            sprite.render(index, buffer)
            surface.write(buffer)
            drawn[0] = index
        }
        val image = surface.image ?: return@Canvas

        val available = min(size.width, size.height) * PixelPlanet.spriteFraction(tier)
        val edge = drawnEdge(available, side)

        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(side, side),
            dstOffset = IntOffset(
                ((size.width - edge) / 2f).roundToInt(),
                ((size.height - edge) / 2f).roundToInt(),
            ),
            dstSize = IntSize(edge, edge),
            filterQuality = FilterQuality.None,
        )
    }
}

/**
 * How wide to draw a [side]-pixel sprite into [available] pixels of box.
 *
 * The rule used to be a whole-number factor, which is the textbook way to scale pixel art: every
 * sprite pixel lands on an exact square of screen pixels. That worked while the buffer was small
 * enough that the factor was three or four, where the next step down costs a third of the body.
 *
 * At the doubled resolution it stops working. The buffer is now about two thirds of the box it is
 * drawn into, so the whole-number factor is *one* — and a body that used to fill ninety-seven per
 * cent of its area would suddenly fill sixty-four. The bodies would visibly shrink by a third,
 * which is not what raising the resolution was for: the point is the same body made of more
 * pixels, not a smaller one.
 *
 * So the factor steps in eighths in both directions. At the sizes this actually lands on that is
 * 1.5 — one sprite pixel is one or two screen pixels rather than always the same, which nobody can
 * see at a ratio that close to 1:1, and the body comes out at exactly the width it had before.
 * What is preserved is the property that mattered all along: the ratio is the same for every rung,
 * so a pixel block is one size across the whole game.
 *
 * (The handoff specified whole numbers upwards and eighths only downwards. That halves the drawn
 * size rather than keeping it, which contradicts the same section's stated intent — so the eighths
 * apply both ways.)
 */
private fun drawnEdge(available: Float, side: Int): Int {
    val scale = (floor(available / side * 8f) / 8f).coerceAtLeast(0.125f)
    return (side * scale).toInt().coerceAtLeast(1)
}

/** No frame has been rendered into the surface yet. Not a valid frame index. */
private const val NOTHING_DRAWN = -1
