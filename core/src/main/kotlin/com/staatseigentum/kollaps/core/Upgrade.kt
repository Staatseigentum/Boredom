package com.staatseigentum.kollaps.core

/** What buying an upgrade changes. */
sealed interface UpgradeEffect {
    /** Adds a flat amount to the base mass per tap. */
    data class TapFlat(val amount: Double) : UpgradeEffect

    /** Multiplies the mass per tap. */
    data class TapMultiplier(val factor: Double) : UpgradeEffect

    /** Multiplies the output of one collector. */
    data class CollectorMultiplier(val collectorId: String, val factor: Double) : UpgradeEffect

    /** Multiplies every source of mass. */
    data class GlobalMultiplier(val factor: Double) : UpgradeEffect

    /** Each tap additionally yields this fraction of the current production per second. */
    data class TapFromProduction(val fraction: Double) : UpgradeEffect

    /** Raises the share of production that is credited while the app is closed. */
    data class OfflineEfficiency(val fraction: Double) : UpgradeEffect

    /** Raises how many hours of offline time are credited at most. */
    data class OfflineCapHours(val hours: Double) : UpgradeEffect
}

/** When an upgrade shows up in the shop. */
sealed interface UnlockCondition {
    data class CollectorsOwned(val collectorId: String, val count: Int) : UnlockCondition
    data class MassCollected(val amount: Double) : UnlockCondition
    data class TierReached(val tierIndex: Int) : UnlockCondition
    data class TapsMade(val count: Long) : UnlockCondition
    data object Always : UnlockCondition
}

enum class UpgradeCategory { TAP, COLLECTOR, COSMIC }

data class Upgrade(
    val id: String,
    val name: String,
    val flavor: String,
    val cost: Double,
    val effect: UpgradeEffect,
    val unlock: UnlockCondition,
    val category: UpgradeCategory,
) {
    /** Human readable summary of what this upgrade does. */
    val effectText: String
        get() = when (effect) {
            is UpgradeEffect.TapFlat ->
                "+${Numbers.format(effect.amount)} kg pro Tipp"

            is UpgradeEffect.TapMultiplier ->
                "${Numbers.formatMultiplier(effect.factor)} Masse pro Tipp"

            is UpgradeEffect.CollectorMultiplier ->
                "${Numbers.formatMultiplier(effect.factor)} ${Collectors.byId(effect.collectorId)?.name ?: effect.collectorId}"

            is UpgradeEffect.GlobalMultiplier ->
                "${Numbers.formatMultiplier(effect.factor)} auf alles"

            is UpgradeEffect.TapFromProduction ->
                "Tippen gibt zusätzlich ${Numbers.formatPercent(effect.fraction)} deiner Produktion"

            is UpgradeEffect.OfflineEfficiency ->
                "Offline-Ertrag auf ${Numbers.formatPercent(effect.fraction)}"

            is UpgradeEffect.OfflineCapHours ->
                "Offline-Zeit zählt bis zu ${effect.hours.toInt()} Stunden"
        }
}

object Upgrades {

    /** Multipliers applied to a collector's base price to get the price of its upgrades. */
    private val COLLECTOR_UPGRADE_STEPS = listOf(
        Triple(10, 20.0, "Mk II"),
        Triple(25, 250.0, "Mk III"),
        Triple(50, 3_000.0, "Mk IV"),
        Triple(100, 40_000.0, "Mk V"),
        Triple(200, 600_000.0, "Mk VI"),
    )

    /**
     * An unlock that names the body it waits for instead of its position on the ladder.
     *
     * Tier indices move whenever a step is inserted, and an unlock that quietly slid one rung
     * down is invisible until someone plays that far. The name is the stable thing.
     */
    private fun atTier(name: String) = UnlockCondition.TierReached(Tiers.indexOf(name))

    private val tapUpgrades: List<Upgrade> = listOf(
        upgradeTap("tap_1", "Verstärkte Finger", "Handschuhe mit Servomotoren. Billig, laut, effektiv.", 100.0, 200.0),
        upgradeTap("tap_2", "Gravitationsgriff", "Zieht das Gestein dir entgegen, statt umgekehrt.", 5_000.0, 8_000.0),
        upgradeTap("tap_3", "Titanfingerkuppen", "Härter als alles, was du anfasst.", 100_000.0, 200_000.0),
        upgradeTap("tap_4", "Kinetischer Impulsgeber", "Jede Berührung schlägt ein wie ein kleiner Meteor.", 5_000_000.0, 9_000_000.0),
        upgradeTap("tap_5", "Tektonischer Druck", "Du tippst nicht mehr, du verschiebst Platten.", 200_000_000.0, 350_000_000.0),
        upgradeTap("tap_6", "Quantenfinger", "Berührt alle möglichen Stellen gleichzeitig.", 10_000_000_000.0, 18_000_000_000.0),
        upgradeTap("tap_7", "Nukleare Berührung", "Fusion auf Fingerdruck. Bitte nicht kratzen.", 500_000_000_000.0, 900_000_000_000.0),
        upgradeTap("tap_8", "Ereignishorizont-Griff", "Was du berührst, kommt nicht zurück.", 20_000_000_000_000.0, 35_000_000_000_000.0),
        upgradeTap("tap_9", "Raumzeitfalte", "Du faltest die Strecke weg, statt sie zurückzulegen.", 700_000_000_000_000.0, 1_200_000_000_000_000.0),
        upgradeTap("tap_10", "Hand der Singularität", "Ein Fingerzeig, und die Materie ordnet sich.", 12_000_000_000_000_000.0, 20_000_000_000_000_000.0),
    )

    private val synergyUpgrades: List<Upgrade> = listOf(
        Upgrade(
            id = "synergy_1",
            name = "Seismische Resonanz",
            flavor = "Dein Tippen bringt die ganze Anlage zum Mitschwingen.",
            cost = 50_000.0,
            effect = UpgradeEffect.TapFromProduction(0.01),
            unlock = UnlockCondition.CollectorsOwned("drone", 10),
            category = UpgradeCategory.TAP,
        ),
        Upgrade(
            id = "synergy_2",
            name = "Harmonischer Kollaps",
            flavor = "Ein Tipp im richtigen Takt und alles arbeitet doppelt.",
            cost = 10_000_000.0,
            effect = UpgradeEffect.TapFromProduction(0.04),
            unlock = UnlockCondition.CollectorsOwned("driver", 25),
            category = UpgradeCategory.TAP,
        ),
        Upgrade(
            id = "synergy_3",
            name = "Singularitätsecho",
            flavor = "Jeder Tipp hallt durch jede Maschine, die du besitzt.",
            cost = 50_000_000_000.0,
            effect = UpgradeEffect.TapFromProduction(0.10),
            unlock = atTier("Roter Zwerg"),
            category = UpgradeCategory.TAP,
        ),
        Upgrade(
            id = "synergy_4",
            name = "Gezeitenkraft",
            flavor = "Der Körper zerrt selbst an dem, was ihn abbaut. Du hältst nur noch dagegen.",
            cost = 1_500_000_000_000_000.0,
            effect = UpgradeEffect.TapFromProduction(0.22),
            unlock = atTier("Hyperriese"),
            category = UpgradeCategory.TAP,
        ),
    )

    private val cosmicUpgrades: List<Upgrade> = listOf(
        Upgrade(
            id = "cosmic_1",
            name = "Kosmische Ausrichtung",
            flavor = "Alle Bahnen in einer Reihe. Der Rest ist Logistik.",
            cost = 1_000_000.0,
            effect = UpgradeEffect.GlobalMultiplier(2.0),
            unlock = atTier("Venus"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "cosmic_2",
            name = "Dunkle-Materie-Verdichter",
            flavor = "Presst das Unsichtbare zu etwas, das man wiegen kann.",
            cost = 1_000_000_000.0,
            effect = UpgradeEffect.GlobalMultiplier(2.0),
            unlock = atTier("Saturn"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "cosmic_3",
            name = "Vakuumfluktuation",
            flavor = "Das Nichts ist erstaunlich ergiebig, wenn man es schüttelt.",
            cost = 1_000_000_000_000.0,
            effect = UpgradeEffect.GlobalMultiplier(2.0),
            unlock = atTier("Sonne"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "cosmic_4",
            name = "Entropieumkehr",
            flavor = "Du räumst auf, was das Universum seit 13 Milliarden Jahren verstreut.",
            cost = 100_000_000_000_000.0,
            effect = UpgradeEffect.GlobalMultiplier(3.0),
            unlock = atTier("Roter Überriese"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "cosmic_5",
            name = "Raumzeitgefälle",
            flavor = "Du legst das Universum leicht schräg und lässt den Rest herunterrollen.",
            cost = 4_000_000_000_000_000.0,
            effect = UpgradeEffect.GlobalMultiplier(3.0),
            unlock = atTier("Weißer Zwerg"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "cosmic_6",
            name = "Letzte Symmetrie",
            flavor = "Die eine Regel, aus der alle anderen folgen. Du hast sie umgestellt.",
            cost = 22_000_000_000_000_000.0,
            effect = UpgradeEffect.GlobalMultiplier(4.0),
            unlock = atTier("Magnetar"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "offline_1",
            name = "Autonome Drohnen",
            flavor = "Sie arbeiten auch weiter, wenn du das Handy weglegst.",
            cost = 500_000.0,
            effect = UpgradeEffect.OfflineEfficiency(1.0),
            unlock = atTier("Erde"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "offline_2",
            name = "Kryostase-Puffer",
            flavor = "Lagert die Ausbeute ein, bis du wiederkommst.",
            cost = 500_000_000.0,
            effect = UpgradeEffect.OfflineCapHours(24.0),
            unlock = atTier("Jupiter"),
            category = UpgradeCategory.COSMIC,
        ),
        Upgrade(
            id = "offline_3",
            name = "Trägheitsspeicher",
            flavor = "Was die Anlage nachts fördert, wartet jetzt zwei Tage auf dich.",
            cost = 8_000_000_000_000.0,
            effect = UpgradeEffect.OfflineCapHours(48.0),
            unlock = atTier("Blauer Riese"),
            category = UpgradeCategory.COSMIC,
        ),
    )

    private val collectorUpgrades: List<Upgrade> = Collectors.all.flatMap { collector ->
        COLLECTOR_UPGRADE_STEPS.map { (required, priceFactor, suffix) ->
            Upgrade(
                id = "${collector.id}_${required}",
                name = "${collector.name} $suffix",
                flavor = "Doppelte Leistung aus jedem ${collector.name}.",
                cost = collector.baseCost * priceFactor,
                effect = UpgradeEffect.CollectorMultiplier(collector.id, 2.0),
                unlock = UnlockCondition.CollectorsOwned(collector.id, required),
                category = UpgradeCategory.COLLECTOR,
            )
        }
    }

    val all: List<Upgrade> =
        (tapUpgrades + synergyUpgrades + cosmicUpgrades + collectorUpgrades).sortedBy { it.cost }

    private val index: Map<String, Upgrade> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Upgrade-ID im Katalog" }
    }

    fun byId(id: String): Upgrade? = index[id]

    private fun upgradeTap(
        id: String,
        name: String,
        flavor: String,
        cost: Double,
        unlockMass: Double,
    ) = Upgrade(
        id = id,
        name = name,
        flavor = flavor,
        cost = cost,
        effect = UpgradeEffect.TapMultiplier(2.0),
        unlock = UnlockCondition.MassCollected(unlockMass),
        category = UpgradeCategory.TAP,
    )
}
