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
            // The bot plays without ever putting the phone down, so one second of play is one
            // second on the wall clock. That is the worst case for the lab — a real player gets
            // more research per hour of play by leaving a long project running overnight.
            state = GameEngine.settleResearch(state, elapsed * 1_000L)
            state = spend(state, elapsed * 1_000L)
            elapsed++

            val tier = GameEngine.tierOf(state).index
            if (tier !in tierTimes) {
                tierTimes[tier] = elapsed
                if (tier == Tiers.last.index) break
            }
        }
        return Run(state, tierTimes, elapsed)
    }

    /**
     * How much more than the price the bot wants in hand before it improves a furnace.
     *
     * Fusion competes with collectors for the same mass, and a bot that spent every kilogram on
     * the chain the moment it unlocked would stall its own production to buy a multiplier of it.
     * The reserve makes it build out of surplus, which is what a player does.
     */
    private val FUSION_RESERVE = 5.0

    /** The same idea for the lab, where the mass is gone the moment a project starts. */
    private val RESEARCH_RESERVE = 3.0

    /** And for the orbits, which pay back slowest of all — a body has to grow before it counts. */
    private val ORBIT_RESERVE = 6.0

    /** Buys every upgrade it can afford, then the collector with the fastest payback. */
    private fun spend(start: GameState, nowMillis: Long): GameState {
        var state = start

        // The bench first, and the cheapest thing on it: an idle lab earns nothing, and the
        // reserve keeps a long project from eating the mass the fleet needs.
        if (state.activeResearch == null) {
            ResearchTree.offered(state)
                .filter { !ResearchTree.isDone(state, it) && it.cost * RESEARCH_RESERVE <= state.mass }
                .minByOrNull { it.cost }
                ?.let { state = GameEngine.startResearch(state, it.id, nowMillis) }
        }

        while (true) {
            val affordable = GameEngine.upgradeOffers(state).firstOrNull { it.affordable }
                ?: break
            state = GameEngine.buyUpgrade(state, affordable.upgrade.id)
        }

        // The fleet's arrangement, redone whenever a slot opens up. A naive player sets the
        // biggest earners to quality and leaves it at that, which is what this does.
        if (Roles.isUnlocked(state) && Roles.hasFreeSlot(state)) {
            val best = GameEngine.collectorOffers(state, BuyAmount.ONE)
                .filter { it.owned > 0 && Roles.roleOf(state, it.collector.id) == null }
                .maxByOrNull { it.output }
            if (best != null) state = Roles.set(state, best.collector.id, Role.GUETE)
        }

        // A slot, then a body on it. Both out of surplus, and inner slots first, because the bot
        // has no way to reason about a body that pays off over the next hour.
        while (true) {
            val slot = Orbits.next(state) ?: break
            if (slot.cost * ORBIT_RESERVE > state.mass) break
            val after = GameEngine.openOrbit(state)
            if (after == state) break
            state = after
        }
        for (orbit in Orbits.opened(state)) {
            if (Orbits.isOccupied(state, orbit)) continue
            if (orbit.seedCost * ORBIT_RESERVE > state.mass) continue
            state = GameEngine.seedSatellite(state, orbit.index)
        }

        // Fusion before collectors: a furnace multiplies what the fleet already makes, so the
        // same mass is worth more here — as long as the reserve keeps the fleet growing too.
        while (true) {
            val next = GameEngine.fusionOffers(state, BuyAmount.ONE)
                .filter { it.amount > 0 && it.cost * FUSION_RESERVE <= state.mass }
                .minByOrNull { it.cost }
                ?: break
            val after = GameEngine.buyFuser(state, next.stage.id, BuyAmount.ONE)
            if (after == state) break
            state = after
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
        // The one number the bands below are about, said once and plainly. It used to be findable
        // only by reading the right row out of a twenty-five row table — which is how it got
        // misread for the *other* run's figure, twice, and wrong conclusions drawn from it both
        // times. A report that has to be interpreted will be.
        val end = run.tierTimes[Tiers.last.index]
        println(
            "  ZEIT BIS SCHWARZES LOCH: " +
                if (end == null) "nicht erreicht" else "%.2f h".format(end / 3_600.0),
        )
        println("  Produktion am Ende: ${Numbers.formatRate(GameEngine.massPerSecond(run.finalState))}")
        println("  Kollektoren: ${run.finalState.collectors.values.sum()}")
        println("  Upgrades: ${run.finalState.upgrades.size} von ${Upgrades.all.size}")
        println("  Fusionsstufen: ${run.finalState.fusers.values.sum()}")
        println("  Rollen: ${Roles.assignedCount(run.finalState)} von ${Roles.slots(run.finalState)}")
        println(
            "  Bahnen: ${run.finalState.orbits}, davon belegt " +
                "${Orbits.occupiedCount(run.finalState)} " +
                "(${Numbers.formatMultiplier(Orbits.multiplier(run.finalState))})",
        )
        println(
            "  Forschung: ${run.finalState.research.size} von ${ResearchTree.all.size}" +
                " (${run.finalState.research.sorted().joinToString(", ")})",
        )
        for (element in Element.entries) {
            val held = Fusion.amountOf(run.finalState, element)
            if (held >= 1.0) {
                println(
                    "    ${element.symbol.padEnd(3)} ${Numbers.format(held).padStart(10)}" +
                        "  ${Numbers.formatMultiplier(Fusion.factor(run.finalState, element))}",
                )
            }
        }
    }

    @Test
    fun `an active player reaches the black hole within a few sessions`() {
        val run = simulate(tapsPerSecond = 4, activeSeconds = 20 * 60)
        report("aktiv: 4 Tipps/s für 20 Minuten, danach idle", run)

        assertTrue(run.reachedBlackHole, "Schwarzes Loch war in 30 Tagen nicht erreichbar")

        // Das Ziel ist eine Leiter, die aktiv gespielt rund vier Stunden trägt.
        val hours = run.tierTimes.getValue(Tiers.last.index) / 3_600.0
        /*
         * Measured, not guessed, and the measurement is the interesting part.
         *
         * Without [Heat] this bot finishes in 3.53 hours — one per cent above the 3.5 the bound
         * used to demand. There was never any headroom here; the number simply happened to land
         * just inside it. Heat costs an actively played run another two per cent, which is the
         * whole point of the feature: being present is supposed to be worth something.
         *
         * So the floor moves rather than the feature shrinking to fit it. What the band is
         * actually protecting is the pacing target of about four hours, and 3.45 is still that.
         *
         * Worth knowing separately: at 3.53 the run was already sitting at the bottom of the
         * intended band before any of this, which is drift worth looking at on its own terms.
         */
        assertTrue(hours > 3.4, "Endgame schon nach $hours Stunden — zu kurz")
        assertTrue(hours < 4.5, "Endgame erst nach $hours Stunden — zu zäh")
    }

    @Test
    fun `a pure idler still gets there, and not much later`() {
        val run = simulate(tapsPerSecond = 1, activeSeconds = 60)
        report("idle: eine Minute antippen, danach nur warten", run)
        assertTrue(run.reachedBlackHole, "Reines Idlen führt nie zum Ende")

        /*
         * A band, not just "arrives".
         *
         * This test used to assert only that the idler gets there at all, and printed its time.
         * The active run next to it has had a band all along — so the report showed two figures
         * of which exactly one was guarded, and the unguarded one is the one that got read as the
         * guarded one. Twice, in one session, leading to two wrong diagnoses.
         *
         * Both are bounded now. The floor matters as much as the ceiling: an idler who arrives as
         * fast as somebody playing means the tapping is decoration.
         */
        val hours = run.tierTimes.getValue(Tiers.last.index) / 3_600.0
        assertTrue(hours > 3.6, "Idlen ist nach $hours Stunden durch — zu schnell für gar nichts")
        assertTrue(hours < 5.5, "Idlen braucht $hours Stunden — als Nebenbeispiel zu zäh")
    }

    @Test
    fun `playing actively is worth doing`() {
        // The whole premise of a tap in an idle game: it has to buy time, and a measurable amount
        // of it. Neither run's own band says this — 3.45 and 3.85 both sit inside both bands — so
        // the two could drift together without anything going red. This is the relationship.
        val idle = simulate(tapsPerSecond = 1, activeSeconds = 60)
        val active = simulate(tapsPerSecond = 4, activeSeconds = 20 * 60)

        val idleHours = idle.tierTimes.getValue(Tiers.last.index) / 3_600.0
        val activeHours = active.tierTimes.getValue(Tiers.last.index) / 3_600.0
        assertTrue(
            activeHours < idleHours * 0.95,
            "Zwanzig Minuten Tippen sparen fast nichts: aktiv $activeHours h, idle $idleHours h",
        )
    }

    @Test
    fun `the early tiers come quickly enough to hook the player`() {
        val run = simulate(tapsPerSecond = 3, activeSeconds = 10 * 60, limitSeconds = 3_600)
        report("erste Stunde", run)

        assertTrue(run.tierTimes.getValue(1) < 120, "Der erste Aufstieg dauert zu lange")
        assertTrue(run.tierTimes.getValue(2) < 300, "Der zweite Aufstieg dauert zu lange")
        // By name, not by number: the ladder gained seven rungs in the middle, and an index here
        // would have quietly started asking for a nearer body than it used to.
        val earth = Tiers.indexOf("Erde")
        assertTrue(
            run.tierTimes.keys.max() >= earth,
            "In der ersten Stunde nur bis ${Tiers.byIndex(run.tierTimes.keys.max()).name}",
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
