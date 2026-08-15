package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.staatseigentum.kollaps.core.FeatureIntro
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * A system, the first time it exists.
 *
 * The one place in this game where a modal is the right shape, and it is worth saying why, because
 * everything else here deliberately avoids one. The tutorial is a nudge because it competes with
 * the thing it is describing: you are meant to be tapping while you read it. An unlock is the
 * opposite — it is a *reward*, it happens once, and the moment it lands is the one moment the
 * player is not in the middle of anything. A strip in the corner would be missed, and being missed
 * is exactly the failure these were written to fix: eleven systems used to arrive by a tab
 * quietly appearing.
 *
 * Three things and no more: what it is called, where to find it, and what it is for. The last one
 * matters most and is the one a tab appearing can never convey.
 */
@Composable
fun IntroDialog(intro: FeatureIntro, onDismiss: () -> Unit) {
    val sfx = LocalSfx.current

    // The unlock cue, once, when the card arrives — the same one an achievement uses, because
    // this is the same kind of moment and the player has already learned what it means.
    LaunchedEffect(intro.id) { sfx?.unlock() }

    Dialog(onDismissRequest = onDismiss) {
        PixelPanel(
            modifier = Modifier.fillMaxWidth(),
            border = Nebula,
            padding = 16,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PixelLabel("Neu freigeschaltet", color = Muted, size = 10)
                Spacer(Modifier.height(6.dp))
                PixelLabel(intro.title, color = Nebula, size = 18)

                /*
                 * Where it lives, on its own line and in its own box.
                 *
                 * The single most useful sentence on the card, and the reason it is set apart
                 * rather than folded into the prose: somebody who reads nothing else still has to
                 * come away knowing which tab to open. A panel appearing somewhere in a shop with
                 * four tabs and six sections is otherwise found by accident, a quarter of an hour
                 * later.
                 */
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpaceElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    PixelLabel("Zu finden: ${intro.where}", color = Ember, size = 11)
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = intro.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Starlight,
                )

                Spacer(Modifier.height(16.dp))
                PixelButton(
                    label = "Verstanden",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accent = Nebula,
                )
            }
        }
    }
}
