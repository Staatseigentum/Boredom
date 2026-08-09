package com.staatseigentum.kollaps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Tutorial
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The first five minutes, said quietly.
 *
 * A strip at the bottom of the body rather than a sequence of dialogs. An idle game is played with
 * one thumb and no patience for being taught, and a modal that has to be dismissed before the
 * first tap would be the worst possible opening — so this never blocks anything, never moves
 * anything, and goes away on its own the moment the step it describes is done.
 *
 * The step is read out of the state, so it also serves the player who did the thing before reading
 * about it: they see the *next* one and never the one they already finished.
 */
@Composable
fun TutorialHint(
    state: GameState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = if (Tutorial.appliesTo(state)) Tutorial.current(state) else null
    val sfx = LocalSfx.current

    AnimatedVisibility(
        visible = step != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        // Held from the last non-null value, so the strip fades out saying what it said rather
        // than going blank for the length of the animation.
        val shown = step ?: return@AnimatedVisibility

        PixelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            border = Ember,
            padding = 10,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        PixelLabel(shown.title, color = Ember, size = 13)
                        PixelLabel(
                            text = "${Tutorial.position(state)}/${Tutorial.total}",
                            color = Muted,
                            size = 11,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = shown.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Starlight,
                    )
                }
                Spacer(Modifier.width(10.dp))
                // A word, not a button: the strip is already the smallest thing on screen and a
                // framed control on it would weigh more than what it dismisses.
                Text(
                    text = "aus",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.clickable {
                        sfx?.click()
                        onDismiss()
                    },
                )
            }
        }
    }
}
