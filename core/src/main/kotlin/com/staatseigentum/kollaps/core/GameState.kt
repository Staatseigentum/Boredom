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
) {
    fun ownedOf(collectorId: String): Int = collectors[collectorId] ?: 0

    fun owns(upgradeId: String): Boolean = upgradeId in upgrades

    fun ownsPrestige(upgradeId: String): Boolean = upgradeId in prestigeUpgrades

    /** The buff currently running, or `null` once it has run out. */
    val buff: Buff? get() = if (buffSecondsLeft > 0.0) Buff.byId(buffId) else null

    companion object {
        /**
         * Two: the first version had no prestige upgrades, achievements or buffs. Every field
         * added since has a default, so an old save still reads — it simply arrives with none of
         * them, which is exactly right for a player who has not earned any yet.
         */
        const val SAVE_VERSION = 2

        fun new(nowMillis: Long): GameState = GameState(
            lastSeenAt = nowMillis,
            startedAt = nowMillis,
        )
    }
}
