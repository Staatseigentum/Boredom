package com.staatseigentum.kollaps.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.core.SaveCodec
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.ui.GameActions

/**
 * The game, driven from plain Compose state instead of a view model.
 *
 * Deliberately without persistence: the harness is for looking at the game, and a save file that
 * survives between runs would make every experiment depend on the last one. Jumping straight to
 * an interesting state is what [seekToTier] is for.
 */
class DesktopGame(start: GameState = GameState.new(NOW)) : GameActions {

    var state by mutableStateOf(start)
        private set

    // Backed privately: a `var buyAmount` would generate a setter with the same JVM signature
    // as the interface method below, which the compiler rejects.
    private var currentBuyAmount by mutableStateOf(BuyAmount.ONE)
    val buyAmount: BuyAmount get() = currentBuyAmount

    var offlineReport by mutableStateOf<OfflineReport?>(null)

    val stats: Stats get() = GameEngine.stats(state)

    fun tick(seconds: Double) {
        state = GameEngine.tick(state, seconds)
    }

    override fun tap(): Double {
        val gained = GameEngine.tapValue(state)
        state = GameEngine.tap(state)
        return gained
    }

    override fun setBuyAmount(amount: BuyAmount) {
        currentBuyAmount = amount
    }

    override fun buyCollector(id: String) {
        state = GameEngine.buyCollector(state, id, currentBuyAmount)
    }

    override fun buyUpgrade(id: String) {
        state = GameEngine.buyUpgrade(state, id)
    }

    override fun buyFuser(id: String) {
        state = GameEngine.buyFuser(state, id, currentBuyAmount)
    }

    override fun startResearch(id: String) {
        state = GameEngine.startResearch(state, id, System.currentTimeMillis())
    }

    override fun cancelResearch() {
        state = GameEngine.cancelResearch(state)
    }

    override fun collapse() {
        state = GameEngine.collapse(state, NOW)
    }

    override fun dismissOfflineReport() {
        offlineReport = null
    }

    override fun acknowledgeTier() {
        state = GameEngine.acknowledgeTier(state)
    }

    override fun catchComet(comet: Comet) {
        state = GameEngine.catchComet(state, comet)
    }

    override fun chooseEvent(optionIndex: Int) {
        state = GameEngine.chooseEvent(state, optionIndex)
    }

    override fun dismissEvent() {
        state = GameEngine.dismissEvent(state)
    }

    override fun bigBang() {
        state = GameEngine.bigBang(state, NOW)
    }

    override fun buyAeonUpgrade(id: String) {
        state = GameEngine.buyAeonUpgrade(state, id)
    }

    override fun startChallenge(id: String) {
        state = GameEngine.startChallenge(state, id, NOW)
    }

    override fun abortChallenge() {
        state = GameEngine.abortChallenge(state, NOW)
    }

    override fun finishChallenge() {
        state = GameEngine.finishChallenge(state, NOW)
    }

    override fun buyPrestigeUpgrade(id: String) {
        state = GameEngine.buyPrestigeUpgrade(state, id)
    }

    override fun setSound(on: Boolean) {
        state = GameEngine.setSound(state, on)
    }

    override fun setHaptics(on: Boolean) {
        state = GameEngine.setHaptics(state, on)
    }

    override fun setAutoBuy(on: Boolean) {
        state = GameEngine.setAutoBuy(state, on)
    }

    override fun setReminders(on: Boolean) {
        state = GameEngine.setReminders(state, on)
    }

    override fun importSave(block: String): Boolean {
        val loaded = SaveCodec.import(block) ?: return false
        state = loaded
        return true
    }

    override fun exportSave(): String = SaveCodec.export(state)

    /**
     * Puts the run straight onto a tier, with the celebration already acknowledged.
     *
     * Reaching the second to last body by playing takes about four hours, which is not a way to
     * look at a bug on it.
     */
    /**
     * Shapes the state directly.
     *
     * The harness regularly needs states no amount of playing would reach in a build step — a
     * player six collapses in, with a shelf of achievements — and the alternative is a seek
     * method per screenshot.
     */
    fun edit(block: (GameState) -> GameState) {
        state = block(state)
    }

    fun seekToTier(index: Int) {
        val tier = com.staatseigentum.kollaps.core.Tiers.all[index]
        state = state.copy(
            mass = tier.threshold,
            runMass = tier.threshold,
            totalMass = tier.threshold,
            celebratedTier = index,
        )
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
