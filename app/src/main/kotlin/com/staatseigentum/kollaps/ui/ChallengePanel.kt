package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Challenge
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.text
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The challenges: optional runs under a rule that makes the game worse, for a permanent reward.
 *
 * Only one thing shows at a time — the running challenge, or the list of the ones on offer —
 * because starting a second while one is going is not a thing the rules allow and a list of
 * greyed-out buttons is a worse way to say so.
 */
@Composable
fun ChallengePanel(
    state: GameState,
    stats: Stats,
    onStart: (String) -> Unit,
    onAbort: () -> Unit,
    onFinish: () -> Unit,
) {
    val running = stats.challenge
    if (running != null) {
        RunningChallenge(
            challenge = running,
            state = state,
            stats = stats,
            onAbort = onAbort,
            onFinish = onFinish,
        )
        return
    }

    val offered = Challenge.offered(state)
    val done = state.challengesDone.size
    PixelPanel(modifier = Modifier.fillMaxWidth(), border = Nebula) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelLabel("Herausforderungen", color = Nebula, size = 15)
            PixelLabel("$done/${Challenge.entries.size}", color = Muted, size = 13)
        }
        Spacer(Modifier.height(8.dp))

        if (offered.isEmpty()) {
            Text(
                text = if (done == Challenge.entries.size) {
                    "Alle bestanden. Es gibt nichts mehr, was du dir noch schwerer machen könntest."
                } else {
                    "Herausforderungen tauchen auf, wenn du kollabiert bist. Sie starten einen " +
                        "Lauf unter einer Regel, die dir etwas wegnimmt — dafür bleibt die " +
                        "Belohnung für immer."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            return@PixelPanel
        }

        Text(
            text = "Jede startet den Lauf neu und nimmt dir etwas weg. Der Vorsprung aus dem " +
                "Prestige zählt dabei nicht — die dauerhaften Multiplikatoren schon.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(8.dp))

        for (challenge in offered) {
            ChallengeRow(challenge = challenge, onStart = { onStart(challenge.id) })
        }
    }
}

@Composable
private fun ChallengeRow(challenge: Challenge, onStart: () -> Unit) {
    var confirming by remember(challenge) { mutableStateOf(false) }

    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        border = Outline,
        padding = 10,
    ) {
        Text(challenge.title, style = MaterialTheme.typography.bodyLarge, color = Starlight)
        Text(challenge.flavor, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(6.dp))
        Line("Regel", challenge.ruleText, Ember)
        Line("Ziel", challenge.goalText, Starlight)
        Line("Belohnung", challenge.reward.text, Positive)
        Spacer(Modifier.height(8.dp))
        PixelButton(
            label = if (confirming) "Lauf wirklich neu starten?" else "Annehmen",
            onClick = { if (confirming) onStart() else confirming = true },
            modifier = Modifier.fillMaxWidth(),
            accent = if (confirming) Ember else Nebula,
        )
    }
}

@Composable
private fun RunningChallenge(
    challenge: Challenge,
    state: GameState,
    stats: Stats,
    onAbort: () -> Unit,
    onFinish: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    val sfx = LocalSfx.current

    PixelPanel(
        modifier = Modifier.fillMaxWidth(),
        border = if (stats.challengeLost) Ember else Nebula,
    ) {
        PixelLabel("Herausforderung läuft", color = Nebula, size = 15)
        Spacer(Modifier.height(6.dp))
        Text(challenge.title, style = MaterialTheme.typography.bodyLarge, color = Starlight)
        Text(challenge.flavor, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(8.dp))

        Line("Regel", challenge.ruleText, Ember)
        Line("Ziel", challenge.goalText, Starlight)
        Line("Belohnung", challenge.reward.text, Positive)
        Line("Gespielt", Numbers.formatDuration(state.challengeSeconds.toLong()), Muted)

        Spacer(Modifier.height(10.dp))
        when {
            stats.challengeMet -> {
                Text(
                    text = "Geschafft. Einlösen setzt den Lauf zurück und behält die Belohnung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Positive,
                )
                Spacer(Modifier.height(8.dp))
                PixelButton(
                    label = "Belohnung einlösen",
                    onClick = {
                        sfx?.success()
                        onFinish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accent = Positive,
                )
            }

            stats.challengeLost -> {
                Text(
                    text = "Die Zeit ist um. Aufgeben setzt den Lauf zurück, danach kannst du " +
                        "es noch mal versuchen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ember,
                )
                Spacer(Modifier.height(8.dp))
                PixelButton(
                    label = "Neu ansetzen",
                    onClick = onAbort,
                    modifier = Modifier.fillMaxWidth(),
                    accent = Ember,
                )
            }

            else -> {
                Text(
                    text = "Kollabieren geht erst wieder, wenn das hier vorbei ist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                PixelButton(
                    label = if (confirming) "Wirklich aufgeben?" else "Aufgeben",
                    onClick = { if (confirming) onAbort() else confirming = true },
                    modifier = Modifier.fillMaxWidth(),
                    accent = Outline,
                )
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.width(78.dp),
        )
        Spacer(Modifier.width(4.dp))
        Column {
            Text(value, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}
