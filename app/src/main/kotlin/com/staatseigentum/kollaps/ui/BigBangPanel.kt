package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.AeonUpgrades
import com.staatseigentum.kollaps.core.BigBang
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Path
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The second reset, and the shop it feeds.
 *
 * Deliberately blunt about what it costs. The collapse takes a run; this takes every collapse the
 * player has ever made, and a button that hid that behind "Neuanfang" would be a trick.
 */
@Composable
fun BigBangPanel(
    state: GameState,
    stats: Stats,
    onBigBang: (String) -> Unit,
    onBuy: (String) -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }

    PixelPanel(modifier = Modifier.fillMaxWidth(), border = Nebula) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelLabel("Urknall", color = Nebula, size = 16)
            if (state.aeons > 0.0) {
                PixelLabel("${Numbers.format(state.aeons)} Äonen", color = Ember, size = 13)
            }
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Wirf alles weg, was deine Kollapse aufgebaut haben: Singularitäten, " +
                "Prestige-Upgrades, den Zähler selbst. Was bleibt, sind Erfolge, bestandene " +
                "Herausforderungen — und Äonen.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        Spacer(Modifier.height(10.dp))

        if (state.bigBangs > 0) {
            Text(
                text = "Bisher ${state.bigBangs}× ausgelöst.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(6.dp))
        }

        Text(
            text = if (stats.canBigBang) {
                "Jetzt zu holen: ${Numbers.format(stats.pendingAeons)} Äonen"
            } else {
                "Ab ${BigBang.REQUIRED_COLLAPSES} Kollapsen. Du bist bei ${state.collapses}."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (stats.canBigBang) Positive else Muted,
        )
        Path.of(state)?.let { running ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dieses Universum: ${running.label}",
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!confirming) {
            PixelButton(
                label = if (stats.canBigBang) "Urknall auslösen" else "Noch nicht so weit",
                onClick = { confirming = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = stats.canBigBang,
                accent = Ember,
            )
        } else {
            // The confirmation *is* the choice. A separate "really?" followed by a picker would
            // be two dialogs for one decision, and picking a universe to live in is a better
            // second thought than a yes-or-no about a button already pressed once.
            PixelLabel("Was für ein Universum?", color = Ember, size = 13)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Die Ausrichtung gilt, bis du das nächste Mal alles wegwirfst.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(8.dp))

            for (path in Path.entries) {
                PathChoice(
                    path = path,
                    onChoose = {
                        onBigBang(path.id)
                        confirming = false
                    },
                )
                Spacer(Modifier.height(6.dp))
            }

            PixelButton(
                label = "Doch nicht",
                onClick = { confirming = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // The shop only means anything once there is something in the purse.
    if (state.aeons > 0.0 || state.aeonUpgrades.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        AeonShop(state = state, onBuy = onBuy)
    }
}

@Composable
private fun AeonShop(state: GameState, onBuy: (String) -> Unit) {
    val offered = BigBang.offered(state)

    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        PixelLabel("Äonen ausgeben", size = 15)
        Spacer(Modifier.height(8.dp))

        if (offered.isEmpty()) {
            Text(
                text = "Alle ${AeonUpgrades.all.size} gekauft. Von hier aus geht es nur noch " +
                    "durch Spielen weiter.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            return@PixelPanel
        }

        for (upgrade in offered) {
            val affordable = state.aeons >= upgrade.cost
            PixelPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                border = if (affordable) Positive else Outline,
                padding = 10,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            upgrade.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Starlight,
                        )
                        Text(
                            upgrade.effectText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Ember,
                        )
                        Text(
                            upgrade.flavor,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    PixelButton(
                        label = Numbers.format(upgrade.cost),
                        onClick = { onBuy(upgrade.id) },
                        enabled = affordable,
                        accent = Nebula,
                    )
                }
            }
        }
    }
}

/** One universe on offer: what it is called, what it leans towards, and one tap to live in it. */
@Composable
private fun PathChoice(path: Path, onChoose: () -> Unit) {
    val sfx = LocalSfx.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceCard)
            .border(2.dp, Outline, RectangleShape)
            .clickable {
                sfx?.purchase()
                onChoose()
            }
            .padding(10.dp),
    ) {
        Column {
            Text(
                text = path.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Starlight,
            )
            Text(
                text = path.flavor,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(4.dp))
            for (line in path.effectTexts) {
                Text(
                    text = "· $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = Positive,
                )
            }
        }
    }
}
