package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.staatseigentum.kollaps.core.Lore
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
                    text = "Du warst ${Numbers.formatDuration(report.awaySeconds)} weg.",
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "+${Numbers.formatMass(report.gained)}",
                    style = MaterialTheme.typography.displayMedium,
                    color = Ember,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${Numbers.formatDuration(report.seconds)} angerechnet, " +
                        "zu ${Numbers.formatPercent(report.efficiency)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )

                // The cap is invisible while it does not bite, and then it silently eats a night.
                // Saying what it cost is the only way the upgrade that raises it means anything.
                if (report.cappedOut) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Die Offline-Grenze war voll — ${Numbers.formatMass(report.lostToCap)} " +
                            "blieben liegen. Ein größerer Speicher hätte sie mitgenommen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ember,
                    )
                }

                if (report.shares.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    PixelLabel("Wer geschuftet hat", size = 12, color = Muted)
                    Spacer(Modifier.height(4.dp))
                    for (share in report.shares.take(SHARES_SHOWN)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${share.collector.name} ×${share.owned}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Starlight,
                            )
                            Text(
                                text = Numbers.formatMass(report.gained * share.share),
                                style = MaterialTheme.typography.bodySmall,
                                color = Ember,
                            )
                        }
                    }
                    if (report.shares.size > SHARES_SHOWN) {
                        Text(
                            text = "… und ${report.shares.size - SHARES_SHOWN} weitere",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                }
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
    val arrival = LocalSfx.current
    LaunchedEffect(tier.index) {
        arrival?.levelUp()
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

            // The one line of story this rung carries. Shown here and nowhere else at the moment
            // it is earned; the whole set is kept in the chronicle in the achievements tab.
            Lore.forTier(tier.index)?.let { fragment ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = fragment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Starlight,
                    textAlign = TextAlign.Center,
                )
            }
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

/** How many collectors the offline report lists before it says "and others". */
private const val SHARES_SHOWN = 4
