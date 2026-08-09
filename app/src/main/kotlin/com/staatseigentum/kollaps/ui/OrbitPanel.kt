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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Orbit
import com.staatseigentum.kollaps.core.Orbits
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The system: eight slots around the body, and what is standing on each of them.
 *
 * Read from the inside out, which is also the order the trade runs in. The innermost slots are fed
 * hard, pulled apart just as hard and worth the most; the outermost are fed slowly, left alone and
 * worth the least. Each row says which end of that it sits on rather than making the player work
 * it out from two numbers.
 */
@Composable
fun OrbitPanel(
    state: GameState,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    if (!Orbits.isUnlocked(state)) {
        OrbitLockedNotice(state = state, modifier = modifier)
        return
    }

    val opened = remember(state) { Orbits.opened(state) }
    val next = Orbits.next(state)
    val perSecond = remember(state) { GameEngine.massPerSecond(state) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            PixelPanel(modifier = Modifier.fillMaxWidth(), border = Nebula) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PixelLabel("Das System", color = Nebula, size = 15)
                    PixelLabel(
                        Numbers.formatMultiplier(Orbits.multiplier(state)),
                        color = Ember,
                        size = 13,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${Orbits.occupiedCount(state)} von ${opened.size} offenen Bahnen " +
                        "besetzt. Innen wird schnell gefüttert und schnell zerrissen, außen " +
                        "langsam und für immer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )

                if (next != null) {
                    Spacer(Modifier.height(10.dp))
                    PixelButton(
                        label = "Bahn ${next.index + 1} öffnen · ${Numbers.formatMass(next.cost)}",
                        onClick = actions::openOrbit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = next.cost <= state.mass,
                        accent = Ember,
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    PixelLabel("Alle Bahnen offen", color = Positive, size = 11)
                }
            }
        }

        items(opened.size, key = { opened[it].index }) { position ->
            val orbit = opened[position]
            OrbitRow(
                state = state,
                orbit = orbit,
                perSecond = perSecond,
                // The nearest occupied slot further in, which is where this one would fall.
                target = opened.take(position).lastOrNull { Orbits.isOccupied(state, it) },
                actions = actions,
            )
        }
    }
}

@Composable
private fun OrbitLockedNotice(state: GameState, modifier: Modifier = Modifier) {
    val target = Tiers.byName(Orbits.UNLOCK_TIER)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLabel(text = "Nichts, was bleiben würde", color = Muted)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Erst ab dem ${target.name} hält deine Schwerkraft etwas auf einer Bahn. " +
                "Vorher fällt alles entweder herunter oder weg.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "noch ${Numbers.formatMass((target.threshold - state.runMass).coerceAtLeast(0.0))}",
            style = MaterialTheme.typography.bodySmall,
            color = Ember,
        )
    }
}

@Composable
private fun OrbitRow(
    state: GameState,
    orbit: Orbit,
    perSecond: Double,
    target: Orbit?,
    actions: GameActions,
) {
    val mass = Orbits.massOn(state, orbit)
    val occupied = mass > 0.0
    val tier = Orbits.tierOn(state, orbit)
    val partners = remember(state, orbit) { Orbits.resonantWith(state, orbit) }
    val sfx = LocalSfx.current

    PixelPanel(
        modifier = Modifier.fillMaxWidth(),
        border = if (partners.isNotEmpty()) Ember else Outline,
        padding = 10,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (occupied) SpaceElevated else SpaceCard)
                    .border(2.dp, Outline, RectangleShape),
                contentAlignment = Alignment.Center,
            ) {
                PixelLabel(
                    text = "${orbit.index + 1}",
                    color = if (occupied) Ember else Muted,
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (occupied) tier.label else "leer",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (occupied) Color(tier.glowColor) else Muted,
                )
                Text(
                    text = if (occupied) {
                        "${Numbers.formatMass(mass)} · " +
                            "+${Numbers.formatPercent(Orbits.totalYieldOf(state, orbit))} Produktion"
                    } else {
                        settlement(orbit, perSecond)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (occupied) Positive else Muted,
                )
                if (partners.isNotEmpty()) {
                    Text(
                        text = "Resonanz mit Bahn ${partners.joinToString { "${it.index + 1}" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ember,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (!occupied) {
                PixelButton(
                    label = Numbers.formatMass(orbit.seedCost),
                    onClick = { actions.seedSatellite(orbit.index) },
                    enabled = orbit.seedCost <= state.mass,
                    accent = Positive,
                )
            } else if (target != null) {
                // One tap, no mode: a body falls onto the nearest one below it. Choosing a target
                // would mean arming the row and then picking, for a decision with one sane answer.
                PixelButton(
                    label = "↓ ${target.index + 1}",
                    onClick = {
                        sfx?.purchase()
                        actions.mergeSatellites(orbit.index, target.index)
                    },
                    accent = Nebula,
                )
            }
        }
    }
}

/** What a slot promises: where a body on it would come to rest, or that it never would. */
private fun settlement(orbit: Orbit, perSecond: Double): String {
    val limit = orbit.equilibrium(perSecond)
    return if (limit.isFinite()) {
        "pendelt sich bei ${Numbers.formatMass(limit)} ein"
    } else {
        "wächst ohne Grenze, dafür langsam"
    }
}
