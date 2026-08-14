package com.staatseigentum.kollaps.core

import kotlin.math.floor
import kotlin.math.ln
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

    /** Collapses needed before the button exists at all — that is, for the very first one. */
    const val REQUIRED_COLLAPSES = 10

    /**
     * Collapses each further big bang asks for on top of the one before it.
     *
     * The requirement used to be flat, which made every big bang after the first one cheaper in
     * real terms: collapses come faster the deeper a player is, so ten of them cost less and less
     * each time round. The road to the fourth universe took an afternoon and then the ladder had
     * nothing left above it.
     *
     * Rising instead, so the eight universes it takes to fill the sky are eight genuinely
     * different stretches of game rather than the same one at eight speeds.
     *
     * Three and not five. Five was set while the catalogue ladder opened at the fourth universe;
     * moving that gate to a full sky doubled the road on its own, and keeping the steeper step on
     * top of it would have stacked two stretches into 220 collapses. This lands at 164 — about
     * four times the old road, which is a long endgame rather than a punishing one.
     */
    const val REQUIREMENT_STEP = 3

    /** Collapses needed for the big bang after [bigBangs] of them. */
    fun requiredFor(bigBangs: Int): Int =
        REQUIRED_COLLAPSES + REQUIREMENT_STEP * bigBangs.coerceAtLeast(0)

    /** What this state needs before the button does anything. */
    fun requiredNow(state: GameState): Int = requiredFor(state.bigBangs)

    fun isUnlocked(state: GameState): Boolean =
        state.collapses >= REQUIRED_COLLAPSES || state.bigBangs > 0 || state.aeons > 0.0

    /** Äonen for exactly the required collapses. It grows with the root from there. */
    const val SCALE = 3.0

    /** Äonen a big bang would pay right now. */
    fun pending(state: GameState): Double {
        val needed = requiredNow(state)
        if (state.collapses < needed) return 0.0
        val fromCollapses = SCALE * sqrt(state.collapses.toDouble() / needed)
        return floor(fromCollapses * depthBonus(state))
    }

    /**
     * What the universe being ended was worth, beyond how many times it was collapsed.
     *
     * A big bang used to read only the collapse counter, so two universes that had been collapsed
     * the same number of times paid the same — whether one of them had been pushed a thousand
     * rungs up the catalogue ladder or parked the moment the button lit up. The counter is a
     * measure of patience; this is a measure of how far the universe actually got, and both should
     * be worth something at the moment it is given up.
     *
     * The same logarithm the collapse uses, for the same reason and deliberately in the same
     * shape: two rewards for depth that grew differently would be two things to learn.
     */
    fun depthBonus(state: GameState): Double {
        val rungs = maxOf(state.bestTier, GameEngine.tierOf(state).index) - Tiers.last.index
        if (rungs <= 0) return 1.0
        return 1.0 + DEPTH_BONUS * ln(1.0 + rungs)
    }

    /** How hard depth pays at the big bang. Gentler than the collapse's, which is paid far oftener. */
    const val DEPTH_BONUS = 0.5

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
        // The shelf used to end here, at twelve Äonen — which the sky now earns on its own in
        // under a fortnight. A currency that keeps coming in and has nothing left to buy is a
        // number going up, and the whole point of parking universes was that they pay for
        // something.
        AeonUpgrade(
            id = "ae_fusion",
            name = "Ewiges Feuer",
            flavor = "Es brannte im letzten Universum und hat den Übergang nicht bemerkt.",
            cost = 16.0,
            effect = PrestigeEffect.FusionRate(5.0),
        ),
        AeonUpgrade(
            id = "ae_research",
            name = "Übertragene Bibliothek",
            flavor = "Ein Labor, das schon weiß, was es diesmal herausfinden wird.",
            cost = 22.0,
            effect = PrestigeEffect.ResearchSpeed(4.0),
        ),
        AeonUpgrade(
            id = "ae_offline",
            name = "Langer Atem",
            flavor = "Vier Tage Abwesenheit sind für eine Galaxie keine Erwähnung wert.",
            cost = 30.0,
            effect = PrestigeEffect.OfflineCapHours(96.0),
        ),
        AeonUpgrade(
            id = "ae_global_3",
            name = "Dritte Ausdehnung",
            flavor = "Der Raum dehnt sich inzwischen, ohne dass jemand es anstößt.",
            cost = 40.0,
            effect = PrestigeEffect.GlobalMultiplier(250.0),
        ),
        AeonUpgrade(
            id = "ae_collectors_2",
            name = "Vollständige Werft",
            flavor = "Fünfhundert Stück von allem, und die Baupläne für den Rest.",
            cost = 55.0,
            effect = PrestigeEffect.StartingCollectors(500),
        ),
        AeonUpgrade(
            id = "ae_global_4",
            name = "Letzte Ausdehnung",
            flavor = "Danach kommt nichts mehr. Es sei denn, du machst weiter.",
            cost = 75.0,
            effect = PrestigeEffect.GlobalMultiplier(1_500.0),
        ),
    )

    private val index: Map<String, AeonUpgrade> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Äonen-ID im Katalog" }
    }

    fun byId(id: String): AeonUpgrade? = index[id]
}
