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
    outline = Color(0xFF2E3564),
)

/** The mass counter uses monospaced digits so it does not jitter while it counts up. */
private val KollapsTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
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
