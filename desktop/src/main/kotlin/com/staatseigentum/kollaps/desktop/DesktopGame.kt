package com.staatseigentum.kollaps.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.core.SaveCodec
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.NumberFormat
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.Roles
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.ui.GameActions

/**
 * The game, driven from plain Compose state instead of a view model.
 *
 * Holds no save file of its own. The window in `Main.kt` loads one and writes it back; the
 * screenshot harness hands in a state directly and never persists anything, which is what keeps
 * every rendered picture independent of the last run.
 */
class DesktopGame(start: GameState = GameState.new(NOW)) : GameActions {

    var state by mutableStateOf(start)
        private set

    // Backed privately: a `var buyAmount` would generate a setter with the same JVM signature
    // as the interface method below, which the compiler rejects.
    private var currentBuyAmount by mutableStateOf(BuyAmount.ONE)
    val buyAmount: BuyAmount get() = currentBuyAmount

    var offlineReport by mutableStateOf<OfflineReport?>(null)

    /** Set while the collapse sequence is on screen. Never saved: it describes a picture. */
    var paused = false
        private set

    /*
     * Worked out once per state rather than on every read.
     *
     * This was a plain getter, and [GameEngine.stats] is not cheap — it folds every owned upgrade,
     * investment, path node and Äonen purchase into a fresh set of modifiers before it can answer.
     * The screen reads it twice per composition and composes on every frame, so the whole fold ran
     * a hundred and twenty times a second and threw all of it away.
     *
     * Keyed on identity, not equality: the state is immutable, so a new object is the only way it
     * can have changed, and comparing forty fields to find that out would cost more than it saves.
     */
    private var statsOf: GameState? = null
    private var statsWere: Stats? = null

    val stats: Stats
        get() {
            val current = state
            val cached = statsWere
            if (cached != null && statsOf === current) return cached
            return GameEngine.stats(current).also {
                statsOf = current
                statsWere = it
            }
        }

    fun tick(seconds: Double) {
        state = GameEngine.tick(state, seconds)
    }

    /**
     * Settles everything that reads the wall clock rather than elapsed play time: the lab, and
     * the two automation rules that act on it.
     */
    fun settleWallClock() {
        state = GameEngine.onWallClock(state, System.currentTimeMillis())
    }

    /**
     * Replaces the running game with another slot's, or with a fresh one where it is empty.
     *
     * Credits the time away for the game being picked up, for the same reason opening the app
     * does: a save that was put down a week ago has earned exactly as much as one that was not.
     */
    fun load(loaded: GameState?) {
        state = loaded ?: GameState.new(System.currentTimeMillis())
        Numbers.format = NumberFormat.byName(state.numberFormat)
        offlineReport = null
        if (loaded != null) creditTimeAway()
    }

    /** Credits the production earned while the game was closed, and shows the report. */
    fun creditTimeAway() {
        val report = GameEngine.applyOffline(state, System.currentTimeMillis())
        state = report.state
        if (report.worthShowing) offlineReport = report
    }

    override fun tap(): Double {
        val gained = GameEngine.tapValue(state)
        state = GameEngine.tap(state)
        return gained
    }

    override fun tapEmpty() {
        state = GameEngine.tapEmpty(state)
    }

    override fun setBuyAmount(amount: BuyAmount) {
        currentBuyAmount = amount
    }

    override fun buyCollector(id: String) {
        state = GameEngine.buyCollector(state, id, currentBuyAmount)
    }

    override fun cycleRole(id: String) {
        state = Roles.cycle(state, id)
    }

    override fun buyUpgrade(id: String) {
        state = GameEngine.buyUpgrade(state, id)
    }

    override fun buyFuser(id: String) {
        state = GameEngine.buyFuser(state, id, currentBuyAmount)
    }

    override fun openOrbit() {
        state = GameEngine.openOrbit(state)
    }

    override fun seedSatellite(orbitIndex: Int) {
        state = GameEngine.seedSatellite(state, orbitIndex)
    }

    override fun mergeSatellites(from: Int, to: Int) {
        state = GameEngine.mergeSatellites(state, from, to)
    }

    override fun startResearch(id: String) {
        state = GameEngine.startResearch(state, id, System.currentTimeMillis())
    }

    override fun cancelResearch() {
        state = GameEngine.cancelResearch(state)
    }

    override fun cycleAutomation(id: String) {
        state = GameEngine.cycleAutomation(state, id)
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

    override fun bigBang(pathId: String) {
        state = GameEngine.bigBang(state, NOW, pathId)
    }

    override fun buyAeonUpgrade(id: String) {
        state = GameEngine.buyAeonUpgrade(state, id)
    }

    override fun forgeAlloy(id: String) {
        state = GameEngine.forgeAlloy(state, id)
    }

    override fun assignGalaxy(slot: Int, jobId: String) {
        state = GameEngine.assignGalaxy(state, slot, jobId)
    }

    override fun mergeGalaxies(keepSlot: Int, absorbSlot: Int) {
        state = GameEngine.mergeGalaxies(state, keepSlot, absorbSlot)
    }

    override fun answerFind(answerId: String) {
        state = GameEngine.answerFind(state, answerId)
    }

    override fun buyPathNode(id: String) {
        state = GameEngine.buyPathNode(state, id)
    }

    override fun startChallenges(ids: Set<String>) {
        state = GameEngine.startChallenges(state, ids, NOW)
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

    override fun buyInvestment(id: String, amount: Int) {
        state = GameEngine.buyInvestment(state, id, amount)
    }

    override fun eraseSave() {
        state = GameState.new(NOW)
    }

    override fun setSound(on: Boolean) {
        state = GameEngine.setSound(state, on)
    }

    override fun setHaptics(on: Boolean) {
        state = GameEngine.setHaptics(state, on)
    }

    override fun setMusic(on: Boolean) {
        state = GameEngine.setMusic(state, on)
    }

    override fun setSkin(id: String) {
        state = GameEngine.setSkin(state, id)
    }

    override fun setAutoBuy(on: Boolean) {
        state = GameEngine.setAutoBuy(state, on)
    }

    override fun setReminders(on: Boolean) {
        state = GameEngine.setReminders(state, on)
    }

    override fun setStatus(on: Boolean) {
        state = GameEngine.setStatus(state, on)
    }

    override fun dismissTutorial() {
        state = GameEngine.dismissTutorial(state)
    }

    /**
     * Holds production still while the collapse plays out.
     *
     * Read by the frame loop in `Main.kt`, which keeps advancing its own clock while this is on —
     * so the seconds spent watching the screen fall in are dropped rather than paid out at the end.
     */
    override fun setPaused(on: Boolean) {
        paused = on
    }

    override fun setNumberFormat(format: NumberFormat) {
        state = GameEngine.setNumberFormat(state, format)
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
        // Through `byIndex`, which clamps and reaches the catalogue ladder. The raw list index
        // threw on `--tier 99` — and `--tier 400` is now a perfectly reasonable thing to want,
        // because there are sixteen thousand rungs to jump to.
        val tier = com.staatseigentum.kollaps.core.Tiers.byIndex(index)
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
