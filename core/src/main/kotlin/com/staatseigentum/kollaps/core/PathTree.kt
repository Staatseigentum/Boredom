package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang

/**
 * One node of a path's tree.
 *
 * Bought with Äonen and kept for good, like everything else at that layer — but it only does
 * anything while the universe is running under [pathId]. That is the whole design: the four paths
 * were an identity without depth, four fixed sets of effects that made the tenth big bang play
 * like the first once you had seen all four. Something you can build into over several universes
 * gives repeating a path a reason, and gives switching away from one a cost.
 */
data class PathNode(
    val id: String,
    val pathId: String,
    val germanName: String,
    val germanFlavor: String,
    /** In Äonen. */
    val cost: Double,
    val effect: PrestigeEffect,
    /** The node that has to be bought first, or `null` for the root of the tree. */
    val requires: String? = null,
) {
    /** Shown text, translated where a translation exists. */
    val name: String get() = Lang.t(germanName)
    val flavor: String get() = Lang.t(germanFlavor)

    val effectText: String get() = effect.text
}

/**
 * Four small trees, one per [Path].
 *
 * Each is a root and three leaves hanging off it. Deliberately not deeper: the leaves are meant to
 * be a choice about what this path is *for* — and a choice between three things you can see is a
 * decision, while a chain of eight is a shopping list you work through in order.
 */
object PathTrees {

    private fun node(
        path: Path,
        suffix: String,
        name: String,
        flavor: String,
        cost: Double,
        effect: PrestigeEffect,
        requires: String? = null,
    ) = PathNode("${path.id}_$suffix", path.id, name, flavor, cost, effect, requires)

    val all: List<PathNode> = listOf(
        // ------------------------------------------------------------------ Die Hand
        node(
            Path.HAND, "root",
            "Geübter Schlag",
            "Zehntausend Wiederholungen später sitzt jede Bewegung.",
            cost = 2.0,
            effect = PrestigeEffect.TapMultiplier(5.0),
        ),
        node(
            Path.HAND, "fast",
            "Doppelgriff",
            "Zwei Hände, und keine wartet auf die andere.",
            cost = 4.0,
            effect = PrestigeEffect.TapMultiplier(4.0),
            requires = "path_hand_root",
        ),
        node(
            Path.HAND, "auto",
            "Nachhall",
            "Was du angestoßen hast, schlägt eine Weile von allein weiter.",
            cost = 5.0,
            effect = PrestigeEffect.AutoTap(25.0),
            requires = "path_hand_root",
        ),
        node(
            Path.HAND, "comet",
            "Scharfes Auge",
            "Nichts zieht mehr unbemerkt vorbei.",
            cost = 6.0,
            effect = PrestigeEffect.CometFrequency(2.0),
            requires = "path_hand_root",
        ),

        // --------------------------------------------------------------- Die Maschine
        node(
            Path.MASCHINE, "root",
            "Durchlaufender Betrieb",
            "Die Anlage kennt keine Schicht, die endet.",
            cost = 2.0,
            effect = PrestigeEffect.GlobalMultiplier(4.0),
        ),
        node(
            Path.MASCHINE, "series",
            "Serienfertigung",
            "Jede Auflage läuft glatter als die davor.",
            cost = 4.0,
            effect = PrestigeEffect.MilestoneBonus(0.06),
            requires = "path_machine_root",
        ),
        node(
            Path.MASCHINE, "night",
            "Nachtschicht",
            "Abwesenheit ist auch eine Betriebsart.",
            cost = 5.0,
            effect = PrestigeEffect.OfflineCapHours(24.0),
            requires = "path_machine_root",
        ),
        node(
            Path.MASCHINE, "fleet",
            "Stehende Flotte",
            "Sie wird zwischen zwei Universen nicht abgebaut.",
            cost = 6.0,
            effect = PrestigeEffect.StartingCollectors(150),
            requires = "path_machine_root",
        ),

        // ------------------------------------------------------------------ Das Labor
        node(
            Path.LABOR, "root",
            "Zweite Bank",
            "Ein Projekt mehr, das über Nacht fertig wird.",
            cost = 2.0,
            effect = PrestigeEffect.ResearchSpeed(2.0),
        ),
        node(
            Path.LABOR, "sleep",
            "Langzeitversuch",
            "Manches muss man einfach lange genug stehen lassen.",
            cost = 4.0,
            effect = PrestigeEffect.OfflineEfficiency(1.0),
            requires = "path_lab_root",
        ),
        node(
            Path.LABOR, "grant",
            "Anschubmittel",
            "Ein Labor, das bei null anfängt, forscht erst mal gar nichts.",
            cost = 5.0,
            effect = PrestigeEffect.StartingMass(5_000_000_000.0),
            requires = "path_lab_root",
        ),
        node(
            Path.LABOR, "insight",
            "Querverweis",
            "Zwei Ergebnisse, die nichts miteinander zu tun hatten, bis jemand hinsah.",
            cost = 6.0,
            effect = PrestigeEffect.GlobalMultiplier(6.0),
            requires = "path_lab_root",
        ),

        // ------------------------------------------------------------------- Der Kern
        node(
            Path.KERN, "root",
            "Heißerer Brennraum",
            "Es fusioniert schneller, als es sollte.",
            cost = 2.0,
            effect = PrestigeEffect.FusionRate(3.0),
        ),
        node(
            Path.KERN, "press",
            "Höherer Druck",
            "Was der Druck nicht schafft, schafft mehr Druck.",
            cost = 4.0,
            effect = PrestigeEffect.FusionRate(3.0),
            requires = "path_core_root",
        ),
        node(
            Path.KERN, "yield",
            "Dichter Rest",
            "Was am Rand des Kollapses bleibt, wiegt mehr.",
            cost = 5.0,
            effect = PrestigeEffect.SingularityGain(1.5),
            requires = "path_core_root",
        ),
        node(
            Path.KERN, "burn",
            "Durchgebrannt",
            "Ein Stern, der schneller stirbt, gibt in kürzerer Zeit mehr her.",
            cost = 6.0,
            effect = PrestigeEffect.GlobalMultiplier(5.0),
            requires = "path_core_root",
        ),
    )

    private val index: Map<String, PathNode> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Pfad-Knoten-ID im Katalog" }
        for (node in all) {
            val parent = node.requires ?: continue
            val found = index[parent]
            require(found != null) { "${node.id} verlangt einen Knoten, den es nicht gibt" }
            require(found.pathId == node.pathId) { "${node.id} hängt an einem fremden Pfad" }
        }
        for (path in Path.entries) {
            require(all.count { it.pathId == path.id && it.requires == null } == 1) {
                "${path.id} hat nicht genau eine Wurzel"
            }
        }
    }

    fun byId(id: String?): PathNode? = index[id]

    /** Every node of one path, root first. */
    fun of(pathId: String?): List<PathNode> =
        all.filter { it.pathId == pathId }.sortedBy { it.requires != null }

    /** The tree the running universe is aligned to. Empty before the first big bang. */
    fun current(state: GameState): List<PathNode> = of(state.path)

    /**
     * Whether this node can be bought right now.
     *
     * Three conditions, and each is its own kind of no: it has to belong to the universe actually
     * running, its parent has to be bought, and the Äonen have to be there.
     */
    fun canBuy(state: GameState, node: PathNode): Boolean =
        node.pathId == state.path &&
            node.id !in state.pathNodes &&
            (node.requires == null || node.requires in state.pathNodes) &&
            state.aeons >= node.cost

    /**
     * The effects a state actually gets from its tree.
     *
     * Nodes bought under another path stay bought and stay silent — which is what makes choosing
     * a path at the big bang mean something after the third or fourth one.
     */
    fun effects(state: GameState): List<PrestigeEffect> =
        state.pathNodes.mapNotNull { byId(it) }
            .filter { it.pathId == state.path }
            .map { it.effect }

    /** How many nodes of this path are bought, for the header of the panel. */
    fun ownedIn(state: GameState, pathId: String?): Int =
        state.pathNodes.count { byId(it)?.pathId == pathId }
}
