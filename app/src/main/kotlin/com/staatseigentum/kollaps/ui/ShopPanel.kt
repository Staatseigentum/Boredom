package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated

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
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Surface(
        modifier = modifier,
        color = SpaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
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
                )
            }
        }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BuyAmount.entries.forEach { amount ->
                FilterChip(
                    selected = buyAmount == amount,
                    onClick = { onBuyAmount(amount) },
                    label = { Text(amount.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onBuy),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (offer.everBought) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = offer.owned.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (offer.everBought) Ember else Muted,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.collector.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
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
                Text(
                    text = Numbers.formatMass(offer.cost),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) Positive else Muted,
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
    if (offers.isEmpty()) {
        EmptyHint(
            "Gerade nichts zu verbessern.\nKauf weitere Kollektoren, dann tauchen hier neue Upgrades auf.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(offers, key = { it.upgrade.id }) { offer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = offer.affordable) { onBuy(offer.upgrade.id) },
                colors = CardDefaults.cardColors(containerColor = SpaceCard),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = offer.upgrade.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
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
                    Text(
                        text = Numbers.formatMass(offer.upgrade.cost),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (offer.affordable) Positive else Muted,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- cosmos

@Composable
private fun CosmosPanel(state: GameState, stats: Stats, onCollapse: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SpaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Kollaps",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ember,
                    )
                    Spacer(Modifier.height(6.dp))
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
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Jetzt zu holen: ${Numbers.format(stats.pendingSingularities)} Singularitäten " +
                            "(${Numbers.formatMultiplier(1.0 + GameEngine.SINGULARITY_BONUS * stats.pendingSingularities)} extra)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (stats.canCollapse) Positive else Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (confirming) {
                                onCollapse()
                                confirming = false
                            } else {
                                confirming = true
                            }
                        },
                        enabled = stats.canCollapse,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (confirming) "Wirklich kollabieren?" else "Kollabieren")
                    }
                }
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
        item {
            StatRow("Offline-Grenze", Numbers.formatDuration(stats.offlineCapSeconds))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = Muted,
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
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
