package com.staatseigentum.kollaps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Achievement
import com.staatseigentum.kollaps.core.Achievements
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlinx.coroutines.delay

/** How long one card stays up before the next is shown. */
private const val SHOWN_MILLIS = 2_600L

/**
 * How many cards one moment may produce.
 *
 * A collapse can cross several thresholds at once and that is worth showing; a slot loaded from a
 * far more advanced save would otherwise queue forty of them and hold the screen for two minutes.
 * The rest are still earned and still in the list — they simply do not each get a card.
 */
private const val MOST_AT_ONCE = 4

/**
 * A card that appears when an achievement is earned, with a sound.
 *
 * Sixty-three of these exist and several can be earned in the same instant — the first collapse
 * alone crosses a handful of thresholds at once. So this is a queue rather than a single slot:
 * they are shown one after another, each for the same short moment, and the sound is tied to the
 * card rather than to the earning. A stack of five cards, or five cues fired together into a
 * chord, would turn the nicest moment in the game into a mess.
 *
 * Which ones are new is worked out here rather than reported by the engine. The rules are pure
 * functions over the save and have no business knowing what has already been shown on a screen;
 * what the screen knows is which achievements it saw a frame ago.
 */
@Composable
fun AchievementToast(
    earned: Set<String>,
    modifier: Modifier = Modifier,
    /**
     * True while something else owns the screen — a collapse or a big bang playing out.
     *
     * Holds the *showing*, never the collecting: what was earned still goes into the queue, and
     * the card appears once the sequence is over. Skipping it instead would quietly swallow the
     * two or three achievements a reset hands out, which are exactly the ones worth seeing.
     */
    hold: Boolean = false,
) {
    // Read fresh on every use rather than captured: a long-lived effect keeps whatever it closed
    // over on the first composition, and switching the sound off would otherwise go unnoticed here.
    val sfx by rememberUpdatedState(LocalSfx.current)

    // Seeded with what the save already had, so opening the game does not replay a lifetime of
    // achievements at somebody who earned them weeks ago.
    var seen by remember { mutableStateOf(earned) }
    val queue = remember { mutableStateListOf<Achievement>() }
    var showing by remember { mutableStateOf<Achievement?>(null) }

    /*
     * Keyed on the set, not on `Unit`.
     *
     * The first version read the parameter inside a `snapshotFlow` in an effect keyed on `Unit`.
     * A parameter is not snapshot state, so the flow saw the value from the first composition and
     * never emitted again — the card simply never appeared. Keying the effect on the set is what
     * makes it run when the set actually changes.
     */
    LaunchedEffect(earned) {
        val fresh = earned - seen
        val replaced = seen.any { it !in earned }
        seen = earned

        // A save that no longer contains something this one had seen is a *different* save —
        // another slot, or an imported block. Its achievements were not just earned, so they are
        // taken as read rather than paraded past somebody who switched tabs.
        if (fresh.isEmpty() || replaced) return@LaunchedEffect

        // In catalogue order rather than set order, so two earned together always appear in the
        // same order and the list on the achievements tab agrees with what was shown.
        queue += Achievements.all.filter { it.id in fresh }.take(MOST_AT_ONCE)
    }

    /*
     * One loop for the whole session rather than an effect keyed on the queue.
     *
     * Keying on the queue deadlocked: an achievement earned while a card was up changed the key,
     * restarted the effect, and the restarted one bailed out because a card was already showing —
     * so the `delay` that takes the card down again never ran, and it stayed up for good. A single
     * loop that waits for work has no such state to get wrong.
     */
    // Read fresh rather than captured, for the same reason the sound is: the loop below outlives
    // the sequence that sets it.
    val holding by rememberUpdatedState(hold)

    LaunchedEffect(Unit) {
        snapshotFlow { queue.isNotEmpty() && !holding }.collect { ready ->
            if (!ready) return@collect
            while (queue.isNotEmpty() && !holding) {
                showing = queue.removeAt(0)
                sfx?.unlock()
                delay(SHOWN_MILLIS)
                showing = null
            }
        }
    }

    AnimatedVisibility(
        visible = showing != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        // Held from the last non-null value so the card fades out saying what it said.
        val achievement = showing ?: return@AnimatedVisibility

        PixelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            border = Positive,
            padding = 10,
        ) {
            Column {
                PixelLabel("Erfolg", color = Positive, size = 12)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Starlight,
                )
                Text(
                    text = achievement.flavor,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}
