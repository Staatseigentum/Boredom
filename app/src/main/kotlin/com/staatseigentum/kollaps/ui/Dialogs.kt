package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
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
        title = { PixelLabel(Lang.t("Willkommen zurück"), color = Starlight, size = 16) },
        text = {
            Column {
                Text(
                    text = Lang.t("Du warst %s weg.", Numbers.formatDuration(report.awaySeconds)),
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Lang.t("+%s", Numbers.formatMass(report.gained)),
                    style = MaterialTheme.typography.displayMedium,
                    color = Ember,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Lang.t("%s angerechnet, zu %s.", Numbers.formatDuration(report.seconds), Numbers.formatPercent(report.efficiency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )

                // The cap is invisible while it does not bite, and then it silently eats a night.
                // Saying what it cost is the only way the upgrade that raises it means anything.
                if (report.cappedOut) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Lang.t("Die Offline-Grenze war voll — %s blieben liegen. Ein größerer Speicher hätte sie mitgenommen.", Numbers.formatMass(report.lostToCap)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Ember,
                    )
                }

                if (report.shares.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    PixelLabel(Lang.t("Wer geschuftet hat"), size = 12, color = Muted)
                    Spacer(Modifier.height(4.dp))
                    for (share in report.shares.take(SHARES_SHOWN)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = Lang.t("%s ×%s", share.collector.name, share.owned),
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
                            text = Lang.t("… und %s weitere", report.shares.size - SHARES_SHOWN),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                }
            }
        },
        confirmButton = {
            PixelButton(label = Lang.t("Weiter geht's"), onClick = onDismiss, accent = Ember)
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
                text = if (tier.isFinal) Lang.t("ENDGAME") else Lang.t("NEUE STUFE"),
                style = MaterialTheme.typography.labelLarge,
                color = Muted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tier.label,
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
                text = Lang.t("Produktion jetzt %s", Numbers.formatMultiplier(tier.productionMultiplier)),
                style = MaterialTheme.typography.bodyMedium,
                color = Ember,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = when {
                    // Above the black hole there is no "how many left" worth printing — the
                    // catalogue ladder is sixteen thousand rungs, and saying so is not encouraging,
                    // it is discouraging. The designation itself is the progress.
                    tier.isDesignated -> Lang.t("Tippen zum Weitermachen · Katalog %s", tier.label)
                    tier.isFinal ->
                        Lang.t("Du hast die Leiter zu Ende geklettert. Im Reiter Kosmos kannst du kollabieren und mit Singularitäten neu anfangen.")
                    else -> Lang.t("Tippen zum Weitermachen · noch %s Stufen", Tiers.all.size - tier.index - 1)
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
