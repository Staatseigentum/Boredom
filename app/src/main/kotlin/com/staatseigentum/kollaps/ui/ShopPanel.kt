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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.CollectorOffer
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.UpgradeOffer
import com.staatseigentum.kollaps.core.Upgrades
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

private val TABS = listOf("Kollektoren", "Upgrades", "Kosmos")

@Composable
fun ShopPanel(
    state: GameState,
    stats: Stats,
    buyAmount: BuyAmount,
    onBuyAmount: (BuyAmount) -> Unit,
    onBuyCollector: (String) -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    /** The update section, handed in so the shop stays free of any networking concern. */
    updateSection: @Composable () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.background(SpaceElevated)) {
        // A hard rule instead of an elevation shadow.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Outline),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            TABS.forEachIndexed { index, title ->
                PixelTab(
                    title = title,
                    selected = tab == index,
                    onClick = { tab = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when (tab) {
            0 -> CollectorList(
                state = state,
                buyAmount = buyAmount,
                onBuyAmount = onBuyAmount,
                onBuy = onBuyCollector,
            )

            1 -> UpgradeList(
                offers = GameEngine.upgradeOffers(state),
                onBuy = onBuyUpgrade,
            )

            else -> CosmosPanel(
                state = state,
                stats = stats,
                onCollapse = onCollapse,
                updateSection = updateSection,
            )
        }
    }
}

@Composable
private fun PixelTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sfx = LocalSfx.current
    Box(
        modifier = modifier
            .background(if (selected) Nebula else SpaceElevated)
            .clickable {
                sfx?.click()
                onClick()
            }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        PixelLabel(
            text = title,
            color = if (selected) Starlight else Muted,
            size = 12,
        )
    }
}

// ---------------------------------------------------------------------- collectors

@Composable
private fun CollectorList(
    state: GameState,
    buyAmount: BuyAmount,
    onBuyAmount: (BuyAmount) -> Unit,
    onBuy: (String) -> Unit,
) {
    val offers = remember(state, buyAmount) {
        GameEngine.collectorOffers(state, buyAmount).filter { it.visible }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BuyAmount.entries.forEach { amount ->
                PixelButton(
                    label = amount.label,
                    onClick = { onBuyAmount(amount) },
                    modifier = Modifier.weight(1f),
                    accent = if (buyAmount == amount) Ember else Outline,
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(offers, key = { it.collector.id }) { offer ->
                CollectorRow(offer = offer, onBuy = { onBuy(offer.collector.id) })
            }
        }
    }
}

@Composable
private fun CollectorRow(offer: CollectorOffer, onBuy: () -> Unit) {
    val enabled = offer.affordable
    val sfx = LocalSfx.current
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                sfx?.click()
                onBuy()
            },
        border = if (enabled) Positive else Outline,
        padding = 10,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (offer.everBought) SpaceElevated else SpaceCard)
                    .border(2.dp, Outline, RectangleShape),
                contentAlignment = Alignment.Center,
            ) {
                PixelLabel(
                    text = offer.owned.toString(),
                    color = if (offer.everBought) Ember else Muted,
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.collector.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Starlight,
                )
                Text(
                    text = if (offer.owned > 0) {
                        "liefert ${Numbers.formatRate(offer.output)}"
                    } else {
                        offer.collector.flavor
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                PixelLabel(
                    text = Numbers.formatMass(offer.cost),
                    color = if (enabled) Positive else Muted,
                    size = 12,
                )
                if (offer.amount > 1) {
                    Text(
                        text = "×${offer.amount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- upgrades

@Composable
private fun UpgradeList(offers: List<UpgradeOffer>, onBuy: (String) -> Unit) {
    val sfx = LocalSfx.current
    if (offers.isEmpty()) {
        EmptyHint(
            "Gerade nichts zu verbessern.\nKauf weitere Kollektoren, dann tauchen hier neue Upgrades auf.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(offers, key = { it.upgrade.id }) { offer ->
            PixelPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = offer.affordable) {
                        sfx?.click()
                        onBuy(offer.upgrade.id)
                    },
                border = if (offer.affordable) Positive else Outline,
                padding = 10,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = offer.upgrade.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Starlight,
                        )
                        Text(
                            text = offer.upgrade.effectText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Ember,
                        )
                        Text(
                            text = offer.upgrade.flavor,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    PixelLabel(
                        text = Numbers.formatMass(offer.upgrade.cost),
                        color = if (offer.affordable) Positive else Muted,
                        size = 12,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- cosmos

@Composable
private fun CosmosPanel(
    state: GameState,
    stats: Stats,
    onCollapse: () -> Unit,
    updateSection: @Composable () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            PixelPanel(modifier = Modifier.fillMaxWidth(), border = Ember) {
                PixelLabel(text = "Kollaps", color = Ember, size = 16)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (stats.canCollapse) {
                        "Lass dein Schwarzes Loch in sich zusammenfallen. Du verlierst Masse, " +
                            "Kollektoren und Upgrades — behältst aber deine Singularitäten."
                    } else {
                        "Erreiche das Schwarze Loch, um zu kollabieren. Jeder Kollaps bringt " +
                            "Singularitäten, die jeden weiteren Durchlauf dauerhaft beschleunigen."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Jetzt zu holen: ${Numbers.format(stats.pendingSingularities)} Singularitäten " +
                        "(${Numbers.formatMultiplier(1.0 + GameEngine.SINGULARITY_BONUS * stats.pendingSingularities)} extra)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (stats.canCollapse) Positive else Muted,
                )
                Spacer(Modifier.height(12.dp))
                PixelButton(
                    label = if (confirming) "Wirklich kollabieren?" else "Kollabieren",
                    onClick = {
                        if (confirming) {
                            onCollapse()
                            confirming = false
                        } else {
                            confirming = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = stats.canCollapse,
                    accent = Ember,
                )
            }
        }

        item { SectionTitle("Dieser Durchlauf") }
        item { StatRow("Gesammelt", Numbers.formatMass(state.runMass)) }
        item { StatRow("Produktion", Numbers.formatRate(stats.massPerSecond)) }
        item { StatRow("Pro Tipp", Numbers.formatMass(stats.massPerTap)) }
        item { StatRow("Kollektoren", state.collectors.values.sum().toString()) }
        item { StatRow("Upgrades", "${state.upgrades.size} von ${Upgrades.all.size}") }

        item { SectionTitle("Insgesamt") }
        item { StatRow("Masse aller Zeiten", Numbers.formatMass(state.totalMass)) }
        item { StatRow("Tipps", state.taps.toString()) }
        item { StatRow("Kollapse", state.collapses.toString()) }
        item { StatRow("Singularitäten", Numbers.format(state.singularities)) }
        item { StatRow("Bonus daraus", Numbers.formatMultiplier(stats.singularityMultiplier)) }
        item { StatRow("Beste Stufe", Tiers.byIndex(state.bestTier).name) }
        item { StatRow("Bester Durchlauf", Numbers.formatMass(state.bestRunMass)) }

        item { SectionTitle("Wenn du weg bist") }
        item { StatRow("Offline-Ertrag", Numbers.formatPercent(stats.offlineEfficiency)) }
        item { StatRow("Offline-Grenze", Numbers.formatDuration(stats.offlineCapSeconds)) }

        item { Spacer(Modifier.height(8.dp)) }
        item { updateSection() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    PixelLabel(
        text = text,
        color = Nebula,
        size = 12,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Starlight)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}
