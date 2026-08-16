package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Starlight

/** How many places there are to keep a game. */
const val SAVE_SLOTS = 3

/**
 * One slot, as far as the screen is concerned.
 *
 * Plain strings rather than a state, because working out what is in a slot means reading a file,
 * and the two platforms keep their files in different places. What crosses over is what to draw.
 */
data class SlotSummary(
    val index: Int,
    /** What is in it, e.g. `Saturn · 4 Kollapse`, or a line saying it is empty. */
    val detail: String,
    val isActive: Boolean,
    val isEmpty: Boolean,
)

/**
 * The three places a game can live.
 *
 * Shared between the phone and the PC so both look the same and neither can drift; what is *not*
 * shared is where the files are, which is why this takes finished strings and hands back an index.
 *
 * Switching writes the running game out first. That is the whole safety of the feature: a slot
 * button that discarded the last few minutes because it forgot to save would be worse than having
 * no slots at all.
 */
@Composable
fun SaveSlotPanel(
    slots: List<SlotSummary>,
    onSwitch: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sfx = LocalSfx.current

    PixelPanel(modifier = modifier.fillMaxWidth()) {
        PixelLabel(Lang.t("Spielstände"), size = 15)
        Spacer(Modifier.height(4.dp))
        Text(
            text = Lang.t("Drei getrennte Spiele. Beim Wechseln wird der laufende Stand zuerst gespeichert — es geht nichts verloren."),
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(8.dp))

        for (slot in slots) {
            PixelPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clickable(enabled = !slot.isActive) {
                        sfx?.click()
                        onSwitch(slot.index)
                    },
                border = if (slot.isActive) Nebula else Outline,
                padding = 10,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Platz ${slot.index + 1}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (slot.isEmpty && !slot.isActive) Muted else Starlight,
                        )
                        Text(
                            text = slot.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (slot.isActive) {
                        PixelLabel(Lang.t("hier"), color = Nebula, size = 12)
                    } else {
                        PixelLabel(
                            text = if (slot.isEmpty) "neu" else "wechseln",
                            color = Ember,
                            size = 12,
                        )
                    }
                }
            }
        }
    }
}
