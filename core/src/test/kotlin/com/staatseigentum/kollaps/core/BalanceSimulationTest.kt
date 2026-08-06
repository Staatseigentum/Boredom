package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Plays the game with a simple bot so the pacing is measured instead of guessed. The bot is
 * deliberately naive — it never plans ahead — so the numbers it produces are an upper bound on
 * how long a human needs.
 */
class BalanceSimulationTest {

    private data class Run(
        val finalState: GameState,
        /** Seconds of play needed to reach each tier index. */
        val tierTimes: Map<Int, Long>,
        val seconds: Long,
    ) {
        val reachedBlackHole: Boolean get() = Tiers.last.index in tierTimes
    }

    /**
     * @param tapsPerSecond how hard the player taps for the first [activeSeconds]
     * @param limitSeconds give up after this much simulated time
     */
    private fun simulate(
        tapsPerSecond: Int,
        activeSeconds: Long,
        limitSeconds: Long = 30L * 24 * 3_600,
    ): Run {
        var state = GameState.new(nowMillis = 0)
        val tierTimes = mutableMapOf(0 to 0L)
        var elapsed = 0L

        while (elapsed < limitSeconds) {
            if (elapsed < activeSeconds) repeat(tapsPerSecond) { state = GameEngine.tap(state) }
            state = GameEngine.tick(state, 1.0)
            state = spend(state)
            elapsed++

            val tier = GameEngine.tierOf(state).index
            if (tier !in tierTimes) {
                tierTimes[tier] = elapsed
                if (tier == Tiers.last.index) break
            }
        }
        return Run(state, tierTimes, elapsed)
    }

    /** Buys every upgrade it can afford, then the collector with the fastest payback. */
    private fun spend(start: GameState): GameState {
        var state = start

        while (true) {
            val affordable = GameEngine.upgradeOffers(state).firstOrNull { it.affordable }
                ?: break
            state = GameEngine.buyUpgrade(state, affordable.upgrade.id)
        }

        while (true) {
            val best = GameEngine.collectorOffers(state, BuyAmount.ONE)
                .filter { it.visible && it.affordable }
                .minByOrNull { it.cost / it.collector.baseRate }
                ?: break
            val after = GameEngine.buyCollector(state, best.collector.id, BuyAmount.ONE)
            if (after == state) break
            state = after
        }
        return state
    }

    private fun report(label: String, run: Run) {
        println("--- $label ---")
        for (tier in Tiers.all) {
            val time = run.tierTimes[tier.index]
            val rendered = if (time == null) "nicht erreicht" else Numbers.formatDuration(time)
            println("  ${tier.index.toString().padStart(2)} ${tier.name.padEnd(18)} $rendered")
        }
        println("  Produktion am Ende: ${Numbers.formatRate(GameEngine.massPerSecond(run.finalState))}")
        println("  Kollektoren: ${run.finalState.collectors.values.sum()}")
        println("  Upgrades: ${run.finalState.upgrades.size} von ${Upgrades.all.size}")
    }

    @Test
    fun `an active player reaches the black hole within a few sessions`() {
        val run = simulate(tapsPerSecond = 4, activeSeconds = 20 * 60)
        report("aktiv: 4 Tipps/s für 20 Minuten, danach idle", run)

        assertTrue(run.reachedBlackHole, "Schwarzes Loch war in 30 Tagen nicht erreichbar")

        // Das Ziel ist eine Leiter, die aktiv gespielt rund vier Stunden trägt.
        val hours = run.tierTimes.getValue(Tiers.last.index) / 3_600.0
        assertTrue(hours > 3.5, "Endgame schon nach $hours Stunden — zu kurz")
        assertTrue(hours < 4.5, "Endgame erst nach $hours Stunden — zu zäh")
    }

    @Test
    fun `a pure idler still gets there`() {
        val run = simulate(tapsPerSecond = 1, activeSeconds = 60)
        report("idle: eine Minute antippen, danach nur warten", run)
        assertTrue(run.reachedBlackHole, "Reines Idlen führt nie zum Ende")
    }

    @Test
    fun `the early tiers come quickly enough to hook the player`() {
        val run = simulate(tapsPerSecond = 3, activeSeconds = 10 * 60, limitSeconds = 3_600)
        report("erste Stunde", run)

        assertTrue(run.tierTimes.getValue(1) < 120, "Der erste Aufstieg dauert zu lange")
        assertTrue(run.tierTimes.getValue(2) < 300, "Der zweite Aufstieg dauert zu lange")
        assertTrue(
            run.tierTimes.keys.max() >= 6,
            "In der ersten Stunde nur bis Stufe ${run.tierTimes.keys.max()}",
        )
    }

    @Test
    fun `collapsing makes the next run noticeably faster`() {
        val first = simulate(tapsPerSecond = 4, activeSeconds = 20 * 60)
        assertTrue(first.reachedBlackHole)

        val collapsed = GameEngine.collapse(first.finalState, nowMillis = 0)
        assertTrue(collapsed.singularities >= 12.0)

        // Replay the opening of a fresh run with and without the prestige bonus.
        val warmup = 5 * 60.0
        val plain = GameEngine.tick(seededRun(GameState.new(0)), warmup)
        val boosted = GameEngine.tick(seededRun(collapsed.copy(mass = 0.0)), warmup)

        assertTrue(
            boosted.runMass > plain.runMass * (1.0 + GameEngine.SINGULARITY_BONUS),
            "Kollaps bringt zu wenig: ${boosted.runMass} gegenüber ${plain.runMass}",
        )
    }

    /** Gives a state a small, identical starting position so two runs can be compared. */
    private fun seededRun(state: GameState): GameState =
        GameEngine.buyCollector(state.copy(mass = 10_000.0), "net", BuyAmount.TEN)
            .copy(mass = 0.0)

    @Test
    fun `production never becomes infinite or negative`() {
        val run = simulate(tapsPerSecond = 10, activeSeconds = 24 * 3_600, limitSeconds = 3 * 3_600)
        val perSecond = GameEngine.massPerSecond(run.finalState)
        assertTrue(perSecond.isFinite() && perSecond >= 0.0, "Produktion entgleist: $perSecond")
        assertTrue(run.finalState.mass >= 0.0, "Masse ist negativ: ${run.finalState.mass}")
    }
}
