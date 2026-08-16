package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Tutorial
import com.staatseigentum.kollaps.core.TutorialSpot
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * How the hint asks to be taken somewhere.
 *
 * A composition local rather than a parameter, and for the usual reason: the hint is drawn deep
 * inside the tap area, three composables below the one that owns which screen is showing, and
 * threading a navigation callback through all of them would put the tutorial into the signature of
 * the body. The default does nothing, so previews and the harness need provide nothing.
 */
val LocalTutorialGuide = staticCompositionLocalOf<(TutorialSpot) -> Unit> { {} }

/**
 * The opening, said quietly — and now able to point.
 *
 * A strip at the bottom of the body rather than a sequence of dialogs. An idle game is played with
 * one thumb and no patience for being taught, and a modal that has to be dismissed before the
 * first tap would be the worst possible opening — so this never blocks anything, never moves
 * anything, and goes away on its own the moment the step it describes is done.
 *
 * What it does now that it did not: name the place. Half the steps are about somewhere that is not
 * on screen — on a phone the shop is a different screen entirely — and an instruction to buy a
 * collector is useless to somebody who has not yet found where collectors are bought. So the strip
 * carries the name of the place, tapping it goes there, and while the step is open the way there
 * is marked in the bar along the bottom.
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
    val guide = LocalTutorialGuide.current

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
                        PixelLabel(shown.title, color = Ember, size = 13, maxLines = 1)
                        PixelLabel(
                            text = "${Tutorial.position(state)}/${Tutorial.total}",
                            color = Muted,
                            size = 11,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = shown.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Starlight,
                    )

                    /*
                     * The way there, as a line you can press.
                     *
                     * Only where the step is actually somewhere else. Two of them are about the
                     * screen the player is already looking at, and offering to take somebody
                     * where they already are is worse than saying nothing — it teaches that the
                     * line does not mean anything.
                     */
                    shown.spot?.let { spot ->
                        Spacer(Modifier.height(6.dp))
                        PixelLabel(
                            text = "→ ${spot.label} zeigen",
                            color = Ember,
                            size = 11,
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                sfx?.click()
                                guide(spot)
                            },
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                // A word, not a button: the strip is already the smallest thing on screen and a
                // framed control on it would weigh more than what it dismisses.
                Text(
                    text = Lang.t("aus"),
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

/**
 * How strongly a marked target is glowing right now, between nought and one.
 *
 * Shared so the bar along the bottom and anything else that ever marks a target pulse together —
 * two things drawing attention to the same place at two different rhythms would read as two
 * separate alerts.
 *
 * Held still under reduced motion. What it marks is a colour change either way, so the target is
 * still obvious; only the breathing goes.
 */
@Composable
fun tutorialPulse(): Float {
    if (LocalReduceMotion.current) return 1f
    val transition = rememberInfiniteTransition(label = "tutorial-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    return pulse
}
