package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang

/** What a challenge takes away for as long as it is running. */
sealed interface ChallengeRule {
    /** Collectors produce nothing. The run is whatever the player's finger can do. */
    data object NoCollectors : ChallengeRule

    /**
     * Tapping yields nothing. The run is whatever the machines can do without help.
     *
     * This is the one rule that cannot simply take something away. A challenge run starts empty on
     * purpose — no mass, no fleet, and none of the head start bought with singularities — which
     * every other rule survives because the finger is still there to earn the first kilogram.
     * Take the finger away as well and the run produces nothing at all, for ever: an empty fleet
     * times any multiplier is zero, offline credit is a multiple of that zero, and a comet pays a
     * span of the same zero. It was not a hard challenge, it was a screen to give up on.
     *
     * So this rule hands over [HEAD_START] machines to get the run moving, and takes the fleet
     * down to [COLLECTOR_POWER] of its usual output to pay for them. The player still cannot help;
     * they can only choose what gets built, which is what the challenge was always about.
     */
    data object NoTaps : ChallengeRule {
        /** What a collector still makes when nobody is helping it along. */
        const val COLLECTOR_POWER = 0.25

        /** Copies of the first collector the run begins with, because nothing else can earn one. */
        const val HEAD_START = 1
    }

    /** Everything is multiplied by [factor], which is below one. */
    data class Handicap(val factor: Double) : ChallengeRule

    /** The upgrade shop is shut. Only more of the same, never better. */
    data object NoUpgrades : ChallengeRule

    /** Nothing stays in orbit, so the system contributes nothing. */
    data object NoOrbits : ChallengeRule

    /** Every collector counts as a single copy of itself: the serial bonuses are off. */
    data object NoMilestones : ChallengeRule

    /** A closed app earns nothing. The run only advances while somebody is watching it. */
    data object NoOffline : ChallengeRule

    /** The chain is cold. Nothing fuses, so none of the element bonuses grow. */
    data object NoFusion : ChallengeRule

    /** The parked universes contribute nothing. Whatever this run manages, it manages alone. */
    data object NoSky : ChallengeRule
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
    val germanTitle: String,
    val germanFlavor: String,
    val rule: ChallengeRule,
    val goal: ChallengeGoal,
    val reward: PrestigeEffect,
    /** Collapses the player needs behind them before this is offered. */
    val requiredCollapses: Int = 1,
) {
    HANDARBEIT(
        id = "c_hand",
        germanTitle = "Handarbeit",
        germanFlavor = "Die ganze Flotte steht still. Was du willst, holst du dir selbst.",
        rule = ChallengeRule.NoCollectors,
        goal = ChallengeGoal.ReachTier("Erde"),
        reward = PrestigeEffect.TapMultiplier(3.0),
    ),
    NICHTSTUN(
        id = "c_idle",
        germanTitle = "Nichtstun",
        germanFlavor = "Nimm die Hände weg. Einen Staubfänger kriegst du geschenkt — der Rest " +
            "wächst ohne dich.",
        rule = ChallengeRule.NoTaps,
        goal = ChallengeGoal.ReachTier("Saturn"),
        reward = PrestigeEffect.GlobalMultiplier(1.5),
    ),
    SPRINT(
        id = "c_sprint",
        germanTitle = "Sprint",
        germanFlavor = "Bis zum Saturn, in fünfundvierzig Minuten. Die Uhr läuft nur, wenn du spielst.",
        rule = ChallengeRule.Handicap(1.0),
        goal = ChallengeGoal.ReachTierWithin("Saturn", 45 * 60.0),
        reward = PrestigeEffect.CometFrequency(2.0),
        requiredCollapses = 2,
    ),
    HALBE_KRAFT(
        id = "c_half",
        germanTitle = "Halbe Kraft",
        germanFlavor = "Alles bringt die Hälfte. Bis zum Schwarzen Loch trotzdem.",
        rule = ChallengeRule.Handicap(0.5),
        goal = ChallengeGoal.ReachTier("Schwarzes Loch"),
        reward = PrestigeEffect.GlobalMultiplier(2.5),
        requiredCollapses = 3,
    ),
    ROHBAU(
        id = "c_raw",
        germanTitle = "Rohbau",
        germanFlavor = "Der Upgrade-Laden ist zu. Mehr Maschinen ja, bessere nein.",
        rule = ChallengeRule.NoUpgrades,
        goal = ChallengeGoal.ReachTier("Jupiter"),
        reward = PrestigeEffect.MilestoneBonus(0.03),
        requiredCollapses = 2,
    ),
    ALLEIN(
        id = "c_alone",
        germanTitle = "Allein",
        germanFlavor = "Nichts bleibt auf einer Bahn. Was du schaffst, schaffst du ohne Trabanten.",
        rule = ChallengeRule.NoOrbits,
        goal = ChallengeGoal.ReachTier("Roter Überriese"),
        reward = PrestigeEffect.FusionRate(1.5),
        requiredCollapses = 3,
    ),
    EILE(
        id = "c_rush",
        germanTitle = "Eile",
        germanFlavor = "Bis zur Sonne, in neunzig Minuten. Die Uhr läuft nur, wenn du spielst.",
        rule = ChallengeRule.Handicap(1.0),
        goal = ChallengeGoal.ReachTierWithin("Sonne", 90 * 60.0),
        reward = PrestigeEffect.ResearchSpeed(1.5),
        requiredCollapses = 4,
    ),
    ASKESE(
        id = "c_ascetic",
        germanTitle = "Askese",
        germanFlavor = "Kein einziges Upgrade, den ganzen Weg bis zur Sonne. Nur Maschinen und Geduld.",
        rule = ChallengeRule.NoUpgrades,
        goal = ChallengeGoal.ReachTier("Sonne"),
        reward = PrestigeEffect.GlobalMultiplier(3.0),
        requiredCollapses = 5,
    ),

    // Everything below here is for the long road to a full sky. Eight universes is a great many
    // more collapses than the first eight challenges were written against, and a list that ran out
    // by the third universe left the other five with nothing optional to do.
    SERIENSTOPP(
        id = "c_nomiles",
        germanTitle = "Serienstopp",
        germanFlavor = "Jede Maschine zählt einzeln. Die Fertigungsstraßen haben Betriebsferien.",
        rule = ChallengeRule.NoMilestones,
        goal = ChallengeGoal.ReachTier("Roter Zwerg"),
        reward = PrestigeEffect.MilestoneBonus(0.04),
        requiredCollapses = 6,
    ),
    WACHDIENST(
        id = "c_nooffline",
        germanTitle = "Wachdienst",
        germanFlavor = "Zugeklappt läuft nichts weiter. Was du willst, musst du sehen.",
        rule = ChallengeRule.NoOffline,
        goal = ChallengeGoal.ReachTier("Blauer Riese"),
        reward = PrestigeEffect.OfflineEfficiency(1.0),
        requiredCollapses = 8,
    ),
    KALTE_KETTE(
        id = "c_nofusion",
        germanTitle = "Kalte Kette",
        germanFlavor = "Kein Ofen brennt. Schwere Kerne musst du diesmal woanders herbekommen.",
        rule = ChallengeRule.NoFusion,
        goal = ChallengeGoal.ReachTier("Neutronenstern"),
        reward = PrestigeEffect.FusionRate(2.0),
        requiredCollapses = 10,
    ),
    EINSAMES_UNIVERSUM(
        id = "c_nosky",
        germanTitle = "Einsames Universum",
        germanFlavor = "Die anderen Galaxien schweigen. Dieses hier schafft es allein oder gar nicht.",
        rule = ChallengeRule.NoSky,
        goal = ChallengeGoal.ReachTier("Schwarzes Loch"),
        reward = PrestigeEffect.GlobalMultiplier(4.0),
        requiredCollapses = 12,
    ),
    HANDBETRIEB(
        id = "c_hand2",
        germanTitle = "Handbetrieb",
        germanFlavor = "Noch einmal ohne Flotte, und diesmal bis zur Sonne.",
        rule = ChallengeRule.NoCollectors,
        goal = ChallengeGoal.ReachTier("Sonne"),
        reward = PrestigeEffect.TapMultiplier(6.0),
        requiredCollapses = 14,
    ),
    VIERTELKRAFT(
        id = "c_quarter",
        germanTitle = "Viertelkraft",
        germanFlavor = "Alles bringt ein Viertel. Bis zum Schwarzen Loch trotzdem.",
        rule = ChallengeRule.Handicap(0.25),
        goal = ChallengeGoal.ReachTier("Schwarzes Loch"),
        reward = PrestigeEffect.GlobalMultiplier(6.0),
        requiredCollapses = 16,
    ),
    HETZE(
        id = "c_dash",
        germanTitle = "Hetze",
        germanFlavor = "Bis zum Schwarzen Loch, in zwei Stunden. Die Uhr läuft nur, wenn du spielst.",
        rule = ChallengeRule.Handicap(1.0),
        goal = ChallengeGoal.ReachTierWithin("Schwarzes Loch", 120 * 60.0),
        reward = PrestigeEffect.SingularityGain(1.6),
        requiredCollapses = 18,
    ),
    ROHBAU_ZWEI(
        id = "c_raw2",
        germanTitle = "Rohbau II",
        germanFlavor = "Der Laden bleibt zu, den ganzen Weg bis zum Schwarzen Loch.",
        rule = ChallengeRule.NoUpgrades,
        goal = ChallengeGoal.ReachTier("Schwarzes Loch"),
        reward = PrestigeEffect.MilestoneBonus(0.05),
        requiredCollapses = 20,
    ),
    ;

    val title: String get() = Lang.t(germanTitle)

    val flavor: String get() = Lang.t(germanFlavor)

    /**
     * What the goal asks for, as a line the player can read.
     *
     * The body's name arrives already translated — [Tiers.nameOf] goes through [Lang] — so the
     * template takes it as a value. Written the other way round, as a translated fragment glued
     * into a German frame, an English goal would read "Reach in 20 minutes Black Hole".
     */
    val goalText: String
        get() = when (goal) {
            is ChallengeGoal.ReachTier -> Lang.t("Erreiche %s", Lang.t(goal.tierName))
            is ChallengeGoal.ReachTierWithin ->
                Lang.t(
                    "Erreiche %s in %s",
                    Lang.t(goal.tierName),
                    Numbers.formatDuration(goal.seconds.toLong()),
                )
        }

    val ruleText: String
        get() = when (rule) {
            is ChallengeRule.NoCollectors -> Lang.t("Kollektoren produzieren nichts")
            is ChallengeRule.NoTaps ->
                Lang.t(
                    "Tippen bringt nichts, Kollektoren nur %s",
                    Numbers.formatPercent(ChallengeRule.NoTaps.COLLECTOR_POWER),
                )
            is ChallengeRule.Handicap ->
                if (rule.factor >= 1.0) Lang.t("Keine Einschränkung")
                else Lang.t("Alles bringt nur %s", Numbers.formatPercent(rule.factor))

            is ChallengeRule.NoUpgrades -> Lang.t("Der Upgrade-Laden bleibt zu")
            is ChallengeRule.NoOrbits -> Lang.t("Nichts hält sich auf einer Bahn")
            is ChallengeRule.NoMilestones -> Lang.t("Keine Meilenstein-Boni")
            is ChallengeRule.NoOffline -> Lang.t("Geschlossen zählt nicht")
            is ChallengeRule.NoFusion -> Lang.t("Die Fusionskette bleibt kalt")
            is ChallengeRule.NoSky -> Lang.t("Die Galaxien tragen nichts bei")
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
            // Both sources of mass switched off is a run that produces nothing at all. NoTaps now
            // brings a fleet of its own, but NoCollectors switches that fleet off too, so the pair
            // is still exactly as dead as it always was — and still the only pair that is refused.
            //
            // Two handicaps together were briefly refused as well, on the theory that a quarter of
            // a half is a run nobody finishes. That was wrong twice over: the timed challenges are
            // written as `Handicap(1.0)`, which handicaps nothing, so the rule would have blocked
            // pairing a stopwatch with anything; and a brutal duo is not a broken one. A pair is
            // supposed to be worse than either half — that is what the bonus is paid for.
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
