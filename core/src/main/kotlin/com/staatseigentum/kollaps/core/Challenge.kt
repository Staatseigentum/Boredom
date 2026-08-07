package com.staatseigentum.kollaps.core

/** What a challenge takes away for as long as it is running. */
sealed interface ChallengeRule {
    /** Collectors produce nothing. The run is whatever the player's finger can do. */
    data object NoCollectors : ChallengeRule

    /** Tapping yields nothing. The run is whatever the machines can do without help. */
    data object NoTaps : ChallengeRule

    /** Everything is multiplied by [factor], which is below one. */
    data class Handicap(val factor: Double) : ChallengeRule
}

/** What finishes a challenge. */
sealed interface ChallengeGoal {
    data class ReachTier(val tierName: String) : ChallengeGoal

    /** Reach a body within [seconds] of play *inside the challenge*, not of wall clock. */
    data class ReachTierWithin(val tierName: String, val seconds: Double) : ChallengeGoal
}

/**
 * An optional run under a rule that makes the game worse, for a reward that makes it better.
 *
 * Prestige answers "how do I go faster"; this answers "what else is there to do". The reward is a
 * [PrestigeEffect] like any other so it flows through the same modifier pass — a challenge is a
 * different way to earn a permanent bonus, not a different kind of bonus.
 */
enum class Challenge(
    val id: String,
    val title: String,
    val flavor: String,
    val rule: ChallengeRule,
    val goal: ChallengeGoal,
    val reward: PrestigeEffect,
    /** Collapses the player needs behind them before this is offered. */
    val requiredCollapses: Int = 1,
) {
    HANDARBEIT(
        id = "c_hand",
        title = "Handarbeit",
        flavor = "Die ganze Flotte steht still. Was du willst, holst du dir selbst.",
        rule = ChallengeRule.NoCollectors,
        goal = ChallengeGoal.ReachTier("Erde"),
        reward = PrestigeEffect.TapMultiplier(3.0),
    ),
    NICHTSTUN(
        id = "c_idle",
        title = "Nichtstun",
        flavor = "Nimm die Hände weg. Die Maschinen können das auch allein.",
        rule = ChallengeRule.NoTaps,
        goal = ChallengeGoal.ReachTier("Saturn"),
        reward = PrestigeEffect.GlobalMultiplier(1.5),
    ),
    SPRINT(
        id = "c_sprint",
        title = "Sprint",
        flavor = "Bis zum Saturn, in fünfundvierzig Minuten. Die Uhr läuft nur, wenn du spielst.",
        rule = ChallengeRule.Handicap(1.0),
        goal = ChallengeGoal.ReachTierWithin("Saturn", 45 * 60.0),
        reward = PrestigeEffect.CometFrequency(2.0),
        requiredCollapses = 2,
    ),
    HALBE_KRAFT(
        id = "c_half",
        title = "Halbe Kraft",
        flavor = "Alles bringt die Hälfte. Bis zum Schwarzen Loch trotzdem.",
        rule = ChallengeRule.Handicap(0.5),
        goal = ChallengeGoal.ReachTier("Schwarzes Loch"),
        reward = PrestigeEffect.GlobalMultiplier(2.5),
        requiredCollapses = 3,
    ),
    ;

    /** What the goal asks for, as a line the player can read. */
    val goalText: String
        get() = when (goal) {
            is ChallengeGoal.ReachTier -> "Erreiche ${goal.tierName}"
            is ChallengeGoal.ReachTierWithin ->
                "Erreiche ${goal.tierName} in ${Numbers.formatDuration(goal.seconds.toLong())}"
        }

    val ruleText: String
        get() = when (rule) {
            is ChallengeRule.NoCollectors -> "Kollektoren produzieren nichts"
            is ChallengeRule.NoTaps -> "Tippen bringt nichts"
            is ChallengeRule.Handicap ->
                if (rule.factor >= 1.0) "Keine Einschränkung"
                else "Alles bringt nur ${Numbers.formatPercent(rule.factor)}"
        }

    companion object {
        fun byId(id: String?): Challenge? = entries.firstOrNull { it.id == id }

        /** Not yet done, and enough collapses behind the player. */
        fun offered(state: GameState): List<Challenge> =
            entries.filter { it.id !in state.challengesDone && state.collapses >= it.requiredCollapses }

        /** Whether the running challenge has been met and can be handed in. */
        fun isMet(state: GameState): Boolean {
            val challenge = byId(state.activeChallenge) ?: return false
            val reached = Tiers.forMass(state.runMass).index
            return when (val goal = challenge.goal) {
                is ChallengeGoal.ReachTier -> reached >= Tiers.indexOf(goal.tierName)
                is ChallengeGoal.ReachTierWithin ->
                    reached >= Tiers.indexOf(goal.tierName) && state.challengeSeconds <= goal.seconds
            }
        }

        /**
         * Whether the running challenge can no longer be won.
         *
         * Only the timed one can actually fail; the others are merely slow. Saying so lets the
         * screen offer a restart instead of leaving the player to work it out.
         */
        fun isLost(state: GameState): Boolean {
            val challenge = byId(state.activeChallenge) ?: return false
            val goal = challenge.goal as? ChallengeGoal.ReachTierWithin ?: return false
            return state.challengeSeconds > goal.seconds &&
                Tiers.forMass(state.runMass).index < Tiers.indexOf(goal.tierName)
        }
    }
}
