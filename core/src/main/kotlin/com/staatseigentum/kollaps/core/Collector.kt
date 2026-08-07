package com.staatseigentum.kollaps.core

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * An idle producer. Every owned copy adds [baseRate] kg/s before any multiplier is applied.
 * Prices grow geometrically with the number already owned, Cookie-Clicker style.
 */
data class Collector(
    val id: String,
    val name: String,
    val flavor: String,
    val baseCost: Double,
    val baseRate: Double,
) {
    /** Price of the next copy when [owned] are already built. */
    fun costAt(owned: Int): Double = baseCost * COST_GROWTH.pow(owned)

    /** Price of buying [amount] more copies on top of [owned]. */
    fun costForBulk(owned: Int, amount: Int): Double {
        if (amount <= 0) return 0.0
        // Geometric series: base * g^owned * (g^amount - 1) / (g - 1)
        return costAt(owned) * (COST_GROWTH.pow(amount) - 1.0) / (COST_GROWTH - 1.0)
    }

    /** How many copies [mass] buys on top of [owned]. */
    fun affordableCount(owned: Int, mass: Double): Int {
        if (mass < costAt(owned)) return 0
        val ratio = mass * (COST_GROWTH - 1.0) / costAt(owned) + 1.0
        return floor(ln(ratio) / ln(COST_GROWTH)).toInt().coerceAtLeast(0)
    }

    companion object {
        const val COST_GROWTH = 1.15
    }
}

object Collectors {

    val all: List<Collector> = listOf(
        Collector(
            id = "dust",
            name = "Staubfänger",
            flavor = "Ein Netz aus Folie, das im Vakuum treibt und Krümel einsammelt.",
            baseCost = 15.0,
            baseRate = 0.1,
        ),
        Collector(
            id = "net",
            name = "Asteroidennetz",
            flavor = "Wirf es aus, warte, zieh es ein. Weltraumfischen eben.",
            baseCost = 100.0,
            baseRate = 1.0,
        ),
        Collector(
            id = "drone",
            name = "Bergbaudrohne",
            flavor = "Bohrt, kaut, spuckt Gestein aus. Fragt nie nach Pause.",
            baseCost = 1_100.0,
            baseRate = 8.0,
        ),
        Collector(
            id = "refinery",
            name = "Orbitalraffinerie",
            flavor = "Trennt Wertvolles von Schotter, direkt im Orbit.",
            baseCost = 12_000.0,
            baseRate = 47.0,
        ),
        Collector(
            id = "driver",
            name = "Massetreiber",
            flavor = "Eine Kanone, die ganze Berge in deine Umlaufbahn schießt.",
            baseCost = 130_000.0,
            baseRate = 260.0,
        ),
        Collector(
            id = "comet",
            name = "Kometenfänger",
            flavor = "Fängt Eisbrocken ein, bevor sie irgendwo einschlagen.",
            baseCost = 1_400_000.0,
            baseRate = 1_400.0,
        ),
        Collector(
            id = "dyson",
            name = "Dyson-Schwarm",
            flavor = "Millionen Spiegel, die einem Stern die Energie abknöpfen.",
            baseCost = 20_000_000.0,
            baseRate = 7_800.0,
        ),
        Collector(
            id = "forge",
            name = "Sternenschmiede",
            flavor = "Fusioniert leichte Kerne zu schweren. Laut. Sehr laut.",
            baseCost = 330_000_000.0,
            baseRate = 44_000.0,
        ),
        Collector(
            id = "quantum",
            name = "Quantenkollektor",
            flavor = "Schöpft Teilchen direkt aus dem Nichts. Ist erlaubt, wenn man schnell ist.",
            baseCost = 5_100_000_000.0,
            baseRate = 260_000.0,
        ),
        Collector(
            id = "extractor",
            name = "Singularitätsextraktor",
            flavor = "Zapft den Rand eines Ereignishorizonts an. Vorsichtig.",
            baseCost = 75_000_000_000.0,
            baseRate = 1_600_000.0,
        ),
        Collector(
            id = "dilator",
            name = "Zeitdilatator",
            flavor = "Draußen vergeht eine Sekunde, drinnen eine Woche Schichtarbeit.",
            baseCost = 1_000_000_000_000.0,
            baseRate = 10_000_000.0,
        ),
        Collector(
            id = "echo",
            name = "Urknall-Echo",
            flavor = "Fängt den Nachhall des ersten Augenblicks ein und presst ihn zu Materie.",
            baseCost = 14_000_000_000_000.0,
            baseRate = 65_000_000.0,
        ),
        Collector(
            id = "vakuum",
            name = "Vakuumdestillat",
            flavor = "Destilliert das Nichts, bis unten etwas übrig bleibt. Fragt nicht, was.",
            baseCost = 200_000_000_000_000.0,
            baseRate = 420_000_000.0,
        ),
        Collector(
            id = "faltwerk",
            name = "Faltwerk",
            flavor = "Legt den Raum in Falten und schüttelt aus, was zwischen ihnen hängt.",
            baseCost = 2_800_000_000_000_000.0,
            baseRate = 2_700_000_000.0,
        ),
        Collector(
            id = "weber",
            name = "Kausalitätsweber",
            flavor = "Knüpft Ursache an Wirkung, bis Materie der kürzeste Weg zwischen beiden ist.",
            baseCost = 40_000_000_000_000_000.0,
            baseRate = 17_500_000_000.0,
        ),
        Collector(
            id = "urgrund",
            name = "Urgrund-Anzapfung",
            flavor = "Unter allem liegt noch etwas. Von dort holt sie es hoch.",
            baseCost = 550_000_000_000_000_000.0,
            baseRate = 115_000_000_000.0,
        ),
        Collector(
            id = "omega",
            name = "Omega-Kollektor",
            flavor = "Sammelt ein, was übrig sein wird. Rückwärts, vom Ende her.",
            baseCost = 7_500_000_000_000_000_000.0,
            baseRate = 750_000_000_000.0,
        ),
    )

    private val index: Map<String, Collector> = all.associateBy { it.id }

    fun byId(id: String): Collector? = index[id]

    fun require(id: String): Collector =
        index[id] ?: error("Unbekannter Kollektor: $id")
}
