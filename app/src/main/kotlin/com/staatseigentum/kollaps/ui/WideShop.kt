package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.CollectorOffer
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Roles
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.UpgradeOffer
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.Space
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The right-hand side of a wide screen: the fleet above, everything else below.
 *
 * The narrow shop puts six panels behind a tab strip, because a phone has room for one panel at a
 * time. A wide screen does not have that problem and was inheriting the answer to it anyway — so
 * the one panel a player is in constantly gets a permanent home, and the strip is left to the five
 * they visit.
 *
 * That split is not arbitrary. Buying machines is the loop; orbits, fusion and the cosmos are
 * places you go, do something, and leave. Having to leave the fleet to look at either was the
 * single most tapped-around thing in the layout.
 */
@Composable
fun WideShop(
    state: GameState,
    stats: Stats,
    buyAmount: BuyAmount,
    actions: GameActions,
    modifier: Modifier = Modifier,
    startSection: Int = 0,
    updateSection: @Composable () -> Unit = {},
    saveSlots: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.background(SpaceElevated).sog(SogDepth.SHELL, Nebula)) {
        FleetSection(
            state = state,
            stats = stats,
            buyAmount = buyAmount,
            actions = actions,
            // Deliberately the smaller half now. The fleet is a list of a dozen short rows and it
            // reads fine cut off — you scroll it. The panel below holds cards with paragraphs in
            // them, two strips of its own and the collapse footer, and at an equal share it was
            // showing one card and a lot of empty space under it.
            modifier = Modifier.fillMaxWidth().weight(0.85f),
        )
        Rule()
        LowerSection(
            state = state,
            stats = stats,
            buyAmount = buyAmount,
            actions = actions,
            startSection = startSection,
            updateSection = updateSection,
            saveSlots = saveSlots,
            modifier = Modifier.fillMaxWidth().weight(1.3f),
        )
    }
}

// ---------------------------------------------------------------------- fleet

@Composable
private fun FleetSection(
    state: GameState,
    stats: Stats,
    buyAmount: BuyAmount,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    val offers = remember(state, buyAmount) {
        GameEngine.collectorOffers(state, buyAmount).filter { it.visible }
    }

    Column(modifier = modifier.background(SpaceElevated)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelLabel(text = "Flotte", color = Starlight, size = 12)
            // Flush against each other rather than spaced: four buttons with gaps read as four
            // choices, one segmented strip reads as one setting with four positions, which is
            // what it is.
            Row {
                for (amount in BuyAmount.entries) {
                    val selected = amount == buyAmount
                    Box(
                        modifier = Modifier
                            .background(if (selected) Ember else SpaceCard)
                            .clickable { actions.setBuyAmount(amount) }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    ) {
                        PixelLabel(
                            text = amount.label,
                            color = if (selected) Space else Muted,
                            size = 10,
                        )
                    }
                }
            }
        }
        Rule()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(offers, key = { it.collector.id }) { offer ->
                FleetRow(
                    state = state,
                    offer = offer,
                    share = if (stats.massPerSecond > 0.0) {
                        (offer.output / stats.massPerSecond).toFloat()
                    } else {
                        0f
                    },
                    onBuy = { actions.buyCollector(offer.collector.id) },
                    onCycleRole = { actions.cycleRole(offer.collector.id) },
                )
            }
        }
    }
}

/**
 * One machine.
 *
 * An accent stripe down the left edge rather than a box around the whole row, which is what the
 * narrow layout uses. At this width a list of framed cards is a stack of rectangles competing with
 * the panel frame around them; a stripe says the same thing — buyable or not — in two pixels.
 *
 * The share bar replaces two lines of text. "Produziert 4,2 Mkg/s, das sind 31 %" is a sentence a
 * player has to read; a bar three quarters full next to a bar a tenth full is a glance.
 */
@Composable
private fun FleetRow(
    state: GameState,
    offer: CollectorOffer,
    share: Float,
    onBuy: () -> Unit,
    onCycleRole: () -> Unit,
) {
    val sfx = LocalSfx.current
    val buyable = offer.affordable && offer.lockedReason == null && offer.amount > 0
    val role = Roles.roleOf(state, offer.collector.id)
    val roleTappable = Roles.isUnlocked(state) && offer.everBought

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceCard)
            .clickable(enabled = buyable) {
                sfx?.purchase()
                onBuy()
            }
            .padding(vertical = 7.dp)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(34.dp)
                .background(if (buyable) Positive else Outline),
        )
        Spacer(Modifier.width(8.dp))

        // The count doubles as the role button, which is where it already was on the narrow row.
        Box(
            modifier = Modifier
                .width(34.dp)
                .clickable(enabled = roleTappable) {
                    sfx?.click()
                    onCycleRole()
                },
            contentAlignment = Alignment.CenterEnd,
        ) {
            PixelLabel(text = offer.owned.toString(), color = Ember, size = 12)
        }
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = offer.collector.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Starlight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (role != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = role.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Nebula,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            PixelBar(
                progress = share,
                color = Positive,
                track = SpaceElevated,
                cells = 20,
                modifier = Modifier.fillMaxWidth().height(5.dp),
            )
        }

        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.width(132.dp),
            horizontalAlignment = Alignment.End,
        ) {
            PixelLabel(
                text = offer.lockedReason ?: Numbers.formatMass(offer.cost),
                color = when {
                    offer.lockedReason != null -> Nebula
                    buyable -> Positive
                    else -> Muted
                },
                size = 11,
            )
            if (share > 0f) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = Numbers.formatPercent(share.toDouble()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------- everything else

@Composable
private fun LowerSection(
    state: GameState,
    stats: Stats,
    buyAmount: BuyAmount,
    actions: GameActions,
    startSection: Int,
    updateSection: @Composable () -> Unit,
    saveSlots: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offers = remember(state) { GameEngine.upgradeOffers(state) }
    val tabs = remember(state) {
        WideTab.entries.filter { it.availableIn(state) }
    }
    var openTab by rememberSaveable { mutableStateOf(WideTab.UPGRADES.name) }
    val tab = WideTab.entries.firstOrNull { it.name == openTab }?.takeIf { it in tabs }
        ?: WideTab.UPGRADES

    Column(modifier = modifier.background(SpaceCard)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sog(SogDepth.CONTAINER, Nebula)
                .urknall(Nebula),
        ) {
            for (entry in tabs) {
                val selected = entry == tab
                val label = if (entry == WideTab.UPGRADES && offers.any { it.affordable }) {
                    "${entry.title} ${offers.count { it.affordable }}"
                } else {
                    entry.title
                }
                Box(
                    modifier = Modifier
                        .background(if (selected) Nebula else Color.Transparent)
                        .clickable { openTab = entry.name }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    PixelLabel(
                        text = label,
                        color = if (selected) Starlight else Muted,
                        size = 12,
                    )
                }
            }
        }
        Rule()

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (tab) {
                WideTab.UPGRADES -> UpgradeGrid(offers = offers, onBuy = actions::buyUpgrade)
                WideTab.ORBITS -> OrbitPanel(state = state, actions = actions)
                WideTab.FUSION -> FusionPanel(
                    state = state,
                    buyAmount = buyAmount,
                    actions = actions,
                )

                WideTab.COSMOS -> CosmosPanel(
                    state = state,
                    stats = stats,
                    actions = actions,
                    startSection = startSection,
                    updateSection = updateSection,
                    saveSlots = saveSlots,
                )
            }
        }

        Rule()
        CollapseFooter(state = state, stats = stats, onCollapse = actions::collapse)
    }
}

/**
 * Upgrades as a grid rather than a list.
 *
 * Two columns, because an upgrade card is a name, an effect and a price — none of which need the
 * full width of this panel, and all of which used to get it. The flavour text is the one thing that
 * does not fit and is dropped from the card; it was never what anybody was reading when deciding
 * what to buy.
 */
@Composable
private fun UpgradeGrid(offers: List<UpgradeOffer>, onBuy: (String) -> Unit) {
    val sfx = LocalSfx.current
    if (offers.isEmpty()) {
        Text(
            text = "Gerade nichts zu verbessern.\nKauf weitere Kollektoren, dann taucht hier " +
                "Neues auf.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(offers, key = { it.upgrade.id }) { offer ->
            Column(
                modifier = Modifier
                    .background(SpaceElevated)
                    .border(2.dp, if (offer.affordable) Positive else Outline, RectangleShape)
                    .clickable(enabled = offer.affordable) {
                        sfx?.purchase()
                        onBuy(offer.upgrade.id)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = offer.upgrade.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Starlight,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = offer.upgrade.effectText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ember,
                )
                Spacer(Modifier.height(6.dp))
                PixelLabel(
                    text = Numbers.formatMass(offer.upgrade.cost),
                    color = if (offer.affordable) Positive else Muted,
                    size = 11,
                )
            }
        }
    }
}

/**
 * The collapse, as one line along the bottom rather than a card in a list.
 *
 * It is the one button on this side of the screen that ends the run, and in the narrow layout it
 * lives four scrolls down inside a tab. Here it is always visible and never in the way — which is
 * the right treatment for something a player presses once an hour and wants to see the price of
 * constantly.
 *
 * The two-tap confirmation is kept exactly as it was. This is not a button to press by accident.
 */
@Composable
private fun CollapseFooter(state: GameState, stats: Stats, onCollapse: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    val soon = remember(state) { GameEngine.singularitiesIn(state, 20 * 60.0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .sog(SogDepth.CONTAINER, Ember),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (stats.canCollapse) {
                    "Kollaps bringt ${Numbers.format(stats.pendingSingularities)} Singularitäten"
                } else {
                    "Erreiche das Schwarze Loch, um zu kollabieren"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (stats.canCollapse) Positive else Muted,
            )
            if (stats.canCollapse && soon > stats.pendingSingularities) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "in 20 min: ${Numbers.format(soon)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        PixelButton(
            label = if (confirming) "Sicher?" else "Kollabieren",
            onClick = {
                if (confirming) {
                    onCollapse()
                    confirming = false
                } else {
                    confirming = true
                }
            },
            enabled = stats.canCollapse,
            accent = Ember,
        )
    }
}

/** The four places the lower panel can be. See [WideShop] for why the fleet is not among them. */
private enum class WideTab(val title: String) {
    UPGRADES("Upgrades"),
    ORBITS("Bahnen"),
    FUSION("Fusion"),
    COSMOS("Kosmos"),
    ;

    fun availableIn(state: GameState): Boolean = when (this) {
        ORBITS -> com.staatseigentum.kollaps.core.Orbits.isUnlocked(state)
        FUSION -> com.staatseigentum.kollaps.core.Fusion.isUnlocked(state)
        else -> true
    }
}
