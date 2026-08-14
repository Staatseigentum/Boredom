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
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.Roles
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.UpgradeOffer
import com.staatseigentum.kollaps.data.SaveStore
import com.staatseigentum.kollaps.data.SlotPreference
import com.staatseigentum.kollaps.ui.SAVE_SLOTS
import com.staatseigentum.kollaps.ui.SlotSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.staatseigentum.kollaps.core.NumberFormat

/**
 * Owns the game state and drives the simulation while the app is in the foreground.
 *
 * All rules live in [GameEngine]; this class only decides *when* they run, keeps the save on
 * disk up to date and exposes what the UI needs.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * The slot this session is playing, and the store that goes with it.
     *
     * Both change together in [switchSlot] and nowhere else, so there is never a moment where the
     * game is writing into one slot while claiming to be in another.
     */
    private var slot = SlotPreference.active(application)
    private var store = SaveStore(application, slot)

    private val _activeSlot = MutableStateFlow(slot)
    val activeSlot: StateFlow<Int> = _activeSlot.asStateFlow()

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

    /**
     * Set while the collapse sequence is on screen.
     *
     * Plain rather than a flow: nothing draws it, the loop below simply asks each time round.
     */
    private var paused = false

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

                // Dropped rather than banked, and that is the whole difference between pausing
                // and stalling: `previous` has already moved on, so when the collapse sequence is
                // over the game carries on from now instead of paying out the three seconds it
                // spent being swallowed.
                if (paused) continue

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
    fun tapEmpty() {
        _state.value = GameEngine.tapEmpty(_state.value)
    }

    fun tap(): Double {
        val current = _state.value
        val gained = GameEngine.tapValue(current)
        _state.value = GameEngine.tap(current)
        return gained
    }

    fun buyCollector(collectorId: String) {
        _state.value = GameEngine.buyCollector(_state.value, collectorId, buyAmount.value)
    }

    fun cycleRole(collectorId: String) {
        _state.value = Roles.cycle(_state.value, collectorId)
    }

    fun buyUpgrade(upgradeId: String) {
        _state.value = GameEngine.buyUpgrade(_state.value, upgradeId)
    }

    fun buyFuser(stageId: String) {
        _state.value = GameEngine.buyFuser(_state.value, stageId, buyAmount.value)
    }

    fun openOrbit() {
        _state.value = GameEngine.openOrbit(_state.value)
    }

    fun seedSatellite(orbitIndex: Int) {
        _state.value = GameEngine.seedSatellite(_state.value, orbitIndex)
    }

    fun mergeSatellites(from: Int, to: Int) {
        _state.value = GameEngine.mergeSatellites(_state.value, from, to)
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

    fun bigBang(pathId: String) {
        _state.value =
            GameEngine.bigBang(_state.value, System.currentTimeMillis(), pathId)
        persist()
    }

    fun buyAeonUpgrade(upgradeId: String) {
        _state.value = GameEngine.buyAeonUpgrade(_state.value, upgradeId)
        persist()
    }

    fun forgeAlloy(alloyId: String) {
        _state.value = GameEngine.forgeAlloy(_state.value, alloyId)
        persist()
    }

    fun assignGalaxy(slot: Int, jobId: String) {
        _state.value = GameEngine.assignGalaxy(_state.value, slot, jobId)
        persist()
    }

    fun mergeGalaxies(keepSlot: Int, absorbSlot: Int) {
        _state.value = GameEngine.mergeGalaxies(_state.value, keepSlot, absorbSlot)
        persist()
    }

    fun answerFind(answerId: String) {
        _state.value = GameEngine.answerFind(_state.value, answerId)
        persist()
    }

    fun buyPathNode(nodeId: String) {
        _state.value = GameEngine.buyPathNode(_state.value, nodeId)
        persist()
    }

    fun startChallenges(challengeIds: Set<String>) {
        _state.value = GameEngine.startChallenges(_state.value, challengeIds, System.currentTimeMillis())
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

    fun setSkin(skinId: String) {
        _state.value = GameEngine.setSkin(_state.value, skinId)
        persist()
    }

    fun setAutoBuy(on: Boolean) {
        _state.value = GameEngine.setAutoBuy(_state.value, on)
        persist()
    }

    fun setNumberFormat(format: NumberFormat) {
        _state.value = GameEngine.setNumberFormat(_state.value, format)
        persist()
    }

    fun dismissTutorial() {
        _state.value = GameEngine.dismissTutorial(_state.value)
        persist()
    }

    /**
     * Holds production still while the collapse plays out.
     *
     * Not persisted, and deliberately so: it describes what is on screen, not what the save is.
     * A game closed while paused reopens running, which is the only sane outcome — the sequence
     * that switched it on is long gone by then.
     */
    fun setPaused(on: Boolean) {
        paused = on
    }

    fun setStatus(on: Boolean) {
        _state.value = GameEngine.setStatus(_state.value, on)
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

    /**
     * The three things the ongoing status line shows, or `null` when it should not be posted.
     *
     * Assembled here rather than in the notification, for the same reason the reminder text is:
     * every decision about wording and formatting stays on this side, and what crosses over is a
     * finished string.
     */
    fun statusLine(): StatusLine? {
        val state = _state.value
        if (!state.remindersOn || !state.statusOn) return null

        val stats = GameEngine.stats(state)
        if (stats.massPerSecond <= 0.0 && state.activeResearch == null) return null

        val project = ResearchTree.byId(state.activeResearch)
        return StatusLine(
            headline = "${Numbers.formatMass(stats.massPerSecond)}/s · ${stats.tier.label}",
            detail = when {
                project != null -> "Labor: ${project.name}"
                // Said once, plainly, rather than left blank: the number above is a snapshot and
                // the shade has no way of showing that on its own.
                else -> "Stand beim Schließen"
            },
            researchDoneAtMillis = state.researchDoneAt.takeIf { project != null && it > 0 },
        )
    }

    /** Replaces the running game with an imported one. Saved at once, so it cannot be lost. */
    fun importSave(block: String): Boolean {
        val loaded = SaveCodec.import(block) ?: return false
        _state.value = loaded
        persist()
        return true
    }

    fun exportSave(): String = SaveCodec.export(_state.value)

    /**
     * Puts the running game away and picks up whatever is in another slot.
     *
     * The order is the whole point: the current state is written out before anything else
     * happens, so a mis-tap costs a tab and not an afternoon. An empty slot starts a new game,
     * which is what an empty slot is for.
     */
    fun switchSlot(target: Int) {
        if (target == slot || target !in 0 until SAVE_SLOTS) return
        viewModelScope.launch {
            store.save(_state.value)

            slot = target
            store = SaveStore(getApplication(), target)
            SlotPreference.setActive(getApplication(), target)
            _activeSlot.value = target

            val loaded = store.load()
            _state.value = loaded ?: GameState.new(System.currentTimeMillis())
            Numbers.format = NumberFormat.byName(_state.value.numberFormat)

            // A slot picked up after a week away has earned its offline production exactly as the
            // one being put down would have.
            if (loaded != null) {
                val report = GameEngine.applyOffline(_state.value, System.currentTimeMillis())
                _state.value = report.state
                _offlineReport.value = report.takeIf { it.worthShowing }
            } else {
                _offlineReport.value = null
            }
            persist()
        }
    }

    /** What each slot holds, for the picker. Reads all three files, which is cheap enough. */
    suspend fun slotSummaries(): List<SlotSummary> = (0 until SAVE_SLOTS).map { index ->
        val state = if (index == slot) _state.value else SaveStore(getApplication(), index).load()
        SlotSummary(
            index = index,
            detail = describe(state),
            isActive = index == slot,
            isEmpty = state == null,
        )
    }

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

/** What the ongoing notification puts on screen. Three finished strings and a target time. */
data class StatusLine(
    val headline: String,
    val detail: String?,
    val researchDoneAtMillis: Long?,
)

/** One line saying what is in a slot, or that there is nothing in it. */
private fun describe(state: GameState?): String {
    if (state == null) return "Leer — hier fängt ein neues Spiel an."
    val tier = com.staatseigentum.kollaps.core.Tiers.forMass(state.runMass)
    val parts = buildList {
        add(tier.label)
        if (state.collapses > 0) add("${state.collapses} Kollapse")
        if (state.bigBangs > 0) add("${state.bigBangs} Urknalle")
    }
    return parts.joinToString(" · ")
}
