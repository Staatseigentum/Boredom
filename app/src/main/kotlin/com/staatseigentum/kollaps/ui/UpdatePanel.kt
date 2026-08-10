package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.update.UpdateState
import com.staatseigentum.kollaps.update.UpdateViewModel
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.SpaceElevated

/** The update section inside the Kosmos tab. Always visible, so a check is never hidden away. */
@Composable
fun UpdateCard(model: UpdateViewModel, modifier: Modifier = Modifier) {
    val state by model.state.collectAsStateWithLifecycle()

    PixelPanel(modifier = modifier.fillMaxWidth(), border = Outline) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelLabel(text = "App-Update", color = Ember, size = 16)
                Text(
                    text = "Version ${model.installedVersion?.raw ?: "unbekannt"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            Spacer(Modifier.height(10.dp))

            when (val current = state) {
                UpdateState.Idle -> {
                    Description("Noch nicht nachgesehen, ob es eine neuere Version gibt.")
                    Action("Nach Updates suchen") { model.check() }
                }

                UpdateState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Ember,
                        )
                        Spacer(Modifier.size(10.dp))
                        Description("Suche nach einer neueren Version …")
                    }
                }

                UpdateState.UpToDate -> {
                    Description("Du spielst die neueste Version.")
                    Action("Nochmal prüfen") { model.check() }
                }

                is UpdateState.Available -> {
                    Description(
                        "${current.update.title} ist verfügbar" +
                            sizeSuffix(current.update),
                    )
                    if (current.update.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Notes(current.update.notes)
                    }
                    Action("Herunterladen") { model.download() }
                }

                is UpdateState.Downloading -> {
                    Description("Lade ${current.update.title} …")
                    Spacer(Modifier.height(10.dp))
                    PixelBar(
                        progress = current.progress,
                        color = Ember,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Numbers.formatPercent(current.progress.toDouble()),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }

                is UpdateState.Ready -> {
                    Description("${current.update.title} ist geladen und wartet auf die Installation.")
                    Action("Installieren") { model.install() }
                }

                is UpdateState.NeedsPermission -> {
                    Description(
                        "Damit die neue Version installiert werden kann, muss Kollaps in den " +
                            "Systemeinstellungen als Quelle erlaubt werden.",
                    )
                    Action("Berechtigung erteilen") { model.install() }
                }

                is UpdateState.Failed -> {
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Action("Nochmal versuchen") { model.check() }
                }
            }
        }
    }
}

/** Raised over the game when a background check turned something up. */
@Composable
fun UpdateDialog(model: UpdateViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val mandatory by model.mandatory.collectAsStateWithLifecycle()
    val update = when (val current = state) {
        is UpdateState.Available -> current.update
        is UpdateState.Downloading -> current.update
        is UpdateState.Ready -> current.update
        is UpdateState.NeedsPermission -> current.update
        else -> null
    } ?: return

    AlertDialog(
        /*
         * A big update has no way past it.
         *
         * No back gesture, no tap outside, no "later" — because a save written by 2.6 and read by
         * 2.5 is a fault report nobody can reproduce, since both people are running "Kollaps".
         *
         * The escape hatch is deliberate and lives one level up: [UpdateViewModel.mandatory] is
         * false the moment the update stops being installable. A failed download, a missing
         * network, a refused permission — anything that means the update cannot actually happen —
         * puts the "Später" button back. Blocking on something that is not working is how an
         * update becomes a brick, and that is a worse outcome than an old version.
         */
        onDismissRequest = { if (!mandatory) model.dismissPrompt() },
        containerColor = SpaceElevated,
        shape = RectangleShape,
        title = {
            PixelLabel(
                text = if (mandatory) "Diese Version musst du installieren" else "Neue Version verfügbar",
                size = 16,
            )
        },
        text = {
            Column {
                Text(
                    text = update.title + sizeSuffix(update),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (mandatory) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Ein großes Update ändert, was im Spielstand steht. Zwei " +
                            "Fassungen nebeneinander vertragen sich dabei nicht — deshalb geht " +
                            "es hier nur vorwärts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                if (update.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Notes(update.notes, maxHeight = 200)
                }
                val current = state
                if (current is UpdateState.Downloading) {
                    Spacer(Modifier.height(14.dp))
                    PixelBar(
                        progress = current.progress,
                        color = Ember,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> PixelButton(label = "Herunterladen", onClick = model::download, accent = Ember)

                is UpdateState.Downloading -> PixelButton(label = "Lädt …", onClick = {}, enabled = false)

                is UpdateState.Ready -> PixelButton(label = "Installieren", onClick = model::install, accent = Ember)

                is UpdateState.NeedsPermission -> PixelButton(label = "Erlauben", onClick = model::install, accent = Ember)

                else -> Unit
            }
        },
        dismissButton = {
            // Absent while the update is both big and installable. It comes back the moment
            // either of those stops being true.
            if (!mandatory) {
                PixelButton(label = "Später", onClick = model::dismissPrompt)
            }
        },
    )
}

// ---------------------------------------------------------------------- small pieces

@Composable
private fun Description(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Muted,
    )
}

@Composable
private fun Notes(text: String, maxHeight: Int = 120) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
        modifier = Modifier
            .heightIn(max = maxHeight.dp)
            .verticalScroll(rememberScrollState()),
    )
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    PixelButton(label = label, onClick = onClick, modifier = Modifier.fillMaxWidth())
}

private fun sizeSuffix(update: AvailableUpdate): String =
    if (update.sizeBytes > 0) " · ${Numbers.formatBytes(update.sizeBytes)}" else ""
