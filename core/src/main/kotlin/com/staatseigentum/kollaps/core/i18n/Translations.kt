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

    /**
     * The ladder. Names first, then the line each rung says about itself.
     *
     * The names are the one place where the English is not a translation but the other language's
     * own word for the same object — a planet has an English name already, and inventing one would
     * be worse than looking it up.
     */
    private val TIERS: Map<String, String> = mapOf(
        "Meteorit" to "Meteoroid",
        "Ein Klumpen Gestein, der niemandem gehört. Fang klein an." to
            "A lump of rock that belongs to nobody. Start small.",
        "Asteroid" to "Asteroid",
        "Groß genug, um einen Namen und eine Nummer zu bekommen." to
            "Big enough to be given a name and a number.",
        "Zwergplanet" to "Dwarf Planet",
        "Rund genug für Stolz, zu klein für Respekt." to
            "Round enough for pride, too small for respect.",
        "Mond" to "Moon",
        "Grau, still, voller Krater. Und trotzdem sieht jeder hoch." to
            "Grey, silent, covered in craters. And still everybody looks up.",
        "Merkur" to "Mercury",
        "Verbrannt, vernarbt, aber offiziell ein Planet." to
            "Burnt, scarred, but officially a planet.",
        "Titan" to "Titan",
        "Ein Mond mit Wetter. Es regnet Methan, seit es ihn gibt." to
            "A moon with weather. It has rained methane since it existed.",
        "Mars" to "Mars",
        "Rost, Staub und ein paar sehr einsame Rover." to
            "Rust, dust and a few very lonely rovers.",
        "Venus" to "Venus",
        "Schön aus der Ferne. Aus der Nähe 460 Grad." to
            "Beautiful from far away. Up close, 460 degrees.",
        "Erde" to "Earth",
        "Der einzige Ort mit Kaffee. Behandle ihn gut." to
            "The only place with coffee. Treat it well.",
        "Supererde" to "Super-Earth",
        "Doppelt so schwer wie zuhause. Treppen wären hier eine Zumutung." to
            "Twice as heavy as home. Stairs here would be an imposition.",
        "Neptun" to "Neptune",
        "Windgeschwindigkeit: 2000 km/h. Niemand beschwert sich." to
            "Wind speed: 2000 km/h. Nobody complains.",
        "Uranus" to "Uranus",
        "Liegt auf der Seite und findet das völlig in Ordnung." to
            "Lies on its side and is entirely fine with that.",
        "Saturn" to "Saturn",
        "Der einzige Planet mit richtig gutem Schmuck." to
            "The only planet with genuinely good jewellery.",
        "Jupiter" to "Jupiter",
        "Ein Sturm, der älter ist als jede Stadt der Erde." to
            "A storm older than any city on Earth.",
        "Heißer Jupiter" to "Hot Jupiter",
        "Ein Gasriese so dicht an seinem Stern, dass er von unten glüht." to
            "A gas giant so close to its star that it glows from below.",
        "Brauner Zwerg" to "Brown Dwarf",
        "Wollte ein Stern werden. Hat es knapp nicht geschafft." to
            "Wanted to be a star. Missed it by a little.",
        "Roter Zwerg" to "Red Dwarf",
        "Brennt sparsam — und dafür ein paar Billionen Jahre." to
            "Burns frugally — and therefore for a few trillion years.",
        "Sonne" to "Sun",
        "Ganz normaler gelber Zwerg. Für uns trotzdem alles." to
            "A perfectly ordinary yellow dwarf. Still everything, to us.",
        "Blauer Riese" to "Blue Giant",
        "Verschwendet in einer Million Jahren, was andere in Milliarden brauchen." to
            "Squanders in a million years what others make last for billions.",
        "Roter Überriese" to "Red Supergiant",
        "So groß, dass die Erdbahn bequem hineinpasst." to
            "So large that Earth's orbit fits comfortably inside it.",
        "Hyperriese" to "Hypergiant",
        "Das größte, was ein Stern werden kann, bevor er sich selbst zerreißt." to
            "The largest a star can get before it tears itself apart.",
        "Weißer Zwerg" to "White Dwarf",
        "Was übrig bleibt, wenn ein Stern fertig ist: heiße Asche, erdgroß." to
            "What is left when a star is done: hot ash, the size of Earth.",
        "Neutronenstern" to "Neutron Star",
        "Ein Teelöffel davon wiegt so viel wie ein Gebirge." to
            "A teaspoon of it weighs as much as a mountain range.",
        "Magnetar" to "Magnetar",
        "Sein Magnetfeld würde dich noch aus tausend Kilometern zerlegen." to
            "Its magnetic field would take you apart from a thousand kilometres away.",
        "Schwarzes Loch" to "Black Hole",
        "Das Ende der Leiter. Ab hier kommt nichts mehr zurück." to
            "The end of the ladder. Nothing comes back from here.",
    )

    /** The fleet. */
    private val COLLECTORS: Map<String, String> = mapOf(
        "Staubfänger" to "Dust Catcher",
        "Ein Netz aus Folie, das im Vakuum treibt und Krümel einsammelt." to
            "A sheet of foil drifting in vacuum, gathering crumbs.",
        "Asteroidennetz" to "Asteroid Net",
        "Wirf es aus, warte, zieh es ein. Weltraumfischen eben." to
            "Cast it, wait, haul it in. Fishing, but in space.",
        "Bergbaudrohne" to "Mining Drone",
        "Bohrt, kaut, spuckt Gestein aus. Fragt nie nach Pause." to
            "Drills, chews, spits out rock. Never asks for a break.",
        "Orbitalraffinerie" to "Orbital Refinery",
        "Trennt Wertvolles von Schotter, direkt im Orbit." to
            "Separates the valuable from the gravel, right there in orbit.",
        "Massetreiber" to "Mass Driver",
        "Eine Kanone, die ganze Berge in deine Umlaufbahn schießt." to
            "A cannon that fires whole mountains into your orbit.",
        "Kometenfänger" to "Comet Catcher",
        "Fängt Eisbrocken ein, bevor sie irgendwo einschlagen." to
            "Catches lumps of ice before they hit anything.",
        "Dyson-Schwarm" to "Dyson Swarm",
        "Millionen Spiegel, die einem Stern die Energie abknöpfen." to
            "Millions of mirrors, relieving a star of its energy.",
        "Sternenschmiede" to "Star Forge",
        "Fusioniert leichte Kerne zu schweren. Laut. Sehr laut." to
            "Fuses light nuclei into heavy ones. Loud. Very loud.",
        "Quantenkollektor" to "Quantum Collector",
        "Schöpft Teilchen direkt aus dem Nichts. Ist erlaubt, wenn man schnell ist." to
            "Scoops particles straight out of nothing. Allowed, if you are quick.",
        "Singularitätsextraktor" to "Singularity Extractor",
        "Zapft den Rand eines Ereignishorizonts an. Vorsichtig." to
            "Taps the edge of an event horizon. Carefully.",
        "Zeitdilatator" to "Time Dilator",
        "Draußen vergeht eine Sekunde, drinnen eine Woche Schichtarbeit." to
            "A second passes outside; inside, a week of shift work.",
        "Urknall-Echo" to "Big Bang Echo",
        "Fängt den Nachhall des ersten Augenblicks ein und presst ihn zu Materie." to
            "Catches the reverberation of the first moment and presses it into matter.",
        "Vakuumdestillat" to "Vacuum Distillate",
        "Destilliert das Nichts, bis unten etwas übrig bleibt. Fragt nicht, was." to
            "Distils nothing until something is left at the bottom. Do not ask what.",
        "Faltwerk" to "Folding Works",
        "Legt den Raum in Falten und schüttelt aus, was zwischen ihnen hängt." to
            "Folds space and shakes out whatever is caught between the creases.",
        "Kausalitätsweber" to "Causality Loom",
        "Knüpft Ursache an Wirkung, bis Materie der kürzeste Weg zwischen beiden ist." to
            "Ties cause to effect until matter is the shortest path between them.",
        "Urgrund-Anzapfung" to "Bedrock Tap",
        "Unter allem liegt noch etwas. Von dort holt sie es hoch." to
            "There is something beneath everything. This brings it up.",
        "Omega-Kollektor" to "Omega Collector",
        "Sammelt ein, was übrig sein wird. Rückwärts, vom Ende her." to
            "Collects what will be left. Backwards, starting from the end.",
    )

    /** Everything, in one map. Areas are added here as each one is finished. */
    val EN: Map<String, String> = buildMap {
        putAll(UI)
        putAll(TIERS)
        putAll(COLLECTORS)
    }
}
