package com.staatseigentum.kollaps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.staatseigentum.kollaps.R

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
 * Two pixel faces rather than one, because the game asks two different things of its text.
 *
 * [PixelDisplay] is Silkscreen: a capitals-only face on a five pixel grid, which is exactly right
 * for the counter, the headings and the buttons — short, loud, and already uppercase in the code.
 * Set a paragraph of German in it and it shouts.
 *
 * [PixelText] is VT323, which has real lowercase and stays readable at the sizes the flavour
 * texts and shop rows need. Point sizes run higher than they did for the system monospace: both
 * faces draw small for their nominal size, so the numbers below are chosen to end up at the same
 * physical size on screen as before.
 */
val PixelDisplay = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold),
)

val PixelText = FontFamily(Font(R.font.vt323_regular))

private val KollapsTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = PixelDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PixelDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PixelText,
        fontSize = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PixelText,
        fontSize = 17.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PixelText,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PixelDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
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
