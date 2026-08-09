package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.AeonUpgrades
import com.staatseigentum.kollaps.core.BigBang
import com.staatseigentum.kollaps.core.PathTrees
import com.staatseigentum.kollaps.core.PathNode
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Path
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The second reset, and the shop it feeds.
 *
 * Deliberately blunt about what it costs. The collapse takes a run; this takes every collapse the
 * player has ever made, and a button that hid that behind "Neuanfang" would be a trick.
 */
@Composable
fun BigBangPanel(
    state: GameState,
    stats: Stats,
    onBigBang: (String) -> Unit,
    onBuy: (String) -> Unit,
    onBuyPathNode: (String) -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }

    PixelPanel(
        modifier = Modifier.fillMaxWidth().sog(SogDepth.CONTAINER, Nebula),
        border = Nebula,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelLabel("Urknall", color = Nebula, size = 16)
            if (state.aeons > 0.0) {
                PixelLabel("${Numbers.format(state.aeons)} Äonen", color = Ember, size = 13)
            }
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Wirf alles weg, was deine Kollapse aufgebaut haben: Singularitäten, " +
                "Prestige-Upgrades, den Zähler selbst. Was bleibt, sind Erfolge, bestandene " +
                "Herausforderungen — und Äonen.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        Spacer(Modifier.height(10.dp))

        if (state.bigBangs > 0) {
            Text(
                text = "Bisher ${state.bigBangs}× ausgelöst.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(6.dp))
        }

        Text(
            text = if (stats.canBigBang) {
                "Jetzt zu holen: ${Numbers.format(stats.pendingAeons)} Äonen"
            } else {
                "Ab ${BigBang.REQUIRED_COLLAPSES} Kollapsen. Du bist bei ${state.collapses}."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (stats.canBigBang) Positive else Muted,
        )
        Path.of(state)?.let { running ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dieses Universum: ${running.label}",
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!confirming) {
            PixelButton(
                label = if (stats.canBigBang) "Urknall auslösen" else "Noch nicht so weit",
                onClick = { confirming = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = stats.canBigBang,
                accent = Ember,
            )
        } else {
            // The confirmation *is* the choice. A separate "really?" followed by a picker would
            // be two dialogs for one decision, and picking a universe to live in is a better
            // second thought than a yes-or-no about a button already pressed once.
            PixelLabel("Was für ein Universum?", color = Ember, size = 13)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Die Ausrichtung gilt, bis du das nächste Mal alles wegwirfst.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(8.dp))

            for (path in Path.entries) {
                PathChoice(
                    path = path,
                    onChoose = {
                        onBigBang(path.id)
                        confirming = false
                    },
                )
                Spacer(Modifier.height(6.dp))
            }

            PixelButton(
                label = "Doch nicht",
                onClick = { confirming = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // The tree belongs to the universe that is running, so it only appears once one is aligned.
    val tree = PathTrees.current(state)
    if (tree.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        PathTreePanel(state = state, nodes = tree, onBuy = onBuyPathNode)
    }

    // The shop only means anything once there is something in the purse.
    if (state.aeons > 0.0 || state.aeonUpgrades.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        AeonShop(state = state, onBuy = onBuy)
    }
}

/**
 * The tree of the universe that is running.
 *
 * Drawn as a root with three indented leaves rather than as a graph: the shape is one level deep,
 * and an indent says "this needs the one above" with no lines to draw and nothing to misread.
 *
 * Nodes bought under a different path are not shown at all. They are still in the save, and the
 * player will see them again the next time they align a universe that way — which is the point of
 * the whole thing, and a list of four greyed-out trees would bury it.
 */
@Composable
private fun PathTreePanel(state: GameState, nodes: List<PathNode>, onBuy: (String) -> Unit) {
    val path = Path.of(state) ?: return
    val owned = PathTrees.ownedIn(state, state.path)

    PixelPanel(modifier = Modifier.fillMaxWidth(), border = Ember) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelLabel(path.label, color = Ember, size = 15)
            PixelLabel("$owned/${nodes.size}", color = Muted, size = 13)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Gekauft bleibt gekauft — wirkt aber nur, solange das Universum so " +
                "ausgerichtet ist. Ein anderer Urknall legt das hier schlafen, kein Urknall " +
                "nimmt es weg.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(8.dp))

        for (node in nodes) {
            val bought = node.id in state.pathNodes
            val locked = node.requires != null && node.requires !in state.pathNodes
            val affordable = state.aeons >= node.cost

            PixelPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (node.requires == null) 0.dp else 14.dp, bottom = 6.dp),
                border = when {
                    bought -> Positive
                    locked -> Outline
                    affordable -> Ember
                    else -> Outline
                },
                padding = 10,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (locked && !bought) Muted else Starlight,
                        )
                        Text(
                            text = node.effectText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (locked && !bought) Muted else Ember,
                        )
                        Text(
                            text = node.flavor,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        bought -> PixelLabel("gekauft", color = Positive, size = 12)
                        locked -> PixelLabel("gesperrt", color = Muted, size = 12)
                        else -> PixelButton(
                            label = Numbers.format(node.cost),
                            onClick = { onBuy(node.id) },
                            enabled = affordable,
                            accent = Ember,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AeonShop(state: GameState, onBuy: (String) -> Unit) {
    val offered = BigBang.offered(state)

    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        PixelLabel("Äonen ausgeben", size = 15)
        Spacer(Modifier.height(8.dp))

        if (offered.isEmpty()) {
            Text(
                text = "Alle ${AeonUpgrades.all.size} gekauft. Von hier aus geht es nur noch " +
                    "durch Spielen weiter.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            return@PixelPanel
        }

        for (upgrade in offered) {
            val affordable = state.aeons >= upgrade.cost
            PixelPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                border = if (affordable) Positive else Outline,
                padding = 10,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            upgrade.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Starlight,
                        )
                        Text(
                            upgrade.effectText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Ember,
                        )
                        Text(
                            upgrade.flavor,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    PixelButton(
                        label = Numbers.format(upgrade.cost),
                        onClick = { onBuy(upgrade.id) },
                        enabled = affordable,
                        accent = Nebula,
                    )
                }
            }
        }
    }
}

/** One universe on offer: what it is called, what it leans towards, and one tap to live in it. */
@Composable
private fun PathChoice(path: Path, onChoose: () -> Unit) {
    val sfx = LocalSfx.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceCard)
            .border(2.dp, Outline, RectangleShape)
            .clickable {
                sfx?.purchase()
                onChoose()
            }
            .padding(10.dp),
    ) {
        Column {
            Text(
                text = path.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Starlight,
            )
            Text(
                text = path.flavor,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(4.dp))
            for (line in path.effectTexts) {
                Text(
                    text = "· $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = Positive,
                )
            }
        }
    }
}
