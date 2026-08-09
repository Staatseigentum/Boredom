package com.staatseigentum.kollaps.core.i18n

/**
 * Every German text the game can show, and its English.
 *
 * Filled in batches, one content area at a time. Until it covers everything, the language switch
 * stays out of the settings — a half-translated game reads as broken rather than as bilingual,
 * and the test in `LanguageCoverageTest` is what decides when that is no longer the case.
 *
 * Kept as one file rather than split per area on purpose: the one question worth asking of it is
 * "is this string in here", and one map answers that without anybody having to know which file a
 * given sentence lives in.
 */
internal object Translations {

    /** The interface: buttons, headings and the sentences that explain a mechanism. */
    private val UI: Map<String, String> = mapOf(
        // -------------------------------------------------------------------- shop tabs
        "Kollektoren" to "Collectors",
        "Upgrades" to "Upgrades",
        "Bahnen" to "Orbits",
        "Fusion" to "Fusion",
        "Erfolge" to "Achievements",
        "Kosmos" to "Cosmos",

        // ------------------------------------------------------------------ cosmos tabs
        "Kollaps" to "Collapse",
        "Labor" to "Lab",
        "Regeln" to "Rules",
        "System" to "System",

        // ------------------------------------------------------------------- the header
        "Masse" to "Mass",
        "Produktion" to "Production",
        "pro Tipp" to "per tap",
        "Nächste Stufe" to "Next tier",

        // --------------------------------------------------------------------- settings
        "Einstellungen" to "Settings",
        "Klickgeräusch" to "Click sound",
        "Vibration" to "Vibration",
        "Musik" to "Music",
        "Zahlen" to "Numbers",
        "Farben" to "Colours",
        "Spielstand" to "Save",
        "Spielstände" to "Saves",
        "Version" to "Version",
        "Namen" to "Names",
        "Wissenschaftlich" to "Scientific",
        "Kurzform" to "Compact",

        // ---------------------------------------------------------------------- buttons
        "Annehmen" to "Accept",
        "Beide annehmen" to "Accept both",
        "Aufgeben" to "Give up",
        "Wirklich aufgeben?" to "Really give up?",
        "Neu ansetzen" to "Start over",
        "Belohnung einlösen" to "Claim reward",
        "Abbrechen" to "Cancel",
        "Doch nicht" to "Never mind",
        "Laden" to "Load",
        "Seite öffnen" to "Open page",
        "Herunterladen" to "Download",
        "Installieren und beenden" to "Install and quit",
        "Kopieren" to "Copy",
        "Einfügen" to "Paste",
        "wechseln" to "switch",
        "gewählt" to "chosen",
        "gekauft" to "owned",
        "gesperrt" to "locked",
        "aktiv" to "active",
        "erfüllt" to "met",
        "offen" to "open",
        "hier" to "here",
        "neu" to "new",
        "aus" to "off",

        // ------------------------------------------------------------------ challenges
        "Herausforderungen" to "Challenges",
        "Herausforderung läuft" to "Challenge running",
        "Zwei Herausforderungen laufen" to "Two challenges running",
        "Regel" to "Rule",
        "Ziel" to "Goal",
        "Belohnung" to "Reward",
        "Bonus" to "Bonus",
        "Gespielt" to "Played",

        // -------------------------------------------------------------------- big bang
        "Urknall" to "Big Bang",
        "Was für ein Universum?" to "What kind of universe?",
        "Urknall auslösen" to "Trigger the Big Bang",
        "Noch nicht so weit" to "Not yet",
        "Äonen ausgeben" to "Spend aeons",

        // ----------------------------------------------------------------------- misc
        "Statistik" to "Statistics",
        "Chronik" to "Chronicle",
        "Suche nach einer neueren Version …" to "Looking for a newer version …",
        "Alles aktuell." to "Up to date.",
        "Nicht erreichbar." to "Not reachable.",
        "Upgrade suchen …" to "Search upgrades …",
    )

    /** Everything, in one map. Areas are added here as each one is finished. */
    val EN: Map<String, String> = buildMap {
        putAll(UI)
    }
}
