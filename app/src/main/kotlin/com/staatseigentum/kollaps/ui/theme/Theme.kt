package com.staatseigentum.kollaps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
 * The capitals face, for the counter, the headings and the buttons.
 *
 * Reached through a composition local rather than a top level value because the font files are
 * loaded differently on each platform — from Android resources in the app, from the classpath in
 * the desktop harness — and everything else about the interface is identical.
 */
val LocalDisplayFont = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

/**
 * Two pixel faces rather than one, because the game asks two different things of its text.
 *
 * The display face is Silkscreen: capitals only on a five pixel grid, which is exactly right for
 * short, loud strings that the code already uppercases. Set a paragraph of German in it and it
 * shouts. The text face is VT323, which has real lowercase and stays readable at the sizes the
 * flavour texts and shop rows need. Point sizes run higher than they did for the system
 * monospace: both faces draw small for their nominal size.
 */
fun kollapsTypography(display: FontFamily, text: FontFamily) = Typography(
    displayMedium = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = text,
        fontSize = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = text,
        fontSize = 17.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = text,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun KollapsTheme(
    display: FontFamily,
    text: FontFamily,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDisplayFont provides display) {
        MaterialTheme(
            colorScheme = KollapsColors,
            typography = kollapsTypography(display, text),
            content = content,
        )
    }
}
