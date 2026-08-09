package com.staatseigentum.kollaps.core

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * The second reset, and what it buys.
 *
 * The first one — the collapse — stops meaning anything once all twelve prestige upgrades are
 * bought: pressing it again is a flat multiplier and nothing else. The big bang gives that shelf
 * a shelf of its own. It costs everything the collapses built, and pays in Äonen, which buy
 * things a singularity never could.
 */
object BigBang {

    /** Collapses needed before the button exists at all. */
    const val REQUIRED_COLLAPSES = 10

    /** Äonen for exactly [REQUIRED_COLLAPSES] collapses. It grows with the root from there. */
    const val SCALE = 3.0

    fun isUnlocked(state: GameState): Boolean =
        state.collapses >= REQUIRED_COLLAPSES || state.bigBangs > 0 || state.aeons > 0.0

    /** Äonen a big bang would pay right now. */
    fun pending(state: GameState): Double {
        if (state.collapses < REQUIRED_COLLAPSES) return 0.0
        return floor(SCALE * sqrt(state.collapses.toDouble() / REQUIRED_COLLAPSES))
    }

    fun canBang(state: GameState): Boolean =
        state.runningChallengeIds.isEmpty() && pending(state) >= 1.0

    /** Not yet bought. Äonen upgrades are never gated on anything but their price. */
    fun offered(state: GameState): List<AeonUpgrade> =
        AeonUpgrades.all.filter { it.id !in state.aeonUpgrades }
}

/** Something bought with Äonen. Survives everything, including the big bang that paid for it. */
data class AeonUpgrade(
    val id: String,
    val name: String,
    val flavor: String,
    val cost: Double,
    val effect: PrestigeEffect,
) {
    val effectText: String get() = effect.text
}

object AeonUpgrades {

    val all: List<AeonUpgrade> = listOf(
        AeonUpgrade(
            id = "ae_global_1",
            name = "Erste Ausdehnung",
            flavor = "Der Raum selbst arbeitet für dich, seit du ihn einmal neu gefaltet hast.",
            cost = 1.0,
            effect = PrestigeEffect.GlobalMultiplier(10.0),
        ),
        AeonUpgrade(
            id = "ae_start_mass",
            name = "Übriggebliebene Materie",
            flavor = "Ein Universum später findet sich immer noch etwas in den Taschen.",
            cost = 2.0,
            effect = PrestigeEffect.StartingMass(2_000_000_000.0),
        ),
        AeonUpgrade(
            id = "ae_singularity",
            name = "Dichteres Nichts",
            flavor = "Jede Singularität wiegt schwerer als in der Welt davor.",
            cost = 3.0,
            effect = PrestigeEffect.SingularityBonus(0.25),
        ),
        AeonUpgrade(
            id = "ae_auto",
            name = "Ewiges Schlagwerk",
            flavor = "Es hat vor diesem Universum getippt und wird nach ihm weitertippen.",
            cost = 4.0,
            effect = PrestigeEffect.AutoTap(30.0),
        ),
        AeonUpgrade(
            id = "ae_milestone",
            name = "Eingespielte Serien",
            flavor = "Die Fertigungsstraßen erinnern sich an jede Auflage, die es je gab.",
            cost = 5.0,
            effect = PrestigeEffect.MilestoneBonus(0.10),
        ),
        AeonUpgrade(
            id = "ae_collectors",
            name = "Mitgenommene Flotte",
            flavor = "Hundert Stück von allem, noch bevor der erste Stein fällt.",
            cost = 6.0,
            effect = PrestigeEffect.StartingCollectors(100),
        ),
        AeonUpgrade(
            id = "ae_comet",
            name = "Dichter Kometenstrom",
            flavor = "Die Trümmer des letzten Universums ziehen immer noch vorbei.",
            cost = 8.0,
            effect = PrestigeEffect.CometFrequency(3.0),
        ),
        AeonUpgrade(
            id = "ae_global_2",
            name = "Zweite Ausdehnung",
            flavor = "Und noch einmal, und diesmal weiß der Raum schon, wie es geht.",
            cost = 12.0,
            effect = PrestigeEffect.GlobalMultiplier(50.0),
        ),
    )

    private val index: Map<String, AeonUpgrade> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Äonen-ID im Katalog" }
    }

    fun byId(id: String): AeonUpgrade? = index[id]
}
