package com.staatseigentum.kollaps.core

import kotlinx.serialization.Serializable

/**
 * Everything that belongs to one universe rather than to the player.
 *
 * The line is not invented here — it is the one the big bang already draws. That reset keeps what
 * a player has earned across universes (Äonen, Erfolge, Legierungen, schwere Elemente, bestandene
 * Herausforderungen) and drops what belonged to the universe it ended: its mass, its fleet, its
 * singularities, its orbits, its furnaces. Those are exactly the fields below, taken from that
 * list rather than assembled by hand, so the two cannot drift apart.
 *
 * What changes is what happens to them. They used to be dropped; now they are put in the galaxy
 * the universe moves into, and visiting one means taking them back out. A parked universe is no
 * longer a photograph of a finished game — it is the game, paused.
 *
 * Deliberately *not* a [GameState]. A state holds the sky, and the sky holds the galaxies, so a
 * state inside a galaxy inside a state is a structure that contains itself — every save would
 * carry eight copies of every other save. This holds only the half that is a universe.
 */
@Serializable
data class UniverseRun(
    val mass: Double = 0.0,
    val runMass: Double = 0.0,
    val collectors: Map<String, Int> = emptyMap(),
    val roles: Map<String, String> = emptyMap(),
    val upgrades: Set<String> = emptySet(),
    val singularities: Double = 0.0,
    val collapses: Int = 0,
    val celebratedTier: Int = 0,
    val prestigeUpgrades: Set<String> = emptySet(),
    val investments: Map<String, Int> = emptyMap(),
    val runSeconds: Double = 0.0,
    val heat: Double = 0.0,
    val buffId: String? = null,
    val buffSecondsLeft: Double = 0.0,
    val activeChallenge: String? = null,
    val activeChallenges: Set<String> = emptySet(),
    val challengeSeconds: Double = 0.0,
    val autoTapCarry: Double = 0.0,
    val pendingFind: String? = null,
    val findsTapped: Int = 0,
    val pendingEvent: String? = null,
    val activeChain: String? = null,
    val chainStation: String? = null,
    val chainsDone: Set<String> = emptySet(),
    val nextEventSeconds: Double = 0.0,
    val history: List<Double> = emptyList(),
    val historySeconds: Double = 0.0,
    val elements: Map<String, Double> = emptyMap(),
    val fusers: Map<String, Int> = emptyMap(),
    val orbits: Int = 0,
    val satellites: Map<Int, Double> = emptyMap(),
    /**
     * Which kind of universe this is. See [Path].
     *
     * The one field the derivation got wrong on its own. It *appears* in the big bang's
     * constructor, so a mechanical reading of "what the big bang keeps" counted it as the
     * player's — but it appears there because it is being set to the newly *chosen* path, not
     * because the old one is carried over. A universe's lean is as much a part of it as its fleet,
     * and without this a visit put an old universe on under the current universe's alignment.
     */
    val path: String? = null,
) {
    /** Puts this universe back on, leaving everything that belongs to the player alone. */
    fun applyTo(state: GameState): GameState = state.copy(
        mass = mass,
        runMass = runMass,
        collectors = collectors,
        roles = roles,
        upgrades = upgrades,
        singularities = singularities,
        collapses = collapses,
        celebratedTier = celebratedTier,
        prestigeUpgrades = prestigeUpgrades,
        investments = investments,
        runSeconds = runSeconds,
        heat = heat,
        buffId = buffId,
        buffSecondsLeft = buffSecondsLeft,
        activeChallenge = activeChallenge,
        activeChallenges = activeChallenges,
        challengeSeconds = challengeSeconds,
        autoTapCarry = autoTapCarry,
        pendingFind = pendingFind,
        findsTapped = findsTapped,
        pendingEvent = pendingEvent,
        activeChain = activeChain,
        chainStation = chainStation,
        chainsDone = chainsDone,
        nextEventSeconds = nextEventSeconds,
        history = history,
        historySeconds = historySeconds,
        elements = elements,
        fusers = fusers,
        orbits = orbits,
        satellites = satellites,
        path = path,
    )

    /** The deepest rung this universe reached, so its galaxy's record keeps up with it. */
    fun bestTierOf(): Int = Tiers.forMass(runMass, deep = true).index

    companion object {
        /** Takes the universe out of [state], leaving the player behind. */
        fun of(state: GameState): UniverseRun = UniverseRun(
        mass = state.mass,
        runMass = state.runMass,
        collectors = state.collectors,
        roles = state.roles,
        upgrades = state.upgrades,
        singularities = state.singularities,
        collapses = state.collapses,
        celebratedTier = state.celebratedTier,
        prestigeUpgrades = state.prestigeUpgrades,
        investments = state.investments,
        runSeconds = state.runSeconds,
        heat = state.heat,
        buffId = state.buffId,
        buffSecondsLeft = state.buffSecondsLeft,
        activeChallenge = state.activeChallenge,
        activeChallenges = state.activeChallenges,
        challengeSeconds = state.challengeSeconds,
        autoTapCarry = state.autoTapCarry,
        pendingFind = state.pendingFind,
        findsTapped = state.findsTapped,
        pendingEvent = state.pendingEvent,
        activeChain = state.activeChain,
        chainStation = state.chainStation,
        chainsDone = state.chainsDone,
        nextEventSeconds = state.nextEventSeconds,
        history = state.history,
        historySeconds = state.historySeconds,
        elements = state.elements,
        fusers = state.fusers,
        orbits = state.orbits,
            satellites = state.satellites,
            path = state.path,
        )
    }
}
