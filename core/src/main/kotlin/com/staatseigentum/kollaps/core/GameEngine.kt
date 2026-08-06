package com.staatseigentum.kollaps.core

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/** Everything the UI needs to draw a frame, derived from a [GameState]. */
data class Stats(
    val tier: CelestialTier,
    val nextTier: CelestialTier?,
    /** Progress towards [nextTier] in `0f..1f`; `1f` once the black hole is reached. */
    val tierProgress: Float,
    val massPerSecond: Double,
    val massPerTap: Double,
    val globalMultiplier: Double,
    val singularityMultiplier: Double,
    val offlineEfficiency: Double,
    val offlineCapSeconds: Long,
    val pendingSingularities: Double,
    val canCollapse: Boolean,
)

/** A collector row in the shop. */
data class CollectorOffer(
    val collector: Collector,
    val owned: Int,
    val amount: Int,
    val cost: Double,
    /** Mass per second this collector currently contributes in total. */
    val output: Double,
    val affordable: Boolean,
    val everBought: Boolean,
    /** Hidden collectors are still too far away to be shown at all. */
    val visible: Boolean,
)

/** An upgrade row in the shop. */
data class UpgradeOffer(
    val upgrade: Upgrade,
    val affordable: Boolean,
)

/** Result of crediting time that passed while the app was closed. */
data class OfflineReport(
    val state: GameState,
    val seconds: Long,
    val gained: Double,
) {
    val worthShowing: Boolean get() = seconds >= 60 && gained > 0.0
}

/** How many collectors to buy at once. */
enum class BuyAmount(val label: String, val count: Int) {
    ONE("×1", 1),
    TEN("×10", 10),
    HUNDRED("×100", 100),
    MAX("Max", -1),
}

/**
 * All game rules. Pure functions over [GameState] — no Android, no coroutines, no clock of its
 * own — so the whole simulation can be unit tested and fast-forwarded.
 */
object GameEngine {

    /** Base mass a single tap yields before any multiplier. */
    const val BASE_TAP = 1.0

    /** Share of production credited while the app is closed, before upgrades. */
    const val BASE_OFFLINE_EFFICIENCY = 0.5

    /** Hours of absence credited at most, before upgrades. */
    const val BASE_OFFLINE_CAP_HOURS = 8.0

    /** Singularities awarded for collapsing exactly at the black hole threshold. */
    const val SINGULARITY_SCALE = 12.0

    /** Production bonus per singularity. */
    const val SINGULARITY_BONUS = 0.10

    /** A collector shows up in the shop once its price is within reach. */
    private const val VISIBILITY_FACTOR = 0.35

    // ---------------------------------------------------------------- derived state

    fun stats(state: GameState): Stats {
        val mods = modifiersOf(state)
        val tier = Tiers.forMass(state.runMass)
        val next = Tiers.next(tier)
        val perSecond = massPerSecond(state, mods, tier)

        return Stats(
            tier = tier,
            nextTier = next,
            tierProgress = tierProgress(state.runMass, tier, next),
            massPerSecond = perSecond,
            massPerTap = massPerTap(state, mods, tier, perSecond),
            globalMultiplier = mods.global,
            singularityMultiplier = singularityMultiplier(state),
            offlineEfficiency = mods.offlineEfficiency,
            offlineCapSeconds = (mods.offlineCapHours * 3_600.0).toLong(),
            pendingSingularities = pendingSingularities(state),
            canCollapse = canCollapse(state),
        )
    }

    fun massPerSecond(state: GameState): Double {
        val tier = Tiers.forMass(state.runMass)
        return massPerSecond(state, modifiersOf(state), tier)
    }

    fun massPerTap(state: GameState): Double {
        val mods = modifiersOf(state)
        val tier = Tiers.forMass(state.runMass)
        return massPerTap(state, mods, tier, massPerSecond(state, mods, tier))
    }

    fun tierOf(state: GameState): CelestialTier = Tiers.forMass(state.runMass)

    // ---------------------------------------------------------------- actions

    /** Advances the simulation by [seconds] of production. */
    fun tick(state: GameState, seconds: Double): GameState {
        if (seconds <= 0.0) return state
        val gained = massPerSecond(state) * seconds
        return credit(state, gained)
    }

    /** Taps the body once. */
    fun tap(state: GameState): GameState {
        val gained = massPerTap(state)
        return credit(state.copy(taps = state.taps + 1), gained)
    }

    /** How much a single tap would yield right now, for the floating number. */
    fun tapValue(state: GameState): Double = massPerTap(state)

    /** Buys [amount] copies of a collector, or nothing if they are not affordable. */
    fun buyCollector(state: GameState, collectorId: String, amount: BuyAmount): GameState {
        val collector = Collectors.byId(collectorId) ?: return state
        val owned = state.ownedOf(collectorId)
        val count = resolveAmount(collector, owned, state.mass, amount)
        if (count <= 0) return state

        val cost = collector.costForBulk(owned, count)
        if (cost > state.mass) return state

        return state.copy(
            mass = state.mass - cost,
            collectors = state.collectors + (collectorId to (owned + count)),
        )
    }

    /** Buys an upgrade if it is unlocked, unowned and affordable. */
    fun buyUpgrade(state: GameState, upgradeId: String): GameState {
        val upgrade = Upgrades.byId(upgradeId) ?: return state
        if (state.owns(upgradeId)) return state
        if (!isUnlocked(state, upgrade)) return state
        if (upgrade.cost > state.mass) return state

        return state.copy(
            mass = state.mass - upgrade.cost,
            upgrades = state.upgrades + upgradeId,
        )
    }

    /** Marks the current tier as celebrated so the animation only plays once. */
    fun acknowledgeTier(state: GameState): GameState =
        state.copy(celebratedTier = tierOf(state).index)

    /** True when the player reached a tier they have not seen the celebration for yet. */
    fun hasUncelebratedTier(state: GameState): Boolean =
        tierOf(state).index > state.celebratedTier

    // ---------------------------------------------------------------- prestige

    fun canCollapse(state: GameState): Boolean =
        state.runMass >= Tiers.last.threshold && pendingSingularities(state) >= 1.0

    /** Singularities the player would receive for collapsing right now. */
    fun pendingSingularities(state: GameState): Double {
        if (state.runMass < Tiers.last.threshold) return 0.0
        return floor(SINGULARITY_SCALE * sqrt(state.runMass / Tiers.last.threshold))
    }

    fun singularityMultiplier(state: GameState): Double =
        1.0 + SINGULARITY_BONUS * state.singularities

    /**
     * Collapses the black hole: the run resets to a meteorite, but the singularities earned
     * stay and speed up every future run.
     */
    fun collapse(state: GameState, nowMillis: Long): GameState {
        if (!canCollapse(state)) return state
        val earned = pendingSingularities(state)
        return GameState(
            singularities = state.singularities + earned,
            collapses = state.collapses + 1,
            taps = state.taps,
            totalMass = state.totalMass,
            bestTier = maxOf(state.bestTier, tierOf(state).index),
            bestRunMass = maxOf(state.bestRunMass, state.runMass),
            lastSeenAt = nowMillis,
            startedAt = if (state.startedAt == 0L) nowMillis else state.startedAt,
        )
    }

    // ---------------------------------------------------------------- offline

    /**
     * Credits production for the time between [GameState.lastSeenAt] and [nowMillis], capped by
     * the player's offline cap and scaled by their offline efficiency.
     */
    fun applyOffline(state: GameState, nowMillis: Long): OfflineReport {
        if (state.lastSeenAt <= 0L) {
            return OfflineReport(state.copy(lastSeenAt = nowMillis), 0, 0.0)
        }
        val elapsedSeconds = (nowMillis - state.lastSeenAt) / 1_000
        if (elapsedSeconds <= 0L) {
            // Clock went backwards, or we were only gone for a moment.
            return OfflineReport(state.copy(lastSeenAt = nowMillis), 0, 0.0)
        }

        val mods = modifiersOf(state)
        val capped = min(elapsedSeconds.toDouble(), mods.offlineCapHours * 3_600.0)
        val gained = massPerSecond(state) * capped * mods.offlineEfficiency
        val credited = credit(state, gained).copy(lastSeenAt = nowMillis)
        return OfflineReport(credited, capped.toLong(), gained)
    }

    fun touch(state: GameState, nowMillis: Long): GameState = state.copy(lastSeenAt = nowMillis)

    // ---------------------------------------------------------------- shop

    fun collectorOffers(state: GameState, amount: BuyAmount): List<CollectorOffer> {
        val mods = modifiersOf(state)
        val tier = Tiers.forMass(state.runMass)
        val scale = mods.global * tier.productionMultiplier * singularityMultiplier(state)

        return Collectors.all.mapIndexed { position, collector ->
            val owned = state.ownedOf(collector.id)
            val count = resolveAmount(collector, owned, state.mass, amount)
            val cost = collector.costForBulk(owned, count)
            // A collector appears once it is roughly within reach, or once one is owned. The
            // very first one is always visible so a fresh save has something to buy.
            val visible = position == 0 ||
                owned > 0 ||
                state.totalMass >= collector.baseCost * VISIBILITY_FACTOR

            CollectorOffer(
                collector = collector,
                owned = owned,
                amount = count,
                cost = cost,
                output = owned * collector.baseRate * mods.collectorFactor(collector.id) * scale,
                affordable = count > 0 && cost <= state.mass,
                everBought = owned > 0,
                visible = visible,
            )
        }
    }

    fun upgradeOffers(state: GameState): List<UpgradeOffer> =
        Upgrades.all
            .asSequence()
            .filter { !state.owns(it.id) && isUnlocked(state, it) }
            .map { UpgradeOffer(it, it.cost <= state.mass) }
            .sortedBy { it.upgrade.cost }
            .toList()

    fun ownedUpgrades(state: GameState): List<Upgrade> =
        Upgrades.all.filter { state.owns(it.id) }

    fun isUnlocked(state: GameState, upgrade: Upgrade): Boolean = when (val u = upgrade.unlock) {
        is UnlockCondition.Always -> true
        is UnlockCondition.CollectorsOwned -> state.ownedOf(u.collectorId) >= u.count
        is UnlockCondition.MassCollected -> state.runMass >= u.amount
        is UnlockCondition.TierReached -> tierOf(state).index >= u.tierIndex
        is UnlockCondition.TapsMade -> state.taps >= u.count
    }

    /** How many copies [amount] resolves to for this collector right now. */
    fun resolveAmount(collector: Collector, owned: Int, mass: Double, amount: BuyAmount): Int =
        if (amount == BuyAmount.MAX) {
            collector.affordableCount(owned, mass).coerceAtMost(MAX_BULK)
        } else {
            amount.count
        }

    // ---------------------------------------------------------------- internals

    private const val MAX_BULK = 1_000

    private fun credit(state: GameState, gained: Double): GameState {
        if (gained <= 0.0 || !gained.isFinite()) return state
        val runMass = state.runMass + gained
        return state.copy(
            mass = state.mass + gained,
            runMass = runMass,
            totalMass = state.totalMass + gained,
            bestRunMass = maxOf(state.bestRunMass, runMass),
            bestTier = maxOf(state.bestTier, Tiers.forMass(runMass).index),
        )
    }

    private fun massPerSecond(state: GameState, mods: Modifiers, tier: CelestialTier): Double {
        var base = 0.0
        for (collector in Collectors.all) {
            val owned = state.ownedOf(collector.id)
            if (owned > 0) base += owned * collector.baseRate * mods.collectorFactor(collector.id)
        }
        return base * mods.global * tier.productionMultiplier * singularityMultiplier(state)
    }

    private fun massPerTap(
        state: GameState,
        mods: Modifiers,
        tier: CelestialTier,
        perSecond: Double,
    ): Double {
        val base = (BASE_TAP + mods.tapFlat) *
            mods.tapMultiplier *
            mods.global *
            tier.productionMultiplier *
            singularityMultiplier(state)
        return base + perSecond * mods.tapFraction
    }

    private fun tierProgress(
        runMass: Double,
        tier: CelestialTier,
        next: CelestialTier?,
    ): Float {
        if (next == null) return 1f
        val span = next.threshold - tier.threshold
        if (span <= 0.0) return 1f
        return (((runMass - tier.threshold) / span).coerceIn(0.0, 1.0)).toFloat()
    }

    private fun modifiersOf(state: GameState): Modifiers {
        val mods = Modifiers()
        for (id in state.upgrades) {
            val upgrade = Upgrades.byId(id) ?: continue
            when (val effect = upgrade.effect) {
                is UpgradeEffect.TapFlat -> mods.tapFlat += effect.amount
                is UpgradeEffect.TapMultiplier -> mods.tapMultiplier *= effect.factor
                is UpgradeEffect.GlobalMultiplier -> mods.global *= effect.factor
                is UpgradeEffect.TapFromProduction -> mods.tapFraction += effect.fraction
                is UpgradeEffect.CollectorMultiplier -> mods.collectors.merge(
                    effect.collectorId,
                    effect.factor,
                ) { a, b -> a * b }

                is UpgradeEffect.OfflineEfficiency ->
                    mods.offlineEfficiency = maxOf(mods.offlineEfficiency, effect.fraction)

                is UpgradeEffect.OfflineCapHours ->
                    mods.offlineCapHours = maxOf(mods.offlineCapHours, effect.hours)
            }
        }
        return mods
    }

    private class Modifiers {
        var tapFlat = 0.0
        var tapMultiplier = 1.0
        var global = 1.0
        var tapFraction = 0.0
        var offlineEfficiency = BASE_OFFLINE_EFFICIENCY
        var offlineCapHours = BASE_OFFLINE_CAP_HOURS
        val collectors = HashMap<String, Double>()

        fun collectorFactor(id: String): Double = collectors[id] ?: 1.0
    }
}

/** Convenience for tests and simulations: run [seconds] of production in one step. */
fun GameState.advanced(seconds: Double): GameState = GameEngine.tick(this, seconds)
