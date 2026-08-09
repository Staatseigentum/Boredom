package com.staatseigentum.kollaps.core

import kotlinx.serialization.Serializable

/**
 * The complete, persisted state of a save file. Everything else the game shows is derived from
 * this by [GameEngine], so the save stays small and forward compatible: content definitions live
 * in code, the save only references them by id.
 */
@Serializable
data class GameState(
    val version: Int = SAVE_VERSION,

    /** Mass available to spend right now. */
    val mass: Double = 0.0,

    /** Mass collected during the current run. Drives the tier ladder. */
    val runMass: Double = 0.0,

    /** Mass collected since the very first launch, across all collapses. */
    val totalMass: Double = 0.0,

    /** Number of copies owned, keyed by collector id. */
    val collectors: Map<String, Int> = emptyMap(),

    /**
     * Which role each collector is set to, keyed by collector id. See [Roles].
     *
     * Part of the run: a collapse takes the fleet, so it takes how the fleet was set up.
     */
    val roles: Map<String, String> = emptyMap(),

    /** Ids of upgrades bought in the current run. */
    val upgrades: Set<String> = emptySet(),

    /** Permanent prestige currency earned by collapsing black holes. */
    val singularities: Double = 0.0,

    /** How often the player collapsed a black hole. */
    val collapses: Int = 0,

    /** Total taps, across all runs. */
    val taps: Long = 0,

    /** Highest tier index ever reached, across all runs. */
    val bestTier: Int = 0,

    /** Most mass ever collected within a single run. */
    val bestRunMass: Double = 0.0,

    /** Highest tier the player has already been shown the "new tier" celebration for. */
    val celebratedTier: Int = 0,

    /** Wall clock of the last save, used to credit offline production. */
    val lastSeenAt: Long = 0,

    /** Wall clock of the first launch. */
    val startedAt: Long = 0,

    /** Prestige upgrades bought. Unlike [upgrades], these survive a collapse. */
    val prestigeUpgrades: Set<String> = emptySet(),

    /**
     * Levels of each repeatable singularity investment. Survive a collapse, not a big bang —
     * they are bought with singularities, and the big bang takes those. See [Investments].
     */
    val investments: Map<String, Int> = emptyMap(),

    /** Achievements earned, across all runs. */
    val achievements: Set<String> = emptySet(),

    /** Seconds the game was actually on screen, for the statistics. */
    val playedSeconds: Double = 0.0,

    /** Seconds of play spent in the current run. Reset by anything that resets the run. */
    val runSeconds: Double = 0.0,

    /** How long the previous run took and how far it got, so this one has something to beat. */
    val lastRunSeconds: Double = 0.0,
    val lastRunMass: Double = 0.0,

    /** Comets caught, across all runs. */
    val cometsCaught: Long = 0,

    /** Id of the buff currently running, if any. */
    val buffId: String? = null,

    /** Seconds the current buff has left. Counted down by the tick, not by the wall clock, so
     *  putting the phone away pauses it rather than wasting it. */
    val buffSecondsLeft: Double = 0.0,

    /** Whether the click sound plays. */
    val soundOn: Boolean = true,

    /** Whether tapping vibrates. */
    val hapticsOn: Boolean = true,

    /** Whether the background loop plays. */
    val musicOn: Boolean = true,

    /** Id of the challenge being run, if any. */
    val activeChallenge: String? = null,

    /** Seconds of play spent inside the running challenge. Only the tick moves it. */
    val challengeSeconds: Double = 0.0,

    /** Challenges completed. Their rewards are permanent, like prestige upgrades. */
    val challengesDone: Set<String> = emptySet(),

    /**
     * Fraction of an automatic tap carried over between ticks.
     *
     * The tick runs many times a second, so three automatic taps per second is a fraction of a
     * tap each time. Without somewhere to keep the remainder the tap counter would round every
     * one of them down to nothing.
     */
    val autoTapCarry: Double = 0.0,

    /** Äonen from big bangs. The currency below the singularity. */
    val aeons: Double = 0.0,

    /** Äonen upgrades bought. These survive even a big bang. */
    val aeonUpgrades: Set<String> = emptySet(),

    /** How often the player has thrown a whole universe away. */
    val bigBangs: Int = 0,

    /** Which kind of universe this one is, chosen at the big bang. See [Path]. */
    val path: String? = null,

    /** Whether the automatic buyer is switched on. Off by default even once it is unlocked. */
    val autoBuyOn: Boolean = false,

    /** Whether the game may remind the player that the collectors have filled up. */
    val remindersOn: Boolean = true,

    /**
     * Id of whatever is waiting for an answer, if anything.
     *
     * Either a [CosmicEvent] id or, while a chain is running, the id of the [ChainStation] on the
     * table. One field rather than two, because "something is asking a question" is one condition
     * and two flags for it is two ways to disagree; which kind it is comes from [activeChain].
     */
    val pendingEvent: String? = null,

    /** The [EventChain] being told, if one is. */
    val activeChain: String? = null,

    /**
     * Which station of that chain comes next.
     *
     * Held apart from [pendingEvent] so that turning a station down closes the dialog without
     * losing the story: the chain stays where it is and asks again at the next interval.
     */
    val chainStation: String? = null,

    /** Chains seen through to an ending. Kept for good, like the achievements. */
    val chainsDone: Set<String> = emptySet(),

    /**
     * Seconds of play until the next event.
     *
     * Zero on a fresh save, which the tick reads as "not scheduled yet" and fills in. Counting in
     * play time rather than wall clock means an event cannot be waiting the moment the app opens
     * after a night away.
     */
    val nextEventSeconds: Double = 0.0,

    /** Events answered, for the statistics. */
    val eventsAnswered: Long = 0,

    /** Production per second, sampled during play. Oldest first. See [History]. */
    val history: List<Double> = emptyList(),

    /** Seconds of play since the last sample was taken. */
    val historySeconds: Double = 0.0,

    /** Elements held, keyed by [Element.id]. Part of the run, so a collapse takes them along. */
    val elements: Map<String, Double> = emptyMap(),

    /** Levels of each fusion stage, keyed by [FusionStage.id]. */
    val fusers: Map<String, Int> = emptyMap(),

    /**
     * Heavy elements forged in past collapses, keyed by [HeavyElement.id].
     *
     * The one holding that survives everything, big bang included — see [Heavy].
     */
    val heavy: Map<String, Double> = emptyMap(),

    /** How many orbit slots are open. Part of the run — a collapse scatters the system. */
    val orbits: Int = 0,

    /** Mass of the body on each occupied slot, keyed by orbit index. See [Orbits]. */
    val satellites: Map<Int, Double> = emptyMap(),

    /** Research projects finished. Permanent: neither a collapse nor a big bang touches them. */
    val research: Set<String> = emptySet(),

    /** Id of the project running in the lab, if any. */
    val activeResearch: String? = null,

    /**
     * Wall clock at which the running project comes due.
     *
     * An absolute time rather than a countdown, because this is the one clock in the game that
     * keeps running with the app closed. A remaining-seconds field would have to be topped up by
     * something, and nothing runs while the phone is off.
     */
    val researchDoneAt: Long = 0,

    /**
     * Automation rules that are switched on, and which setting each runs at.
     *
     * A rule missing from the map is off. See [Automation], which also explains why the older
     * [autoBuyOn] flag is still read rather than migrated away.
     */
    val automation: Map<String, Int> = emptyMap(),
) {
    fun ownedOf(collectorId: String): Int = collectors[collectorId] ?: 0

    fun owns(upgradeId: String): Boolean = upgradeId in upgrades

    fun ownsPrestige(upgradeId: String): Boolean = upgradeId in prestigeUpgrades

    fun ownsAeon(upgradeId: String): Boolean = upgradeId in aeonUpgrades

    /** The buff currently running, or `null` once it has run out. */
    val buff: Buff? get() = if (buffSecondsLeft > 0.0) Buff.byId(buffId) else null

    /** The challenge currently being run, or `null`. */
    val challenge: Challenge? get() = Challenge.byId(activeChallenge)

    /** The one-off event waiting for an answer, or `null` — a chain station is not one. */
    val event: CosmicEvent? get() = if (activeChain != null) null else CosmicEvent.byId(pendingEvent)

    /** The chain station waiting for an answer, or `null`. */
    val station: ChainStation? get() = Chains.stationOf(this)

    /**
     * What the dialog should put on screen, whichever kind of event produced it.
     *
     * The screen has no business knowing whether a question came from a one-off or from the middle
     * of a story — it draws a title, a sentence and two answers either way. Deriving the one shape
     * here keeps that difference in the rules, where it belongs.
     */
    val prompt: EventPrompt?
        get() {
            station?.let { stop ->
                return EventPrompt(
                    title = stop.title,
                    flavor = stop.flavor,
                    first = stop.first,
                    second = stop.second,
                    chain = Chains.byId(activeChain)?.title,
                )
            }
            val single = event ?: return null
            return EventPrompt(single.title, single.flavor, single.first, single.second, chain = null)
        }

    companion object {
        /**
         * Four. Every field added since version one has a default, so an old save still reads —
         * it simply arrives with none of them, which is exactly right for a player who has not
         * earned any yet. The number itself only matters where a value means something different
         * than it used to; see the tier remap in [SaveCodec].
         */
        const val SAVE_VERSION = 4

        fun new(nowMillis: Long): GameState = GameState(
            lastSeenAt = nowMillis,
            startedAt = nowMillis,
        )
    }
}
