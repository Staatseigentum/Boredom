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

    /** The lab bench. */
    private val RESEARCH: Map<String, String> = mapOf(
        "Spektralanalyse" to "Spectral Analysis",
        "Wer weiß, woraus ein Lichtpunkt besteht, sieht ihn früher kommen." to
            "Knowing what a point of light is made of means seeing it coming sooner.",
        "Massespeicher" to "Mass Store",
        "Ein Lager, das auch dann noch annimmt, wenn niemand hinsieht." to
            "A depot that keeps taking deliveries when nobody is watching.",
        "Ionenantrieb" to "Ion Drive",
        "Wenig Schub, endlos lange. Genau richtig für etwas, das nie ankommen muss." to
            "Little thrust, forever. Just right for something that never has to arrive.",
        "Telemetrie" to "Telemetry",
        "Die Flotte funkt, was sie tut. Vorher war es Vertrauenssache." to
            "The fleet reports what it is doing. Before, it was a matter of trust.",
        "Schwarmlogik" to "Swarm Logic",
        "Hundert Maschinen, die sich absprechen, sind mehr als hundert Maschinen." to
            "A hundred machines that talk to each other are more than a hundred machines.",
        "Kryospeicher" to "Cryogenic Store",
        "Kalt genug, dass sich ein ganzer Tag Produktion nicht langweilt." to
            "Cold enough that a whole day of production does not get bored.",
        "Lademaschine" to "Loading Engine",
        "Kauft nach, solange Überschuss da ist. Fragt nicht, ob es passt." to
            "Buys more while there is a surplus. Does not ask whether it fits.",
        "Resonanzhammer" to "Resonance Hammer",
        "Schlägt im Takt der Eigenfrequenz. Dein Finger darf sich ausruhen." to
            "Strikes on the natural frequency. Your finger may rest.",
        "Gravitationslinse" to "Gravitational Lens",
        "Krümmt den Raum so, dass mehr davon auf dich zeigt." to
            "Bends space so that more of it points at you.",
        "Parallelrechnung" to "Parallel Computation",
        "Zwei Fragen gleichzeitig zu stellen war die letzte Frage." to
            "Asking two questions at once was the last question.",
        "Magnetischer Einschluss" to "Magnetic Confinement",
        "Hält das Plasma dort, wo es brennen soll, statt an der Wand." to
            "Keeps the plasma where it should burn, rather than against the wall.",
        "Katalysierte Fusion" to "Catalysed Fusion",
        "Ein Myon an der richtigen Stelle spart dem Kern zehn Millionen Grad." to
            "One muon in the right place saves the core ten million degrees.",
        "Horizontmechanik" to "Horizon Mechanics",
        "Was hineinfällt, ist weg. Was am Rand bleibt, lässt sich zählen." to
            "What falls in is gone. What stays at the edge can be counted.",
        "Ewigkeitsformel" to "Eternity Formula",
        "Acht Stunden Rechenzeit für einen Satz, den danach niemand mehr braucht." to
            "Eight hours of computing for a sentence nobody needs afterwards.",
    )

    /** The repeatable singularity sinks. */
    private val INVESTMENTS: Map<String, String> = mapOf(
        "Rücklagenkonto" to "Reserve Account",
        "Jeder Durchlauf legt etwas zur Seite, das der nächste vorfindet." to
            "Every run puts something aside for the next one to find.",
        "Muskelgedächtnis" to "Muscle Memory",
        "Die Hand weiß, wo sie hinschlägt, bevor der Kopf es merkt." to
            "The hand knows where it is striking before the head notices.",
        "Verdichtung" to "Compaction",
        "Was oft genug durch einen Horizont ging, bleibt dichter zurück." to
            "What has been through a horizon often enough comes back denser.",
        "Bahnrechnung" to "Orbital Reckoning",
        "Du weißt inzwischen nicht nur wo, sondern auch wann." to
            "By now you know not only where, but when.",
        "Tiefkühlhalle" to "Cold Store",
        "Reihe um Reihe Kammern, und alle nehmen weiter an." to
            "Row upon row of chambers, all of them still accepting.",
        "Nachtschicht" to "Night Shift",
        "Irgendwann arbeitet die Flotte ohne dich genauso gut wie mit dir." to
            "Eventually the fleet works as well without you as with you.",
        "Eingelagerte Flotte" to "Stored Fleet",
        "Nicht die Anlagen überleben den Kollaps, sondern das Lagerverzeichnis." to
            "It is not the machines that survive the collapse — it is the inventory.",
        "Serienfertigung" to "Series Production",
        "Jede fünfundzwanzigste Maschine ist ein bisschen besser als die davor." to
            "Every twenty-fifth machine is a little better than the one before.",
        "Brennkammern" to "Burn Chambers",
        "Mehr Öfen an derselben Kette, alle mit demselben Feuer." to
            "More furnaces on the same chain, all on the same fire.",
        "Zweite Schicht" to "Second Shift",
        "Das Labor läuft jetzt auch nachts. Warten muss man trotzdem." to
            "The lab runs at night too now. You still have to wait.",
        "Gebündelte Enden" to "Bundled Ends",
        "Singularitäten liegen dichter, wenn man sie ordentlich stapelt." to
            "Singularities sit closer together when stacked properly.",
        "Sauberer Schnitt" to "Clean Cut",
        "Beim nächsten Kollaps geht weniger daneben." to
            "Less goes to waste in the next collapse.",
    )

    /** The four path trees. */
    private val PATHS: Map<String, String> = mapOf(
        "Geübter Schlag" to "Practised Strike",
        "Zehntausend Wiederholungen später sitzt jede Bewegung." to
            "Ten thousand repetitions later, every movement lands.",
        "Doppelgriff" to "Double Grip",
        "Zwei Hände, und keine wartet auf die andere." to
            "Two hands, and neither waits for the other.",
        "Nachhall" to "Reverberation",
        "Was du angestoßen hast, schlägt eine Weile von allein weiter." to
            "What you set going keeps striking on its own for a while.",
        "Scharfes Auge" to "Sharp Eye",
        "Nichts zieht mehr unbemerkt vorbei." to "Nothing drifts past unnoticed any more.",
        "Durchlaufender Betrieb" to "Continuous Operation",
        "Die Anlage kennt keine Schicht, die endet." to
            "The plant knows no shift that ends.",
        "Jede Auflage läuft glatter als die davor." to
            "Every production run goes more smoothly than the last.",
        "Abwesenheit ist auch eine Betriebsart." to
            "Absence is a mode of operation too.",
        "Stehende Flotte" to "Standing Fleet",
        "Sie wird zwischen zwei Universen nicht abgebaut." to
            "It is not dismantled between two universes.",
        "Zweite Bank" to "Second Bench",
        "Ein Projekt mehr, das über Nacht fertig wird." to
            "One more project that finishes overnight.",
        "Langzeitversuch" to "Long-Term Trial",
        "Manches muss man einfach lange genug stehen lassen." to
            "Some things simply have to be left standing long enough.",
        "Anschubmittel" to "Seed Funding",
        "Ein Labor, das bei null anfängt, forscht erst mal gar nichts." to
            "A lab that starts from nothing researches nothing at first.",
        "Querverweis" to "Cross-Reference",
        "Zwei Ergebnisse, die nichts miteinander zu tun hatten, bis jemand hinsah." to
            "Two results with nothing to do with each other, until somebody looked.",
        "Heißerer Brennraum" to "Hotter Chamber",
        "Es fusioniert schneller, als es sollte." to "It fuses faster than it should.",
        "Höherer Druck" to "Higher Pressure",
        "Was der Druck nicht schafft, schafft mehr Druck." to
            "What pressure cannot manage, more pressure can.",
        "Dichter Rest" to "Denser Remnant",
        "Was am Rand des Kollapses bleibt, wiegt mehr." to
            "What stays at the edge of the collapse weighs more.",
        "Durchgebrannt" to "Burnt Through",
        "Ein Stern, der schneller stirbt, gibt in kürzerer Zeit mehr her." to
            "A star that dies faster gives up more in less time.",
    )


    /**
     * The shop: what a machine's improvement is called and what it claims to do.
     *
     * The nine mark names and the sentence under them are templates — one each rather than one per
     * machine. See `Upgrade.compose`, which fills the name in at the moment it is read.
     */
    private val UPGRADES: Map<String, String> = mapOf(
        // ---- the marks, once, for all two hundred rows
        "%s Mk II" to "%s Mk II",
        "%s Mk III" to "%s Mk III",
        "%s Mk IV" to "%s Mk IV",
        "%s Mk V" to "%s Mk V",
        "%s Mk VI" to "%s Mk VI",
        "%s Mk VII" to "%s Mk VII",
        "%s Mk VIII" to "%s Mk VIII",
        "%s Mk IX" to "%s Mk IX",
        "%s Mk X" to "%s Mk X",
        "Doppelte Leistung aus jedem %s." to "Twice the output from every %s.",

        // ---- tapping
        "Verstärkte Finger" to "Reinforced Fingers",
        "Handschuhe mit Servomotoren. Billig, laut, effektiv." to
            "Gloves with servo motors. Cheap, loud, effective.",
        "Titanfingerkuppen" to "Titanium Fingertips",
        "Härter als alles, was du anfasst." to "Harder than anything you touch.",
        "Kinetischer Impulsgeber" to "Kinetic Impactor",
        "Jede Berührung schlägt ein wie ein kleiner Meteor." to
            "Every touch lands like a small meteor.",
        "Tektonischer Druck" to "Tectonic Pressure",
        "Du tippst nicht mehr, du verschiebst Platten." to
            "You are not tapping any more, you are moving plates.",
        "Quantenfinger" to "Quantum Finger",
        "Berührt alle möglichen Stellen gleichzeitig." to
            "Touches every possible spot at once.",
        "Nukleare Berührung" to "Nuclear Touch",
        "Fusion auf Fingerdruck. Bitte nicht kratzen." to
            "Fusion at the press of a finger. Please do not scratch.",
        "Ereignishorizont-Griff" to "Event Horizon Grip",
        "Was du berührst, kommt nicht zurück." to "What you touch does not come back.",
        "Hand der Singularität" to "Hand of the Singularity",
        "Ein Fingerzeig, und die Materie ordnet sich." to
            "One gesture, and matter falls into line.",
        "Seismische Resonanz" to "Seismic Resonance",
        "Dein Tippen bringt die ganze Anlage zum Mitschwingen." to
            "Your tapping sets the whole operation ringing.",
        "Harmonischer Kollaps" to "Harmonic Collapse",
        "Ein Tipp im richtigen Takt und alles arbeitet doppelt." to
            "One tap on the beat and everything works twice as hard.",
        "Singularitätsecho" to "Singularity Echo",
        "Jeder Tipp hallt durch jede Maschine, die du besitzt." to
            "Every tap echoes through every machine you own.",

        // ---- global and offline
        "Gravitationsgriff" to "Gravitational Grip",
        "Zieht das Gestein dir entgegen, statt umgekehrt." to
            "Pulls the rock towards you instead of the other way round.",
        "Kosmische Ausrichtung" to "Cosmic Alignment",
        "Alle Bahnen in einer Reihe. Der Rest ist Logistik." to
            "Every orbit in a row. The rest is logistics.",
        "Dunkle-Materie-Verdichter" to "Dark Matter Compactor",
        "Presst das Unsichtbare zu etwas, das man wiegen kann." to
            "Presses the invisible into something you can weigh.",
        "Vakuumfluktuation" to "Vacuum Fluctuation",
        "Das Nichts ist erstaunlich ergiebig, wenn man es schüttelt." to
            "Nothing turns out to be remarkably productive once you shake it.",
        "Entropieumkehr" to "Entropy Reversal",
        "Du räumst auf, was das Universum seit 13 Milliarden Jahren verstreut." to
            "You are tidying up what the universe has been scattering for 13 billion years.",
        "Raumzeitfalte" to "Spacetime Fold",
        "Du faltest die Strecke weg, statt sie zurückzulegen." to
            "You fold the distance away instead of covering it.",
        "Letzte Symmetrie" to "Final Symmetry",
        "Die eine Regel, aus der alle anderen folgen. Du hast sie umgestellt." to
            "The one rule the others all follow from. You have rearranged it.",
        "Autonome Drohnen" to "Autonomous Drones",
        "Sie arbeiten auch weiter, wenn du das Handy weglegst." to
            "They keep working when you put the phone down.",
        "Kryostase-Puffer" to "Cryostasis Buffer",
        "Lagert die Ausbeute ein, bis du wiederkommst." to
            "Stores the yield until you come back.",
        "Trägheitsspeicher" to "Inertial Store",
        "Was die Anlage nachts fördert, wartet jetzt zwei Tage auf dich." to
            "What the operation brings in overnight will now wait two days for you.",

        // ---- synergies
        "Sortierte Krümel" to "Sorted Crumbs",
        "Was das Netz sonst mühsam suchen müsste, liegt schon vorsortiert bereit." to
            "What the net would have to hunt for is already sorted and waiting.",
        "Vermessene Brocken" to "Surveyed Boulders",
        "Die Netze wissen, wo es sich zu bohren lohnt." to
            "The nets know where it is worth drilling.",
        "Kurze Wege" to "Short Hauls",
        "Die Drohnen kippen direkt in den Trichter, statt zwischenzulagern." to
            "The drones tip straight into the hopper instead of stockpiling.",
        "Gereinigte Ladung" to "Cleaned Payload",
        "Nur noch Wertvolles wird beschleunigt. Schotter fliegt nicht mit." to
            "Only the valuable part gets accelerated. Gravel stays behind.",
        "Abgelenkte Bahnen" to "Deflected Trajectories",
        "Ein Schuss in die richtige Richtung, und der Brocken kommt von selbst." to
            "One shot in the right direction and the rock arrives on its own.",
        "Eisgekühlte Spiegel" to "Ice-Cooled Mirrors",
        "Kometeneis hält die Spiegel kalt, und kalte Spiegel liefern mehr." to
            "Comet ice keeps the mirrors cold, and cold mirrors deliver more.",
        "Gemeinsame Netze" to "Shared Grid",
        "Ein Stern versorgt jede Maschine, die du hast." to
            "One star supplies every machine you own.",
        "Angeheizte Schmelze" to "Stoked Melt",
        "Die Schmiede bekommt ihre Energie nicht mehr aus eigener Tasche." to
            "The forge no longer pays for its own heat.",
        "Schwere Kerne" to "Heavy Cores",
        "Aus dem Nichts lässt sich leichter schöpfen, wenn daneben etwas ist." to
            "Drawing from nothing is easier when something is standing next to it.",
        "Vorgespannter Rand" to "Pre-Stressed Edge",
        "Der Horizont gibt williger her, wenn das Vakuum schon zittert." to
            "The horizon gives more willingly when the vacuum is already trembling.",
        "Geteilter Horizont" to "Shared Horizon",
        "Alle zapfen dieselbe Quelle an, und keiner merkt es dem anderen an." to
            "They all tap the same source, and none of them can tell.",
        "Geliehene Krümmung" to "Borrowed Curvature",
        "Zeit dehnt sich leichter dort, wo die Raumzeit ohnehin schon reißt." to
            "Time stretches more easily where spacetime is already tearing.",
        "Gezeitenkraft" to "Tidal Force",
        "Der Körper zerrt selbst an dem, was ihn abbaut. Du hältst nur noch dagegen." to
            "The body pulls at what is mining it. You are only holding on.",
        "Gedehnter Nachhall" to "Stretched Reverberation",
        "Eine Sekunde Urknall dauert drinnen erheblich länger." to
            "One second of Big Bang lasts considerably longer on the inside.",
        "Raumzeitgefälle" to "Spacetime Gradient",
        "Du legst das Universum leicht schräg und lässt den Rest herunterrollen." to
            "You tilt the universe a little and let the rest roll downhill.",
        "Gleichgeschaltet" to "In Lockstep",
        "Die ganze Anlage schwingt im Takt des ersten Augenblicks." to
            "The whole operation beats in time with the first moment.",
        "Nachhall im Kessel" to "Reverberation in the Vessel",
        "Der Nachhall rührt das Nichts um, und das Nichts gibt nach." to
            "The reverberation stirs the nothing, and the nothing yields.",
        "Vorgeklärtes Vakuum" to "Clarified Vacuum",
        "Ein sauberer Raum lässt sich sauberer falten." to
            "A clean space folds more cleanly.",
        "Kurze Fäden" to "Short Threads",
        "Zwischen zwei Falten ist der Weg von Ursache zu Wirkung kaum noch einer." to
            "Between two folds, cause and effect are barely a distance apart.",
        "Gespanntes Gewebe" to "Taut Weave",
        "Wo alles zusammenhängt, hängt auch der Grund mit dran." to
            "Where everything is connected, the reason comes along too.",
        "Vom Ende her" to "From the End Backwards",
        "Wer weiß, was unten liegt, weiß auch, was übrig bleibt." to
            "Whoever knows what lies at the bottom knows what will be left.",
        "Rückwärts geplant" to "Planned in Reverse",
        "Jede Maschine tut schon jetzt, was sie am Ende getan haben wird." to
            "Every machine is already doing what it will have done at the end.",
        "Vorsortierte Unordnung" to "Pre-Sorted Disorder",
        "Was am Ende übrig bleibt, ist schon halb geordnet." to
            "What is left at the end is already half in order.",
        "Gemahlener Boden" to "Milled Ground",
        "In geordnetem Grund zieht sich die Furche von allein." to
            "In ordered ground the furrow cuts itself.",
        "Aufgebrochener Rand" to "Broken Edge",
        "Ein gepflügter Horizont gibt beim Pressen leichter nach." to
            "A ploughed horizon gives way more easily under the press.",
        "Unter gleichem Druck" to "Under Equal Pressure",
        "Die ganze Anlage steht im selben gepressten Nichts." to
            "The whole operation stands in the same pressed nothing.",
        "Unter Druck" to "Under Pressure",
        "Gepresstes Nichts drückt die Schleuse von selbst auf." to
            "Pressed nothing forces the lock open by itself.",
        "Offener Durchgang" to "Open Passage",
        "Wenn die Schleuse steht, ist der Weg vor den Anfang kurz." to
            "Once the lock is open, the way back before the beginning is short.",
        "Vor dem Anfang gebaut" to "Built Before the Beginning",
        "Jede Maschine stand schon da, bevor es ein Davor gab." to
            "Every machine was already standing there before there was a before.",
    )

    /** The three ways a machine can be set up. */
    private val ROLES: Map<String, String> = mapOf(
        "Menge" to "Quantity",
        "Billiger gebaut, dafür schlampiger. Es zählt, wie viele es sind." to
            "Built cheaper and sloppier. What counts is how many there are.",
        "Güte" to "Quality",
        "Sorgfältig gebaut und entsprechend teuer. Es zählt, was einer leistet." to
            "Built carefully and priced accordingly. What counts is what one can do.",
        "Netz" to "Network",
        "Arbeitet kaum noch selbst, sondern koordiniert alle anderen." to
            "Barely works itself any more; it coordinates all the others.",
        "%s Ausstoß" to "%s output",
        "%s Preis" to "%s price",
        "+%s auf alle anderen je %s Stück" to "+%s to all others per %s owned",
    )


    /** What singularities and Äonen buy, and the four leanings a universe can have. */
    private val PRESTIGE: Map<String, String> = mapOf(
        // ---- bought with singularities
        "Wache Drohnen" to "Watchful Drones",
        "Sie hören nicht auf, nur weil du weg bist." to
            "They do not stop just because you are gone.",
        "Rücklage" to "Reserve",
        "Ein Rest Masse, den der Kollaps nicht mitgenommen hat." to
            "A remainder of mass the collapse did not take with it.",
        "Eingeübter Griff" to "Practised Grip",
        "Die Hände erinnern sich an jeden Durchlauf." to
            "The hands remember every run.",
        "Kometenbahn" to "Comet Track",
        "Du weißt inzwischen, wo man wartet." to "By now you know where to wait.",
        "Kleiner Automat" to "Small Automaton",
        "Ein Arm, ein Motor, ein Takt. Er wird nicht müde und beschwert sich nie." to
            "One arm, one motor, one beat. It never tires and never complains.",
        "Schlagwerk" to "Striking Works",
        "Zehn Arme im Takt. Du darfst zusehen — oder mittippen, das zählt dazu." to
            "Ten arms in time. You may watch — or tap along, that counts too.",
        "Langzeitspeicher" to "Long-Term Store",
        "Lagert die Ausbeute einen ganzen Tag lang ein." to
            "Stores the yield for a whole day.",
        "Bewahrte Baupläne" to "Kept Blueprints",
        "Der Kollaps frisst die Anlagen, nicht das Wissen." to
            "The collapse eats the machines, not the knowledge.",
        "Verdichtete Materie" to "Compacted Matter",
        "Was einmal durch ein schwarzes Loch ging, wiegt mehr." to
            "What has been through a black hole once weighs more.",
        "Selbsttätige Beschaffung" to "Automatic Procurement",
        "Sie kauft nach, wenn reichlich da ist, und lässt dir den Rest für Upgrades." to
            "It restocks when there is plenty and leaves you the rest for upgrades.",
        "Saubere Trennung" to "Clean Separation",
        "Beim Kollabieren geht weniger verloren." to "Less is lost when you collapse.",
        "Vorgefertigte Flotte" to "Prefabricated Fleet",
        "Der nächste Durchlauf beginnt nicht mehr bei null." to
            "The next run no longer starts from nothing.",

        // ---- bought with Äonen
        "Entropiekonto" to "Entropy Account",
        "Die Unordnung von neun Universen, gebündelt." to
            "The disorder of nine universes, bundled together.",
        "Dritte Ausdehnung" to "Third Expansion",
        "Der Raum hat sich daran gewöhnt, für dich zu arbeiten." to
            "Space has got used to working for you.",
        "Vollständiges Lagerverzeichnis" to "Complete Inventory",
        "Nicht mehr nur die Liste überlebt, sondern auch, wo alles stand." to
            "It is not only the list that survives now, but where everything stood.",
        "Dauerbetrieb" to "Continuous Operation",
        "Drei Tage ohne dich, und niemand hat gemerkt, dass du weg warst." to
            "Three days without you and nobody noticed you were gone.",
        "Durchgeheizt" to "Kept Burning",
        "Die Öfen gehen zwischen zwei Universen nicht mehr aus." to
            "The furnaces no longer go out between two universes.",
        "Übertragene Notizen" to "Carried-Over Notes",
        "Was einmal verstanden wurde, muss nicht zweimal verstanden werden." to
            "What has been understood once does not need understanding twice.",
        "Unermüdlich" to "Tireless",
        "Dreißig Schläge in der Sekunde, und keiner davon von dir." to
            "Thirty strikes a second, and none of them yours.",
        "Eingefahrene Serien" to "Established Production Runs",
        "Die Fertigung kennt jede Auflage, die es je gegeben hat." to
            "The line knows every edition there has ever been.",
        "Dichter Trümmergürtel" to "Dense Debris Belt",
        "Von acht Universen bleibt einiges liegen, und alles davon fliegt." to
            "Eight universes leave a good deal behind, and all of it is moving.",
        "Vierte Ausdehnung" to "Fourth Expansion",
        "Irgendwann fragt der Raum nicht mehr nach, er dehnt sich einfach." to
            "At some point space stops asking and simply expands.",
        "Doppelter Schnitt" to "Double Cut",
        "Zwei Singularitäten, wo vorher eine war. Frag nicht, welche die echte ist." to
            "Two singularities where there was one. Do not ask which is the real one.",
        "Fremde Finger" to "Other People's Fingers",
        "Irgendwo tippt etwas weiter, das du nie eingestellt hast." to
            "Somewhere something you never hired is still tapping.",
        "Erste Ausdehnung" to "First Expansion",
        "Der Raum selbst arbeitet für dich, seit du ihn einmal neu gefaltet hast." to
            "Space itself has been working for you since you folded it once.",
        "Übriggebliebene Materie" to "Leftover Matter",
        "Ein Universum später findet sich immer noch etwas in den Taschen." to
            "A universe later there is still something in the pockets.",
        "Dichteres Nichts" to "Denser Nothing",
        "Jede Singularität wiegt schwerer als in der Welt davor." to
            "Every singularity weighs more than it did in the world before.",
        "Ewiges Schlagwerk" to "Eternal Striking Works",
        "Es hat vor diesem Universum getippt und wird nach ihm weitertippen." to
            "It was tapping before this universe and will go on tapping after it.",
        "Eingespielte Serien" to "Settled Production Runs",
        "Die Fertigungsstraßen erinnern sich an jede Auflage, die es je gab." to
            "The assembly lines remember every edition there ever was.",
        "Mitgenommene Flotte" to "Fleet Brought Along",
        "Hundert Stück von allem, noch bevor der erste Stein fällt." to
            "A hundred of everything, before the first stone even falls.",
        "Dichter Kometenstrom" to "Dense Comet Stream",
        "Die Trümmer des letzten Universums ziehen immer noch vorbei." to
            "The debris of the last universe is still drifting past.",
        "Zweite Ausdehnung" to "Second Expansion",
        "Und noch einmal, und diesmal weiß der Raum schon, wie es geht." to
            "And again, and this time space already knows how it goes.",
        "Ewiges Feuer" to "Eternal Fire",
        "Es brannte im letzten Universum und hat den Übergang nicht bemerkt." to
            "It burned in the last universe and did not notice the changeover.",
        "Übertragene Bibliothek" to "Carried-Over Library",
        "Ein Labor, das schon weiß, was es diesmal herausfinden wird." to
            "A lab that already knows what it is going to find out this time.",
        "Langer Atem" to "Long Breath",
        "Vier Tage Abwesenheit sind für eine Galaxie keine Erwähnung wert." to
            "Four days away is not worth a galaxy's mention.",
        "Der Raum dehnt sich inzwischen, ohne dass jemand es anstößt." to
            "Space expands on its own now, without anybody nudging it.",
        "Vollständige Werft" to "Complete Shipyard",
        "Fünfhundert Stück von allem, und die Baupläne für den Rest." to
            "Five hundred of everything, and the blueprints for the rest.",
        "Letzte Ausdehnung" to "Final Expansion",
        "Danach kommt nichts mehr. Es sei denn, du machst weiter." to
            "Nothing comes after this. Unless you carry on.",

        // ---- the four kinds of universe
        "Die Hand" to "The Hand",
        "Ein Universum, in dem Masse dorthin kommt, wo jemand hinschlägt." to
            "A universe where mass arrives wherever somebody strikes.",
        "Die Maschine" to "The Machine",
        "Eines, das ohne dich weiterläuft und dabei kaum langsamer wird." to
            "One that keeps running without you and barely slows down doing it.",
        "Das Labor" to "The Laboratory",
        "Eines, in dem Wissen schneller entsteht als Materie — und über Nacht am meisten." to
            "One where knowledge grows faster than matter — and fastest overnight.",
        "Der Kern" to "The Core",
        "Eines, in dem Sterne heißer brennen und schwerer sterben." to
            "One where stars burn hotter and die harder.",
    )

    /** The fusion chain: six elements, six stages, and what each one is worth. */
    private val FUSION: Map<String, String> = mapOf(
        "Wasserstoff" to "Hydrogen",
        "Der erste Stoff überhaupt. Alles andere ist daraus gemacht." to
            "The first substance there was. Everything else is made out of it.",
        "Helium" to "Helium",
        "Asche des ersten Feuers. Wurde am Himmel entdeckt, bevor jemand es in der Hand hatte." to
            "Ash of the first fire. Found in the sky before anybody held any.",
        "Kohlenstoff" to "Carbon",
        "Drei Heliumkerne, die sich gleichzeitig treffen. Unwahrscheinlich, und doch bist du daraus." to
            "Three helium nuclei meeting at once. Unlikely, and yet you are made of it.",
        "Sauerstoff" to "Oxygen",
        "Das dritthäufigste Element im Universum, und das erste, das jemand vermisst." to
            "The third most common element in the universe, and the first anybody misses.",
        "Silizium" to "Silicon",
        "Sand, Glas, Rechner. Im Stern bleibt dafür etwa ein Tag Zeit." to
            "Sand, glass, computers. A star has about a day for it.",
        "Eisen" to "Iron",
        "Hier hört Fusion auf zu zahlen. Was jetzt noch wächst, wächst nach innen." to
            "This is where fusion stops paying. What grows now grows inwards.",
        "Wasserstoffzapfung" to "Hydrogen Tap",
        "Schöpft den dünnen Nebel zwischen den Sternen ab und drückt ihn nach innen." to
            "Skims the thin fog between the stars and presses it inwards.",
        "Protonenkette" to "Proton Chain",
        "Vier Protonen gehen hinein, ein Heliumkern kommt heraus. Der Rest wird Licht." to
            "Four protons go in, one helium nucleus comes out. The rest becomes light.",
        "Drei-Alpha-Ofen" to "Triple-Alpha Furnace",
        "Zwingt drei Heliumkerne zur selben Sekunde an denselben Ort." to
            "Forces three helium nuclei to the same place in the same second.",
        "Alpha-Prozess" to "Alpha Process",
        "Kohlenstoff fängt ein weiteres Helium ein. Der Stern merkt kaum, dass er brennt." to
            "Carbon catches another helium. The star barely notices it is burning.",
        "Sauerstoffbrand" to "Oxygen Burning",
        "Die vorletzte Stufe. Ab hier zählt der Stern in Tagen statt in Jahrmillionen." to
            "The second to last stage. From here the star counts in days, not millions of years.",
        "Siliziumbrand" to "Silicon Burning",
        "Vierundzwanzig Stunden, dann steht ein Eisenkern im Zentrum und alles ist vorbei." to
            "Twenty-four hours, then there is an iron core at the centre and it is over.",
        "Gesamtproduktion" to "Total production",
        "Masse pro Tipp" to "Mass per tap",
        "Offline-Ausbeute" to "Offline yield",
        "Kometenhäufigkeit" to "Comet frequency",
        "Singularitäten" to "Singularities",
        "Fusionstempo" to "Fusion rate",
        "Forschungstempo" to "Research rate",
    )


    /** What a collapse forges, and what can be welded out of it. */
    private val METALS: Map<String, String> = mapOf(
        "Gold" to "Gold",
        "Jedes Gramm davon war einmal in einem Stern, der schon tot war, als die Sonne anfing." to
            "Every gram of it was once in a star that was already dead when the sun began.",
        "Platin" to "Platinum",
        "Seltener als Gold, härter als Gold, und genauso wenig von hier." to
            "Rarer than gold, harder than gold, and just as little from around here.",
        "Uran" to "Uranium",
        "Das schwerste, was ohne Hilfe entsteht. Es zerfällt seitdem und ist immer noch da." to
            "The heaviest thing that forms unaided. It has been decaying ever since and is still here.",
        "Iridium" to "Iridium",
        "Liegt weltweit in genau einer Gesteinsschicht. Darunter Dinosaurier, darüber keine." to
            "Found worldwide in exactly one layer of rock. Dinosaurs below it, none above.",
        "Osmium" to "Osmium",
        "Das dichteste Ding, das man anfassen kann. Ein Würfel davon steht, wo man ihn hinstellt." to
            "The densest thing you can touch. A cube of it stays where you put it.",
        "Plutonium" to "Plutonium",
        "Kommt in der Natur praktisch nicht vor. In einem sterbenden Stern schon." to
            "Practically never occurs in nature. In a dying star it does.",
        "Elektrum" to "Electrum",
        "Gold und Platin, in einem Guss. Die Legierung, aus der die ersten Münzen waren." to
            "Gold and platinum, cast together. The alloy the first coins were made of.",
        "Schwerguss" to "Heavy Cast",
        "Osmium in Platin gelöst. Ein Barren davon ist nicht zu tragen, sondern zu schieben." to
            "Osmium dissolved in platinum. An ingot of it is not carried, it is pushed.",
        "Zündkern" to "Ignition Core",
        "Uran, mit Iridium umwickelt. Es brennt nicht, es fängt einfach an." to
            "Uranium wrapped in iridium. It does not burn, it simply begins.",
        "Sternstahl" to "Star Steel",
        "Gold und Iridium. Weich genug zum Formen, hart genug, um es danach zu bereuen." to
            "Gold and iridium. Soft enough to shape, hard enough to regret it afterwards.",
        "Endlegierung" to "Final Alloy",
        "Plutonium und Osmium. Es gibt keinen Grund, warum das halten sollte, und es hält." to
            "Plutonium and osmium. There is no reason it should hold, and it holds.",
    )

    /** The lab's upper storey, and the rules that run the game while nobody is looking. */
    private val LAB: Map<String, String> = mapOf(
        "Himmelskartierung" to "Sky Survey",
        "Acht Galaxien, endlich richtig vermessen. Sie waren die ganze Zeit schwerer." to
            "Eight galaxies, finally measured properly. They were heavier all along.",
        "Ephemeriden" to "Ephemerides",
        "Wo ein Trabant morgen steht, weiß man heute. Das allein bringt schon etwas." to
            "Where a satellite will be tomorrow is known today. That alone is worth something.",
        "Transmutation" to "Transmutation",
        "Dasselbe Eisen, mehr Gold. Die Alchemisten lagen nur um ein Sternenleben daneben." to
            "The same iron, more gold. The alchemists were only one stellar lifetime out.",
        "Verwaltungsapparat" to "Administration",
        "Irgendwer muss die Aufträge gegenzeichnen. Er nimmt einen Äon Bearbeitungsgebühr." to
            "Somebody has to countersign the contracts. They take one Aeon as a handling fee.",
        "Durchmusterung" to "Survey Catalogue",
        "Sechzehntausend Einträge, und die Liste fängt erst an." to
            "Sixteen thousand entries, and the list is only getting started.",
        "Rekursionssatz" to "Recursion Theorem",
        "Ein Universum, das eines baut, das eines baut. Irgendwo hört es auf, nur nicht hier." to
            "A universe that builds one that builds one. It stops somewhere, just not here.",

        // ---- automation
        "Kollektoren nachkaufen" to "Restock collectors",
        "Kauft den Kollektor mit der besten Rendite, solange genug übrig bleibt." to
            "Buys the collector with the best return as long as enough is left over.",
        "Upgrades kaufen" to "Buy upgrades",
        "Nimmt jede Verbesserung mit, sobald sie klein genug gegen dein Vermögen ist." to
            "Takes every improvement as soon as it is small against what you hold.",
        "Fusionskette ausbauen" to "Extend the fusion chain",
        "Baut die billigste Stufe aus, damit kein Ofen lange hungert." to
            "Extends the cheapest stage so no furnace goes hungry for long.",
        "Bahnen ausbauen" to "Extend the orbits",
        "Setzt Körper auf freie Bahnen und öffnet die nächste, wenn sie leicht drin ist." to
            "Puts bodies on free orbits and opens the next once it is easily afforded.",
        "Forschung anstoßen" to "Start research",
        "Lässt die Bank nie leer stehen." to "Never lets the bench stand empty.",
        "Kollabieren lassen" to "Collapse automatically",
        "Kollabiert, sobald Warten kaum noch etwas bringt — und hört danach von selbst auf." to
            "Collapses once waiting barely pays any more — and stops on its own afterwards.",
        "Rücklage" to "Reserve",
        "die Hälfte" to "half",
        "ein Zehntel" to "a tenth",
        "ein Hundertstel" to "a hundredth",
        "das billigste" to "the cheapest",
        "das teuerste leistbare" to "the dearest affordable",

        // ---- what a parked galaxy can be put on
        "Fördern" to "Produce",
        "Schickt herüber, was sie herstellt. Das aktive Universum merkt es sofort." to
            "Sends over what it makes. The active universe notices at once.",
        "Rechnen" to "Compute",
        "Stellt die Produktion ein und rechnet stattdessen. Zahlt in Äonen." to
            "Stops producing and computes instead. Pays in Aeons.",
        "Suchen" to "Search",
        "Durchkämmt sich selbst nach Losem und wirft es herüber." to
            "Combs itself for anything loose and throws it over.",
        "Graben" to "Dig",
        "Holt schwere Kerne aus dem eigenen Kern. Langsam, aber ohne Kollaps." to
            "Pulls heavy nuclei out of its own core. Slow, but without a collapse.",
    )

    /** What drifts past, what it grants, and the questions the sky asks. */
    private val EVENTS: Map<String, String> = mapOf(
        "Brocken" to "Boulder",
        "Fünfzehn Minuten Arbeit, auf einen Schlag." to "Fifteen minutes of work, all at once.",
        "Sternwind" to "Stellar Wind",
        "Alles läuft eine halbe Minute lang siebenfach." to
            "Everything runs sevenfold for half a minute.",
        "Splitterregen" to "Shard Rain",
        "Eine Minute lang zählt jeder Tipp hundertfach." to
            "For one minute every tap counts a hundredfold.",
        "Eiskern" to "Ice Core",
        "Drei Treffer, bis die Kruste bricht. Darunter eine dreiviertel Stunde Arbeit." to
            "Three hits to break the crust. Three quarters of an hour of work underneath.",
        "Glutkern" to "Ember Core",
        "Zwei Treffer, und danach brennt anderthalb Minuten lang alles fünfzehnfach." to
            "Two hits, and then everything burns fifteenfold for a minute and a half.",
        "Schub" to "Surge",
        "Klickrausch" to "Click Frenzy",
        "Feuersturm" to "Firestorm",

        "Sonnensturm" to "Solar Storm",
        "Eine Plasmawolke rollt heran. Du kannst sie einfangen oder in ihr surfen." to
            "A plasma cloud is rolling in. You can catch it or ride it.",
        "Einfangen" to "Catch it",
        "Die Ladung geht direkt in die Speicher." to "The charge goes straight into storage.",
        "Mitreiten" to "Ride it",
        "Alles läuft heiß, solange der Sturm anhält." to
            "Everything runs hot for as long as the storm lasts.",
        "Trümmerfeld" to "Debris Field",
        "Reste von etwas Großem, das hier einmal vorbeikam." to
            "What is left of something large that once came past here.",
        "Absammeln" to "Pick it clean",
        "Langsam, gründlich, und die Ausbeute ist beträchtlich." to
            "Slow, thorough, and the yield is considerable.",
        "Durchpflügen" to "Plough through",
        "Jeder Griff trifft etwas. Für kurze Zeit." to
            "Every grab hits something. For a short while.",
        "Für ein paar Minuten steht etwas Schweres genau richtig." to
            "For a few minutes something heavy is lined up exactly right.",
        "Durchleiten" to "Channel it",
        "Die gebündelte Materie fällt dir in den Schoß." to
            "The focused matter drops into your lap.",
        "Fokussieren" to "Focus it",
        "Die ganze Anlage arbeitet durch die Linse." to
            "The whole operation works through the lens.",
    )


    /** The stories told across several stations, and the third of them that answers back. */
    private val CHAINS: Map<String, String> = mapOf(
        "Stille" to "Quiet",
        "Nichts passiert. Das ist selten genug, um es zu nutzen." to
            "Nothing is happening. That is rare enough to make use of.",
        "Aufräumen" to "Tidy up",
        "Ein Rest, den bisher niemand eingesammelt hat." to
            "A remainder nobody has collected yet.",
        "Konzentrieren" to "Concentrate",
        "Ohne Ablenkung sitzt jeder Griff." to "With nothing to distract you, every grab lands.",

        // ---- the signal
        "Das Signal" to "The Signal",
        "Ein Muster im Rauschen" to "A pattern in the noise",
        "Zwischen zwei Frequenzen liegt etwas, das sich alle elf Sekunden wiederholt. Rauschen macht das nicht." to
            "Between two frequencies there is something that repeats every eleven seconds. Noise does not do that.",
        "Zuhören" to "Listen",
        "Die Antennen bleiben, wo sie sind. Man verrät nichts, indem man wartet." to
            "The antennas stay where they are. Waiting gives nothing away.",
        "Antworten" to "Answer",
        "Dasselbe Muster zurück, einmal. Mehr braucht es nicht, um gesehen zu werden." to
            "The same pattern back, once. It takes no more than that to be seen.",
        "Es wiederholt sich" to "It repeats",
        "Vier Tage lang derselbe Abstand, dieselbe Länge. Und dann, einmal, ein Abstand zu viel." to
            "Four days of the same interval, the same length. And then, once, one interval too many.",
        "Aufzeichnen" to "Record it",
        "Alles mitschneiden und später verstehen. Wenn es ein Später gibt." to
            "Capture all of it and understand it later. If there is a later.",
        "Abschalten" to "Shut it down",
        "Manche Dinge hören auf, wenn man aufhört hinzusehen. Manche nicht." to
            "Some things stop when you stop looking. Some do not.",
        "Etwas antwortet" to "Something answers",
        "Dasselbe Muster kommt zurück — aber schneller, als Licht die Strecke schafft. Es hat nicht auf dich gewartet. Es war schon näher." to
            "The same pattern comes back — faster than light could cover the distance. It was not waiting for you. It was already closer.",
        "Weitersenden" to "Keep transmitting",
        "Wenn es ohnehin kommt, ist es besser, es kommt als etwas Erwartetes." to
            "If it is coming anyway, better that it arrives as something expected.",
        "Funkstille" to "Radio silence",
        "Alle Sender aus. Die Anlage steht still, und die Speicher füllen sich." to
            "Every transmitter off. The operation stands still and the stores fill up.",
        "Das Archiv" to "The Archive",
        "Die Aufzeichnung ist vollständig. Sie beschreibt, in einer Sprache aus Abständen, den Aufbau von etwas sehr Schwerem." to
            "The recording is complete. In a language made of intervals, it describes how to build something very heavy.",
        "Nachbauen" to "Build it",
        "Es steht alles da. Es steht sogar da, in welcher Reihenfolge." to
            "It is all written down. Even the order is written down.",
        "Wegschließen" to "Lock it away",
        "Man muss nicht jede Anleitung befolgen, die man geschenkt bekommt." to
            "You do not have to follow every set of instructions you are given.",
        "Es kommt näher" to "It is getting closer",
        "Kein Objekt auf keinem Radar. Nur das Muster, jeden Tag lauter, und die Instrumente, die schwerer werden, als sie sein dürften." to
            "No object on any radar. Only the pattern, louder every day, and instruments growing heavier than they ought to be.",
        "Entgegengehen" to "Go to meet it",
        "Alles, was fliegt, in eine Richtung. Was auch immer das wird, es wird schnell." to
            "Everything that flies, in one direction. Whatever this turns into, it will be quick.",
        "Abwarten" to "Wait it out",
        "Es kommt so oder so. Bis dahin läuft die Produktion weiter." to
            "It is coming either way. Until then, production carries on.",

        // ---- the passengers
        "Die Passagiere" to "The Passengers",
        "Nicht allein angekommen" to "It did not arrive alone",
        "In den Rissen des Brockens sitzt etwas, das Wärme abgibt. Wenig, aber regelmäßig." to
            "Something in the cracks of the rock is giving off heat. Not much, but regularly.",
        "Einsammeln" to "Collect them",
        "Behälter, Etiketten, Katalognummern. So macht man das." to
            "Containers, labels, catalogue numbers. This is how it is done.",
        "In Ruhe lassen" to "Leave them alone",
        "Der Brocken kommt auf ein eigenes Feld, und das Feld bekommt einen Zaun." to
            "The rock gets a field of its own, and the field gets a fence.",
        "Im Labor" to "In the lab",
        "Sie halten Vakuum aus, Kälte, harte Strahlung. Was sie nicht aushalten, ist, einzeln zu sein." to
            "They survive vacuum, cold, hard radiation. What they do not survive is being alone.",
        "Aufschließen" to "Open them up",
        "Was drin ist, ist mehr wert als das, was sie tun. Vermutlich." to
            "What is inside is worth more than what they do. Presumably.",
        "Zurückbringen" to "Take them back",
        "Dahin, wo sie herkamen, samt der Wärme, die sie brauchen." to
            "Back where they came from, along with the warmth they need.",
        "Das Feld" to "The Field",
        "Sie sind mehr geworden. Nicht schnell — aber sie haben angefangen, sich am Zaun entlang anzuordnen." to
            "There are more of them. Not quickly — but they have begun to arrange themselves along the fence.",
        "Zusehen" to "Watch",
        "Wer zusieht, lernt. Wer eingreift, bekommt etwas anderes zu sehen." to
            "Watch and you learn. Interfere and you get to see something else.",
        "Umsiedeln" to "Relocate them",
        "Ein größeres Feld, weiter draußen. Es ist genug Platz da." to
            "A bigger field, further out. There is room enough.",

        // ---- the structure
        "Der Bau" to "The Structure",
        "Eine Schale" to "A shell",
        "Etwas umschließt den Stern zu zwei Dritteln. Es ist alt, es ist leer, und es ist nicht abgestürzt." to
            "Something encloses two thirds of the star. It is old, it is empty, and it has not fallen in.",
        "Betreten" to "Go inside",
        "Die Schleusen öffnen sich, als hätten sie darauf gewartet." to
            "The airlocks open as though they had been waiting for it.",
        "Abtragen" to "Strip it",
        "Was da hängt, ist Material. Sehr viel Material." to
            "What is hanging there is material. A very great deal of material.",
        "Innen" to "Inside",
        "Gänge für etwas, das größer war als du und dieselbe Schwerkraft mochte. Kein Staub. Irgendwer hält hier sauber." to
            "Corridors for something larger than you that liked the same gravity. No dust. Somebody keeps this place clean.",
        "Weitergehen" to "Keep going",
        "Bis dahin, wo die Gänge aufhören, sich zu wiederholen." to
            "As far as the point where the corridors stop repeating.",
        "Einziehen" to "Move in",
        "Die Anlage passt hinein. Sie passt sogar auffällig gut hinein." to
            "The operation fits inside. It fits conspicuously well, in fact.",
        "Der Abbau" to "The Stripping",
        "Das dritte Drittel fehlte nicht. Es war abgetragen worden, von jemandem, der genauso angefangen hat wie du." to
            "The third third was not missing. It had been stripped, by somebody who started exactly as you did.",
        "Weitermachen" to "Carry on",
        "Der Vorgänger hat aufgehört. Das muss keinen Grund gehabt haben." to
            "The one before stopped. There need not have been a reason.",
        "Stehen lassen" to "Leave it standing",
        "Was übrig ist, bleibt, wo es ist. Es steht dort länger als jede Anlage." to
            "What is left stays where it is. It will stand there longer than any operation.",
    )

    /** What turns up on the catalogue ladder, and the three ways to answer it. */
    private val FINDS: Map<String, String> = mapOf(
        "Ein Signal ohne Absender" to "A signal with no sender",
        "Eine Wiederholung, sauber und regelmäßig, aus einer Richtung ohne Sterne." to
            "A repetition, clean and regular, from a direction with no stars in it.",
        "Es wiederholt sich alle elf Sekunden. Elf ist keine Zahl, die zufällig entsteht." to
            "It repeats every eleven seconds. Eleven is not a number that happens by chance.",
        "Eine leere Hülle" to "An empty shell",
        "Ein Körper mit einer Kruste und ohne Inneres. Etwas hat ihn von innen ausgeräumt." to
            "A body with a crust and no inside. Something cleared it out from within.",
        "Die Kruste hält. Wer auch immer das getan hat, war ordentlich dabei." to
            "The crust is holding. Whoever did this was tidy about it.",
        "Zwei, die sich umkreisen" to "Two that circle each other",
        "Zwei identische Körper, gleiche Masse, gleiche Zusammensetzung, gleiche Farbe." to
            "Two identical bodies: same mass, same composition, same colour.",
        "Identisch bis auf die Nachkommastelle. So etwas entsteht nicht, so etwas wird geteilt." to
            "Identical to the decimal place. Things like this do not form, they are divided.",
    )

    /** Everything, in one map. Areas are added here as each one is finished. */
    val EN: Map<String, String> = buildMap {
        putAll(UI)
        putAll(TIERS)
        putAll(COLLECTORS)
        putAll(RESEARCH)
        putAll(INVESTMENTS)
        putAll(PATHS)
        putAll(UPGRADES)
        putAll(ROLES)
        putAll(PRESTIGE)
        putAll(FUSION)
        putAll(METALS)
        putAll(LAB)
        putAll(EVENTS)
        putAll(CHAINS)
        putAll(FINDS)
    }
}
