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
    /** Whether the automatic buyer is available at all. */
    val autoBuyUnlocked: Boolean = false,
    /** The second reset: whether it exists yet, and what it would pay. */
    val bigBangUnlocked: Boolean = false,
    val pendingAeons: Double = 0.0,
    val canBigBang: Boolean = false,
    /** Whether the body can hold satellites yet, and what the ones up there are worth. */
    val orbitsUnlocked: Boolean = false,
    val orbitMultiplier: Double = 1.0,
    /** Whether the body is hot enough to fuse, and what the elements are worth together. */
    val fusionUnlocked: Boolean = false,
    val fusionMultiplier: Double = 1.0,
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

/** A fusion machine row. */
data class FusionOffer(
    val stage: FusionStage,
    val level: Int,
    val amount: Int,
    val cost: Double,
    val affordable: Boolean,
    /** Units of [FusionStage.output] this stage actually makes per second right now. */
    val outputPerSecond: Double,
    /** True when the stage has levels but not enough input to use them. */
    val starving: Boolean,
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

    /** How many times the price the automatic buyer wants in hand before it spends. */
    const val AUTO_BUY_RESERVE = 4.0

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
            autoBuyUnlocked = mods.autoBuy,
            bigBangUnlocked = BigBang.isUnlocked(state),
            pendingAeons = BigBang.pending(state),
            canBigBang = BigBang.canBang(state),
            orbitsUnlocked = Orbits.isUnlocked(state),
            orbitMultiplier = Orbits.multiplier(state),
            fusionUnlocked = Fusion.isUnlocked(state),
            // Only the two production levers, because that is the number the header claims to be.
            // Offline yield and comet frequency are worth having and are shown where they apply.
            fusionMultiplier = Fusion.factorFor(state, FusionBonus.GLOBAL),
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
        ticked = Fusion.advance(ticked, seconds)
        // Fed and eroded by what the body itself makes, so the system scales with the run rather
        // than mattering enormously the hour it unlocks and never again.
        ticked = Orbits.advance(ticked, seconds, gained / seconds)
        ticked = automate(ticked)
        ticked = advanceEvents(ticked, seconds)
        ticked = sample(ticked, seconds)
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

    /**
     * Takes a production sample every so often, for the curve in the statistics.
     *
     * Sampled on play time rather than wall clock, so the line shows sessions rather than a flat
     * stretch for every night the phone was in a drawer.
     */
    private fun sample(state: GameState, seconds: Double): GameState {
        val elapsed = state.historySeconds + seconds
        if (elapsed < History.SAMPLE_SECONDS) return state.copy(historySeconds = elapsed)
        return state.copy(
            history = History.append(state.history, massPerSecond(state)),
            // Modulo rather than zero, so a single long tick does not lose the remainder.
            historySeconds = elapsed % History.SAMPLE_SECONDS,
        )
    }

    /**
     * Moves the event clock, and puts one on the table when it runs out.
     *
     * Nothing happens while an event is already waiting: a queue of unanswered questions would
     * turn a decision into paperwork.
     */
    private fun advanceEvents(state: GameState, seconds: Double): GameState {
        if (!CosmicEvent.appearsAt(state)) return state
        if (state.pendingEvent != null) return state

        // A fresh save has no schedule yet; the first one is a whole interval away.
        if (state.nextEventSeconds <= 0.0) {
            return state.copy(nextEventSeconds = eventInterval(state))
        }

        val left = state.nextEventSeconds - seconds
        if (left > 0.0) return state.copy(nextEventSeconds = left)

        return state.copy(
            pendingEvent = CosmicEvent.pick(state).id,
            nextEventSeconds = eventInterval(state),
        )
    }

    /** Spread across the window, derived from the save so it needs no random source. */
    private fun eventInterval(state: GameState): Double {
        val span = CosmicEvent.MAX_SECONDS - CosmicEvent.MIN_SECONDS
        val spread = (state.playedSeconds.toLong() * 37 + state.taps * 11).mod(1_000L) / 1_000.0
        return CosmicEvent.MIN_SECONDS + span * spread
    }

    /**
     * Answers the waiting event and pays out whatever that option was worth.
     *
     * Same two shapes a comet pays in, and deliberately so: a windfall is mass in hand, a buff is
     * mass you have to be present to use, and the whole point of the question is picking between
     * those two.
     */
    fun chooseEvent(state: GameState, optionIndex: Int): GameState {
        val event = state.event ?: return state
        val option = event.optionAt(optionIndex) ?: return state

        val answered = state.copy(
            pendingEvent = null,
            eventsAnswered = state.eventsAnswered + 1,
        )
        return award(
            when (val reward = option.reward) {
                is CometReward.Windfall ->
                    credit(answered, massPerSecond(answered) * reward.secondsOfProduction)

                is CometReward.Timed -> answered.copy(
                    buffId = reward.buff.id,
                    buffSecondsLeft = reward.buff.seconds,
                )
            },
        )
    }

    /** Turns the event down. Nothing is paid, and the clock simply starts again. */
    fun dismissEvent(state: GameState): GameState =
        if (state.pendingEvent == null) state else state.copy(pendingEvent = null)

    /**
     * Carries out the rules that only need to know how much mass is in hand.
     *
     * One purchase per rule per tick rather than a loop, so no rule can empty a shop inside a
     * single frame — and so the three of them stay in a fixed order the player can predict.
     */
    private fun automate(state: GameState): GameState {
        if (!Automation.isUnlocked(state)) return state
        var next = autoCollectors(state)
        next = autoUpgrades(next)
        next = autoFusion(next)
        return next
    }

    /**
     * Buys one collector, if the rule is on and the mass is comfortably there.
     *
     * The reserve is the whole design: an automatic buyer that spends down to the last kilogram
     * is a machine that stops you ever affording an upgrade, and upgrades are worth far more than
     * one more copy of anything. Requiring several times the price means it buys out of surplus
     * and quietly stops as prices climb — exactly when the player wants to be saving.
     */
    private fun autoCollectors(state: GameState): GameState {
        if (!Automation.isAvailable(state, AutomationRule.COLLECTORS)) return state
        val reserve = Automation.valueOf(state, AutomationRule.COLLECTORS) ?: return state

        val best = collectorOffers(state, BuyAmount.ONE)
            .filter { it.visible && it.amount > 0 && it.cost * reserve <= state.mass }
            .minByOrNull { it.cost / it.collector.baseRate }
            ?: return state

        return buyCollector(state, best.collector.id, BuyAmount.ONE)
    }

    /**
     * Buys the cheapest upgrade that is small enough against the pile.
     *
     * A share rather than a reserve, because an upgrade is bought once and then owned forever:
     * what matters is not keeping a multiple of the price in hand afterwards but not spending the
     * afternoon's savings on something the player was about to outgrow anyway.
     */
    private fun autoUpgrades(state: GameState): GameState {
        if (!Automation.isAvailable(state, AutomationRule.UPGRADES)) return state
        val share = Automation.valueOf(state, AutomationRule.UPGRADES) ?: return state

        val next = upgradeOffers(state)
            .firstOrNull { it.affordable && it.upgrade.cost <= state.mass * share }
            ?: return state

        return buyUpgrade(state, next.upgrade.id)
    }

    /** Adds a level to the cheapest fusion stage, so no furnace stays starved for long. */
    private fun autoFusion(state: GameState): GameState {
        if (!Automation.isAvailable(state, AutomationRule.FUSION)) return state
        val reserve = Automation.valueOf(state, AutomationRule.FUSION) ?: return state

        val next = fusionOffers(state, BuyAmount.ONE)
            .filter { it.amount > 0 && it.cost * reserve <= state.mass }
            .minByOrNull { it.cost }
            ?: return state

        return buyFuser(state, next.stage.id, BuyAmount.ONE)
    }

    /**
     * Everything that has to know what time it actually is: the lab, and the two rules that act
     * on it.
     *
     * Kept apart from [tick] for the same reason [settleResearch] is — the tick counts elapsed
     * play time and has no business holding a wall clock. The caller has one; it passes it here.
     */
    fun onWallClock(state: GameState, nowMillis: Long): GameState {
        var next = settleResearch(state, nowMillis)
        next = autoResearch(next, nowMillis)
        next = autoCollapse(next, nowMillis)
        return next
    }

    /** Puts something on the bench whenever it is free. */
    private fun autoResearch(state: GameState, nowMillis: Long): GameState {
        if (!Automation.isAvailable(state, AutomationRule.RESEARCH)) return state
        val mode = Automation.valueOf(state, AutomationRule.RESEARCH) ?: return state
        if (state.activeResearch != null) return state

        val affordable = ResearchTree.offered(state)
            .filter { !ResearchTree.isDone(state, it) && it.cost <= state.mass }
        val pick = if (mode >= 1.0) affordable.maxByOrNull { it.cost } else affordable.minByOrNull { it.cost }

        return startResearch(state, (pick ?: return state).id, nowMillis)
    }

    /**
     * Collapses once the payout is worth the reset.
     *
     * The threshold is the point of the rule. Collapsing the moment it becomes possible is almost
     * always wrong — the singularities scale with the square root of the mass overshoot, so
     * waiting is worth real money — and a rule with no floor would rob the player of that every
     * single run.
     */
    private fun autoCollapse(state: GameState, nowMillis: Long): GameState {
        if (!Automation.isAvailable(state, AutomationRule.COLLAPSE)) return state
        val floor = Automation.valueOf(state, AutomationRule.COLLAPSE) ?: return state
        if (!canCollapse(state)) return state
        if (pendingSingularities(state) < floor) return state

        return collapse(state, nowMillis)
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
        // Under the rule that shuts the shop, buying has to be refused rather than merely made
        // pointless: an upgrade that takes the mass and then does nothing is a bug, not a rule.
        if (!modifiersOf(state).upgradesWork) return state
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
        singularityMultiplier(state, modifiersOf(state))

    private fun singularityMultiplier(state: GameState, mods: Modifiers): Double =
        1.0 + mods.singularityBonus * state.singularities

    /**
     * Collapses the black hole: the run resets to a meteorite, but the singularities earned
     * stay and speed up every future run.
     */
    fun collapse(state: GameState, nowMillis: Long): GameState {
        if (!canCollapse(state)) return state
        val earned = pendingSingularities(state)
        // Neutron capture: the iron in the core soaks up what the collapse throws at it, and
        // what falls out is the only thing in the game that survives every reset there is.
        val forged = Heavy.forge(state.heavy, Fusion.amountOf(state, Element.EISEN))
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
                heavy = forged,
                prestigeUpgrades = state.prestigeUpgrades,
                investments = state.investments,
                achievements = state.achievements,
                playedSeconds = state.playedSeconds,
                cometsCaught = state.cometsCaught,
                soundOn = state.soundOn,
                hapticsOn = state.hapticsOn,
                musicOn = state.musicOn,
                autoBuyOn = state.autoBuyOn,
                automation = state.automation,
                remindersOn = state.remindersOn,
                eventsAnswered = state.eventsAnswered,
                challengesDone = state.challengesDone,
                aeons = state.aeons,
                aeonUpgrades = state.aeonUpgrades,
                bigBangs = state.bigBangs,
                path = state.path,
                // Research is paid for in wall clock, which no reset can hand back.
                research = state.research,
                activeResearch = state.activeResearch,
                researchDoneAt = state.researchDoneAt,
            ),
        )
    }

    // ---------------------------------------------------------------- big bang

    /**
     * Throws the whole universe away: singularities, prestige upgrades, collapses, the run.
     *
     * What survives is what the player *is* rather than what they own — achievements, challenges
     * beaten, lifetime totals — plus the Äonen this pays out. Refusing while a challenge is
     * running is the same rule the collapse follows: one reset at a time.
     */
    fun bigBang(state: GameState, nowMillis: Long, pathId: String? = null): GameState {
        if (!BigBang.canBang(state)) return state
        val earned = BigBang.pending(state)
        // An unknown id keeps the universe unaligned rather than refusing the press. Losing a
        // ten-collapse reset to a typo in a save file is not a trade worth making.
        val chosen = Path.byId(pathId)?.id

        return award(
            GameState(
                lastSeenAt = nowMillis,
                startedAt = if (state.startedAt == 0L) nowMillis else state.startedAt,
                // Kept: everything that is a record rather than a possession.
                taps = state.taps,
                totalMass = state.totalMass,
                bestTier = maxOf(state.bestTier, tierOf(state).index),
                bestRunMass = maxOf(state.bestRunMass, state.runMass),
                achievements = state.achievements,
                challengesDone = state.challengesDone,
                heavy = state.heavy,
                playedSeconds = state.playedSeconds,
                cometsCaught = state.cometsCaught,
                soundOn = state.soundOn,
                hapticsOn = state.hapticsOn,
                musicOn = state.musicOn,
                autoBuyOn = state.autoBuyOn,
                automation = state.automation,
                remindersOn = state.remindersOn,
                eventsAnswered = state.eventsAnswered,
                // The point of pressing it.
                aeons = state.aeons + earned,
                aeonUpgrades = state.aeonUpgrades,
                bigBangs = state.bigBangs + 1,
                path = chosen,
                research = state.research,
                activeResearch = state.activeResearch,
                researchDoneAt = state.researchDoneAt,
            ),
        )
    }

    /** Buys an Äonen upgrade if it is unbought and affordable. */
    fun buyAeonUpgrade(state: GameState, upgradeId: String): GameState {
        val upgrade = AeonUpgrades.byId(upgradeId) ?: return state
        if (state.ownsAeon(upgradeId)) return state
        if (upgrade.cost > state.aeons) return state

        return award(
            state.copy(
                aeons = state.aeons - upgrade.cost,
                aeonUpgrades = state.aeonUpgrades + upgradeId,
            ),
        )
    }

    // ---------------------------------------------------------------- the system

    /** Opens the next orbit slot, if the body is big enough and the mass is there. */
    fun openOrbit(state: GameState): GameState {
        if (!Orbits.isUnlocked(state)) return state
        if (!modifiersOf(state).orbitsWork) return state
        val next = Orbits.next(state) ?: return state
        if (next.cost > state.mass) return state

        return award(state.copy(mass = state.mass - next.cost, orbits = state.orbits + 1))
    }

    /**
     * Puts a body on an open, empty slot.
     *
     * Seeded with a minute of the run's own production rather than a fixed lump. A fixed seed
     * would be everything at the moment the first slot opens and a rounding error two collapses
     * later, and either way the number would be about the catalogue rather than about the run.
     *
     * The floor of one kilogram only exists so that a body placed by somebody with no collectors
     * at all is still a body. The feed brings it up to a minute of production inside twenty
     * seconds anyway.
     */
    fun seedSatellite(state: GameState, orbitIndex: Int): GameState {
        val orbit = Orbits.at(orbitIndex) ?: return state
        if (!modifiersOf(state).orbitsWork) return state
        if (orbitIndex >= state.orbits) return state
        if (Orbits.isOccupied(state, orbit)) return state
        if (orbit.seedCost > state.mass) return state

        val seed = (massPerSecond(state) * SEED_SECONDS).coerceAtLeast(1.0)
        return award(
            state.copy(
                mass = state.mass - orbit.seedCost,
                satellites = state.satellites + (orbitIndex to seed),
            ),
        )
    }

    /**
     * Drops one body onto another. The inner slot keeps the pair, the outer one comes free.
     *
     * Inwards because that is the direction things fall, and because it hands the player a way to
     * move mass from a stable outer orbit to a productive inner one — which is the only reason to
     * ever build on the outside when the inside pays better.
     */
    fun mergeSatellites(state: GameState, first: Int, second: Int): GameState {
        if (first == second) return state
        val inner = Orbits.at(minOf(first, second)) ?: return state
        val outer = Orbits.at(maxOf(first, second)) ?: return state
        if (!Orbits.isOccupied(state, inner) || !Orbits.isOccupied(state, outer)) return state

        val combined =
            (Orbits.massOn(state, inner) + Orbits.massOn(state, outer)) * Orbits.MERGE_BONUS
        return award(
            state.copy(
                satellites = state.satellites - outer.index + (inner.index to combined),
            ),
        )
    }

    /** Seconds of production a freshly placed body starts with. */
    private const val SEED_SECONDS = 60.0

    // ---------------------------------------------------------------- research

    /** How much faster this player's lab works than the catalogue's stated times. */
    fun researchSpeed(state: GameState): Double = modifiersOf(state).researchSpeed

    /** How much faster this player's furnaces run than their stated rates. */
    fun fusionRate(state: GameState): Double = modifiersOf(state).fusionRate

    /**
     * Starts a project: the mass is taken now, the result arrives on the wall clock.
     *
     * One at a time, deliberately. A lab that can run everything at once is a shopping list; a lab
     * with one bench is a decision about what to have finished by morning.
     */
    fun startResearch(state: GameState, projectId: String, nowMillis: Long): GameState {
        val project = ResearchTree.byId(projectId) ?: return state
        if (!ResearchTree.isUnlocked(state)) return state
        if (state.activeResearch != null) return state
        if (ResearchTree.isDone(state, project)) return state
        if (!ResearchTree.isOpen(state, project)) return state
        if (project.cost > state.mass) return state

        val seconds = ResearchTree.duration(state, project)
        return state.copy(
            mass = state.mass - project.cost,
            activeResearch = project.id,
            researchDoneAt = nowMillis + (seconds * 1_000.0).toLong(),
        )
    }

    /**
     * Calls the running project off. The mass is gone.
     *
     * No refund on purpose: a refund would make starting the longest project the correct move
     * every time, to be cancelled the moment something better came into reach.
     */
    fun cancelResearch(state: GameState): GameState =
        if (state.activeResearch == null) {
            state
        } else {
            state.copy(activeResearch = null, researchDoneAt = 0)
        }

    /**
     * Books a project that has come due.
     *
     * Separate from [tick] because it is the only rule that reads the wall clock rather than
     * elapsed play time, and folding it in would mean handing every caller of `tick` a clock it
     * has no other use for.
     */
    fun settleResearch(state: GameState, nowMillis: Long): GameState {
        if (!ResearchTree.isFinished(state, nowMillis)) return state
        val finished = state.activeResearch ?: return state
        return award(
            state.copy(
                research = state.research + finished,
                activeResearch = null,
                researchDoneAt = 0,
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
        investments = state.investments,
        heavy = state.heavy,
        achievements = state.achievements,
        playedSeconds = state.playedSeconds,
        cometsCaught = state.cometsCaught,
        soundOn = state.soundOn,
        hapticsOn = state.hapticsOn,
        musicOn = state.musicOn,
        autoBuyOn = state.autoBuyOn,
        automation = state.automation,
        remindersOn = state.remindersOn,
        eventsAnswered = state.eventsAnswered,
        challengesDone = state.challengesDone,
        aeons = state.aeons,
        aeonUpgrades = state.aeonUpgrades,
        bigBangs = state.bigBangs,
        path = state.path,
        research = state.research,
        activeResearch = state.activeResearch,
        researchDoneAt = state.researchDoneAt,
    )

    /**
     * Every permanent effect the player owns, from all five sources.
     *
     * The two effects a run *begins* with are never read by [modifiersOf] — they are settled once,
     * here, when a run starts. Walking one sequence rather than five lists means a starting bonus
     * added to a challenge reward or an Äonen upgrade later works without anybody remembering to
     * come back to this function.
     */
    private fun permanentEffects(state: GameState): Sequence<PrestigeEffect> = sequence {
        for (id in state.prestigeUpgrades) PrestigeUpgrades.byId(id)?.effect?.let { yield(it) }
        for ((id, level) in state.investments) {
            val investment = Investments.byId(id) ?: continue
            val owned = level.coerceIn(0, investment.maxLevel)
            if (owned > 0) yield(investment.effectAt(owned))
        }
        for (id in state.challengesDone) Challenge.byId(id)?.reward?.let { yield(it) }
        for (id in state.research) ResearchTree.byId(id)?.effect?.let { yield(it) }
        for (id in state.aeonUpgrades) AeonUpgrades.byId(id)?.effect?.let { yield(it) }
    }

    /** Mass a fresh run begins with. These add up. */
    private fun startingMass(state: GameState): Double =
        permanentEffects(state)
            .filterIsInstance<PrestigeEffect.StartingMass>()
            .sumOf { it.mass }

    /** Collectors a fresh run begins with. The best one wins, they do not add. */
    private fun startingCollectors(state: GameState): Map<String, Int> {
        val count = permanentEffects(state)
            .filterIsInstance<PrestigeEffect.StartingCollectors>()
            .maxOfOrNull { it.count } ?: 0
        if (count <= 0) return emptyMap()
        return Collectors.all.associate { it.id to count }
    }

    /**
     * Buys levels of a repeatable investment.
     *
     * Bulk is walked one level at a time rather than solved, because the closed form for the
     * price of a run of levels is one rounding error away from charging for a level the player
     * cannot afford — and here that error would be paid in the currency a whole evening of play
     * produces about thirty of.
     */
    fun buyInvestment(state: GameState, investmentId: String, amount: Int = 1): GameState {
        val investment = Investments.byId(investmentId) ?: return state
        if (state.collapses < investment.requiredCollapses) return state

        val level = Investments.levelOf(state, investment)
        val wanted = if (amount <= 0) {
            investment.affordableLevels(level, state.singularities)
        } else {
            amount.coerceAtMost(investment.maxLevel - level)
        }
        if (wanted <= 0) return state

        val cost = investment.costForLevels(level, wanted)
        if (cost > state.singularities) return state

        return award(
            state.copy(
                singularities = state.singularities - cost,
                investments = state.investments + (investment.id to (level + wanted)),
            ),
        )
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

    fun setMusic(state: GameState, on: Boolean): GameState = state.copy(musicOn = on)

    /** The old single switch, kept because the settings screen still offers it as one. */
    fun setAutoBuy(state: GameState, on: Boolean): GameState =
        Automation.set(
            state,
            Automation.LEGACY_RULE,
            if (on) Automation.LEGACY_OPTION else null,
        )

    /** Moves one automation rule to its next setting, and off after the last one. */
    fun cycleAutomation(state: GameState, ruleId: String): GameState {
        val rule = AutomationRule.byId(ruleId) ?: return state
        if (!Automation.isAvailable(state, rule)) return state
        return Automation.cycle(state, rule)
    }

    fun setReminders(state: GameState, on: Boolean): GameState = state.copy(remindersOn = on)

    /** Whether the automatic buyer has been unlocked at all. */
    fun hasAutoBuy(state: GameState): Boolean = modifiersOf(state).autoBuy

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
            Milestones.factor(owned, mods.milestoneFactor) *
            mods.global * tier.productionMultiplier * singularityMultiplier(state, mods)
    }

    fun collectorOffers(state: GameState, amount: BuyAmount): List<CollectorOffer> {
        val mods = modifiersOf(state)
        val tier = Tiers.forMass(state.runMass)
        val scale = mods.global * tier.productionMultiplier * singularityMultiplier(state, mods)

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
                        Milestones.factor(owned, mods.milestoneFactor) * scale
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

    // ---------------------------------------------------------------- fusion

    /**
     * One row of the fusion panel: the machine, what it costs to improve, and whether it is
     * actually running.
     *
     * Starving is its own flag rather than something the panel infers from a rate of zero. A
     * furnace with no fuel and a furnace with no levels look identical in the numbers and mean
     * completely different things: one needs mass, the other needs the stage below it.
     */
    fun fusionOffers(state: GameState, amount: BuyAmount): List<FusionOffer> {
        val perSecond = fusionThroughput(state)
        val rate = modifiersOf(state).fusionRate
        return Fusion.stages.map { stage ->
            val level = Fusion.levelOf(state, stage)
            val count = if (amount == BuyAmount.MAX) {
                Fusion.affordableLevels(stage, level, state.mass)
            } else {
                amount.count.coerceAtMost(Fusion.MAX_LEVEL_STEP)
            }
            val cost = Fusion.costForLevels(stage, level, count)
            val capacity = level * stage.baseRate * rate
            val actual = perSecond[stage.id] ?: 0.0

            FusionOffer(
                stage = stage,
                level = level,
                amount = count,
                cost = cost,
                affordable = count > 0 && cost <= state.mass,
                outputPerSecond = actual,
                starving = level > 0 && actual < capacity * STARVING_BELOW,
            )
        }
    }

    /** Below this share of its capacity a furnace counts as starved rather than merely slow. */
    private const val STARVING_BELOW = 0.99

    /**
     * What each stage would actually manage over one second, given what is in the tanks.
     *
     * Runs the same walk [Fusion.advance] does, on a copy, rather than re-deriving it: two
     * implementations of the chain would drift, and the panel showing a rate the simulation does
     * not deliver is exactly the kind of lie that costs an evening to track down.
     */
    private fun fusionThroughput(state: GameState): Map<String, Double> {
        val after = Fusion.advance(state, 1.0)
        return Fusion.stages.associate { stage ->
            val produced = (after.elements[stage.output.id] ?: 0.0) -
                (state.elements[stage.output.id] ?: 0.0)
            // The stage above already ate some of this output in the same walk, so what is left in
            // the tank understates what was made. Adding back what the consumer took recovers it.
            val consumer = Fusion.stages.firstOrNull { it.input == stage.output }
            val eaten = if (consumer == null) {
                0.0
            } else {
                val madeAbove = (after.elements[consumer.output.id] ?: 0.0) -
                    (state.elements[consumer.output.id] ?: 0.0)
                madeAbove * consumer.ratio
            }
            stage.id to (produced + eaten).coerceAtLeast(0.0)
        }
    }

    /** Buys levels of a fusion stage, or nothing if they are unaffordable or still locked. */
    fun buyFuser(state: GameState, stageId: String, amount: BuyAmount): GameState {
        val stage = Fusion.byId(stageId) ?: return state
        if (!Fusion.isUnlocked(state)) return state

        val level = Fusion.levelOf(state, stage)
        val count = if (amount == BuyAmount.MAX) {
            Fusion.affordableLevels(stage, level, state.mass)
        } else {
            amount.count.coerceAtMost(Fusion.MAX_LEVEL_STEP)
        }
        if (count <= 0) return state

        val cost = Fusion.costForLevels(stage, level, count)
        if (cost > state.mass) return state

        return award(
            state.copy(
                mass = state.mass - cost,
                fusers = state.fusers + (stage.id to (level + count)),
            ),
        )
    }

    fun upgradeOffers(state: GameState): List<UpgradeOffer> =
        if (!modifiersOf(state).upgradesWork) emptyList() else Upgrades.all
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
                    Milestones.factor(owned, mods.milestoneFactor)
            }
        }
        return base * mods.global * tier.productionMultiplier * singularityMultiplier(state, mods)
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
            singularityMultiplier(state, mods)
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

        // Äonen first, then prestige, then the run: deepest layer sets the floor the shallower
        // ones build on.
        for (id in state.aeonUpgrades) {
            apply(mods, AeonUpgrades.byId(id)?.effect)
        }
        for (id in state.prestigeUpgrades) {
            apply(mods, PrestigeUpgrades.byId(id)?.effect)
        }

        // Investments speak the same vocabulary and go through the same fold. Each one hands over
        // the total effect of the levels owned rather than the effect of one level, which is what
        // keeps a repeatable purchase linear instead of turning it into a power.
        for ((id, level) in state.investments) {
            val investment = Investments.byId(id) ?: continue
            val owned = level.coerceIn(0, investment.maxLevel)
            if (owned > 0) apply(mods, investment.effectAt(owned))
        }

        // Challenge rewards are permanent in the same way prestige is, and are the same kind of
        // thing, so they go through the same switch rather than growing a parallel one.
        for (id in state.challengesDone) {
            apply(mods, Challenge.byId(id)?.reward)
        }

        // The lean of this universe, before anything bought inside it.
        Path.of(state)?.effects?.forEach { apply(mods, it) }

        // Finished research, likewise. It is the fourth kind of permanent thing and the fourth
        // list to walk, and all four say what they do in the same vocabulary — which is the whole
        // reason [apply] exists rather than four switches that drift apart.
        for (id in state.research) {
            apply(mods, ResearchTree.byId(id)?.effect)
        }

        // The running challenge, which is the only modifier that takes something away.
        when (val rule = state.challenge?.rule) {
            is ChallengeRule.NoCollectors -> mods.collectorsWork = false
            is ChallengeRule.NoTaps -> mods.tapsWork = false
            is ChallengeRule.Handicap -> mods.global *= rule.factor
            is ChallengeRule.NoUpgrades -> mods.upgradesWork = false
            is ChallengeRule.NoOrbits -> mods.orbitsWork = false
            null -> Unit
        }

        // Every achievement is worth a little, which is what stops them being decoration.
        mods.global *= Achievements.multiplier(state)

        // A buff is the only modifier with a clock on it.
        when (state.buff) {
            Buff.SURGE -> mods.global *= Buff.SURGE.factor
            Buff.INFERNO -> mods.global *= Buff.INFERNO.factor
            Buff.FRENZY -> mods.tapMultiplier *= Buff.FRENZY.factor
            null -> Unit
        }

        for (id in if (mods.upgradesWork) state.upgrades else emptySet()) {
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

        // Fusion last, and multiplying rather than raising a floor. Every other source of offline
        // efficiency is a `maxOf` — one upgrade replaces another — but the elements are a running
        // furnace, not a purchase, so they scale whatever the player has already earned. Capped at
        // one because crediting more than full production for time not spent playing would make
        // being away the better move.
        // The bodies in orbit, before fusion so that the two read in the order they unlock.
        if (mods.orbitsWork) mods.global *= Orbits.multiplier(state)

        // What past collapses forged. Unconditional, unlike the fusion chain below: these are
        // held rather than running, and an empty holding is a factor of one anyway.
        mods.global *= Heavy.factorFor(state, FusionBonus.GLOBAL)
        mods.tapMultiplier *= Heavy.factorFor(state, FusionBonus.TAP)
        mods.singularityGain *= Heavy.factorFor(state, FusionBonus.SINGULARITY)

        if (Fusion.isUnlocked(state)) {
            mods.global *= Fusion.factorFor(state, FusionBonus.GLOBAL)
            mods.tapMultiplier *= Fusion.factorFor(state, FusionBonus.TAP)
            mods.cometFrequency *= Fusion.factorFor(state, FusionBonus.COMETS)
            mods.singularityGain *= Fusion.factorFor(state, FusionBonus.SINGULARITY)
            mods.offlineEfficiency =
                (mods.offlineEfficiency * Fusion.factorFor(state, FusionBonus.OFFLINE))
                    .coerceAtMost(1.0)
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

            is PrestigeEffect.SingularityBonus ->
                mods.singularityBonus = maxOf(mods.singularityBonus, effect.perSingularity)

            is PrestigeEffect.MilestoneBonus -> mods.milestoneFactor += effect.extra
            is PrestigeEffect.AutoBuy -> mods.autoBuy = true
            is PrestigeEffect.FusionRate -> mods.fusionRate *= effect.factor
            is PrestigeEffect.ResearchSpeed -> mods.researchSpeed *= effect.factor

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
        var singularityBonus = SINGULARITY_BONUS
        var milestoneFactor = Milestones.FACTOR
        var autoBuy = false
        var fusionRate = 1.0
        var researchSpeed = 1.0

        /** A challenge can switch off a whole source of mass. */
        var collectorsWork = true
        var tapsWork = true
        var upgradesWork = true
        var orbitsWork = true

        val collectors = HashMap<String, Double>()

        fun collectorFactor(id: String): Double = collectors[id] ?: 1.0
    }
}

/** Convenience for tests and simulations: run [seconds] of production in one step. */
fun GameState.advanced(seconds: Double): GameState = GameEngine.tick(this, seconds)
