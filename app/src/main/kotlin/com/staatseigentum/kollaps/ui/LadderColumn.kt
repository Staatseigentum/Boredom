package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Lore
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight
import com.staatseigentum.kollaps.ui.theme.Unreached
import kotlinx.coroutines.delay

/**
 * The ladder, as a column you can look up and down.
 *
 * Twenty-five rungs existed all along and the interface only ever showed you two of them: the one
 * you are on and the one after it. Everything else — how far you have come, how far there is to go,
 * what the rung you passed an hour ago was even called — lived in a progress bar and a name.
 *
 * There is room for it on a wide screen and nowhere else, which is why this is the one piece of the
 * overhaul with no phone equivalent.
 *
 * ## The chronicle
 *
 * The foot of the column carries the line from [Lore] for whichever rung is under the pointer, and
 * the current one when nothing is. That is the whole interaction: the ladder is a list of names
 * until you run a pointer down it, and then it is the story.
 *
 * Only as far as the player has actually climbed. A line for a body fifteen rungs above them is a
 * spoiler for a game whose only voice this is — so the hover stops at [GameState.bestTier], which
 * is the same measure [Lore.unlocked] uses.
 */
@Composable
fun LadderColumn(state: GameState, stats: Stats, modifier: Modifier = Modifier) {
    // Which rung the pointer is over. Null is "none", and the foot falls back to where the player
    // is standing — which is what it shows for the whole time nobody is touching this.
    var hovered by remember { mutableStateOf<Int?>(null) }

    // On the catalogue ladder the player is above every named rung, so "current" would point past
    // the end of the list. The last one is where they are as far as this column is concerned; the
    // designation itself is in the HUD, where the number belongs.
    val standing = stats.tier.index.coerceAtMost(Tiers.all.lastIndex)
    val listState = rememberLazyListState()

    // Follows the climb. Without this the column shows the first ten rungs for ever and the player
    // has to scroll to find themselves after every collapse.
    LaunchedEffect(standing) {
        listState.animateScrollToItem((standing - 3).coerceAtLeast(0))
    }

    Column(
        modifier = modifier
            .background(SpaceElevated)
            .sog(SogDepth.SHELL, Nebula),
    ) {
        PixelLabel(
            text = Lang.t("Leiter %s/%s", standing + 1, Tiers.all.size),
            color = Muted,
            size = 10,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        )
        Rule()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            itemsIndexed(Tiers.all) { index, tier ->
                LadderRow(
                    tier = tier.label,
                    // Above the black hole every named rung is behind the player, so the list marks
                    // all of them reached rather than pretending the last one is still ahead.
                    reached = index < standing || stats.tier.isDesignated,
                    standing = index == standing && !stats.tier.isDesignated,
                    glow = Color(stats.tier.glowColor),
                    readable = index <= minOf(state.bestTier, standing),
                    onHover = { hovered = if (it) index else null },
                )
            }
        }

        Rule()

        /*
         * The line flashes when a rung is reached.
         *
         * The celebration ring goes off around the body, on the other side of the screen from the
         * column that just gained a step — so for a second and a bit the chronicle line is lit
         * rather than muted, which is enough to pull the eye over and show *where* the ring came
         * from. It says nothing new; it says the same thing louder, once.
         *
         * Only upwards. A collapse drops the rung by twenty-four steps at once, and lighting the
         * meteorite's line after four hours of climbing would be the interface congratulating the
         * player on losing everything.
         */
        var fresh by remember { mutableStateOf(false) }
        val seen = remember { intArrayOf(standing) }
        LaunchedEffect(standing) {
            if (standing > seen[0]) {
                fresh = true
                delay(ASCENT_FLASH_MILLIS)
                fresh = false
            }
            seen[0] = standing
        }

        val shown = hovered ?: standing
        val fragment = Lore.forTier(shown)
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            PixelLabel(
                text = Lang.t("Chronik · %s", Tiers.all[shown].label),
                color = Nebula,
                size = 9,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = fragment?.text.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                // Lit only for the rung actually reached — hovering somewhere else while the flash
                // is running is the player asking about a different line, and it gets the usual one.
                color = if (fresh && hovered == null) Starlight else Muted,
            )
        }
    }
}

@Composable
private fun LadderRow(
    tier: String,
    reached: Boolean,
    standing: Boolean,
    glow: Color,
    readable: Boolean,
    onHover: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Both, because the wide layout is not only a desktop: a tablet in landscape crosses
            // the same threshold and has no pointer to hover with.
            .pointerInput(readable) {
                if (!readable) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> onHover(true)
                            PointerEventType.Exit -> onHover(false)
                            else -> Unit
                        }
                    }
                }
            }
            .clickable(enabled = readable) { onHover(true) }
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(
                    when {
                        standing -> glow
                        reached -> Positive
                        else -> Outline
                    },
                ),
        )
        Text(
            text = tier,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                standing -> Starlight
                reached -> Muted
                else -> Unreached
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Two device pixels of [Outline] across the full width. The overhaul's only kind of divider. */
@Composable
internal fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(2.dp).background(Outline))
}

/** How long a newly reached rung keeps its chronicle line lit. */
private const val ASCENT_FLASH_MILLIS = 1_200L
