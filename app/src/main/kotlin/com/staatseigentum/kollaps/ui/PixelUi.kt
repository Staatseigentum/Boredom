package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import com.staatseigentum.kollaps.ui.theme.PixelDisplay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.round
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The building blocks of the interface, in the same visual language as the sprites: square
 * corners, hard two pixel borders, no gradients and no soft shadows. Anything rounded would
 * fight the pixel art.
 */

/** A bordered box. The panel every list row, card and dialog body is built from. */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    background: Color = SpaceCard,
    border: Color = Outline,
    padding: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(background)
            .border(BORDER, border, RectangleShape)
            .padding(padding.dp),
        content = content,
    )
}

/** A chunky square button that inverts while held instead of rippling. */
@Composable
fun PixelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Nebula,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sfx = LocalSfx.current

    Box(
        modifier = modifier
            .background(
                when {
                    !enabled -> SpaceCard
                    pressed -> accent
                    else -> SpaceElevated
                },
            )
            .border(BORDER, if (enabled) accent else Outline, RectangleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = {
                    sfx?.click()
                    onClick()
                },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        PixelLabel(
            text = label,
            color = if (!enabled) Muted else Starlight,
        )
    }
}

/** Uppercase monospace with wide tracking — the retro interface voice. */
@Composable
fun PixelLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Starlight,
    size: Int = 13,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = PixelDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = size.sp,
        letterSpacing = 0.5.sp,
    )
}

/**
 * A progress bar built the way a sprite artist would draw one: a hard frame, a recessed track,
 * and discrete cells with a lit top edge and a shaded bottom edge so each block reads as a solid
 * object rather than a coloured rectangle.
 *
 * The leading cell is drawn dimmed rather than either full or empty. Without it a bar of two
 * dozen blocks jumps in visible steps, and the tier bar in particular spends minutes on a single
 * block near the end of a run.
 */
@Composable
fun PixelBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color = SpaceCard,
    cells: Int = 24,
) {
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        // One whole device pixel is the unit everything snaps to, so no edge lands on a half.
        // Capped against the height as well: on a short bar a frame sized purely by density
        // would swallow the fill it is supposed to surround.
        val unit = floor(density).coerceIn(1f, floor(size.height / 6f).coerceAtLeast(1f))
        val frame = unit * 2f
        if (size.width <= frame * 2f || size.height <= frame * 2f) return@Canvas

        drawRect(color = Outline, size = size)
        val innerLeft = frame
        val innerTop = frame
        val innerWidth = size.width - frame * 2f
        val innerHeight = size.height - frame * 2f
        drawRect(
            color = SpaceElevated,
            topLeft = Offset(innerLeft, innerTop),
            size = Size(innerWidth, innerHeight),
        )

        val exact = progress.coerceIn(0f, 1f) * cells
        val full = floor(exact).toInt()
        val leading = exact - full

        val step = innerWidth / cells
        val highlight = lerp(color, Color.White, 0.4f)
        val shade = lerp(color, Color.Black, 0.35f)

        for (index in 0 until cells) {
            val left = innerLeft + round(index * step)
            val right = innerLeft + round((index + 1) * step) - unit
            val width = right - left
            if (width <= 0f) continue

            val fill = when {
                index < full -> color
                // Anything on the leading block at all lights it, faintly — a bar that shows
                // nothing until a whole cell is earned looks stuck.
                index == full && leading > 0.05f -> lerp(track, color, 0.25f + leading * 0.5f)
                else -> track
            }

            drawRect(color = fill, topLeft = Offset(left, innerTop), size = Size(width, innerHeight))

            if (index < full && innerHeight > unit * 2f) {
                drawRect(
                    color = highlight,
                    topLeft = Offset(left, innerTop),
                    size = Size(width, unit),
                )
                drawRect(
                    color = shade,
                    topLeft = Offset(left, innerTop + innerHeight - unit),
                    size = Size(width, unit),
                )
            }
        }
    }
}

private val BORDER = 2.dp
