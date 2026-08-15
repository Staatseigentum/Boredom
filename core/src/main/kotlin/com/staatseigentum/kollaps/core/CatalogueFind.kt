package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang

/**
 * Something worth stopping for, found while climbing the catalogue ladder.
 *
 * The ladder above the black hole is sixteen thousand rungs of the same loop with larger numbers.
 * That is a long time for nothing new to happen, and "long" is not the same as "deep" — a stretch
 * of game where the only thing that changes is the exponent is a stretch nobody remembers playing.
 *
 * So every [EVERY_RUNGS] rungs the survey turns something up, and it asks the one question the rest
 * of the game does not: what is this *for*. The three answers pay in three different currencies and
 * none of them is best.
 *
 * - **Auswerten** pays Äonen, which is the deep shelf and the slowest thing in the game to fill.
 * - **Anzapfen** pays a multiplier that lasts as long as this run does — enormous right now,
 *   nothing at all after the next collapse.
 * - **In Ruhe lassen** pays nothing and records a fragment. It is a real answer and it is meant to
 *   be: a game that punishes curiosity with nothing but arithmetic has taught the player not to
 *   read anything.
 *
 * The choice is genuinely open because the three currencies are wanted at different moments. A run
 * that is about to end wants Äonen; one that just started wants the multiplier.
 */
data class CatalogueFind(
    val id: String,
    val germanTitle: String,
    val germanFlavor: String,
    val germanFragment: String,
) {
    val title: String get() = Lang.t(germanTitle)

    val flavor: String get() = Lang.t(germanFlavor)

    val fragment: String get() = Lang.t(germanFragment)

    companion object {

        /** Rungs of the catalogue ladder between two finds. */
        const val EVERY_RUNGS = 50

        /** Äonen for evaluating one. Flat, because Äonen are flat everywhere else. */
        const val AEON_REWARD = 1.0

        /** What tapping one is worth to the rest of the run. */
        const val TAP_BONUS = 0.35

        /**
         * The most a run's tapped finds can be worth together.
         *
         * A run held far enough up the ladder passes hundreds of these, and an uncapped stack of
         * multipliers would make sitting still the whole strategy — which is the exact failure the
         * ladder's own logarithm exists to avoid. Ten is a lot and it is reachable, which is the
         * point: the twenty-ninth find is worth stopping for and the sixtieth is not.
         */
        const val TAP_CAP = 10.0

        val all: List<CatalogueFind> = listOf(
            CatalogueFind(
                "find_signal",
                "Ein Signal ohne Absender",
                "Eine Wiederholung, sauber und regelmäßig, aus einer Richtung ohne Sterne.",
                "Es wiederholt sich alle elf Sekunden. Elf ist keine Zahl, die zufällig entsteht.",
            ),
            CatalogueFind(
                "find_shell",
                "Eine leere Hülle",
                "Ein Körper mit einer Kruste und ohne Inneres. Etwas hat ihn von innen ausgeräumt.",
                "Die Kruste hält. Wer auch immer das getan hat, war ordentlich dabei.",
            ),
            CatalogueFind(
                "find_pair",
                "Zwei, die sich umkreisen",
                "Zwei identische Körper, gleiche Masse, gleiche Zusammensetzung, gleiche Farbe.",
                "Identisch bis auf die Nachkommastelle. So etwas entsteht nicht, so etwas wird geteilt.",
            ),
            CatalogueFind(
                "find_cold",
                "Ein kalter Fleck",
                "Ein Gebiet, in dem die Hintergrundstrahlung fehlt. Nicht schwächer — sie fehlt.",
                "Dahinter ist nichts. Nicht Dunkelheit, sondern die Abwesenheit von Dahinter.",
            ),
            CatalogueFind(
                "find_slow",
                "Etwas, das zu langsam fällt",
                "Ein Brocken auf einer Bahn, die er bei seiner Masse nicht halten dürfte.",
                "Entweder ist die Masse falsch oder die Gravitation. Beides wäre unangenehm.",
            ),
            CatalogueFind(
                "find_mirror",
                "Ein Katalogeintrag, den es schon gibt",
                "Dieselbe Kennung, dieselben Werte, zweimal vergeben. Einer davon ist neu.",
                "Der Katalog hat sich nicht geirrt. Es sind zwei.",
            ),
            CatalogueFind(
                "find_old",
                "Älter als es sein dürfte",
                "Die Zusammensetzung passt zu einem Universum, das es noch nicht gegeben hat.",
                "Es ist nicht von hier. Es ist von vorher.",
            ),
            CatalogueFind(
                "find_quiet",
                "Der stille Bereich",
                "Vierhundert Sprossen ohne einen einzigen Fund. Statistisch beinahe unmöglich.",
                "Nichts zu finden ist auch ein Befund. Jemand hat hier aufgeräumt.",
            ),
        )

        fun byId(id: String?): CatalogueFind? = all.firstOrNull { it.id == id }

        /** Whether finds happen at all yet. They belong to the ladder, so they need the ladder. */
        fun isUnlocked(state: GameState): Boolean = Designations.isUnlocked(state)

        /**
         * How many finds this run has earned by climbing.
         *
         * Read off the rung rather than counted by a timer, so a run that is pushed hard turns up
         * more of them than one left standing — the finds are a reward for climbing, and a clock
         * would have paid them for waiting.
         */
        fun earnedBy(state: GameState): Int {
            val rungs = GameEngine.tierOf(state).index - Tiers.last.index
            return if (rungs < EVERY_RUNGS) 0 else rungs / EVERY_RUNGS
        }

        /** Whether a find is waiting to be answered right now. */
        fun isDue(state: GameState): Boolean =
            isUnlocked(state) && state.pendingFind == null && earnedBy(state) > state.findsAnswered

        /**
         * Which find turns up next.
         *
         * Derived from how many have been answered rather than drawn at random, for the same reason
         * the event chains are: the rules have no random source, and a save that is reloaded must
         * not be able to re-roll a question into a better one.
         */
        fun next(state: GameState): CatalogueFind = all[state.findsAnswered.mod(all.size)]

        /** What the tapped finds of this run are worth together. */
        fun tapMultiplier(state: GameState): Double =
            (1.0 + TAP_BONUS * state.findsTapped).coerceAtMost(TAP_CAP)
    }
}

/** What the player did with a find. */
enum class FindAnswer(val id: String, val germanLabel: String, val germanFlavor: String) {
    AUSWERTEN(
        id = "find_study",
        germanLabel = "Auswerten",
        germanFlavor = "Zerlegen, vermessen, aufschreiben. Zahlt ein Äon.",
    ),
    ANZAPFEN(
        id = "find_tap",
        germanLabel = "Anzapfen",
        germanFlavor = "Nehmen, was drin ist. Wirkt bis zum nächsten Kollaps und nicht darüber hinaus.",
    ),
    RUHEN(
        id = "find_leave",
        germanLabel = "In Ruhe lassen",
        germanFlavor = "Notieren und weiterziehen. Bringt nichts außer dem Eintrag.",
    ),
    ;

    val label: String get() = Lang.t(germanLabel)

    val flavor: String get() = Lang.t(germanFlavor)

    companion object {
        fun byId(id: String?): FindAnswer? = entries.firstOrNull { it.id == id }
    }
}
