package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
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
        modifier = Modifier
            .fillMaxWidth()
            .sog(SogDepth.CONTAINER, Nebula)
            .urknall(Nebula),
        border = Nebula,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().urknall(Nebula),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelLabel(Lang.t("Urknall"), color = Nebula, size = 16)
            if (state.aeons > 0.0) {
                PixelLabel(Lang.t("%s Äonen", Numbers.format(state.aeons)), color = Ember, size = 13)
            }
        }
        Spacer(Modifier.height(8.dp))

        /*
         * The long description steps aside while the choice is open.
         *
         * Four cards, each with a name, a line of flavour and up to three effects, is already
         * more than fits on a phone; a paragraph explaining what a big bang is on top of that
         * pushes the last card off the bottom. And by the time the picker is open the paragraph
         * has done its job — the player has read it and pressed the button.
         */
        if (!confirming) {
            Text(
                text = Lang.t("Wirf alles weg, was deine Kollapse aufgebaut haben: Singularitäten, Prestige-Upgrades, den Zähler selbst. Was bleibt, sind Erfolge, bestandene Herausforderungen — und Äonen."),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.urknall(Muted),
            )
            Spacer(Modifier.height(10.dp))

            if (state.bigBangs > 0) {
                Text(
                    text = Lang.t("Bisher %s× ausgelöst.", state.bigBangs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.urknall(Muted),
                )
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text = if (stats.canBigBang) {
                    Lang.t("Jetzt zu holen: %s Äonen", Numbers.format(stats.pendingAeons))
                } else {
                    // `requiredNow` and not `REQUIRED_COLLAPSES`. The constant is what the *first*
                    // big bang costs; every one after it asks for three more. The card printed the
                    // constant for ever, so after one big bang it read "Ab 10 Kollapsen. Du bist
                    // bei 12" next to a button that refused — the card said yes and the rules said
                    // no, and the rules were right.
                    Lang.t("Ab %s Kollapsen. Du bist bei %s.", BigBang.requiredNow(state), state.collapses)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (stats.canBigBang) Positive else Muted,
                modifier = Modifier.urknall(if (stats.canBigBang) Positive else Muted),
            )
            Path.of(state)?.let { running ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Lang.t("Dieses Universum: %s", running.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ember,
                    modifier = Modifier.urknall(Ember),
                )
            }
        } else {
            // The confirmation *is* the choice. A separate "really?" followed by a picker would
            // be two dialogs for one decision, and picking a universe to live in is a better
            // second thought than a yes-or-no about a button already pressed once.
            PixelLabel(
                Lang.t("Was für ein Universum?"),
                color = Ember,
                size = 13,
                modifier = Modifier.urknall(Ember),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Lang.t("Die Ausrichtung gilt, bis du das nächste Mal alles wegwirfst."),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.urknall(Muted),
            )
            Spacer(Modifier.height(8.dp))

            for (path in Path.entries) {
                PathChoice(
                    path = path,
                    onChoose = {
                        // Closed in the same frame the sequence starts, with no transition of its
                        // own: whatever is on screen at that moment is what gets pressed flat, and
                        // a picker fading out through the flattening would be two animations
                        // arguing over the same pixels.
                        confirming = false
                        onBigBang(path.id)
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // One button that changes what it says, rather than a second one below the cards. The
        // way back out of a decision belongs where the way in was.
        PixelButton(
            label = when {
                confirming -> Lang.t("Doch nicht")
                stats.canBigBang -> Lang.t("Urknall auslösen")
                else -> Lang.t("Noch nicht so weit")
            },
            onClick = { confirming = !confirming },
            modifier = Modifier.fillMaxWidth().urknall(Nebula),
            enabled = stats.canBigBang,
            accent = Ember,
        )
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
            PixelLabel(Lang.t("%s/%s", owned, nodes.size), color = Muted, size = 13)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = Lang.t("Gekauft bleibt gekauft — wirkt aber nur, solange das Universum so ausgerichtet ist. Ein anderer Urknall legt das hier schlafen, kein Urknall nimmt es weg."),
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
                        bought -> PixelLabel(Lang.t("gekauft"), color = Positive, size = 12)
                        locked -> PixelLabel(Lang.t("gesperrt"), color = Muted, size = 12)
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
        PixelLabel(Lang.t("Äonen ausgeben"), size = 15)
        Spacer(Modifier.height(8.dp))

        if (offered.isEmpty()) {
            Text(
                text = Lang.t(
                    "Alle %s gekauft. Von hier aus geht es nur noch durch Spielen weiter.",
                    AeonUpgrades.all.size,
                ),
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
            .padding(10.dp)
            .urknall(Nebula),
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
                    text = Lang.t("· %s", line),
                    style = MaterialTheme.typography.bodySmall,
                    color = Positive,
                )
            }
        }
    }
}
