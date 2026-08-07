package com.staatseigentum.kollaps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staatseigentum.kollaps.GameViewModel
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.update.UpdateViewModel

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
            override fun setBuyAmount(amount: BuyAmount) = model.setBuyAmount(amount)
            override fun buyCollector(id: String) = model.buyCollector(id)
            override fun buyUpgrade(id: String) = model.buyUpgrade(id)
            override fun collapse() = model.collapse()
            override fun dismissOfflineReport() = model.dismissOfflineReport()
            override fun acknowledgeTier() = model.acknowledgeTier()
        }
    }

    GameScreen(
        state = state,
        stats = stats,
        buyAmount = buyAmount,
        offlineReport = offlineReport,
        actions = actions,
        updateSection = { UpdateCard(updateModel) },
        updateDialog = { if (updatePrompt) UpdateDialog(updateModel) },
    )
}
