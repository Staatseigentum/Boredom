package com.staatseigentum.kollaps.core

/**
 * Something the player did, recorded once and kept forever.
 *
 * Each one is worth a small permanent bonus rather than nothing. A list of ticks is decoration;
 * a list of ticks that adds up to a real multiplier is a reason to go and get the awkward ones.
 */
data class Achievement(
    val id: String,
    val name: String,
    val flavor: String,
    /** True once the state has earned it. Only ever asked about the current state. */
    val earned: (GameState) -> Boolean,
)

object Achievements {

    /** What each one adds to the global multiplier. */
    const val BONUS_EACH = 0.01

    val all: List<Achievement> = buildList {
        // ---- the ladder
        add(tier("a_tier_mond", "Erster Blick nach oben", "Grau, still, voller Krater.", "Mond"))
        add(tier("a_tier_erde", "Blauer Punkt", "Der einzige Ort mit Kaffee.", "Erde"))
        add(tier("a_tier_supererde", "Schwerer Boden", "Doppelt so schwer wie zuhause.", "Supererde"))
        add(tier("a_tier_saturn", "Beringt", "Der einzige Planet mit gutem Schmuck.", "Saturn"))
        add(tier("a_tier_sonne", "Hauptreihe", "Ganz normal, und trotzdem alles.", "Sonne"))
        add(tier("a_tier_hyper", "So groß es geht", "Größer wird ein Stern nicht.", "Hyperriese"))
        add(tier("a_tier_weiss", "Heiße Asche", "Was übrig bleibt, wenn ein Stern fertig ist.", "Weißer Zwerg"))
        add(tier("a_tier_neutron", "Ein Teelöffel", "Wiegt so viel wie ein Gebirge.", "Neutronenstern"))
        add(tier("a_tier_magnetar", "Unter Spannung", "Ein Feld, das dich aus tausend Kilometern zerlegt.", "Magnetar"))
        add(tier("a_tier_loch", "Ende der Leiter", "Ab hier kommt nichts mehr zurück.", "Schwarzes Loch"))

        // ---- tapping
        // The one achievement in the list that rewards doing nothing at all, which is the joke.
        add(
            Achievement(
                "a_leer",
                "Was war dat jetze?",
                "Das war genau so wenig wert wie eh und je",
            ) { it.missedTaps >= 1 },
        )
        add(taps("a_taps_100", "Angefasst", "Hundert Mal auf einen Stein.", 100))
        add(taps("a_taps_1k", "Hartnäckig", "Tausend Mal. Der Stein merkt nichts.", 1_000))
        add(taps("a_taps_10k", "Zwanghaft", "Zehntausend. Vielleicht mal Pause?", 10_000))
        add(taps("a_taps_50k", "Der Finger", "Fünfzigtausend Mal. Respekt und Sorge.", 50_000))

        // ---- mass
        add(mass("a_mass_1m", "Erste Million", "Eine Million Kilogramm, zusammengekratzt.", 1e6))
        add(mass("a_mass_1g", "Milliardenschwer", "Reicht für einen kleinen Mond.", 1e9))
        add(mass("a_mass_1t", "Billionär", "Die Zahl hat aufgehört, etwas zu bedeuten.", 1e12))
        add(mass("a_mass_1e18", "Jenseits", "Niemand kann sich das noch vorstellen.", 1e18))

        // ---- collectors
        add(collector("a_dust_25", "Staubsammlung", "Fünfundzwanzig Netze im Vakuum.", "dust", 25))
        add(collector("a_dust_100", "Staubfabrik", "Hundert Netze. Es fängt an, sich zu lohnen.", "dust", 100))
        add(collector("a_drone_50", "Schwarm", "Fünfzig Drohnen, die nie schlafen.", "drone", 50))
        add(collector("a_dyson_10", "Halber Ring", "Zehn Spiegel um einen Stern.", "dyson", 10))
        add(collector("a_echo_1", "Nachhall", "Der erste Augenblick, eingefangen.", "echo", 1))
        // Every sort at the cap: the one achievement that cannot be stumbled into, and the only
        // one a palette hangs on by name rather than by counting how many are ticked.
        add(
            Achievement(
                "a_alle_voll",
                "Ausgebaut",
                "Jede Sorte fünfhundert Mal. Es passt buchstäblich nichts mehr rein.",
            ) { state ->
                Collectors.all.all { state.ownedOf(it.id) >= Collector.MAX_OWNED }
            },
        )
        add(collector("a_weber_5", "Verwoben", "Fünf Weber an derselben Kausalkette.", "weber", 5))
        add(collector("a_omega_1", "Das letzte Gerät", "Sammelt ein, was übrig sein wird.", "omega", 1))
        add(
            Achievement(
                "a_all_collectors",
                "Vollständig",
                "Von jedem Kollektor mindestens einen.",
            ) { state -> Collectors.all.all { state.ownedOf(it.id) > 0 } },
        )

        // ---- upgrades
        add(
            Achievement("a_upgrades_10", "Aufgerüstet", "Zehn Verbesserungen gekauft.") {
                it.upgrades.size >= 10
            },
        )
        add(
            Achievement("a_upgrades_40", "Durchoptimiert", "Vierzig Verbesserungen in einem Lauf.") {
                it.upgrades.size >= 40
            },
        )

        // ---- comets
        add(comets("a_comet_1", "Erwischt", "Der erste Komet, rechtzeitig getippt.", 1))
        add(comets("a_comet_25", "Geübtes Auge", "Fünfundzwanzig davon.", 25))
        add(comets("a_comet_100", "Kometenjäger", "Hundert. Du wartest inzwischen darauf.", 100))

        // ---- prestige
        add(
            Achievement("a_collapse_1", "Erster Kollaps", "Alles hergegeben für einen Neuanfang.") {
                it.collapses >= 1
            },
        )
        add(
            Achievement("a_collapse_5", "Wiederholungstäter", "Fünf Universen später.") {
                it.collapses >= 5
            },
        )
        add(
            Achievement("a_singularity_50", "Gesammelte Enden", "Fünfzig Singularitäten besessen.") {
                it.singularities >= 50
            },
        )
        add(
            Achievement("a_prestige_5", "Vorbereitet", "Fünf Prestige-Upgrades gekauft.") {
                it.prestigeUpgrades.size >= 5
            },
        )

        add(
            Achievement("a_invest_10", "Angelegt", "Zehn Stufen in Investitionen gesteckt.") {
                Investments.totalLevels(it) >= 10
            },
        )
        add(
            Achievement(
                "a_invest_50",
                "Vermögensverwaltung",
                "Fünfzig Stufen. Die Singularitäten arbeiten für dich.",
            ) { Investments.totalLevels(it) >= 50 },
        )

        // ---- challenges
        add(
            Achievement("a_challenge_1", "Freiwillig schwerer", "Eine Herausforderung bestanden.") {
                it.challengesDone.isNotEmpty()
            },
        )
        add(
            Achievement("a_challenge_all", "Alles mitgenommen", "Jede Herausforderung bestanden.") { state ->
                Challenge.entries.all { it.id in state.challengesDone }
            },
        )

        // ---- the big bang
        add(
            Achievement("a_bigbang_1", "Von vorn, wirklich", "Ein ganzes Universum weggeworfen.") {
                it.bigBangs >= 1
            },
        )
        add(
            Achievement("a_bigbang_5", "Serientäter", "Fünf Universen. Keines davon vermisst.") {
                it.bigBangs >= 5
            },
        )
        add(
            Achievement("a_aeon_all", "Zeitlos", "Jedes Äonen-Upgrade gekauft.") { state ->
                AeonUpgrades.all.all { it.id in state.aeonUpgrades }
            },
        )

        add(
            Achievement(
                "a_roles_all",
                "Aufgestellt",
                "Jeder freie Platz mit einer Ausrichtung belegt.",
            ) { state ->
                Roles.isUnlocked(state) && Roles.assignedCount(state) >= Roles.slots(state)
            },
        )
        add(
            Achievement(
                "a_role_network",
                "Koordiniert",
                "Ein Kollektor, der nicht mehr selbst arbeitet, sondern alle anderen antreibt.",
            ) { state ->
                Collectors.all.any { Roles.roleOf(state, it.id) == Role.NETZ }
            },
        )

        // ---- the system
        add(
            Achievement("a_orbit_1", "Erste Bahn", "Etwas kreist um dich.") { state ->
                Orbits.occupiedCount(state) >= 1
            },
        )
        add(
            Achievement("a_orbit_full", "Vollbesetzt", "Auf jeder Bahn steht ein Körper.") { state ->
                state.orbits >= Orbits.MAX && Orbits.occupiedCount(state) >= Orbits.MAX
            },
        )
        add(
            Achievement(
                "a_orbit_resonance",
                "Im Gleichschritt",
                "Zwei Trabanten in Resonanz — dieselbe Kraft an derselben Stelle, jedes Mal.",
            ) { state ->
                Orbits.opened(state).any { Orbits.resonantWith(state, it).isNotEmpty() }
            },
        )

        // ---- fusion
        add(
            Achievement("a_fusion_1", "Gezündet", "Die erste Protonenkette läuft.") { state ->
                Fusion.amountOf(state, Element.HELIUM) >= 1.0
            },
        )
        add(
            Achievement("a_fusion_carbon", "Aus Sternen gemacht", "Kohlenstoff im Kern. Wie du.") { state ->
                Fusion.amountOf(state, Element.KOHLENSTOFF) >= 1.0
            },
        )
        add(
            Achievement("a_fusion_iron", "Eisenkern", "Weiter geht es nicht. Genau darum geht es.") { state ->
                Fusion.amountOf(state, Element.EISEN) >= 1.0
            },
        )
        add(
            Achievement(
                "a_fusion_full",
                "Ganze Kette",
                "Jede Stufe der Fusion läuft gleichzeitig.",
            ) { state -> Fusion.stages.all { Fusion.levelOf(state, it) > 0 } },
        )

        // ---- what the collapse forges
        add(
            Achievement(
                "a_heavy_1",
                "Sternenstaub",
                "Das erste Gold, aus dem Eisen eines gestorbenen Kerns.",
            ) { Heavy.amountOf(it, HeavyElement.GOLD) >= 1.0 },
        )
        add(
            Achievement(
                "a_heavy_uranium",
                "Das schwerste von selbst",
                "Uran. Schwerer geht es ohne fremde Hilfe nicht.",
            ) { Heavy.amountOf(it, HeavyElement.URAN) >= 1.0 },
        )
        add(
            Achievement(
                "a_heavy_1000",
                "Schatzkammer",
                "Tausend Einheiten schwerer Elemente, über alle Universen hinweg.",
            ) { Heavy.total(it) >= 1_000.0 },
        )

        // ---- research
        add(
            Achievement("a_research_1", "Erste Erkenntnis", "Ein Projekt zu Ende gewartet.") {
                it.research.isNotEmpty()
            },
        )
        add(
            Achievement("a_research_5", "Laborbetrieb", "Fünf Projekte abgeschlossen.") {
                it.research.size >= 5
            },
        )
        add(
            Achievement("a_research_all", "Ausgeforscht", "Der ganze Baum steht.") { state ->
                ResearchTree.all.all { it.id in state.research }
            },
        )

        // ---- milestones
        add(
            Achievement(
                "a_milestone_1",
                "Erster Meilenstein",
                "Fünfundzwanzig Stück von einer Sorte.",
            ) { state -> state.collectors.values.any { it >= Milestones.STEP } },
        )
        add(
            Achievement(
                "a_milestone_8",
                "Serienfertigung",
                "Zweihundert Stück von einer Sorte — acht Meilensteine auf einem Kollektor.",
            ) { state -> state.collectors.values.any { it >= Milestones.STEP * 8 } },
        )

        // ---- the awkward ones
        add(
            Achievement(
                "a_bare_hands",
                "Mit bloßen Händen",
                "Erreiche den Merkur, ohne einen einzigen Kollektor zu besitzen.",
            ) { state ->
                Tiers.forMass(state.runMass).index >= Tiers.indexOf("Merkur") &&
                    state.collectors.values.all { it == 0 }
            },
        )
        add(
            Achievement(
                "a_patient",
                "Geduldig",
                "Eine Stunde Spielzeit auf der Uhr.",
            ) { it.playedSeconds >= 3_600 },
        )
        add(
            Achievement(
                "a_marathon",
                "Marathon",
                "Sechs Stunden Spielzeit.",
            ) { it.playedSeconds >= 6 * 3_600 },
        )
    }

    private val index: Map<String, Achievement> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Erfolgs-ID im Katalog" }
    }

    fun byId(id: String): Achievement? = index[id]

    /** Ids earned by this state that are not already recorded. */
    fun newlyEarned(state: GameState): Set<String> =
        all.asSequence()
            .filter { it.id !in state.achievements && it.earned(state) }
            .map { it.id }
            .toSet()

    /** The multiplier all recorded achievements add up to. */
    fun multiplier(state: GameState): Double = 1.0 + BONUS_EACH * state.achievements.size

    // ------------------------------------------------------------------ small builders

    private fun tier(id: String, name: String, flavor: String, tierName: String) = Achievement(
        id, name, flavor,
    ) { state ->
        val target = Tiers.all.first { it.name == tierName }.index
        state.bestTier >= target || Tiers.forMass(state.runMass).index >= target
    }

    private fun taps(id: String, name: String, flavor: String, count: Long) =
        Achievement(id, name, flavor) { it.taps >= count }

    private fun mass(id: String, name: String, flavor: String, amount: Double) =
        Achievement(id, name, flavor) { it.totalMass >= amount }

    private fun collector(id: String, name: String, flavor: String, collectorId: String, count: Int) =
        Achievement(id, name, flavor) { it.ownedOf(collectorId) >= count }

    private fun comets(id: String, name: String, flavor: String, count: Long) =
        Achievement(id, name, flavor) { it.cometsCaught >= count }
}
