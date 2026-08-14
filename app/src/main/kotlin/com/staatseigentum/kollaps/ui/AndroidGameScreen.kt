package com.staatseigentum.kollaps.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staatseigentum.kollaps.GameViewModel
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.update.UpdateViewModel
import com.staatseigentum.kollaps.core.NumberFormat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Binds the platform-free [GameScreen] to the app's view models.
 *
 * All the Android-shaped concerns live here — lifecycle-aware collection, persistence, the
 * updater — so the screen itself stays something the desktop harness can run unchanged.
 */
@Composable
fun GameScreen(model: GameViewModel, updateModel: UpdateViewModel) {
    /*
     * Nothing is drawn until the save is back, and that is a bug fix rather than a nicety.
     *
     * The view model starts on `GameState.new()` and loads the file in a coroutine, so the first
     * composition always saw a brand new game: no mass, no collectors, and — the part that showed
     * — *no collapses*. [GameScreen] takes the counters it sees on that first frame as its
     * baseline for "has anything happened since". A moment later the real save arrived with
     * fourteen collapses on it, the counter had gone up by fourteen, and the screen did the only
     * thing it could reasonably conclude: it played the collapse sequence. On every single launch.
     *
     * The same trap was set for the big bang, the research chime and the ignition — all of them
     * compare against a baseline read before the save exists. Waiting closes all four at once,
     * which is why this is here rather than four guards further down.
     */
    val ready by model.ready.collectAsStateWithLifecycle()
    if (!ready) return

    val state by model.state.collectAsStateWithLifecycle()
    val stats by model.stats.collectAsStateWithLifecycle()
    val buyAmount by model.buyAmount.collectAsStateWithLifecycle()
    val offlineReport by model.offlineReport.collectAsStateWithLifecycle()
    val updatePrompt by updateModel.prompt.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { updateModel.checkOnLaunch() }

    val actions = remember(model) {
        object : GameActions {
            override fun tap(): Double = model.tap()
            override fun tapEmpty() = model.tapEmpty()
            override fun setBuyAmount(amount: BuyAmount) = model.setBuyAmount(amount)
            override fun buyCollector(id: String) = model.buyCollector(id)
            override fun cycleRole(id: String) = model.cycleRole(id)
            override fun buyUpgrade(id: String) = model.buyUpgrade(id)
            override fun buyFuser(id: String) = model.buyFuser(id)
            override fun openOrbit() = model.openOrbit()
            override fun seedSatellite(orbitIndex: Int) = model.seedSatellite(orbitIndex)
            override fun mergeSatellites(from: Int, to: Int) = model.mergeSatellites(from, to)
            override fun startResearch(id: String) = model.startResearch(id)
            override fun cancelResearch() = model.cancelResearch()
            override fun cycleAutomation(id: String) = model.cycleAutomation(id)
            override fun collapse() = model.collapse()
            override fun dismissOfflineReport() = model.dismissOfflineReport()
            override fun acknowledgeTier() = model.acknowledgeTier()
            override fun catchComet(comet: Comet) = model.catchComet(comet)
            override fun chooseEvent(optionIndex: Int) = model.chooseEvent(optionIndex)
            override fun dismissEvent() = model.dismissEvent()
            override fun bigBang(pathId: String) = model.bigBang(pathId)
            override fun buyAeonUpgrade(id: String) = model.buyAeonUpgrade(id)
            override fun forgeAlloy(id: String) = model.forgeAlloy(id)
            override fun assignGalaxy(slot: Int, jobId: String) = model.assignGalaxy(slot, jobId)
            override fun mergeGalaxies(keepSlot: Int, absorbSlot: Int) =
                model.mergeGalaxies(keepSlot, absorbSlot)
            override fun developGalaxy(slot: Int) = model.developGalaxy(slot)
            override fun answerFind(answerId: String) = model.answerFind(answerId)
            override fun claimContract(id: String) = model.claimContract(id)
            override fun buyPathNode(id: String) = model.buyPathNode(id)
            override fun startChallenges(ids: Set<String>) = model.startChallenges(ids)
            override fun abortChallenge() = model.abortChallenge()
            override fun finishChallenge() = model.finishChallenge()
            override fun buyPrestigeUpgrade(id: String) = model.buyPrestigeUpgrade(id)
            override fun buyInvestment(id: String, amount: Int) = model.buyInvestment(id, amount)
            override fun eraseSave() = model.eraseSave()
            override fun setSound(on: Boolean) = model.setSound(on)
            override fun setHaptics(on: Boolean) = model.setHaptics(on)
            override fun setMusic(on: Boolean) = model.setMusic(on)
            override fun setSkin(id: String) = model.setSkin(id)
            override fun setAutoBuy(on: Boolean) = model.setAutoBuy(on)
            override fun setReminders(on: Boolean) = model.setReminders(on)
            override fun setStatus(on: Boolean) = model.setStatus(on)
            override fun dismissTutorial() = model.dismissTutorial()
            override fun setPaused(on: Boolean) = model.setPaused(on)
            override fun setNumberFormat(format: NumberFormat) = model.setNumberFormat(format)
            override fun importSave(block: String): Boolean = model.importSave(block)
            override fun exportSave(): String = model.exportSave()
        }
    }

    /*
     * Whether the phone has been told to keep still.
     *
     * Android has no flag for this, only the scale it multiplies every animation duration by —
     * developer options and several accessibility settings all end up writing a zero there. Read
     * once per composition rather than watched: somebody who changes it mid-collapse has bigger
     * things going on, and it is the sequence's own start that reads it.
     */
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        GameScreen(
            state = state,
            stats = stats,
            buyAmount = buyAmount,
            offlineReport = offlineReport,
            actions = actions,
            updateSection = { UpdateCard(updateModel) },
            saveSlots = {
                // Re-read whenever the slot changes rather than once: switching has to leave the
                // list describing where the player actually is.
                val slot by model.activeSlot.collectAsStateWithLifecycle()
                var summaries by remember { mutableStateOf(emptyList<SlotSummary>()) }
                LaunchedEffect(slot, state.collapses, state.bigBangs) {
                    summaries = model.slotSummaries()
                }
                if (summaries.isNotEmpty()) {
                    SaveSlotPanel(slots = summaries, onSwitch = model::switchSlot)
                }
            },
            updateDialog = { if (updatePrompt) UpdateDialog(updateModel) },
        )
    }
}
