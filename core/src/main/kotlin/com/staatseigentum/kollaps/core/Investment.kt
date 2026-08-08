package com.staatseigentum.kollaps.core

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * A singularity sink that never runs dry.
 *
 * The thirteen prestige upgrades are one-off purchases, and a player who has bought all of them
 * has nothing left to do with the currency the whole collapse loop exists to produce. That is the
 * hole this fills: every investment can be bought again, each level costs more than the last, and
 * the price curve rather than a list length is what decides when to stop.
 *
 * Every effect is linear in the level — `1 + level × something`, never `something ^ level`. An
 * exponential bought with an exponentially priced currency is a race between two curves, and the
 * one that wins is decided by rounding rather than by design. Linear means twenty levels is worth
 * twenty times one level, which is a sentence a player can act on.
 */
data class Investment(
    val id: String,
    val name: String,
    val flavor: String,
    /** What a level says it does, for the row. */
    val perLevel: String,
    val baseCost: Double,
    /** Price multiplier per level already owned. */
    val growth: Double,
    val maxLevel: Int,
    val requiredCollapses: Int = 0,
    /**
     * The whole effect of owning this many levels, not the effect of one more.
     *
     * Total rather than incremental because [GameEngine] folds each effect exactly once. Handing
     * it a per-level effect and applying it repeatedly is how a multiplier turns into a power.
     */
    val effectAt: (Int) -> PrestigeEffect,
) {
    /** Price of going from [level] to [level] + 1. */
    fun costAt(level: Int): Double = baseCost * growth.pow(level)

    /** Price of [amount] more levels on top of [level]. */
    fun costForLevels(level: Int, amount: Int): Double {
        if (amount <= 0) return 0.0
        return costAt(level) * (growth.pow(amount) - 1.0) / (growth - 1.0)
    }

    /** How many more levels [singularities] buys, never past [maxLevel]. */
    fun affordableLevels(level: Int, singularities: Double): Int {
        val room = maxLevel - level
        if (room <= 0 || singularities < costAt(level)) return 0
        val ratio = singularities * (growth - 1.0) / costAt(level) + 1.0
        return floor(ln(ratio) / ln(growth)).toInt().coerceIn(0, room)
    }
}

object Investments {

    val all: List<Investment> = listOf(
        Investment(
            id = "i_start_mass",
            name = "Rücklagenkonto",
            flavor = "Jeder Durchlauf legt etwas zur Seite, das der nächste vorfindet.",
            perLevel = "je Stufe ×4 Startmasse",
            baseCost = 4.0,
            growth = 1.26,
            maxLevel = 40,
            // Starting mass may be exponential: it is a head start on a run whose numbers grow
            // exponentially anyway, so a linear one would be worthless by the third collapse.
            // Minus one, so the first level adds to the flat upgrade rather than repeating it.
            effectAt = { level -> PrestigeEffect.StartingMass(50_000.0 * (4.0.pow(level) - 1.0)) },
        ),
        Investment(
            id = "i_tap",
            name = "Muskelgedächtnis",
            flavor = "Die Hand weiß, wo sie hinschlägt, bevor der Kopf es merkt.",
            perLevel = "je Stufe +30 % pro Tipp",
            baseCost = 5.0,
            growth = 1.28,
            maxLevel = 60,
            effectAt = { level -> PrestigeEffect.TapMultiplier(1.0 + 0.30 * level) },
        ),
        Investment(
            id = "i_global",
            name = "Verdichtung",
            flavor = "Was oft genug durch einen Horizont ging, bleibt dichter zurück.",
            perLevel = "je Stufe +12 % auf alles",
            baseCost = 8.0,
            growth = 1.32,
            maxLevel = 80,
            requiredCollapses = 1,
            effectAt = { level -> PrestigeEffect.GlobalMultiplier(1.0 + 0.12 * level) },
        ),
        Investment(
            id = "i_comets",
            name = "Bahnrechnung",
            flavor = "Du weißt inzwischen nicht nur wo, sondern auch wann.",
            perLevel = "je Stufe +15 % Kometen",
            baseCost = 7.0,
            growth = 1.30,
            maxLevel = 25,
            requiredCollapses = 1,
            effectAt = { level -> PrestigeEffect.CometFrequency(1.0 + 0.15 * level) },
        ),
        Investment(
            id = "i_offline_cap",
            name = "Tiefkühlhalle",
            flavor = "Reihe um Reihe Kammern, und alle nehmen weiter an.",
            perLevel = "je Stufe +3 Stunden offline",
            baseCost = 9.0,
            growth = 1.30,
            maxLevel = 40,
            requiredCollapses = 1,
            effectAt = { level -> PrestigeEffect.OfflineCapHours(24.0 + 3.0 * level) },
        ),
        Investment(
            id = "i_offline_share",
            name = "Nachtschicht",
            flavor = "Irgendwann arbeitet die Flotte ohne dich genauso gut wie mit dir.",
            perLevel = "je Stufe +4 Punkte Offline-Ausbeute",
            baseCost = 10.0,
            growth = 1.34,
            // Twelve levels is the whole way from the base share to everything; beyond that a
            // level would be a purchase with nothing behind it.
            maxLevel = 13,
            requiredCollapses = 1,
            effectAt = { level ->
                PrestigeEffect.OfflineEfficiency(
                    (GameEngine.BASE_OFFLINE_EFFICIENCY + 0.04 * level).coerceAtMost(1.0),
                )
            },
        ),
        Investment(
            id = "i_fleet",
            name = "Eingelagerte Flotte",
            flavor = "Nicht die Anlagen überleben den Kollaps, sondern das Lagerverzeichnis.",
            perLevel = "je Stufe +3 Kollektoren zum Start",
            baseCost = 16.0,
            growth = 1.34,
            maxLevel = 60,
            requiredCollapses = 2,
            effectAt = { level -> PrestigeEffect.StartingCollectors(25 + 3 * level) },
        ),
        Investment(
            id = "i_milestone",
            name = "Serienfertigung",
            flavor = "Jede fünfundzwanzigste Maschine ist ein bisschen besser als die davor.",
            perLevel = "je Stufe +1 Punkt je Meilenstein",
            baseCost = 22.0,
            growth = 1.36,
            maxLevel = 25,
            requiredCollapses = 2,
            effectAt = { level -> PrestigeEffect.MilestoneBonus(0.01 * level) },
        ),
        Investment(
            id = "i_fusion",
            name = "Brennkammern",
            flavor = "Mehr Öfen an derselben Kette, alle mit demselben Feuer.",
            perLevel = "je Stufe +20 % Fusionstempo",
            baseCost = 26.0,
            growth = 1.32,
            maxLevel = 40,
            requiredCollapses = 2,
            effectAt = { level -> PrestigeEffect.FusionRate(1.0 + 0.20 * level) },
        ),
        Investment(
            id = "i_research",
            name = "Zweite Schicht",
            flavor = "Das Labor läuft jetzt auch nachts. Warten muss man trotzdem.",
            perLevel = "je Stufe +15 % Forschungstempo",
            baseCost = 30.0,
            growth = 1.34,
            maxLevel = 30,
            requiredCollapses = 2,
            effectAt = { level -> PrestigeEffect.ResearchSpeed(1.0 + 0.15 * level) },
        ),
        Investment(
            id = "i_bonus",
            name = "Gebündelte Enden",
            flavor = "Singularitäten liegen dichter, wenn man sie ordentlich stapelt.",
            perLevel = "je Stufe +2 Punkte je Singularität",
            baseCost = 35.0,
            growth = 1.38,
            maxLevel = 40,
            requiredCollapses = 3,
            effectAt = { level ->
                PrestigeEffect.SingularityBonus(GameEngine.SINGULARITY_BONUS + 0.02 * level)
            },
        ),
        Investment(
            id = "i_gain",
            name = "Sauberer Schnitt",
            flavor = "Beim nächsten Kollaps geht weniger daneben.",
            // The one that pays in its own currency, so it is the steepest and the shortest.
            perLevel = "je Stufe +8 % Singularitäten je Kollaps",
            baseCost = 45.0,
            growth = 1.45,
            maxLevel = 25,
            requiredCollapses = 3,
            effectAt = { level -> PrestigeEffect.SingularityGain(1.0 + 0.08 * level) },
        ),
    )

    private val index: Map<String, Investment> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Investitions-ID im Katalog" }
    }

    fun byId(id: String?): Investment? = if (id == null) null else index[id]

    fun levelOf(state: GameState, investment: Investment): Int =
        (state.investments[investment.id] ?: 0).coerceIn(0, investment.maxLevel)

    /** What the player can see: enough collapses behind them. Bought-out ones stay, greyed. */
    fun offered(state: GameState): List<Investment> =
        all.filter { state.collapses >= it.requiredCollapses || levelOf(state, it) > 0 }

    /** Singularities sunk into investments so far, for the statistics. */
    fun totalLevels(state: GameState): Int = all.sumOf { levelOf(state, it) }
}
