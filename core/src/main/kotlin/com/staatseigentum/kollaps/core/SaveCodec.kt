package com.staatseigentum.kollaps.core

import kotlinx.serialization.json.Json

/**
 * Turns a [GameState] into the string that gets persisted and back.
 *
 * Decoding never throws: a corrupt or unreadable save yields `null` so the caller can decide
 * between starting over and keeping what is in memory. Unknown keys are ignored so that saves
 * written by a newer build still load.
 */
object SaveCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: GameState): String = json.encodeToString(GameState.serializer(), state)

    fun decode(raw: String?): GameState? {
        if (raw.isNullOrBlank()) return null
        return try {
            migrate(json.decodeFromString(GameState.serializer(), raw))
        } catch (_: Exception) {
            null
        }
    }

    /** Brings a save written by an older build up to the current shape. */
    private fun migrate(state: GameState): GameState {
        var migrated = state
        if (migrated.version < GameState.SAVE_VERSION) {
            migrated = migrated.copy(version = GameState.SAVE_VERSION)
        }
        // Drop references to content that no longer exists, so a renamed collector or a removed
        // upgrade cannot break the save.
        val knownCollectors = migrated.collectors.filterKeys { Collectors.byId(it) != null }
        val knownUpgrades = migrated.upgrades.filterTo(mutableSetOf()) { Upgrades.byId(it) != null }
        if (knownCollectors.size != migrated.collectors.size ||
            knownUpgrades.size != migrated.upgrades.size
        ) {
            migrated = migrated.copy(collectors = knownCollectors, upgrades = knownUpgrades)
        }
        return migrated
    }
}
