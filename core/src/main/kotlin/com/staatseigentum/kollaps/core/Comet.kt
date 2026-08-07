package com.staatseigentum.kollaps.core

import kotlin.random.Random

/**
 * A timed effect won by catching a comet.
 *
 * Buffs are the only part of the game that runs on a clock rather than on a counter, which is
 * exactly why they are worth having: everything else can be left alone, so nothing rewards being
 * at the screen. A buff does.
 */
enum class Buff(
    val id: String,
    val label: String,
    val seconds: Double,
    /** How much it multiplies, so the engine never has to look up which comet granted it. */
    val factor: Double,
) {
    /** Multiplies everything, so it is worth spending a windfall right before catching one. */
    SURGE("surge", "Schub", 30.0, 7.0),

    /** Makes tapping matter again, briefly, however far into the run you are. */
    FRENZY("frenzy", "Klickrausch", 60.0, 100.0),
    ;

    companion object {
        fun byId(id: String?): Buff? = entries.firstOrNull { it.id == id }
    }
}

/** What catching a comet is worth. */
sealed interface CometReward {
    /** Mass equal to this many seconds of current production, paid at once. */
    data class Windfall(val secondsOfProduction: Double) : CometReward

    /** A buff for its own duration. */
    data class Timed(val buff: Buff) : CometReward
}

/**
 * The comets that drift through the starfield.
 *
 * Weights rather than equal chances: the windfall is the least interesting of the three, so it
 * is the common one, and the frenzy — which can carry a whole tier on its own — is rare.
 */
enum class Comet(
    val id: String,
    val title: String,
    val flavor: String,
    val weight: Int,
    val reward: CometReward,
) {
    WINDFALL(
        id = "windfall",
        title = "Brocken",
        flavor = "Fünfzehn Minuten Arbeit, auf einen Schlag.",
        weight = 5,
        reward = CometReward.Windfall(15 * 60.0),
    ),
    SURGE(
        id = "surge",
        title = "Sternwind",
        flavor = "Alles läuft eine halbe Minute lang siebenfach.",
        weight = 4,
        reward = CometReward.Timed(Buff.SURGE),
    ),
    FRENZY(
        id = "frenzy",
        title = "Splitterregen",
        flavor = "Eine Minute lang zählt jeder Tipp hundertfach.",
        weight = 2,
        reward = CometReward.Timed(Buff.FRENZY),
    ),
    ;

    companion object {
        fun byId(id: String?): Comet? = entries.firstOrNull { it.id == id }
    }
}

/**
 * When comets appear and which one it is.
 *
 * Kept out of the interface on purpose: the schedule is a rule of the game, so it belongs where
 * the rest of the rules are and can be tested without a screen.
 */
object Comets {

    /** Shortest and longest wait between two comets, before any prestige upgrade. */
    const val MIN_SECONDS = 150.0
    const val MAX_SECONDS = 330.0

    /** How long one stays catchable. Long enough to notice, short enough to miss. */
    const val VISIBLE_SECONDS = 11.0

    /** Nothing appears until the player is past the first couple of tiers. */
    const val FIRST_TIER = 2

    /** Seconds until the next comet, given how much more often prestige makes them come. */
    fun nextDelay(random: Random, frequency: Double = 1.0): Double {
        val span = MAX_SECONDS - MIN_SECONDS
        return (MIN_SECONDS + random.nextDouble() * span) / frequency.coerceAtLeast(0.1)
    }

    /** Picks one, honouring the weights. */
    fun pick(random: Random): Comet {
        val total = Comet.entries.sumOf { it.weight }
        var roll = random.nextInt(total)
        for (comet in Comet.entries) {
            roll -= comet.weight
            if (roll < 0) return comet
        }
        return Comet.WINDFALL
    }

    /** Comets only start once there is production worth multiplying. */
    fun appearsAt(state: GameState): Boolean = Tiers.forMass(state.runMass).index >= FIRST_TIER
}
