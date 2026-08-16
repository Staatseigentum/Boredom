package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateListOf
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
 * Two can be taken at once, which is why this is a selection rather than a row of buttons: a
 * player picking a pair is making one decision about the next run, not two decisions in a row, and
 * the second half of the pair changes what the first one is worth.
 *
 * Only one thing shows at a time — what is running, or what is on offer — because starting
 * anything while a run is going is not a thing the rules allow, and a list of greyed-out buttons
 * is a worse way to say so.
 */
@Composable
fun ChallengePanel(
    state: GameState,
    stats: Stats,
    onStart: (Set<String>) -> Unit,
    onAbort: () -> Unit,
    onFinish: () -> Unit,
) {
    if (stats.challenges.isNotEmpty()) {
        RunningChallenges(
            challenges = stats.challenges,
            state = state,
            stats = stats,
            onAbort = onAbort,
            onFinish = onFinish,
        )
        return
    }

    val offered = Challenge.offered(state)
    val done = state.challengesDone.size

    // Cleared whenever the offer changes, so a challenge that has just been beaten cannot stay
    // selected under a card that is no longer there.
    val picked = remember(offered) { mutableStateListOf<String>() }
    var confirming by remember(picked.size) { mutableStateOf(false) }

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
                    Lang.t("Alle bestanden. Es gibt nichts mehr, was du dir noch schwerer machen könntest.")
                } else {
                    Lang.t("Herausforderungen tauchen auf, wenn du kollabiert bist. Sie starten einen Lauf unter einer Regel, die dir etwas wegnimmt — dafür bleibt die Belohnung für immer.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            return@PixelPanel
        }

        Text(
            text = Lang.t("Jede startet den Lauf neu und nimmt dir etwas weg. Der Vorsprung aus dem Prestige zählt dabei nicht — die dauerhaften Multiplikatoren schon. Zwei gleichzeitig gehen auch: beide Regeln, beide Ziele, beide Belohnungen — und obendrauf %s für immer, wenn keine der beiden vorher schon bestanden war.", Numbers.formatMultiplier(Challenge.DUO_BONUS)),
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(8.dp))

        for (challenge in offered) {
            val selected = challenge.id in picked
            // A card is refused rather than hidden when it cannot join the selection, because the
            // reason is worth reading: it is the one pair that would produce nothing at all.
            val blocked = !selected && picked.isNotEmpty() &&
                picked.mapNotNull(Challenge::byId).any { !Challenge.canCombine(it, challenge) }

            ChallengeRow(
                challenge = challenge,
                selected = selected,
                blocked = blocked,
                onToggle = {
                    when {
                        selected -> picked.remove(challenge.id)
                        blocked -> Unit
                        picked.size < Challenge.MAX_AT_ONCE -> picked.add(challenge.id)
                        // Full: the tap replaces the older pick rather than doing nothing, which
                        // is what a player who has already chosen two obviously means by it.
                        else -> {
                            picked.removeAt(0)
                            picked.add(challenge.id)
                        }
                    }
                },
            )
        }

        if (picked.isEmpty()) return@PixelPanel

        Spacer(Modifier.height(4.dp))
        val names = picked.mapNotNull { Challenge.byId(it)?.title }
        val duo = picked.size == Challenge.MAX_AT_ONCE
        Text(
            text = if (duo) {
                Lang.t("%s — beides gleichzeitig, beide Ziele nötig.", names.joinToString(" + "))
            } else {
                names.first()
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (duo) Positive else Muted,
        )
        Spacer(Modifier.height(6.dp))
        PixelButton(
            label = when {
                confirming -> "Lauf wirklich neu starten?"
                duo -> "Beide annehmen"
                else -> "Annehmen"
            },
            onClick = { if (confirming) onStart(picked.toSet()) else confirming = true },
            modifier = Modifier.fillMaxWidth(),
            accent = if (confirming) Ember else Nebula,
        )
    }
}

@Composable
private fun ChallengeRow(
    challenge: Challenge,
    selected: Boolean,
    blocked: Boolean,
    onToggle: () -> Unit,
) {
    val sfx = LocalSfx.current
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clickable(enabled = !blocked) {
                sfx?.click()
                onToggle()
            },
        border = if (selected) Nebula else Outline,
        padding = 10,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (blocked) Muted else Starlight,
            )
            if (selected) PixelLabel(Lang.t("gewählt"), color = Nebula, size = 12)
        }
        Text(challenge.flavor, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(6.dp))
        Line("Regel", challenge.ruleText, if (blocked) Muted else Ember)
        Line("Ziel", challenge.goalText, if (blocked) Muted else Starlight)
        Line("Belohnung", challenge.reward.text, if (blocked) Muted else Positive)
        if (blocked) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = Lang.t("Zusammen mit der anderen bliebe nichts übrig, was Masse macht."),
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
            )
        }
    }
}

@Composable
private fun RunningChallenges(
    challenges: List<Challenge>,
    state: GameState,
    stats: Stats,
    onAbort: () -> Unit,
    onFinish: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val duo = challenges.size > 1

    PixelPanel(
        modifier = Modifier.fillMaxWidth(),
        border = if (stats.challengeLost) Ember else Nebula,
    ) {
        PixelLabel(
            text = if (duo) "Zwei Herausforderungen laufen" else Lang.t("Herausforderung läuft"),
            color = Nebula,
            size = 15,
        )
        Spacer(Modifier.height(6.dp))

        // Each one gets its own block with its own met-or-not mark: with two running, "geschafft"
        // as a single verdict would hide which half is still open.
        for (challenge in challenges) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Starlight,
                )
                if (duo) {
                    val met = challenge.isMetBy(state)
                    PixelLabel(
                        text = if (met) Lang.t("erfüllt") else "offen",
                        color = if (met) Positive else Muted,
                        size = 12,
                    )
                }
            }
            Text(challenge.flavor, style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(6.dp))
            Line("Regel", challenge.ruleText, Ember)
            Line("Ziel", challenge.goalText, Starlight)
            Line("Belohnung", challenge.reward.text, Positive)
            Spacer(Modifier.height(8.dp))
        }

        if (duo) {
            Line(
                "Bonus",
                Lang.t("%s zusätzlich, dauerhaft", Numbers.formatMultiplier(Challenge.DUO_BONUS)),
                Positive,
            )
        }
        Line("Gespielt", Numbers.formatDuration(state.challengeSeconds.toLong()), Muted)

        Spacer(Modifier.height(10.dp))
        when {
            stats.challengeMet -> {
                Text(
                    text = Lang.t("Geschafft. Einlösen setzt den Lauf zurück und behält die Belohnung."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Positive,
                )
                Spacer(Modifier.height(8.dp))
                PixelButton(
                    label = Lang.t("Belohnung einlösen"),
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
                    text = Lang.t("Die Zeit ist um. Aufgeben setzt den Lauf zurück, danach kannst du es noch mal versuchen."),
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
                    text = Lang.t("Kollabieren geht erst wieder, wenn das hier vorbei ist."),
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
