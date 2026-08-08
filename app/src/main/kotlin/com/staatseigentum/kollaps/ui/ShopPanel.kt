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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Automation
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.CollectorOffer
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Heavy
import com.staatseigentum.kollaps.core.HeavyElement
import com.staatseigentum.kollaps.core.Milestones
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Orbits
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.UpgradeGroup
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

/**
 * The shop's tabs, as things rather than as positions.
 *
 * Fusion only appears once the body has ignited, so the strip has to be able to change length —
 * and the moment it can, an integer index stops meaning the same tab from one state to the next.
 * Naming them lets [ShopPanel] hold on to which tab is open across an unlock instead of quietly
 * sliding the player one panel to the left.
 */
private enum class ShopTab(val title: String) {
    COLLECTORS("Kollektoren"),
    UPGRADES("Upgrades"),
    ORBITS("Bahnen"),
    FUSION("Fusion"),
    ACHIEVEMENTS("Erfolge"),
    COSMOS("Kosmos"),
}

/** The tabs worth showing for this state, in strip order. */
private fun tabsFor(state: GameState): List<ShopTab> =
    ShopTab.entries.filter {
        when (it) {
            ShopTab.ORBITS -> Orbits.isUnlocked(state)
            ShopTab.FUSION -> Fusion.isUnlocked(state)
            else -> true
        }
    }

@Composable
fun ShopPanel(
    state: GameState,
    stats: Stats,
    buyAmount: BuyAmount,
    actions: GameActions,
    modifier: Modifier = Modifier,
    /** Which tab the shop opens on. Only the harness passes anything else. */
    startTab: Int = 0,
    /** Which section of the Kosmos tab it opens on. Only the harness passes anything else. */
    startSection: Int = 0,
    /** The update section, handed in so the shop stays free of any networking concern. */
    updateSection: @Composable () -> Unit = {},
) {
    val tabs = tabsFor(state)
    // Saved by name so that reopening the app, or unlocking fusion mid-session, still lands on
    // the panel the player was actually looking at.
    var openTab by rememberSaveable {
        mutableStateOf(tabs.getOrElse(startTab) { ShopTab.COLLECTORS }.name)
    }
    val tab = ShopTab.entries.firstOrNull { it.name == openTab }
        ?.takeIf { it in tabs }
        ?: ShopTab.COLLECTORS

    Column(modifier = modifier.background(SpaceElevated)) {
        // A hard rule instead of an elevation shadow.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Outline),
        )

        // Scrolls rather than sharing the width six ways. Sharing was already tight at four —
        // "Kollektoren" is eleven characters — and a sixth tab would have cut two labels in half.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            tabs.forEach { entry ->
                PixelTab(
                    title = entry.title,
                    selected = tab == entry,
                    onClick = { openTab = entry.name },
                )
            }
        }

        when (tab) {
            ShopTab.COLLECTORS -> CollectorList(
                state = state,
                buyAmount = buyAmount,
                onBuyAmount = actions::setBuyAmount,
                onBuy = actions::buyCollector,
            )

            ShopTab.UPGRADES -> UpgradeList(
                offers = GameEngine.upgradeOffers(state),
                onBuy = actions::buyUpgrade,
            )

            ShopTab.ORBITS -> OrbitPanel(state = state, actions = actions)

            ShopTab.FUSION -> FusionPanel(
                state = state,
                buyAmount = buyAmount,
                actions = actions,
            )

            ShopTab.ACHIEVEMENTS -> AchievementList(state = state)

            ShopTab.COSMOS -> CosmosPanel(
                state = state,
                stats = stats,
                actions = actions,
                startSection = startSection,
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // A size smaller than the labels inside the panels: even scrolling, six of these want to
        // be readable at a glance rather than filling the strip.
        PixelLabel(
            text = title,
            color = if (selected) Starlight else Muted,
            size = 10,
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
                sfx?.purchase()
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
                // What the counter is counting towards. Without this the milestone bonus is a
                // number that changes on its own and never says why.
                if (offer.owned > 0) {
                    val next = offer.nextMilestoneAt
                    Text(
                        text = buildString {
                            if (offer.milestones > 0) {
                                append("${Numbers.formatMultiplier(Milestones.factor(offer.owned))} aus ")
                                append(if (offer.milestones == 1) "1 Meilenstein" else "${offer.milestones} Meilensteinen")
                            }
                            if (next != null) {
                                if (isNotEmpty()) append(" · ")
                                append("nächster bei $next")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Nebula,
                    )
                }
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

    // Null is "everything". Not saved across restarts on purpose: a filter the player set
    // yesterday and forgot is a shop that looks emptier than it is.
    var filter by remember { mutableStateOf<UpgradeGroup?>(null) }
    val counts = remember(offers) { offers.groupingBy { it.upgrade.group }.eachCount() }
    val shown = remember(offers, filter) {
        if (filter == null) offers else offers.filter { it.upgrade.group == filter }
    }

    Column {
        // Scrolls sideways rather than sharing the width five ways: "Kollektoren" needs about a
        // third of a phone at this size, and a label cut in half is worse than a swipe.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                label = "Alle ${offers.size}",
                selected = filter == null,
                onClick = { filter = null },
            )
            // Only groups that have something in them right now: a chip onto an empty list is a
            // promise the shop cannot keep.
            for (group in UpgradeGroup.entries) {
                val count = counts[group] ?: continue
                FilterChip(
                    label = "${group.label} $count",
                    selected = filter == group,
                    onClick = { filter = if (filter == group) null else group },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(shown, key = { it.upgrade.id }) { offer ->
            PixelPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = offer.affordable) {
                        sfx?.purchase()
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
}

/** A small pill for the upgrade filter. Selected is filled, unselected is outlined. */
@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val sfx = LocalSfx.current
    Box(
        modifier = Modifier
            .background(if (selected) Nebula else SpaceCard)
            .border(2.dp, if (selected) Nebula else Outline, RectangleShape)
            .clickable {
                sfx?.click()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        PixelLabel(
            text = label,
            color = if (selected) Starlight else Muted,
            size = 10,
        )
    }
}

// ---------------------------------------------------------------------- cosmos

/**
 * The four things the Kosmos tab is actually about.
 *
 * It used to be one scroll of twenty-two cards — the collapse, two shops, the big bang, the lab,
 * the automation rules, the challenges, a page of statistics, the settings and the updater, in
 * that order. Everything below the fold was found by accident, and the panel a player wanted was
 * never the one on screen. Four sections is the smallest split where each one answers a single
 * question: what do I do with a finished run, what is the lab doing, what runs without me, and
 * how is the app set up.
 */
private enum class CosmosSection(val title: String) {
    COLLAPSE("Kollaps"),
    LAB("Labor"),
    RULES("Regeln"),
    SYSTEM("System"),
}

/** Sections worth offering for this state. Empty ones would be a tab onto a blank page. */
private fun sectionsFor(state: GameState, stats: Stats): List<CosmosSection> =
    CosmosSection.entries.filter {
        when (it) {
            CosmosSection.COLLAPSE, CosmosSection.SYSTEM -> true
            CosmosSection.LAB -> ResearchTree.isUnlocked(state)
            CosmosSection.RULES ->
                Automation.isUnlocked(state) || state.collapses > 0 || stats.challenge != null
        }
    }

@Composable
private fun CosmosPanel(
    state: GameState,
    stats: Stats,
    actions: GameActions,
    startSection: Int,
    updateSection: @Composable () -> Unit,
) {
    val sections = sectionsFor(state, stats)
    // By name, for the same reason the tab strip above is: the list grows as things unlock, and
    // an index would quietly move the player to a different page the moment it does.
    var openSection by rememberSaveable {
        mutableStateOf(sections.getOrElse(startSection) { CosmosSection.COLLAPSE }.name)
    }
    val section = CosmosSection.entries.firstOrNull { it.name == openSection }
        ?.takeIf { it in sections }
        ?: CosmosSection.COLLAPSE

    Column {
        // A second strip, a size smaller than the one above it, so the hierarchy reads without
        // a line or a label saying which belongs to which.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceCard),
        ) {
            sections.forEach { entry ->
                CosmosChip(
                    title = entry.title,
                    selected = section == entry,
                    onClick = { openSection = entry.name },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (section) {
                CosmosSection.COLLAPSE -> {
                    item {
                        CollapseCard(
                            state = state,
                            stats = stats,
                            onCollapse = actions::collapse,
                        )
                    }

                    // Only worth showing once there is something to spend, and something to
                    // spend it on.
                    if (state.collapses > 0 || state.singularities > 0.0) {
                        item { PrestigeShop(state = state, onBuy = actions::buyPrestigeUpgrade) }
                        item { InvestmentPanel(state = state, actions = actions) }
                    }

                    if (stats.bigBangUnlocked) {
                        item {
                            BigBangPanel(
                                state = state,
                                stats = stats,
                                onBigBang = actions::bigBang,
                                onBuy = actions::buyAeonUpgrade,
                            )
                        }
                    }
                }

                CosmosSection.LAB -> item { ResearchPanel(state = state, actions = actions) }

                CosmosSection.RULES -> {
                    if (Automation.isUnlocked(state)) {
                        item { AutomationPanel(state = state, actions = actions) }
                    }
                    if (state.collapses > 0 || stats.challenge != null) {
                        item {
                            ChallengePanel(
                                state = state,
                                stats = stats,
                                onStart = actions::startChallenge,
                                onAbort = actions::abortChallenge,
                                onFinish = actions::finishChallenge,
                            )
                        }
                    }
                }

                CosmosSection.SYSTEM -> {
                    item { SectionTitle("Dieser Durchlauf") }
                    item { StatRow("Gesammelt", Numbers.formatMass(state.runMass)) }
                    item { StatRow("Produktion", Numbers.formatRate(stats.massPerSecond)) }
                    item { StatRow("Pro Tipp", Numbers.formatMass(stats.massPerTap)) }
                    item { StatRow("Kollektoren", state.collectors.values.sum().toString()) }
                    item { StatRow("Upgrades", "${state.upgrades.size} von ${Upgrades.all.size}") }
                    item { StatRow("Beste Stufe", Tiers.byIndex(state.bestTier).name) }
                    item {
                        StatRow(
                            "Bonus aus Singularitäten",
                            Numbers.formatMultiplier(stats.singularityMultiplier),
                        )
                    }

                    item { SectionTitle("Wenn du weg bist") }
                    item { StatRow("Offline-Ertrag", Numbers.formatPercent(stats.offlineEfficiency)) }
                    item {
                        StatRow("Offline-Grenze", Numbers.formatDuration(stats.offlineCapSeconds))
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                    item {
                        SettingsSection(
                            state = state,
                            stats = stats,
                            onSound = actions::setSound,
                            onHaptics = actions::setHaptics,
                            onMusic = actions::setMusic,
                            onAutoBuy = actions::setAutoBuy,
                            onReminders = actions::setReminders,
                            onExport = actions::exportSave,
                            onImport = actions::importSave,
                            onErase = actions::eraseSave,
                        )
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                    item { updateSection() }
                }
            }
        }
    }
}

/** The second-level tab: shorter, quieter, and underlined rather than filled. */
@Composable
private fun CosmosChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sfx = LocalSfx.current
    Column(
        modifier = modifier
            .clickable {
                sfx?.click()
                onClick()
            }
            .padding(top = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLabel(
            text = title,
            color = if (selected) Ember else Muted,
            size = 10,
        )
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) Ember else Outline),
        )
    }
}

@Composable
private fun CollapseCard(state: GameState, stats: Stats, onCollapse: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    val forged = remember(state) { Heavy.pending(state) }

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
        // Only once there is iron in the core. Before that the line would be an empty promise
        // about a system the player has not switched on yet.
        if (forged.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Im Zusammenbruch entsteht: " + HeavyElement.entries
                    .mapNotNull { element ->
                        forged[element.id]?.let { "${Numbers.format(it)} ${element.label}" }
                    }
                    .joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
            )
        }

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
