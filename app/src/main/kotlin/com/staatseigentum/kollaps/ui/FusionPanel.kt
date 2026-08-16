package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.Element
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.FusionOffer
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Heavy
import com.staatseigentum.kollaps.core.HeavyElement
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The fusion chain: what is in the tanks along the top, the machines that fill them below.
 *
 * The two halves answer different questions and are deliberately not merged. The strip says what
 * the elements are currently worth, which is the reason to run the chain at all; the rows say
 * which machine to feed next, which is the only decision the panel actually asks for.
 */
@Composable
fun FusionPanel(
    state: GameState,
    buyAmount: BuyAmount,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    if (!Fusion.isUnlocked(state)) {
        LockedNotice(state = state, modifier = modifier)
        return
    }

    val offers = remember(state, buyAmount) { GameEngine.fusionOffers(state, buyAmount) }

    Column(modifier = modifier) {
        ElementStrip(state = state)
        HeavyStrip(state = state)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BuyAmount.entries.forEach { amount ->
                PixelButton(
                    label = amount.label,
                    onClick = { actions.setBuyAmount(amount) },
                    modifier = Modifier.weight(1f),
                    accent = if (buyAmount == amount) Ember else Outline,
                )
            }
        }

        // Takes the rest of the height rather than whatever is spare. Without the weight the
        // list is sized by its content and anything above it that grows pushes it off the
        // bottom — which is exactly what happened when the strip below went eight hundred
        // pixels tall and left room for a single machine.
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(offers, key = { it.stage.id }) { offer ->
                FusionRow(
                    offer = offer,
                    onBuy = { actions.buyFuser(offer.stage.id) },
                )
            }
        }
    }
}

/** What the player sees before a brown dwarf: the reason there is nothing here yet. */
@Composable
private fun LockedNotice(state: GameState, modifier: Modifier = Modifier) {
    val target = Tiers.byName(Fusion.UNLOCK_TIER)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLabel(text = Lang.t("Kein Feuer im Kern"), color = Muted)
        Spacer(Modifier.height(10.dp))
        Text(
            text = Lang.t("Fusion beginnt erst beim %s. Vorher ist in der Mitte nichts heiß genug, um irgendetwas zu verschmelzen.", target.name),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = Lang.t("noch %s", Numbers.formatMass((target.threshold - state.runMass).coerceAtLeast(0.0))),
            style = MaterialTheme.typography.bodySmall,
            color = Ember,
        )
    }
}

/**
 * The tanks, in the order a star burns through them.
 *
 * Scrolls sideways rather than wrapping: six elements in a chain read as a chain only while they
 * stay on one line, and a phone in portrait has room for about four of them.
 */
@Composable
private fun ElementStrip(state: GameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Element.entries.forEachIndexed { index, element ->
            if (index > 0) {
                Text(
                    text = ">",
                    style = MaterialTheme.typography.bodySmall,
                    color = Outline,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            ElementChip(state = state, element = element)
        }
    }
}

/**
 * What is left over from past collapses.
 *
 * Its own strip below the chain rather than another few chips on the end of it, because these are
 * a different kind of thing: nothing here is produced by a furnace, none of it is lost to a reset,
 * and no amount of running the chain longer makes any of it appear. Only pressing the button does.
 */
@Composable
private fun HeavyStrip(state: GameState) {
    if (!Heavy.isUnlocked(state)) return

    /*
     * Scrolls sideways, for the reason the chain above it always did.
     *
     * This row did not, and with three elements in it there was no room left: the last box was
     * squeezed to about one character wide, its text wrapped a letter per line, and the box grew
     * to eight hundred pixels tall. The row centres its contents vertically, so the label and the
     * other two boxes ended up floating in the middle of that — and everything below, the buy
     * amounts and the whole list of machines, was pushed off the bottom of the screen.
     *
     * A row that cannot fit its contents has two honest answers: wrap or scroll. Wrapping breaks
     * the reading order of a chain, so it scrolls.
     */
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelLabel(text = "Danach", color = Muted, size = 10)
        Spacer(Modifier.width(8.dp))

        HeavyElement.entries.forEach { element ->
            val held = Heavy.amountOf(state, element)
            if (held < 1.0) return@forEach
            Column(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .background(SpaceCard)
                    .border(2.dp, Positive, RectangleShape)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PixelLabel(text = element.symbol, color = Starlight, size = 14)
                // Never wrapped. A chip in a scrolling row is entitled to its natural width, and
                // a wrapping one is how a box becomes a column of single letters.
                Text(
                    text = Numbers.format(held),
                    style = MaterialTheme.typography.bodySmall,
                    color = Starlight,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = "${Numbers.formatMultiplier(Heavy.factor(state, element))} " +
                        element.bonus.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Positive,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun ElementChip(state: GameState, element: Element) {
    val held = Fusion.amountOf(state, element)
    val active = held >= 1.0
    val factor = Fusion.factor(state, element)

    Column(
        modifier = Modifier
            .background(if (active) SpaceCard else SpaceElevated)
            .border(2.dp, if (active) Ember else Outline, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLabel(
            text = element.symbol,
            color = if (active) Starlight else Muted,
            size = 14,
        )
        Text(
            text = if (active) Numbers.format(Fusion.whole(held)) else "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (active) Starlight else Muted,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            // The lever, not just the multiplier: two elements pull on production and the other
            // four do not, and a bare number would suggest they are interchangeable.
            text = if (active) {
                "${Numbers.formatMultiplier(factor)} ${element.bonus.label}"
            } else {
                element.bonus.label
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (active) Positive else Outline,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun FusionRow(offer: FusionOffer, onBuy: () -> Unit) {
    val enabled = offer.affordable
    val sfx = LocalSfx.current
    val stage = offer.stage

    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                sfx?.purchase()
                onBuy()
            },
        border = when {
            enabled -> Positive
            offer.starving -> Ember
            else -> Outline
        },
        padding = 10,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (offer.level > 0) SpaceElevated else SpaceCard)
                    .border(2.dp, Outline, RectangleShape),
                contentAlignment = Alignment.Center,
            ) {
                PixelLabel(
                    text = offer.level.toString(),
                    color = if (offer.level > 0) Ember else Muted,
                )
            }

            Spacer(Modifier.width(10.dp))

            val input = stage.input
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stage.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Starlight,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (input == null) {
                            "> ${stage.output.symbol}"
                        } else {
                            "${input.symbol} > ${stage.output.symbol}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                    )
                }

                Text(
                    text = when {
                        offer.level <= 0 -> stage.flavor
                        offer.starving && input != null ->
                            Lang.t("wartet auf %s: nur %s", input.label, rate(offer))

                        else -> "liefert ${rate(offer)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (offer.starving) Ember else Muted,
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Numbers.formatMass(offer.cost),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) Positive else Muted,
                )
                if (offer.amount > 1) {
                    Text(
                        text = "+${offer.amount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                    )
                }
            }
        }
    }
}

private fun rate(offer: FusionOffer): String =
    "${Numbers.format(offer.outputPerSecond)} ${offer.stage.output.symbol}/s"
