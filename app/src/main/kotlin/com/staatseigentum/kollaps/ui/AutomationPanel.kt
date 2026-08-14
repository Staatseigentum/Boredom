package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.Automation
import com.staatseigentum.kollaps.core.AutomationRule
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The standing orders.
 *
 * Every rule is one row and one tap: the tap walks the dial forward and off the end, so switching
 * a rule on, tuning it and switching it off are the same gesture. A toggle plus a picker would be
 * two controls for a thing that has three or four settings in total.
 */
@Composable
fun AutomationPanel(
    state: GameState,
    actions: GameActions,
    modifier: Modifier = Modifier,
) {
    if (!Automation.isUnlocked(state)) return

    PixelPanel(modifier = modifier.fillMaxWidth(), border = Positive) {
        PixelLabel(text = "Automatik", color = Positive, size = 16)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Jede Regel läuft für sich. Tippen schaltet weiter — nach der letzten " +
                "Einstellung wieder aus.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(10.dp))

        for (rule in AutomationRule.entries) {
            RuleRow(
                state = state,
                rule = rule,
                onCycle = { actions.cycleAutomation(rule.id) },
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun RuleRow(state: GameState, rule: AutomationRule, onCycle: () -> Unit) {
    val available = Automation.isAvailable(state, rule)
    val setting = if (available) Automation.settingOf(state, rule) else null
    val on = setting != null
    val sfx = LocalSfx.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceCard)
            .border(2.dp, if (on) Positive else Outline, RectangleShape)
            .clickable(enabled = available) {
                sfx?.click()
                onCycle()
            }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // A filled square for on, an empty frame for off. No animation: the row is a switch,
            // and a switch that slides is a switch you have to wait for.
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(if (on) Positive else SpaceElevated)
                    .border(2.dp, if (on) Positive else Outline, RectangleShape),
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (available) Starlight else Muted,
                )
                Text(
                    text = when {
                        !available -> lockedReason(rule)
                        on -> rule.flavor
                        else -> "aus"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            if (on) {
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = rule.setting,
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                    )
                    PixelLabel(
                        text = rule.optionAt(setting).label,
                        color = Ember,
                        size = 12,
                    )
                }
            }
        }
    }
}

/** Why a rule cannot be switched on yet, in the player's terms rather than the code's. */
private fun lockedReason(rule: AutomationRule): String = when (rule) {
    AutomationRule.COLLECTORS, AutomationRule.UPGRADES -> "noch nicht freigeschaltet"
    AutomationRule.FUSION -> "erst, wenn der Kern brennt"
    AutomationRule.ORBITS -> "erst mit dem eigenen System"
    AutomationRule.RESEARCH -> "erst mit dem Labor"
}
