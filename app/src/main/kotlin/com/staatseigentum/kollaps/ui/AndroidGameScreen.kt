package com.staatseigentum.kollaps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
            override fun setNumberFormat(format: NumberFormat) = model.setNumberFormat(format)
            override fun importSave(block: String): Boolean = model.importSave(block)
            override fun exportSave(): String = model.exportSave()
        }
    }

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
