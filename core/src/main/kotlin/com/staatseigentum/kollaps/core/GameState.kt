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
) {
    fun ownedOf(collectorId: String): Int = collectors[collectorId] ?: 0

    fun owns(upgradeId: String): Boolean = upgradeId in upgrades

    companion object {
        const val SAVE_VERSION = 1

        fun new(nowMillis: Long): GameState = GameState(
            lastSeenAt = nowMillis,
            startedAt = nowMillis,
        )
    }
}
