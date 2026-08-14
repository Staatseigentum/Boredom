package com.staatseigentum.kollaps.ui

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
import com.staatseigentum.kollaps.core.Contract
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * Three things worth doing next.
 *
 * A bar under each, because the interesting information is never "is it done" — that is one bit and
 * the button already carries it — but "how close am I", which is what decides whether the thing on
 * the table is worth changing plans for.
 */
@Composable
fun ContractPanel(state: GameState, actions: GameActions, modifier: Modifier = Modifier) {
    val offered = Contract.offered(state)
    if (offered.isEmpty()) return

    PixelPanel(modifier = modifier.fillMaxWidth(), border = Positive, padding = 14) {
        Column {
            PixelLabel("Aufträge", color = Positive, size = 14)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Drei Ziele, die sich nachlegen. Bezahlt wird in Äonen.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )

            Spacer(Modifier.height(10.dp))
            offered.forEach { contract ->
                ContractRow(state, contract, onClaim = { actions.claimContract(contract.id) })
                Spacer(Modifier.height(6.dp))
            }

            if (state.contractsDone > 0) {
                Spacer(Modifier.height(4.dp))
                PixelLabel("${state.contractsDone} erledigt", color = Muted, size = 10)
            }
        }
    }
}

@Composable
private fun ContractRow(
    state: GameState,
    contract: Contract,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val met = contract.isMetBy(state)

    Column(modifier = modifier.fillMaxWidth().background(SpaceElevated).padding(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = contract.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (met) Positive else Starlight,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${Numbers.format(contract.reward)} Äonen",
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(6.dp))
        PixelBar(
            progress = contract.fractionOf(state).toFloat(),
            color = if (met) Positive else Ember,
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildString {
                append(contract.statusOf(state))
                // Only where there is one. Printing "0 von unbegrenzt" on the six contracts that
                // finish themselves would invent a rule the player then has to unlearn.
                contract.dailyLimit?.let { limit ->
                    append(" · heute ${Contract.doneToday(state, contract)}/$limit")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )

        if (met) {
            Spacer(Modifier.height(8.dp))
            PixelButton(
                label = "Abgeben",
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth(),
                accent = Positive,
            )
        }
    }
}
