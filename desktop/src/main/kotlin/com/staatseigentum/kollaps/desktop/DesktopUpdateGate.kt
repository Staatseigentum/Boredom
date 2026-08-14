package com.staatseigentum.kollaps.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.ui.PixelBar
import com.staatseigentum.kollaps.ui.PixelButton
import com.staatseigentum.kollaps.ui.PixelLabel
import com.staatseigentum.kollaps.ui.PixelPanel
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The window that stands in front of everything when a big update is waiting.
 *
 * The phone gets a dialog it cannot dismiss; this is the same idea where the desktop keeps its
 * updater — except the desktop had nowhere to put it, because its only update surface is a card
 * buried in the Kosmos tab that nobody opens. A card cannot block, so this is a sheet over the
 * whole window instead.
 *
 * Only for a *big* step — a new major or minor. A patch stays where it always was, on the card,
 * because nothing depends on it and interrupting somebody for it would be rude.
 *
 * Two escapes, both deliberate:
 *
 * - if the check finds nothing, or cannot reach the network at all, the game simply starts. An
 *   update nobody can fetch must never be a locked door.
 * - if the download or the handover to the installer fails, the block lifts and says so. Being
 *   stuck one version behind is a far better outcome than being stuck outside the game.
 *
 * Everything is checked once per launch and never again: a player who has been running for two
 * hours is not the person this is for.
 */
@Composable
fun DesktopUpdateGate(onBeforeExit: () -> Unit, content: @Composable () -> Unit) {
    var required by remember { mutableStateOf<AvailableUpdate?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }
    var trouble by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val installed = DesktopUpdater.installedVersion() ?: return@LaunchedEffect
        val found = withContext(Dispatchers.IO) { DesktopUpdater.check() } ?: return@LaunchedEffect
        // The same rule the phone uses, read off the numbers rather than out of a flag on the
        // release, so there is nothing anybody has to remember to tick.
        if (found.version.isBigStepFrom(installed)) required = found
    }

    content()

    val update = required ?: return

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        /*
         * The curtain, and it has to be a sibling *behind* the panel rather than the panel's
         * parent.
         *
         * It was the parent, which is the obvious way to write "nothing behind this reacts" and is
         * exactly wrong: `PointerEventPass.Initial` travels from the outside in, so a parent that
         * consumes on it consumes before its own children are offered anything. The gate ate the
         * clicks meant for its own download button, and the one screen in the game a player cannot
         * leave had a button that did nothing.
         *
         * As a sibling drawn first, the panel sits on top and is hit first; whatever the panel does
         * not take lands here and stops. The game underneath keeps running — production does not
         * pause for an update — but no click reaches it.
         */
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Space)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                },
        )

        PixelPanel(
            modifier = Modifier.widthIn(max = 520.dp).padding(24.dp),
            border = Ember,
            padding = 18,
        ) {
            Column {
                PixelLabel("Diese Version musst du installieren", color = Ember, size = 16)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = update.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ein großes Update ändert, was im Spielstand steht. Zwei Fassungen " +
                        "nebeneinander vertragen sich dabei nicht — deshalb geht es hier nur " +
                        "vorwärts. Der Installer ersetzt die laufende Fassung; das Spiel " +
                        "schließt sich dafür.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )

                trouble?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Ember,
                    )
                }

                if (busy) {
                    Spacer(Modifier.height(14.dp))
                    PixelBar(
                        progress = progress,
                        color = Ember,
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                PixelButton(
                    label = if (busy) "Lädt …" else "Herunterladen und installieren",
                    onClick = {
                        if (busy) return@PixelButton
                        busy = true
                        trouble = null
                        Thread {
                            val file = DesktopUpdater.download(update) { progress = it }
                            if (file == null) {
                                trouble = "Download fehlgeschlagen."
                                busy = false
                                return@Thread
                            }
                            onBeforeExit()
                            // `install` does not return on success — it hands the file to Windows
                            // and quits, because msiexec cannot replace a file this process is
                            // holding open. Reaching the next line means it failed.
                            DesktopUpdater.install(file)
                            trouble = "Der Installer ließ sich nicht starten."
                            busy = false
                        }.apply { isDaemon = true }.start()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    accent = Ember,
                )

                // Only after something has actually gone wrong. Until then there is no way past
                // this; afterwards there has to be, because the alternative is a game nobody can
                // open because a server was down.
                if (trouble != null) {
                    Spacer(Modifier.height(8.dp))
                    PixelButton(
                        label = "Trotzdem spielen",
                        onClick = { required = null },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
