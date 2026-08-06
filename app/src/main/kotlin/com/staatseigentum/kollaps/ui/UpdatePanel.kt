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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.update.UpdateState
import com.staatseigentum.kollaps.update.UpdateViewModel
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated

/** The update section inside the Kosmos tab. Always visible, so a check is never hidden away. */
@Composable
fun UpdateCard(model: UpdateViewModel, modifier: Modifier = Modifier) {
    val state by model.state.collectAsStateWithLifecycle()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "App-Update",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ember,
                )
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
                    LinearProgressIndicator(
                        progress = { current.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Ember,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
    val update = when (val current = state) {
        is UpdateState.Available -> current.update
        is UpdateState.Downloading -> current.update
        is UpdateState.Ready -> current.update
        is UpdateState.NeedsPermission -> current.update
        else -> null
    } ?: return

    AlertDialog(
        onDismissRequest = model::dismissPrompt,
        containerColor = SpaceElevated,
        title = { Text("Neue Version verfügbar") },
        text = {
            Column {
                Text(
                    text = update.title + sizeSuffix(update),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (update.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Notes(update.notes, maxHeight = 200)
                }
                val current = state
                if (current is UpdateState.Downloading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { current.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Ember,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> TextButton(onClick = model::download) {
                    Text("Herunterladen")
                }

                is UpdateState.Downloading -> TextButton(onClick = {}, enabled = false) {
                    Text("Lädt …")
                }

                is UpdateState.Ready -> TextButton(onClick = model::install) {
                    Text("Installieren")
                }

                is UpdateState.NeedsPermission -> TextButton(onClick = model::install) {
                    Text("Erlauben")
                }

                else -> Unit
            }
        },
        dismissButton = {
            TextButton(onClick = model::dismissPrompt) { Text("Später") }
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
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

private fun sizeSuffix(update: AvailableUpdate): String =
    if (update.sizeBytes > 0) " · ${Numbers.formatBytes(update.sizeBytes)}" else ""
