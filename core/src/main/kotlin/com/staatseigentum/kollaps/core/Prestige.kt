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
    val effectText: String
        get() = when (effect) {
            is PrestigeEffect.OfflineEfficiency ->
                "Offline-Ertrag mindestens ${Numbers.formatPercent(effect.fraction)}"

            is PrestigeEffect.OfflineCapHours ->
                "Offline-Zeit zählt bis zu ${effect.hours.toInt()} Stunden"

            is PrestigeEffect.StartingCollectors ->
                "Jeder Kollektor startet mit ${effect.count} Stück"

            is PrestigeEffect.StartingMass ->
                "Start mit ${Numbers.formatMass(effect.mass)}"

            is PrestigeEffect.CometFrequency ->
                "Kometen kommen ${Numbers.formatMultiplier(effect.factor)} so oft"

            is PrestigeEffect.GlobalMultiplier ->
                "${Numbers.formatMultiplier(effect.factor)} auf alles, dauerhaft"

            is PrestigeEffect.SingularityGain ->
                "${Numbers.formatMultiplier(effect.factor)} Singularitäten je Kollaps"

            is PrestigeEffect.TapMultiplier ->
                "${Numbers.formatMultiplier(effect.factor)} Masse pro Tipp, dauerhaft"
        }
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
