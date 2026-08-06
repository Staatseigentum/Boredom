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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staatseigentum.kollaps.GameViewModel
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.update.UpdateViewModel
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Space
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameScreen(model: GameViewModel, updateModel: UpdateViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val stats by model.stats.collectAsStateWithLifecycle()
    val buyAmount by model.buyAmount.collectAsStateWithLifecycle()
    val offlineReport by model.offlineReport.collectAsStateWithLifecycle()
    val updatePrompt by updateModel.prompt.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { updateModel.checkOnLaunch() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Space),
    ) {
        Starfield(
            tint = Color(stats.tier.glowColor),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Header(state = state, stats = stats)

            TapArea(
                tier = stats.tier,
                onTap = model::tap,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            ShopPanel(
                state = state,
                stats = stats,
                buyAmount = buyAmount,
                onBuyAmount = model::setBuyAmount,
                onBuyCollector = model::buyCollector,
                onBuyUpgrade = model::buyUpgrade,
                onCollapse = model::collapse,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f),
                updateSection = { UpdateCard(updateModel) },
            )
        }

        offlineReport?.let { report ->
            OfflineDialog(report = report, onDismiss = model::dismissOfflineReport)
        }

        if (GameEngine.hasUncelebratedTier(state)) {
            TierCelebration(tier = stats.tier, onDismiss = model::acknowledgeTier)
        }

        if (updatePrompt) {
            UpdateDialog(updateModel)
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
                    text = "◍ ${Numbers.format(state.singularities)} Singularitäten",
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

        Spacer(Modifier.height(12.dp))

        val next = stats.nextTier
        Text(
            text = if (next != null) {
                "${stats.tier.name} → ${next.name}"
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
    tier: CelestialTier,
    onTap: () -> Double,
    modifier: Modifier = Modifier,
) {
    val effects = remember { mutableStateListOf<TapEffect>() }
    var nextId by remember { mutableLongStateOf(0L) }
    val squash = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { position ->
                val gained = onTap()
                effects += TapEffect(nextId++, position, Numbers.format(gained))
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

        for (effect in effects) {
            key(effect.id) {
                TapFeedback(
                    effect = effect,
                    color = Color(tier.glowColor),
                    onFinished = { effects.remove(effect) },
                )
            }
        }
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
