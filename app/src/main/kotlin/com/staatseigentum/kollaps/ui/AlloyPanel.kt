package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Alloy
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Heavy
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The forge: the only place the heavy elements are ever spent.
 *
 * Every alloy is shown from the moment the workshop opens, forged or not and affordable or not.
 * Hiding the ones that cannot be paid for yet would hide the reason to keep collapsing — the piles
 * grow on their own, so the interesting question is never "what can I afford" but "what am I
 * heading for", and that needs the whole list on screen.
 */
@Composable
fun AlloyPanel(state: GameState, actions: GameActions, modifier: Modifier = Modifier) {
    val offered = Alloy.offered(state)
    if (offered.isEmpty()) return

    PixelPanel(modifier = modifier.fillMaxWidth(), border = Ember, padding = 14) {
        Column {
            PixelLabel(Lang.t("Schmiede"), color = Ember, size = 14)
            Spacer(Modifier.height(4.dp))
            Text(
                text = Lang.t(
                    "Zwei schwere Elemente, in einem Guss. Einmal geschmiedet und für immer " +
                        "behalten — auch durch den Urknall.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )

            Spacer(Modifier.height(10.dp))
            offered.forEach { alloy ->
                AlloyRow(state, alloy, onForge = { actions.forgeAlloy(alloy.id) })
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AlloyRow(
    state: GameState,
    alloy: Alloy,
    onForge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forged = Alloy.isForged(state, alloy)
    val canForge = Alloy.canForge(state, alloy)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SpaceElevated)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = alloy.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (forged) Positive else Starlight,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                text = alloy.effectText,
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(text = alloy.flavor, style = MaterialTheme.typography.bodySmall, color = Muted)

        Spacer(Modifier.height(8.dp))
        if (forged) {
            PixelLabel(Lang.t("Geschmiedet"), color = Positive, size = 11)
        } else {
            // What is in hand against what it takes, on one line, because the piles only grow at
            // a collapse and the gap is the thing worth watching between two of them.
            Text(
                text = "${Numbers.format(Heavy.amountOf(state, alloy.first))}/" +
                    "${Numbers.format(alloy.cost)} ${alloy.first.symbol}   " +
                    "${Numbers.format(Heavy.amountOf(state, alloy.second))}/" +
                    "${Numbers.format(alloy.cost)} ${alloy.second.symbol}",
                style = MaterialTheme.typography.bodySmall,
                color = if (canForge) Positive else Muted,
            )
            Spacer(Modifier.height(6.dp))
            PixelButton(
                label = Lang.t("Schmieden"),
                onClick = onForge,
                modifier = Modifier.fillMaxWidth(),
                enabled = canForge,
                accent = Ember,
            )
        }
    }
}
