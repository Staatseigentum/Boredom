package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Accretion
import com.staatseigentum.kollaps.core.Depth
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Lane
import com.staatseigentum.kollaps.core.Material
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Shell
import com.staatseigentum.kollaps.core.Shells
import com.staatseigentum.kollaps.core.Worlds
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.Space
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight
import com.staatseigentum.kollaps.ui.theme.Unreached

/**
 * The body, as something that is built rather than only grown.
 *
 * Three panels in one, in the order the player meets them: what is lying about, what it can be
 * built into, and what the building has made this body *be*. They are one panel and not three tabs
 * because none of them means anything without the other two — a stock of ice is a number until you
 * can see it is two short of the next crust, and a crust is a bonus until you can see that four
 * more of them makes this an ice world you have not been yet.
 */
@Composable
fun AufbauPanel(
    state: GameState,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { MaterialStock(state) }
        item { ArrivalNote(state) }
        for (shell in Shell.entries) {
            item(key = shell.id) {
                ShellRow(
                    state = state,
                    shell = shell,
                    onBuild = { actions.buildShell(shell.id) },
                )
            }
        }
        item { WorldRecord(state) }
    }
}

// ---------------------------------------------------------------------- stock

/**
 * What is lying on the surface.
 *
 * Four chips rather than a list, and always all four even when three are empty: the missing ones
 * are the point. A player looking at a crust they cannot afford needs to see *which* of the two
 * materials is the one they are short of, and a list that hides empties would show them neither.
 */
@Composable
private fun MaterialStock(state: GameState) {
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        PixelLabel(text = "Material", color = Starlight, size = 12)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (material in Material.entries) {
                val held = Shells.amountOf(state, material)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SpaceElevated)
                        .border(2.dp, if (held > 0.0) material.accent else Outline, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // The colour is the label, really. The word underneath is for the first time.
                    Box(Modifier.size(10.dp).background(material.accent))
                    Spacer(Modifier.height(5.dp))
                    PixelLabel(
                        text = Numbers.format(held),
                        color = if (held > 0.0) Starlight else Unreached,
                        size = 12,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    PixelLabel(text = material.label, color = Muted, size = 8, maxLines = 1)
                }
            }
        }
    }
}

/**
 * One line saying whether anything is still coming, and how often.
 *
 * Written as a sentence rather than a statistic because it answers a question a player actually
 * asks — "is this over?" — at the one moment the answer changes, which is when the impacts stop
 * and the comets take over.
 */
@Composable
private fun ArrivalNote(state: GameState) {
    val active = Accretion.isActive(state)
    val text = if (active) {
        val every = Accretion.interval(state)
        "Etwa alle ${Numbers.format(every)} Sekunden fällt etwas ein. Tippe es an, bevor es " +
            "aufschlägt — sonst prallt das meiste davon wieder ab."
    } else {
        "Der Körper ist zu groß geworden; kleine Brocken merkt er nicht mehr. Ab hier bringen " +
            "die Kometen das Material des Himmels."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
    )
}

// ---------------------------------------------------------------------- shells

/**
 * One shell: what it is, what it does, what the next level costs.
 *
 * The cost is drawn as two chips in the colours of the two materials, each lit when the stock
 * covers it. That replaces a sentence — "3 Metall und 2 Silikat, du hast 3 und 1" — with something
 * that can be read without reading: one chip green, one chip not.
 */
@Composable
private fun ShellRow(state: GameState, shell: Shell, onBuild: () -> Unit) {
    val level = Shells.levelOf(state, shell)
    val maxed = level >= Shells.MAX_LEVEL
    val cost = remember(state, shell) { Shells.costOf(state, shell) }
    val affordable = Shells.canBuild(state, shell)
    val share = Shells.shareOf(state, shell)

    PixelPanel(modifier = Modifier.fillMaxWidth(), border = if (affordable) Positive else Outline) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(shell.accent))
            Spacer(Modifier.width(8.dp))
            PixelLabel(text = shell.label, color = Starlight, size = 12)
            Spacer(Modifier.weight(1f))
            PixelLabel(
                text = "$level / ${Shells.MAX_LEVEL}",
                color = if (maxed) Ember else Muted,
                size = 11,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = shell.effect,
            style = MaterialTheme.typography.bodySmall,
            color = Ember,
        )

        // What share of the whole body this shell is, which is what the world type is read off.
        Spacer(Modifier.height(6.dp))
        PixelBar(
            progress = share.toFloat(),
            color = shell.accent,
            modifier = Modifier.fillMaxWidth().height(10.dp),
            cells = 20,
        )

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (maxed) {
                PixelLabel(text = "Fertig gebaut", color = Ember, size = 11)
            } else {
                for ((material, amount) in cost) {
                    val enough = Shells.amountOf(state, material) >= amount
                    Row(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .background(SpaceElevated)
                            .border(2.dp, if (enough) Positive else Outline, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).background(material.accent))
                        Spacer(Modifier.width(5.dp))
                        PixelLabel(
                            text = Numbers.format(amount),
                            color = if (enough) Starlight else Muted,
                            size = 11,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            PixelButton(
                label = "Bauen",
                onClick = onBuild,
                enabled = affordable,
                accent = Positive,
            )
        }
    }
}

// ---------------------------------------------------------------------- record

/**
 * Everything this save has ever been, as a grid.
 *
 * A grid and not a list, because the two axes are the whole idea: the row says what you built the
 * body out of and the column says how far you took it. A player with three young worlds and no
 * grown ones can see that the thing missing is patience; one with a full metal row can see that
 * the thing missing is a different decision. A flat list of twelve names says neither.
 */
@Composable
private fun WorldRecord(state: GameState) {
    val current = Worlds.current(state)
    val bonus = Worlds.multiplier(state)

    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelLabel(text = "Weltentypen", color = Starlight, size = 12)
            Spacer(Modifier.weight(1f))
            PixelLabel(
                text = "${state.worldTypes.size} / ${Worlds.all.size}",
                color = Muted,
                size = 11,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = if (current == null) {
                "Noch nichts Bestimmtes. Ab ${Depth.JUNG.atLeast} Schichten bekommt der Körper " +
                    "einen Namen — und der bleibt eingetragen, auch nach dem Kollaps."
            } else {
                "Gerade: ${current.label} — ${current.flavor}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (current == null) Muted else Ember,
        )

        Spacer(Modifier.height(8.dp))
        for (lane in Lane.entries) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (depth in Depth.entries) {
                    val world = Worlds.of(lane, depth)
                    val known = world.id in state.worldTypes
                    val here = world == current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (known) SpaceElevated else Space)
                            .border(
                                2.dp,
                                when {
                                    here -> Ember
                                    known -> Nebula
                                    else -> Outline
                                },
                                RectangleShape,
                            )
                            .padding(horizontal = 4.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PixelLabel(
                            // Unknown ones keep their shape and lose their name: a row of blanks
                            // says "three of these exist" without saying what they are, which is
                            // the only thing a collection has to do before it is collected.
                            text = if (known) world.label else "???",
                            color = if (known) Starlight else Unreached,
                            size = 9,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        PixelLabel(
            text = "Bonus ${Numbers.formatMultiplier(bonus)}",
            color = if (bonus > 1.0) Positive else Muted,
            size = 11,
        )
    }
}

/**
 * Whether the body is worth a whole area of the interface yet.
 *
 * Its own function so the phone's bar, the wide screen's tab strip and the narrow shop's strip all
 * ask the same question — three places is three chances for one of them to drift.
 */
fun aufbauAvailable(state: GameState): Boolean =
    Accretion.isUnlocked(state) || Worlds.isUnlocked(state)

/** How many shells could be built right now, which is what badges the tab. */
fun buildableCount(state: GameState): Int =
    Shell.entries.count { Shells.canBuild(state, it) }
