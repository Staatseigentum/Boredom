package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.CosmicEvent
import com.staatseigentum.kollaps.core.EventOption
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The event, and the two answers to it.
 *
 * Both buttons are the same size and the same weight, because neither is the right one: mass in
 * hand is better if the phone is about to go back in a pocket, and a buff is better if the player
 * is going to sit and tap. Marking one as the recommendation would remove the only decision.
 */
@Composable
fun EventDialog(
    event: CosmicEvent,
    onChoose: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceElevated,
        shape = RectangleShape,
        title = { PixelLabel(event.title, color = Nebula, size = 16) },
        text = {
            Column {
                Text(
                    text = event.flavor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
                Spacer(Modifier.height(12.dp))
                Choice(event.first)
                Spacer(Modifier.height(10.dp))
                Choice(event.second)
            }
        },
        confirmButton = {
            PixelButton(
                label = event.first.label,
                onClick = { onChoose(0) },
                accent = Ember,
            )
        },
        dismissButton = {
            PixelButton(
                label = event.second.label,
                onClick = { onChoose(1) },
                accent = Positive,
            )
        },
    )
}

@Composable
private fun Choice(option: EventOption) {
    PixelPanel(modifier = Modifier.fillMaxWidth(), padding = 10) {
        Text(option.label, style = MaterialTheme.typography.bodyLarge, color = Starlight)
        Text(option.rewardText, style = MaterialTheme.typography.bodySmall, color = Ember)
        Text(option.flavor, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
