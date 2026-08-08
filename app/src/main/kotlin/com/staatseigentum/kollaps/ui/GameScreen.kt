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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.OfflineReport
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.audio.Mood
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Space
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Everything the screen can ask the game to do. */
interface GameActions {
    /** Returns the mass the tap produced, for the floating number. */
    fun tap(): Double
    fun setBuyAmount(amount: BuyAmount)
    fun buyCollector(id: String)
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

    fun bigBang()
    fun buyAeonUpgrade(id: String)

    fun startChallenge(id: String)
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
    fun setAutoBuy(on: Boolean)
    fun setReminders(on: Boolean)

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

    val collapsesAtStart = remember { state.collapses }
    LaunchedEffect(state.collapses) {
        if (state.collapses > collapsesAtStart) {
            blast = state.collapses to BlastKind.KOLLAPS
            blastSfx?.success()
        }
    }

    val bangsAtStart = remember { state.bigBangs }
    LaunchedEffect(state.bigBangs) {
        if (state.bigBangs > bangsAtStart) {
            blast = -state.bigBangs to BlastKind.URKNALL
            blastSfx?.success()
        }
    }

    CompositionLocalProvider(LocalSfx provides sfx.takeIf { state.soundOn }) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Space),
        ) {
            Starfield(
                tint = Color(stats.tier.glowColor),
                depth = stats.tier.index / (Tiers.all.size - 1f).coerceAtLeast(1f),
                modifier = Modifier.fillMaxSize(),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            ) {
                val body = @Composable { modifier: Modifier ->
                    TapArea(state = state, stats = stats, actions = actions, modifier = modifier)
                }
                val shop = @Composable { modifier: Modifier ->
                    ShopPanel(
                        state = state,
                        stats = stats,
                        buyAmount = buyAmount,
                        actions = actions,
                        modifier = modifier,
                        startTab = startTab,
                        startSection = startSection,
                        updateSection = updateSection,
                    )
                }

                // Side by side once there is room for it. Stacked, the body gets a strip of a
                // landscape screen and the shop scrolls a line at a time; the tap area is the
                // thing that wants height, and in a row it can have all of it.
                if (maxWidth >= WIDE_THRESHOLD && maxWidth > maxHeight) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Header(state = state, stats = stats)
                            body(Modifier.fillMaxWidth().weight(1f))
                        }
                        shop(Modifier.fillMaxHeight().weight(1f))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Header(state = state, stats = stats)
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

            state.event?.let { event ->
                EventDialog(
                    event = event,
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

            updateDialog()
        }
    }
}

// ---------------------------------------------------------------------- header

@Composable
private fun Header(state: GameState, stats: Stats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        )

        Text(
            text = "${Numbers.formatRate(stats.massPerSecond)}  ·  ${Numbers.format(stats.massPerTap)} pro Tipp",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
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

        Spacer(Modifier.height(12.dp))

        val next = stats.nextTier
        Text(
            text = if (next != null) {
                "${stats.tier.name} > ${next.name}"
            } else {
                "${stats.tier.name} — das Ende der Leiter"
            },
            style = MaterialTheme.typography.titleLarge,
            color = Color(stats.tier.glowColor),
        )

        Spacer(Modifier.height(6.dp))

        PixelBar(
            progress = stats.tierProgress,
            color = Color(stats.tier.glowColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        )

        if (next != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "noch ${Numbers.formatMass(next.threshold - state.runMass)}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
    }
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
    val onTap: () -> Double = actions::tap
    val effects = remember { mutableStateListOf<TapEffect>() }
    var nextId by remember { mutableLongStateOf(0L) }
    val squash = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val sfx = LocalSfx.current

    // A ring thrown off the body every time it climbs a rung. Only upwards: a collapse drops the
    // tier by twenty-four steps at once and already has a blast of its own.
    val lastTier = remember { mutableIntStateOf(tier.index) }
    var pulse by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(tier.index) {
        if (tier.index > lastTier.intValue) pulse = tier.index
        lastTier.intValue = tier.index
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { position ->
                val gained = onTap()
                effects += TapEffect(nextId++, position, Numbers.format(gained))
                sfx?.click()
                if (state.hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        CelestialBody(
            tier = tier,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = squash.value
                    scaleY = squash.value
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
