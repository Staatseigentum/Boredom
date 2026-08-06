package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineTest {

    private fun fresh() = GameState.new(nowMillis = 1_000_000)

    private fun rich(mass: Double) = fresh().copy(mass = mass, runMass = mass, totalMass = mass)

    /** Rich, but still on the first tier, so no tier multiplier distorts the arithmetic. */
    private fun wealthy(mass: Double) = fresh().copy(mass = mass, totalMass = mass)

    @Test
    fun `a fresh save produces nothing and taps for the base amount`() {
        val state = fresh()
        assertEquals(0.0, GameEngine.massPerSecond(state))
        assertEquals(GameEngine.BASE_TAP, GameEngine.massPerTap(state))
        assertEquals(Tiers.first, GameEngine.tierOf(state))
    }

    @Test
    fun `tapping credits mass and counts the tap`() {
        val after = GameEngine.tap(fresh())
        assertEquals(1L, after.taps)
        assertEquals(GameEngine.BASE_TAP, after.mass)
        assertEquals(GameEngine.BASE_TAP, after.runMass)
        assertEquals(GameEngine.BASE_TAP, after.totalMass)
    }

    @Test
    fun `buying a collector spends mass and adds production`() {
        val state = GameEngine.buyCollector(rich(100.0), "dust", BuyAmount.ONE)
        assertEquals(1, state.ownedOf("dust"))
        assertEquals(85.0, state.mass, 1e-9)
        assertEquals(0.1, GameEngine.massPerSecond(state), 1e-9)
    }

    @Test
    fun `buying is refused when the mass is missing`() {
        val poor = rich(5.0)
        assertEquals(poor, GameEngine.buyCollector(poor, "dust", BuyAmount.ONE))
        assertEquals(poor, GameEngine.buyCollector(poor, "gibtsnicht", BuyAmount.ONE))
    }

    @Test
    fun `bulk price matches buying one at a time`() {
        val start = rich(1e9)
        val bulk = GameEngine.buyCollector(start, "dust", BuyAmount.TEN)
        var oneByOne = start
        repeat(10) { oneByOne = GameEngine.buyCollector(oneByOne, "dust", BuyAmount.ONE) }

        assertEquals(10, bulk.ownedOf("dust"))
        assertEquals(oneByOne.ownedOf("dust"), bulk.ownedOf("dust"))
        assertEquals(oneByOne.mass, bulk.mass, 1e-6)
    }

    @Test
    fun `max buy never costs more than the player owns`() {
        val start = rich(50_000.0)
        val after = GameEngine.buyCollector(start, "dust", BuyAmount.MAX)
        assertTrue(after.ownedOf("dust") > 0)
        assertTrue(after.mass >= 0.0, "Max-Kauf hat ins Minus gekauft: ${after.mass}")
        // One more would have been unaffordable.
        val collector = Collectors.require("dust")
        assertTrue(collector.costAt(after.ownedOf("dust")) > after.mass)
    }

    @Test
    fun `ticking credits production over time`() {
        val state = GameEngine.buyCollector(wealthy(1_000.0), "net", BuyAmount.ONE)
        val before = state.mass
        val after = GameEngine.tick(state, 10.0)
        assertEquals(before + 10.0, after.mass, 1e-9)
    }

    @Test
    fun `upgrades apply their effect and are only bought once`() {
        var state = rich(10_000.0).copy(runMass = 10_000.0)
        val perTapBefore = GameEngine.massPerTap(state)

        state = GameEngine.buyUpgrade(state, "tap_1")
        assertTrue(state.owns("tap_1"))
        assertEquals(perTapBefore * 2.0, GameEngine.massPerTap(state), 1e-9)

        val massAfterFirst = state.mass
        state = GameEngine.buyUpgrade(state, "tap_1")
        assertEquals(massAfterFirst, state.mass, "Upgrade wurde doppelt bezahlt")
    }

    @Test
    fun `locked upgrades cannot be bought`() {
        val state = rich(1e15).copy(runMass = 0.0)
        val locked = Upgrades.all.first { it.unlock is UnlockCondition.TierReached }
        assertFalse(GameEngine.isUnlocked(state, locked))
        assertEquals(state, GameEngine.buyUpgrade(state, locked.id))
    }

    @Test
    fun `collector upgrades only multiply their own collector`() {
        var state = wealthy(1e9)
        state = GameEngine.buyCollector(state, "dust", BuyAmount.TEN)
        state = GameEngine.buyCollector(state, "net", BuyAmount.ONE)
        val before = GameEngine.massPerSecond(state)

        state = GameEngine.buyUpgrade(state, "dust_10")
        assertTrue(state.owns("dust_10"))
        // dust contributes 10 × 0.1 = 1.0, doubling it adds exactly 1.0 kg/s.
        assertEquals(before + 1.0, GameEngine.massPerSecond(state), 1e-9)
    }

    @Test
    fun `the tier climbs with the mass collected in this run`() {
        val state = fresh().copy(runMass = Tiers.all[4].threshold)
        val stats = GameEngine.stats(state)
        assertEquals(Tiers.all[4], stats.tier)
        assertEquals(Tiers.all[5], stats.nextTier)
        assertEquals(0f, stats.tierProgress)

        val half = Tiers.all[4].threshold + (Tiers.all[5].threshold - Tiers.all[4].threshold) / 2
        assertEquals(0.5f, GameEngine.stats(state.copy(runMass = half)).tierProgress, 0.001f)
    }

    @Test
    fun `reaching a tier multiplies production`() {
        val base = GameEngine.buyCollector(rich(1e6), "net", BuyAmount.ONE)
        val atMeteorite = GameEngine.massPerSecond(base.copy(runMass = 0.0))
        val atEarth = GameEngine.massPerSecond(base.copy(runMass = Tiers.all[6].threshold))
        assertEquals(atMeteorite * Tiers.all[6].productionMultiplier, atEarth, 1e-6)
    }

    @Test
    fun `tap synergy adds a share of production`() {
        var state = rich(1e9)
        repeat(2) { state = GameEngine.buyCollector(state, "drone", BuyAmount.TEN) }
        val perSecond = GameEngine.massPerSecond(state)
        val perTapBefore = GameEngine.massPerTap(state)

        state = GameEngine.buyUpgrade(state, "synergy_1")
        assertTrue(state.owns("synergy_1"))
        assertEquals(perTapBefore + perSecond * 0.01, GameEngine.massPerTap(state), 1e-6)
    }

    @Test
    fun `offline production is capped and scaled by efficiency`() {
        val state = GameEngine.buyCollector(rich(1e6), "net", BuyAmount.TEN)
        val perSecond = GameEngine.massPerSecond(state)

        val oneHour = GameEngine.applyOffline(state, state.lastSeenAt + 3_600_000)
        assertEquals(3_600L, oneHour.seconds)
        assertEquals(perSecond * 3_600 * GameEngine.BASE_OFFLINE_EFFICIENCY, oneHour.gained, 1e-6)

        val threeDays = GameEngine.applyOffline(state, state.lastSeenAt + 259_200_000)
        assertEquals((GameEngine.BASE_OFFLINE_CAP_HOURS * 3_600).toLong(), threeDays.seconds)
    }

    @Test
    fun `offline upgrades raise efficiency and cap`() {
        val state = GameEngine.buyCollector(rich(1e6), "net", BuyAmount.TEN)
            .copy(upgrades = setOf("offline_1", "offline_2"))
        val report = GameEngine.applyOffline(state, state.lastSeenAt + 259_200_000)
        assertEquals(24L * 3_600, report.seconds)
        assertEquals(GameEngine.massPerSecond(state) * 24 * 3_600, report.gained, 1e-3)
    }

    @Test
    fun `a clock jumping backwards credits nothing`() {
        val state = GameEngine.buyCollector(rich(1e6), "net", BuyAmount.TEN)
        val report = GameEngine.applyOffline(state, state.lastSeenAt - 500_000)
        assertEquals(0L, report.seconds)
        assertEquals(0.0, report.gained)
        assertEquals(state.mass, report.state.mass)
    }

    @Test
    fun `a first launch credits nothing but records the time`() {
        val report = GameEngine.applyOffline(GameState(), 5_000)
        assertEquals(0.0, report.gained)
        assertEquals(5_000L, report.state.lastSeenAt)
    }

    @Test
    fun `collapsing is refused before the black hole`() {
        val state = fresh().copy(runMass = Tiers.last.threshold - 1)
        assertFalse(GameEngine.canCollapse(state))
        assertEquals(state, GameEngine.collapse(state, 2_000_000))
    }

    @Test
    fun `collapsing resets the run but keeps singularities and records`() {
        val state = GameEngine.buyCollector(
            fresh().copy(mass = 1e16, runMass = Tiers.last.threshold * 4, totalMass = 1e16),
            "net",
            BuyAmount.TEN,
        ).copy(upgrades = setOf("tap_1"), taps = 500)

        assertTrue(GameEngine.canCollapse(state))
        val expected = GameEngine.pendingSingularities(state)
        assertEquals(24.0, expected, "sqrt(4) × 12 erwartet")

        val after = GameEngine.collapse(state, 2_000_000)
        assertEquals(expected, after.singularities)
        assertEquals(1, after.collapses)
        assertEquals(0.0, after.mass)
        assertEquals(0.0, after.runMass)
        assertTrue(after.collectors.isEmpty())
        assertTrue(after.upgrades.isEmpty())
        assertEquals(500L, after.taps, "Tipps sind ein Gesamtrekord")
        assertEquals(state.totalMass, after.totalMass)
        assertEquals(Tiers.last.index, after.bestTier)
        assertEquals(state.runMass, after.bestRunMass)
    }

    @Test
    fun `singularities speed up the next run`() {
        val plain = GameEngine.buyCollector(rich(1e6), "net", BuyAmount.TEN)
        val boosted = plain.copy(singularities = 10.0)
        assertEquals(
            GameEngine.massPerSecond(plain) * 2.0,
            GameEngine.massPerSecond(boosted),
            1e-9,
        )
    }

    @Test
    fun `the shop reveals collectors as the player gets richer`() {
        val start = GameEngine.collectorOffers(fresh(), BuyAmount.ONE)
        assertTrue(start.first().visible, "Der erste Kollektor muss sofort sichtbar sein")
        assertEquals(1, start.count { it.visible })

        val later = GameEngine.collectorOffers(rich(1e7), BuyAmount.ONE)
        assertTrue(later.count { it.visible } > 4)
    }

    @Test
    fun `the shop hides owned upgrades and shows unlocked ones cheapest first`() {
        val state = rich(1e7)
        val offers = GameEngine.upgradeOffers(state)
        assertTrue(offers.isNotEmpty())
        assertEquals(offers.map { it.upgrade.cost }.sorted(), offers.map { it.upgrade.cost })

        val bought = GameEngine.buyUpgrade(state, offers.first().upgrade.id)
        assertFalse(GameEngine.upgradeOffers(bought).any { it.upgrade.id == offers.first().upgrade.id })
    }

    @Test
    fun `tier celebration fires once per tier`() {
        var state = fresh().copy(runMass = Tiers.all[3].threshold)
        assertTrue(GameEngine.hasUncelebratedTier(state))
        state = GameEngine.acknowledgeTier(state)
        assertFalse(GameEngine.hasUncelebratedTier(state))

        state = state.copy(runMass = Tiers.all[4].threshold)
        assertTrue(GameEngine.hasUncelebratedTier(state))
    }
}
