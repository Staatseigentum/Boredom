package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
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
    val germanLabel: String,
    val seconds: Double,
    /** How much it multiplies, so the engine never has to look up which comet granted it. */
    val factor: Double,
) {
    /** Multiplies everything, so it is worth spending a windfall right before catching one. */
    SURGE("surge", "Schub", 30.0, 7.0),

    /** Makes tapping matter again, briefly, however far into the run you are. */
    FRENZY("frenzy", "Klickrausch", 60.0, 100.0),

    /** The one worth breaking a hard core open for: longer than a surge and twice as strong. */
    INFERNO("inferno", "Feuersturm", 90.0, 15.0),
    ;

    val label: String get() = Lang.t(germanLabel)

    companion object {
        fun byId(id: String?): Buff? = entries.firstOrNull { it.id == id }
    }
}

/** What catching a comet is worth. */
sealed interface CometReward {

    companion object {
        /**
         * The most instant production any one payout may hand over, in seconds.
         *
         * Three quarters of an hour, and the same number as the hardest comet in the game — the
         * one that has to be hit three times while it crosses the screen. That is the point of
         * the ceiling: the best thing a windfall can do is match the best thing skill can do.
         *
         * The end of an event chain used to pay two hours, which is most of a run handed over for
         * picking the second option four times. A reward that large stops being a reward and
         * starts being the game skipping itself.
         */
        const val MAX_WINDFALL_SECONDS = 45 * 60.0
    }

    /** Mass equal to this many seconds of current production, paid at once. */
    data class Windfall(private val seconds: Double) : CometReward {
        /**
         * Clamped here rather than at each of the three places that pay it out.
         *
         * A catalogue is a list of numbers somebody will add to, and a ceiling that lives at the
         * point of payment is one every future entry has to remember. Held at the point of
         * *description*, the card and the credit can never disagree about what was promised.
         */
        val secondsOfProduction: Double get() = seconds.coerceAtMost(MAX_WINDFALL_SECONDS)
    }

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
    val germanTitle: String,
    val germanFlavor: String,
    val weight: Int,
    val reward: CometReward,
    /**
     * How many taps it takes to break open.
     *
     * One for the three that always existed. More is the only thing here that asks for anything
     * beyond noticing: a hard core has to be hit again while it is still moving, which is a
     * different skill from spotting it, and pays accordingly.
     */
    val hits: Int = 1,
    /**
     * What breaking it open spills, per material, before the crust's bonus.
     *
     * Empty for the three that cross whole, which is the point rather than an omission: a comet
     * you can take with one tap pays in production, and the ones that pay in *material* are the
     * ones with a core to crack. Material is what the body is built out of and a collapse takes
     * all of it, so a source that arrived every four minutes for one tap would make the deepest
     * world types a formality. These arrive every quarter of an hour or so, and have to be hit
     * two or three times while they are still moving.
     *
     * Each carries what it is made of, with a lesser second so that no shell can ever be blocked
     * on a material only the other core brings.
     */
    val carries: Map<Material, Double> = emptyMap(),
) {
    WINDFALL(
        id = "windfall",
        germanTitle = "Brocken",
        germanFlavor = "Fünfzehn Minuten Arbeit, auf einen Schlag.",
        weight = 5,
        reward = CometReward.Windfall(15 * 60.0),
    ),
    SURGE(
        id = "surge",
        germanTitle = "Sternwind",
        germanFlavor = "Alles läuft eine halbe Minute lang siebenfach.",
        weight = 4,
        reward = CometReward.Timed(Buff.SURGE),
    ),
    FRENZY(
        id = "frenzy",
        germanTitle = "Splitterregen",
        germanFlavor = "Eine Minute lang zählt jeder Tipp hundertfach. Ein paar Splitter bleiben liegen.",
        weight = 2,
        reward = CometReward.Timed(Buff.FRENZY),
        // The name was always a promise of debris. Small, because it costs one tap like the two
        // above it — enough that catching one is never *only* a buff.
        carries = mapOf(Material.SILIKAT to 4.0, Material.KOHLENSTOFF to 4.0),
    ),
    ICE_CORE(
        id = "ice",
        germanTitle = "Eiskern",
        germanFlavor = "Drei Treffer, bis die Kruste bricht. Darunter eine dreiviertel Stunde Arbeit — und Eis.",
        weight = 2,
        reward = CometReward.Windfall(45 * 60.0),
        hits = 3,
        carries = mapOf(Material.EIS to 30.0, Material.SILIKAT to 10.0),
    ),
    EMBER_CORE(
        id = "ember",
        germanTitle = "Glutkern",
        germanFlavor = "Zwei Treffer, und danach brennt anderthalb Minuten lang alles fünfzehnfach. Der Kern bleibt.",
        weight = 1,
        reward = CometReward.Timed(Buff.INFERNO),
        hits = 2,
        carries = mapOf(Material.METALL to 30.0, Material.KOHLENSTOFF to 10.0),
    ),
    ;

    val title: String get() = Lang.t(germanTitle)

    val flavor: String get() = Lang.t(germanFlavor)

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
