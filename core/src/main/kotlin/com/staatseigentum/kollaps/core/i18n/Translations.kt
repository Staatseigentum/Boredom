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


    /** The rest of the catalogue finds, and the three answers each one offers. */
    private val FINDS_MORE: Map<String, String> = mapOf(
        "Ein kalter Fleck" to "A cold spot",
        "Ein Gebiet, in dem die Hintergrundstrahlung fehlt. Nicht schwächer — sie fehlt." to
            "A region where the background radiation is absent. Not weaker — absent.",
        "Dahinter ist nichts. Nicht Dunkelheit, sondern die Abwesenheit von Dahinter." to
            "There is nothing behind it. Not darkness — the absence of behind.",
        "Etwas, das zu langsam fällt" to "Something falling too slowly",
        "Ein Brocken auf einer Bahn, die er bei seiner Masse nicht halten dürfte." to
            "A rock on an orbit it has no business holding at that mass.",
        "Entweder ist die Masse falsch oder die Gravitation. Beides wäre unangenehm." to
            "Either the mass is wrong or gravity is. Both would be awkward.",
        "Ein Katalogeintrag, den es schon gibt" to "A catalogue entry that already exists",
        "Dieselbe Kennung, dieselben Werte, zweimal vergeben. Einer davon ist neu." to
            "The same designation, the same figures, issued twice. One of them is new.",
        "Der Katalog hat sich nicht geirrt. Es sind zwei." to
            "The catalogue did not make a mistake. There are two.",
        "Älter als es sein dürfte" to "Older than it ought to be",
        "Die Zusammensetzung passt zu einem Universum, das es noch nicht gegeben hat." to
            "The composition matches a universe that has not existed yet.",
        "Es ist nicht von hier. Es ist von vorher." to
            "It is not from here. It is from before.",
        "Der stille Bereich" to "The quiet stretch",
        "Vierhundert Sprossen ohne einen einzigen Fund. Statistisch beinahe unmöglich." to
            "Four hundred rungs without a single find. Statistically all but impossible.",
        "Nichts zu finden ist auch ein Befund. Jemand hat hier aufgeräumt." to
            "Finding nothing is a finding too. Somebody tidied up here.",
        "Auswerten" to "Analyse it",
        "Zerlegen, vermessen, aufschreiben. Zahlt ein Äon." to
            "Take it apart, measure it, write it down. Pays one Aeon.",
        "Anzapfen" to "Tap it",
        "Nehmen, was drin ist. Wirkt bis zum nächsten Kollaps und nicht darüber hinaus." to
            "Take what is inside. Lasts until the next collapse and no further.",
        "Notieren und weiterziehen. Bringt nichts außer dem Eintrag." to
            "Note it down and move on. Brings nothing but the entry.",
    )

    /** What the game asks of you: the runs under a rule, and the rotating contracts. */
    private val CHALLENGES: Map<String, String> = mapOf(
        "Handarbeit" to "By Hand",
        "Die ganze Flotte steht still. Was du willst, holst du dir selbst." to
            "The whole fleet stands idle. What you want, you fetch yourself.",
        "Nichtstun" to "Doing Nothing",
        "Nimm die Hände weg. Einen Staubfänger kriegst du geschenkt — der Rest wächst ohne dich." to
            "Hands off. You get one Dust Catcher for free — the rest grows without you.",
        "Sprint" to "Sprint",
        "Bis zum Saturn, in fünfundvierzig Minuten. Die Uhr läuft nur, wenn du spielst." to
            "To Saturn, in forty-five minutes. The clock only runs while you play.",
        "Halbe Kraft" to "Half Power",
        "Alles bringt die Hälfte. Bis zum Schwarzen Loch trotzdem." to
            "Everything yields half. To the Black Hole all the same.",
        "Rohbau" to "Shell and Core",
        "Der Upgrade-Laden ist zu. Mehr Maschinen ja, bessere nein." to
            "The upgrade shop is shut. More machines yes, better ones no.",
        "Allein" to "Alone",
        "Nichts bleibt auf einer Bahn. Was du schaffst, schaffst du ohne Trabanten." to
            "Nothing stays in orbit. What you manage, you manage without satellites.",
        "Eile" to "Haste",
        "Bis zur Sonne, in neunzig Minuten. Die Uhr läuft nur, wenn du spielst." to
            "To the Sun, in ninety minutes. The clock only runs while you play.",
        "Askese" to "Austerity",
        "Kein einziges Upgrade, den ganzen Weg bis zur Sonne. Nur Maschinen und Geduld." to
            "Not one upgrade, the whole way to the Sun. Machines and patience only.",
        "Serienstopp" to "Line Stoppage",
        "Jede Maschine zählt einzeln. Die Fertigungsstraßen haben Betriebsferien." to
            "Every machine counts on its own. The assembly lines are on holiday.",
        "Wachdienst" to "Night Watch",
        "Zugeklappt läuft nichts weiter. Was du willst, musst du sehen." to
            "Nothing runs while it is shut. What you want, you have to watch.",
        "Kalte Kette" to "Cold Chain",
        "Kein Ofen brennt. Schwere Kerne musst du diesmal woanders herbekommen." to
            "No furnace is lit. You will have to get heavy nuclei elsewhere this time.",
        "Einsames Universum" to "Lonely Universe",
        "Die anderen Galaxien schweigen. Dieses hier schafft es allein oder gar nicht." to
            "The other galaxies are silent. This one manages alone or not at all.",
        "Handbetrieb" to "Manual Operation",
        "Noch einmal ohne Flotte, und diesmal bis zur Sonne." to
            "Once more without a fleet, and this time to the Sun.",
        "Viertelkraft" to "Quarter Power",
        "Alles bringt ein Viertel. Bis zum Schwarzen Loch trotzdem." to
            "Everything yields a quarter. To the Black Hole all the same.",
        "Hetze" to "Chase",
        "Bis zum Schwarzen Loch, in zwei Stunden. Die Uhr läuft nur, wenn du spielst." to
            "To the Black Hole, in two hours. The clock only runs while you play.",
        "Rohbau II" to "Shell and Core II",
        "Der Laden bleibt zu, den ganzen Weg bis zum Schwarzen Loch." to
            "The shop stays shut, the whole way to the Black Hole.",

        // ---- the contract table
        "Fünfzig Sprossen über dem Tor" to "Fifty rungs above the gate",
        "Zwei Legierungen schmieden" to "Forge two alloys",
        "Fünf Kollapse" to "Five collapses",
        "Tausend Maschinen gleichzeitig" to "A thousand machines at once",
        "Drei Katalogfunde beantworten" to "Answer three catalogue finds",
        "Vier Galaxien gleichzeitig am Rechnen" to "Four galaxies computing at once",
        "Eine Herausforderung bestehen" to "Beat one challenge",
        "Sechs Bahnen gleichzeitig besetzt" to "Six orbits occupied at once",
        "Drei Projekte durchziehen" to "See three projects through",
        "Fünfhundert Gramm Gold im Lager" to "Five hundred grams of gold in store",
    )

    /** The chronicle: one line per rung, and a handful for each kind of reset. */
    private val LORE: Map<String, String> = mapOf(
        // ---- the units under the numbers
        "unendlich" to "infinite",
        "%s Sek" to "%s s",
        "%s Min" to "%s min",
        "%s Std" to "%s h",
        "%s Std %s Min" to "%s h %s min",
        "%s Tage" to "%s days",
        "%s Tage %s Std" to "%s days %s h",
        "Namen" to "Names",
        "Wissenschaftlich" to "Scientific",
        "Kurzform" to "Compact",

        "%s. Kollaps" to "Collapse %s",
        "%s. Urknall" to "Big Bang %s",
        "Es fängt mit einem Stein an, der niemandem gehört. Du legst die Hand darauf." to
            "It starts with a stone that belongs to nobody. You put your hand on it.",
        "Groß genug, dass ihn jemand aufschreibt. Ein Name, eine Nummer, ein Eintrag." to
            "Big enough for somebody to write down. A name, a number, an entry.",
        "Rund. Nicht weil es jemand so wollte, sondern weil Schwerkraft keine Ecken mag." to
            "Round. Not because anybody wanted it that way, but because gravity dislikes corners.",
        "Grau und still. Und trotzdem hebt unten jemand den Kopf und sieht hinauf." to
            "Grey and silent. And still somebody down there lifts their head and looks up.",
        "Ein Planet, offiziell. Verbrannt auf der einen Seite, erfroren auf der anderen." to
            "A planet, officially. Burnt on one side, frozen on the other.",
        "Es regnet. Nicht Wasser, aber es regnet, und das ist mehr, als die meisten haben." to
            "It rains. Not water, but it rains, which is more than most have.",
        "Rost, soweit man sehen kann. Irgendwo darin ein Rover, der aufgehört hat zu funken." to
            "Rust as far as you can see. Somewhere in it a rover that stopped transmitting.",
        "Von weitem der schönste Punkt am Morgenhimmel. Von nahem 460 Grad und Schwefelsäure." to
            "From afar the loveliest point in the morning sky. Close up, 460 degrees and sulphuric acid.",
        "Blau. Der einzige Ort, von dem irgendjemand je zurückkommen wollte." to
            "Blue. The only place anybody ever wanted to come back from.",
        "Doppelt so schwer. Wer hier aufsteht, meint es ernst." to
            "Twice as heavy. Anybody who stands up here means it.",
        "Kein Boden mehr. Nur noch Wetter, das nach unten immer dichter wird." to
            "No ground any more. Only weather, getting denser the further down you go.",
        "Er liegt auf der Seite. Irgendetwas hat ihn getroffen, vor sehr langer Zeit." to
            "It lies on its side. Something hit it, a very long time ago.",
    )


    /** The rest of the chronicle: the upper ladder, the collapses and the big bangs. */
    private val LORE_MORE: Map<String, String> = mapOf(
        "Die Ringe sind jünger als die Dinosaurier und werden sie nicht lange überleben." to
            "The rings are younger than the dinosaurs and will not outlast them by much.",
        "Ein Sturm, in den die Erde zweimal hineinpasst, und er dreht sich seit Jahrhunderten." to
            "A storm you could fit Earth into twice, and it has been turning for centuries.",
        "So nah an seinem Stern, dass er von unten glüht. Jahre dauern hier ein paar Tage." to
            "So close to its star that it glows from below. Years here last a few days.",
        "Fast ein Stern. Es fehlt nicht viel, und es wird nie reichen." to
            "Almost a star. Not much is missing, and it will never be enough.",
        "Jetzt brennt es. Sparsam, aber es brennt, und es wird länger brennen als alles andere." to
            "Now it burns. Frugally, but it burns, and it will burn longer than anything else.",
        "Ein ganz gewöhnlicher gelber Zwerg. Acht Planeten halten ihn für den Mittelpunkt." to
            "A perfectly ordinary yellow dwarf. Eight planets take it for the centre.",
        "Zu heiß, zu hell, zu schnell. Wer so brennt, hat es in einer Million Jahren hinter sich." to
            "Too hot, too bright, too fast. Burn like that and it is over in a million years.",
        "Die Erdbahn passt bequem hinein. Was darin war, ist längst nicht mehr da." to
            "Earth's orbit fits comfortably inside. Whatever was in it is long gone.",
        "Größer geht nicht. Was jetzt noch dazukommt, bläst er wieder ab." to
            "It cannot get bigger. Anything added now gets blown straight off again.",
        "Der Rest. Erdgroß, weiß, und er kühlt aus — für länger, als das Universum alt ist." to
            "What is left. Earth-sized, white, and cooling — for longer than the universe is old.",
        "Ein Teelöffel wiegt ein Gebirge. Die Oberfläche ist einen Zentimeter hoch und aus Eisen." to
            "A teaspoon weighs a mountain range. The surface is a centimetre tall and made of iron.",
        "Sein Feld würde dich aus tausend Kilometern in deine Atome zerlegen. Es ist nicht böse." to
            "Its field would take you apart into atoms from a thousand kilometres. It means nothing by it.",
        "Ab hier kommt nichts zurück. Nicht Licht, nicht Information, nicht du." to
            "Nothing comes back from here. Not light, not information, not you.",
        "Alles, was du gesammelt hast, fällt in sich zusammen. Übrig bleibt ein Punkt." to
            "Everything you gathered falls in on itself. What is left is a point.",
        "Beim zweiten Mal weißt du schon, was kommt. Es hilft nicht besonders." to
            "The second time you already know what is coming. It does not help much.",
        "Du fängst an, den Weg zu kennen. Die Steine liegen jedes Mal woanders." to
            "You are starting to know the way. The stones lie somewhere else every time.",
        "Irgendwann hört es auf, ein Verlust zu sein, und wird ein Handgriff." to
            "At some point it stops being a loss and becomes a movement of the hand.",
        "Ein Punkt ist keine Größe. Er ist eine Stelle, an der die Frage aufhört." to
            "A point is not a size. It is a place where the question stops.",
        "Was am Rand bleibt, lässt sich zählen. Mehr weiß niemand darüber." to
            "What stays at the edge can be counted. Nobody knows more about it than that.",
        "Du hast das jetzt oft genug gemacht, dass die Zahl selbst dir egal geworden ist." to
            "You have done this often enough that the number itself has stopped mattering.",
        "Und wieder von vorn. Es ist immer noch dasselbe Universum." to
            "And from the beginning again. It is still the same universe.",
        "Dieses Mal nicht der Körper. Das Universum. Alles davon, auf einmal." to
            "Not the body this time. The universe. All of it, at once.",
        "Ein zweites, aus denselben Regeln. Es kommt anders heraus, und das ist der Punkt." to
            "A second one, from the same rules. It comes out different, and that is the point.",
        "Du legst inzwischen fest, wie es wird, bevor es losgeht. Das ist kein kleiner Schritt." to
            "By now you decide how it turns out before it starts. That is not a small step.",
        "Irgendwann bleibt nur die Frage, warum überhaupt etwas ist und nicht nichts." to
            "In the end only one question is left: why there is anything at all rather than nothing.",
    )

    /** Achievements: what you did, and the dry line under it. */
    private val ACHIEVEMENTS: Map<String, String> = mapOf(
        "Erster Blick nach oben" to "First Look Up",
        "Grau, still, voller Krater." to "Grey, silent, full of craters.",
        "Blauer Punkt" to "Blue Dot",
        "Der einzige Ort mit Kaffee." to "The only place with coffee.",
        "Schwerer Boden" to "Heavy Ground",
        "Doppelt so schwer wie zuhause." to "Twice as heavy as home.",
        "Beringt" to "Ringed",
        "Der einzige Planet mit gutem Schmuck." to "The only planet with decent jewellery.",
        "Hauptreihe" to "Main Sequence",
        "Ganz normal, und trotzdem alles." to "Perfectly ordinary, and still everything.",
        "So groß es geht" to "As Big As It Gets",
        "Größer wird ein Stern nicht." to "A star does not get bigger than this.",
        "Heiße Asche" to "Hot Ash",
        "Was übrig bleibt, wenn ein Stern fertig ist." to
            "What is left when a star has finished.",
        "Ein Teelöffel" to "One Teaspoon",
        "Wiegt so viel wie ein Gebirge." to "Weighs as much as a mountain range.",
        "Unter Spannung" to "Under Tension",
        "Ein Feld, das dich aus tausend Kilometern zerlegt." to
            "A field that takes you apart from a thousand kilometres.",
        "Ende der Leiter" to "End of the Ladder",
        "Ab hier kommt nichts mehr zurück." to "Nothing comes back from here.",
        // The one achievement for doing nothing at all, which is the joke. It is written in
        // Ruhrgebiet dialect, so the English is written in something with the same shrug in it.
        "Was war dat jetze?" to "The 'ell was that?",
        "Das war genau so wenig wert wie eh und je" to
            "Worth exactly as little as it has always been",
        "Angefasst" to "Touched",
        "Hundert Mal auf einen Stein." to "A hundred times on a rock.",
        "Hartnäckig" to "Persistent",
        "Tausend Mal. Der Stein merkt nichts." to "A thousand times. The rock notices nothing.",
        "Zwanghaft" to "Compulsive",
        "Zehntausend. Vielleicht mal Pause?" to "Ten thousand. Maybe take a break?",
        "Der Finger" to "The Finger",
        "Fünfzigtausend Mal. Respekt und Sorge." to "Fifty thousand times. Respect, and concern.",
        "Erste Million" to "First Million",
        "Eine Million Kilogramm, zusammengekratzt." to "A million kilograms, scraped together.",
        "Milliardenschwer" to "Billion Heavy",
        "Reicht für einen kleinen Mond." to "Enough for a small moon.",
        "Billionär" to "Trillionaire",
        "Die Zahl hat aufgehört, etwas zu bedeuten." to "The number has stopped meaning anything.",
        "Jenseits" to "Beyond",
        "Niemand kann sich das noch vorstellen." to "Nobody can picture this any more.",
        "Staubsammlung" to "Dust Collection",
        "Fünfundzwanzig Netze im Vakuum." to "Twenty-five nets in vacuum.",
        "Staubfabrik" to "Dust Factory",
        "Hundert Netze. Es fängt an, sich zu lohnen." to
            "A hundred nets. It is starting to pay off.",
        "Schwarm" to "Swarm",
        "Fünfzig Drohnen, die nie schlafen." to "Fifty drones that never sleep.",
        "Halber Ring" to "Half a Ring",
        "Zehn Spiegel um einen Stern." to "Ten mirrors around a star.",
        "Der erste Augenblick, eingefangen." to "The first moment, captured.",
        "Ausgebaut" to "Fully Built",
        "Jede Sorte 2500 Mal. Es passt buchstäblich nichts mehr rein." to
            "Two and a half thousand of every kind. Literally nothing else fits.",
        "Verwoben" to "Interwoven",
        "Fünf Weber an derselben Kausalkette." to "Five weavers on the same causal chain.",
        "Das letzte Gerät" to "The Last Device",
        "Sammelt ein, was übrig sein wird." to "Collects what will be left.",
        "Rückwärts gemahlen" to "Milled Backwards",
        "Zehn Mühlen, die die Unordnung zurückdrehen." to
            "Ten mills turning the disorder back.",
        "Aus dem Nichts gepresst" to "Pressed Out of Nothing",
        "Fünf Pressen am leersten Vakuum, das es gibt." to
            "Five presses on the emptiest vacuum there is.",
        "Vor dem Anfang" to "Before the Beginning",
        "Einmal zurückgegriffen bis vor den ersten Augenblick." to
            "Reached back once, past the first moment.",
        "Ausverkauft" to "Sold Out",
        "Jedes Prestige-Upgrade gekauft und jedes Konto voll. Der Laden hat nichts mehr." to
            "Every prestige upgrade bought and every account full. The shop has nothing left.",
        "Jenseits der Währung" to "Past the Currency",
        "Jedes Äonen-Upgrade gekauft. Die Galaxien zahlen jetzt auf ein volles Konto ein." to
            "Every Aeon upgrade bought. The galaxies now pay into a full account.",
        "Es ging weiter" to "It Carried On",
        "Ein Universum überlebt seinen eigenen Urknall und arbeitet weiter." to
            "A universe survives its own Big Bang and keeps working.",
        "Halber Himmel" to "Half a Sky",
        "Vier Galaxien am Laufen. Die Hälfte der Arbeit macht schon jemand anders." to
            "Four galaxies running. Somebody else is already doing half the work.",
        "Voller Himmel" to "Full Sky",
        "Acht Galaxien, alle besetzt. Über dem Schwarzen Loch geht es jetzt weiter." to
            "Eight galaxies, all occupied. Above the Black Hole it carries on.",
        "Vier Ausrichtungen" to "Four Leanings",
        "Von jedem Pfad eine Galaxie. Der Himmel tut vier Dinge gleichzeitig." to
            "One galaxy of every path. The sky does four things at once.",
        "Katalogisiert" to "Catalogued",
        "Die erste Sprosse über dem Schwarzen Loch. Ab hier haben die Körper Nummern." to
            "The first rung above the Black Hole. From here the bodies have numbers.",
        "Hundert Nummern weiter" to "A Hundred Numbers On",
        "Hundert Katalogsprossen. Der Meteorit sieht aus wie am Anfang und wiegt es nicht." to
            "A hundred catalogue rungs. The meteoroid looks the same as at the start and does not weigh it.",
        "Durchgezählt" to "Counted Through",
        "Einen ganzen Körper durch alle Kennungen von AA bis ZZ getrieben." to
            "Drove one whole body through every designation from AA to ZZ.",
        "Zusammengeschweißt" to "Welded Together",
        "Die erste Legierung. Das Metall aus dem Kollaps ist endlich für etwas gut." to
            "The first alloy. The metal from the collapse is finally good for something.",
        "Vollständige Schmiede" to "Complete Forge",
        "Jede Legierung geschmiedet. Es gibt nichts mehr zu verschweißen." to
            "Every alloy forged. There is nothing left to weld.",
        "Auftragslage" to "Order Book",
        "Zwanzig Aufträge abgegeben. Es gab immer etwas zu tun." to
            "Twenty contracts handed in. There was always something to do.",
        "Zwei Galaxien zu einer gemacht. Sie trägt jetzt beide Ausrichtungen." to
            "Made two galaxies into one. It now carries both leanings.",
    )


    /** The rest of the achievement shelf. */
    private val ACHIEVEMENTS_MORE: Map<String, String> = mapOf(
        "Vollbeschäftigung" to "Full Employment",
        "Jede der vier Aufgaben läuft irgendwo am Himmel." to
            "All four jobs are running somewhere in the sky.",
        "Vermessen" to "Surveyed",
        "Zehn Katalogfunde beantwortet. Der Katalog wird länger als die Leiter." to
            "Ten catalogue finds answered. The catalogue is getting longer than the ladder.",
        "Eine Galaxie vollständig ausgebaut. Sie ist besser als an dem Tag, an dem sie endete." to
            "One galaxy built out completely. It is better than on the day it ended.",
        "Nicht angerührt" to "Left Untouched",
        "Fünf Funde in Ruhe gelassen. Manches ist mehr wert, wenn man es stehen lässt." to
            "Five finds left alone. Some things are worth more if you leave them standing.",
        "Katalogflotte" to "Catalogue Fleet",
        "Von jeder Maschine der Katalogleiter mindestens eine." to
            "At least one of every machine on the catalogue ladder.",
        "Vollständig" to "Complete",
        "Von jedem Kollektor mindestens einen." to "At least one of every collector.",
        "Aufgerüstet" to "Upgraded",
        "Zehn Verbesserungen gekauft." to "Ten improvements bought.",
        "Durchoptimiert" to "Fully Optimised",
        "Vierzig Verbesserungen in einem Lauf." to "Forty improvements in a single run.",
        "Erwischt" to "Caught",
        "Der erste Komet, rechtzeitig getippt." to "The first comet, tapped in time.",
        "Geübtes Auge" to "Practised Eye",
        "Fünfundzwanzig davon." to "Twenty-five of them.",
        "Kometenjäger" to "Comet Hunter",
        "Hundert. Du wartest inzwischen darauf." to "A hundred. By now you are waiting for them.",
        "Erster Kollaps" to "First Collapse",
        "Alles hergegeben für einen Neuanfang." to "Gave up everything for a fresh start.",
        "Wiederholungstäter" to "Repeat Offender",
        "Fünf Universen später." to "Five universes later.",
        "Gesammelte Enden" to "Collected Endings",
        "Fünfzig Singularitäten besessen." to "Held fifty singularities.",
        "Vorbereitet" to "Prepared",
        "Fünf Prestige-Upgrades gekauft." to "Five prestige upgrades bought.",
        "Angelegt" to "Invested",
        "Zehn Stufen in Investitionen gesteckt." to "Ten levels put into investments.",
        "Vermögensverwaltung" to "Asset Management",
        "Fünfzig Stufen. Die Singularitäten arbeiten für dich." to
            "Fifty levels. The singularities are working for you.",
        "Freiwillig schwerer" to "Harder on Purpose",
        "Eine Herausforderung bestanden." to "One challenge beaten.",
        "Alles mitgenommen" to "Took the Lot",
        "Jede Herausforderung bestanden." to "Every challenge beaten.",
        "Von vorn, wirklich" to "From the Start, Really",
        "Ein ganzes Universum weggeworfen." to "Threw away a whole universe.",
        "Serientäter" to "Serial Offender",
        "Fünf Universen. Keines davon vermisst." to "Five universes. None of them missed.",
        "Zeitlos" to "Timeless",
        "Jedes Äonen-Upgrade gekauft." to "Every Aeon upgrade bought.",
        "Aufgestellt" to "Set Up",
        "Jeder freie Platz mit einer Ausrichtung belegt." to
            "Every free slot filled with a role.",
        "Koordiniert" to "Coordinated",
        "Ein Kollektor, der nicht mehr selbst arbeitet, sondern alle anderen antreibt." to
            "One collector that no longer works itself but drives all the others.",
        "Erste Bahn" to "First Orbit",
        "Etwas kreist um dich." to "Something is circling you.",
        "Vollbesetzt" to "Fully Occupied",
        "Auf jeder Bahn steht ein Körper." to "There is a body on every orbit.",
        "Im Gleichschritt" to "In Step",
        "Zwei Trabanten in Resonanz — dieselbe Kraft an derselben Stelle, jedes Mal." to
            "Two satellites in resonance — the same pull at the same place, every time.",
        "Gezündet" to "Ignited",
        "Die erste Protonenkette läuft." to "The first proton chain is running.",
        "Aus Sternen gemacht" to "Made of Stars",
        "Kohlenstoff im Kern. Wie du." to "Carbon in the core. Like you.",
        "Eisenkern" to "Iron Core",
        "Weiter geht es nicht. Genau darum geht es." to
            "It goes no further. That is exactly the point.",
        "Ganze Kette" to "Whole Chain",
        "Jede Stufe der Fusion läuft gleichzeitig." to "Every fusion stage running at once.",
        "Sternenstaub" to "Stardust",
        "Das erste Gold, aus dem Eisen eines gestorbenen Kerns." to
            "The first gold, out of the iron of a dead core.",
        "Das schwerste von selbst" to "The Heaviest Unaided",
        "Uran. Schwerer geht es ohne fremde Hilfe nicht." to
            "Uranium. It gets no heavier without help.",
        "Schatzkammer" to "Treasury",
        "Tausend Einheiten schwerer Elemente, über alle Universen hinweg." to
            "A thousand units of heavy elements, across every universe.",
        "Erste Erkenntnis" to "First Finding",
        "Ein Projekt zu Ende gewartet." to "Waited one project out.",
        "Laborbetrieb" to "Lab Running",
        "Fünf Projekte abgeschlossen." to "Five projects finished.",
        "Ausgeforscht" to "Fully Researched",
        "Der ganze Baum steht." to "The whole tree is standing.",
        "Erster Meilenstein" to "First Milestone",
        "Fünfundzwanzig Stück von einer Sorte." to "Twenty-five of one kind.",
        "Zweihundert Stück von einer Sorte — acht Meilensteine auf einem Kollektor." to
            "Two hundred of one kind — eight milestones on a single collector.",
        "Mit bloßen Händen" to "With Bare Hands",
        "Erreiche den Merkur, ohne einen einzigen Kollektor zu besitzen." to
            "Reach Mercury without owning a single collector.",
        "Geduldig" to "Patient",
        "Eine Stunde Spielzeit auf der Uhr." to "One hour of play time on the clock.",
        "Marathon" to "Marathon",
        "Sechs Stunden Spielzeit." to "Six hours of play time.",
    )

    /** The accretion update: what falls in, what it builds, and what that makes you. */
    private val ACCRETION: Map<String, String> = mapOf(
        "Eis" to "Ice",
        "Silikat" to "Silicate",
        "Metall" to "Metal",
        "Staubschwade" to "Dust Plume",
        "Eisscherbe" to "Ice Shard",
        "Metallklumpen" to "Metal Nugget",
        "Teerbrocken" to "Tar Lump",
        "Felsbrocken" to "Boulder",
        "Kernfragment" to "Core Fragment",
        "Kern" to "Core",
        "Produktion, und die Fusion startet heißer" to
            "Production, and fusion starts hotter",
        "Mantel" to "Mantle",
        "Tippwert und Gravitation" to "Tap value and gravity",
        "Kruste" to "Crust",
        "Offline-Anteil und Materialausbeute" to "Offline share and material yield",
        "Geschichtet" to "Layered",
        "jung" to "young",
        "gereift" to "grown",
        "vollendet" to "whole",
        "Eisenkiesel" to "Iron Pebble",
        "Schwer für seine Größe. Ein Magnet würde es merken." to
            "Heavy for its size. A magnet would notice.",
        "Metallwelt" to "Metal World",
        "Der Kern ist größer als alles, was darauf liegt." to
            "The core is bigger than everything lying on it.",
        "Eisenherz" to "Iron Heart",
        "Fast nur Kern. Es klingt, wenn etwas darauf fällt." to
            "Almost all core. It rings when something falls on it.",
        "Geröllhaufen" to "Rubble Pile",
        "Lose zusammengehalten, aber es hält." to "Loosely held together, but it holds.",
        "Gesteinswelt" to "Rock World",
        "Ein Mantel, dick genug, um sich selbst zu tragen." to
            "A mantle thick enough to carry itself.",
        "Titanenfels" to "Titan Rock",
        "Stein bis fast nach unten. Nichts hier bewegt sich schnell." to
            "Stone almost all the way down. Nothing here moves quickly.",
        "Frostklumpen" to "Frost Clump",
        "Außen hart, innen noch nicht entschieden." to
            "Hard on the outside, undecided within.",
        "Eiswelt" to "Ice World",
        "Eine Kruste, unter der es sehr lange dunkel bleibt." to
            "A crust under which it stays dark for a very long time.",
        "Gletscherleib" to "Glacier Body",
        "Ein Panzer aus Eis, und darunter beinahe nichts." to
            "An armour of ice, and almost nothing underneath.",
        "Mischling" to "Mongrel",
        "Von allem ein bisschen. Noch keine Entscheidung getroffen." to
            "A little of everything. No decision made yet.",
        "Schichtwelt" to "Layered World",
        "Drei Schichten, keine davon im Weg." to "Three layers, none of them in the way.",
        "Dreiklang" to "Triad",
        "Kern, Mantel, Kruste — und keine davon zu knapp." to
            "Core, mantle, crust — and none of them scarce.",
    )


    /** The first five minutes, and the card that arrives with each new system. */
    private val GUIDE: Map<String, String> = mapOf(
        "Tipp den Körper an" to "Tap the body",
        "Jeder Tipp bringt Masse. Am Anfang ist das die einzige Quelle — und die Zahl oben zählt mit, was du hast." to
            "Every tap brings in mass. At the start it is the only source there is — and the number at the top keeps count of what you have.",
        "Kauf deinen ersten Kollektor" to "Buy your first collector",
        "Kollektoren sammeln Masse, ohne dass du etwas tust — auch dann, wenn die App zu ist. Ab hier läuft das Spiel auch ohne dich weiter." to
            "Collectors gather mass without you doing anything — even while the app is shut. From here the game keeps running without you.",
        "Und noch ein paar davon" to "And a few more of them",
        "Alle zehn Stück wird ein Kollektor dauerhaft besser. Der Knopf oben in der Flotte kauft gleich zehn oder hundert auf einmal, statt hundertmal zu tippen." to
            "Every ten copies a collector gets permanently better. The button at the top of the fleet buys ten or a hundred at once, so you do not have to tap a hundred times.",
        "Nimm eine zweite Sorte dazu" to "Add a second kind",
        "Weiter unten in der Flotte stehen teurere Maschinen. Eine neue Sorte bringt fast immer mehr als die zehnte Kopie der alten — und schaltet später eigene Upgrades frei." to
            "Further down the fleet there are dearer machines. A new kind is almost always worth more than a tenth copy of the old one — and unlocks upgrades of its own later.",
        "Kauf ein Upgrade" to "Buy an upgrade",
        "Kollektoren machen mehr Masse, Upgrades machen jede davon mehr wert — und sie sind es, die über einen Lauf den Unterschied ausmachen." to
            "Collectors make more mass; upgrades make each of them worth more — and it is the upgrades that decide how a run goes.",
        "Werde größer" to "Grow",
        "Genug Masse, und aus dem Gestein wird ein größerer Körper. Der Balken oben zeigt, wie weit es noch ist." to
            "Enough mass and the rock becomes a larger body. The bar at the top shows how far there is to go.",
        "Deine Flotte fliegt mit" to "Your fleet flies with you",
        "Was du kaufst, siehst du: die Maschinen kreisen um deinen Körper, eine Bahn je Sorte. Wer viel besitzt, sieht es, ohne in den Laden zu gehen." to
            "What you buy, you can see: the machines circle your body, one ring per kind. Own a lot and you can tell without opening the shop.",
        "Es läuft auch ohne dich" to "It runs without you",
        "Leg das Spiel ruhig weg. Beim Öffnen bekommst du die Zeit gutgeschrieben — ein Bericht sagt dir dann, was in der Zwischenzeit angefallen ist." to
            "Feel free to put the game down. You are credited for the time when you open it again — a report tells you what came in while you were away.",
        "Sieh dir den Kosmos an" to "Have a look at the Cosmos",
        "Dort steht alles, was nicht gekauft wird: der Kollaps, das Labor, die Automatik, deine Erfolge und die Einstellungen. Kein Grund zur Eile — aber gut zu wissen, wo es liegt." to
            "That is where everything you do not buy lives: the collapse, the lab, the automation, your achievements and the settings. No hurry — but worth knowing where it is.",
        "Der Kollaps kommt noch" to "The collapse is still to come",
        "Irgendwann ist dein Körper schwer genug, um zusammenzufallen. Das setzt den Lauf zurück und macht dich dauerhaft schneller. Es meldet sich von selbst, wenn es so weit ist." to
            "At some point your body will be heavy enough to fall in on itself. That resets the run and makes you permanently faster. It will say so when the time comes.",

        // ---- the card each system arrives with
        "Einschläge" to "Impacts",
        "Aufbau" to "Build",
        "Du bist ein Brocken, und du wächst, weil andere Brocken auf dich fallen. Tippe sie an, bevor sie aufschlagen: das bringt sofort Masse — und Material, das liegen bleibt. Was du damit anfängst, steht unter Aufbau." to
            "You are a rock, and you grow because other rocks fall on you. Tap them before they land: that brings mass straight away — and material, which stays behind. What to do with it is under Build.",
        "Schichten" to "Layers",
        "Aus Material baust du Kern, Mantel und Kruste. Der Kern erhöht die Produktion, der Mantel Tippwert und Anziehung, die Kruste Offline-Ertrag und Ausbeute. Ab sechs Schichten bekommt dein Körper einen Typ — und der bleibt eingetragen, auch wenn der Kollaps alles andere mitnimmt." to
            "Out of material you build core, mantle and crust. The core raises production, the mantle tap value and pull, the crust offline yield and material yield. At six layers your body gets a type — and that stays on the record even when the collapse takes everything else.",
        "Der Kollaps" to "The Collapse",
        "Kosmos · Kollaps" to "Cosmos · Collapse",
        "Dein Körper ist schwer genug, um in sich zusammenzufallen. Das setzt den Lauf zurück — Masse, Kollektoren, Upgrades, alles — und du bekommst Singularitäten dafür, die dauerhaft bleiben und alles Folgende schneller machen. Es ist kein Verlust, es ist die zweite Hälfte des Spiels." to
            "Your body is heavy enough to fall in on itself. That resets the run — mass, collectors, upgrades, all of it — and pays you singularities, which are permanent and make everything after them faster. It is not a loss, it is the second half of the game.",
        "Rollen" to "Roles",
        "Flotte · auf einen Kollektor tippen" to "Fleet · tap a collector",
        "Jeder Kollektor kann eine Aufgabe bekommen. Eine erhöht seinen eigenen Ausstoß, eine andere den seiner Nachbarn — es lohnt sich, nicht überall dasselbe einzustellen." to
            "Every collector can be given a job. One raises its own output, another raises its neighbours' — it pays not to set them all the same.",
        "Um deinen Körper lassen sich Bahnen öffnen und mit Trabanten besetzen. Zwei Trabanten derselben Stufe verschmelzen zu einer höheren. Und Bahnen, deren Umlaufzeiten glatt zueinander passen, verstärken sich gegenseitig." to
            "Orbits can be opened around your body and filled with satellites. Two satellites of the same tier merge into a higher one. And orbits whose periods fit neatly together reinforce each other.",
        "Dein Körper brennt jetzt. Aus Wasserstoff wird Helium, daraus Kohlenstoff, und so weiter bis zum Eisen — jedes Element multipliziert, was du ohnehin produzierst. Fusoren kaufst du wie Kollektoren." to
            "Your body is burning now. Hydrogen becomes helium, that becomes carbon, and so on up to iron — every element multiplies what you already produce. Fusers are bought like collectors.",
        "Kosmos · Labor" to "Cosmos · Lab",
        "Ein Projekt läuft auf der echten Uhr — auch wenn das Spiel zu ist. Es gibt nur eine Bank, also läuft immer nur eines. Vor dem Weglegen etwas anzuschieben ist darum fast immer richtig." to
            "A project runs on the real clock — even while the game is shut. There is only one bench, so only one runs at a time. Starting something before you put the game down is therefore almost always right.",
        "Automatik" to "Automation",
        "Kosmos · Regeln" to "Cosmos · Rules",
        "Regeln nehmen dir ab, was du sonst von Hand machst: Kollektoren nachkaufen, Bahnen ausbauen, Projekte anschieben. Jede lässt sich einzeln einstellen und einzeln wieder ausschalten." to
            "Rules take over what you would otherwise do by hand: restocking collectors, extending orbits, starting projects. Each one is set and switched off on its own.",
        "Aufträge" to "Contracts",
        "Drei Ziele liegen auf dem Tisch und werden nachgelegt. Bezahlt wird in Äonen — der Währung des Urknalls. Ein Balken unter jedem sagt, wie weit du bist." to
            "Three goals lie on the table and are replaced as they are met. They pay in Aeons — the currency of the Big Bang. A bar under each says how far along you are.",
        "Ein Lauf unter erschwerten Regeln — kein Tippen, keine Upgrades, halbe Produktion. Wer ihn schafft, behält einen dauerhaften Bonus. Mehrere lassen sich kombinieren, und das zahlt sich überproportional aus." to
            "A run under a harder rule — no tapping, no upgrades, half production. Beat it and you keep a permanent bonus. Several can be combined, and that pays more than the sum of its parts.",
        "Schwere Elemente" to "Heavy Elements",
        "Jenseits von Eisen geht es nicht mehr durch Brennen weiter — nur der Kollaps selbst schmiedet diese Elemente. Sie bleiben über den Lauf hinaus und heben an, was deine Fusionskette wert ist." to
            "Past iron, burning gets you no further — only the collapse itself forges these. They outlast the run and raise what your fusion chain is worth.",
        "Der Urknall" to "The Big Bang",
        "Die dritte Ebene. Der Urknall räumt auch die Singularitäten ab und gibt Äonen dafür — und du wählst eine von vier Ausrichtungen, die den ganzen nächsten Durchgang prägt. Dein altes Universum geht dabei nicht verloren." to
            "The third layer. The Big Bang clears the singularities too and pays Aeons for them — and you choose one of four leanings that shapes the whole next run. Your old universe is not lost in the process.",
        "Der Himmel" to "The Sky",
        "Kosmos · Himmel" to "Cosmos · Sky",
        "Jedes Universum, das du hinter dir lässt, bleibt als Galaxie am Himmel stehen und arbeitet weiter. Du kannst ihnen Aufgaben geben, sie ausbauen, zwei verschmelzen — und alte wieder besuchen." to
            "Every universe you leave behind stays in the sky as a galaxy and keeps working. You can give them jobs, build them up, merge two — and visit the old ones again.",
        "Legierungen" to "Alloys",
        "Zwei schwere Elemente lassen sich zu einer Legierung schmieden. Die kostet beide dauerhaft und gibt dafür einen Bonus, den kein einzelnes Element hat." to
            "Two heavy elements can be forged into an alloy. It costs both of them permanently and gives a bonus no single element has.",
        "Die Kennungsleiter" to "The Designation Ladder",
        "Leiter · links am Rand" to "Ladder · down the left edge",
        "Über dem Schwarzen Loch hört die Leiter nicht auf. Jeder Körper kommt sechshundertsechsundsiebzig Mal wieder, mit einer Kennung von AA bis ZZ — sechzehntausend Sprossen. Manche davon sind Funde und wollen bestimmt werden." to
            "The ladder does not stop at the Black Hole. Every body comes round six hundred and seventy-six more times, with a designation from AA to ZZ — sixteen thousand rungs. Some of them are finds, and want identifying.",
    )

    /** The palettes the bodies can be drawn in. */
    private val SKINS: Map<String, String> = mapOf(
        "Wie es ist" to "As It Is",
        "Die Farben, die die Körper ohnehin haben." to
            "The colours the bodies have anyway.",
        "Kaltlicht" to "Cold Light",
        "Alles zwei Kelvin zu blau. So sieht es aus, wenn niemand zusieht." to
            "Everything two kelvin too blue. This is how it looks when nobody is watching.",
        "Rost" to "Rust",
        "Eisen, überall, seit sehr langer Zeit." to "Iron, everywhere, for a very long time.",
        "Grünstich" to "Green Cast",
        "Ein Himmel, unter dem man besser nicht länger stehen bleibt." to
            "A sky you would rather not stand under for long.",
        "Asche" to "Ash",
        "Was übrig bleibt, wenn die Farbe als Erstes geht." to
            "What is left when the colour goes first.",
        "Vollzählig" to "All Present",
        "Als hätte jemand jeden einzelnen Fänger vergolden lassen. Hat auch jemand." to
            "As though somebody had every last catcher gilded. Somebody did.",
        "Acht Universen, die alle noch da sind. Man sieht sie einander leuchten." to
            "Eight universes, all still there. You can see them lighting each other.",
        "Katalog" to "Catalogue",
        "Die Farben einer Übersichtskarte. Kein Körper mehr, nur noch ein Eintrag." to
            "The colours of a survey chart. Not a body any more, just an entry.",
        "Ein Kanal" to "One Channel",
        "Ein Bildschirm, der nur eine Farbe konnte, und es hat gereicht." to
            "A screen that could only do one colour, and it was enough.",
    )

    /**
     * The sentences the game builds around a number.
     *
     * Whole sentences, never pieces. `"Erreiche %s in %s"` is "Reach %s within %s" — word order is
     * exactly what differs between the two languages, and a sentence glued together from separately
     * translated fragments can only ever have German word order with English words in it.
     */
    private val TEMPLATES: Map<String, String> = mapOf(
        "+%s kg pro Tipp" to "+%s kg per tap",
        "%s Masse pro Tipp" to "%s mass per tap",
        // A multiplier in front of a machine's name: "×2 Dust Catcher". The same in both.
        "%s %s" to "%s %s",
        "Jeder %s gibt %s +%s" to "Every %s gives %s +%s",
        "Jeder %s gibt allen Kollektoren +%s" to "Every %s gives all collectors +%s",
        "%s auf alles" to "%s to everything",
        "Tippen gibt zusätzlich %s deiner Produktion" to
            "Tapping also gives %s of your production",
        "Offline-Ertrag auf %s" to "Offline yield to %s",
        "Offline-Zeit zählt bis zu %s Stunden" to "Offline time counts for up to %s hours",
        "Offline-Ertrag mindestens %s" to "Offline yield at least %s",
        "Jeder freigeschaltete Kollektor startet mit %s Stück" to
            "Every unlocked collector starts with %s",
        "Start mit %s" to "Start with %s",
        "Kometen kommen %s so oft" to "Comets arrive %s as often",
        "%s auf alles, dauerhaft" to "%s to everything, permanently",
        "%s Singularitäten je Kollaps" to "%s singularities per collapse",
        "%s Masse pro Tipp, dauerhaft" to "%s mass per tap, permanently",
        "Tippt %s× pro Sekunde von allein" to "Taps %s× per second on its own",
        "Jede Singularität gibt %s statt %s" to "Every singularity gives %s instead of %s",
        "Kauft Kollektoren von allein, sobald du das Vierfache übrig hast" to
            "Buys collectors on its own once you have four times the price to spare",
        "Jeder Meilenstein gibt %s statt %s" to "Every milestone gives %s instead of %s",
        "Jede Fusionsstufe läuft %s so schnell" to "Every fusion stage runs %s as fast",
        "Forschung dauert nur noch %s der Zeit" to "Research takes only %s of the time",
        "Galaxien wiegen %s so schwer" to "Galaxies weigh %s as much",
        "Trabanten liefern %s" to "Satellites deliver %s",
        "Der Kollaps schmiedet %s Metall" to "The collapse forges %s metal",
        "Jeder Auftrag zahlt %s Äonen extra" to "Every contract pays %s Aeons extra",
        "Erreiche %s" to "Reach %s",
        "Erreiche %s in %s" to "Reach %s within %s",
        "Kollektoren produzieren nichts" to "Collectors produce nothing",
        "Tippen bringt nichts, Kollektoren nur %s" to "Tapping gives nothing, collectors only %s",
        "Keine Einschränkung" to "No restriction",
        "Alles bringt nur %s" to "Everything gives only %s",
        "Der Upgrade-Laden bleibt zu" to "The upgrade shop stays shut",
        "Nichts hält sich auf einer Bahn" to "Nothing stays in orbit",
        "Keine Meilenstein-Boni" to "No milestone bonuses",
        "Geschlossen zählt nicht" to "Closed does not count",
        "Die Fusionskette bleibt kalt" to "The fusion chain stays cold",
        "Die Galaxien tragen nichts bei" to "The galaxies contribute nothing",
    )


    /** The five machines above the black hole, which the first pass over the fleet missed. */
    private val COLLECTORS_LATE: Map<String, String> = mapOf(
        "Entropiemühle" to "Entropy Mill",
        "Mahlt Unordnung zurück zu Ordnung. Läuft rückwärts und beschwert sich nicht." to
            "Grinds disorder back into order. Runs backwards and does not complain.",
        "Horizontpflug" to "Horizon Plough",
        "Zieht Furchen in den Ereignishorizont und erntet, was dabei hochkommt." to
            "Cuts furrows into the event horizon and harvests whatever comes up.",
        "Nullpunktpresse" to "Zero-Point Press",
        "Presst das leerste Vakuum, bis unten Zahlen herauslaufen." to
            "Presses the emptiest vacuum until numbers run out of the bottom.",
        "Ewigkeitsschleuse" to "Eternity Lock",
        "Öffnet sich einmal pro Ewigkeit. Die Ewigkeiten sind kürzer geworden." to
            "Opens once per eternity. The eternities have been getting shorter.",
        "Alpha-Rückgriff" to "Alpha Retrieval",
        "Greift zurück bis vor den Anfang und nimmt mit, was dort noch liegt." to
            "Reaches back past the beginning and takes whatever is still lying there.",
    )

    /**
     * The settings that are numerals rather than words.
     *
     * Listed rather than filtered out of the collector, because "this needs no translation" is a
     * decision somebody made and the map is where decisions are written down. Filtering them would
     * mean the same judgement lived in a regular expression, where the next reader cannot see it.
     */
    private val NUMERALS: Map<String, String> = mapOf(
        "×2" to "×2",
        "×4" to "×4",
        "×5" to "×5",
        "×10" to "×10",
        "×20" to "×20",
        "×50" to "×50",
        "×100" to "×100",
        "5" to "5",
        "10" to "10",
        "25" to "25",
        "50" to "50",
    )


    /**
     * The screens' own words: buttons, headings, the sentences that explain a mechanism.
     *
     * Whole sentences, including the ones with a value in them. "noch %s" is "%s to go" — the
     * words end up on the other side of the figure, which is exactly why these are templates and
     * not a translated prefix glued to a number.
     */
    private val SCREENS: Map<String, String> = mapOf(
        // ---- headings and navigation
        "Körper" to "Body",
        "Flotte" to "Fleet",
        "Bahnen" to "Orbits",
        "Kosmos" to "Cosmos",
        "Aufträge" to "Contracts",
        "Spielstände" to "Saves",
        "Singularitäten" to "Singularities",
        "Äonen" to "Aeons",
        "Geräusche" to "Sound",
        "Sprache" to "Language",
        "Version %s" to "Version %s",
        "unbekannt" to "unknown",
        "Singularitäten ausgeben" to "Spend singularities",
        "Äonen ausgeben" to "Spend Aeons",
        "Spielstand einfügen" to "Paste save",
        "Spielstand löschen" to "Delete save",
        "Wer die Arbeit macht" to "Who does the work",
        "Wer geschuftet hat" to "Who did the grafting",
        "Wenn du weg bist" to "While you are away",
        "Willkommen zurück" to "Welcome back",
        "Was für ein Universum?" to "What kind of universe?",
        "Nächster Kauf" to "Next purchase",
        "Nächstes Äon" to "Next Aeon",
        "Bonus aus Singularitäten" to "Bonus from singularities",
        "Neue Version verfügbar" to "New version available",
        "Diese Version musst du installieren" to "You have to install this version",
        "Herausforderung läuft" to "Challenge running",

        // ---- buttons and short states
        "Doch nicht" to "Never mind",
        "Später" to "Later",
        "Nochmal prüfen" to "Check again",
        "Einfügen" to "Paste",
        "Urknall auslösen" to "Trigger Big Bang",
        "Noch nicht so weit" to "Not far enough yet",
        "Belohnung einlösen" to "Claim reward",
        "Hierher verschweißen" to "Weld into this one",
        "Zurück ins neueste Universum" to "Back to the newest universe",
        "Abbrechen (Masse ist weg)" to "Cancel (the mass is gone)",
        "Wirklich? Alles wird gelöscht" to "Really? Everything gets deleted",
        "Lädt …" to "Loading …",
        "aus" to "off",
        "erfüllt" to "met",
        "gewählt" to "chosen",
        "hier" to "here",
        "noch unbekannt" to "not known yet",
        "Du spielst gerade hier" to "You are playing here",
        "Du spielst die neueste Version." to "You are on the newest version.",
        "Kein Feuer im Kern" to "No fire in the core",
        "Nichts, was bleiben würde" to "Nothing that would stay",
        "Vollständig ausgebaut. Mehr geht hier nicht." to
            "Fully built. There is no more to be had here.",
        "In die Zwischenablage kopiert." to "Copied to the clipboard.",
        "Alles gelöscht. Neuer Anfang." to "All deleted. A fresh start.",
        "Das war kein Kollaps-Spielstand." to "That was not a Kollaps save.",
        "Überhitzt " to "Overheated ",

        // ---- automation states
        "noch nicht freigeschaltet" to "not unlocked yet",
        "erst, wenn der Kern brennt" to "not until the core is burning",
        "erst mit dem eigenen System" to "not until you have a system of your own",
        "erst mit dem Labor" to "not until the lab is built",

        // ---- lines with a value in them
        "%s von %s" to "%s of %s",
        "%s Äonen" to "%s Aeons",
        "%s Äon · %s" to "%s Aeon · %s",
        "%s zusätzlich, dauerhaft" to "%s extra, permanently",
        "%s — das Ende der Leiter" to "%s — the end of the ladder",
        "%s — beides gleichzeitig, beide Ziele nötig." to
            "%s — both at once, and both goals are required.",
        "%s/%s · noch %s" to "%s/%s · %s to go",
        "Stufe %s von %s" to "Level %s of %s",
        "Stufe %s/%s · noch %s" to "Tier %s/%s · %s to go",
        "noch %s" to "%s to go",
        "noch %s s" to "%s s to go",
        " · nächster bei %s" to " · next at %s",
        "zu %s." to "at %s.",
        "• %s Singularitäten" to "• %s singularities",
        "… und %s weitere" to "… and %s more",
        "Ausbau %s von %s" to "Level %s of %s",
        "Ausbauen · %s Äonen" to "Build up · %s Aeons",
        "Bahn %s öffnen · %s" to "Open orbit %s · %s",
        "Trabant auf Bahn %s" to "Satellite on orbit %s",
        "Resonanz mit Bahn %s · %s" to "Resonance with orbit %s · %s",
        "Hier läge Resonanz mit Bahn %s · %s" to "Resonance would sit here with orbit %s · %s",
        "Umstellung auf %s · noch %s" to "Switching to %s · %s to go",
        "Bisher %s× ausgelöst." to "Triggered %s× so far.",
        "Jetzt zu holen: %s Äonen" to "Available now: %s Aeons",
        "Jetzt zu holen: %s Singularitäten (%s extra)" to
            "Available now: %s singularities (%s extra)",
        "Kollaps bringt %s Singularitäten" to "Collapsing pays %s singularities",
        "Grau: dein bester Lauf, %s" to "Grey: your best run, %s",
        "Läuft auf %s · %s" to "Runs at %s · %s",
        "pendelt sich bei %s ein" to "settles at %s",
        "wartet auf %s: nur %s" to "waiting on %s: only %s",
        "wächst ohne Grenze, dafür langsam" to "grows without limit, but slowly",
        "Tippen zum Weitermachen · noch %s Stufen" to "Tap to carry on · %s tiers to go",
        "%s ist verfügbar%s" to "%s is available%s",
        "%s ist geladen und wartet auf die Installation." to
            "%s has downloaded and is waiting to be installed.",
        "blieben liegen. Ein größerer Speicher hätte sie mitgenommen." to
            "were left behind. A bigger store would have taken them along.",

        // ---- the longer explanations
        "Alle %s gekauft. Von hier aus geht es nur noch durch Spielen weiter." to
            "All %s bought. From here it only goes further by playing.",
        "Alle Klangeffekte: Tippen, vorbeiziehende und gefangene Kometen, Käufe, Zündung, fertige Forschung, neue Erfolge und der Kollaps." to
            "Every sound effect: tapping, comets passing and caught, purchases, ignition, finished research, new achievements and the collapse.",
        "Alle bestanden. Es gibt nichts mehr, was du dir noch schwerer machen könntest." to
            "All beaten. There is nothing left to make harder for yourself.",
        "Alles gekauft. Es gibt nichts mehr, was ein Neuanfang billiger machen könnte." to
            "Everything bought. There is nothing left for a fresh start to make cheaper.",
        "Achtung: das ersetzt den laufenden Spielstand vollständig." to
            "Careful: this replaces the running save completely.",
        "Ausrichtungen: %s von %s belegt — tippe auf die Zahl links, um eine zu vergeben." to
            "Roles: %s of %s assigned — tap the number on the left to assign one.",
        "Bahnen, deren Nummern in einem kleinen Verhältnis stehen — 1:2, 2:3, 1:3, 3:4, 2:5 — koppeln aneinander. Jede gekoppelte Nachbarin gibt beiden %s mehr." to
            "Orbits whose numbers stand in a small ratio — 1:2, 2:3, 1:3, 3:4, 2:5 — couple to each other. Every coupled neighbour gives both %s more.",
        "Damit die neue Version installiert werden kann, muss Kollaps in den Systemeinstellungen als Quelle erlaubt werden." to
            "Before the new version can be installed, Kollaps has to be allowed as a source in the system settings.",
        "Der Himmel ist voll. Zwei Galaxien lassen sich verschweißen — die verschmolzene trägt beide Ausrichtungen und macht einen Platz frei." to
            "The sky is full. Two galaxies can be welded together — the merged one carries both leanings and frees a slot.",
        "Der Körper ist zu groß geworden; kleine Brocken merkt er nicht mehr. Ab hier bringen die Kometen das Material des Himmels." to
            "The body has grown too large to notice small rocks. From here the comets bring what the sky has to offer.",
        "Der Spielstand liegt nur auf diesem Gerät. Kopier ihn dir irgendwohin, sonst ist er weg, wenn die App es ist." to
            "The save lives only on this device. Copy it somewhere, or it goes when the app does.",
        "Die Ausrichtung gilt, bis du das nächste Mal alles wegwirfst." to
            "The leaning holds until the next time you throw everything away.",
        "Die Zeit ist um. Aufgeben setzt den Lauf zurück, danach kannst du es noch mal versuchen." to
            "Time is up. Giving up resets the run, and then you can try again.",
        "Drei Ziele, die sich nachlegen. Bezahlt wird in Äonen." to
            "Three goals, replaced as they are met. They pay in Aeons.",
        "Drei getrennte Spiele. Beim Wechseln wird der laufende Stand zuerst gespeichert — es geht nichts verloren." to
            "Three separate games. Switching saves the running one first — nothing is lost.",
        "Du hast die Leiter zu Ende geklettert. Im Reiter Kosmos kannst du kollabieren und mit Singularitäten neu anfangen." to
            "You have climbed the ladder to the top. In the Cosmos tab you can collapse and start again with singularities.",
        "Ein Klangteppich, der sich ändert, sobald aus dem Gestein eine Welt, aus der Welt ein Gasriese und aus dem Gasriesen ein Stern wird." to
            "A bed of sound that changes as the rock becomes a world, the world a gas giant and the gas giant a star.",
        "Ein Projekt läuft auf der echten Uhr weiter — auch wenn das Spiel zu ist. Es gibt nur eine Bank, also läuft immer nur eines." to
            "A project keeps running on the real clock — even while the game is shut. There is only one bench, so only one runs at a time.",
        "Ein großes Update ändert, was im Spielstand steht. Zwei Fassungen nebeneinander vertragen sich dabei nicht — deshalb geht es hier nur vorwärts." to
            "A large update changes what is in the save. Two versions side by side do not get along — which is why this only goes forwards.",
        "Eine stille Zeile mit Produktion und der Restzeit im Labor. Sie bleibt stehen, bis du das Spiel wieder öffnest." to
            "A quiet line with your production and the time left in the lab. It stays until you open the game again.",
        "Erinnerung, wenn der Speicher voll ist" to "Remind me when the store is full",
        "Erreiche das Schwarze Loch, um zu kollabieren" to "Reach the Black Hole to collapse",
        "Erreiche das Schwarze Loch, um zu kollabieren. Jeder Kollaps bringt Singularitäten, die jeden weiteren Durchlauf dauerhaft beschleunigen." to
            "Reach the Black Hole to collapse. Every collapse pays singularities, which speed up every run after it, permanently.",
        "Erst ab dem %s hält deine Schwerkraft etwas auf einer Bahn. Vorher fällt alles entweder herunter oder weg." to
            "Not until %s is your gravity enough to hold anything in orbit. Before that, everything either falls in or falls away.",
        "Etwa alle %s Sekunden fällt etwas ein. Tippe es an, bevor es aufschlägt — sonst prallt das meiste davon wieder ab." to
            "Something falls in roughly every %s seconds. Tap it before it lands — otherwise most of it bounces straight off again.",
        "Fusion beginnt erst beim %s. Vorher ist in der Mitte nichts heiß genug, um irgendetwas zu verschmelzen." to
            "Fusion does not begin until %s. Before that, nothing in the middle is hot enough to fuse anything.",
        "Für heute alles abgeräumt. Morgen liegen wieder welche auf dem Tisch." to
            "Cleared for today. There will be more on the table tomorrow.",
        "Gekauft bleibt gekauft — wirkt aber nur, solange das Universum so ausgerichtet ist. Ein anderer Urknall legt das hier schlafen, kein Urknall nimmt es weg." to
            "Bought stays bought — but it only works while the universe leans this way. A different Big Bang puts it to sleep; no Big Bang takes it away.",
        "Gerade nichts zu verbessern.\nKauf weitere Kollektoren, dann tauchen hier neue Upgrades auf." to
            "Nothing to improve right now.\nBuy more collectors and new upgrades will turn up here.",
        "Gerade nichts zu verbessern.\nKauf weitere Kollektoren, dann taucht hier Neues auf." to
            "Nothing to improve right now.\nBuy more collectors and something new will turn up here.",
        "Geschafft. Einlösen setzt den Lauf zurück und behält die Belohnung." to
            "Done. Claiming resets the run and keeps the reward.",
        "Herausforderungen tauchen auf, wenn du kollabiert bist. Sie starten einen Lauf unter einer Regel, die dir etwas wegnimmt — dafür bleibt die Belohnung für immer." to
            "Challenges turn up once you have collapsed. They start a run under a rule that takes something away — and the reward is permanent.",
        "Jede Regel läuft für sich. Tippen schaltet weiter — nach der letzten Einstellung wieder aus." to
            "Every rule runs on its own. Tapping moves to the next setting — and off again after the last.",
        "Jede Stufe bringt der Galaxie dauerhaft mehr Gewicht — auch für ihre Äonen, ihre Kometen und ihr Metall." to
            "Every level permanently adds weight to the galaxy — for its Aeons, its comets and its metal too.",
        "Jede Stufe kostet mehr als die davor. Es gibt kein Ende der Liste — nur einen Preis, bei dem du aufhörst." to
            "Every level costs more than the one before. The list has no end — only a price at which you stop.",
        "Jede startet den Lauf neu und nimmt dir etwas weg. Der Vorsprung aus dem Prestige zählt dabei nicht — die dauerhaften Multiplikatoren schon. Zwei gleichzeitig gehen auch: beide Regeln, beide Ziele, beide Belohnungen — und obendrauf %s für immer, wenn keine der beiden vorher schon bestanden war." to
            "Each one restarts the run and takes something away. The head start from prestige does not count — the permanent multipliers do. Two at once also works: both rules, both goals, both rewards — and %s on top, for good, if neither had been beaten before.",
        "Kollabieren geht erst wieder, wenn das hier vorbei ist." to
            "Collapsing is not available again until this is over.",
        "Lass dein Schwarzes Loch in sich zusammenfallen. Du verlierst Masse, Kollektoren und Upgrades — behältst aber deine Singularitäten." to
            "Let your black hole fall in on itself. You lose mass, collectors and upgrades — but you keep your singularities.",
        "Laufende Anzeige, solange das Spiel zu ist" to "A standing notice while the game is shut",
        "Löscht alles: Masse, Kollektoren, Erfolge, Singularitäten, Äonen, Forschung. Es gibt kein Zurück — kopier dir vorher den Spielstand, falls du unsicher bist." to
            "Deletes everything: mass, collectors, achievements, singularities, Aeons, research. There is no way back — copy the save first if you are unsure.",
        "Nichts gefunden für „%s“.\nEs wird in Name, Wirkung und Beschreibung gesucht." to
            "Nothing found for “%s”.\nThe search covers name, effect and description.",
        "Noch leer. Der erste Urknall stellt das erste Universum hier ab, statt es wegzuwerfen." to
            "Empty so far. The first Big Bang parks the first universe here instead of throwing it away.",
        "Noch nicht nachgesehen, ob es eine neuere Version gibt." to
            "Have not looked yet whether there is a newer version.",
        "Noch nichts Bestimmtes. Ab %s Schichten bekommt der Körper einen Namen — und der bleibt eingetragen, auch nach dem Kollaps." to
            "Nothing in particular yet. At %s layers the body gets a name — and that stays on the record, even after the collapse.",
        "Sie wird aus ihrer Chronik aufgebaut: Sprosse, Kollapse und Singularitäten stimmen, die Masse bekommst du ungenutzt zurück — die Flotte musst du neu kaufen." to
            "It is rebuilt from its own record: rung, collapses and singularities are right, the mass comes back unspent — the fleet you have to buy again.",
        "Spielzeit, dann geht es zurück ins neueste Universum. Alles, was du hier tust, bleibt hier." to
            "of play time, then it goes back to the newest universe. Everything you do here stays here.",
        "Weitere Upgrades erscheinen, wenn du öfter kollabiert bist." to
            "More upgrades appear once you have collapsed more often.",
        "Wirf alles weg, was deine Kollapse aufgebaut haben: Singularitäten, Prestige-Upgrades, den Zähler selbst. Was bleibt, sind Erfolge, bestandene Herausforderungen — und Äonen." to
            "Throw away everything your collapses built: singularities, prestige upgrades, the counter itself. What stays is achievements, beaten challenges — and Aeons.",
        "%s von %s Galaxien. Jede davon ist ein Universum, das du zu Ende gespielt hast und das weiterläuft." to
            "%s of %s galaxies. Each one is a universe you played to the end and that keeps running.",
        "%s von %s offenen Bahnen besetzt. Innen wird schnell gefüttert und schnell zerrissen, außen langsam und für immer." to
            "%s of %s open orbits occupied. Close in, they are fed fast and torn apart fast; further out, slowly and for good.",
        "Zusammen mit der anderen bliebe nichts übrig, was Masse macht." to
            "Together with the other one, nothing would be left that makes mass.",
        "Zwei schwere Elemente, in einem Guss. Einmal geschmiedet und für immer behalten — auch durch den Urknall." to
            "Two heavy elements, cast together. Forged once and kept for good — through the Big Bang too.",
    )


    /**
     * The last of the screens: short labels, and the layouts that are only a number.
     *
     * A handful of these translate to themselves — "%s / %s" is a layout, not a sentence. They are
     * listed anyway rather than filtered out of the collector, because "this needs no translation"
     * is a judgement somebody made, and the map is where judgements are written down.
     */
    /**
     * The updater and the save slots, which live outside the screens.
     *
     * They are the one part of the interface that is not in a panel: the update card's error
     * lines come from the service that fetches, and the slot summary from the file layer. Both
     * end up in front of a player all the same.
     */
    private val PLUMBING: Map<String, String> = mapOf(
        "Der Installer ließ sich nicht öffnen" to "The installer would not open",
        "Die heruntergeladene Datei ist leer" to "The downloaded file is empty",
        "Eigene Version nicht lesbar" to "Cannot read this build's own version",
        "Einstellungsseite nicht erreichbar" to "The settings page is not reachable",
        "GitHub hat die Anfrage abgelehnt. Später nochmal versuchen." to
            "GitHub turned the request down. Try again later.",
        "Installation konnte nicht gestartet werden" to "The installation could not be started",
        "Keine Veröffentlichungen gefunden. Ist das Repository öffentlich?" to
            "No releases found. Is the repository public?",
        "Leer — hier fängt ein neues Spiel an." to "Empty — a new game starts here.",
        "Update-Prüfung fehlgeschlagen" to "The update check failed",
    )

    private val SCREENS_MORE: Map<String, String> = mapOf(
        // ---- pure layout
        "%s" to "%s",
        "%s / %s" to "%s / %s",
        "%s > %s" to "%s > %s",
        "%s/%s" to "%s/%s",
        "%s: %s" to "%s: %s",
        "%s ×%s" to "%s ×%s",
        "+%s" to "+%s",
        "×%s" to "×%s",
        "· %s" to "· %s",
        "• %s" to "• %s",
        "↓ %s" to "↓ %s",

        // ---- short labels
        "Material" to "Material",
        "Bauen" to "Build",
        "Weltentypen" to "World types",
        "Fertig gebaut" to "Fully built",
        "Bonus %s" to "Bonus %s",
        "Schmiede" to "Forge",
        "Schmieden" to "Forge it",
        "Geschmiedet" to "Forged",
        "Das System" to "The System",
        "Investitionen" to "Investments",
        "Katalogfund" to "Catalogue find",
        "Erfolg" to "Achievement",
        "Erfolge %s/%s" to "Achievements %s/%s",
        "App-Update" to "App update",
        "Installieren" to "Install",
        "Erlauben" to "Allow",
        "Abgeben" to "Hand in",
        "Verstanden" to "Got it",
        "Neu freigeschaltet" to "Newly unlocked",
        "Von vorn anfangen" to "Start over",
        "Weiter geht's" to "Carry on",
        "Lauf wirklich neu starten?" to "Really restart the run?",
        "Danach" to "Then",
        "voll" to "full",
        "fertig" to "done",
        "ohne Ausrichtung" to "no leaning",
        "Alle Bahnen offen" to "Every orbit open",
        "Koppelt an keine Bahn" to "Couples to no orbit",
        "erst nach dem ersten eigenen Kollaps" to "not until your first collapse",
        "Produktion, letzte halbe Stunde" to "Production, last half hour",

        // ---- lines with a value
        "%s  ·  %s pro Tipp" to "%s  ·  %s per tap",
        "%s pro Tipp" to "%s per tap",
        "%s erledigt" to "%s done",
        "%s · %s Kollapse · %s" to "%s · %s collapses · %s",
        "%s angerechnet, zu %s." to "%s credited, at %s.",
        "Du warst %s weg." to "You were away for %s.",
        "Du bist in %s" to "You are in %s",
        "Dieses Universum: %s" to "This universe: %s",
        "Alle %s" to "All %s",
        "Chronik · %s" to "Chronicle · %s",
        "Leiter %s/%s" to "Ladder %s/%s",
        "Platz %s" to "Slot %s",
        "Labor: %s" to "Lab: %s",
        "Zu finden: %s" to "Where to find it: %s",
        "Koppelt an Bahn %s" to "Couples to orbit %s",
        "Hineingehen · %s" to "Go inside · %s",
        "liefert %s" to "delivers %s",
        "in 20 min: %s" to "in 20 min: %s",
        "Produktion jetzt %s" to "Production now %s",
        "Überhitzt %s" to "Overheated %s",
        "→ %s zeigen" to "→ show %s",
        "Tippen zum Weitermachen · Katalog %s" to "Tap to carry on · catalogue %s",
        "Im Zusammenbruch entsteht: %s" to "The collapse forges: %s",
        "In zwanzig Minuten: %s — letzter Lauf: %s" to
            "In twenty minutes: %s — last run: %s",
        "Die Offline-Grenze war voll — %s blieben liegen. Ein größerer Speicher hätte sie mitgenommen." to
            "The offline cap was full — %s were left behind. A bigger store would have taken them along.",
        "Diese Galaxie wurde geparkt, bevor Universen aufbewahrt wurden. Sie wird aus ihrer Chronik aufgebaut: Sprosse, Kollapse und Singularitäten stimmen, die Masse bekommst du ungenutzt zurück — die Flotte musst du neu kaufen." to
            "This galaxy was parked before universes were kept. It is rebuilt from its own record: rung, collapses and singularities are right, the mass comes back unspent — the fleet you have to buy again.",
        "Noch %s Spielzeit, dann geht es zurück ins neueste Universum. Alles, was du hier tust, bleibt hier." to
            "%s of play time left, then it goes back to the newest universe. Everything you do here stays here.",
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
        putAll(FINDS_MORE)
        putAll(CHALLENGES)
        putAll(LORE)
        putAll(LORE_MORE)
        putAll(ACHIEVEMENTS)
        putAll(ACHIEVEMENTS_MORE)
        putAll(ACCRETION)
        putAll(GUIDE)
        putAll(SKINS)
        putAll(TEMPLATES)
        putAll(COLLECTORS_LATE)
        putAll(NUMERALS)
        putAll(SCREENS)
        putAll(SCREENS_MORE)
        putAll(PLUMBING)
    }
}
