package com.staatseigentum.kollaps.core

/**
 * One system, the moment it first exists, said in three sentences.
 *
 * [where] is not decoration. A new panel appearing somewhere in a shop with four tabs and six
 * sections is a thing a player finds by accident a quarter of an hour later, and the whole reason
 * these exist is that the game had no way of saying "the lab is now a thing, and it is *there*".
 */
data class FeatureIntro(
    val id: String,
    val title: String,
    /** Where to go to find it, in the words the interface itself uses. */
    val where: String,
    /** What it is, and — more useful — what it is *for*. */
    val text: String,
    val appearsWhen: (GameState) -> Boolean,
)

/**
 * What to say when something new turns up.
 *
 * The game unlocks eleven systems over a run and a half and used to announce none of them. A tab
 * simply appeared. That is a strange way to treat the best moments a game like this has: the
 * unlock *is* the reward, and one that arrives silently is one a player has to notice on their own
 * before it can feel like anything.
 *
 * ## Why this is a list and not a step in the tutorial
 *
 * The tutorial is a sequence — it has a first step and a last one and it ends. These do not: they
 * are spread over hours and arrive in an order that depends on how somebody plays. Orbits before
 * fusion, or the other way round, are both ordinary. So each carries its own condition and the
 * game shows whichever has come true and not yet been read.
 *
 * ## The order in the list
 *
 * Roughly the order they come up in, which matters only when two unlock in the same tick — then
 * the earlier one is shown first and the other waits for the next frame. That happens in practice
 * after a big bang, when several things arrive at once.
 */
object Unlocks {

    val all: List<FeatureIntro> = listOf(
        /*
         * The two earliest cards in the game, and the only two that are hung on something the
         * player *did* rather than on a system unlocking.
         *
         * That is not a style choice, it is the only shape that works here. Accretion is live from
         * the first second of a dev game, and [GameEngine.seedIntros] marks everything already
         * reached as read on the first tick — so a card gated on "the system exists" would be
         * marked read before the window had finished opening, on a brand new save, for ever. Hung
         * on the first catch and the first layer instead, both of which a new game is at nought.
         */
        FeatureIntro(
            id = "un_impacts",
            title = "Einschläge",
            where = "Aufbau",
            text = "Du bist ein Brocken, und du wächst, weil andere Brocken auf dich fallen. " +
                "Tippe sie an, bevor sie aufschlagen: das bringt sofort Masse — und Material, " +
                "das liegen bleibt. Was du damit anfängst, steht unter Aufbau.",
            appearsWhen = { it.impactsAbsorbed > 0 },
        ),
        FeatureIntro(
            id = "un_shells",
            title = "Schichten",
            where = "Aufbau",
            text = "Aus Material baust du Kern, Mantel und Kruste. Der Kern erhöht die " +
                "Produktion, der Mantel Tippwert und Anziehung, die Kruste Offline-Ertrag und " +
                "Ausbeute. Ab sechs Schichten bekommt dein Körper einen Typ — und der bleibt " +
                "eingetragen, auch wenn der Kollaps alles andere mitnimmt.",
            appearsWhen = { Shells.total(it) >= 3 },
        ),
        FeatureIntro(
            id = "un_collapse",
            title = "Der Kollaps",
            where = "Kosmos · Kollaps",
            text = "Dein Körper ist schwer genug, um in sich zusammenzufallen. Das setzt den " +
                "Lauf zurück — Masse, Kollektoren, Upgrades, alles — und du bekommst " +
                "Singularitäten dafür, die dauerhaft bleiben und alles Folgende schneller " +
                "machen. Es ist kein Verlust, es ist die zweite Hälfte des Spiels.",
            appearsWhen = { GameEngine.canCollapse(it) || it.collapses > 0 },
        ),
        FeatureIntro(
            id = "un_roles",
            title = "Rollen",
            where = "Flotte · auf einen Kollektor tippen",
            text = "Jeder Kollektor kann eine Aufgabe bekommen. Eine erhöht seinen eigenen " +
                "Ausstoß, eine andere den seiner Nachbarn — es lohnt sich, nicht überall " +
                "dasselbe einzustellen.",
            appearsWhen = { Roles.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_orbits",
            title = "Bahnen",
            where = "Bahnen",
            text = "Um deinen Körper lassen sich Bahnen öffnen und mit Trabanten besetzen. Zwei " +
                "Trabanten derselben Stufe verschmelzen zu einer höheren. Und Bahnen, deren " +
                "Umlaufzeiten glatt zueinander passen, verstärken sich gegenseitig.",
            appearsWhen = { Orbits.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_fusion",
            title = "Fusion",
            where = "Fusion",
            text = "Dein Körper brennt jetzt. Aus Wasserstoff wird Helium, daraus Kohlenstoff, " +
                "und so weiter bis zum Eisen — jedes Element multipliziert, was du ohnehin " +
                "produzierst. Fusoren kaufst du wie Kollektoren.",
            appearsWhen = { Fusion.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_lab",
            title = "Das Labor",
            where = "Kosmos · Labor",
            text = "Ein Projekt läuft auf der echten Uhr — auch wenn das Spiel zu ist. Es gibt " +
                "nur eine Bank, also läuft immer nur eines. Vor dem Weglegen etwas anzuschieben " +
                "ist darum fast immer richtig.",
            appearsWhen = { ResearchTree.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_rules",
            title = "Automatik",
            where = "Kosmos · Regeln",
            text = "Regeln nehmen dir ab, was du sonst von Hand machst: Kollektoren nachkaufen, " +
                "Bahnen ausbauen, Projekte anschieben. Jede lässt sich einzeln einstellen und " +
                "einzeln wieder ausschalten.",
            appearsWhen = { Automation.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_contracts",
            title = "Aufträge",
            where = "Kosmos · Regeln",
            text = "Drei Ziele liegen auf dem Tisch und werden nachgelegt. Bezahlt wird in " +
                "Äonen — der Währung des Urknalls. Ein Balken unter jedem sagt, wie weit du bist.",
            appearsWhen = { Contract.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_challenges",
            title = "Herausforderungen",
            where = "Kosmos · Regeln",
            text = "Ein Lauf unter erschwerten Regeln — kein Tippen, keine Upgrades, halbe " +
                "Produktion. Wer ihn schafft, behält einen dauerhaften Bonus. Mehrere lassen " +
                "sich kombinieren, und das zahlt sich überproportional aus.",
            appearsWhen = { it.collapses > 0 },
        ),
        FeatureIntro(
            id = "un_heavy",
            title = "Schwere Elemente",
            where = "Fusion",
            text = "Jenseits von Eisen geht es nicht mehr durch Brennen weiter — nur der " +
                "Kollaps selbst schmiedet diese Elemente. Sie bleiben über den Lauf hinaus und " +
                "heben an, was deine Fusionskette wert ist.",
            appearsWhen = { Heavy.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_bigbang",
            title = "Der Urknall",
            where = "Kosmos · Kollaps",
            text = "Die dritte Ebene. Der Urknall räumt auch die Singularitäten ab und gibt " +
                "Äonen dafür — und du wählst eine von vier Ausrichtungen, die den ganzen " +
                "nächsten Durchgang prägt. Dein altes Universum geht dabei nicht verloren.",
            appearsWhen = { BigBang.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_sky",
            title = "Der Himmel",
            where = "Kosmos · Himmel",
            text = "Jedes Universum, das du hinter dir lässt, bleibt als Galaxie am Himmel " +
                "stehen und arbeitet weiter. Du kannst ihnen Aufgaben geben, sie ausbauen, zwei " +
                "verschmelzen — und alte wieder besuchen.",
            appearsWhen = { Multiverse.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_alloys",
            title = "Legierungen",
            where = "Fusion",
            text = "Zwei schwere Elemente lassen sich zu einer Legierung schmieden. Die kostet " +
                "beide dauerhaft und gibt dafür einen Bonus, den kein einzelnes Element hat.",
            appearsWhen = { Alloy.isUnlocked(it) },
        ),
        FeatureIntro(
            id = "un_catalogue",
            title = "Die Kennungsleiter",
            where = "Leiter · links am Rand",
            text = "Über dem Schwarzen Loch hört die Leiter nicht auf. Jeder Körper kommt " +
                "sechshundertsechsundsiebzig Mal wieder, mit einer Kennung von AA bis ZZ — " +
                "sechzehntausend Sprossen. Manche davon sind Funde und wollen bestimmt werden.",
            appearsWhen = { Designations.isUnlocked(it) },
        ),
    )

    /**
     * The one to show now, or `null`.
     *
     * One at a time on purpose. Several unlock together after a big bang, and four cards stacked
     * on top of each other at the moment a run restarts is not an explanation, it is a wall.
     */
    fun pending(state: GameState): FeatureIntro? =
        all.firstOrNull { it.id !in state.seenIntros && it.appearsWhen(state) }

    /** Everything this state has already reached, whether or not it has been read. */
    fun applicable(state: GameState): List<FeatureIntro> = all.filter { it.appearsWhen(state) }
}
