package com.staatseigentum.kollaps.desktop

import com.staatseigentum.kollaps.core.i18n.Lang
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.ui.PixelBar
import com.staatseigentum.kollaps.ui.PixelButton
import com.staatseigentum.kollaps.ui.PixelLabel
import com.staatseigentum.kollaps.ui.PixelPanel
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Where the launch has got to. */
private sealed interface Stage {
    /** Asking the feed. The only stage a fresh launch normally passes through. */
    data object Looking : Stage

    /** Fetching the installer. How far along lives in its own state, so the bar can move. */
    data class Fetching(val update: AvailableUpdate) : Stage

    /**
     * Something went wrong and the update matters enough to say so.
     *
     * Only ever reached for a big step. A patch that fails to download is not worth a screen — the
     * game starts and the card in the Kosmos tab is still there for whenever the network is back.
     */
    data class Stuck(val update: AvailableUpdate, val reason: String) : Stage

    /** Play. */
    data object Done : Stage
}

/**
 * The few seconds before the game, spent making sure it is the current game.
 *
 * The updater was already here, in two places, and both of them asked the player to do something:
 * a card in the Kosmos tab that had to be found, and a window in front of everything that had to be
 * pressed. A sideloaded game has no store behind it, so "the player has to notice" was the whole
 * distribution strategy — and a player who never opens the Kosmos tab simply stays on an old
 * version for ever.
 *
 * So the launch does it. Look, fetch, hand over, quit; the installer restarts a newer game. In the
 * ordinary case — nothing new — this costs one request and about half a second.
 *
 * ## The rules that keep it from being a trap
 *
 * Every one of these exists because a launcher that can hang is worse than no launcher at all.
 *
 * - **Everything is on a clock.** The look gets [LOOK_TIMEOUT_MILLIS], the fetch gets
 *   [FETCH_TIMEOUT_MILLIS]. A server that accepts a connection and then says nothing for ever is a
 *   completely ordinary thing for a server to do, and it must not mean a game that never opens.
 * - **Every failure ends in the game.** No network, a feed that changed shape, a half-written
 *   download, a machine that is not Windows: all of it falls through to playing. The only thing
 *   that holds the door is a *big* update that could not be installed — and even that offers a way
 *   past after it has actually failed.
 * - **It happens once per launch.** Nothing here retries, polls, or wakes up later.
 * - **`--kein-update` skips the whole thing**, for developing against a released version without
 *   being upgraded out from under it.
 */
@Composable
fun DesktopBootstrap(
    skip: Boolean,
    onBeforeExit: () -> Unit,
    content: @Composable () -> Unit,
) {
    var stage by remember { mutableStateOf<Stage>(if (skip) Stage.Done else Stage.Looking) }
    var progress by remember { mutableFloatStateOf(0f) }
    // Keyed on, so that "try again" actually tries again. Without it the effect had already run
    // for this composition and the retry button would have shown "Suche nach Updates …" for ever
    // while looking for nothing at all.
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(skip, attempt) {
        if (skip) return@LaunchedEffect
        if (stage !is Stage.Looking) return@LaunchedEffect

        // Nothing to install on a machine with no installer, and nothing to compare against when
        // the version was never stamped in — which is how this runs from Gradle.
        val installed = DesktopUpdater.installedVersion()
        if (installed == null || !DesktopUpdater.canInstall) {
            stage = Stage.Done
            return@LaunchedEffect
        }

        val found = withTimeoutOrNull(LOOK_TIMEOUT_MILLIS) {
            withContext(Dispatchers.IO) { DesktopUpdater.check() }
        }
        if (found == null) {
            stage = Stage.Done
            return@LaunchedEffect
        }

        val required = found.version.isBigStepFrom(installed)
        stage = Stage.Fetching(found)
        progress = 0f

        val file = withTimeoutOrNull(FETCH_TIMEOUT_MILLIS) {
            withContext(Dispatchers.IO) {
                DesktopUpdater.download(found) { progress = it }
            }
        }
        if (file == null) {
            // A patch nobody could fetch is not worth a word. A big step is.
            stage = if (required) Stage.Stuck(found, Lang.t("Der Download ist fehlgeschlagen.")) else Stage.Done
            return@LaunchedEffect
        }

        onBeforeExit()
        // Does not return when it works: it hands the file to Windows and quits, because msiexec
        // cannot replace a file this process is holding open.
        withContext(Dispatchers.IO) { DesktopUpdater.install(file) }
        stage = if (required) {
            Stage.Stuck(found, Lang.t("Der Installer ließ sich nicht starten."))
        } else {
            Stage.Done
        }
    }

    when (val current = stage) {
        Stage.Done -> content()
        Stage.Looking -> Curtain { PixelLabel(Lang.t("Suche nach Updates …"), color = Muted, size = 13) }

        is Stage.Fetching -> Curtain {
            PixelLabel(
                Lang.t("Version %s wird geladen", current.update.version.canonical()),
                color = Ember,
                size = 13,
            )
            Spacer(Modifier.height(12.dp))
            PixelBar(
                progress = progress,
                color = Ember,
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Lang.t("Danach schließt sich das Spiel und der Installer übernimmt."),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
            )
        }

        is Stage.Stuck -> Curtain {
            PixelLabel(Lang.t("Diese Version musst du installieren"), color = Ember, size = 14)
            Spacer(Modifier.height(8.dp))
            Text(
                text = current.update.title,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Lang.t(
                    "%s Ein großes Update ändert, was im Spielstand steht — zwei Fassungen " +
                        "nebeneinander vertragen sich dabei nicht.",
                    current.reason,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            PixelButton(
                label = Lang.t("Noch einmal versuchen"),
                onClick = {
                    stage = Stage.Looking
                    attempt++
                },
                modifier = Modifier.fillMaxWidth(),
                accent = Ember,
            )
            Spacer(Modifier.height(8.dp))
            PixelButton(
                label = Lang.t("Seite im Browser öffnen"),
                onClick = { DesktopUpdater.openReleasesPage() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            // Only here, on a screen that is already reporting a failure. Being one version behind
            // is a far better outcome than being locked out of a game because a server was down.
            PixelButton(
                label = Lang.t("Trotzdem spielen"),
                onClick = { stage = Stage.Done },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The launch screen: the game's name, and one line about what is happening.
 *
 * Deliberately the whole window rather than an overlay. Nothing is running behind it yet — the
 * game has not started — so there is nothing to see through to and nothing to block.
 */
@Composable
private fun Curtain(body: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Space),
        contentAlignment = Alignment.Center,
    ) {
        PixelPanel(
            modifier = Modifier.widthIn(max = 460.dp).padding(24.dp),
            border = Nebula,
            padding = 20,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PixelLabel("KOLLAPS", color = Nebula, size = 22)
                Spacer(Modifier.height(14.dp))
                body()
            }
        }
    }
}

/**
 * How long the feed gets to answer.
 *
 * Six seconds, which is generous for one small request and short enough that nobody sitting in
 * front of a dead network wonders whether the game is broken.
 */
private const val LOOK_TIMEOUT_MILLIS = 6_000L

/**
 * And how long the installer gets to arrive.
 *
 * Sixty megabytes over a bad connection is minutes, not seconds, so this is deliberately long —
 * but it is not infinite, because a stalled socket looks exactly like a slow one and only one of
 * them ever finishes.
 */
private const val FETCH_TIMEOUT_MILLIS = 10 * 60_000L
