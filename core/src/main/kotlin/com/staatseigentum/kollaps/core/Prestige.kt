package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang

/** What a prestige upgrade changes. Every one of these survives a collapse. */
sealed interface PrestigeEffect {
    /** Raises the share of production credited while the app is closed. */
    data class OfflineEfficiency(val fraction: Double) : PrestigeEffect

    /** Raises how many hours of absence are credited at most. */
    data class OfflineCapHours(val hours: Double) : PrestigeEffect

    /** Every collector starts a new run with this many copies already built. */
    data class StartingCollectors(val count: Int) : PrestigeEffect

    /** A new run starts with this much mass in hand. */
    data class StartingMass(val mass: Double) : PrestigeEffect

    /** Comets arrive this many times as often. */
    data class CometFrequency(val factor: Double) : PrestigeEffect

    /** Multiplies every source of mass. */
    data class GlobalMultiplier(val factor: Double) : PrestigeEffect

    /** Multiplies the singularities a collapse pays out. */
    data class SingularityGain(val factor: Double) : PrestigeEffect

    /** Multiplies the mass a tap yields. */
    data class TapMultiplier(val factor: Double) : PrestigeEffect

    /** Taps the body this often per second without anybody touching it. */
    data class AutoTap(val perSecond: Double) : PrestigeEffect

    /** Replaces what one singularity is worth, when it is more than the base. */
    data class SingularityBonus(val perSingularity: Double) : PrestigeEffect

    /** Adds to what each collector milestone multiplies by. */
    data class MilestoneBonus(val extra: Double) : PrestigeEffect

    /** Unlocks the automatic buyer. Whether it actually runs is a setting. */
    data object AutoBuy : PrestigeEffect

    /** Multiplies how fast every fusion stage runs. */
    data class FusionRate(val factor: Double) : PrestigeEffect

    /** Divides how long a research project takes on the wall clock. */
    data class ResearchSpeed(val factor: Double) : PrestigeEffect

    /*
     * The four below speak to the systems the game grew after the lab was written.
     *
     * The tree stopped at [ResearchSpeed] and had nothing to say about the sky, the orbits, the
     * forge or the contract table — so a player who reached the multiverse found the one part of
     * the game that runs on the wall clock had quietly finished. These are what the second storey
     * of the tree is built out of.
     */

    /** Every parked galaxy weighs more, whatever job it is on. */
    data class SkyYield(val factor: Double) : PrestigeEffect

    /** Bodies on orbits contribute more. */
    data class OrbitYield(val factor: Double) : PrestigeEffect

    /** A collapse forges more heavy metal out of the same iron. */
    data class MetalYield(val factor: Double) : PrestigeEffect

    /** Every contract handed in pays this much extra, in Äonen. */
    data class ContractBonus(val extra: Double) : PrestigeEffect
}

/**
 * What an effect does, in words.
 *
 * Lives on the effect rather than on the upgrade because challenges hand out the same effects and
 * have to describe them the same way.
 */
val PrestigeEffect.text: String
    get() = when (this) {
        is PrestigeEffect.OfflineEfficiency ->
            Lang.t("Offline-Ertrag mindestens %s", Numbers.formatPercent(fraction))

        is PrestigeEffect.OfflineCapHours ->
            Lang.t("Offline-Zeit zählt bis zu %s Stunden", hours.toInt())

        is PrestigeEffect.StartingCollectors ->
            // "Jeder freigeschaltete" and not "jeder". The head start covers the machines the shop
            // will actually sell, which is not every machine in the game — the catalogue fleet is
            // out until its ladder opens. The sentence used to promise all of them, and for a
            // while it delivered on that promise, which is how the dead buy button got made.
            Lang.t("Jeder freigeschaltete Kollektor startet mit %s Stück", count)

        is PrestigeEffect.StartingMass ->
            Lang.t("Start mit %s", Numbers.formatMass(mass))

        is PrestigeEffect.CometFrequency ->
            Lang.t("Kometen kommen %s so oft", Numbers.formatMultiplier(factor))

        is PrestigeEffect.GlobalMultiplier ->
            Lang.t("%s auf alles, dauerhaft", Numbers.formatMultiplier(factor))

        is PrestigeEffect.SingularityGain ->
            Lang.t("%s Singularitäten je Kollaps", Numbers.formatMultiplier(factor))

        is PrestigeEffect.TapMultiplier ->
            Lang.t("%s Masse pro Tipp, dauerhaft", Numbers.formatMultiplier(factor))

        is PrestigeEffect.AutoTap ->
            Lang.t("Tippt %s× pro Sekunde von allein", Numbers.format(perSecond))

        is PrestigeEffect.SingularityBonus ->
            Lang.t(
                "Jede Singularität gibt %s statt %s",
                Numbers.formatPercent(perSingularity),
                Numbers.formatPercent(GameEngine.SINGULARITY_BONUS),
            )

        is PrestigeEffect.AutoBuy ->
            Lang.t("Kauft Kollektoren von allein, sobald du das Vierfache übrig hast")

        is PrestigeEffect.MilestoneBonus ->
            Lang.t(
                "Jeder Meilenstein gibt %s statt %s",
                Numbers.formatPercent(Milestones.FACTOR - 1.0 + extra),
                Numbers.formatPercent(Milestones.FACTOR - 1.0),
            )

        is PrestigeEffect.FusionRate ->
            Lang.t("Jede Fusionsstufe läuft %s so schnell", Numbers.formatMultiplier(factor))

        is PrestigeEffect.ResearchSpeed ->
            Lang.t("Forschung dauert nur noch %s der Zeit", Numbers.formatPercent(1.0 / factor))

        is PrestigeEffect.SkyYield ->
            Lang.t("Galaxien wiegen %s so schwer", Numbers.formatMultiplier(factor))

        is PrestigeEffect.OrbitYield ->
            Lang.t("Trabanten liefern %s", Numbers.formatMultiplier(factor))

        is PrestigeEffect.MetalYield ->
            Lang.t("Der Kollaps schmiedet %s Metall", Numbers.formatMultiplier(factor))

        is PrestigeEffect.ContractBonus ->
            Lang.t("Jeder Auftrag zahlt %s Äonen extra", Numbers.format(extra))
    }

/**
 * Something bought with singularities rather than with mass.
 *
 * The point is to give the collapse a decision. Singularities used to be a flat bonus, which
 * made the button the whole feature: press it, get more of the same. Spending them means a run
 * can be set up to be different from the last one.
 */
data class PrestigeUpgrade(
    val id: String,
    val germanName: String,
    val germanFlavor: String,
    /** Singularities it costs. */
    val cost: Double,
    val effect: PrestigeEffect,
    /** Collapses the player needs before this is even shown. */
    val requiredCollapses: Int = 0,
) {
    val name: String get() = Lang.t(germanName)

    val flavor: String get() = Lang.t(germanFlavor)

    val effectText: String get() = effect.text
}

object PrestigeUpgrades {

    val all: List<PrestigeUpgrade> = listOf(
        PrestigeUpgrade(
            id = "p_offline_1",
            germanName = "Wache Drohnen",
            germanFlavor = "Sie hören nicht auf, nur weil du weg bist.",
            cost = 3.0,
            effect = PrestigeEffect.OfflineEfficiency(1.0),
        ),
        PrestigeUpgrade(
            id = "p_start_mass",
            germanName = "Rücklage",
            germanFlavor = "Ein Rest Masse, den der Kollaps nicht mitgenommen hat.",
            cost = 5.0,
            effect = PrestigeEffect.StartingMass(50_000.0),
        ),
        PrestigeUpgrade(
            id = "p_tap",
            germanName = "Eingeübter Griff",
            germanFlavor = "Die Hände erinnern sich an jeden Durchlauf.",
            cost = 6.0,
            effect = PrestigeEffect.TapMultiplier(5.0),
        ),
        PrestigeUpgrade(
            id = "p_comet_1",
            germanName = "Kometenbahn",
            germanFlavor = "Du weißt inzwischen, wo man wartet.",
            cost = 8.0,
            effect = PrestigeEffect.CometFrequency(2.0),
        ),
        PrestigeUpgrade(
            id = "p_auto_1",
            germanName = "Kleiner Automat",
            germanFlavor = "Ein Arm, ein Motor, ein Takt. Er wird nicht müde und beschwert sich nie.",
            cost = 12.0,
            effect = PrestigeEffect.AutoTap(3.0),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_auto_2",
            germanName = "Schlagwerk",
            germanFlavor = "Zehn Arme im Takt. Du darfst zusehen — oder mittippen, das zählt dazu.",
            cost = 40.0,
            effect = PrestigeEffect.AutoTap(10.0),
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_offline_2",
            germanName = "Langzeitspeicher",
            germanFlavor = "Lagert die Ausbeute einen ganzen Tag lang ein.",
            cost = 10.0,
            effect = PrestigeEffect.OfflineCapHours(24.0),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_collectors_1",
            germanName = "Bewahrte Baupläne",
            germanFlavor = "Der Kollaps frisst die Anlagen, nicht das Wissen.",
            cost = 15.0,
            effect = PrestigeEffect.StartingCollectors(5),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_global_1",
            germanName = "Verdichtete Materie",
            germanFlavor = "Was einmal durch ein schwarzes Loch ging, wiegt mehr.",
            cost = 20.0,
            effect = PrestigeEffect.GlobalMultiplier(3.0),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_autobuy",
            germanName = "Selbsttätige Beschaffung",
            germanFlavor = "Sie kauft nach, wenn reichlich da ist, und lässt dir den Rest für Upgrades.",
            cost = 25.0,
            effect = PrestigeEffect.AutoBuy,
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_singularity",
            germanName = "Saubere Trennung",
            germanFlavor = "Beim Kollabieren geht weniger verloren.",
            cost = 30.0,
            effect = PrestigeEffect.SingularityGain(1.5),
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_collectors_2",
            germanName = "Vorgefertigte Flotte",
            germanFlavor = "Der nächste Durchlauf beginnt nicht mehr bei null.",
            cost = 45.0,
            effect = PrestigeEffect.StartingCollectors(25),
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_global_2",
            germanName = "Entropiekonto",
            germanFlavor = "Die Unordnung von neun Universen, gebündelt.",
            cost = 80.0,
            effect = PrestigeEffect.GlobalMultiplier(5.0),
            requiredCollapses = 3,
        ),
        // Everything below here exists because the road got longer. Eight universes now stand
        // between a new save and the catalogue ladder, and a shelf that was fully bought by the
        // third collapse left most of that road with nothing new on it.
        PrestigeUpgrade(
            id = "p_global_3",
            germanName = "Dritte Ausdehnung",
            germanFlavor = "Der Raum hat sich daran gewöhnt, für dich zu arbeiten.",
            cost = 150.0,
            effect = PrestigeEffect.GlobalMultiplier(8.0),
            requiredCollapses = 5,
        ),
        PrestigeUpgrade(
            id = "p_collectors_3",
            germanName = "Vollständiges Lagerverzeichnis",
            germanFlavor = "Nicht mehr nur die Liste überlebt, sondern auch, wo alles stand.",
            cost = 220.0,
            effect = PrestigeEffect.StartingCollectors(80),
            requiredCollapses = 6,
        ),
        PrestigeUpgrade(
            id = "p_offline_3",
            germanName = "Dauerbetrieb",
            germanFlavor = "Drei Tage ohne dich, und niemand hat gemerkt, dass du weg warst.",
            cost = 300.0,
            effect = PrestigeEffect.OfflineCapHours(72.0),
            requiredCollapses = 7,
        ),
        PrestigeUpgrade(
            id = "p_fusion_1",
            germanName = "Durchgeheizt",
            germanFlavor = "Die Öfen gehen zwischen zwei Universen nicht mehr aus.",
            cost = 400.0,
            effect = PrestigeEffect.FusionRate(2.5),
            requiredCollapses = 8,
        ),
        PrestigeUpgrade(
            id = "p_research_1",
            germanName = "Übertragene Notizen",
            germanFlavor = "Was einmal verstanden wurde, muss nicht zweimal verstanden werden.",
            cost = 550.0,
            effect = PrestigeEffect.ResearchSpeed(2.0),
            requiredCollapses = 10,
        ),
        PrestigeUpgrade(
            id = "p_auto_3",
            germanName = "Unermüdlich",
            germanFlavor = "Dreißig Schläge in der Sekunde, und keiner davon von dir.",
            cost = 700.0,
            effect = PrestigeEffect.AutoTap(30.0),
            requiredCollapses = 12,
        ),
        PrestigeUpgrade(
            id = "p_milestone_1",
            germanName = "Eingefahrene Serien",
            germanFlavor = "Die Fertigung kennt jede Auflage, die es je gegeben hat.",
            cost = 900.0,
            effect = PrestigeEffect.MilestoneBonus(0.05),
            requiredCollapses = 14,
        ),
        PrestigeUpgrade(
            id = "p_comet_2",
            germanName = "Dichter Trümmergürtel",
            germanFlavor = "Von acht Universen bleibt einiges liegen, und alles davon fliegt.",
            cost = 1_100.0,
            effect = PrestigeEffect.CometFrequency(3.0),
            requiredCollapses = 16,
        ),
        PrestigeUpgrade(
            id = "p_global_4",
            germanName = "Vierte Ausdehnung",
            germanFlavor = "Irgendwann fragt der Raum nicht mehr nach, er dehnt sich einfach.",
            cost = 1_400.0,
            effect = PrestigeEffect.GlobalMultiplier(15.0),
            requiredCollapses = 18,
        ),
        PrestigeUpgrade(
            id = "p_singularity_2",
            germanName = "Doppelter Schnitt",
            germanFlavor = "Zwei Singularitäten, wo vorher eine war. Frag nicht, welche die echte ist.",
            cost = 2_000.0,
            effect = PrestigeEffect.SingularityGain(2.0),
            requiredCollapses = 22,
        ),
    )

    private val index: Map<String, PrestigeUpgrade> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Prestige-ID im Katalog" }
    }

    fun byId(id: String): PrestigeUpgrade? = index[id]

    /** What the player can see right now: not yet bought, and enough collapses behind them. */
    fun offered(state: GameState): List<PrestigeUpgrade> =
        all.filter { it.id !in state.prestigeUpgrades && state.collapses >= it.requiredCollapses }
}
