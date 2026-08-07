package com.staatseigentum.kollaps.core

/** One of the two things an event lets the player pick. */
data class EventOption(val label: String, val flavor: String, val reward: CometReward) {
    val rewardText: String
        get() = when (reward) {
            is CometReward.Windfall ->
                "${Numbers.formatDuration(reward.secondsOfProduction.toLong())} Produktion sofort"

            is CometReward.Timed ->
                "${reward.buff.label} für ${Numbers.formatDuration(reward.buff.seconds.toLong())}"
        }
}

/**
 * Something that happens every few minutes and asks a question.
 *
 * Comets are about reaction: they cross the screen and are caught or missed. This is the other
 * half — it waits, and the interesting part is which of the two you take. Both options are worth
 * having, and which one is worth more depends on whether the player is about to put the phone
 * down or about to sit and tap: mass in hand suits the first, a timed buff the second.
 */
enum class CosmicEvent(
    val id: String,
    val title: String,
    val flavor: String,
    val first: EventOption,
    val second: EventOption,
) {
    SONNENSTURM(
        id = "e_sturm",
        title = "Sonnensturm",
        flavor = "Eine Plasmawolke rollt heran. Du kannst sie einfangen oder in ihr surfen.",
        first = EventOption(
            "Einfangen",
            "Die Ladung geht direkt in die Speicher.",
            CometReward.Windfall(10 * 60.0),
        ),
        second = EventOption(
            "Mitreiten",
            "Alles läuft heiß, solange der Sturm anhält.",
            CometReward.Timed(Buff.SURGE),
        ),
    ),
    TRUEMMERFELD(
        id = "e_truemmer",
        title = "Trümmerfeld",
        flavor = "Reste von etwas Großem, das hier einmal vorbeikam.",
        first = EventOption(
            "Absammeln",
            "Langsam, gründlich, und die Ausbeute ist beträchtlich.",
            CometReward.Windfall(20 * 60.0),
        ),
        second = EventOption(
            "Durchpflügen",
            "Jeder Griff trifft etwas. Für kurze Zeit.",
            CometReward.Timed(Buff.FRENZY),
        ),
    ),
    LINSE(
        id = "e_linse",
        title = "Gravitationslinse",
        flavor = "Für ein paar Minuten steht etwas Schweres genau richtig.",
        first = EventOption(
            "Durchleiten",
            "Die gebündelte Materie fällt dir in den Schoß.",
            CometReward.Windfall(30 * 60.0),
        ),
        second = EventOption(
            "Fokussieren",
            "Die ganze Anlage arbeitet durch die Linse.",
            CometReward.Timed(Buff.SURGE),
        ),
    ),
    STILLE(
        id = "e_stille",
        title = "Stille",
        flavor = "Nichts passiert. Das ist selten genug, um es zu nutzen.",
        first = EventOption(
            "Aufräumen",
            "Ein Rest, den bisher niemand eingesammelt hat.",
            CometReward.Windfall(5 * 60.0),
        ),
        second = EventOption(
            "Konzentrieren",
            "Ohne Ablenkung sitzt jeder Griff.",
            CometReward.Timed(Buff.FRENZY),
        ),
    ),
    ;

    fun optionAt(index: Int): EventOption? = when (index) {
        0 -> first
        1 -> second
        else -> null
    }

    companion object {
        /** Seconds of play between one event and the next. */
        const val MIN_SECONDS = 240.0
        const val MAX_SECONDS = 420.0

        /** Not before this body, so the first minutes stay about the ladder. */
        const val FIRST_TIER_NAME = "Erde"

        fun byId(id: String?): CosmicEvent? = entries.firstOrNull { it.id == id }

        fun appearsAt(state: GameState): Boolean =
            Tiers.forMass(state.runMass).index >= Tiers.indexOf(FIRST_TIER_NAME)

        /**
         * Which event is next, chosen from the play time rather than from a random source.
         *
         * The engine is a pure function of the state and has no clock and no generator of its
         * own; deriving the pick from a number the save already carries keeps it that way, and
         * keeps a reload from being a way to reroll for a better event.
         */
        fun pick(state: GameState): CosmicEvent =
            entries[(state.playedSeconds.toLong() / 7 + state.taps).mod(entries.size.toLong()).toInt()]
    }
}
