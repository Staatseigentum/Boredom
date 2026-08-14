package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * An idle producer. Every owned copy adds [baseRate] kg/s before any multiplier is applied.
 * Prices grow geometrically with the number already owned, Cookie-Clicker style.
 */
data class Collector(
    val id: String,
    val germanName: String,
    val germanFlavor: String,
    val baseCost: Double,
    val baseRate: Double,
    /**
     * Whether this machine belongs to the catalogue ladder rather than to the named one.
     *
     * A gate and not a price. The obvious way to keep the endgame fleet out of a first run is to
     * make it unaffordable, but the cost curve is not free to move: every collector's price sits
     * about seven orders above its output, and that ratio is what makes buying the next machine a
     * decision rather than an obvious yes. Pricing these above the black hole would have meant
     * lifting their output to match, which puts a factor of a million between the Omega and its
     * successor and turns a smooth ladder into a cliff.
     *
     * So the curve stays smooth and the shop simply does not offer them yet. Measured, not
     * guessed: on the old rules the bot bought three of these before the black hole and finished
     * the opening run in three hours instead of three and a half.
     */
    val catalogueOnly: Boolean = false,
    // Anything reading this must also remember that it gates the *head start*, not only the shop:
    // see `GameEngine.startingCollectors`.
) {
    /** What the shop row says. Translated here so no call site has to remember to. */
    val name: String get() = Lang.t(germanName)
    val flavor: String get() = Lang.t(germanFlavor)

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

        /**
         * The most copies of any one collector that can be built.
         *
         * There was no limit at all before this, and the only thing stopping anybody was the price
         * curve — which is not a design, it is arithmetic that happens to look like one. A number
         * the player can see coming and aim at is a design, and this one lands exactly on a
         * milestone, so the counter arrives at the cap and at its last bonus in the same purchase
         * rather than trailing off between two of them.
         *
         * Five hundred to begin with, and two and a half thousand since the ladder grew. Five
         * hundred was chosen against a game that ended at the black hole; above it the catalogue
         * runs for sixteen thousand rungs, and a cap that low turned every one of them into the
         * same fleet with a larger multiplier in front of it. At 2 500 the price curve is what
         * bites again — the last copy costs 1.15^2499 times the first, which is a number with a
         * hundred and fifty digits — so the cap is a horizon rather than a wall somebody hits on
         * a Tuesday.
         *
         * A save from before any cap that somehow holds more keeps every copy it has. Taking them
         * away would be the rules reaching backwards into a game already played; refusing to sell
         * one more is enough.
         */
        const val MAX_OWNED = 2_500
    }
}

object Collectors {

    val all: List<Collector> = listOf(
        Collector(
            id = "dust",
            germanName = "Staubfänger",
            germanFlavor = "Ein Netz aus Folie, das im Vakuum treibt und Krümel einsammelt.",
            baseCost = 15.0,
            baseRate = 0.1,
        ),
        Collector(
            id = "net",
            germanName = "Asteroidennetz",
            germanFlavor = "Wirf es aus, warte, zieh es ein. Weltraumfischen eben.",
            baseCost = 100.0,
            baseRate = 1.0,
        ),
        Collector(
            id = "drone",
            germanName = "Bergbaudrohne",
            germanFlavor = "Bohrt, kaut, spuckt Gestein aus. Fragt nie nach Pause.",
            baseCost = 1_100.0,
            baseRate = 8.0,
        ),
        Collector(
            id = "refinery",
            germanName = "Orbitalraffinerie",
            germanFlavor = "Trennt Wertvolles von Schotter, direkt im Orbit.",
            baseCost = 12_000.0,
            baseRate = 47.0,
        ),
        Collector(
            id = "driver",
            germanName = "Massetreiber",
            germanFlavor = "Eine Kanone, die ganze Berge in deine Umlaufbahn schießt.",
            baseCost = 130_000.0,
            baseRate = 260.0,
        ),
        Collector(
            id = "comet",
            germanName = "Kometenfänger",
            germanFlavor = "Fängt Eisbrocken ein, bevor sie irgendwo einschlagen.",
            baseCost = 1_400_000.0,
            baseRate = 1_400.0,
        ),
        Collector(
            id = "dyson",
            germanName = "Dyson-Schwarm",
            germanFlavor = "Millionen Spiegel, die einem Stern die Energie abknöpfen.",
            baseCost = 20_000_000.0,
            baseRate = 7_800.0,
        ),
        Collector(
            id = "forge",
            germanName = "Sternenschmiede",
            germanFlavor = "Fusioniert leichte Kerne zu schweren. Laut. Sehr laut.",
            baseCost = 330_000_000.0,
            baseRate = 44_000.0,
        ),
        Collector(
            id = "quantum",
            germanName = "Quantenkollektor",
            germanFlavor = "Schöpft Teilchen direkt aus dem Nichts. Ist erlaubt, wenn man schnell ist.",
            baseCost = 5_100_000_000.0,
            baseRate = 260_000.0,
        ),
        Collector(
            id = "extractor",
            germanName = "Singularitätsextraktor",
            germanFlavor = "Zapft den Rand eines Ereignishorizonts an. Vorsichtig.",
            baseCost = 75_000_000_000.0,
            baseRate = 1_600_000.0,
        ),
        Collector(
            id = "dilator",
            germanName = "Zeitdilatator",
            germanFlavor = "Draußen vergeht eine Sekunde, drinnen eine Woche Schichtarbeit.",
            baseCost = 1_000_000_000_000.0,
            baseRate = 10_000_000.0,
        ),
        Collector(
            id = "echo",
            germanName = "Urknall-Echo",
            germanFlavor = "Fängt den Nachhall des ersten Augenblicks ein und presst ihn zu Materie.",
            baseCost = 14_000_000_000_000.0,
            baseRate = 65_000_000.0,
        ),
        Collector(
            id = "vakuum",
            germanName = "Vakuumdestillat",
            germanFlavor = "Destilliert das Nichts, bis unten etwas übrig bleibt. Fragt nicht, was.",
            baseCost = 200_000_000_000_000.0,
            baseRate = 420_000_000.0,
        ),
        Collector(
            id = "faltwerk",
            germanName = "Faltwerk",
            germanFlavor = "Legt den Raum in Falten und schüttelt aus, was zwischen ihnen hängt.",
            baseCost = 2_800_000_000_000_000.0,
            baseRate = 2_700_000_000.0,
        ),
        Collector(
            id = "weber",
            germanName = "Kausalitätsweber",
            germanFlavor = "Knüpft Ursache an Wirkung, bis Materie der kürzeste Weg zwischen beiden ist.",
            baseCost = 40_000_000_000_000_000.0,
            baseRate = 17_500_000_000.0,
        ),
        Collector(
            id = "urgrund",
            germanName = "Urgrund-Anzapfung",
            germanFlavor = "Unter allem liegt noch etwas. Von dort holt sie es hoch.",
            baseCost = 550_000_000_000_000_000.0,
            baseRate = 115_000_000_000.0,
        ),
        Collector(
            id = "omega",
            germanName = "Omega-Kollektor",
            germanFlavor = "Sammelt ein, was übrig sein wird. Rückwärts, vom Ende her.",
            baseCost = 7_500_000_000_000_000_000.0,
            baseRate = 750_000_000_000.0,
        ),
        // Everything past here exists because the ladder does. The catalogue rungs above the black
        // hole run for sixteen thousand steps, and a fleet that topped out at the Omega would have
        // spent all of them buying the same seventeen machines.
        Collector(
            id = "entropie",
            germanName = "Entropiemühle",
            germanFlavor = "Mahlt Unordnung zurück zu Ordnung. Läuft rückwärts und beschwert sich nicht.",
            baseCost = 1.0e20,
            baseRate = 5.0e12,
            catalogueOnly = true,
        ),
        Collector(
            id = "horizont",
            germanName = "Horizontpflug",
            germanFlavor = "Zieht Furchen in den Ereignishorizont und erntet, was dabei hochkommt.",
            baseCost = 1.4e21,
            baseRate = 3.2e13,
            catalogueOnly = true,
        ),
        Collector(
            id = "nullpunkt",
            germanName = "Nullpunktpresse",
            germanFlavor = "Presst das leerste Vakuum, bis unten Zahlen herauslaufen.",
            baseCost = 2.0e22,
            baseRate = 2.1e14,
            catalogueOnly = true,
        ),
        Collector(
            id = "schleuse",
            germanName = "Ewigkeitsschleuse",
            germanFlavor = "Öffnet sich einmal pro Ewigkeit. Die Ewigkeiten sind kürzer geworden.",
            baseCost = 3.0e23,
            baseRate = 1.4e15,
            catalogueOnly = true,
        ),
        Collector(
            id = "alpha",
            germanName = "Alpha-Rückgriff",
            germanFlavor = "Greift zurück bis vor den Anfang und nimmt mit, was dort noch liegt.",
            baseCost = 4.5e24,
            baseRate = 9.0e15,
            catalogueOnly = true,
        ),
    )

    private val index: Map<String, Collector> = all.associateBy { it.id }

    fun byId(id: String): Collector? = index[id]

    fun require(id: String): Collector =
        index[id] ?: error("Unbekannter Kollektor: $id")
}
