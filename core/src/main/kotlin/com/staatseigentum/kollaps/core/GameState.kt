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

    /** Achievements earned, across all runs. */
    val achievements: Set<String> = emptySet(),

    /** Seconds the game was actually on screen, for the statistics. */
    val playedSeconds: Double = 0.0,

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

    /** Whether the automatic buyer is switched on. Off by default even once it is unlocked. */
    val autoBuyOn: Boolean = false,

    /** Whether the game may remind the player that the collectors have filled up. */
    val remindersOn: Boolean = true,

    /** Id of the event waiting for an answer, if any. */
    val pendingEvent: String? = null,

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

    /** The event waiting for an answer, or `null`. */
    val event: CosmicEvent? get() = CosmicEvent.byId(pendingEvent)

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
