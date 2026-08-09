package com.staatseigentum.kollaps.core

/**
 * One stop in a chain: a situation and the two ways out of it.
 *
 * Reusing [EventOption] rather than inventing a second kind of answer. The two look identical to
 * the player — a label, a sentence, and what it pays — and the only thing a chain adds is that an
 * answer also decides where the story goes next, which lives in [EventOption.next].
 */
data class ChainStation(
    val id: String,
    val title: String,
    val flavor: String,
    val first: EventOption,
    val second: EventOption,
) {
    fun optionAt(index: Int): EventOption? = when (index) {
        0 -> first
        1 -> second
        else -> null
    }
}

/**
 * A story told across several events.
 *
 * A single event asks one question and forgets it. This asks three in a row, and the first answer
 * decides which second question exists at all — so two players who both saw "Das Signal" through
 * to the end did not read the same thing. That is the whole reason it is worth having: the
 * branching is the content, and text costs nothing to ship.
 *
 * The stations are held in a flat list keyed by id rather than nested, because the shape is a tree
 * and a tree written as nested constructors is unreadable by the third level. Ids also survive a
 * save: what a save has to remember is "which station", and an id is the only stable name for it.
 */
data class EventChain(
    val id: String,
    val title: String,
    /** Not before this body — the later chains lean on things the early game has not met yet. */
    val unlockTier: String,
    val start: String,
    val stations: List<ChainStation>,
) {
    fun station(id: String?): ChainStation? = stations.firstOrNull { it.id == id }

    /** How many answers it takes to reach the end, on the longest way through. */
    val depth: Int
        get() {
            fun walk(from: String?, seen: Set<String>): Int {
                val station = station(from) ?: return 0
                if (station.id in seen) return 0
                val onwards = seen + station.id
                val a = walk(station.first.next, onwards)
                val b = walk(station.second.next, onwards)
                return 1 + maxOf(a, b)
            }
            return walk(start, emptySet())
        }
}

object Chains {

    /**
     * Something is out there and it is aware of you.
     *
     * The first branch is the one that matters: listening keeps it a thing you study, answering
     * makes it a thing that studies back, and nothing after that is shared between the two.
     */
    private val SIGNAL = EventChain(
        id = "chain_signal",
        title = "Das Signal",
        unlockTier = "Erde",
        start = "sig_1",
        stations = listOf(
            ChainStation(
                id = "sig_1",
                title = "Ein Muster im Rauschen",
                flavor = "Zwischen zwei Frequenzen liegt etwas, das sich alle elf Sekunden " +
                    "wiederholt. Rauschen macht das nicht.",
                first = EventOption(
                    "Zuhören",
                    "Die Antennen bleiben, wo sie sind. Man verrät nichts, indem man wartet.",
                    CometReward.Windfall(6 * 60.0),
                    next = "sig_hoeren",
                ),
                second = EventOption(
                    "Antworten",
                    "Dasselbe Muster zurück, einmal. Mehr braucht es nicht, um gesehen zu werden.",
                    CometReward.Timed(Buff.SURGE),
                    next = "sig_antworten",
                ),
            ),
            ChainStation(
                id = "sig_hoeren",
                title = "Es wiederholt sich",
                flavor = "Vier Tage lang derselbe Abstand, dieselbe Länge. Und dann, einmal, " +
                    "ein Abstand zu viel.",
                first = EventOption(
                    "Aufzeichnen",
                    "Alles mitschneiden und später verstehen. Wenn es ein Später gibt.",
                    CometReward.Windfall(15 * 60.0),
                    next = "sig_archiv",
                ),
                second = EventOption(
                    "Abschalten",
                    "Manche Dinge hören auf, wenn man aufhört hinzusehen. Manche nicht.",
                    CometReward.Windfall(25 * 60.0),
                ),
            ),
            ChainStation(
                id = "sig_antworten",
                title = "Etwas antwortet",
                flavor = "Dasselbe Muster kommt zurück — aber schneller, als Licht die Strecke " +
                    "schafft. Es hat nicht auf dich gewartet. Es war schon näher.",
                first = EventOption(
                    "Weitersenden",
                    "Wenn es ohnehin kommt, ist es besser, es kommt als etwas Erwartetes.",
                    CometReward.Timed(Buff.FRENZY),
                    next = "sig_naeher",
                ),
                second = EventOption(
                    "Funkstille",
                    "Alle Sender aus. Die Anlage steht still, und die Speicher füllen sich.",
                    CometReward.Windfall(30 * 60.0),
                ),
            ),
            ChainStation(
                id = "sig_archiv",
                title = "Das Archiv",
                flavor = "Die Aufzeichnung ist vollständig. Sie beschreibt, in einer Sprache " +
                    "aus Abständen, den Aufbau von etwas sehr Schwerem.",
                first = EventOption(
                    "Nachbauen",
                    "Es steht alles da. Es steht sogar da, in welcher Reihenfolge.",
                    CometReward.Timed(Buff.INFERNO),
                ),
                second = EventOption(
                    "Wegschließen",
                    "Man muss nicht jede Anleitung befolgen, die man geschenkt bekommt.",
                    CometReward.Windfall(45 * 60.0),
                ),
            ),
            ChainStation(
                id = "sig_naeher",
                title = "Es kommt näher",
                flavor = "Kein Objekt auf keinem Radar. Nur das Muster, jeden Tag lauter, und " +
                    "die Instrumente, die schwerer werden, als sie sein dürften.",
                first = EventOption(
                    "Entgegengehen",
                    "Alles, was fliegt, in eine Richtung. Was auch immer das wird, es wird schnell.",
                    CometReward.Timed(Buff.INFERNO),
                ),
                second = EventOption(
                    "Abwarten",
                    "Es kommt so oder so. Bis dahin läuft die Produktion weiter.",
                    CometReward.Windfall(50 * 60.0),
                ),
            ),
        ),
    )

    /**
     * Something alive turned up on a rock, and every answer is about what you owe it.
     *
     * Deliberately the chain with no clean option: taking them apart pays, leaving them alone
     * pays, and the game does not say which one you should have picked.
     */
    private val PASSAGIERE = EventChain(
        id = "chain_passagiere",
        title = "Die Passagiere",
        unlockTier = "Saturn",
        start = "pas_1",
        stations = listOf(
            ChainStation(
                id = "pas_1",
                title = "Nicht allein angekommen",
                flavor = "In den Rissen des Brockens sitzt etwas, das Wärme abgibt. Wenig, " +
                    "aber regelmäßig.",
                first = EventOption(
                    "Einsammeln",
                    "Behälter, Etiketten, Katalognummern. So macht man das.",
                    CometReward.Windfall(8 * 60.0),
                    next = "pas_labor",
                ),
                second = EventOption(
                    "In Ruhe lassen",
                    "Der Brocken kommt auf ein eigenes Feld, und das Feld bekommt einen Zaun.",
                    CometReward.Timed(Buff.SURGE),
                    next = "pas_feld",
                ),
            ),
            ChainStation(
                id = "pas_labor",
                title = "Im Labor",
                flavor = "Sie halten Vakuum aus, Kälte, harte Strahlung. Was sie nicht aushalten, " +
                    "ist, einzeln zu sein.",
                first = EventOption(
                    "Aufschließen",
                    "Was drin ist, ist mehr wert als das, was sie tun. Vermutlich.",
                    CometReward.Windfall(35 * 60.0),
                ),
                second = EventOption(
                    "Zurückbringen",
                    "Dahin, wo sie herkamen, samt der Wärme, die sie brauchen.",
                    CometReward.Timed(Buff.FRENZY),
                ),
            ),
            ChainStation(
                id = "pas_feld",
                title = "Das Feld",
                flavor = "Sie sind mehr geworden. Nicht schnell — aber sie haben angefangen, " +
                    "sich am Zaun entlang anzuordnen.",
                first = EventOption(
                    "Zusehen",
                    "Wer zusieht, lernt. Wer eingreift, bekommt etwas anderes zu sehen.",
                    CometReward.Windfall(40 * 60.0),
                ),
                second = EventOption(
                    "Umsiedeln",
                    "Ein größeres Feld, weiter draußen. Es ist genug Platz da.",
                    CometReward.Timed(Buff.INFERNO),
                ),
            ),
        ),
    )

    /**
     * A structure that was here first, and the question of whether to move in.
     */
    private val BAU = EventChain(
        id = "chain_bau",
        title = "Der Bau",
        unlockTier = "Sonne",
        start = "bau_1",
        stations = listOf(
            ChainStation(
                id = "bau_1",
                title = "Eine Schale",
                flavor = "Etwas umschließt den Stern zu zwei Dritteln. Es ist alt, es ist leer, " +
                    "und es ist nicht abgestürzt.",
                first = EventOption(
                    "Betreten",
                    "Die Schleusen öffnen sich, als hätten sie darauf gewartet.",
                    CometReward.Timed(Buff.SURGE),
                    next = "bau_innen",
                ),
                second = EventOption(
                    "Abtragen",
                    "Was da hängt, ist Material. Sehr viel Material.",
                    CometReward.Windfall(60 * 60.0),
                    next = "bau_abbau",
                ),
            ),
            ChainStation(
                id = "bau_innen",
                title = "Innen",
                flavor = "Gänge für etwas, das größer war als du und dieselbe Schwerkraft mochte. " +
                    "Kein Staub. Irgendwer hält hier sauber.",
                first = EventOption(
                    "Weitergehen",
                    "Bis dahin, wo die Gänge aufhören, sich zu wiederholen.",
                    CometReward.Timed(Buff.INFERNO),
                ),
                second = EventOption(
                    "Einziehen",
                    "Die Anlage passt hinein. Sie passt sogar auffällig gut hinein.",
                    CometReward.Windfall(90 * 60.0),
                ),
            ),
            ChainStation(
                id = "bau_abbau",
                title = "Der Abbau",
                flavor = "Das dritte Drittel fehlte nicht. Es war abgetragen worden, von jemandem, " +
                    "der genauso angefangen hat wie du.",
                first = EventOption(
                    "Weitermachen",
                    "Der Vorgänger hat aufgehört. Das muss keinen Grund gehabt haben.",
                    CometReward.Windfall(120 * 60.0),
                ),
                second = EventOption(
                    "Stehen lassen",
                    "Was übrig ist, bleibt, wo es ist. Es steht dort länger als jede Anlage.",
                    CometReward.Timed(Buff.INFERNO),
                ),
            ),
        ),
    )

    val all: List<EventChain> = listOf(SIGNAL, PASSAGIERE, BAU)

    fun byId(id: String?): EventChain? = all.firstOrNull { it.id == id }

    /** The station a state is waiting on, or `null` if what is on the table is a one-off event. */
    fun stationOf(state: GameState): ChainStation? {
        val pending = state.pendingEvent ?: return null
        return byId(state.activeChain)?.station(pending)
    }

    /**
     * The next chain worth starting, or `null`.
     *
     * In list order rather than at random, so the story the player meets first is the one written
     * for the earliest body. Chains already finished never come back: a branch whose ending is
     * known is a menu, not a decision.
     */
    fun startable(state: GameState): EventChain? {
        val reached = Tiers.forMass(state.runMass).index
        return all.firstOrNull { chain ->
            chain.id !in state.chainsDone && reached >= Tiers.indexOf(chain.unlockTier)
        }
    }

    /** How many stories the player has seen through, for the chronicle. */
    fun doneCount(state: GameState): Int = state.chainsDone.count { byId(it) != null }

    val total: Int get() = all.size
}
