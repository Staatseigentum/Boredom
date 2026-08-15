package com.staatseigentum.kollaps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.staatseigentum.kollaps.core.Designations
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Heat
import com.staatseigentum.kollaps.core.NumberFormat
import com.staatseigentum.kollaps.core.Multiverse
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.Wallclock
import com.staatseigentum.kollaps.core.audio.Mood
import com.staatseigentum.kollaps.core.pixel.Skins
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Space
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
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

    /** Welds two heavy elements into an alloy. See [com.staatseigentum.kollaps.core.Alloy]. */
    fun forgeAlloy(id: String)

    /** Puts a parked galaxy on a job. See [com.staatseigentum.kollaps.core.GalaxyJob]. */
    fun assignGalaxy(slot: Int, jobId: String)

    /** Welds two galaxies into one, freeing a slot. */
    fun mergeGalaxies(keepSlot: Int, absorbSlot: Int)

    /** Builds one level onto a parked galaxy, paid in Äonen. */
    fun developGalaxy(slot: Int)

    /** Drops into the universe parked in a galaxy, for an hour of play. */
    fun visitGalaxy(slot: Int)

    /** Puts it back and returns to the newest universe. */
    fun leaveGalaxy()

    /** Answers the catalogue find on the table. See [com.staatseigentum.kollaps.core.FindAnswer]. */
    fun answerFind(answerId: String)

    /** Hands a finished contract in. See [com.staatseigentum.kollaps.core.Contract]. */
    fun claimContract(id: String)

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
     * The big bang, as a picture, on the same terms and none of the same movements.
     *
     * A second sequence rather than a mode of the first, and deliberately: the two resets sit in
     * the same tab, and if they shared a class they would end up sharing a shape. The collapse
     * spirals inward and never stops; this one presses the whole screen flat, squeezes the line to
     * a pixel, and then does nothing at all for half a second before it detonates.
     */
    val bigBang = remember { BigBangSequence() }

    /*
     * Recorded here in the composition rather than from an effect, because effects run on the frame
     * that already carries the reset state — anything written from one would be the new run, which
     * is precisely the screen the sequence is not supposed to show. While the counter still agrees
     * this is simply the current screen; the frame it stops agreeing, the sequence is about to take
     * over and this is what it inherits. Recording resumes when the sequence hands it back.
     */
    val resets = state.collapses + state.bigBangs
    val held = remember { HeldScreen(state, stats, resets) }
    if (resets == held.resets) {
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
    val showingOld by remember(collapse, bigBang) {
        derivedStateOf { collapse.showingOld || bigBang.showingOld }
    }
    val shownState = if (showingOld) held.state else state
    val shownStats = if (showingOld) held.stats else stats

    val collapsesAtStart = remember { state.collapses }
    LaunchedEffect(state.collapses) {
        if (state.collapses <= collapsesAtStart) return@LaunchedEffect

        val detonate: () -> Unit = {
            blast = state.collapses to BlastKind.KOLLAPS
            blastSfx?.explosion()
        }

        // Straight to the bang when the system has been told to keep still. Not a shortened
        // version of the same thing — somebody who switched animations off does not want a
        // politer spiral, they want it over with.
        if (reduceMotion) {
            detonate()
            held.resets = resets
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
            held.resets = resets
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
        if (state.bigBangs <= bangsAtStart) return@LaunchedEffect

        val detonate: () -> Unit = {
            blast = -state.bigBangs to BlastKind.URKNALL
            blastSfx?.explosion()
        }

        // The whine that runs under the flattening, fired once. It stops of its own accord at the
        // moment the line becomes a point — and the four hundred and sixty milliseconds of nothing
        // that follow are the reason the bang after them lands.
        blastSfx?.flatten()

        actions.setPaused(true)
        try {
            bigBang.run(detonate)
        } finally {
            actions.setPaused(false)
            held.resets = resets
        }
    }

    // The palette is settled once, here, so every body on screen agrees on it — and it is
    // resolved rather than taken raw, so a scheme that is not actually earned falls back. Off the
    // shown state, so the body being pulled in keeps the colours it had.
    val skin = Skins.current(shownState.skinId, shownState.achievements)

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
        LocalBigBang provides bigBang,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Space)
                // How wide the screen is and where its middle is, which is what the line the
                // interface becomes is measured against.
                .bigBangStage()
                /*
                 * The shake, on everything at once.
                 *
                 * Applied out here rather than per element so the sky, the interface and the
                 * canvas move as one picture — a screen where only some of it shook would read
                 * as a rendering fault rather than as an impact. The offset is already snapped
                 * to the block grid inside the sequence: a smooth shake would blur every hard
                 * edge on screen for half a second, which is the whole style undone.
                 */
                .graphicsLayer {
                    translationX = bigBang.shake.x
                    translationY = bigBang.shake.y
                },
        ) {
            Starfield(
                tint = Color(shownStats.tier.glowColor),
                // Capped at one: the starfield's depth is how far up the named ladder the player
                // is, and the catalogue rungs above it would drive it far past its own range.
                depth = (shownStats.tier.index / (Tiers.all.size - 1f).coerceAtLeast(1f)).coerceAtMost(1f),
                warp = { collapse.warp },
                warpCentre = { collapse.centre },
                crush = { bigBang.skyCrush },
                pinch = { bigBang.skyPinch },
                birth = { bigBang.skyBirth },
                modifier = Modifier.fillMaxSize(),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                val body = @Composable { modifier: Modifier, phone: Boolean ->
                    TapArea(
                        state = shownState,
                        stats = shownStats,
                        actions = actions,
                        buyAmount = buyAmount,
                        // On a phone this area carries what the status band above it gave up: the
                        // running chips along the top, and the one purchase worth not switching
                        // screens for along the bottom. The wide layout has a HUD for the first
                        // and a whole column for the second.
                        phone = phone,
                        // The one thing that does not move: it is what everything else moves
                        // towards, and the sprite in the middle of it is the hole itself — and,
                        // for the big bang, the line the universe is pressed onto.
                        modifier = modifier.collapseCentre().bigBangCentre(),
                    )
                }
                val shop = @Composable { modifier: Modifier, pinned: ShopTab? ->
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
                        pinned = pinned,
                    )
                }

                // Side by side once there is room for it. Stacked, the body gets a strip of a
                // landscape screen and the shop scrolls a line at a time; the tap area is the
                // thing that wants height, and in a row it can have all of it.
                if (maxWidth >= WIDE_THRESHOLD && maxWidth > maxHeight) {
                    /*
                     * Three columns under a band.
                     *
                     * The wide layout used to be the phone's column with a shop bolted to the side:
                     * a centred header stacked over the body on the left, the whole narrow shop on
                     * the right. It worked, and it spent a wide screen on two things — the header
                     * filled a fifth of the height to say nine short facts, and the ladder the
                     * whole game climbs was a progress bar.
                     *
                     * So the header lies down into a band, the space it frees becomes a column for
                     * the ladder, and the shop stops hiding the fleet behind a tab.
                     */
                    Column(modifier = Modifier.fillMaxSize()) {
                        HudBand(state = shownState, stats = shownStats)
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            LadderColumn(
                                state = shownState,
                                stats = shownStats,
                                modifier = Modifier.width(LADDER_WIDTH).fillMaxHeight(),
                            )
                            Box(Modifier.width(2.dp).fillMaxHeight().background(Outline))
                            // The body keeps the largest share: it is the thing being looked at.
                            body(Modifier.fillMaxHeight().weight(1.35f), false)
                            WideShop(
                                state = shownState,
                                stats = shownStats,
                                buyAmount = buyAmount,
                                actions = actions,
                                startSection = startSection,
                                updateSection = updateSection,
                                saveSlots = saveSlots,
                                modifier = Modifier.fillMaxHeight().weight(1.15f),
                            )
                        }
                    }
                } else {
                    /*
                     * One thing at a time.
                     *
                     * The old phone layout stacked three zones and let them fight over the height:
                     * a header, the body, and the shop, each getting a third of a screen that was
                     * never big enough for one of them. The body ended up a strip too small to
                     * aim at and the shop showed two and a half rows of a list twenty long.
                     * Squeezing the header helped and did not fix it, because the problem was
                     * never density — it was that a phone was being asked to be two screens.
                     *
                     * So it is two screens. The body gets all of the height when you are tapping
                     * it, the shop gets all of it when you are building, and a bar along the
                     * bottom — where the thumb already is — says which. The status stays on top
                     * of both, because the mass is the one number you want while doing either.
                     */
                    /*
                     * Which area the phone is on.
                     *
                     * Seeded from [startTab], which is how the screenshot harness reaches anything
                     * but the body — and that stopped working silently when the bottom bar took
                     * over from the shop's tab strip. `startTab` still went to the strip, the strip
                     * was no longer what chose the screen, and so every phone screenshot in the
                     * build quietly became another photograph of the same body. Zero is the body,
                     * which is where a player starts, so nothing changes for anybody else.
                     */
                    var view by rememberSaveable {
                        mutableStateOf(PhoneView.entries.getOrElse(startTab) { PhoneView.BODY }.name)
                    }
                    val available = PhoneView.availableIn(shownState)
                    val current = PhoneView.entries.firstOrNull { it.name == view }
                        ?.takeIf { it in available }
                        ?: PhoneView.BODY

                    // A collapse or a big bang pulls the whole interface into the body, and
                    // watching that happen from another area would be watching the wrong half.
                    LaunchedEffect(collapse.running, bigBang.running) {
                        if (collapse.running || bigBang.running) view = PhoneView.BODY.name
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        StatusBand(state = shownState, stats = shownStats)

                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            when (current) {
                                PhoneView.BODY -> body(Modifier.fillMaxSize(), true)
                                PhoneView.FLEET -> shop(Modifier.fillMaxSize(), ShopTab.COLLECTORS)
                                PhoneView.ORBITS -> shop(Modifier.fillMaxSize(), ShopTab.ORBITS)
                                PhoneView.FUSION -> shop(Modifier.fillMaxSize(), ShopTab.FUSION)
                                PhoneView.COSMOS -> shop(Modifier.fillMaxSize(), ShopTab.COSMOS)
                            }
                        }

                        PhoneNav(
                            state = shownState,
                            stats = shownStats,
                            current = current,
                            onSelect = { view = it.name },
                        )
                    }
                }
            }

            /*
             * Nothing pops up over a sequence.
             *
             * The new run climbs the first rungs of the ladder within a second of a collapse — the
             * starting mass alone is enough — so the tier celebration was landing on top of the
             * screen being pulled into the hole, over a body it was not describing. The same goes
             * for an event coming due and for an offline report on a save reopened mid-animation.
             *
             * Held rather than dropped: `hasUncelebratedTier` stays true, the prompt stays in the
             * state and the report stays in its flow, so all three arrive the moment the picture
             * is over. Deferring is the whole fix; none of them is worth skipping.
             */
            val quiet by remember(collapse, bigBang) {
                derivedStateOf { collapse.running || bigBang.running }
            }

            if (!quiet) {
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

                // After the event, so the two never stack: a find waits on the table until it is
                // answered and can afford to wait one more question.
                if (state.prompt == null && state.pendingFind != null) {
                    FindDialog(state = state, onAnswer = actions::answerFind)
                }
            }

            // Over everything, including the dialogs: the blast is the loudest thing that can
            // happen, and something covering half of it would read as a glitch.
            blast?.let { (trigger, kind) ->
                Blast(
                    trigger = trigger,
                    kind = kind,
                    /*
                     * From the body, not from the middle of the window.
                     *
                     * The big bang had its own point from the start — it is squeezed onto one, so
                     * there was no way to write it without. The collapse quietly fell through to
                     * `Unspecified`, which the blast reads as "the middle of whatever you are
                     * drawn in", and on a phone the body sits near the middle anyway, so nothing
                     * looked wrong. In the two-column window the body is on the left and the
                     * explosion went off over the shop.
                     *
                     * The collapse has been tracking the body's centre all along — `collapseCentre`
                     * records it every time the layout settles, because the whole sequence falls
                     * towards that point. It simply was never handed on.
                     */
                    centre = when {
                        kind == BlastKind.URKNALL && bigBang.running -> bigBang.centre
                        collapse.centre != Offset.Zero -> collapse.centre
                        // Before the body has ever been laid out there is nothing to aim at, and
                        // the middle is a better guess than the top left corner.
                        else -> Offset.Unspecified
                    },
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
            val blocking by remember(collapse, bigBang) {
                derivedStateOf { collapse.running || bigBang.running }
            }
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

            // The big bang's own layer: the shards on the way in, the line, the point, and the
            // matter thrown back out. One canvas, because they are drawn in that order.
            BigBangCanvas(sequence = bigBang, modifier = Modifier.fillMaxSize())

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
private class HeldScreen(var state: GameState, var stats: Stats, var resets: Int)

// ---------------------------------------------------------------------- header

@Composable
private fun Header(state: GameState, stats: Stats, compact: Boolean) {
    /*
     * Seven rows became four.
     *
     * The header had grown a line at a time — rung, singularities, mass, rate, per tap, lab, tier
     * name, bar, remaining — and every one of them was justified on its own. Together they filled
     * half of a phone before anything happened, and the thing the screen is actually about, the
     * body, was pushed into a strip.
     *
     * Nothing is deleted. What changes on a narrow screen is that lines which belong together
     * share a row: the rung sits with the singularities, and what is left to the next body sits
     * on the same line as which body that is. Both pairs were always one thought printed twice.
     */
    val gap = if (compact) 4.dp else 6.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = if (compact) 8.dp else 12.dp)
            // The lines inside go first and go into this column; a second later the column itself
            // goes into the hole, carrying whatever is left of them.
            .sog(SogDepth.SHELL, Ember),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sog(SogDepth.CONTENT, Muted)
                .urknall(Muted),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                // On the catalogue ladder the fraction is meaningless — "Stufe 3471/16925" is a
                // number, not a position — so the designation stands in its place.
                text = if (stats.tier.isDesignated) {
                    "Katalog ${stats.tier.label}"
                } else {
                    "Stufe ${stats.tier.index + 1}/${Tiers.all.size}"
                },
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

        Spacer(Modifier.height(gap))

        Text(
            text = Numbers.formatMass(state.mass),
            // A rung smaller on a phone: "1,43 Qua kg" in displayMedium wraps on a narrow screen,
            // and a headline that wraps is two rows pretending to be one.
            style = if (compact) {
                MaterialTheme.typography.displaySmall
            } else {
                MaterialTheme.typography.displayMedium
            },
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.sog(SogDepth.CONTENT, Starlight).urknall(Starlight),
        )

        Text(
            text = "${Numbers.formatRate(stats.massPerSecond)}  ·  ${Numbers.format(stats.massPerTap)} pro Tipp",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            modifier = Modifier.sog(SogDepth.CONTENT, Muted).urknall(Muted),
        )

        val buff = stats.buff
        if (buff != null) {
            Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
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

        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

        val next = stats.nextTier
        val glow = Color(stats.tier.glowColor)
        val climb = when {
            next != null -> "${stats.tier.label} > ${next.label}"
            // Above the gate with the catalogue still shut. Not the end of anything — there are
            // sixteen thousand rungs over this one — and saying "das Ende der Leiter" to somebody
            // fifty orders of magnitude past it is the interface calling a locked door a wall. The
            // line names what is missing instead, because that is the one thing worth knowing here.
            !Designations.isUnlocked(state) ->
                "${stats.tier.label} · Katalog ab ${Multiverse.SLOTS} Galaxien " +
                    "(${Multiverse.count(state)})"

            else -> "${stats.tier.label} — das Ende der Leiter"
        }
        val remaining = next?.let { "noch ${Numbers.formatMass(it.threshold - state.runMass)}" }

        if (compact) {
            // Where you are and how far to the next one, on one line with the bar under it.
            // Two facts about the same climb do not need two rows and a spacer between them.
            Row(
                modifier = Modifier.fillMaxWidth().sog(SogDepth.CONTENT, glow).urknall(glow),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = climb,
                    style = MaterialTheme.typography.titleMedium,
                    color = glow,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (remaining != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = remaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            Spacer(Modifier.height(gap))
            PixelBar(
                progress = stats.tierProgress,
                color = glow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .sog(SogDepth.CONTENT, glow)
                    .urknall(Ember),
            )
        } else {
            Text(
                text = climb,
                style = MaterialTheme.typography.titleLarge,
                color = glow,
                modifier = Modifier.sog(SogDepth.CONTENT, glow).urknall(glow),
            )

            Spacer(Modifier.height(6.dp))

            PixelBar(
                progress = stats.tierProgress,
                color = glow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .sog(SogDepth.CONTENT, glow)
                    .urknall(Ember),
            )

            if (remaining != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = remaining,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.sog(SogDepth.CONTENT, Muted).urknall(Muted),
                )
            }
        }
    }
}

/** One line under the header while a project is running, counting down on the wall clock. */
@Composable
private fun ResearchTicker(state: GameState) {
    val running = ResearchTree.active(state) ?: return

    var now by remember { mutableLongStateOf(Wallclock.millis()) }
    LaunchedEffect(running.id) {
        while (true) {
            now = Wallclock.millis()
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
    buyAmount: BuyAmount,
    /** Whether this is the phone's body screen, which carries two things the wide one does not. */
    phone: Boolean,
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

    /*
     * Whether the player has asked the system to move less, read the same way as everything else
     * the handler needs: fresh, not captured on the first composition.
     *
     * The rule this file follows for it: *information stays, decoration goes*. The floating number
     * says what a tap was worth and is the only feedback that a tap paid anything at all, so it
     * remains — it simply stops travelling. The shock rings and the squash say nothing the number
     * does not, so under this setting they do not happen.
     *
     * Not a gentler version of them. Somebody who switches this on is not asking for a smaller
     * bounce, which is the same mistake the collapse sequence already refuses to make.
     */
    val quiet by rememberUpdatedState(LocalReduceMotion.current)

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
                if (quiet) return@detectTapGestures
                scope.launch {
                    /*
                     * A stepped squash-and-stretch rather than a spring.
                     *
                     * The spring was the obvious choice and the wrong one for this art: it settles
                     * through a continuum of scales, and a pixel sprite drawn at 0.973 of its size
                     * is a sprite with soft edges. Every frame of it looked slightly blurred, which
                     * is the entire style undone once per tap.
                     *
                     * This runs the same shape — squash, overshoot, settle — as four held poses
                     * over 260 ms. Nothing in between is ever drawn, so every frame is the sprite
                     * at a scale it looks right at.
                     */
                    squash.snapTo(1f)
                    squash.animateTo(0.90f, tween(SQUASH_MILLIS * 12 / 100, easing = LinearEasing))
                    squash.animateTo(1.05f, tween(SQUASH_MILLIS * 28 / 100, easing = LinearEasing))
                    squash.animateTo(1f, tween(SQUASH_MILLIS * 60 / 100, easing = LinearEasing))
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        // The body is the one thing the collapse does not pull anywhere — it swells as it feeds,
        // is crushed to nothing, and comes back as the meteorite of the new run. Its share of the
        // sequence is read straight off the clock rather than going through [sog].
        val collapse = LocalCollapse.current
        val bigBang = LocalBigBang.current
        // Under the body, so the far half of every orbit passes behind the planet instead of
        // over it. That one ordering is most of the difference between a system and a sticker.
        OrbitingBodies(
            state = state,
            tier = tier,
            side = OrbitSide.BEHIND,
            modifier = Modifier.fillMaxSize(),
        )
        Satellites(
            state = state,
            tier = tier,
            side = OrbitSide.BEHIND,
            modifier = Modifier.fillMaxSize(),
        )

        CelestialBody(
            tier = tier,
            // Only one of the two can be running, so the sum is whichever it is.
            extraTurns = { (collapse?.extraSpin ?: 0f) + (bigBang?.extraSpin ?: 0f) },
            scale = BODY_SCALE_IN_FIELD,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val swell = when {
                        bigBang?.running == true -> bigBang.bodyScale
                        collapse != null -> collapse.bodyScale
                        else -> 1f
                    }
                    // Snapped on the way out, so the tween between the held poses above is never
                    // actually drawn — see the comment where the squash is started.
                    val held = quantise(squash.value, SQUASH_STEPS)
                    scaleX = held * swell
                    scaleY = held * swell
                    alpha = when {
                        bigBang?.running == true -> bigBang.bodyAlpha
                        collapse != null -> collapse.bodyAlpha
                        else -> 1f
                    }
                },
        )

        TierPulse(
            trigger = pulse,
            color = Color(tier.glowColor),
            modifier = Modifier.fillMaxSize(),
        )

        // The real system, under the collector rings: these are bodies with mass and a tier,
        // the rings above them are a picture of how many machines are in the shop. This pass is
        // the near half of both; the far half was drawn under the body above.
        OrbitingBodies(
            state = state,
            tier = tier,
            side = OrbitSide.INFRONT,
            modifier = Modifier.fillMaxSize(),
        )
        Satellites(
            state = state,
            tier = tier,
            side = OrbitSide.INFRONT,
            modifier = Modifier.fillMaxSize(),
        )

        for (effect in effects) {
            key(effect.id) {
                TapFeedback(
                    effect = effect,
                    color = Color(tier.glowColor),
                    onFinished = { effects.remove(effect) },
                )
            }
        }

        // Along the top, where nothing else is: the band sits above this box, not in it.
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // What is running, on a phone. Here rather than in the band above because the band has
            // to stay exactly three lines tall however much is going on at once, and these come and
            // go — a chip that is absent costs nothing, a row that is empty costs a row.
            if (phone) {
                StatusMarks(state = state, stats = stats, size = 9)
                Spacer(Modifier.height(6.dp))
            }
            AchievementToast(
                earned = state.achievements,
                // Collected as normal, shown afterwards: a collapse earns two or three of these at
                // once and a card sliding in over the explosion is the same mistake as a dialog.
                hold = collapse?.running == true || bigBang?.running == true,
            )
        }

        // Along the bottom of the body, out of the way of the thumb that is tapping it. Both in
        // one column, because on a phone they would otherwise sit on top of each other in the
        // first five minutes, which is exactly when both have something to say.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TutorialHint(state = state, onDismiss = actions::dismissTutorial)
            HeatMeter(heat = state.heat)
            // Underneath both, so neither ever has to move around it.
            if (phone) {
                Spacer(Modifier.height(10.dp))
                NextBuyRow(
                    state = state,
                    buyAmount = buyAmount,
                    onBuy = actions::buyCollector,
                )
            }
        }

        // Last, so a comet is never covered by the body it drifts past.
        CometOverlay(
            state = state,
            frequency = GameEngine.cometFrequency(state),
            onCatch = actions::catchComet,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * What a tap looks like.
 *
 * Two rings rather than one, and every part of it stepped.
 *
 * ## Why two
 *
 * One ring expanding out of a point reads as a circle growing. Two, offset by ninety milliseconds
 * and with the second one thinner and starting further out, read as an impact — the eye gets a
 * front and a wake, which is the difference between "something is drawing a circle" and "something
 * was hit". It costs one more ring's worth of rectangles.
 *
 * ## Why stepped
 *
 * Everything else on this screen is made of hard blocks that sit on whole pixels. A ring that
 * travels outwards continuously is the one smoothly moving thing in the picture, and next to
 * sprites it reads as a rendering artefact rather than as motion. Quantising the progress into a
 * dozen steps makes it move the way the rest of the game does: in visible increments.
 *
 * The animation itself is still a plain linear tween — it is the *value read out of it* that is
 * snapped, which keeps the timing exact and the positions on the grid.
 */
@Composable
private fun TapFeedback(effect: TapEffect, color: Color, onFinished: () -> Unit) {
    val quiet = LocalReduceMotion.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(effect.id) {
        progress.animateTo(1f, animationSpec = tween(900, easing = LinearOutSlowInEasing))
        onFinished()
    }

    // Its own full-size box, so the offsets below are measured from the top left of the tap
    // area and not from the centre where the planet sits.
    Box(modifier = Modifier.fillMaxSize()) {
        // Decoration, and the first thing to go when the player has asked for less movement.
        if (!quiet) Canvas(modifier = Modifier.fillMaxSize()) {
            // The leading ring is done at 620 ms of the 900; the trailing one starts 90 ms in and
            // runs to 790. Both are read off the same clock so they can never drift apart.
            ring(
                effect = effect,
                color = color,
                progress = stepped(progress.value * 900f, from = 0f, to = 620f),
                startRadius = 12.dp.toPx(),
                reach = 48.dp.toPx(),
                block = 6.dp.toPx(),
            )
            ring(
                effect = effect,
                color = color,
                progress = stepped(progress.value * 900f, from = 90f, to = 790f),
                startRadius = 22.dp.toPx(),
                reach = 54.dp.toPx(),
                block = 4.dp.toPx(),
            )
        }

        // The number climbs in fourteen steps and fades in over the first fifteen per cent, so it
        // arrives rather than being simply present. It starts a little below the tap and never
        // overshoots — a number that springs past its mark reads as a different number.
        // Still there, still fading, but it no longer travels: the reading is the information, the
        // journey up the screen is the decoration.
        val climb = if (quiet) 0f else quantise(progress.value, TAP_NUMBER_STEPS)
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
                        y = (effect.position.y - 20.dp.toPx() - climb * 54.dp.toPx()).roundToInt(),
                    )
                }
                .alpha(
                    if (progress.value < 0.15f) {
                        progress.value / 0.15f
                    } else {
                        1f - ((progress.value - 0.15f) / 0.85f)
                    },
                ),
        )
    }
}

/** One ring of blocks, at a progress already stepped by the caller. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.ring(
    effect: TapEffect,
    color: Color,
    progress: Float,
    startRadius: Float,
    reach: Float,
    block: Float,
) {
    if (progress <= 0f || progress >= 1f) return
    val radius = startRadius + progress * reach
    val edge = block * (1f - progress * 0.5f)
    val alpha = (1f - progress) * 0.9f
    for (index in 0 until SHOCKWAVE_BLOCKS) {
        val angle = index.toFloat() / SHOCKWAVE_BLOCKS * TWO_PI
        drawRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(
                effect.position.x + cos(angle) * radius - edge / 2f,
                effect.position.y + sin(angle) * radius - edge / 2f,
            ),
            size = Size(edge, edge),
        )
    }
}

/**
 * A window of the tap's clock, in [TAP_RING_STEPS] steps.
 *
 * Returns 0 before the window and 1 after it, so a ring that has not started and one that has
 * finished are both simply absent.
 */
private fun stepped(millis: Float, from: Float, to: Float): Float {
    val raw = ((millis - from) / (to - from)).coerceIn(0f, 1f)
    return quantise(raw, TAP_RING_STEPS)
}

/**
 * Snaps a 0..1 progress to [steps] discrete values.
 *
 * The whole of part four goes through here. Compose has no stepped easing, and writing one per
 * animation is how five animations end up stepping by slightly different amounts.
 */
internal fun quantise(value: Float, steps: Int): Float =
    (value * steps).toInt().toFloat() / steps

/**
 * What tapping is worth right now, shown only while it is worth anything.
 *
 * Next to the body rather than up in the header, and for the reason the whole feature exists: this
 * is feedback on something the thumb is doing, and feedback belongs where the thumb is. The header
 * is also the part of the screen with the least room left on a phone.
 *
 * Absent at zero rather than empty. A bar showing nought per cent is a permanent reminder of a
 * bonus you are not getting, which is the opposite of the point.
 */
@Composable
private fun HeatMeter(heat: Double) {
    if (heat <= 0.0) return

    Spacer(Modifier.height(6.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PixelLabel(
            text = "Überhitzt " + Numbers.formatMultiplier(Heat.factor(heat)),
            color = Ember,
            size = 11,
        )
        Spacer(Modifier.height(3.dp))
        PixelBar(
            progress = heat.toFloat(),
            color = Ember,
            cells = 12,
            modifier = Modifier
                .width(120.dp)
                .height(6.dp),
        )
    }
}

private const val SHOCKWAVE_BLOCKS = 14
private const val TWO_PI = 6.2831855f

/** Steps a tap's rings travel outwards in. Twelve reads as motion; smooth reads as a smear. */
private const val TAP_RING_STEPS = 12

/** And the steps the floating number climbs in. */
private const val TAP_NUMBER_STEPS = 14

/** How long the whole squash takes, split 12 / 28 / 60 between its three moves. */
private const val SQUASH_MILLIS = 260

/**
 * Scales the body is ever actually drawn at, between 0.90 and 1.05.
 *
 * Twenty-four across the whole 0..1 range works out to about four distinct scales inside the range
 * the squash uses, which is what a hand-drawn squash would have had.
 */
private const val SQUASH_STEPS = 24

/**
 * Width at which the screen puts the body and the shop next to each other.
 *
 * Six hundred density-independent pixels is where Android itself draws the line between a phone
 * and something larger, and it is also roughly where a stacked layout starts giving the shop a
 * window two rows tall.
 */
private val WIDE_THRESHOLD = 600.dp

/**
 * How wide the ladder column is.
 *
 * Wide enough for "Roter Überriese" at bodyMedium plus its square and the padding around it, and
 * not a pixel more — every dp here comes off the body in the middle.
 */
private val LADDER_WIDTH = 172.dp

/**
 * How much larger the tap target is than the body drawn inside it.
 *
 * A tenth. Enough that the edge of a planet is not a trap, small enough that the empty half of
 * the screen stays empty — which it has to, because one achievement depends on somebody being
 * able to tap it on purpose.
 */
private const val HIT_FORGIVENESS = 1.1f
