package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Research
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Wallclock
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlinx.coroutines.delay

/**
 * The lab: one bench, a queue of one, and a clock nobody can hurry.
 *
 * The countdown reads the wall clock directly rather than the game state, because that is what it
 * is counting. A project keeps running with the app closed, so a bar driven by the tick would
 * jump forward on every return instead of simply being further along.
 */
@Composable
fun ResearchPanel(
    state: GameState,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    if (!ResearchTree.isUnlocked(state)) return

    var now by remember { mutableLongStateOf(Wallclock.millis()) }
    val running = ResearchTree.active(state)

    // Only while something is running: an idle lab has nothing that changes on its own, and a
    // ticker behind a still panel would keep the whole screen recomposing once a second.
    LaunchedEffect(running?.id) {
        while (running != null) {
            now = Wallclock.millis()
            delay(500)
        }
    }

    PixelPanel(modifier = modifier.fillMaxWidth(), border = Nebula) {
        PixelLabel(text = "Labor", color = Nebula, size = 16)
        Spacer(Modifier.height(8.dp))

        if (running == null) {
            Text(
                text = Lang.t("Ein Projekt läuft auf der echten Uhr weiter — auch wenn das Spiel zu ist. Es gibt nur eine Bank, also läuft immer nur eines."),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
        } else {
            RunningProject(
                state = state,
                project = running,
                now = now,
                onCancel = actions::cancelResearch,
            )
        }

        Spacer(Modifier.height(12.dp))

        for (project in ResearchTree.offered(state)) {
            ProjectRow(
                state = state,
                project = project,
                busy = running != null,
                onStart = { actions.startResearch(project.id) },
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun RunningProject(
    state: GameState,
    project: Research,
    now: Long,
    onCancel: () -> Unit,
) {
    val left = ResearchTree.secondsLeft(state, now)
    val done = left <= 0.0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Starlight,
            )
            Text(
                text = if (done) "fertig" else Lang.t("noch %s", Numbers.formatDuration(left.toLong())),
                style = MaterialTheme.typography.bodySmall,
                color = if (done) Positive else Muted,
            )
        }

        Spacer(Modifier.height(6.dp))

        PixelBar(
            progress = ResearchTree.progress(state, now),
            color = if (done) Positive else Nebula,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = project.effectText,
            style = MaterialTheme.typography.bodySmall,
            color = Positive,
        )

        if (!done) {
            Spacer(Modifier.height(10.dp))
            PixelButton(
                label = Lang.t("Abbrechen (Masse ist weg)"),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                accent = Outline,
            )
        }
    }
}

@Composable
private fun ProjectRow(
    state: GameState,
    project: Research,
    busy: Boolean,
    onStart: () -> Unit,
) {
    val done = ResearchTree.isDone(state, project)
    val startable = !done && !busy && project.cost <= state.mass
    val sfx = LocalSfx.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceCard)
            .clickable(enabled = startable) {
                sfx?.purchase()
                onStart()
            }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (done) Muted else Starlight,
                )
                Text(
                    text = if (done) project.effectText else project.flavor,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (done) Positive else Muted,
                )
            }

            Spacer(Modifier.width(10.dp))

            if (done) {
                PixelLabel(text = "fertig", color = Positive, size = 10)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Numbers.formatMass(project.cost),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (project.cost <= state.mass) Positive else Muted,
                    )
                    Text(
                        // The catalogue time would be a lie for anyone who researched
                        // Parallelrechnung, and the lab's whole currency is time.
                        text = Numbers.formatDuration(
                            ResearchTree.duration(state, project).toLong(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Ember,
                    )
                }
            }
        }
    }
}
