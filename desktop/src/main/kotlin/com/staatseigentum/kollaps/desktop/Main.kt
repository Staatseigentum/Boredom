package com.staatseigentum.kollaps.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.staatseigentum.kollaps.ui.GameScreen

/**
 * Plays the game in a window, at phone proportions.
 *
 * Pass a tier number to start there, e.g. `--tier 17` for the neutron star, which is otherwise
 * four hours away.
 */
fun main(args: Array<String>) = application {
    val startTier = args.indexOf("--tier").takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1)?.toIntOrNull() }
        ?.minus(1)

    val game = remember { DesktopGame().also { startTier?.let { tier -> it.seekToTier(tier) } } }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Kollaps — Testfenster",
        state = rememberWindowState(size = DpSize(411.dp, 891.dp)),
    ) {
        DesktopPlatform {
            RunningGame(game)
        }
    }
}

/** Drives the simulation off the frame clock, which is the desktop stand-in for the app's loop. */
@Composable
fun RunningGame(game: DesktopGame, modifier: Modifier = Modifier) {
    LaunchedEffect(game) {
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) game.tick((now - previous) / 1_000_000_000.0)
                previous = now
            }
        }
    }

    GameScreen(
        state = game.state,
        stats = game.stats,
        buyAmount = game.buyAmount,
        offlineReport = game.offlineReport,
        actions = game,
        modifier = modifier,
    )
}

/** The game at a fixed size and a fixed moment, for rendering to an image. */
@Composable
fun StillGame(game: DesktopGame, width: Int, height: Int) {
    Box(modifier = Modifier.size(width.dp, height.dp)) {
        GameScreen(
            state = game.state,
            stats = game.stats,
            buyAmount = game.buyAmount,
            offlineReport = game.offlineReport,
            actions = game,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
