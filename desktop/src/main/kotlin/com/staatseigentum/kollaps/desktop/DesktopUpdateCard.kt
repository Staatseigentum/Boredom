package com.staatseigentum.kollaps.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.ui.PixelBar
import com.staatseigentum.kollaps.ui.PixelButton
import com.staatseigentum.kollaps.ui.PixelLabel
import com.staatseigentum.kollaps.ui.PixelPanel
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

/** Where the card is in its one job. */
private sealed interface Phase {
    data object Looking : Phase
    data object UpToDate : Phase
    data class Found(val update: AvailableUpdate) : Phase
    data class Downloading(val update: AvailableUpdate) : Phase
    data class Ready(val installer: File) : Phase
    data object Failed : Phase
}

/**
 * The PC version's update card, for the Kosmos tab.
 *
 * Same seam the phone hangs its updater on, so the screen itself needs to know nothing about
 * either. The check runs once when the tab is first drawn rather than on a timer: a game that
 * quietly talks to the network every few minutes is not what anybody installed.
 *
 * On anything that is not Windows there is no installer to run, so the card offers the releases
 * page instead of pretending it can do the work.
 */
@Composable
fun DesktopUpdateCard(onBeforeExit: () -> Unit = {}) {
    var phase by remember { mutableStateOf<Phase>(Phase.Looking) }
    var progress by remember { mutableFloatStateOf(0f) }
    val installed = remember { DesktopUpdater.installedVersion() }

    LaunchedEffect(Unit) {
        phase = withContext(Dispatchers.IO) {
            when (val found = DesktopUpdater.check()) {
                null -> if (installed == null) Phase.Failed else Phase.UpToDate
                else -> Phase.Found(found)
            }
        }
    }

    PixelPanel(modifier = Modifier.fillMaxWidth(), padding = 12) {
        PixelLabel("Version", size = 14)
        Spacer(Modifier.height(6.dp))
        Text(
            text = installed?.let { "Installiert: ${it.canonical()}" }
                ?: "Aus dem Quelltext gestartet — kein Update möglich.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(10.dp))

        when (val current = phase) {
            Phase.Looking -> Text(
                text = "Suche nach einer neueren Version …",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )

            Phase.UpToDate -> Text(
                text = "Alles aktuell.",
                style = MaterialTheme.typography.bodySmall,
                color = Positive,
            )

            Phase.Failed -> Row {
                Text(
                    text = "Nicht erreichbar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Spacer(Modifier.width(10.dp))
                PixelButton(
                    label = "Seite öffnen",
                    onClick = { DesktopUpdater.openReleasesPage() },
                )
            }

            is Phase.Found -> Column {
                Text(
                    text = current.update.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Starlight,
                )
                Text(
                    text = megabytes(current.update.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Spacer(Modifier.height(10.dp))
                if (DesktopUpdater.canInstall) {
                    PixelButton(
                        label = "Herunterladen",
                        accent = Ember,
                        onClick = { phase = Phase.Downloading(current.update) },
                    )
                } else {
                    // Only Windows gets an installer built, so anywhere else this is a link.
                    PixelButton(
                        label = "Seite öffnen",
                        onClick = { DesktopUpdater.openReleasesPage() },
                    )
                }
            }

            is Phase.Downloading -> {
                Text(
                    text = "Lädt … ${(progress * 100).toInt()} %",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Spacer(Modifier.height(6.dp))
                PixelBar(
                    progress = progress,
                    color = Ember,
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )

                LaunchedEffect(current.update.downloadUrl) {
                    val file = withContext(Dispatchers.IO) {
                        DesktopUpdater.download(current.update) { progress = it }
                    }
                    phase = if (file == null) Phase.Failed else Phase.Ready(file)
                }
            }

            is Phase.Ready -> Column {
                Text(
                    text = "Fertig geladen. Der Installer ersetzt die vorhandene Fassung; " +
                        "das Spiel schließt sich dafür.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Spacer(Modifier.height(10.dp))
                PixelButton(
                    label = "Installieren und beenden",
                    accent = Ember,
                    onClick = {
                        // The save is written first: the process is about to end on purpose, and
                        // an update that costs the last few minutes of play would be a bad trade.
                        onBeforeExit()
                        if (DesktopUpdater.install(current.installer)) exitProcess(0)
                    },
                )
            }
        }
    }
}

private fun megabytes(bytes: Long): String =
    if (bytes <= 0) "" else "%.1f MB".format(bytes / 1_048_576.0)
