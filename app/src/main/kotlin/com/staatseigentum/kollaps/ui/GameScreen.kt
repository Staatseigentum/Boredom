package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.core.Element
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.NumberFormat
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.audio.Mood
import com.staatseigentum.kollaps.core.pixel.Skins
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Space
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Everything the screen can ask the game to do. */
interface GameActions {
    /** Returns the mass the tap produced, for the floating number. */
    fun tap(): Double

    /** A tap that landed on the sky rather than on the body. Pays nothing, counts once. */
    fun tapEmpty()
    fun setBuyAmount(amount: BuyAmount)
    fun buyCollector(id: String)

    /** Walks one collector to its next role, and back to none after the last. */
    fun cycleRole(id: String)
    fun buyUpgrade(id: String)

    /** Buys levels of a fusion stage, in whatever the current buy amount is. */
    fun buyFuser(id: String)

    /** Opens the next orbit slot, puts a body on one, or drops one body onto another. */
    fun openOrbit()
    fun seedSatellite(orbitIndex: Int)
    fun mergeSatellites(from: Int, to: Int)

    /** Puts a project on the lab bench. The mass is taken now, the result arrives later. */
    fun startResearch(id: String)
    fun cancelResearch()

    /** Walks one automation rule to its next setting, and off after the last. */
    fun cycleAutomation(id: String)
    fun collapse()
    fun dismissOfflineReport()
    fun acknowledgeTier()

    /** Catches a comet that drifted past and was tapped in time. */
    fun catchComet(comet: Comet)

    /** Answers the waiting event with one of its two options. */
    fun chooseEvent(optionIndex: Int)
    fun dismissEvent()

    /** Throws the universe away and starts one leaning towards [pathId]. */
    fun bigBang(pathId: String)
    fun buyAeonUpgrade(id: String)

    /** Buys one node of the running universe's path tree. */
    fun buyPathNode(id: String)

    /** Starts a run under one or two challenges at once. */
    fun startChallenges(ids: Set<String>)
    fun abortChallenge()
    fun finishChallenge()

    fun buyPrestigeUpgrade(id: String)

    /** Buys levels of a repeatable investment. Zero or less means as many as are affordable. */
    fun buyInvestment(id: String, amount: Int)

    /** Throws the whole save away and starts over. There is no way back from this. */
    fun eraseSave()
    fun setSound(on: Boolean)
    fun setHaptics(on: Boolean)
    fun setMusic(on: Boolean)

    /** Picks the colour scheme the bodies are drawn in. */
    fun setSkin(id: String)
    fun setAutoBuy(on: Boolean)
    fun setReminders(on: Boolean)

    /** Whether a quiet line stays in the shade while the game is closed. */
    fun setStatus(on: Boolean)

    /** Sends the first-steps nudge away for good. */
    fun dismissTutorial()

    /**
     * Holds the game still while the collapse plays out.
     *
     * The three seconds it costs are not the point — the point is that the screen is showing the
     * run that just ended, and a game that went on earning behind that picture would be lying
     * about where the player is. Time spent paused is dropped rather than banked, so nothing is
     * owed when it starts again.
     *
     * The collapse itself has already been applied to the save by the time this is switched on,
     * so a phone locked mid-sequence loses the animation and the pause together, and keeps the
     * collapse.
     */
    fun setPaused(on: Boolean)

    /** Picks how the very large numbers are written. */
    fun setNumberFormat(format: NumberFormat)

    /** Replaces the running game with an exported one. False when the block was not readable. */
    fun importSave(block: String): Boolean

    /** The current save as a block the player can copy out. */
    fun exportSave(): String
}

/**
 * The whole game, taking plain state and callbacks rather than view models.
 *
 * That is what lets the desktop harness run this exact screen instead of a lookalike: nothing
 * here knows about Android, persistence or the updater. The app supplies a thin wrapper that
 * collects the flows, and the updater is handed in as two slots because it is Android-only —
 * no other platform can install an APK over itself.
 */
@Composable
fun GameScreen(
    state: GameState,
    stats: Stats,
    buyAmount: BuyAmount,
    offlineReport: OfflineReport?,
    actions: GameActions,
    modifier: Modifier = Modifier,
    /** Which shop tab to open on — the harness uses it to photograph the other ones. */
    startTab: Int = 0,
    /** Which section of the Kosmos tab to open on. Only the harness passes anything else. */
    startSection: Int = 0,
    updateSection: @Composable () -> Unit = {},
    /** The save slots, handed in because where the files live is the platform's business. */
    saveSlots: @Composable () -> Unit = {},
    updateDialog: @Composable () -> Unit = {},
) {
    // The sound setting is enforced once, here, by taking the player away from every widget
    // below rather than by teaching each of them to ask whether it is allowed to make a noise.
    val sfx = LocalSfx.current

    // The loop follows the kind of body rather than the tier, so it changes on the seven moments
    // that mean something instead of on all twenty-four. Driven from here because this is the one
    // place that knows both which body is on screen and whether the player wants to hear it.
    val music = LocalMusic.current
    LaunchedEffect(music, state.musicOn, stats.tier.kind) {
        if (state.musicOn) music?.play(Mood.of(stats.tier.kind)) else music?.stop()
    }

    // Read off the counters rather than hung on the buttons, so a collapse the automation rule
    // carried out while the player was watching the body still goes off on screen. The remembered
    // starting values are what keeps a freshly loaded save from detonating on the first frame.
    var blast by remember { mutableStateOf<Pair<Int, BlastKind>?>(null) }

    // Muted here rather than at the call site, because this fires from a state change and the
    // provider below has not narrowed the sound away yet.
    val blastSfx = sfx.takeIf { state.soundOn }

    /*
     * The collapse, as a picture.
     *
     * The rules have already run by the time this starts — `state` is the new run, one meteorite
     * and no mass. That is deliberate: making the sequence part of the game state would mean a
     * player who closes the app a second in has not collapsed, and a collapse that can be lost by
     * locking a phone is worse than an animation that can be. So the screen keeps the *old* state
     * on display for as long as it is being pulled in, and nothing below has to know.
     */
    val collapse = remember { CollapseSequence() }
    val reduceMotion = LocalReduceMotion.current

    /*
     * Recorded here in the composition rather than from an effect, because effects run on the frame
     * that already carries the reset state — anything written from one would be the new run, which
     * is precisely the screen the sequence is not supposed to show. While the counter still agrees
     * this is simply the current screen; the frame it stops agreeing, the sequence is about to take
     * over and this is what it inherits. Recording resumes when the sequence hands it back.
     */
    val held = remember { HeldScreen(state, stats, state.collapses) }
    if (state.collapses == held.collapses) {
        held.state = state
        held.stats = stats
    }

    /*
     * Through [derivedStateOf], which matters more than it looks.
     *
     * `showingOld` is a function of the clock, and the clock moves sixty times a second. Read
     * directly, this composable would subscribe to it and recompose the entire screen — header,
     * shop, every row in it — on every frame of the sequence, at exactly the moment the game most
     * needs the frames. The derived value changes twice in four seconds; that is what recomposes.
     */
    val showingOld by remember(collapse) { derivedStateOf { collapse.showingOld } }
    val shownState = if (showingOld) held.state else state
    val shownStats = if (showingOld) held.stats else stats

    val collapsesAtStart = remember { state.collapses }
    LaunchedEffect(state.collapses) {
        if (state.collapses <= collapsesAtStart) return@LaunchedEffect

        val detonate: () -> Unit = {
            blast = state.collapses to BlastKind.KOLLAPS
            blastSfx?.success()
        }

        // Straight to the bang when the system has been told to keep still. Not a shortened
        // version of the same thing — somebody who switched animations off does not want a
        // politer spiral, they want it over with.
        if (reduceMotion) {
            detonate()
            held.collapses = state.collapses
            return@LaunchedEffect
        }

        // Fired once and then left alone. The rumble is exactly as long as the pull and ends in
        // silence of its own accord, so there is nothing to stop and nothing to keep in step —
        // and an app closed halfway through simply takes it along.
        blastSfx?.collapse()

        // In a `finally` so that a screen torn down mid-fall still hands the game back: were it
        // not, the next composition would go on showing a run that ended six seconds ago, and the
        // one after that would find the game still paused.
        actions.setPaused(true)
        try {
            collapse.run(detonate)
        } finally {
            actions.setPaused(false)
            held.collapses = state.collapses
        }
    }

    // The two quiet ones. Both fire off a state change rather than off a button, because neither
    // has one: a project comes due on the wall clock and a core catches light on its own.
    val researchAtStart = remember { state.research.size }
    LaunchedEffect(state.research.size) {
        if (state.research.size > researchAtStart) blastSfx?.research()
    }

    val litAtStart = remember { Fusion.amountOf(state, Element.HELIUM) >= 1.0 }
    val lit = Fusion.amountOf(state, Element.HELIUM) >= 1.0
    LaunchedEffect(lit) {
        if (lit && !litAtStart) blastSfx?.ignition()
    }

    val bangsAtStart = remember { state.bigBangs }
    LaunchedEffect(state.bigBangs) {
        if (state.bigBangs > bangsAtStart) {
            blast = -state.bigBangs to BlastKind.URKNALL
            blastSfx?.success()
        }
    }

    // The palette is settled once, here, so every body on screen agrees on it — and it is
    // resolved rather than taken raw, so a scheme that is not actually earned falls back. Off the
    // shown state, so the body being pulled in keeps the colours it had.
    val skin = Skins.current(shownState.skinId, shownState.achievements.size)

    // The number format likewise, and for the same reason: one place decides, everything below
    // reads the same thing. Applied on every change rather than once, because a save imported
    // mid-session brings its own preference with it.
    LaunchedEffect(state.numberFormat) {
        Numbers.format = NumberFormat.byName(state.numberFormat)
    }

    CompositionLocalProvider(
        LocalSfx provides sfx.takeIf { state.soundOn },
        LocalSkin provides skin,
        LocalCollapse provides collapse,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Space),
        ) {
            Starfield(
                tint = Color(shownStats.tier.glowColor),
                depth = shownStats.tier.index / (Tiers.all.size - 1f).coerceAtLeast(1f),
                warp = { collapse.warp },
                warpCentre = { collapse.centre },
                modifier = Modifier.fillMaxSize(),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                val body = @Composable { modifier: Modifier ->
                    TapArea(
                        state = shownState,
                        stats = shownStats,
                        actions = actions,
                        // The one thing that does not move: it is what everything else moves
                        // towards, and the sprite in the middle of it is the hole itself.
                        modifier = modifier.collapseCentre(),
                    )
                }
                val shop = @Composable { modifier: Modifier ->
                    ShopPanel(
                        state = shownState,
                        stats = shownStats,
                        buyAmount = buyAmount,
                        actions = actions,
                        modifier = modifier,
                        startTab = startTab,
                        startSection = startSection,
                        updateSection = updateSection,
                        saveSlots = saveSlots,
                    )
                }

                // Side by side once there is room for it. Stacked, the body gets a strip of a
                // landscape screen and the shop scrolls a line at a time; the tap area is the
                // thing that wants height, and in a row it can have all of it.
                if (maxWidth >= WIDE_THRESHOLD && maxWidth > maxHeight) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Header(state = shownState, stats = shownStats)
                            body(Modifier.fillMaxWidth().weight(1f))
                        }
                        shop(Modifier.fillMaxHeight().weight(1f))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Header(state = shownState, stats = shownStats)
                        body(Modifier.fillMaxWidth().weight(1f))
                        shop(Modifier.fillMaxWidth().weight(1.15f))
                    }
                }
            }

            offlineReport?.let { report ->
                OfflineDialog(report = report, onDismiss = actions::dismissOfflineReport)
            }

            if (GameEngine.hasUncelebratedTier(state)) {
                TierCelebration(tier = stats.tier, onDismiss = actions::acknowledgeTier)
            }

            state.prompt?.let { prompt ->
                EventDialog(
                    prompt = prompt,
                    onChoose = actions::chooseEvent,
                    onDismiss = actions::dismissEvent,
                )
            }

            // Over everything, including the dialogs: the blast is the loudest thing that can
            // happen, and something covering half of it would read as a glitch.
            blast?.let { (trigger, kind) ->
                Blast(
                    trigger = trigger,
                    kind = kind,
                    onFinished = { blast = null },
                )
            }

            /*
             * Nothing gets through while the collapse is playing.
             *
             * Pausing the tick is only half of holding the game still: the shop is on screen and
             * every row in it is a button, and the body being pulled into the hole is still a tap
             * target. Worse, the screen is showing the run that just ended — a purchase made
             * against those numbers would be charged against a game the player cannot see.
             *
             * Every event is consumed on the initial pass, before anything below has a chance to
             * look at it. Derived rather than read straight off the clock, so this appears and
             * disappears once instead of on every frame.
             */
            val blocking by remember(collapse) { derivedStateOf { collapse.running } }
            if (blocking) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                        .changes
                                        .forEach { it.consume() }
                                }
                            }
                        },
                )
            }

            // Higher still, because the pieces come off things that are themselves above the
            // sky — and because they are drawn in window coordinates, which is what this box is.
            CollapseDebris(sequence = collapse, modifier = Modifier.fillMaxSize())

            updateDialog()
        }
    }
}

/**
 * The screen as it stood before the last collapse.
 *
 * A plain object rather than snapshot state on purpose: it is written during composition, and a
 * snapshot write there would either be discarded or start the composition over. Nothing observes
 * it — what reads it is already recomposing on the sequence's clock.
 */
private class HeldScreen(var state: GameState, var stats: Stats, var collapses: Int)

// ---------------------------------------------------------------------- header

@Composable
private fun Header(state: GameState, stats: Stats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            // The lines inside go first and go into this column; a second later the column itself
            // goes into the hole, carrying whatever is left of them.
            .sog(SogDepth.SHELL, Ember),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().sog(SogDepth.CONTENT, Muted),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Stufe ${stats.tier.index + 1}/${Tiers.all.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Muted,
            )
            if (state.singularities > 0) {
                Text(
                    text = "• ${Numbers.format(state.singularities)} Singularitäten",
                    style = MaterialTheme.typography.labelLarge,
                    color = Ember,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = Numbers.formatMass(state.mass),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.sog(SogDepth.CONTENT, Starlight),
        )

        Text(
            text = "${Numbers.formatRate(stats.massPerSecond)}  ·  ${Numbers.format(stats.massPerTap)} pro Tipp",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            modifier = Modifier.sog(SogDepth.CONTENT, Muted),
        )

        val buff = stats.buff
        if (buff != null) {
            Spacer(Modifier.height(8.dp))
            PixelPanel(
                modifier = Modifier.fillMaxWidth(),
                border = Ember,
                padding = 8,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PixelLabel(
                        text = "${buff.label} ${Numbers.formatMultiplier(buff.factor)}",
                        color = Ember,
                        size = 12,
                    )
                    Text(
                        text = "noch ${stats.buffSecondsLeft.toInt()} s",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        }

        // What the lab is doing, in the one place the player is always looking. The Kosmos tab
        // is three taps away and the whole point of the lab is that it runs while you are not
        // looking at it.
        ResearchTicker(state = state)

        Spacer(Modifier.height(12.dp))

        val next = stats.nextTier
        val glow = Color(stats.tier.glowColor)
        Text(
            text = if (next != null) {
                "${stats.tier.label} > ${next.label}"
            } else {
                "${stats.tier.label} — das Ende der Leiter"
            },
            style = MaterialTheme.typography.titleLarge,
            color = glow,
            modifier = Modifier.sog(SogDepth.CONTENT, glow),
        )

        Spacer(Modifier.height(6.dp))

        PixelBar(
            progress = stats.tierProgress,
            color = glow,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .sog(SogDepth.CONTENT, glow),
        )

        if (next != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "noch ${Numbers.formatMass(next.threshold - state.runMass)}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.sog(SogDepth.CONTENT, Muted),
            )
        }
    }
}

/** One line under the header while a project is running, counting down on the wall clock. */
@Composable
private fun ResearchTicker(state: GameState) {
    val running = ResearchTree.active(state) ?: return

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val left = ResearchTree.secondsLeft(state, now)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Labor: ${running.name}",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Text(
            text = if (left <= 0.0) "fertig" else Numbers.formatDuration(left.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = Ember,
        )
    }
    Spacer(Modifier.height(4.dp))
    PixelBar(
        progress = ResearchTree.progress(state, now),
        color = Ember,
        cells = 20,
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
    )
}

// ---------------------------------------------------------------------- tap area

private class TapEffect(val id: Long, val position: Offset, val label: String)

@Composable
private fun TapArea(
    state: GameState,
    stats: Stats,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    val tier = stats.tier
    val effects = remember { mutableStateListOf<TapEffect>() }
    var nextId by remember { mutableLongStateOf(0L) }
    val squash = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    /*
     * Read through [rememberUpdatedState] rather than captured directly.
     *
     * The gesture detector below is keyed on `Unit`, so Compose sets it up once and never restarts
     * it — which is what you want for a handler that fires several times a second. The catch is
     * that its lambda then keeps whatever it closed over on the *first* composition, for good. That
     * is exactly what happened here: switching the click sound off changed what `LocalSfx` provides,
     * the composable recomposed, and the tap handler went on calling the sound object it had
     * captured minutes earlier. The setting looked ignored because it was.
     *
     * Re-keying the detector on these values would fix it too, and would tear down and rebuild the
     * gesture handler every time a switch is flipped. This way the handler stays put and simply
     * reads the current value each time it fires.
     */
    val sfx by rememberUpdatedState(LocalSfx.current)
    val hapticsOn by rememberUpdatedState(state.hapticsOn)
    val tap by rememberUpdatedState(actions::tap)
    val empty by rememberUpdatedState(actions::tapEmpty)

    // The body grows as the ladder is climbed — a meteorite fills a quarter of the area and a
    // black hole all of it. Captured directly, the handler would keep sizing the target to
    // whatever body was on screen when the game opened.
    val hitTier by rememberUpdatedState(tier)

    // A ring thrown off the body every time it climbs a rung. Only upwards: a collapse drops the
    // tier by twenty-four steps at once and already has a blast of its own.
    val lastTier = remember { mutableIntStateOf(tier.index) }
    var pulse by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(tier.index) {
        if (tier.index > lastTier.intValue) pulse = tier.index
        lastTier.intValue = tier.index
    }

    /*
     * The body has to be hit, not merely the screen it is on.
     *
     * The radius is worked out the same way [CelestialBody] works out how large to draw: the
     * shorter side of the area times the tier's sprite fraction, halved. Deriving it rather than
     * picking a number is the whole point — the bodies range from a quarter of the area to all of
     * it, and a fixed radius would make the meteorite unhittable and the black hole hittable from
     * the corners.
     *
     * A little larger than what is drawn, because the sprite is a circle inside a square buffer
     * and a thumb is not a pixel. Missing the body you clearly aimed at is a worse feeling than
     * hitting one you nearly missed.
     */
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { position ->
                val centre = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) / 2f *
                    PixelPlanet.spriteFraction(hitTier) * HIT_FORGIVENESS

                if ((position - centre).getDistance() > radius) {
                    // The sky. Pays nothing and says so — but it is counted, because exactly one
                    // achievement is waiting for somebody to do it.
                    empty()
                    sfx?.missed()
                    return@detectTapGestures
                }

                val gained = tap()
                effects += TapEffect(nextId++, position, Numbers.format(gained))
                sfx?.click()
                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    squash.snapTo(0.93f)
                    squash.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        // The body is the one thing the collapse does not pull anywhere — it swells as it feeds,
        // is crushed to nothing, and comes back as the meteorite of the new run. Its share of the
        // sequence is read straight off the clock rather than going through [sog].
        val collapse = LocalCollapse.current
        CelestialBody(
            tier = tier,
            extraTurns = { collapse?.extraSpin ?: 0f },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val swell = collapse?.bodyScale ?: 1f
                    scaleX = squash.value * swell
                    scaleY = squash.value * swell
                    alpha = collapse?.bodyAlpha ?: 1f
                },
        )

        TierPulse(
            trigger = pulse,
            color = Color(tier.glowColor),
            modifier = Modifier.fillMaxSize(),
        )

        // The real system, under the collector rings: these are bodies with mass and a tier,
        // the rings above them are a picture of how many machines are in the shop.
        OrbitingBodies(state = state, modifier = Modifier.fillMaxSize())

        // Drawn over the body rather than behind it: half of each orbit passes in front, and
        // sorting per satellite would cost more than the illusion is worth at this size.
        Satellites(state = state, tier = tier, modifier = Modifier.fillMaxSize())

        for (effect in effects) {
            key(effect.id) {
                TapFeedback(
                    effect = effect,
                    color = Color(tier.glowColor),
                    onFinished = { effects.remove(effect) },
                )
            }
        }

        // Along the top, where nothing else is: the header sits above this box, not in it.
        AchievementToast(
            earned = state.achievements,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        )

        // Along the bottom of the body, out of the way of the thumb that is tapping it.
        TutorialHint(
            state = state,
            onDismiss = actions::dismissTutorial,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        )

        // Last, so a comet is never covered by the body it drifts past.
        CometOverlay(
            state = state,
            frequency = GameEngine.cometFrequency(state),
            onCatch = actions::catchComet,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TapFeedback(effect: TapEffect, color: Color, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(effect.id) {
        progress.animateTo(1f, animationSpec = tween(900, easing = LinearOutSlowInEasing))
        onFinished()
    }

    // Its own full-size box, so the offsets below are measured from the top left of the tap
    // area and not from the centre where the planet sits.
    Box(modifier = Modifier.fillMaxSize()) {
        // A ring of blocks flying outwards, rather than a smooth expanding circle.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = progress.value
            val radius = 12.dp.toPx() + p * 48.dp.toPx()
            val block = 5.dp.toPx() * (1f - p * 0.5f)
            val alpha = (1f - p) * 0.9f
            for (index in 0 until SHOCKWAVE_BLOCKS) {
                val angle = index.toFloat() / SHOCKWAVE_BLOCKS * TWO_PI
                drawRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(
                        effect.position.x + cos(angle) * radius - block / 2f,
                        effect.position.y + sin(angle) * radius - block / 2f,
                    ),
                    size = Size(block, block),
                )
            }
        }

        Text(
            text = "+${effect.label}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (effect.position.x - 40.dp.toPx()).roundToInt(),
                        y = (effect.position.y - 24.dp.toPx() - progress.value * 90.dp.toPx())
                            .roundToInt(),
                    )
                }
                .alpha(1f - progress.value * progress.value),
        )
    }
}

private const val SHOCKWAVE_BLOCKS = 14
private const val TWO_PI = 6.2831855f

/**
 * Width at which the screen puts the body and the shop next to each other.
 *
 * Six hundred density-independent pixels is where Android itself draws the line between a phone
 * and something larger, and it is also roughly where a stacked layout starts giving the shop a
 * window two rows tall.
 */
private val WIDE_THRESHOLD = 600.dp

/**
 * How much larger the tap target is than the body drawn inside it.
 *
 * A tenth. Enough that the edge of a planet is not a trap, small enough that the empty half of
 * the screen stays empty — which it has to, because one achievement depends on somebody being
 * able to tap it on purpose.
 */
private const val HIT_FORGIVENESS = 1.1f
