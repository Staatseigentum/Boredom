package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.CatalogueFind
import com.staatseigentum.kollaps.core.FindAnswer
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Space
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The question a catalogue find asks.
 *
 * Three answers rather than the two an event offers, and no way to dismiss it without picking one —
 * a find that could be tapped away would be tapped away, and the whole reason it exists is that
 * sixteen thousand rungs need something on them that is not a number.
 *
 * Each answer prints what it pays, because the three currencies are wanted at different moments and
 * the choice is only real if the player can compare them without leaving the dialog.
 */
@Composable
fun FindDialog(state: GameState, onAnswer: (String) -> Unit, modifier: Modifier = Modifier) {
    val find = CatalogueFind.byId(state.pendingFind) ?: return

    Box(
        modifier = modifier.fillMaxSize().background(Space.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        PixelPanel(
            modifier = Modifier.widthIn(max = 460.dp).padding(20.dp),
            border = Nebula,
            padding = 16,
        ) {
            Column {
                PixelLabel("Katalogfund", color = Nebula, size = 12)
                Spacer(Modifier.height(8.dp))
                Text(find.title, style = MaterialTheme.typography.titleMedium, color = Starlight)
                Spacer(Modifier.height(6.dp))
                Text(find.flavor, style = MaterialTheme.typography.bodySmall, color = Muted)

                Spacer(Modifier.height(16.dp))
                FindAnswer.entries.forEach { answer ->
                    PixelButton(
                        label = answer.label,
                        onClick = { onAnswer(answer.id) },
                        modifier = Modifier.fillMaxWidth(),
                        accent = if (answer == FindAnswer.ANZAPFEN) Ember else Nebula,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = when (answer) {
                            FindAnswer.AUSWERTEN ->
                                "${Numbers.format(CatalogueFind.AEON_REWARD)} Äon · ${answer.flavor}"
                            // The live number, not the flat bonus: with the cap in play what the
                            // next tap is actually worth is the only figure worth comparing.
                            FindAnswer.ANZAPFEN ->
                                "Läuft auf ${Numbers.formatMultiplier(
                                    CatalogueFind.tapMultiplier(state.copy(findsTapped = state.findsTapped + 1)),
                                )} · ${answer.flavor}"
                            FindAnswer.RUHEN -> answer.flavor
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
