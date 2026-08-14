package com.staatseigentum.kollaps.core

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
            "Offline-Ertrag mindestens ${Numbers.formatPercent(fraction)}"

        is PrestigeEffect.OfflineCapHours ->
            "Offline-Zeit zählt bis zu ${hours.toInt()} Stunden"

        is PrestigeEffect.StartingCollectors ->
            "Jeder Kollektor startet mit $count Stück"

        is PrestigeEffect.StartingMass ->
            "Start mit ${Numbers.formatMass(mass)}"

        is PrestigeEffect.CometFrequency ->
            "Kometen kommen ${Numbers.formatMultiplier(factor)} so oft"

        is PrestigeEffect.GlobalMultiplier ->
            "${Numbers.formatMultiplier(factor)} auf alles, dauerhaft"

        is PrestigeEffect.SingularityGain ->
            "${Numbers.formatMultiplier(factor)} Singularitäten je Kollaps"

        is PrestigeEffect.TapMultiplier ->
            "${Numbers.formatMultiplier(factor)} Masse pro Tipp, dauerhaft"

        is PrestigeEffect.AutoTap ->
            "Tippt ${Numbers.format(perSecond)}× pro Sekunde von allein"

        is PrestigeEffect.SingularityBonus ->
            "Jede Singularität gibt ${Numbers.formatPercent(perSingularity)} statt " +
                "${Numbers.formatPercent(GameEngine.SINGULARITY_BONUS)}"

        is PrestigeEffect.AutoBuy ->
            "Kauft Kollektoren von allein, sobald du das Vierfache übrig hast"

        is PrestigeEffect.MilestoneBonus ->
            "Jeder Meilenstein gibt ${Numbers.formatPercent(Milestones.FACTOR - 1.0 + extra)} " +
                "statt ${Numbers.formatPercent(Milestones.FACTOR - 1.0)}"

        is PrestigeEffect.FusionRate ->
            "Jede Fusionsstufe läuft ${Numbers.formatMultiplier(factor)} so schnell"

        is PrestigeEffect.ResearchSpeed ->
            "Forschung dauert nur noch ${Numbers.formatPercent(1.0 / factor)} der Zeit"
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
    val name: String,
    val flavor: String,
    /** Singularities it costs. */
    val cost: Double,
    val effect: PrestigeEffect,
    /** Collapses the player needs before this is even shown. */
    val requiredCollapses: Int = 0,
) {
    val effectText: String get() = effect.text
}

object PrestigeUpgrades {

    val all: List<PrestigeUpgrade> = listOf(
        PrestigeUpgrade(
            id = "p_offline_1",
            name = "Wache Drohnen",
            flavor = "Sie hören nicht auf, nur weil du weg bist.",
            cost = 3.0,
            effect = PrestigeEffect.OfflineEfficiency(1.0),
        ),
        PrestigeUpgrade(
            id = "p_start_mass",
            name = "Rücklage",
            flavor = "Ein Rest Masse, den der Kollaps nicht mitgenommen hat.",
            cost = 5.0,
            effect = PrestigeEffect.StartingMass(50_000.0),
        ),
        PrestigeUpgrade(
            id = "p_tap",
            name = "Eingeübter Griff",
            flavor = "Die Hände erinnern sich an jeden Durchlauf.",
            cost = 6.0,
            effect = PrestigeEffect.TapMultiplier(5.0),
        ),
        PrestigeUpgrade(
            id = "p_comet_1",
            name = "Kometenbahn",
            flavor = "Du weißt inzwischen, wo man wartet.",
            cost = 8.0,
            effect = PrestigeEffect.CometFrequency(2.0),
        ),
        PrestigeUpgrade(
            id = "p_auto_1",
            name = "Kleiner Automat",
            flavor = "Ein Arm, ein Motor, ein Takt. Er wird nicht müde und beschwert sich nie.",
            cost = 12.0,
            effect = PrestigeEffect.AutoTap(3.0),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_auto_2",
            name = "Schlagwerk",
            flavor = "Zehn Arme im Takt. Du darfst zusehen — oder mittippen, das zählt dazu.",
            cost = 40.0,
            effect = PrestigeEffect.AutoTap(10.0),
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_offline_2",
            name = "Langzeitspeicher",
            flavor = "Lagert die Ausbeute einen ganzen Tag lang ein.",
            cost = 10.0,
            effect = PrestigeEffect.OfflineCapHours(24.0),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_collectors_1",
            name = "Bewahrte Baupläne",
            flavor = "Der Kollaps frisst die Anlagen, nicht das Wissen.",
            cost = 15.0,
            effect = PrestigeEffect.StartingCollectors(5),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_global_1",
            name = "Verdichtete Materie",
            flavor = "Was einmal durch ein schwarzes Loch ging, wiegt mehr.",
            cost = 20.0,
            effect = PrestigeEffect.GlobalMultiplier(3.0),
            requiredCollapses = 1,
        ),
        PrestigeUpgrade(
            id = "p_autobuy",
            name = "Selbsttätige Beschaffung",
            flavor = "Sie kauft nach, wenn reichlich da ist, und lässt dir den Rest für Upgrades.",
            cost = 25.0,
            effect = PrestigeEffect.AutoBuy,
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_singularity",
            name = "Saubere Trennung",
            flavor = "Beim Kollabieren geht weniger verloren.",
            cost = 30.0,
            effect = PrestigeEffect.SingularityGain(1.5),
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_collectors_2",
            name = "Vorgefertigte Flotte",
            flavor = "Der nächste Durchlauf beginnt nicht mehr bei null.",
            cost = 45.0,
            effect = PrestigeEffect.StartingCollectors(25),
            requiredCollapses = 2,
        ),
        PrestigeUpgrade(
            id = "p_global_2",
            name = "Entropiekonto",
            flavor = "Die Unordnung von neun Universen, gebündelt.",
            cost = 80.0,
            effect = PrestigeEffect.GlobalMultiplier(5.0),
            requiredCollapses = 3,
        ),
        // Everything below here exists because the road got longer. Eight universes now stand
        // between a new save and the catalogue ladder, and a shelf that was fully bought by the
        // third collapse left most of that road with nothing new on it.
        PrestigeUpgrade(
            id = "p_global_3",
            name = "Dritte Ausdehnung",
            flavor = "Der Raum hat sich daran gewöhnt, für dich zu arbeiten.",
            cost = 150.0,
            effect = PrestigeEffect.GlobalMultiplier(8.0),
            requiredCollapses = 5,
        ),
        PrestigeUpgrade(
            id = "p_collectors_3",
            name = "Vollständiges Lagerverzeichnis",
            flavor = "Nicht mehr nur die Liste überlebt, sondern auch, wo alles stand.",
            cost = 220.0,
            effect = PrestigeEffect.StartingCollectors(80),
            requiredCollapses = 6,
        ),
        PrestigeUpgrade(
            id = "p_offline_3",
            name = "Dauerbetrieb",
            flavor = "Drei Tage ohne dich, und niemand hat gemerkt, dass du weg warst.",
            cost = 300.0,
            effect = PrestigeEffect.OfflineCapHours(72.0),
            requiredCollapses = 7,
        ),
        PrestigeUpgrade(
            id = "p_fusion_1",
            name = "Durchgeheizt",
            flavor = "Die Öfen gehen zwischen zwei Universen nicht mehr aus.",
            cost = 400.0,
            effect = PrestigeEffect.FusionRate(2.5),
            requiredCollapses = 8,
        ),
        PrestigeUpgrade(
            id = "p_research_1",
            name = "Übertragene Notizen",
            flavor = "Was einmal verstanden wurde, muss nicht zweimal verstanden werden.",
            cost = 550.0,
            effect = PrestigeEffect.ResearchSpeed(2.0),
            requiredCollapses = 10,
        ),
        PrestigeUpgrade(
            id = "p_auto_3",
            name = "Unermüdlich",
            flavor = "Dreißig Schläge in der Sekunde, und keiner davon von dir.",
            cost = 700.0,
            effect = PrestigeEffect.AutoTap(30.0),
            requiredCollapses = 12,
        ),
        PrestigeUpgrade(
            id = "p_milestone_1",
            name = "Eingefahrene Serien",
            flavor = "Die Fertigung kennt jede Auflage, die es je gegeben hat.",
            cost = 900.0,
            effect = PrestigeEffect.MilestoneBonus(0.05),
            requiredCollapses = 14,
        ),
        PrestigeUpgrade(
            id = "p_comet_2",
            name = "Dichter Trümmergürtel",
            flavor = "Von acht Universen bleibt einiges liegen, und alles davon fliegt.",
            cost = 1_100.0,
            effect = PrestigeEffect.CometFrequency(3.0),
            requiredCollapses = 16,
        ),
        PrestigeUpgrade(
            id = "p_global_4",
            name = "Vierte Ausdehnung",
            flavor = "Irgendwann fragt der Raum nicht mehr nach, er dehnt sich einfach.",
            cost = 1_400.0,
            effect = PrestigeEffect.GlobalMultiplier(15.0),
            requiredCollapses = 18,
        ),
        PrestigeUpgrade(
            id = "p_singularity_2",
            name = "Doppelter Schnitt",
            flavor = "Zwei Singularitäten, wo vorher eine war. Frag nicht, welche die echte ist.",
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
