package com.staatseigentum.kollaps.core

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/** What a fused element is worth. Two elements may pull on the same lever. */
enum class FusionBonus(val label: String) {
    GLOBAL("Gesamtproduktion"),
    TAP("Masse pro Tipp"),
    OFFLINE("Offline-Ausbeute"),
    COMETS("Kometenhäufigkeit"),
    SINGULARITY("Singularitäten"),
}

/**
 * One rung of the fusion chain.
 *
 * Deliberately the real sequence a star burns through — hydrogen to helium to carbon, then oxygen,
 * silicon and finally iron. Iron is where it stops, in the game for the same reason it stops in a
 * star: fusing it costs more than it gives back. That is why it is the last one and why it pays
 * out in singularities rather than in production — the only thing left to do with an iron core is
 * let it fall in.
 *
 * The bonus is logarithmic on purpose. Element counts climb without limit once the chain runs, and
 * anything linear would have turned the last third of the ladder into a formality.
 */
enum class Element(
    val id: String,
    val symbol: String,
    val label: String,
    val flavor: String,
    /** How much [bonus] grows per tenfold increase in the amount held. */
    val perDecade: Double,
    val bonus: FusionBonus,
) {
    WASSERSTOFF(
        id = "h",
        symbol = "H",
        label = "Wasserstoff",
        flavor = "Der erste Stoff überhaupt. Alles andere ist daraus gemacht.",
        perDecade = 0.05,
        bonus = FusionBonus.GLOBAL,
    ),
    HELIUM(
        id = "he",
        symbol = "He",
        label = "Helium",
        flavor = "Asche des ersten Feuers. Wurde am Himmel entdeckt, bevor jemand es in der Hand hatte.",
        perDecade = 0.12,
        bonus = FusionBonus.TAP,
    ),
    KOHLENSTOFF(
        id = "c",
        symbol = "C",
        label = "Kohlenstoff",
        flavor = "Drei Heliumkerne, die sich gleichzeitig treffen. Unwahrscheinlich, und doch bist du daraus.",
        perDecade = 0.09,
        bonus = FusionBonus.GLOBAL,
    ),
    SAUERSTOFF(
        id = "o",
        symbol = "O",
        label = "Sauerstoff",
        flavor = "Das dritthäufigste Element im Universum, und das erste, das jemand vermisst.",
        perDecade = 0.05,
        bonus = FusionBonus.OFFLINE,
    ),
    SILIZIUM(
        id = "si",
        symbol = "Si",
        label = "Silizium",
        flavor = "Sand, Glas, Rechner. Im Stern bleibt dafür etwa ein Tag Zeit.",
        perDecade = 0.15,
        bonus = FusionBonus.COMETS,
    ),
    EISEN(
        id = "fe",
        symbol = "Fe",
        label = "Eisen",
        flavor = "Hier hört Fusion auf zu zahlen. Was jetzt noch wächst, wächst nach innen.",
        perDecade = 0.20,
        bonus = FusionBonus.SINGULARITY,
    ),
    ;

    companion object {
        fun byId(id: String?): Element? = entries.firstOrNull { it.id == id }
    }
}

/**
 * A machine in the fusion chain: either the tap that brings hydrogen in, or a furnace that burns
 * one element into the next.
 *
 * Every level adds [baseRate] outputs per second, and every output eats [ratio] of the input. The
 * ratio is what makes the chain narrow towards the top: the further up an element sits, the less
 * of it there is, which is why the later ones are worth more per decade.
 */
data class FusionStage(
    val id: String,
    val name: String,
    val flavor: String,
    /** `null` for the intake, which makes its output out of nothing. */
    val input: Element?,
    val output: Element,
    val baseCost: Double,
    val baseRate: Double,
    val ratio: Double,
) {
    fun costAt(level: Int): Double = baseCost * COST_GROWTH.pow(level)

    companion object {
        /** Steeper than a collector's: a fusion level is worth far more than one more drone. */
        const val COST_GROWTH = 1.30
    }
}

object Fusion {

    /**
     * The body that first ignites.
     *
     * A brown dwarf is the honest place to start — it is exactly the mass at which deuterium
     * begins to burn, and the reason a brown dwarf is called a failed star rather than a large
     * planet. Before it there is nothing hot enough in the middle to fuse anything at all.
     */
    const val UNLOCK_TIER = "Brauner Zwerg"

    /** Levels bought at once, mirroring the collector shop's bulk buttons. */
    const val MAX_LEVEL_STEP = 25

    val stages: List<FusionStage> = listOf(
        FusionStage(
            id = "intake",
            name = "Wasserstoffzapfung",
            flavor = "Schöpft den dünnen Nebel zwischen den Sternen ab und drückt ihn nach innen.",
            input = null,
            output = Element.WASSERSTOFF,
            baseCost = 5e10,
            baseRate = 2.0,
            ratio = 0.0,
        ),
        FusionStage(
            id = "pp",
            name = "Protonenkette",
            flavor = "Vier Protonen gehen hinein, ein Heliumkern kommt heraus. Der Rest wird Licht.",
            input = Element.WASSERSTOFF,
            output = Element.HELIUM,
            baseCost = 3e11,
            baseRate = 1.5,
            ratio = 2.5,
        ),
        FusionStage(
            id = "triple",
            name = "Drei-Alpha-Ofen",
            flavor = "Zwingt drei Heliumkerne zur selben Sekunde an denselben Ort.",
            input = Element.HELIUM,
            output = Element.KOHLENSTOFF,
            baseCost = 2.5e12,
            baseRate = 1.2,
            ratio = 2.5,
        ),
        FusionStage(
            id = "alpha",
            name = "Alpha-Prozess",
            flavor = "Kohlenstoff fängt ein weiteres Helium ein. Der Stern merkt kaum, dass er brennt.",
            input = Element.KOHLENSTOFF,
            output = Element.SAUERSTOFF,
            baseCost = 2e13,
            baseRate = 1.0,
            ratio = 2.5,
        ),
        FusionStage(
            id = "burning",
            name = "Sauerstoffbrand",
            flavor = "Die vorletzte Stufe. Ab hier zählt der Stern in Tagen statt in Jahrmillionen.",
            input = Element.SAUERSTOFF,
            output = Element.SILIZIUM,
            baseCost = 1.6e14,
            baseRate = 0.8,
            ratio = 2.5,
        ),
        FusionStage(
            id = "silicon",
            name = "Siliziumbrand",
            flavor = "Vierundzwanzig Stunden, dann steht ein Eisenkern im Zentrum und alles ist vorbei.",
            input = Element.SILIZIUM,
            output = Element.EISEN,
            baseCost = 1.2e15,
            baseRate = 0.6,
            ratio = 2.5,
        ),
    )

    private val index: Map<String, FusionStage> = stages.associateBy { it.id }

    init {
        require(index.size == stages.size) { "Doppelte Fusionsstufen-ID" }
    }

    fun byId(id: String?): FusionStage? = if (id == null) null else index[id]

    /** True once the body is hot enough in the middle for any of this to run. */
    fun isUnlocked(state: GameState): Boolean =
        GameEngine.tierOf(state).index >= Tiers.indexOf(UNLOCK_TIER) ||
            state.fusers.values.any { it > 0 }

    fun amountOf(state: GameState, element: Element): Double = state.elements[element.id] ?: 0.0

    fun levelOf(state: GameState, stage: FusionStage): Int = state.fusers[stage.id] ?: 0

    /**
     * What holding this much of an element multiplies its lever by.
     *
     * One at nothing, so an element nobody has produced yet costs the player nothing either.
     */
    fun factor(state: GameState, element: Element): Double =
        1.0 + element.perDecade * log10(1.0 + amountOf(state, element))

    /** The combined factor of every element pulling on [bonus]. */
    fun factorFor(state: GameState, bonus: FusionBonus): Double =
        Element.entries.filter { it.bonus == bonus }
            .fold(1.0) { acc, element -> acc * factor(state, element) }

    /**
     * Runs the chain for [seconds].
     *
     * In order from the intake upwards, so hydrogen tapped this tick can be burnt in the same one.
     * The alternative — settling every stage against the amounts as they were at the start — would
     * make a freshly built chain sit idle for as many ticks as it has rungs, which reads as broken
     * rather than as physics.
     */
    fun advance(state: GameState, seconds: Double): GameState {
        if (seconds <= 0.0) return state
        if (!isUnlocked(state)) return state

        val rate = GameEngine.fusionRate(state)
        var amounts: MutableMap<String, Double>? = null
        for (stage in stages) {
            val level = levelOf(state, stage)
            if (level <= 0) continue

            val capacity = level * stage.baseRate * seconds * rate
            val current = amounts ?: state.elements.toMutableMap().also { amounts = it }

            val produced = if (stage.input == null) {
                capacity
            } else {
                val available = current[stage.input.id] ?: 0.0
                val possible = min(capacity, available / stage.ratio)
                if (possible <= 0.0) continue
                current[stage.input.id] = available - possible * stage.ratio
                possible
            }
            current[stage.output.id] = (current[stage.output.id] ?: 0.0) + produced
        }

        val settled = amounts ?: return state
        return state.copy(elements = settled)
    }

    /**
     * How many levels [mass] would buy on top of [level], capped at [MAX_LEVEL_STEP].
     *
     * Walked one at a time rather than solved: the cap is small, and the closed form for a
     * geometric series is one rounding error away from offering a level the player cannot pay for.
     */
    fun affordableLevels(stage: FusionStage, level: Int, mass: Double): Int {
        var count = 0
        var spent = 0.0
        while (count < MAX_LEVEL_STEP) {
            val next = spent + stage.costAt(level + count)
            if (next > mass) break
            spent = next
            count++
        }
        return count
    }

    /** Price of [amount] more levels on top of [level]. */
    fun costForLevels(stage: FusionStage, level: Int, amount: Int): Double {
        if (amount <= 0) return 0.0
        val growth = FusionStage.COST_GROWTH
        return stage.costAt(level) * (growth.pow(amount) - 1.0) / (growth - 1.0)
    }

    /** Elements are part of a run, so a reset takes them along. Nothing here survives a collapse. */
    fun cleared(): Map<String, Double> = emptyMap()

    /** Total elements held, for the achievements and the panel header. */
    fun totalHeld(state: GameState): Double = state.elements.values.sum()

    /** The highest element the player has ever actually made. */
    fun deepest(state: GameState): Element? =
        Element.entries.lastOrNull { amountOf(state, it) >= 1.0 }

    /** Whole units of an element, which is all the panel ever shows. */
    fun whole(amount: Double): Double = floor(amount)
}
