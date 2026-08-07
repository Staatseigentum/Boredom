package com.staatseigentum.kollaps.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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

    /**
     * How a tier index written by save version 2 maps onto the current ladder.
     *
     * Version 3 inserted seven bodies into the middle of the ladder, so every index above the
     * dwarf planet now means a different thing than it did. Two numbers in the save are raw
     * indices — the best tier ever reached and the highest one already celebrated — and left
     * alone they would quietly demote a player's record and re-run celebrations they have
     * already seen. Everything else about the tier is derived from mass and needs no help.
     */
    private val TIERS_V2_TO_V3 = intArrayOf(
        0, // Meteorit
        1, // Asteroid
        2, // Zwergplanet
        4, // Merkur          (Mond eingefügt)
        6, // Mars            (Titan eingefügt)
        7, // Venus
        8, // Erde
        10, // Neptun         (Supererde eingefügt)
        11, // Uranus
        12, // Saturn
        13, // Jupiter
        15, // Brauner Zwerg  (Heißer Jupiter eingefügt)
        16, // Roter Zwerg
        17, // Sonne
        18, // Blauer Riese
        19, // Roter Überriese
        22, // Neutronenstern (Hyperriese und Weißer Zwerg eingefügt)
        24, // Schwarzes Loch (Magnetar eingefügt)
    )

    /** Brings a save written by an older build up to the current shape. */
    private fun migrate(state: GameState): GameState {
        var migrated = state
        if (migrated.version < 3) {
            migrated = migrated.copy(
                bestTier = remapTier(migrated.bestTier),
                celebratedTier = remapTier(migrated.celebratedTier),
            )
        }
        if (migrated.version < GameState.SAVE_VERSION) {
            migrated = migrated.copy(version = GameState.SAVE_VERSION)
        }
        // Drop references to content that no longer exists, so a renamed collector or a removed
        // upgrade cannot break the save.
        val knownCollectors = migrated.collectors.filterKeys { Collectors.byId(it) != null }
        val knownUpgrades = migrated.upgrades.filterTo(mutableSetOf()) { Upgrades.byId(it) != null }
        val knownPrestige = migrated.prestigeUpgrades.filterTo(mutableSetOf()) {
            PrestigeUpgrades.byId(it) != null
        }
        val knownAchievements = migrated.achievements.filterTo(mutableSetOf()) {
            Achievements.byId(it) != null
        }
        val knownChallenges = migrated.challengesDone.filterTo(mutableSetOf()) {
            Challenge.byId(it) != null
        }
        val knownAeons = migrated.aeonUpgrades.filterTo(mutableSetOf()) {
            AeonUpgrades.byId(it) != null
        }
        if (knownCollectors.size != migrated.collectors.size ||
            knownUpgrades.size != migrated.upgrades.size ||
            knownPrestige.size != migrated.prestigeUpgrades.size ||
            knownAchievements.size != migrated.achievements.size ||
            knownChallenges.size != migrated.challengesDone.size ||
            knownAeons.size != migrated.aeonUpgrades.size
        ) {
            migrated = migrated.copy(
                collectors = knownCollectors,
                upgrades = knownUpgrades,
                prestigeUpgrades = knownPrestige,
                achievements = knownAchievements,
                challengesDone = knownChallenges,
                aeonUpgrades = knownAeons,
            )
        }
        // A challenge that no longer exists would otherwise leave the run stuck under a rule
        // nothing can lift.
        if (migrated.activeChallenge != null && Challenge.byId(migrated.activeChallenge) == null) {
            migrated = migrated.copy(activeChallenge = null, challengeSeconds = 0.0)
        }
        // Same for an event nobody can answer any more.
        if (migrated.pendingEvent != null && CosmicEvent.byId(migrated.pendingEvent) == null) {
            migrated = migrated.copy(pendingEvent = null)
        }
        // A buff that was running when the app closed is not owed to anyone.
        if (migrated.buffSecondsLeft > 0.0) {
            migrated = migrated.copy(buffId = null, buffSecondsLeft = 0.0)
        }
        return migrated
    }

    private fun remapTier(old: Int): Int =
        TIERS_V2_TO_V3.getOrNull(old) ?: old.coerceIn(Tiers.all.indices)

    /**
     * The save as something a player can copy out of the app and paste back in.
     *
     * The game is sideloaded and keeps its save in the app's own directory, so uninstalling —
     * or losing the phone — takes everything with it. Base64 rather than the raw JSON so that a
     * messaging app cannot quietly reformat it, and with a prefix so an obviously wrong paste
     * can be rejected before it is parsed.
     */
    fun export(state: GameState): String = PREFIX + Base64.encode(encode(state))

    /** Reads a block produced by [export]. Returns `null` for anything else. */
    fun import(block: String?): GameState? {
        val trimmed = block?.filterNot { it.isWhitespace() } ?: return null
        if (!trimmed.startsWith(PREFIX)) return null
        val decoded = Base64.decode(trimmed.removePrefix(PREFIX)) ?: return null
        if (!looksLikeSave(decoded)) return null
        return decode(decoded)
    }

    /**
     * Whether the text is plausibly one of our saves.
     *
     * Needed because every field of [GameState] has a default, so the parser happily turns any
     * JSON object at all — `{"nope":1}` included — into a brand new game. On import that is the
     * worst possible outcome: a mistyped paste would replace a real save with an empty one. A
     * save has to carry its own version number to be accepted.
     */
    private fun looksLikeSave(text: String): Boolean {
        val version = try {
            json.parseToJsonElement(text)
                .let { it as? JsonObject }
                ?.get("version")
                ?.let { it as? JsonPrimitive }
                ?.content
                ?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
        return version != null && version in 1..GameState.SAVE_VERSION
    }

    /** Marks the block as ours and as version one of the format. */
    private const val PREFIX = "KOLLAPS1:"
}

/**
 * Base64, written out rather than taken from a platform.
 *
 * `java.util.Base64` would be the obvious choice and is the reason this exists: the whole of
 * [SaveCodec] is meant to stay free of the JVM so the rules can be moved elsewhere later.
 */
internal object Base64 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(text: String): String {
        val bytes = text.encodeToByteArray()
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index < bytes.size) {
            val b0 = bytes[index].toInt() and 0xFF
            val b1 = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xFF else 0
            val b2 = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else 0
            val chunk = (b0 shl 16) or (b1 shl 8) or b2

            out.append(ALPHABET[chunk ushr 18 and 0x3F])
            out.append(ALPHABET[chunk ushr 12 and 0x3F])
            out.append(if (index + 1 < bytes.size) ALPHABET[chunk ushr 6 and 0x3F] else '=')
            out.append(if (index + 2 < bytes.size) ALPHABET[chunk and 0x3F] else '=')
            index += 3
        }
        return out.toString()
    }

    fun decode(encoded: String): String? {
        val body = encoded.trimEnd('=')
        val bytes = ArrayList<Byte>(body.length * 3 / 4 + 3)
        var buffer = 0
        var bits = 0
        for (character in body) {
            val value = ALPHABET.indexOf(character)
            if (value < 0) return null
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                bytes.add(((buffer ushr bits) and 0xFF).toByte())
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
