package com.staatseigentum.kollaps.core

import kotlinx.serialization.Serializable

/**
 * The complete, persisted state of a save file. Everything else the game shows is derived from
 * this by [GameEngine], so the save stays small and forward compatible: content definitions live
 * in code, the save only references them by id.
 */
@Serializable
data class GameState(
    val version: Int = SAVE_VERSION,

    /** Mass available to spend right now. */
    val mass: Double = 0.0,

    /** Mass collected during the current run. Drives the tier ladder. */
    val runMass: Double = 0.0,

    /** Mass collected since the very first launch, across all collapses. */
    val totalMass: Double = 0.0,

    /** Number of copies owned, keyed by collector id. */
    val collectors: Map<String, Int> = emptyMap(),

    /**
     * Which role each collector is set to, keyed by collector id. See [Roles].
     *
     * Part of the run: a collapse takes the fleet, so it takes how the fleet was set up.
     */
    val roles: Map<String, String> = emptyMap(),

    /** Ids of upgrades bought in the current run. */
    val upgrades: Set<String> = emptySet(),

    /** Permanent prestige currency earned by collapsing black holes. */
    val singularities: Double = 0.0,

    /** How often the player collapsed a black hole. */
    val collapses: Int = 0,

    /** Total taps, across all runs. */
    val taps: Long = 0,

    /**
     * Taps that hit the sky instead of the body, across all runs.
     *
     * Counted rather than ignored purely so that one achievement can notice. Nothing else reads
     * it, and it is deliberately not part of [taps] — a miss produced nothing, and rolling it into
     * the tap counter would make every tapping achievement claimable by flailing at the background.
     */
    val missedTaps: Long = 0,

    /** Highest tier index ever reached, across all runs. */
    val bestTier: Int = 0,

    /** Most mass ever collected within a single run. */
    val bestRunMass: Double = 0.0,

    /** Highest tier the player has already been shown the "new tier" celebration for. */
    val celebratedTier: Int = 0,

    /** Wall clock of the last save, used to credit offline production. */
    val lastSeenAt: Long = 0,

    /** Wall clock of the first launch. */
    val startedAt: Long = 0,

    /** Prestige upgrades bought. Unlike [upgrades], these survive a collapse. */
    val prestigeUpgrades: Set<String> = emptySet(),

    /**
     * Levels of each repeatable singularity investment. Survive a collapse, not a big bang —
     * they are bought with singularities, and the big bang takes those. See [Investments].
     */
    val investments: Map<String, Int> = emptyMap(),

    /** Achievements earned, across all runs. */
    val achievements: Set<String> = emptySet(),

    /** Seconds the game was actually on screen, for the statistics. */
    val playedSeconds: Double = 0.0,

    /** Seconds of play spent in the current run. Reset by anything that resets the run. */
    val runSeconds: Double = 0.0,

    /**
     * How hot the body is from being tapped, `0.0..1.0`.
     *
     * The game is called tapping on celestial bodies, and by the fifteenth rung a tap is worth
     * nothing against what the collectors make — the central verb quietly stopped mattering
     * halfway through. This is the answer: tapping does not pay *itself* better, it makes the
     * whole production faster while it lasts.
     *
     * Deliberately small and deliberately short-lived. A heat that doubled output would turn an
     * idle game into an obligation and punish the player for putting the phone down, which is the
     * one thing the genre must never do. What this buys is a reason to stay for a minute, not a
     * reason to feel guilty for leaving.
     *
     * Not something that can be banked: it decays in seconds, and time away zeroes it outright.
     */
    val heat: Double = 0.0,

    /** How long the previous run took and how far it got, so this one has something to beat. */
    val lastRunSeconds: Double = 0.0,
    val lastRunMass: Double = 0.0,

    /**
     * The fastest run ever finished, in seconds of play. Zero until the first collapse.
     *
     * The last run is what you are beating right now; this is what you are beating for good. A
     * prestige loop without it is the same four hours again — with it, it is the same four hours
     * with a number attached, which is the entire difference between repetition and progress.
     *
     * Only runs that actually reached the black hole land here, because only those are
     * comparable: a collapse is impossible before it, so there is no such thing as a short run
     * that cheated.
     */
    val bestRunSeconds: Double = 0.0,

    /** Comets caught, across all runs. */
    val cometsCaught: Long = 0,

    /** Id of the buff currently running, if any. */
    val buffId: String? = null,

    /** Seconds the current buff has left. Counted down by the tick, not by the wall clock, so
     *  putting the phone away pauses it rather than wasting it. */
    val buffSecondsLeft: Double = 0.0,

    /** Whether the click sound plays. */
    val soundOn: Boolean = true,

    /** Whether tapping vibrates. */
    val hapticsOn: Boolean = true,

    /** Whether the background loop plays. */
    val musicOn: Boolean = true,

    /**
     * Which colour scheme the bodies are drawn in, by [com.staatseigentum.kollaps.core.pixel.Skin].
     *
     * Null is the plain one. An id that is unknown, or one whose achievements are no longer there,
     * falls back rather than failing — a palette is decoration and never a reason not to draw.
     */
    val skinId: String? = null,

    /**
     * Id of the challenge being run — the shape from before two could run at once.
     *
     * Read only where an old save carries it; see [runningChallengeIds]. Kept rather than removed
     * because deleting it would make every save written before this point start its challenge over.
     */
    val activeChallenge: String? = null,

    /** Ids of the challenges being run, at most [Challenge.MAX_AT_ONCE] of them. */
    val activeChallenges: Set<String> = emptySet(),

    /** Seconds of play spent inside the running challenge. Only the tick moves it. */
    val challengeSeconds: Double = 0.0,

    /** Challenges completed. Their rewards are permanent, like prestige upgrades. */
    val challengesDone: Set<String> = emptySet(),

    /**
     * Pairs handed in together, by [Challenge.duoId].
     *
     * A pair is only recorded when neither of the two had been done before, which is what bounds
     * this: with eight challenges there are at most four pairs of untouched ones to be had, ever.
     */
    val challengeDuos: Set<String> = emptySet(),

    /**
     * Fraction of an automatic tap carried over between ticks.
     *
     * The tick runs many times a second, so three automatic taps per second is a fraction of a
     * tap each time. Without somewhere to keep the remainder the tap counter would round every
     * one of them down to nothing.
     */
    val autoTapCarry: Double = 0.0,

    /** Äonen from big bangs. The currency below the singularity. */
    val aeons: Double = 0.0,

    /**
     * Äonen earned but not yet whole, carried between ticks. See [Multiverse.advance].
     *
     * Below one and therefore invisible, which is exactly why it has to be in the save: a parked
     * galaxy earns an Äon every couple of days, and rounding this away every tick would pay out
     * nothing, for ever, while looking like it worked.
     */
    val aeonFraction: Double = 0.0,

    /** Äonen upgrades bought. These survive even a big bang. */
    val aeonUpgrades: Set<String> = emptySet(),

    /**
     * The universes that have been through their big bang and kept going. See [Multiverse].
     *
     * The big bang no longer ends a universe, it parks one — so this is the record of every one
     * the player has finished, each in a galaxy of its own, each still earning. Nothing in here is
     * ever removed: a save that has been through more than [Multiverse.SLOTS] big bangs keeps the
     * best of them and the rest sit as history.
     */
    val universes: List<ParkedUniverse> = emptyList(),

    /**
     * Alloys forged in the workshop above the heavy elements. See [Alloy].
     *
     * Kept through both resets, exactly like the metals they are made of: whatever an alloy is, it
     * was welded out of something a dying star made, and neither a collapse nor a big bang is
     * going to unweld it.
     */
    val alloys: Set<String> = emptySet(),

    /** How often the player has finished a universe and started the next one. */
    val bigBangs: Int = 0,

    /** Which kind of universe this one is, chosen at the big bang. See [Path]. */
    val path: String? = null,

    /**
     * Nodes of the path trees that have been bought, by [PathNode.id].
     *
     * Every one of them is kept for good — but a node only does anything while [path] is the one
     * it belongs to. Nodes bought for a path the player has since left stay in here, waiting for
     * the universe that aligns to it again.
     */
    val pathNodes: Set<String> = emptySet(),

    /** Whether the automatic buyer is switched on. Off by default even once it is unlocked. */
    val autoBuyOn: Boolean = false,

    /** Whether the game may remind the player that the collectors have filled up. */
    val remindersOn: Boolean = true,

    /**
     * Whether a quiet line stays in the shade while the game is closed.
     *
     * Its own switch rather than part of [remindersOn], because the two are different bargains: a
     * reminder arrives once and is gone, while this one sits there until the game is opened, and
     * plenty of people who want the first want nothing to do with the second.
     */
    val statusOn: Boolean = true,

    /** How numbers are written, by [NumberFormat] name. Null is the German names. */
    val numberFormat: String? = null,

    /**
     * Whether the first-five-minutes nudge has been sent away for good.
     *
     * Only ever set by the player pressing the button on it. Finishing the last step hides it
     * without setting this, because the two are different things: one is "there is nothing left
     * to say", the other is "do not talk to me".
     */
    val tutorialDone: Boolean = false,

    /**
     * Id of whatever is waiting for an answer, if anything.
     *
     * Either a [CosmicEvent] id or, while a chain is running, the id of the [ChainStation] on the
     * table. One field rather than two, because "something is asking a question" is one condition
     * and two flags for it is two ways to disagree; which kind it is comes from [activeChain].
     */
    val pendingEvent: String? = null,

    /** The [EventChain] being told, if one is. */
    val activeChain: String? = null,

    /**
     * Which station of that chain comes next.
     *
     * Held apart from [pendingEvent] so that turning a station down closes the dialog without
     * losing the story: the chain stays where it is and asks again at the next interval.
     */
    val chainStation: String? = null,

    /** Chains seen through to an ending. Kept for good, like the achievements. */
    val chainsDone: Set<String> = emptySet(),

    /**
     * Seconds of play until the next event.
     *
     * Zero on a fresh save, which the tick reads as "not scheduled yet" and fills in. Counting in
     * play time rather than wall clock means an event cannot be waiting the moment the app opens
     * after a night away.
     */
    val nextEventSeconds: Double = 0.0,

    /** Events answered, for the statistics. */
    val eventsAnswered: Long = 0,

    /** Production per second, sampled during play. Oldest first. See [History]. */
    val history: List<Double> = emptyList(),

    /** Seconds of play since the last sample was taken. */
    val historySeconds: Double = 0.0,

    /** Elements held, keyed by [Element.id]. Part of the run, so a collapse takes them along. */
    val elements: Map<String, Double> = emptyMap(),

    /** Levels of each fusion stage, keyed by [FusionStage.id]. */
    val fusers: Map<String, Int> = emptyMap(),

    /**
     * Heavy elements forged in past collapses, keyed by [HeavyElement.id].
     *
     * The one holding that survives everything, big bang included — see [Heavy].
     */
    val heavy: Map<String, Double> = emptyMap(),

    /** How many orbit slots are open. Part of the run — a collapse scatters the system. */
    val orbits: Int = 0,

    /** Mass of the body on each occupied slot, keyed by orbit index. See [Orbits]. */
    val satellites: Map<Int, Double> = emptyMap(),

    /** Research projects finished. Permanent: neither a collapse nor a big bang touches them. */
    val research: Set<String> = emptySet(),

    /** Id of the project running in the lab, if any. */
    val activeResearch: String? = null,

    /**
     * Wall clock at which the running project comes due.
     *
     * An absolute time rather than a countdown, because this is the one clock in the game that
     * keeps running with the app closed. A remaining-seconds field would have to be topped up by
     * something, and nothing runs while the phone is off.
     */
    val researchDoneAt: Long = 0,

    /**
     * Automation rules that are switched on, and which setting each runs at.
     *
     * A rule missing from the map is off. See [Automation], which also explains why the older
     * [autoBuyOn] flag is still read rather than migrated away.
     */
    val automation: Map<String, Int> = emptyMap(),
) {
    fun ownedOf(collectorId: String): Int = collectors[collectorId] ?: 0

    fun owns(upgradeId: String): Boolean = upgradeId in upgrades

    fun ownsPrestige(upgradeId: String): Boolean = upgradeId in prestigeUpgrades

    fun ownsAeon(upgradeId: String): Boolean = upgradeId in aeonUpgrades

    /** The buff currently running, or `null` once it has run out. */
    val buff: Buff? get() = if (buffSecondsLeft > 0.0) Buff.byId(buffId) else null

    /**
     * The challenges currently being run, as ids.
     *
     * The set wins where it has anything in it, and the old single field answers otherwise — a
     * save written before pairs existed keeps running exactly the challenge it was running.
     */
    val runningChallengeIds: Set<String>
        get() = if (activeChallenges.isNotEmpty()) activeChallenges else setOfNotNull(activeChallenge)

    /**
     * The first challenge being run, or `null`.
     *
     * Everything that needs all of them asks [Challenge.running]; this stays for the places that
     * only want to know whether a challenge is on at all.
     */
    val challenge: Challenge? get() = Challenge.running(this).firstOrNull()

    /** The one-off event waiting for an answer, or `null` — a chain station is not one. */
    val event: CosmicEvent? get() = if (activeChain != null) null else CosmicEvent.byId(pendingEvent)

    /** The chain station waiting for an answer, or `null`. */
    val station: ChainStation? get() = Chains.stationOf(this)

    /**
     * What the dialog should put on screen, whichever kind of event produced it.
     *
     * The screen has no business knowing whether a question came from a one-off or from the middle
     * of a story — it draws a title, a sentence and two answers either way. Deriving the one shape
     * here keeps that difference in the rules, where it belongs.
     */
    val prompt: EventPrompt?
        get() {
            station?.let { stop ->
                return EventPrompt(
                    title = stop.title,
                    flavor = stop.flavor,
                    first = stop.first,
                    second = stop.second,
                    chain = Chains.byId(activeChain)?.title,
                )
            }
            val single = event ?: return null
            return EventPrompt(single.title, single.flavor, single.first, single.second, chain = null)
        }

    companion object {
        /**
         * Four. Every field added since version one has a default, so an old save still reads —
         * it simply arrives with none of them, which is exactly right for a player who has not
         * earned any yet. The number itself only matters where a value means something different
         * than it used to; see the tier remap in [SaveCodec].
         */
        const val SAVE_VERSION = 4

        fun new(nowMillis: Long): GameState = GameState(
            lastSeenAt = nowMillis,
            startedAt = nowMillis,
        )
    }
}
