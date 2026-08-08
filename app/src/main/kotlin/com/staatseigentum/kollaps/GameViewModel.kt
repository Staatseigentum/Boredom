package com.staatseigentum.kollaps

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.CollectorOffer
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.core.SaveCodec
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.UpgradeOffer
import com.staatseigentum.kollaps.data.SaveStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * Owns the game state and drives the simulation while the app is in the foreground.
 *
 * All rules live in [GameEngine]; this class only decides *when* they run, keeps the save on
 * disk up to date and exposes what the UI needs.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SaveStore(application)

    private val _state = MutableStateFlow(GameState.new(System.currentTimeMillis()))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _offlineReport = MutableStateFlow<OfflineReport?>(null)
    val offlineReport: StateFlow<OfflineReport?> = _offlineReport.asStateFlow()

    val stats: StateFlow<Stats> = _state
        .map { GameEngine.stats(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameEngine.stats(_state.value))

    /** How many collectors a single purchase buys. */
    val buyAmount = MutableStateFlow(BuyAmount.ONE)

    private var loop: Job? = null
    private var lastSaveUptime = 0L
    private var restored = false

    init {
        viewModelScope.launch {
            store.load()?.let { _state.value = it }
            restored = true
            creditOfflineTime()
            _ready.value = true
        }
    }

    // ------------------------------------------------------------------ lifecycle

    fun onForeground() {
        if (restored) creditOfflineTime()
        if (loop?.isActive == true) return
        loop = viewModelScope.launch {
            var previous = SystemClock.elapsedRealtime()
            while (true) {
                delay(TICK_MILLIS)
                val now = SystemClock.elapsedRealtime()
                val seconds = (now - previous).coerceAtLeast(0L) / 1_000.0
                previous = now
                // The lab and two of the automation rules run on the wall clock rather than on
                // elapsed play time, so they are settled next to the tick rather than inside it.
                _state.value = GameEngine.onWallClock(
                    GameEngine.tick(_state.value, seconds),
                    System.currentTimeMillis(),
                )
                if (now - lastSaveUptime >= AUTOSAVE_MILLIS) {
                    lastSaveUptime = now
                    persist()
                }
            }
        }
    }

    fun onBackground() {
        loop?.cancel()
        loop = null
        persist()
    }

    private fun creditOfflineTime() {
        val now = System.currentTimeMillis()
        val report = GameEngine.applyOffline(_state.value, now)
        // Settled after the offline credit, not before: a project that finished during the night
        // should not have been multiplying the production it was away for.
        _state.value = GameEngine.onWallClock(report.state, now)
        if (report.worthShowing) _offlineReport.value = report
    }

    private fun persist() {
        val snapshot = GameEngine.touch(_state.value, System.currentTimeMillis())
        _state.value = snapshot
        viewModelScope.launch { store.save(snapshot) }
    }

    // ------------------------------------------------------------------ player actions

    /** Taps the body and returns how much mass that yielded, for the floating number. */
    fun tap(): Double {
        val current = _state.value
        val gained = GameEngine.tapValue(current)
        _state.value = GameEngine.tap(current)
        return gained
    }

    fun buyCollector(collectorId: String) {
        _state.value = GameEngine.buyCollector(_state.value, collectorId, buyAmount.value)
    }

    fun buyUpgrade(upgradeId: String) {
        _state.value = GameEngine.buyUpgrade(_state.value, upgradeId)
    }

    fun buyFuser(stageId: String) {
        _state.value = GameEngine.buyFuser(_state.value, stageId, buyAmount.value)
    }

    fun startResearch(projectId: String) {
        _state.value =
            GameEngine.startResearch(_state.value, projectId, System.currentTimeMillis())
    }

    fun cancelResearch() {
        _state.value = GameEngine.cancelResearch(_state.value)
    }

    fun cycleAutomation(ruleId: String) {
        _state.value = GameEngine.cycleAutomation(_state.value, ruleId)
    }

    fun setBuyAmount(amount: BuyAmount) {
        buyAmount.value = amount
    }

    fun collapse() {
        _state.value = GameEngine.collapse(_state.value, System.currentTimeMillis())
        persist()
    }

    fun acknowledgeTier() {
        _state.value = GameEngine.acknowledgeTier(_state.value)
    }

    fun dismissOfflineReport() {
        _offlineReport.value = null
    }

    fun catchComet(comet: Comet) {
        _state.value = GameEngine.catchComet(_state.value, comet)
    }

    fun chooseEvent(optionIndex: Int) {
        _state.value = GameEngine.chooseEvent(_state.value, optionIndex)
        persist()
    }

    fun dismissEvent() {
        _state.value = GameEngine.dismissEvent(_state.value)
    }

    fun bigBang() {
        _state.value = GameEngine.bigBang(_state.value, System.currentTimeMillis())
        persist()
    }

    fun buyAeonUpgrade(upgradeId: String) {
        _state.value = GameEngine.buyAeonUpgrade(_state.value, upgradeId)
        persist()
    }

    fun startChallenge(challengeId: String) {
        _state.value = GameEngine.startChallenge(_state.value, challengeId, System.currentTimeMillis())
        persist()
    }

    fun abortChallenge() {
        _state.value = GameEngine.abortChallenge(_state.value, System.currentTimeMillis())
        persist()
    }

    fun finishChallenge() {
        _state.value = GameEngine.finishChallenge(_state.value, System.currentTimeMillis())
        persist()
    }

    fun buyInvestment(investmentId: String, amount: Int) {
        _state.value = GameEngine.buyInvestment(_state.value, investmentId, amount)
        persist()
    }

    /**
     * Throws the save away, on disk and in memory, and starts a brand new game.
     *
     * The file is cleared before the fresh state is written, so a crash between the two leaves
     * nothing rather than half of the old game. There is no undo, which is why the button that
     * calls this asks twice.
     */
    fun eraseSave() {
        viewModelScope.launch {
            store.clear()
            val fresh = GameState.new(System.currentTimeMillis())
            _state.value = fresh
            _offlineReport.value = null
            store.save(fresh)
        }
    }

    fun buyPrestigeUpgrade(upgradeId: String) {
        _state.value = GameEngine.buyPrestigeUpgrade(_state.value, upgradeId)
        persist()
    }

    fun setSound(on: Boolean) {
        _state.value = GameEngine.setSound(_state.value, on)
        persist()
    }

    fun setHaptics(on: Boolean) {
        _state.value = GameEngine.setHaptics(_state.value, on)
        persist()
    }

    fun setMusic(on: Boolean) {
        _state.value = GameEngine.setMusic(_state.value, on)
        persist()
    }

    fun setAutoBuy(on: Boolean) {
        _state.value = GameEngine.setAutoBuy(_state.value, on)
        persist()
    }

    fun setReminders(on: Boolean) {
        _state.value = GameEngine.setReminders(_state.value, on)
        persist()
    }

    /**
     * What the reminder should say, or `null` when there is nothing worth saying.
     *
     * Computed here, where the rules and the number formatting already live, so the worker that
     * eventually posts it only has to hand a finished string to the system.
     */
    fun reminderText(): String? {
        val state = _state.value
        if (!state.remindersOn) return null
        val stats = GameEngine.stats(state)
        val gained = stats.massPerSecond * stats.offlineCapSeconds * stats.offlineEfficiency
        if (gained <= 0.0) return null
        return "${Numbers.formatMass(gained)} liegen bereit — mehr passt nicht in den Speicher."
    }

    /** The player's offline cap, which is when the collectors stop earning. */
    fun offlineCapSeconds(): Long = GameEngine.stats(_state.value).offlineCapSeconds

    /** Replaces the running game with an imported one. Saved at once, so it cannot be lost. */
    fun importSave(block: String): Boolean {
        val loaded = SaveCodec.import(block) ?: return false
        _state.value = loaded
        persist()
        return true
    }

    fun exportSave(): String = SaveCodec.export(_state.value)

    // ------------------------------------------------------------------ derived views

    fun collectorOffers(state: GameState, amount: BuyAmount): List<CollectorOffer> =
        GameEngine.collectorOffers(state, amount)

    fun upgradeOffers(state: GameState): List<UpgradeOffer> = GameEngine.upgradeOffers(state)

    override fun onCleared() {
        super.onCleared()
        loop?.cancel()
    }

    private companion object {
        /**
         * Ten steps per second. Every step recomposes the counter and the shop rows, so going
         * faster costs battery without making the numbers look any more alive.
         */
        const val TICK_MILLIS = 100L
        const val AUTOSAVE_MILLIS = 10_000L
    }
}
