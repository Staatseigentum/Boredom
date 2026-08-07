package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Milestones, the automatic tapper and challenges. */
class ChallengeAndAutomationTest {

    /** A player several collapses in, so every challenge is on offer. */
    private fun veteran(): GameState = GameState.new(NOW).copy(
        mass = 1e9,
        runMass = 1e9,
        totalMass = 1e12,
        collectors = mapOf("dust" to 60, "net" to 30),
        singularities = 60.0,
        collapses = 4,
    )

    // ------------------------------------------------------------------ milestones

    @Test
    fun `milestones count every full step and nothing in between`() {
        assertEquals(0, Milestones.reached(0))
        assertEquals(0, Milestones.reached(Milestones.STEP - 1))
        assertEquals(1, Milestones.reached(Milestones.STEP))
        assertEquals(4, Milestones.reached(Milestones.STEP * 4 + 3))

        assertEquals(1.0, Milestones.factor(0))
        assertEquals(Milestones.FACTOR, Milestones.factor(Milestones.STEP), 1e-9)
        assertEquals(
            Milestones.FACTOR * Milestones.FACTOR,
            Milestones.factor(Milestones.STEP * 2),
            1e-9,
        )
    }

    @Test
    fun `the next milestone is always ahead of what is owned`() {
        for (owned in 0..(Milestones.STEP * 3)) {
            val next = Milestones.nextAt(owned)!!
            assertTrue(next > owned, "Bei $owned Stück liegt der nächste Meilenstein auf $next")
        }
    }

    @Test
    fun `crossing a milestone raises what that collector produces`() {
        val below = GameState.new(NOW).copy(collectors = mapOf("dust" to Milestones.STEP - 1))
        val above = GameState.new(NOW).copy(collectors = mapOf("dust" to Milestones.STEP))

        val perUnitBelow = GameEngine.massPerSecond(below) / (Milestones.STEP - 1)
        val perUnitAbove = GameEngine.massPerSecond(above) / Milestones.STEP
        assertEquals(perUnitBelow * Milestones.FACTOR, perUnitAbove, perUnitAbove * 1e-9)
    }

    @Test
    fun `the shop reports the milestone a collector is working towards`() {
        val state = GameState.new(NOW).copy(collectors = mapOf("dust" to Milestones.STEP + 4))
        val offer = GameEngine.collectorOffers(state, BuyAmount.ONE).first { it.collector.id == "dust" }
        assertEquals(1, offer.milestones)
        assertEquals(Milestones.STEP * 2, offer.nextMilestoneAt)
    }

    // ------------------------------------------------------------------ automation

    @Test
    fun `without the upgrade nothing taps by itself`() {
        val state = veteran()
        val ticked = GameEngine.tick(state, 10.0)
        assertEquals(state.taps, ticked.taps)
    }

    @Test
    fun `the automatic tapper adds taps and the mass that goes with them`() {
        val state = GameEngine.buyPrestigeUpgrade(veteran(), "p_auto_1")
        val perTap = GameEngine.massPerTap(state)
        val perSecond = GameEngine.massPerSecond(state)

        val ticked = GameEngine.tick(state, 10.0)

        assertEquals(state.taps + 30, ticked.taps)
        assertEquals(
            state.mass + (perSecond + perTap * 3.0) * 10.0,
            ticked.mass,
            ticked.mass * 1e-9,
        )
    }

    @Test
    fun `fractions of a tap are carried instead of rounded away`() {
        // The app ticks many times a second, so at three taps a second every single tick is a
        // fraction of one. Truncating each would count no automatic taps at all.
        var state = GameEngine.buyPrestigeUpgrade(veteran(), "p_auto_1")
        val before = state.taps
        repeat(100) { state = GameEngine.tick(state, 0.1) }
        assertEquals(before + 30, state.taps)
    }

    @Test
    fun `the faster tapper replaces the slower one instead of adding to it`() {
        var state = GameEngine.buyPrestigeUpgrade(veteran(), "p_auto_1")
        state = GameEngine.buyPrestigeUpgrade(state, "p_auto_2")
        assertEquals(10.0, GameEngine.stats(state).autoTapsPerSecond)
    }

    // ------------------------------------------------------------------ challenges

    @Test
    fun `a challenge needs the collapses behind it`() {
        val fresh = GameState.new(NOW)
        assertTrue(Challenge.offered(fresh).isEmpty())
        assertEquals(fresh, GameEngine.startChallenge(fresh, "c_hand", NOW))

        assertEquals(Challenge.entries.size, Challenge.offered(veteran()).size)
    }

    @Test
    fun `starting a challenge wipes the run and withholds the head start`() {
        val state = GameEngine.buyPrestigeUpgrade(veteran(), "p_start_mass")
        val started = GameEngine.startChallenge(state, "c_hand", NOW)

        assertEquals("c_hand", started.activeChallenge)
        assertEquals(0.0, started.runMass)
        assertEquals(0.0, started.mass, "Die Rücklage darf in einer Herausforderung nicht greifen")
        assertTrue(started.collectors.isEmpty())
        // Everything permanent survives.
        assertEquals(state.singularities, started.singularities)
        assertEquals(state.collapses, started.collapses)
        assertEquals(state.prestigeUpgrades, started.prestigeUpgrades)
    }

    @Test
    fun `a second challenge cannot start while one is running`() {
        val started = GameEngine.startChallenge(veteran(), "c_hand", NOW)
        assertEquals(started, GameEngine.startChallenge(started, "c_idle", NOW))
    }

    @Test
    fun `the no collector rule switches production off and leaves tapping alone`() {
        val started = GameEngine.startChallenge(veteran(), "c_hand", NOW)
            .copy(collectors = mapOf("dust" to 100))

        assertEquals(0.0, GameEngine.massPerSecond(started))
        assertTrue(GameEngine.massPerTap(started) > 0.0)
        assertTrue(GameEngine.tap(started).runMass > 0.0)
    }

    @Test
    fun `the no tap rule switches tapping off and leaves production alone`() {
        val started = GameEngine.startChallenge(veteran(), "c_idle", NOW)
            .copy(collectors = mapOf("dust" to 100))

        assertEquals(0.0, GameEngine.massPerTap(started))
        assertTrue(GameEngine.massPerSecond(started) > 0.0)
        assertEquals(0.0, GameEngine.tap(started).runMass)
    }

    @Test
    fun `the handicap rule scales everything down`() {
        // Both at the same point of the ladder: starting a challenge resets the run, and the
        // tier multiplier would otherwise swamp the handicap being measured.
        val plain = veteran().copy(collectors = mapOf("dust" to 100), runMass = 0.0)
        val handicapped = GameEngine.startChallenge(veteran(), "c_half", NOW)
            .copy(collectors = mapOf("dust" to 100))

        val factor = (Challenge.HALBE_KRAFT.rule as ChallengeRule.Handicap).factor
        assertEquals(
            GameEngine.massPerSecond(plain) * factor,
            GameEngine.massPerSecond(handicapped),
            GameEngine.massPerSecond(plain) * 1e-9,
        )
    }

    @Test
    fun `collapsing is off the table until the challenge is over`() {
        val done = veteran().copy(runMass = Tiers.last.threshold * 2)
        assertTrue(GameEngine.canCollapse(done))

        val inChallenge = done.copy(activeChallenge = "c_hand")
        assertFalse(GameEngine.canCollapse(inChallenge))
        assertEquals(inChallenge, GameEngine.collapse(inChallenge, NOW))
    }

    @Test
    fun `finishing a challenge records the reward and hands the run back`() {
        val started = GameEngine.startChallenge(veteran(), "c_hand", NOW)
        val met = started.copy(runMass = Tiers.byName("Erde").threshold)

        assertTrue(Challenge.isMet(met))
        val finished = GameEngine.finishChallenge(met, NOW)

        assertNull(finished.activeChallenge)
        assertTrue("c_hand" in finished.challengesDone)
        assertEquals(0.0, finished.runMass)
        assertTrue(Challenge.offered(finished).none { it.id == "c_hand" })
    }

    @Test
    fun `an unmet challenge cannot be handed in`() {
        val started = GameEngine.startChallenge(veteran(), "c_hand", NOW)
        assertFalse(Challenge.isMet(started))
        assertEquals(started, GameEngine.finishChallenge(started, NOW))
    }

    @Test
    fun `the reward keeps working after the challenge is over`() {
        val plain = veteran()
        val rewarded = plain.copy(challengesDone = setOf("c_hand"))
        val factor = (Challenge.HANDARBEIT.reward as PrestigeEffect.TapMultiplier).factor

        assertEquals(
            GameEngine.massPerTap(plain) * factor,
            GameEngine.massPerTap(rewarded),
            GameEngine.massPerTap(rewarded) * 1e-9,
        )
    }

    @Test
    fun `giving up resets the run without recording anything`() {
        val started = GameEngine.startChallenge(veteran(), "c_hand", NOW)
        val aborted = GameEngine.abortChallenge(started.copy(runMass = 5_000.0), NOW)

        assertNull(aborted.activeChallenge)
        assertTrue(aborted.challengesDone.isEmpty())
        assertEquals(0.0, aborted.runMass)
    }

    @Test
    fun `the timed challenge runs on play time and can be lost`() {
        var state = GameEngine.startChallenge(veteran(), "c_sprint", NOW)
        val limit = (Challenge.SPRINT.goal as ChallengeGoal.ReachTierWithin).seconds

        state = GameEngine.tick(state, 60.0)
        assertEquals(60.0, state.challengeSeconds, 1e-9)
        assertFalse(Challenge.isLost(state))

        state = GameEngine.tick(state, limit)
        assertTrue(Challenge.isLost(state), "Die Uhr ist abgelaufen, die Stufe nicht erreicht")
        assertFalse(Challenge.isMet(state))

        // Reaching the goal after the clock ran out does not count.
        val late = state.copy(runMass = Tiers.byName("Saturn").threshold)
        assertFalse(Challenge.isMet(late))
    }

    @Test
    fun `a challenge clock does not run when no challenge does`() {
        val ticked = GameEngine.tick(veteran(), 30.0)
        assertEquals(0.0, ticked.challengeSeconds)
    }

    @Test
    fun `challenge progress survives a save`() {
        val started = GameEngine.startChallenge(veteran(), "c_sprint", NOW)
        val ticked = GameEngine.tick(started, 120.0)
        val restored = SaveCodec.decode(SaveCodec.encode(ticked))!!

        assertEquals("c_sprint", restored.activeChallenge)
        assertEquals(ticked.challengeSeconds, restored.challengeSeconds, 1e-9)
    }

    @Test
    fun `a challenge that no longer exists is dropped instead of trapping the run`() {
        val raw = SaveCodec.encode(veteran().copy(activeChallenge = "c_gibtsnicht"))
        val decoded = SaveCodec.decode(raw)!!
        assertNull(decoded.activeChallenge)
    }

    @Test
    fun `every challenge names a body that is actually on the ladder`() {
        for (challenge in Challenge.entries) {
            val name = when (val goal = challenge.goal) {
                is ChallengeGoal.ReachTier -> goal.tierName
                is ChallengeGoal.ReachTierWithin -> goal.tierName
            }
            assertNotEquals(0, Tiers.indexOf(name), "${challenge.title} zielt auf $name")
            assertTrue(challenge.goalText.isNotBlank())
            assertTrue(challenge.ruleText.isNotBlank())
        }
    }

    // ------------------------------------------------------------------ offline report

    @Test
    fun `the offline report says what was credited and what the cap ate`() {
        val state = veteran().copy(
            collectors = mapOf("dust" to 100, "net" to 50),
            lastSeenAt = NOW,
        )
        val capHours = GameEngine.stats(state).offlineCapSeconds / 3_600.0
        val awaySeconds = (capHours * 3_600.0 * 3).toLong()

        val report = GameEngine.applyOffline(state, NOW + awaySeconds * 1_000)

        assertEquals(awaySeconds, report.awaySeconds)
        assertTrue(report.seconds < report.awaySeconds, "Die Grenze hat nicht gegriffen")
        assertTrue(report.cappedOut)
        // Twice the credited span was lost, at the same rate as what was credited.
        assertEquals(report.gained * 2.0, report.lostToCap, report.gained * 1e-6)
        assertEquals(GameEngine.stats(state).offlineEfficiency, report.efficiency)
    }

    @Test
    fun `the offline report breaks the haul down by collector`() {
        val state = veteran().copy(
            collectors = mapOf("dust" to 100, "net" to 50, "drone" to 10),
            lastSeenAt = NOW,
        )
        val report = GameEngine.applyOffline(state, NOW + 600_000)

        assertEquals(3, report.shares.size)
        assertEquals(
            report.gained,
            report.shares.sumOf { report.gained * it.share },
            report.gained * 1e-4,
        )
        assertTrue(report.shares.zipWithNext().all { (a, b) -> a.output >= b.output })
    }

    @Test
    fun `an absence inside the cap loses nothing`() {
        val state = veteran().copy(collectors = mapOf("dust" to 10), lastSeenAt = NOW)
        val report = GameEngine.applyOffline(state, NOW + 60_000)

        assertFalse(report.cappedOut)
        assertEquals(0.0, report.lostToCap)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
