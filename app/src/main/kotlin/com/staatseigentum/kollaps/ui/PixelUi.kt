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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                onClick = onClick,
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
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = size.sp,
        letterSpacing = 1.sp,
    )
}

/** A progress bar made of discrete blocks rather than a smooth sweep. */
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
        val gap = size.width / cells * 0.22f
        val cellWidth = (size.width - gap * (cells - 1)) / cells
        val filled = (progress.coerceIn(0f, 1f) * cells).toInt()
        for (index in 0 until cells) {
            drawRect(
                color = if (index < filled) color else track,
                topLeft = Offset(index * (cellWidth + gap), 0f),
                size = Size(cellWidth, size.height),
            )
        }
    }
}

private val BORDER = 2.dp
