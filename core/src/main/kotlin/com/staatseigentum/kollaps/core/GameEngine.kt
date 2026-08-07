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
    /** The buff running right now, if any, and how long it has left. */
    val buff: Buff? = null,
    val buffSecondsLeft: Double = 0.0,
    /** What the earned achievements are worth together. */
    val achievementMultiplier: Double = 1.0,
    /** The challenge being run, if any, and whether it can be handed in or is already lost. */
    val challenge: Challenge? = null,
    val challengeMet: Boolean = false,
    val challengeLost: Boolean = false,
    /** Automatic taps per second, from prestige. Zero until one is bought. */
    val autoTapsPerSecond: Double = 0.0,
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
    /** Milestones passed, and the count the next one lands on. */
    val milestones: Int = 0,
    val nextMilestoneAt: Int? = null,
)

/** An upgrade row in the shop. */
data class UpgradeOffer(
    val upgrade: Upgrade,
    val affordable: Boolean,
)

/** Result of crediting time that passed while the app was closed. */
data class OfflineReport(
    val state: GameState,
    /** Seconds actually credited, after the cap. */
    val seconds: Long,
    val gained: Double,
    /** Seconds the player was away, before the cap. */
    val awaySeconds: Long = 0,
    /** Share of production credited, in `0f..1f`. */
    val efficiency: Double = 1.0,
    /** What each collector contributed, biggest first. Empty when nothing was earned. */
    val shares: List<CollectorShare> = emptyList(),
) {
    val worthShowing: Boolean get() = seconds >= 60 && gained > 0.0

    /** True when the absence ran past the offline cap and the rest went uncredited. */
    val cappedOut: Boolean get() = awaySeconds > seconds

    /** What a longer cap would have been worth, at the same rate. */
    val lostToCap: Double
        get() = if (!cappedOut || seconds <= 0) 0.0 else gained / seconds * (awaySeconds - seconds)
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
            buff = state.buff,
            buffSecondsLeft = state.buffSecondsLeft,
            achievementMultiplier = Achievements.multiplier(state),
            challenge = state.challenge,
            challengeMet = Challenge.isMet(state),
            challengeLost = Challenge.isLost(state),
            autoTapsPerSecond = mods.autoTapsPerSecond,
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

    /**
     * Advances the simulation by [seconds] of production.
     *
     * Also the only place the clock moves: play time, the running buff and any achievement that
     * has just come true are all settled here, so nothing else has to remember to do it.
     */
    fun tick(state: GameState, seconds: Double): GameState {
        if (seconds <= 0.0) return state
        val gained = massPerSecond(state) * seconds
        var ticked = credit(state, gained).copy(playedSeconds = state.playedSeconds + seconds)
        ticked = autoTap(ticked, seconds)
        if (ticked.activeChallenge != null) {
            ticked = ticked.copy(challengeSeconds = ticked.challengeSeconds + seconds)
        }
        return award(expireBuff(ticked, seconds))
    }

    /**
     * Credits the automatic taps owed for [seconds].
     *
     * The mass is credited in full even for a fraction of a tap, because production is a
     * continuous number anyway; only the *counter* has to wait for whole taps, and the remainder
     * is carried so it is never lost. Automatic taps count towards achievements exactly like a
     * finger, which is the point of buying one.
     */
    private fun autoTap(state: GameState, seconds: Double): GameState {
        val perSecond = modifiersOf(state).autoTapsPerSecond
        if (perSecond <= 0.0) return state

        val taps = perSecond * seconds
        val carried = state.autoTapCarry + taps
        val whole = floor(carried)
        return credit(state, massPerTap(state) * taps).copy(
            taps = state.taps + whole.toLong(),
            autoTapCarry = carried - whole,
        )
    }

    /** Counts the running buff down. Only the tick does this, so a closed app does not burn it. */
    private fun expireBuff(state: GameState, seconds: Double): GameState {
        if (state.buffSecondsLeft <= 0.0) return state
        val left = state.buffSecondsLeft - seconds
        return if (left > 0.0) state.copy(buffSecondsLeft = left)
        else state.copy(buffId = null, buffSecondsLeft = 0.0)
    }

    /** Records anything newly earned. Cheap enough to ask on every tick. */
    fun award(state: GameState): GameState {
        val earned = Achievements.newlyEarned(state)
        return if (earned.isEmpty()) state
        else state.copy(achievements = state.achievements + earned)
    }

    /** Taps the body once. */
    fun tap(state: GameState): GameState {
        val gained = massPerTap(state)
        return award(credit(state.copy(taps = state.taps + 1), gained))
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
        state.activeChallenge == null &&
            state.runMass >= Tiers.last.threshold &&
            pendingSingularities(state) >= 1.0

    /** Singularities the player would receive for collapsing right now. */
    fun pendingSingularities(state: GameState): Double {
        if (state.runMass < Tiers.last.threshold) return 0.0
        val base = SINGULARITY_SCALE * sqrt(state.runMass / Tiers.last.threshold)
        return floor(base * modifiersOf(state).singularityGain)
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
        return award(
            GameState(
                mass = startingMass(state),
                collectors = startingCollectors(state),
                singularities = state.singularities + earned,
                collapses = state.collapses + 1,
                taps = state.taps,
                totalMass = state.totalMass,
                bestTier = maxOf(state.bestTier, tierOf(state).index),
                bestRunMass = maxOf(state.bestRunMass, state.runMass),
                lastSeenAt = nowMillis,
                startedAt = if (state.startedAt == 0L) nowMillis else state.startedAt,
                // Everything below is the point of collapsing: it is what carries over.
                prestigeUpgrades = state.prestigeUpgrades,
                achievements = state.achievements,
                playedSeconds = state.playedSeconds,
                cometsCaught = state.cometsCaught,
                soundOn = state.soundOn,
                hapticsOn = state.hapticsOn,
                challengesDone = state.challengesDone,
            ),
        )
    }

    // ---------------------------------------------------------------- challenges

    /**
     * Begins a challenge: the run starts over under a rule, and the prestige head start is
     * withheld.
     *
     * Keeping the starting mass and the prefabricated fleet would defeat every challenge whose
     * point is doing without one of them, and would make the timed one a formality. The permanent
     * multipliers do carry over — a challenge unlocked three collapses in has to be winnable by
     * the player who unlocked it.
     */
    fun startChallenge(state: GameState, challengeId: String, nowMillis: Long): GameState {
        val challenge = Challenge.byId(challengeId) ?: return state
        if (state.activeChallenge != null) return state
        if (challenge.id in state.challengesDone) return state
        if (state.collapses < challenge.requiredCollapses) return state

        return freshRun(state, nowMillis).copy(
            activeChallenge = challenge.id,
            challengeSeconds = 0.0,
        )
    }

    /** Gives a challenge up. The run resets, the challenge stays unfinished. */
    fun abortChallenge(state: GameState, nowMillis: Long): GameState {
        if (state.activeChallenge == null) return state
        return freshRun(state, nowMillis)
    }

    /** Hands a met challenge in, which records the reward and starts an ordinary run. */
    fun finishChallenge(state: GameState, nowMillis: Long): GameState {
        val challenge = state.challenge ?: return state
        if (!Challenge.isMet(state)) return state
        return award(
            freshRun(state, nowMillis).copy(
                challengesDone = state.challengesDone + challenge.id,
            ),
        )
    }

    /**
     * The state with the run wiped but everything permanent kept.
     *
     * Unlike [collapse] this pays nothing and counts nothing: it is what a challenge begins and
     * ends with, so starting one is never a way to farm singularities.
     */
    private fun freshRun(state: GameState, nowMillis: Long): GameState = GameState(
        singularities = state.singularities,
        collapses = state.collapses,
        taps = state.taps,
        totalMass = state.totalMass,
        bestTier = maxOf(state.bestTier, tierOf(state).index),
        bestRunMass = maxOf(state.bestRunMass, state.runMass),
        lastSeenAt = nowMillis,
        startedAt = if (state.startedAt == 0L) nowMillis else state.startedAt,
        prestigeUpgrades = state.prestigeUpgrades,
        achievements = state.achievements,
        playedSeconds = state.playedSeconds,
        cometsCaught = state.cometsCaught,
        soundOn = state.soundOn,
        hapticsOn = state.hapticsOn,
        challengesDone = state.challengesDone,
    )

    /** Mass a fresh run begins with, from prestige. */
    private fun startingMass(state: GameState): Double =
        state.prestigeUpgrades.sumOf {
            (PrestigeUpgrades.byId(it)?.effect as? PrestigeEffect.StartingMass)?.mass ?: 0.0
        }

    /** Collectors a fresh run begins with, from prestige. The best upgrade wins, they do not add. */
    private fun startingCollectors(state: GameState): Map<String, Int> {
        val count = state.prestigeUpgrades.maxOfOrNull {
            (PrestigeUpgrades.byId(it)?.effect as? PrestigeEffect.StartingCollectors)?.count ?: 0
        } ?: 0
        if (count <= 0) return emptyMap()
        return Collectors.all.associate { it.id to count }
    }

    /** Buys a prestige upgrade if it is offered and the singularities are there. */
    fun buyPrestigeUpgrade(state: GameState, upgradeId: String): GameState {
        val upgrade = PrestigeUpgrades.byId(upgradeId) ?: return state
        if (state.ownsPrestige(upgradeId)) return state
        if (state.collapses < upgrade.requiredCollapses) return state
        if (upgrade.cost > state.singularities) return state

        return award(
            state.copy(
                singularities = state.singularities - upgrade.cost,
                prestigeUpgrades = state.prestigeUpgrades + upgradeId,
            ),
        )
    }

    // ---------------------------------------------------------------- comets

    /** How much more often comets should come, after prestige. */
    fun cometFrequency(state: GameState): Double = modifiersOf(state).cometFrequency

    /**
     * Catches a comet and pays out whatever it was carrying.
     *
     * A windfall is worth a fixed span of the player's *current* production, so it stays
     * meaningful at every tier instead of turning into a rounding error by the third hour.
     */
    fun catchComet(state: GameState, comet: Comet): GameState {
        val caught = state.copy(cometsCaught = state.cometsCaught + 1)
        return award(
            when (val reward = comet.reward) {
                is CometReward.Windfall ->
                    credit(caught, massPerSecond(caught) * reward.secondsOfProduction)

                is CometReward.Timed -> caught.copy(
                    buffId = reward.buff.id,
                    // A second catch restarts the buff rather than stacking it, which keeps the
                    // ceiling somewhere a player can reason about.
                    buffSecondsLeft = reward.buff.seconds,
                )
            },
        )
    }

    // ---------------------------------------------------------------- settings

    fun setSound(state: GameState, on: Boolean): GameState = state.copy(soundOn = on)

    fun setHaptics(state: GameState, on: Boolean): GameState = state.copy(hapticsOn = on)

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
        return OfflineReport(
            state = credited,
            seconds = capped.toLong(),
            gained = gained,
            awaySeconds = elapsedSeconds,
            efficiency = mods.offlineEfficiency,
            // The same breakdown the statistics tab shows, so the player can see which machine
            // actually worked the night shift instead of only that some of them did.
            shares = if (gained > 0.0) Statistics.shares(state) else emptyList(),
        )
    }

    fun touch(state: GameState, nowMillis: Long): GameState = state.copy(lastSeenAt = nowMillis)

    // ---------------------------------------------------------------- shop

    /** What one collector contributes to the total production right now. */
    fun collectorOutput(state: GameState, collector: Collector): Double {
        val owned = state.ownedOf(collector.id)
        if (owned <= 0) return 0.0
        val mods = modifiersOf(state)
        val tier = Tiers.forMass(state.runMass)
        if (!mods.collectorsWork) return 0.0
        return owned * collector.baseRate * mods.collectorFactor(collector.id) *
            Milestones.factor(owned) *
            mods.global * tier.productionMultiplier * singularityMultiplier(state)
    }

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
                output = if (mods.collectorsWork) {
                    owned * collector.baseRate * mods.collectorFactor(collector.id) *
                        Milestones.factor(owned) * scale
                } else {
                    0.0
                },
                milestones = Milestones.reached(owned),
                nextMilestoneAt = Milestones.nextAt(owned),
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
        if (!mods.collectorsWork) return 0.0
        var base = 0.0
        for (collector in Collectors.all) {
            val owned = state.ownedOf(collector.id)
            if (owned > 0) {
                base += owned * collector.baseRate *
                    mods.collectorFactor(collector.id) *
                    Milestones.factor(owned)
            }
        }
        return base * mods.global * tier.productionMultiplier * singularityMultiplier(state)
    }

    private fun massPerTap(
        state: GameState,
        mods: Modifiers,
        tier: CelestialTier,
        perSecond: Double,
    ): Double {
        if (!mods.tapsWork) return 0.0
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

        // Prestige first: it sets the floor the run-local upgrades then build on.
        for (id in state.prestigeUpgrades) {
            apply(mods, PrestigeUpgrades.byId(id)?.effect)
        }

        // Challenge rewards are permanent in the same way prestige is, and are the same kind of
        // thing, so they go through the same switch rather than growing a parallel one.
        for (id in state.challengesDone) {
            apply(mods, Challenge.byId(id)?.reward)
        }

        // The running challenge, which is the only modifier that takes something away.
        when (val rule = state.challenge?.rule) {
            is ChallengeRule.NoCollectors -> mods.collectorsWork = false
            is ChallengeRule.NoTaps -> mods.tapsWork = false
            is ChallengeRule.Handicap -> mods.global *= rule.factor
            null -> Unit
        }

        // Every achievement is worth a little, which is what stops them being decoration.
        mods.global *= Achievements.multiplier(state)

        // A buff is the only modifier with a clock on it.
        when (state.buff) {
            Buff.SURGE -> mods.global *= Buff.SURGE.factor
            Buff.FRENZY -> mods.tapMultiplier *= Buff.FRENZY.factor
            null -> Unit
        }

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

                is UpgradeEffect.CollectorSynergy -> mods.collectors.merge(
                    effect.targetId,
                    1.0 + effect.perUnit * state.ownedOf(effect.sourceId),
                ) { a, b -> a * b }

                is UpgradeEffect.FleetSynergy -> {
                    val factor = 1.0 + effect.perUnit * state.ownedOf(effect.sourceId)
                    for (collector in Collectors.all) {
                        mods.collectors.merge(collector.id, factor) { a, b -> a * b }
                    }
                }

                is UpgradeEffect.OfflineEfficiency ->
                    mods.offlineEfficiency = maxOf(mods.offlineEfficiency, effect.fraction)

                is UpgradeEffect.OfflineCapHours ->
                    mods.offlineCapHours = maxOf(mods.offlineCapHours, effect.hours)
            }
        }
        return mods
    }

    /** Folds one permanent effect in. Shared by prestige upgrades and challenge rewards. */
    private fun apply(mods: Modifiers, effect: PrestigeEffect?) {
        when (effect) {
            is PrestigeEffect.OfflineEfficiency ->
                mods.offlineEfficiency = maxOf(mods.offlineEfficiency, effect.fraction)

            is PrestigeEffect.OfflineCapHours ->
                mods.offlineCapHours = maxOf(mods.offlineCapHours, effect.hours)

            is PrestigeEffect.GlobalMultiplier -> mods.global *= effect.factor
            is PrestigeEffect.TapMultiplier -> mods.tapMultiplier *= effect.factor
            is PrestigeEffect.CometFrequency -> mods.cometFrequency *= effect.factor
            is PrestigeEffect.SingularityGain -> mods.singularityGain *= effect.factor
            is PrestigeEffect.AutoTap ->
                mods.autoTapsPerSecond = maxOf(mods.autoTapsPerSecond, effect.perSecond)

            is PrestigeEffect.StartingCollectors -> Unit // only read when a run begins
            is PrestigeEffect.StartingMass -> Unit // only read when a run begins
            null -> Unit
        }
    }

    private class Modifiers {
        var tapFlat = 0.0
        var tapMultiplier = 1.0
        var global = 1.0
        var tapFraction = 0.0
        var offlineEfficiency = BASE_OFFLINE_EFFICIENCY
        var offlineCapHours = BASE_OFFLINE_CAP_HOURS
        var cometFrequency = 1.0
        var singularityGain = 1.0
        var autoTapsPerSecond = 0.0

        /** A challenge can switch off a whole source of mass. */
        var collectorsWork = true
        var tapsWork = true

        val collectors = HashMap<String, Double>()

        fun collectorFactor(id: String): Double = collectors[id] ?: 1.0
    }
}

/** Convenience for tests and simulations: run [seconds] of production in one step. */
fun GameState.advanced(seconds: Double): GameState = GameEngine.tick(this, seconds)
