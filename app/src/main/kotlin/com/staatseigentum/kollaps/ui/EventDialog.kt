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
import com.staatseigentum.kollaps.core.EventOption
import com.staatseigentum.kollaps.core.EventPrompt
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
 *
 * A stop inside a chain is drawn by the same dialog, with the story's name over the title. It is
 * the one thing worth saying differently, and it changes how the answer reads: the same two
 * buttons mean something else when what follows depends on which one is pressed.
 */
@Composable
fun EventDialog(
    prompt: EventPrompt,
    onChoose: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceElevated,
        shape = RectangleShape,
        title = {
            Column {
                prompt.chain?.let { chain ->
                    Text(
                        text = chain.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Ember,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                PixelLabel(prompt.title, color = Nebula, size = 16)
            }
        },
        text = {
            Column {
                Text(
                    text = prompt.flavor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
                Spacer(Modifier.height(12.dp))
                Choice(prompt.first)
                Spacer(Modifier.height(10.dp))
                Choice(prompt.second)
            }
        },
        confirmButton = {
            PixelButton(
                label = prompt.first.label,
                onClick = { onChoose(0) },
                accent = Ember,
            )
        },
        dismissButton = {
            PixelButton(
                label = prompt.second.label,
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
