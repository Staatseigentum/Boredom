package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * What is made in the collapse itself, out of the iron that was in the core.
 *
 * The fusion chain stops at iron because fusing iron costs more than it returns — which is true,
 * and which left the whole chain standing beside the game rather than inside it. Everything above
 * iron in the real universe is made in the seconds a star dies: free neutrons flood the core
 * faster than they can decay, iron soaks them up, and what falls out is gold, platinum and
 * uranium. That is exactly the moment this game already has a button for.
 *
 * These survive everything — the collapse that makes them, and the big bang after it. They are the
 * one thing that carries forward, which is also what happens: the heavy elements in your hand were
 * forged in a star that died before the sun existed.
 */
enum class HeavyElement(
    val id: String,
    val symbol: String,
    val germanLabel: String,
    val germanFlavor: String,
    /** How much of it one square root of iron yields. */
    val perRoot: Double,
    /** How much [bonus] grows per tenfold increase in the amount held. */
    val perDecade: Double,
    val bonus: FusionBonus,
) {
    GOLD(
        id = "au",
        symbol = "Au",
        germanLabel = "Gold",
        germanFlavor = "Jedes Gramm davon war einmal in einem Stern, der schon tot war, als die Sonne anfing.",
        perRoot = 0.5,
        perDecade = 0.18,
        bonus = FusionBonus.SINGULARITY,
    ),
    PLATIN(
        id = "pt",
        symbol = "Pt",
        germanLabel = "Platin",
        germanFlavor = "Seltener als Gold, härter als Gold, und genauso wenig von hier.",
        perRoot = 0.15,
        perDecade = 0.22,
        bonus = FusionBonus.GLOBAL,
    ),
    URAN(
        id = "u",
        symbol = "U",
        germanLabel = "Uran",
        germanFlavor = "Das schwerste, was ohne Hilfe entsteht. Es zerfällt seitdem und ist immer noch da.",
        perRoot = 0.04,
        perDecade = 0.30,
        bonus = FusionBonus.TAP,
    ),
    IRIDIUM(
        id = "ir",
        symbol = "Ir",
        germanLabel = "Iridium",
        germanFlavor = "Liegt weltweit in genau einer Gesteinsschicht. Darunter Dinosaurier, darüber keine.",
        perRoot = 0.030,
        perDecade = 0.34,
        bonus = FusionBonus.FUSION,
    ),
    OSMIUM(
        id = "os",
        symbol = "Os",
        germanLabel = "Osmium",
        germanFlavor = "Das dichteste Ding, das man anfassen kann. Ein Würfel davon steht, wo man ihn hinstellt.",
        perRoot = 0.018,
        perDecade = 0.38,
        bonus = FusionBonus.RESEARCH,
    ),
    PLUTONIUM(
        id = "pu",
        symbol = "Pu",
        germanLabel = "Plutonium",
        germanFlavor = "Kommt in der Natur praktisch nicht vor. In einem sterbenden Stern schon.",
        perRoot = 0.007,
        perDecade = 0.44,
        bonus = FusionBonus.COMETS,
    ),
    ;

    val label: String get() = Lang.t(germanLabel)

    val flavor: String get() = Lang.t(germanFlavor)

    companion object {
        fun byId(id: String?): HeavyElement? = entries.firstOrNull { it.id == id }
    }
}

object Heavy {

    /**
     * What a collapse would forge out of the iron held right now.
     *
     * The square root is the shape of the whole thing: doubling the iron in the core is worth
     * about forty percent more gold, not twice as much. Without it, one enormous run would be
     * worth more than every other run the player will ever have, and the answer to "should I
     * collapse" would stop being a question.
     */
    fun yieldFrom(iron: Double): Map<String, Double> {
        if (iron <= 0.0) return emptyMap()
        val root = sqrt(iron)
        return HeavyElement.entries
            .associate { it.id to floor(root * it.perRoot) }
            .filterValues { it >= 1.0 }
    }

    /** What the collapse would pay, for the card that asks the player to press it. */
    fun pending(state: GameState): Map<String, Double> =
        yieldFrom(Fusion.amountOf(state, Element.EISEN))

    /** Adds a collapse's worth of heavy elements to what is already held. */
    fun forge(held: Map<String, Double>, iron: Double): Map<String, Double> {
        val made = yieldFrom(iron)
        if (made.isEmpty()) return held
        return held.toMutableMap().apply {
            for ((id, amount) in made) combine(id, amount, Double::plus)
        }
    }

    fun amountOf(state: GameState, element: HeavyElement): Double = state.heavy[element.id] ?: 0.0

    /** What holding this much of an element multiplies its lever by. */
    fun factor(state: GameState, element: HeavyElement): Double =
        1.0 + element.perDecade * log10(1.0 + amountOf(state, element))

    /** The combined factor of every heavy element pulling on [bonus]. */
    fun factorFor(state: GameState, bonus: FusionBonus): Double =
        HeavyElement.entries.filter { it.bonus == bonus }
            .fold(1.0) { acc, element -> acc * factor(state, element) }

    /** True once the player has ever forged any. Nothing is shown before the first collapse. */
    fun isUnlocked(state: GameState): Boolean = state.heavy.values.any { it >= 1.0 }

    /** Everything held, for the statistics. */
    fun total(state: GameState): Double = state.heavy.values.sum()
}
