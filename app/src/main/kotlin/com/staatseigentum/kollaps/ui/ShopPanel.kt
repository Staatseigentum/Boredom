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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Automation
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.Collector
import com.staatseigentum.kollaps.core.Contract
import com.staatseigentum.kollaps.core.CollectorOffer
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Heavy
import com.staatseigentum.kollaps.core.HeavyElement
import com.staatseigentum.kollaps.core.Milestones
import com.staatseigentum.kollaps.core.Alloy
import com.staatseigentum.kollaps.core.Multiverse
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Orbits
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Roles
import com.staatseigentum.kollaps.core.Statistics
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
enum class ShopTab(val title: String) {
    COLLECTORS("Flotte"),
    UPGRADES("Upgrades"),
    AUFBAU("Aufbau"),
    ORBITS("Bahnen"),
    FUSION("Fusion"),
    COSMOS("Kosmos"),
}

/**
 * The tabs worth showing for this state, in strip order.
 *
 * Five where there were six: the achievements moved into the Kosmos panel as a section of their
 * own. They are a record of what has been done rather than somewhere to spend anything, which is
 * what every other Kosmos section is — and it is the tab nobody opened twice in a session, holding
 * a sixth of a strip that both shells needed to get down to four or five.
 */
private fun tabsFor(state: GameState): List<ShopTab> =
    ShopTab.entries.filter {
        when (it) {
            ShopTab.AUFBAU -> aufbauAvailable(state)
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
    /** The save slots, handed in for the same reason: the files are the platform's business. */
    saveSlots: @Composable () -> Unit = {},
    /**
     * Show one tab and no strip, because something outside has already chosen.
     *
     * This is what the phone's bottom bar hands in. Two levels of navigation for one list — a bar
     * to reach the shop and a strip inside it to reach a tab — is one level too many on a screen
     * that has room for neither, so on a phone the bar *is* the strip and the panel is told what to
     * show. The collectors are a special case even then; see [FleetPanel].
     */
    pinned: ShopTab? = null,
) {
    val tabs = tabsFor(state)
    // Saved by name so that reopening the app, or unlocking fusion mid-session, still lands on
    // the panel the player was actually looking at.
    var openTab by rememberSaveable {
        mutableStateOf(tabs.getOrElse(startTab) { ShopTab.COLLECTORS }.name)
    }
    val tab = pinned ?: ShopTab.entries.firstOrNull { it.name == openTab }
        ?.takeIf { it in tabs }
        ?: ShopTab.COLLECTORS

    // The outermost of the three levels. Nothing clips this one, which is exactly why the whole
    // panel is what makes the journey into the hole — the cards inside it live in a scrolling
    // list that would cut them off at its own edge.
    Column(modifier = modifier.background(SpaceElevated).sog(SogDepth.SHELL, Nebula)) {
        // A hard rule instead of an elevation shadow.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Outline),
        )

        // Scrolls rather than sharing the width. Sharing was already tight at four — and the
        // strip is not drawn at all when something outside has already chosen the tab.
        if (pinned == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    // As one piece: a tab strip whose tabs each fell in on their own would read as
                    // the interface coming apart before it is pulled, which is the next phase's job.
                    .sog(SogDepth.CONTAINER, Nebula)
                    .urknall(Nebula),
            ) {
                tabs.forEach { entry ->
                    PixelTab(
                        title = entry.title,
                        selected = tab == entry,
                        onClick = { openTab = entry.name },
                    )
                }
            }
        }

        when (tab) {
            // Pinned, the collectors carry the upgrades with them behind a switch — that is the
            // one place the phone's bar is not enough on its own.
            ShopTab.COLLECTORS -> if (pinned == null) {
                CollectorList(
                    state = state,
                    buyAmount = buyAmount,
                    onBuyAmount = actions::setBuyAmount,
                    onBuy = actions::buyCollector,
                    onCycleRole = actions::cycleRole,
                )
            } else {
                FleetPanel(state = state, buyAmount = buyAmount, actions = actions)
            }

            ShopTab.UPGRADES -> UpgradeList(
                offers = GameEngine.upgradeOffers(state),
                onBuy = actions::buyUpgrade,
            )

            ShopTab.AUFBAU -> AufbauPanel(state = state, actions = actions)

            ShopTab.ORBITS -> OrbitPanel(state = state, actions = actions)

            ShopTab.FUSION -> FusionPanel(
                state = state,
                buyAmount = buyAmount,
                actions = actions,
            )

            ShopTab.COSMOS -> CosmosPanel(
                state = state,
                stats = stats,
                actions = actions,
                startSection = startSection,
                updateSection = updateSection,
                saveSlots = saveSlots,
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

/**
 * The machines and what makes them better, under one heading.
 *
 * Only on a phone, where the bottom bar has five places and upgrades would be a sixth. The switch
 * is two segments rather than two tabs, and they sit flush against each other — a segmented control
 * says "two halves of one thing", which is exactly the relationship, where two tabs would say "two
 * places" and put the player back where they started.
 */
@Composable
private fun FleetPanel(state: GameState, buyAmount: BuyAmount, actions: GameActions) {
    var upgrades by rememberSaveable { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val offers = remember(state) { GameEngine.upgradeOffers(state) }

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            listOf(false to "Flotte", true to "Upgrades ${offers.count { it.affordable }}")
                .forEach { (isUpgrades, title) ->
                    val selected = upgrades == isUpgrades
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selected) Nebula else SpaceCard)
                            .clickable {
                                sfx?.click()
                                upgrades = isUpgrades
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PixelLabel(
                            text = title,
                            color = if (selected) Starlight else Muted,
                            size = 11,
                        )
                    }
                }
        }

        if (upgrades) {
            UpgradeList(offers = offers, onBuy = actions::buyUpgrade)
        } else {
            CollectorList(
                state = state,
                buyAmount = buyAmount,
                onBuyAmount = actions::setBuyAmount,
                onBuy = actions::buyCollector,
                onCycleRole = actions::cycleRole,
            )
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
    onCycleRole: (String) -> Unit,
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

        if (Roles.isUnlocked(state)) {
            Text(
                text = "Ausrichtungen: ${Roles.assignedCount(state)} von ${Roles.slots(state)} " +
                    "belegt — tippe auf die Zahl links, um eine zu vergeben.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(6.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(offers, key = { it.collector.id }) { offer ->
                CollectorRow(
                    state = state,
                    offer = offer,
                    onBuy = { onBuy(offer.collector.id) },
                    onCycleRole = { onCycleRole(offer.collector.id) },
                )
            }
        }
    }
}

@Composable
private fun CollectorRow(
    state: GameState,
    offer: CollectorOffer,
    onBuy: () -> Unit,
    onCycleRole: () -> Unit,
) {
    val enabled = offer.affordable
    val sfx = LocalSfx.current
    val role = Roles.roleOf(state, offer.collector.id)
    val roleTappable = Roles.isUnlocked(state) && offer.everBought
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
            // The count badge doubles as the role control. Its own tap target inside a row that
            // is itself tappable: buying and setting up are different intentions, and a long
            // press would hide the second one behind a gesture nobody discovers.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (offer.everBought) SpaceElevated else SpaceCard)
                    .border(2.dp, if (role != null) Nebula else Outline, RectangleShape)
                    .clickable(enabled = roleTappable) {
                        sfx?.click()
                        onCycleRole()
                    },
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
                /*
                 * One line about output, not two.
                 *
                 * A row used to be able to reach four lines — name, output, role, milestones —
                 * and ten of those is a wall of text on a phone with nothing to hold on to. Output
                 * and the milestone bonus are the same sentence anyway: what this machine makes,
                 * and why it makes that much. The role stays on its own line, because it is the
                 * one thing on the row the player set by hand.
                 */
                Text(
                    text = if (offer.owned > 0) {
                        buildString {
                            append("liefert ${Numbers.formatRate(offer.output)}")
                            if (offer.milestones > 0) {
                                append(" · ")
                                append(Numbers.formatMultiplier(Milestones.factor(offer.owned)))
                            }
                            offer.nextMilestoneAt?.let { append(" · nächster bei $it") }
                        }
                    } else {
                        offer.collector.flavor
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                offer.lockedReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = Ember,
                    )
                }
                if (role != null) {
                    Text(
                        text = "${role.label}: ${role.effectText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Nebula,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                // A price of zero for something that cannot be bought would read as free. At the
                // cap the row says so instead, the same way a full investment does.
                val full = offer.owned >= Collector.MAX_OWNED
                PixelLabel(
                    // A rule beats the cap beats the price. Naming the rule matters most: the
                    // other two are states the player can see coming, and this one is a door.
                    text = when {
                        offer.lockedReason != null -> "gesperrt"
                        full -> "voll"
                        else -> Numbers.formatMass(offer.cost)
                    },
                    color = when {
                        full -> Nebula
                        enabled -> Positive
                        else -> Muted
                    },
                    size = 12,
                )
                if (!full && offer.amount > 1) {
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
    // yesterday and forgot is a shop that looks emptier than it is. The same goes for the query.
    var filter by remember { mutableStateOf<UpgradeGroup?>(null) }
    var query by remember { mutableStateOf("") }

    // The search narrows first and the chips count what is left, so the numbers on them describe
    // the list the player is actually looking at rather than one the search has already ruled out.
    val found = remember(offers, query) {
        if (query.isBlank()) offers else offers.filter { it.upgrade.matches(query) }
    }
    val counts = remember(found) { found.groupingBy { it.upgrade.group }.eachCount() }
    val shown = remember(found, filter) {
        if (filter == null) found else found.filter { it.upgrade.group == filter }
    }

    // A chip whose group the search has emptied would leave the player staring at nothing with a
    // filter selected that they can no longer see the reason for. Handled here rather than in the
    // field's callback, where `counts` would still describe the list from before the keystroke.
    LaunchedEffect(counts) {
        if (filter != null && counts[filter] == null) filter = null
    }

    Column {
        SearchField(query = query, onQueryChange = { query = it })

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
                label = "Alle ${found.size}",
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

        // Only reachable through the search — the group chips never offer an empty shelf.
        if (shown.isEmpty()) {
            EmptyHint("Nichts gefunden für „$query“.\nEs wird in Name, Wirkung und Beschreibung gesucht.")
            return@Column
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

/**
 * The search box over the upgrade list.
 *
 * A single line with its own clear button rather than a text field from the material set: the rest
 * of this screen is drawn in flat rectangles with a two-pixel border, and a rounded outlined field
 * with a floating label would be the one control that came from somewhere else.
 *
 * The list is a hundred and twenty-eight rows deep by the end of a run, which is the point at
 * which chips alone stop being enough to find the one thing you half remember.
 */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val sfx = LocalSfx.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp)
            .background(SpaceCard)
            .border(2.dp, if (query.isBlank()) Outline else Nebula, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Upgrade suchen …",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Starlight),
                cursorBrush = SolidColor(Ember),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.clickable {
                    sfx?.click()
                    onQueryChange("")
                },
            ) {
                PixelLabel(text = "×", color = Ember, size = 15)
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
internal enum class CosmosSection(val title: String) {
    COLLAPSE("Kollaps"),
    SKY("Himmel"),
    LAB("Labor"),
    RULES("Regeln"),
    ACHIEVEMENTS("Erfolge"),
    SYSTEM("System"),
}

/** Sections worth offering for this state. Empty ones would be a tab onto a blank page. */
internal fun sectionsFor(state: GameState, stats: Stats): List<CosmosSection> =
    CosmosSection.entries.filter {
        when (it) {
            CosmosSection.COLLAPSE, CosmosSection.SYSTEM, CosmosSection.ACHIEVEMENTS -> true
            // Only once there is a sky at all. Before the first big bang this would be a tab onto
            // an empty ring, which promises nothing and explains less.
            CosmosSection.SKY -> Multiverse.isUnlocked(state)
            CosmosSection.LAB -> ResearchTree.isUnlocked(state)
            CosmosSection.RULES ->
                Automation.isUnlocked(state) || state.collapses > 0 || stats.challenge != null ||
                    Contract.isUnlocked(state)
        }
    }

@Composable
internal fun CosmosPanel(
    state: GameState,
    stats: Stats,
    actions: GameActions,
    startSection: Int,
    updateSection: @Composable () -> Unit,
    saveSlots: @Composable () -> Unit,
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
        /*
         * Scrolls, and the chips are as wide as their words.
         *
         * They used to share the width equally, which was written when this strip had three
         * entries. There are six, and on a phone that is sixty-eight points each — narrower than
         * the word ERFOLGE in the display face, so the label wrapped, the cell grew to two lines,
         * and the whole strip went with it. The section holding the contracts sat in the middle of
         * that, which is why they were hard to get at on a phone at all.
         *
         * The strip above this one has scrolled since it reached four tabs, for exactly the same
         * reason and with the same note against it.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceCard)
                .horizontalScroll(rememberScrollState())
                .sog(SogDepth.CONTAINER, Nebula)
                .urknall(Nebula),
        ) {
            sections.forEach { entry ->
                CosmosChip(
                    title = entry.title,
                    selected = section == entry,
                    onClick = { openSection = entry.name },
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
                                onBuyPathNode = actions::buyPathNode,
                            )
                        }
                    }
                }

                CosmosSection.SKY -> {
                    item { GalaxyPanel(state = state, actions = actions) }
                    if (Alloy.isUnlocked(state)) {
                        item { AlloyPanel(state = state, actions = actions) }
                    }
                }

                CosmosSection.LAB -> item { ResearchPanel(state = state, actions = actions) }

                CosmosSection.RULES -> {
                    // First on the page: it is the only thing here that answers "what now", and
                    // everything below it answers "how".
                    if (Contract.isUnlocked(state)) {
                        item { ContractPanel(state = state, actions = actions) }
                    }
                    if (Automation.isUnlocked(state)) {
                        item { AutomationPanel(state = state, actions = actions) }
                    }
                    if (state.collapses > 0 || stats.challenge != null) {
                        item {
                            ChallengePanel(
                                state = state,
                                stats = stats,
                                onStart = actions::startChallenges,
                                onAbort = actions::abortChallenge,
                                onFinish = actions::finishChallenge,
                            )
                        }
                    }
                }

                // Moved here from a tab of its own. The list already scrolls and already carries
                // the palette picker, so it needs nothing from a strip position it was only
                // holding because it had always held it.
                CosmosSection.ACHIEVEMENTS -> {
                    achievementItems(state = state, onPickSkin = actions::setSkin)
                }

                CosmosSection.SYSTEM -> {
                    item { SectionTitle("Dieser Durchlauf") }
                    item { StatRow("Gesammelt", Numbers.formatMass(state.runMass)) }
                    item { StatRow("Produktion", Numbers.formatRate(stats.massPerSecond)) }
                    item { StatRow("Pro Tipp", Numbers.formatMass(stats.massPerTap)) }
                    item { StatRow("Kollektoren", state.collectors.values.sum().toString()) }
                    item { StatRow("Upgrades", "${state.upgrades.size} von ${Upgrades.all.size}") }
                    // `label` and not `name`: on the catalogue ladder the name is the bare body and
                    // the designation is the whole of what distinguishes one rung from the six
                    // hundred and seventy-five others that share it.
                    item { StatRow("Beste Stufe", Tiers.byIndex(state.bestTier).label) }
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
                            onStatus = actions::setStatus,
                            onNumberFormat = actions::setNumberFormat,
                            onExport = actions::exportSave,
                            onImport = actions::importSave,
                            onErase = actions::eraseSave,
                        )
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                    item { saveSlots() }

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
            // Its own width now that the strip scrolls, with enough on either side of the word to
            // make the whole cell a target rather than the seven letters in the middle of it.
            .padding(top = 11.dp, start = 14.dp, end = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLabel(
            text = title,
            color = if (selected) Ember else Muted,
            size = 12,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
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

    // Two depths on one card, which is the whole idea: the lines implode into the card, and a
    // quarter of a second later the card follows them into the hole.
    PixelPanel(
        modifier = Modifier.fillMaxWidth().sog(SogDepth.CONTAINER, Ember),
        border = Ember,
    ) {
        PixelLabel(
            text = "Kollaps",
            color = Ember,
            size = 16,
            modifier = Modifier.sog(SogDepth.CONTENT, Ember),
        )
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
            modifier = Modifier.sog(SogDepth.CONTENT, Muted),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Jetzt zu holen: ${Numbers.format(stats.pendingSingularities)} Singularitäten " +
                "(${Numbers.formatMultiplier(1.0 + GameEngine.SINGULARITY_BONUS * stats.pendingSingularities)} extra)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (stats.canCollapse) Positive else Muted,
            modifier = Modifier.sog(SogDepth.CONTENT, Positive),
        )
        // What waiting would pay, next to what pressing now pays.
        //
        // Only while the button is live, and only while the projection is actually higher — at the
        // very top of a long run the two round to the same number, and a line that says "in zwanzig
        // Minuten: dasselbe" is noise pretending to be advice.
        if (stats.canCollapse) {
            val soon = remember(state) { GameEngine.singularitiesIn(state, 20 * 60.0) }
            if (soon > stats.pendingSingularities) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "In zwanzig Minuten: ${Numbers.format(soon)} — " +
                        "letzter Lauf: ${Numbers.format(state.lastRunSingularities)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.sog(SogDepth.CONTENT, Muted),
                )
            }
        }

        // Where the run stands, next to the button that ends it. Only after the first collapse:
        // before that there is nothing to be faster than.
        Statistics.standing(state)?.let { standing ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = standing,
                style = MaterialTheme.typography.bodySmall,
                color = Nebula,
                modifier = Modifier.sog(SogDepth.CONTENT, Nebula).urknall(Nebula),
            )
        }

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
            modifier = Modifier.fillMaxWidth().sog(SogDepth.CONTENT, Ember),
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
