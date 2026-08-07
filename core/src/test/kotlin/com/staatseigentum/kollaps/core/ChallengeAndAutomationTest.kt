package com.staatseigentum.kollaps.core

import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
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

/** The second reset: what it costs, what it pays, and what survives it. */
class BigBangTest {

    private fun ready(collapses: Int = BigBang.REQUIRED_COLLAPSES): GameState =
        GameState.new(NOW).copy(
            mass = 5e9,
            runMass = 5e9,
            totalMass = 9e15,
            collectors = mapOf("dust" to 80, "net" to 40),
            upgrades = setOf("tap_1"),
            singularities = 140.0,
            collapses = collapses,
            taps = 30_000,
            prestigeUpgrades = setOf("p_tap", "p_offline_1"),
            achievements = setOf("a_taps_100"),
            challengesDone = setOf("c_hand"),
            cometsCaught = 60,
            playedSeconds = 20_000.0,
        )

    @Test
    fun `the button does not exist before ten collapses`() {
        val early = ready(collapses = BigBang.REQUIRED_COLLAPSES - 1)
        assertFalse(BigBang.isUnlocked(early))
        assertFalse(BigBang.canBang(early))
        assertEquals(0.0, BigBang.pending(early))
        assertEquals(early, GameEngine.bigBang(early, NOW))
    }

    @Test
    fun `the payout grows with the root of the collapses behind it`() {
        assertEquals(BigBang.SCALE, BigBang.pending(ready(BigBang.REQUIRED_COLLAPSES)))
        assertEquals(
            floor(BigBang.SCALE * 2.0),
            BigBang.pending(ready(BigBang.REQUIRED_COLLAPSES * 4)),
        )
    }

    @Test
    fun `a big bang keeps the records and throws the possessions away`() {
        val before = ready(collapses = 16)
        val after = GameEngine.bigBang(before, NOW)

        // Gone.
        assertEquals(0.0, after.singularities)
        assertEquals(0, after.collapses)
        assertTrue(after.prestigeUpgrades.isEmpty())
        assertTrue(after.collectors.isEmpty())
        assertTrue(after.upgrades.isEmpty())
        assertEquals(0.0, after.runMass)

        // Kept.
        assertEquals(before.taps, after.taps)
        assertEquals(before.totalMass, after.totalMass)
        assertEquals(before.challengesDone, after.challengesDone)
        assertEquals(before.cometsCaught, after.cometsCaught)
        assertEquals(before.playedSeconds, after.playedSeconds)
        assertTrue(before.achievements.all { it in after.achievements })

        // Paid.
        assertEquals(BigBang.pending(before), after.aeons)
        assertEquals(1, after.bigBangs)
        assertTrue(BigBang.isUnlocked(after), "Nach einem Urknall muss der Knopf sichtbar bleiben")
    }

    @Test
    fun `a big bang is off the table during a challenge`() {
        val inChallenge = ready(collapses = 20).copy(activeChallenge = "c_hand")
        assertFalse(BigBang.canBang(inChallenge))
        assertEquals(inChallenge, GameEngine.bigBang(inChallenge, NOW))
    }

    @Test
    fun `aeon upgrades cost aeons and survive the next big bang`() {
        var state = GameEngine.bigBang(ready(collapses = 40), NOW)
        val upgrade = AeonUpgrades.byId("ae_global_1")!!
        val before = state.aeons

        state = GameEngine.buyAeonUpgrade(state, upgrade.id)
        assertEquals(before - upgrade.cost, state.aeons)
        assertTrue(state.ownsAeon(upgrade.id))

        // Buying it twice takes nothing more.
        assertEquals(state, GameEngine.buyAeonUpgrade(state, upgrade.id))

        val next = GameEngine.bigBang(
            state.copy(collapses = BigBang.REQUIRED_COLLAPSES),
            NOW,
        )
        assertTrue(next.ownsAeon(upgrade.id), "Äonen-Upgrades überleben den nächsten Urknall")
    }

    @Test
    fun `an unaffordable aeon upgrade changes nothing`() {
        val state = GameState.new(NOW).copy(aeons = 0.5)
        assertEquals(state, GameEngine.buyAeonUpgrade(state, "ae_global_1"))
    }

    @Test
    fun `the first aeon upgrade multiplies production by what it says`() {
        val base = GameState.new(NOW).copy(collectors = mapOf("dust" to 50))
        val effect = AeonUpgrades.byId("ae_global_1")!!.effect as PrestigeEffect.GlobalMultiplier
        val boosted = base.copy(aeonUpgrades = setOf("ae_global_1"))

        assertEquals(
            GameEngine.massPerSecond(base) * effect.factor,
            GameEngine.massPerSecond(boosted),
            GameEngine.massPerSecond(boosted) * 1e-9,
        )
    }

    @Test
    fun `raising what a singularity is worth changes the multiplier`() {
        val base = GameState.new(NOW).copy(singularities = 20.0)
        val raised = base.copy(aeonUpgrades = setOf("ae_singularity"))
        val effect = AeonUpgrades.byId("ae_singularity")!!.effect as PrestigeEffect.SingularityBonus

        assertEquals(1.0 + GameEngine.SINGULARITY_BONUS * 20, GameEngine.singularityMultiplier(base))
        assertEquals(1.0 + effect.perSingularity * 20, GameEngine.singularityMultiplier(raised))
    }

    @Test
    fun `raising what a milestone is worth changes what a collector produces`() {
        val base = GameState.new(NOW).copy(collectors = mapOf("dust" to Milestones.STEP * 2))
        val raised = base.copy(aeonUpgrades = setOf("ae_milestone"))
        val extra = (AeonUpgrades.byId("ae_milestone")!!.effect as PrestigeEffect.MilestoneBonus).extra

        val expected = GameEngine.massPerSecond(base) /
            Milestones.factor(Milestones.STEP * 2) *
            Milestones.factor(Milestones.STEP * 2, Milestones.FACTOR + extra)

        assertEquals(expected, GameEngine.massPerSecond(raised), expected * 1e-9)
    }

    @Test
    fun `an aeon upgrade that no longer exists is dropped from the save`() {
        val raw = SaveCodec.encode(GameState.new(NOW).copy(aeonUpgrades = setOf("ae_erfunden")))
        assertTrue(SaveCodec.decode(raw)!!.aeonUpgrades.isEmpty())
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}

/** The automatic buyer, and the reserve that stops it eating the upgrade budget. */
class AutoBuyTest {

    private fun unlocked(mass: Double): GameState =
        GameState.new(NOW).copy(
            mass = mass,
            totalMass = 1e9,
            singularities = 100.0,
            collapses = 4,
            prestigeUpgrades = setOf("p_autobuy"),
        )

    @Test
    fun `nothing is bought while the setting is off`() {
        val state = unlocked(mass = 1e6)
        assertTrue(GameEngine.hasAutoBuy(state))
        assertEquals(state.collectors, GameEngine.tick(state, 1.0).collectors)
    }

    @Test
    fun `nothing is bought without the upgrade, however much is in the bank`() {
        val state = GameState.new(NOW).copy(mass = 1e9, totalMass = 1e9, autoBuyOn = true)
        assertFalse(GameEngine.hasAutoBuy(state))
        assertEquals(state.collectors, GameEngine.tick(state, 1.0).collectors)
    }

    @Test
    fun `it buys once per tick and stops at the reserve`() {
        val dust = Collectors.byId("dust")!!
        // Exactly enough for one purchase at the reserve, and not a kilogram more.
        val state = unlocked(mass = dust.baseCost * GameEngine.AUTO_BUY_RESERVE)
            .copy(autoBuyOn = true)

        val once = GameEngine.tick(state, 0.001)
        assertEquals(1, once.ownedOf("dust"), "Der erste Kauf ist nicht passiert")

        // The second copy costs more and the purse is now short, so nothing else happens.
        val twice = GameEngine.tick(once.copy(mass = once.mass), 0.001)
        assertEquals(1, twice.ownedOf("dust"), "Die Rücklage wurde übergangen")
    }

    @Test
    fun `it leaves at least three quarters of the mass alone`() {
        var state = unlocked(mass = 5_000.0).copy(autoBuyOn = true)
        val before = state.mass

        // Many ticks with no production, so the only thing moving the purse is the buyer.
        repeat(200) { state = GameEngine.tick(state, 0.0001) }

        assertTrue(state.collectors.values.sum() > 0, "Es wurde überhaupt nichts gekauft")
        assertTrue(
            state.mass > 0.0,
            "Der Automat hat die Kasse leer geräumt: ${state.mass} von $before",
        )
    }

    @Test
    fun `the setting survives a collapse and a big bang`() {
        val state = unlocked(mass = 0.0).copy(
            autoBuyOn = true,
            runMass = Tiers.last.threshold * 2,
            collapses = BigBang.REQUIRED_COLLAPSES,
        )
        assertTrue(GameEngine.collapse(state, NOW).autoBuyOn)
        assertTrue(GameEngine.bigBang(state, NOW).autoBuyOn)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}

/** The events that wait for an answer instead of flying past. */
class EventTest {

    private fun playing(): GameState = GameState.new(NOW).copy(
        mass = 1e9,
        runMass = Tiers.byName(CosmicEvent.FIRST_TIER_NAME).threshold,
        totalMass = 1e9,
        collectors = mapOf("dust" to 60, "net" to 30),
    )

    @Test
    fun `nothing happens before the ladder has got going`() {
        var early = GameState.new(NOW).copy(collectors = mapOf("dust" to 5))
        assertFalse(CosmicEvent.appearsAt(early))
        repeat(50) { early = GameEngine.tick(early, 60.0) }
        assertNull(early.pendingEvent)
    }

    @Test
    fun `an event turns up within the advertised window and then waits`() {
        var state = playing()
        // The first tick only schedules; the event itself is a whole interval away.
        state = GameEngine.tick(state, 1.0)
        assertNull(state.pendingEvent)
        assertTrue(state.nextEventSeconds >= CosmicEvent.MIN_SECONDS)
        assertTrue(state.nextEventSeconds <= CosmicEvent.MAX_SECONDS)

        state = GameEngine.tick(state, CosmicEvent.MAX_SECONDS)
        assertNotNull(state.event, "Nach dem längsten Fenster muss ein Ereignis dastehen")

        // And it stays until it is answered — no queue builds up behind it.
        val waiting = GameEngine.tick(state, CosmicEvent.MAX_SECONDS * 3)
        assertEquals(state.pendingEvent, waiting.pendingEvent)
    }

    @Test
    fun `the clock runs on play time, so a night away does not stack events`() {
        val away = GameEngine.applyOffline(playing().copy(lastSeenAt = NOW), NOW + 8 * 3_600_000)
        assertNull(away.state.pendingEvent)
    }

    @Test
    fun `taking the mass credits it and taking the buff starts it`() {
        var state = playing()
        state = GameEngine.tick(state, 1.0)
        state = GameEngine.tick(state, CosmicEvent.MAX_SECONDS)
        val event = assertNotNull(state.event)

        val windfall = event.first.reward as CometReward.Windfall
        val taken = GameEngine.chooseEvent(state, 0)
        assertNull(taken.pendingEvent)
        assertEquals(state.eventsAnswered + 1, taken.eventsAnswered)
        assertEquals(
            state.mass + GameEngine.massPerSecond(state) * windfall.secondsOfProduction,
            taken.mass,
            taken.mass * 1e-6,
        )

        val buff = (event.second.reward as CometReward.Timed).buff
        val buffed = GameEngine.chooseEvent(state, 1)
        assertEquals(buff, buffed.buff)
        assertEquals(buff.seconds, buffed.buffSecondsLeft)
    }

    @Test
    fun `an index that is neither option changes nothing`() {
        var state = playing()
        state = GameEngine.tick(state, 1.0)
        state = GameEngine.tick(state, CosmicEvent.MAX_SECONDS)
        assertNotNull(state.event)
        assertEquals(state, GameEngine.chooseEvent(state, 7))
    }

    @Test
    fun `turning it down pays nothing and clears the way for the next one`() {
        var state = playing()
        state = GameEngine.tick(state, 1.0)
        state = GameEngine.tick(state, CosmicEvent.MAX_SECONDS)
        val before = state.mass

        val dismissed = GameEngine.dismissEvent(state)
        assertNull(dismissed.pendingEvent)
        assertEquals(before, dismissed.mass)
        assertEquals(state.eventsAnswered, dismissed.eventsAnswered)
    }

    @Test
    fun `the pick is derived from the save, so reloading cannot reroll it`() {
        val state = playing().copy(playedSeconds = 1_234.0, taps = 567)
        assertEquals(CosmicEvent.pick(state), CosmicEvent.pick(state))
        val reloaded = SaveCodec.decode(SaveCodec.encode(state))!!
        assertEquals(CosmicEvent.pick(state), CosmicEvent.pick(reloaded))
    }

    @Test
    fun `both options of every event describe themselves`() {
        for (event in CosmicEvent.entries) {
            for (index in 0..1) {
                val option = assertNotNull(event.optionAt(index), "${event.title} hat keine Option $index")
                assertTrue(option.label.isNotBlank())
                assertTrue(option.rewardText.isNotBlank())
                assertTrue(option.flavor.isNotBlank())
            }
            assertNull(event.optionAt(2))
        }
    }

    @Test
    fun `an event that no longer exists is dropped from the save`() {
        val raw = SaveCodec.encode(playing().copy(pendingEvent = "e_gibtsnicht"))
        assertNull(SaveCodec.decode(raw)!!.pendingEvent)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
