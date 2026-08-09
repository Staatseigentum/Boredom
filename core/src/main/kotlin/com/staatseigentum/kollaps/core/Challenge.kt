package com.staatseigentum.kollaps.core

/** What a challenge takes away for as long as it is running. */
sealed interface ChallengeRule {
    /** Collectors produce nothing. The run is whatever the player's finger can do. */
    data object NoCollectors : ChallengeRule

    /** Tapping yields nothing. The run is whatever the machines can do without help. */
    data object NoTaps : ChallengeRule

    /** Everything is multiplied by [factor], which is below one. */
    data class Handicap(val factor: Double) : ChallengeRule

    /** The upgrade shop is shut. Only more of the same, never better. */
    data object NoUpgrades : ChallengeRule

    /** Nothing stays in orbit, so the system contributes nothing. */
    data object NoOrbits : ChallengeRule
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
    ROHBAU(
        id = "c_raw",
        title = "Rohbau",
        flavor = "Der Upgrade-Laden ist zu. Mehr Maschinen ja, bessere nein.",
        rule = ChallengeRule.NoUpgrades,
        goal = ChallengeGoal.ReachTier("Jupiter"),
        reward = PrestigeEffect.MilestoneBonus(0.03),
        requiredCollapses = 2,
    ),
    ALLEIN(
        id = "c_alone",
        title = "Allein",
        flavor = "Nichts bleibt auf einer Bahn. Was du schaffst, schaffst du ohne Trabanten.",
        rule = ChallengeRule.NoOrbits,
        goal = ChallengeGoal.ReachTier("Roter Überriese"),
        reward = PrestigeEffect.FusionRate(1.5),
        requiredCollapses = 3,
    ),
    EILE(
        id = "c_rush",
        title = "Eile",
        flavor = "Bis zur Sonne, in neunzig Minuten. Die Uhr läuft nur, wenn du spielst.",
        rule = ChallengeRule.Handicap(1.0),
        goal = ChallengeGoal.ReachTierWithin("Sonne", 90 * 60.0),
        reward = PrestigeEffect.ResearchSpeed(1.5),
        requiredCollapses = 4,
    ),
    ASKESE(
        id = "c_ascetic",
        title = "Askese",
        flavor = "Kein einziges Upgrade, den ganzen Weg bis zur Sonne. Nur Maschinen und Geduld.",
        rule = ChallengeRule.NoUpgrades,
        goal = ChallengeGoal.ReachTier("Sonne"),
        reward = PrestigeEffect.GlobalMultiplier(3.0),
        requiredCollapses = 5,
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

            is ChallengeRule.NoUpgrades -> "Der Upgrade-Laden bleibt zu"
            is ChallengeRule.NoOrbits -> "Nichts hält sich auf einer Bahn"
        }

    /** Whether this one, on its own, has been met by the state given. */
    fun isMetBy(state: GameState): Boolean {
        val reached = Tiers.forMass(state.runMass).index
        return when (val goal = goal) {
            is ChallengeGoal.ReachTier -> reached >= Tiers.indexOf(goal.tierName)
            is ChallengeGoal.ReachTierWithin ->
                reached >= Tiers.indexOf(goal.tierName) && state.challengeSeconds <= goal.seconds
        }
    }

    /** Whether this one can no longer be won. Only the timed goals can actually fail. */
    fun isLostBy(state: GameState): Boolean {
        val goal = goal as? ChallengeGoal.ReachTierWithin ?: return false
        return state.challengeSeconds > goal.seconds &&
            Tiers.forMass(state.runMass).index < Tiers.indexOf(goal.tierName)
    }

    companion object {
        /**
         * How many may run at once.
         *
         * Two rather than any number. Three of these rules together is not a harder run, it is a
         * slower one with the same shape, and the screen would need a third column to say so.
         */
        const val MAX_AT_ONCE = 2

        /** What a pair is worth on top of the two rewards, for good. */
        const val DUO_BONUS = 1.2

        /**
         * Whether two challenges can be run together.
         *
         * There is exactly one pair that cannot: the game has two sources of mass, and switching
         * both off leaves a run that produces nothing at all. That is not a hard challenge, it is
         * a screen the player has to give up on — so it is refused where it is chosen rather than
         * discovered ten minutes in.
         */
        fun canCombine(first: Challenge, second: Challenge): Boolean {
            if (first == second) return false
            val rules = setOf(first.rule, second.rule)
            return !(ChallengeRule.NoCollectors in rules && ChallengeRule.NoTaps in rules)
        }

        /** Whether a whole selection may be started together. */
        fun canCombineAll(challenges: Collection<Challenge>): Boolean =
            challenges.size <= 1 ||
                challenges.all { one -> challenges.all { other -> one == other || canCombine(one, other) } }

        fun byId(id: String?): Challenge? = entries.firstOrNull { it.id == id }

        /** Not yet done, and enough collapses behind the player. */
        fun offered(state: GameState): List<Challenge> =
            entries.filter { it.id !in state.challengesDone && state.collapses >= it.requiredCollapses }

        /**
         * The name under which a pair is recorded, and the reason it is sorted.
         *
         * A duo is one thing however the player picked the two, so the id may not depend on the
         * order they were tapped in — otherwise the same pair could be earned twice.
         */
        fun duoId(ids: Collection<String>): String = ids.sorted().joinToString("+")

        /** Every running challenge, in a fixed order so the screen never reshuffles. */
        fun running(state: GameState): List<Challenge> =
            entries.filter { it.id in state.runningChallengeIds }

        /** Whether everything currently running has been met and the set can be handed in. */
        fun isMet(state: GameState): Boolean {
            val running = running(state)
            return running.isNotEmpty() && running.all { it.isMetBy(state) }
        }

        /**
         * Whether the run can no longer be won.
         *
         * One lost challenge loses the pair: they are handed in together or not at all, which is
         * the risk that pays for the bonus.
         */
        fun isLost(state: GameState): Boolean = running(state).any { it.isLostBy(state) }
    }
}
