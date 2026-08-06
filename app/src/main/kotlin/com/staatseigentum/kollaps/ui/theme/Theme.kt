package com.staatseigentum.kollaps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Space = Color(0xFF05060F)
val SpaceElevated = Color(0xFF0E1226)
val SpaceCard = Color(0xFF161B36)
val Nebula = Color(0xFF7C5CFF)
val Ember = Color(0xFFFFB74D)
val Starlight = Color(0xFFE9ECFF)
val Muted = Color(0xFF8E95C4)
val Positive = Color(0xFF5CE1A6)
val Outline = Color(0xFF2E3564)

private val KollapsColors = darkColorScheme(
    primary = Nebula,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2260),
    onPrimaryContainer = Starlight,
    secondary = Ember,
    onSecondary = Color(0xFF2A1800),
    tertiary = Positive,
    background = Space,
    onBackground = Starlight,
    surface = SpaceElevated,
    onSurface = Starlight,
    surfaceVariant = SpaceCard,
    onSurfaceVariant = Muted,
    outline = Outline,
)

/**
 * Everything is monospaced. It keeps the counter from jittering while it ticks up, and it is the
 * closest a system font gets to the pixel look of the sprites without shipping a font file.
 */
private val KollapsTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
    ),
)

@Composable
fun KollapsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KollapsColors,
        typography = KollapsTypography,
        content = content,
    )
}
