package com.staatseigentum.kollaps.core

/**
 * Something the player pays for once with mass and then waits out on the wall clock.
 *
 * The waiting is the point, and it is the one thing in the game that is not for sale. Every other
 * currency can be earned faster by playing better; a project that takes forty minutes takes forty
 * minutes whether the phone is in a hand or in a drawer. That makes it the only reason to start
 * something before putting the game down, and the only reward that is waiting when it is picked
 * back up.
 *
 * What a project pays out survives a collapse and a big bang alike, which is why the effects lean
 * towards reach — offline hours, automation, comets — rather than towards another multiplier on
 * production. A permanent multiplier compounds with every reset; a permanent convenience does not.
 */
data class Research(
    val id: String,
    val name: String,
    val flavor: String,
    /** Mass paid up front, when the project starts. */
    val cost: Double,
    /** Seconds of wall clock the project runs for, before any speed bonus. */
    val seconds: Double,
    /** Projects that must be finished first. Empty for the three at the root. */
    val requires: List<String> = emptyList(),
    val effect: PrestigeEffect,
) {
    val effectText: String get() = effect.text
}

object ResearchTree {

    /**
     * The body at which the lab opens.
     *
     * Late enough that the player already knows what a collector and an upgrade are, early enough
     * that the first long project can be started on the first evening rather than the third.
     */
    const val UNLOCK_TIER = "Erde"

    val all: List<Research> = listOf(
        // ---- the root: cheap, quick, and each one opens a different branch
        Research(
            id = "r_optics",
            name = "Spektralanalyse",
            flavor = "Wer weiß, woraus ein Lichtpunkt besteht, sieht ihn früher kommen.",
            cost = 5e4,
            seconds = 3 * 60.0,
            effect = PrestigeEffect.CometFrequency(1.35),
        ),
        Research(
            id = "r_storage",
            name = "Massespeicher",
            flavor = "Ein Lager, das auch dann noch annimmt, wenn niemand hinsieht.",
            cost = 3e5,
            seconds = 6 * 60.0,
            effect = PrestigeEffect.OfflineCapHours(12.0),
        ),
        Research(
            id = "r_drives",
            name = "Ionenantrieb",
            flavor = "Wenig Schub, endlos lange. Genau richtig für etwas, das nie ankommen muss.",
            cost = 4e6,
            seconds = 12 * 60.0,
            effect = PrestigeEffect.GlobalMultiplier(1.25),
        ),

        // ---- second row
        Research(
            id = "r_telemetry",
            name = "Telemetrie",
            flavor = "Die Flotte funkt, was sie tut. Vorher war es Vertrauenssache.",
            cost = 8e7,
            seconds = 20 * 60.0,
            requires = listOf("r_optics"),
            effect = PrestigeEffect.OfflineEfficiency(0.7),
        ),
        Research(
            id = "r_swarm",
            name = "Schwarmlogik",
            flavor = "Hundert Maschinen, die sich absprechen, sind mehr als hundert Maschinen.",
            cost = 6e8,
            seconds = 30 * 60.0,
            requires = listOf("r_drives"),
            effect = PrestigeEffect.MilestoneBonus(0.02),
        ),
        Research(
            id = "r_cryo",
            name = "Kryospeicher",
            flavor = "Kalt genug, dass sich ein ganzer Tag Produktion nicht langweilt.",
            cost = 2e10,
            seconds = 45 * 60.0,
            requires = listOf("r_storage"),
            effect = PrestigeEffect.OfflineCapHours(24.0),
        ),

        // ---- third row: the automation the player has been doing by hand until now
        Research(
            id = "r_autoloader",
            name = "Lademaschine",
            flavor = "Kauft nach, solange Überschuss da ist. Fragt nicht, ob es passt.",
            cost = 8e11,
            seconds = 60 * 60.0,
            requires = listOf("r_swarm"),
            effect = PrestigeEffect.AutoBuy,
        ),
        Research(
            id = "r_hammer",
            name = "Resonanzhammer",
            flavor = "Schlägt im Takt der Eigenfrequenz. Dein Finger darf sich ausruhen.",
            cost = 3e12,
            seconds = 90 * 60.0,
            requires = listOf("r_drives"),
            effect = PrestigeEffect.AutoTap(2.0),
        ),
        Research(
            id = "r_lens",
            name = "Gravitationslinse",
            flavor = "Krümmt den Raum so, dass mehr davon auf dich zeigt.",
            cost = 8e13,
            seconds = 2 * 60 * 60.0,
            requires = listOf("r_telemetry"),
            effect = PrestigeEffect.GlobalMultiplier(1.4),
        ),
        Research(
            id = "r_parallel",
            name = "Parallelrechnung",
            flavor = "Zwei Fragen gleichzeitig zu stellen war die letzte Frage.",
            cost = 2e14,
            seconds = 60 * 60.0,
            requires = listOf("r_autoloader"),
            effect = PrestigeEffect.ResearchSpeed(2.0),
        ),

        // ---- the long ones, all pointed at the fusion chain and the collapse
        Research(
            id = "r_confinement",
            name = "Magnetischer Einschluss",
            flavor = "Hält das Plasma dort, wo es brennen soll, statt an der Wand.",
            cost = 2e15,
            seconds = 2 * 60 * 60.0,
            requires = listOf("r_lens"),
            effect = PrestigeEffect.FusionRate(2.0),
        ),
        Research(
            id = "r_catalysis",
            name = "Katalysierte Fusion",
            flavor = "Ein Myon an der richtigen Stelle spart dem Kern zehn Millionen Grad.",
            cost = 8e16,
            seconds = 3 * 60 * 60.0,
            requires = listOf("r_confinement"),
            effect = PrestigeEffect.FusionRate(3.0),
        ),
        Research(
            id = "r_horizon",
            name = "Horizontmechanik",
            flavor = "Was hineinfällt, ist weg. Was am Rand bleibt, lässt sich zählen.",
            cost = 5e18,
            seconds = 4 * 60 * 60.0,
            requires = listOf("r_lens"),
            effect = PrestigeEffect.SingularityGain(1.5),
        ),
        Research(
            id = "r_eternity",
            name = "Ewigkeitsformel",
            flavor = "Acht Stunden Rechenzeit für einen Satz, den danach niemand mehr braucht.",
            cost = 1e20,
            seconds = 8 * 60 * 60.0,
            requires = listOf("r_horizon", "r_catalysis"),
            effect = PrestigeEffect.GlobalMultiplier(1.6),
        ),
    )

    private val index: Map<String, Research> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Forschungs-ID im Katalog" }
        for (project in all) {
            for (required in project.requires) {
                require(index.containsKey(required)) {
                    "${project.name} verlangt unbekannte Forschung: $required"
                }
            }
        }
    }

    fun byId(id: String?): Research? = if (id == null) null else index[id]

    /** True once the lab exists at all. Owning finished work keeps it open for good. */
    fun isUnlocked(state: GameState): Boolean =
        state.bestTier >= Tiers.indexOf(UNLOCK_TIER) ||
            state.research.isNotEmpty() ||
            state.activeResearch != null

    fun isDone(state: GameState, project: Research): Boolean = project.id in state.research

    /** Whether every prerequisite is finished. */
    fun isOpen(state: GameState, project: Research): Boolean =
        project.requires.all { it in state.research }

    /** The projects worth listing: everything open or already finished, newest work last. */
    fun offered(state: GameState): List<Research> =
        all.filter { isOpen(state, it) || isDone(state, it) }

    /** The one running, if any. */
    fun active(state: GameState): Research? = byId(state.activeResearch)

    /** Seconds this project would take for this player, after any speed bonus. */
    fun duration(state: GameState, project: Research): Double =
        project.seconds / GameEngine.researchSpeed(state)

    /** Seconds left on the running project, or zero when nothing is running or it is due. */
    fun secondsLeft(state: GameState, nowMillis: Long): Double {
        if (state.activeResearch == null) return 0.0
        return ((state.researchDoneAt - nowMillis) / 1_000.0).coerceAtLeast(0.0)
    }

    /** Progress of the running project in `0f..1f`. */
    fun progress(state: GameState, nowMillis: Long): Float {
        val project = active(state) ?: return 0f
        val total = duration(state, project)
        if (total <= 0.0) return 1f
        return (1.0 - secondsLeft(state, nowMillis) / total).coerceIn(0.0, 1.0).toFloat()
    }

    /** True when the running project has come due and only needs settling. */
    fun isFinished(state: GameState, nowMillis: Long): Boolean =
        state.activeResearch != null && nowMillis >= state.researchDoneAt
}
