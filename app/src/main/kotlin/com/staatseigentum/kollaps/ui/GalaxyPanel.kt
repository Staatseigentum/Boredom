package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GalaxyJob
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Multiverse
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.ParkedUniverse
import com.staatseigentum.kollaps.core.Path
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The sky: every universe that has been through its big bang and kept working.
 *
 * The rules for this landed a while before the screen did, which meant eight galaxies were quietly
 * multiplying production and earning Äonen with nowhere to look at them. A system the player cannot
 * see is a system they cannot plan around — the whole reason a path is chosen at each big bang is
 * that the sky remembers which ones were chosen before, and that is only a decision if it is
 * visible.
 *
 * Eight slots in a fixed ring, occupied or not. The empty ones are drawn too, because the shape of
 * what is still missing is the thing that makes the next big bang worth pressing.
 */
@Composable
fun GalaxyPanel(state: GameState, actions: GameActions, modifier: Modifier = Modifier) {
    val parked = Multiverse.parked(state)
    val perSecond = Multiverse.aeonsPerSecond(state)

    PixelPanel(modifier = modifier.fillMaxWidth(), border = Nebula, padding = 14) {
        Column {
            PixelLabel("Der Himmel", color = Nebula, size = 14)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${parked.size} von ${Multiverse.SLOTS} Galaxien. Jede davon ist ein " +
                    "Universum, das du zu Ende gespielt hast und das weiterläuft.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )

            Spacer(Modifier.height(12.dp))
            GalaxyRing(parked)

            Spacer(Modifier.height(12.dp))
            SkyStat("Produktion", Numbers.formatMultiplier(Multiverse.multiplier(state)))
            SkyStat(
                "Äonen",
                // Per day rather than per second, because per second is a row of zeroes. A galaxy
                // earns on the scale of days, and a number nobody can watch move is a number that
                // reads as broken.
                if (perSecond > 0.0) "${Numbers.format(perSecond * 86_400.0)} pro Tag" else "—",
            )
            if (state.aeonFraction > 0.0) {
                SkyStat("Nächstes Äon", Numbers.formatPercent(state.aeonFraction.coerceIn(0.0, 1.0)))
            }

            if (parked.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Noch leer. Der erste Urknall stellt das erste Universum hier ab, " +
                        "statt es wegzuwerfen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                return@Column
            }

            if (!Multiverse.hasRoom(state)) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Der Himmel ist voll. Zwei Galaxien lassen sich verschweißen — die " +
                        "verschmolzene trägt beide Ausrichtungen und macht einen Platz frei.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            Spacer(Modifier.height(12.dp))
            parked.forEach { universe ->
                GalaxyRow(state, universe, actions, parked)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** One label and one number, on a line. The shop's own version of this is private to its file. */
@Composable
private fun SkyStat(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Muted)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Starlight)
    }
}

/**
 * The eight slots, drawn as a ring.
 *
 * A ring and not a list because the list is directly underneath it: this is the picture that says
 * "five of eight" at a glance, and a second list would only say the same thing twice.
 */
@Composable
private fun GalaxyRing(parked: List<ParkedUniverse>, modifier: Modifier = Modifier) {
    val taken = parked.associateBy { it.slot }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.4f)
            .background(SpaceElevated)
            .drawBehind {
                val centre = Offset(size.width / 2f, size.height / 2f)
                val radius = minOf(size.width, size.height) * 0.36f

                for (slot in 0 until Multiverse.SLOTS) {
                    // Starting at the top and going clockwise, so slot zero — the first universe
                    // ever finished — sits where an eye lands first.
                    val angle = -PI / 2.0 + 2.0 * PI * slot / Multiverse.SLOTS
                    val at = Offset(
                        centre.x + (radius * cos(angle)).toFloat(),
                        centre.y + (radius * sin(angle)).toFloat(),
                    )
                    val universe = taken[slot]
                    val colour = universe?.let { pathColour(it.pathId) }

                    if (colour == null) {
                        // An empty slot is an outline, which is the point: it is the shape of what
                        // the next big bang is for.
                        drawCircle(Outline, radius = 9f, center = at, style = Stroke(2f))
                    } else {
                        drawCircle(colour.copy(alpha = 0.25f), radius = 16f, center = at)
                        drawCircle(colour, radius = 8f, center = at)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${parked.size}/${Multiverse.SLOTS}",
            style = MaterialTheme.typography.titleMedium,
            color = if (parked.size >= Multiverse.SLOTS) Positive else Starlight,
            textAlign = TextAlign.Center,
        )
    }
}

/** One galaxy: what it is called, how deep it got, which way it leans, and what it is doing. */
@Composable
private fun GalaxyRow(
    state: GameState,
    universe: ParkedUniverse,
    actions: GameActions,
    others: List<ParkedUniverse>,
    modifier: Modifier = Modifier,
) {
    val path = universe.path
    val body = Tiers.byIndex(universe.bestTier)

    Column(modifier = modifier.fillMaxWidth().background(SpaceElevated).padding(10.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .aspectRatio(1f)
                .drawBehind {
                    val colour = pathColour(universe.pathId)
                    drawCircle(colour.copy(alpha = 0.28f), radius = size.minDimension * 0.48f)
                    drawCircle(colour, radius = size.minDimension * 0.24f)
                },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = universe.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Starlight,
                maxLines = 1,
            )
            Text(
                text = "${body.label} · ${universe.collapses} Kollapse · " +
                    when {
                        universe.paths.size > 1 -> universe.paths.joinToString(" + ") { it.label }
                        else -> path?.label ?: "ohne Ausrichtung"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
            )
        }
        Text(
            text = Numbers.formatMultiplier(1.0 + Multiverse.weightedYieldOf(state, universe)),
            style = MaterialTheme.typography.bodyMedium,
            color = Ember,
            maxLines = 1,
        )
    }

        Spacer(Modifier.height(8.dp))
        if (universe.isRamping) {
            // While it changes over it does nothing, so the strip is replaced by the reason —
            // four chips that all look pressable would invite pressing them again.
            PixelLabel(
                "Umstellung auf ${universe.job.label} · noch " +
                    Numbers.formatDuration(universe.rampSeconds.toLong()),
                color = Muted,
                size = 11,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GalaxyJob.entries.forEach { job ->
                    JobChip(
                        job = job,
                        selected = universe.job == job,
                        onClick = { actions.assignGalaxy(universe.slot, job.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = universe.job.flavor,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )

            // Only offered with a full sky, and only onto galaxies this one can actually absorb.
            // A row of buttons that all refuse is worse than no row at all.
            val absorbable = others.filter { Multiverse.canMerge(state, universe.slot, it.slot) }
            if (absorbable.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                PixelLabel("Hierher verschweißen", color = Muted, size = 10)
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    absorbable.forEach { other ->
                        Text(
                            text = other.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Ember,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .clickable { actions.mergeGalaxies(universe.slot, other.slot) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** One of the four jobs, as something to press. */
@Composable
private fun JobChip(
    job: GalaxyJob,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = job.label,
        style = MaterialTheme.typography.bodySmall,
        color = if (selected) Starlight else Muted,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .background(if (selected) Nebula.copy(alpha = 0.30f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
    )
}

/**
 * The colour a lean is drawn in.
 *
 * Four paths, four colours, and one for the universes from before paths existed — a save carried
 * over from an older version has galaxies with no alignment at all, and they have to be drawable.
 */
private fun pathColour(pathId: String?): Color = when (Path.byId(pathId)) {
    Path.HAND -> Ember
    Path.MASCHINE -> Positive
    Path.LABOR -> Color(0xFF5CC8FF)
    Path.KERN -> Color(0xFFFF6B8A)
    null -> Muted
}
