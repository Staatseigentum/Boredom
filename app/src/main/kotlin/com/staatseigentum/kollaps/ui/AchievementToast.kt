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
) {
    val sfx = LocalSfx.current

    // Seeded with what the save already had, so opening the game does not replay a lifetime of
    // achievements at somebody who earned them weeks ago.
    val seen = remember { mutableStateOf(earned) }
    val queue = remember { mutableStateListOf<Achievement>() }
    var showing by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { earned }.collect { now ->
            val fresh = now - seen.value
            seen.value = now
            // In catalogue order rather than set order, so two earned together always appear in
            // the same order and the list on the achievements tab agrees with what was shown.
            queue += Achievements.all.filter { it.id in fresh }
        }
    }

    LaunchedEffect(queue.size, showing) {
        if (showing != null || queue.isEmpty()) return@LaunchedEffect
        showing = queue.removeAt(0)
        sfx?.unlock()
        delay(SHOWN_MILLIS)
        showing = null
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
