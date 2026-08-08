package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Achievements
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.History
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.PrestigeUpgrades
import com.staatseigentum.kollaps.core.Statistics
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

/** Achievements, and the statistics that explain how they were earned. */
@Composable
fun AchievementList(state: GameState, modifier: Modifier = Modifier) {
    val earned = state.achievements
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            PixelPanel(modifier = Modifier.fillMaxWidth(), border = Ember) {
                PixelLabel("Statistik", color = Ember, size = 15)
                Spacer(Modifier.height(8.dp))
                for (line in Statistics.lines(state)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(line.label, style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(
                            line.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = Starlight,
                        )
                    }
                }
            }
        }

        if (History.isWorthShowing(state.history)) {
            item {
                PixelPanel(modifier = Modifier.fillMaxWidth()) {
                    PixelLabel("Produktion, letzte halbe Stunde", size = 13)
                    Spacer(Modifier.height(8.dp))
                    Sparkline(
                        samples = state.history,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Numbers.formatRate(state.history.min()),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        Text(
                            text = Numbers.formatRate(state.history.max()),
                            style = MaterialTheme.typography.bodySmall,
                            color = Ember,
                        )
                    }
                }
            }
        }

        val shares = Statistics.shares(state)
        if (shares.isNotEmpty()) {
            item {
                PixelPanel(modifier = Modifier.fillMaxWidth()) {
                    PixelLabel("Wer die Arbeit macht", size = 13)
                    Spacer(Modifier.height(8.dp))
                    for (share in shares) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${share.collector.name} ×${share.owned}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Starlight,
                            )
                            Text(
                                Numbers.formatPercent(share.share.toDouble()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Ember,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        PixelBar(
                            progress = share.share,
                            color = Ember,
                            cells = 16,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        item {
            PixelLabel(
                text = "Erfolge ${earned.size}/${Achievements.all.size}",
                color = Positive,
                size = 15,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }

        items(Achievements.all, key = { it.id }) { achievement ->
            val has = achievement.id in earned
            PixelPanel(
                modifier = Modifier.fillMaxWidth(),
                border = if (has) Positive else Outline,
                padding = 10,
            ) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (has) Starlight else Muted,
                )
                Text(
                    text = achievement.flavor,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

/** The prestige shop: what singularities are actually for. */
@Composable
fun PrestigeShop(state: GameState, onBuy: (String) -> Unit) {
    val offered = PrestigeUpgrades.offered(state)
    PixelPanel(modifier = Modifier.fillMaxWidth(), border = Nebula) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelLabel("Singularitäten ausgeben", color = Nebula, size = 15)
            PixelLabel(Numbers.format(state.singularities), color = Ember, size = 13)
        }
        Spacer(Modifier.height(8.dp))

        if (offered.isEmpty()) {
            Text(
                text = if (state.prestigeUpgrades.size == PrestigeUpgrades.all.size) {
                    "Alles gekauft. Es gibt nichts mehr, was ein Neuanfang billiger machen könnte."
                } else {
                    "Weitere Upgrades erscheinen, wenn du öfter kollabiert bist."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            return@PixelPanel
        }

        for (upgrade in offered) {
            val affordable = state.singularities >= upgrade.cost
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
                        // Same marker the header uses for singularities; the pixel fonts are
                        // thin on symbols, so the whole game spends this one.
                        label = "• ${Numbers.format(upgrade.cost)}",
                        onClick = { onBuy(upgrade.id) },
                        enabled = affordable,
                        accent = Nebula,
                    )
                }
            }
        }
    }
}

/**
 * Sound, vibration and the save itself.
 *
 * The export is the important one: the game is sideloaded and keeps its save in the app's own
 * directory, so uninstalling takes it with it and there is no other copy anywhere.
 */
@Composable
fun SettingsSection(
    state: GameState,
    stats: Stats,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onMusic: (Boolean) -> Unit,
    onAutoBuy: (Boolean) -> Unit,
    onReminders: (Boolean) -> Unit,
    onExport: () -> String,
    onImport: (String) -> Boolean,
    onErase: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var importing by remember { mutableStateOf(false) }
    var erasing by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        PixelLabel("Einstellungen", size = 15)
        Spacer(Modifier.height(10.dp))

        Toggle("Klickgeräusch", state.soundOn) { onSound(!state.soundOn) }
        Spacer(Modifier.height(6.dp))
        Toggle("Vibration", state.hapticsOn) { onHaptics(!state.hapticsOn) }
        Spacer(Modifier.height(6.dp))
        Toggle("Musik", state.musicOn) { onMusic(!state.musicOn) }
        Text(
            text = "Ein Klangteppich, der sich ändert, sobald aus dem Gestein eine Welt, aus der " +
                "Welt ein Gasriese und aus dem Gasriesen ein Stern wird.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(6.dp))
        Toggle("Erinnerung, wenn der Speicher voll ist", state.remindersOn) {
            onReminders(!state.remindersOn)
        }

        // The automatic buyer used to live here as a single switch. It is a rule among five now,
        // and a second control for the same setting would only be a way to disagree with itself.

        Spacer(Modifier.height(14.dp))
        PixelLabel("Spielstand", size = 13, color = Muted)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Der Spielstand liegt nur auf diesem Gerät. Kopier ihn dir irgendwohin, " +
                "sonst ist er weg, wenn die App es ist.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PixelButton(
                label = "Kopieren",
                onClick = {
                    clipboard.setText(AnnotatedString(onExport()))
                    note = "In die Zwischenablage kopiert."
                },
                modifier = Modifier.weight(1f),
            )
            PixelButton(
                label = "Einfügen",
                onClick = { importing = true },
                modifier = Modifier.weight(1f),
                accent = Ember,
            )
        }
        note?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Positive)
        }

        Spacer(Modifier.height(14.dp))
        PixelLabel("Von vorn anfangen", size = 13, color = Muted)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Löscht alles: Masse, Kollektoren, Erfolge, Singularitäten, Äonen, Forschung. " +
                "Es gibt kein Zurück — kopier dir vorher den Spielstand, falls du unsicher bist.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(8.dp))
        // Two taps, and the first one is undone by leaving the panel. A single button here would
        // be the one control in the game that destroys hours of play by being brushed against.
        PixelButton(
            label = if (erasing) "Wirklich? Alles wird gelöscht" else "Spielstand löschen",
            onClick = {
                if (erasing) {
                    onErase()
                    erasing = false
                    note = "Alles gelöscht. Neuer Anfang."
                } else {
                    erasing = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            accent = if (erasing) Ember else Outline,
        )
        if (erasing) {
            Spacer(Modifier.height(6.dp))
            PixelButton(
                label = "Doch nicht",
                onClick = { erasing = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (importing) {
        ImportDialog(
            onDismiss = { importing = false },
            onConfirm = { block ->
                importing = false
                note = if (onImport(block)) {
                    "Spielstand geladen."
                } else {
                    "Das war kein Kollaps-Spielstand."
                }
            },
        )
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Starlight)
        PixelButton(
            label = if (on) "An" else "Aus",
            onClick = onToggle,
            accent = if (on) Positive else Outline,
        )
    }
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceElevated,
        shape = RectangleShape,
        title = { PixelLabel("Spielstand einfügen", size = 15) },
        text = {
            Column {
                Text(
                    text = "Achtung: das ersetzt den laufenden Spielstand vollständig.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ember,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Starlight),
                        cursorBrush = SolidColor(Ember),
                    )
                }
            }
        },
        confirmButton = {
            PixelButton(
                label = "Laden",
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                accent = Ember,
            )
        },
        dismissButton = { PixelButton(label = "Abbrechen", onClick = onDismiss) },
    )
}

/**
 * The production record as pixel columns.
 *
 * Logarithmic, because production in an idle game is exponential and a linear line would be flat
 * for twenty-nine minutes and then vertical. On a log scale a steady climb is a straight ramp,
 * which is what the player actually wants to see. Drawn as whole blocks on a grid rather than as
 * a stroked path, so it belongs to the same picture as the planets.
 */
@Composable
private fun Sparkline(samples: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (samples.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas

        val block = floor(3.dp.toPx()).coerceAtLeast(2f)
        val columns = floor(size.width / block).toInt().coerceAtLeast(1)
        val rows = floor(size.height / block).toInt().coerceAtLeast(1)

        val logs = samples.map { ln(it.coerceAtLeast(1.0)) }
        val low = logs.min()
        val high = logs.max()
        val span = (high - low).takeIf { it > 1e-9 }

        for (column in 0 until columns) {
            // The record is shorter than the strip is wide, so each sample owns a slice of it.
            val index = (column.toFloat() / columns * samples.size).toInt().coerceIn(samples.indices)
            val height = if (span == null) 0.6f else ((logs[index] - low) / span).toFloat()
            val filled = (height * (rows - 1)).roundToInt() + 1

            for (row in 0 until filled) {
                drawRect(
                    color = if (row == filled - 1) Ember else Ember.copy(alpha = 0.35f),
                    topLeft = Offset(column * block, size.height - (row + 1) * block),
                    size = Size(block, block),
                )
            }
        }
    }
}
