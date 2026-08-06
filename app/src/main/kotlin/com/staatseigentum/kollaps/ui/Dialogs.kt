package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/** Shown once on return: what the collectors brought in while the app was closed. */
@Composable
fun OfflineDialog(report: OfflineReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceElevated,
        shape = RectangleShape,
        title = { PixelLabel("Willkommen zurück", color = Starlight, size = 16) },
        text = {
            Column {
                Text(
                    text = "Du warst ${Numbers.formatDuration(report.seconds)} weg.",
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "+${Numbers.formatMass(report.gained)}",
                    style = MaterialTheme.typography.displayMedium,
                    color = Ember,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Deine Kollektoren haben ohne dich weitergemacht.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        },
        confirmButton = {
            PixelButton(label = "Weiter geht's", onClick = onDismiss, accent = Ember)
        },
    )
}

/** Full screen moment when the player's body evolves into the next one. */
@Composable
fun TierCelebration(tier: CelestialTier, onDismiss: () -> Unit) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(tier.index) {
        entrance.snapTo(0f)
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    val interaction = remember { MutableInteractionSource() }
    // The overlay lands mid-tapping, so this tap clicks like every other one — a silent gap here
    // would read as a dropped input.
    val sfx = LocalSfx.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    sfx?.click()
                    onDismiss()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = entrance.value
                    scaleY = entrance.value
                    alpha = entrance.value.coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (tier.isFinal) "ENDGAME" else "NEUE STUFE",
                style = MaterialTheme.typography.labelLarge,
                color = Muted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tier.name,
                style = MaterialTheme.typography.displayMedium,
                color = Color(tier.glowColor),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            CelestialBody(
                tier = tier,
                modifier = Modifier.size(220.dp),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = tier.flavor,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Produktion jetzt ${Numbers.formatMultiplier(tier.productionMultiplier)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Ember,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (tier.isFinal) {
                    "Du hast die Leiter zu Ende geklettert. Im Reiter Kosmos kannst du " +
                        "kollabieren und mit Singularitäten neu anfangen."
                } else {
                    "Tippen zum Weitermachen · noch ${Tiers.all.size - tier.index - 1} Stufen"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
