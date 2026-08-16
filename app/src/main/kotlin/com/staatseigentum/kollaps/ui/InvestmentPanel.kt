package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Investment
import com.staatseigentum.kollaps.core.Investments
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/** How many levels one tap buys. Zero stands for "as many as are affordable". */
private val STEPS = listOf(1, 5, 25, 0)

private fun stepLabel(step: Int): String = if (step <= 0) "Max" else "×$step"

/**
 * The bottomless end of the prestige shop.
 *
 * The thirteen upgrades above this are a list that runs out; these twelve never do. That is the
 * whole reason they exist — a currency the collapse loop keeps producing needs somewhere to go
 * after the shopping list is ticked off, or the loop stops meaning anything.
 *
 * Each row shows the level owned against its ceiling and what one more costs. The ceiling is not
 * decoration: several of these run into something real — an offline share cannot pass everything,
 * a milestone bonus stops being interesting — and a row that silently accepted money forever would
 * be selling nothing.
 */
@Composable
fun InvestmentPanel(
    state: GameState,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    val offered = Investments.offered(state)
    if (offered.isEmpty()) return

    var step by rememberSaveable { mutableIntStateOf(STEPS.first()) }

    PixelPanel(
        modifier = modifier.fillMaxWidth().sog(SogDepth.CONTAINER, Nebula),
        border = Nebula,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelLabel("Investitionen", color = Nebula, size = 15)
            PixelLabel(Numbers.format(state.singularities), color = Ember, size = 13)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = Lang.t("Jede Stufe kostet mehr als die davor. Es gibt kein Ende der Liste — nur einen Preis, bei dem du aufhörst."),
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            STEPS.forEach { option ->
                PixelButton(
                    label = stepLabel(option),
                    onClick = { step = option },
                    modifier = Modifier.weight(1f),
                    accent = if (step == option) Ember else Outline,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        for (investment in offered) {
            InvestmentRow(
                state = state,
                investment = investment,
                step = step,
                onBuy = { actions.buyInvestment(investment.id, step) },
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun InvestmentRow(
    state: GameState,
    investment: Investment,
    step: Int,
    onBuy: () -> Unit,
) {
    val level = Investments.levelOf(state, investment)
    val maxed = level >= investment.maxLevel

    // What this tap would actually buy, which is not what the button says when the ceiling or the
    // singularities in hand are the smaller number.
    val wanted = remember(state, investment, step, level) {
        if (step <= 0) {
            investment.affordableLevels(level, state.singularities)
        } else {
            step.coerceAtMost(investment.maxLevel - level)
        }
    }
    val cost = investment.costForLevels(level, wanted)
    val affordable = wanted > 0 && cost <= state.singularities
    val sfx = LocalSfx.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceCard)
            .border(2.dp, if (affordable) Positive else Outline, RectangleShape)
            .clickable(enabled = affordable) {
                sfx?.purchase()
                onBuy()
            }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (level > 0) SpaceElevated else SpaceCard)
                    .border(2.dp, Outline, RectangleShape),
                contentAlignment = Alignment.Center,
            ) {
                PixelLabel(
                    text = level.toString(),
                    color = if (level > 0) Ember else Muted,
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = investment.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Starlight,
                )
                Text(
                    text = investment.effectText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ember,
                )
                Text(
                    text = if (level > 0) {
                        Lang.t("Stufe %s von %s", level, investment.maxLevel)
                    } else {
                        investment.flavor
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (maxed) {
                    PixelLabel(text = "voll", color = Positive, size = 10)
                } else {
                    Text(
                        text = Numbers.format(cost.coerceAtLeast(investment.costAt(level))),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (affordable) Positive else Muted,
                    )
                    if (wanted > 1) {
                        Text(
                            text = "+$wanted",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted,
                        )
                    }
                }
            }
        }
    }
}
