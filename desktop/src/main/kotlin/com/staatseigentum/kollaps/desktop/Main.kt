package com.staatseigentum.kollaps.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.GameScreen
import com.staatseigentum.kollaps.ui.SaveSlotPanel
import com.staatseigentum.kollaps.ui.SlotSummary

/**
 * The game, in a window, as a thing somebody can actually play.
 *
 * The same screen the phone runs, driven by the same rules, with a save file next to the user's
 * own — and it reads the same exported block the phone writes, because the format was text from
 * the start. It looks after its own updates too, for the same reason the phone does: this is not
 * shipped through a store, so nothing else is going to say that a new version exists.
 *
 * Arguments: `--tier 17` starts on a given rung, which is otherwise hours away, and `--frisch`
 * ignores whatever is in the save file.
 */
fun main(args: Array<String>) = application {
    val startTier = args.indexOf("--tier").takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1)?.toIntOrNull() }
        ?.minus(1)
    val ignoreSave = "--frisch" in args

    val game = remember {
        val loaded = if (ignoreSave) null else DesktopSave.load()
        DesktopGame(loaded ?: GameState.new(System.currentTimeMillis())).also { fresh ->
            startTier?.let { fresh.seekToTier(it) }
            // Credited before the first frame, so the report is on screen when the window opens
            // rather than a second later.
            if (loaded != null) fresh.creditTimeAway()
        }
    }

    if (!ignoreSave) println("Spielstand: ${DesktopSave.location()}")

    Window(
        onCloseRequest = {
            DesktopSave.save(game.state)
            exitApplication()
        },
        title = "Kollaps",
        // Wide enough for the two-column layout, and resizable down to a phone shape if that is
        // what somebody wants. Both are the same screen; only the width decides.
        state = rememberWindowState(size = DpSize(1_100.dp, 760.dp)),
    ) {
        DesktopPlatform {
            RunningGame(game)
        }
    }
}

/**
 * Drives the simulation off the frame clock, which is the desktop stand-in for the app's loop.
 *
 * Three clocks meet here, exactly as they do on the phone: the frame clock advances production,
 * the wall clock settles the lab and the two automation rules that read it, and a slower loop
 * writes the save. Autosaving on a timer rather than on every frame, because a save is a file
 * write and a frame is sixteen milliseconds.
 */
@Composable
fun RunningGame(game: DesktopGame, modifier: Modifier = Modifier) {
    LaunchedEffect(game) {
        var previous = 0L
        var sinceSave = 0.0
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) {
                    val seconds = (now - previous) / 1_000_000_000.0
                    game.tick(seconds)
                    game.settleWallClock()
                    sinceSave += seconds
                    if (sinceSave >= AUTOSAVE_SECONDS) {
                        sinceSave = 0.0
                        DesktopSave.save(game.state)
                    }
                }
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
        updateSection = { DesktopUpdateCard(onBeforeExit = { DesktopSave.save(game.state) }) },
        saveSlots = { DesktopSlots(game) },
    )
}

/**
 * The three save slots on the PC.
 *
 * Same panel the phone draws, filled from files in the user's home directory. Switching writes the
 * running game out first, exactly as on the phone — the order is the safety of the whole feature.
 */
@Composable
private fun DesktopSlots(game: DesktopGame) {
    var active by remember { mutableStateOf(DesktopSave.activeSlot()) }
    var summaries by remember { mutableStateOf(emptyList<SlotSummary>()) }

    LaunchedEffect(active, game.state.collapses, game.state.bigBangs) {
        summaries = (0 until DesktopSave.SLOTS).map { index ->
            val state = if (index == active) game.state else DesktopSave.load(index)
            SlotSummary(
                index = index,
                detail = describeSlot(state),
                isActive = index == active,
                isEmpty = state == null,
            )
        }
    }

    if (summaries.isEmpty()) return
    SaveSlotPanel(
        slots = summaries,
        onSwitch = { target ->
            DesktopSave.save(game.state, active)
            DesktopSave.setActiveSlot(target)
            active = target
            game.load(DesktopSave.load(target))
        },
    )
}

/** One line saying what is in a slot, or that there is nothing in it. */
private fun describeSlot(state: GameState?): String {
    if (state == null) return "Leer — hier fängt ein neues Spiel an."
    val parts = buildList {
        add(Tiers.forMass(state.runMass).name)
        if (state.collapses > 0) add("${state.collapses} Kollapse")
        if (state.bigBangs > 0) add("${state.bigBangs} Urknalle")
    }
    return parts.joinToString(" · ")
}

private const val AUTOSAVE_SECONDS = 12.0

/** The game at a fixed size and a fixed moment, for rendering to an image. */
@Composable
fun StillGame(game: DesktopGame, width: Int, height: Int, tab: Int = 0, section: Int = 0) {
    Box(modifier = Modifier.size(width.dp, height.dp)) {
        GameScreen(
            state = game.state,
            stats = game.stats,
            buyAmount = game.buyAmount,
            offlineReport = game.offlineReport,
            actions = game,
            modifier = Modifier.fillMaxSize(),
            startTab = tab,
            startSection = section,
        )
    }
}
