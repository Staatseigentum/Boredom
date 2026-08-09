package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Two challenges at once: what may be combined, what it costs, and what it pays. */
class ChallengeDuoTest {

    private val now = 1_700_000_000_000L

    private fun veteran(collapses: Int = 20) = GameState(
        collapses = collapses,
        singularities = 50.0,
        runMass = 0.0,
    )

    /** Two of the eight that can legitimately run together. */
    private fun pair(): List<Challenge> = Challenge.entries
        .flatMap { first -> Challenge.entries.map { second -> first to second } }
        .first { (first, second) -> Challenge.canCombine(first, second) }
        .let { listOf(it.first, it.second) }

    @Test
    fun `two can be started together`() {
        val two = pair()
        val started = GameEngine.startChallenges(veteran(), two.map { it.id }.toSet(), now)

        assertEquals(two.map { it.id }.toSet(), started.runningChallengeIds)
        assertEquals(0.0, started.runMass, "Der Lauf wurde nicht zurückgesetzt")
    }

    /**
     * The one pair the game cannot offer.
     *
     * Collectors off and taps off together is a run that produces nothing at all — not a hard
     * challenge but a screen to give up on. It has to be refused where it is chosen.
     */
    @Test
    fun `the pair that would produce nothing is refused`() {
        val silent = setOf(Challenge.HANDARBEIT.id, Challenge.NICHTSTUN.id)
        assertFalse(Challenge.canCombine(Challenge.HANDARBEIT, Challenge.NICHTSTUN))

        val state = veteran()
        assertEquals(state, GameEngine.startChallenges(state, silent, now), "Sie wurde angenommen")
    }

    @Test
    fun `every other pair is allowed`() {
        for (first in Challenge.entries) {
            for (second in Challenge.entries) {
                if (first == second) continue
                val silent = setOf(first.rule, second.rule) ==
                    setOf<ChallengeRule>(ChallengeRule.NoCollectors, ChallengeRule.NoTaps)
                assertEquals(
                    !silent,
                    Challenge.canCombine(first, second),
                    "${first.id} + ${second.id}",
                )
            }
        }
    }

    @Test
    fun `more than two at once is refused`() {
        val three = Challenge.entries.take(3).map { it.id }.toSet()
        val state = veteran()
        assertEquals(state, GameEngine.startChallenges(state, three, now))
    }

    @Test
    fun `nothing starts while something is already running`() {
        val two = pair()
        val started = GameEngine.startChallenges(veteran(), setOf(two[0].id), now)
        val again = GameEngine.startChallenges(started, setOf(two[1].id), now)

        assertEquals(setOf(two[0].id), again.runningChallengeIds, "Eine kam mitten im Lauf dazu")
    }

    @Test
    fun `both rules apply at the same time`() {
        val handicapped = GameEngine.startChallenges(
            veteran(),
            setOf(Challenge.HANDARBEIT.id, Challenge.HALBE_KRAFT.id),
            now,
        )
        assertTrue(
            Challenge.canCombine(Challenge.HANDARBEIT, Challenge.HALBE_KRAFT),
            "Der Testaufbau prüft ein verbotenes Paar",
        )

        // Collectors are off, so what the fleet would make does not arrive whatever else is on.
        val withFleet = handicapped.copy(collectors = mapOf("dust" to 50))
        assertEquals(0.0, GameEngine.massPerSecond(withFleet), 1e-9)
    }

    @Test
    fun `both goals are needed before either counts`() {
        val two = pair()
        val started = GameEngine.startChallenges(veteran(), two.map { it.id }.toSet(), now)
        val far = started.copy(runMass = Tiers.last.threshold)

        assertTrue(Challenge.isMet(far), "Ganz oben ist immer noch nicht beides erfüllt")

        // Only as far as the easier of the two: the pair is not met until both are.
        val lowest = two.minOf { challenge ->
            when (val goal = challenge.goal) {
                is ChallengeGoal.ReachTier -> Tiers.indexOf(goal.tierName)
                is ChallengeGoal.ReachTierWithin -> Tiers.indexOf(goal.tierName)
            }
        }
        val partway = started.copy(runMass = Tiers.all[lowest].threshold)
        val bothMet = two.all { it.isMetBy(partway) }
        assertEquals(bothMet, Challenge.isMet(partway))
    }

    @Test
    fun `one lost challenge loses the pair`() {
        val timed = Challenge.entries.first { it.goal is ChallengeGoal.ReachTierWithin }
        val other = Challenge.entries.first { Challenge.canCombine(timed, it) }
        val limit = (timed.goal as ChallengeGoal.ReachTierWithin).seconds

        val started = GameEngine.startChallenges(veteran(), setOf(timed.id, other.id), now)
        val overdue = started.copy(challengeSeconds = limit + 1.0)

        assertTrue(Challenge.isLost(overdue))
        assertFalse(Challenge.isMet(overdue))
    }

    @Test
    fun `handing a pair in records both and the bonus`() {
        val two = pair()
        val started = GameEngine.startChallenges(veteran(), two.map { it.id }.toSet(), now)
        val done = GameEngine.finishChallenge(started.copy(runMass = Tiers.last.threshold), now)

        for (challenge in two) {
            assertTrue(challenge.id in done.challengesDone, "${challenge.id} fehlt")
        }
        assertEquals(
            setOf(Challenge.duoId(two.map { it.id })),
            done.challengeDuos,
            "Der Paar-Bonus wurde nicht vermerkt",
        )
        assertTrue(done.runningChallengeIds.isEmpty())
    }

    /** What keeps the bonus bounded: it can only ever be earned on untouched ground. */
    @Test
    fun `a pair whose half was already beaten earns no bonus`() {
        val two = pair()
        val already = veteran().copy(challengesDone = setOf(two[0].id))
        val started = GameEngine.startChallenges(already, setOf(two[1].id), now)
        val done = GameEngine.finishChallenge(started.copy(runMass = Tiers.last.threshold), now)

        assertTrue(done.challengeDuos.isEmpty(), "Ein Bonus für halb bekanntes Gelände")
    }

    @Test
    fun `a single challenge earns no pair bonus`() {
        val one = Challenge.entries.first()
        val started = GameEngine.startChallenges(veteran(), setOf(one.id), now)
        val done = GameEngine.finishChallenge(started.copy(runMass = Tiers.last.threshold), now)

        assertTrue(one.id in done.challengesDone)
        assertTrue(done.challengeDuos.isEmpty())
    }

    @Test
    fun `the pair id does not depend on the order they were picked`() {
        assertEquals(Challenge.duoId(listOf("b", "a")), Challenge.duoId(listOf("a", "b")))
    }

    @Test
    fun `the bonus is worth something and survives every reset`() {
        val two = pair()
        val earned = veteran().copy(challengeDuos = setOf(Challenge.duoId(two.map { it.id })))
        val without = earned.copy(challengeDuos = emptySet())

        val withFleet = { s: GameState -> s.copy(collectors = mapOf("dust" to 20)) }
        assertTrue(
            GameEngine.massPerSecond(withFleet(earned)) > GameEngine.massPerSecond(withFleet(without)),
            "Der Paar-Bonus wirkt nicht",
        )

        val collapsed = GameEngine.collapse(earned.copy(runMass = Tiers.last.threshold), now)
        assertEquals(earned.challengeDuos, collapsed.challengeDuos, "Der Kollaps hat ihn gefressen")

        val banged = GameEngine.bigBang(
            collapsed.copy(runMass = Tiers.last.threshold, collapses = 60, singularities = 1e9),
            now,
        )
        assertEquals(earned.challengeDuos, banged.challengeDuos, "Der Urknall hat ihn gefressen")
    }

    @Test
    fun `an old save keeps running the challenge it was running`() {
        val legacy = veteran().copy(activeChallenge = Challenge.entries.first().id)
        assertEquals(setOf(Challenge.entries.first().id), legacy.runningChallengeIds)
        assertEquals(Challenge.entries.first(), legacy.challenge)
    }
}
