package com.staatseigentum.kollaps.ui

import com.staatseigentum.kollaps.core.i18n.Lang
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight

/**
 * The band across the top of a wide screen: three cells, and everything the player glances up for.
 *
 * It replaces a centred header that was a column of nine rows. That header made sense on a phone,
 * where a column is the only shape there is — but on a wide screen it left two thirds of its own
 * width empty and still pushed the body down by a fifth of the window. The same nine facts fit in
 * three cells across, at twice the size, in a quarter of the height.
 *
 * The divisions are two-pixel rules rather than gaps, so the band reads as one instrument with
 * three dials rather than as three panels that happen to be adjacent.
 *
 * ## Why the marks are here
 *
 * The old header carried a buff panel and a research ticker, each of which was a row that existed
 * whether or not it had anything to say. As chips in the third cell they take space only while
 * something is running — which is what lets this band have a fixed height at all.
 */
@Composable
fun HudBand(state: GameState, stats: Stats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SpaceElevated)
            .sog(SogDepth.SHELL, Ember),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Cell(
                modifier = Modifier
                    .widthIn(min = 300.dp)
                    .sog(SogDepth.CONTENT, Starlight)
                    .urknall(Starlight),
            ) {
                PixelLabel(text = Lang.t("Masse"), color = Muted, size = 10)
                Spacer(Modifier.height(6.dp))
                // The largest thing on the screen that is not the body, which is right: it is the
                // number the whole game is about.
                PixelLabel(text = Numbers.formatMass(state.mass), color = Starlight, size = 34)
            }

            Divider()

            Cell(
                modifier = Modifier
                    .widthIn(min = 190.dp)
                    .sog(SogDepth.CONTENT, Positive)
                    .urknall(Positive),
            ) {
                PixelLabel(text = Lang.t("Produktion"), color = Muted, size = 10)
                Spacer(Modifier.height(6.dp))
                PixelLabel(text = Numbers.formatRate(stats.massPerSecond), color = Positive, size = 18)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Lang.t("%s pro Tipp", Numbers.format(stats.massPerTap)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            Divider()

            val glow = Color(stats.tier.glowColor)
            Cell(
                modifier = Modifier
                    .weight(1f)
                    .sog(SogDepth.CONTENT, glow)
                    .urknall(glow),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PixelLabel(
                        text = climbLine(state, stats),
                        color = glow,
                        size = 12,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = rungLine(state, stats),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                Spacer(Modifier.height(6.dp))
                PixelBar(
                    progress = stats.tierProgress,
                    color = glow,
                    modifier = Modifier.fillMaxWidth().height(14.dp).urknall(Ember),
                )
                // Only present while something is running, which is the whole reason the band can
                // have one height.
                val marks = state.singularities > 0 || stats.buff != null
                if (marks || ResearchTree.active(state) != null) {
                    Spacer(Modifier.height(8.dp))
                    StatusMarks(state = state, stats = stats, size = 10)
                }
            }
        }
        Rule()
    }
}

/** Where on the ladder, in the form that means something at that height. */
private fun rungLine(state: GameState, stats: Stats): String {
    val next = stats.nextTier ?: return if (stats.tier.isDesignated) {
        Lang.t("Katalog %s", stats.tier.label)
    } else {
        Lang.t("Stufe %s/%s", stats.tier.index + 1, Tiers.all.size)
    }
    return Lang.t(
        "Stufe %s/%s · noch %s",
        stats.tier.index + 1,
        Tiers.all.size,
        Numbers.formatMass(next.threshold - state.runMass),
    )
}

@Composable
private fun Cell(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 14.dp)) { content() }
}

/** The vertical rules between cells. Full height, so the band is divided rather than spaced. */
@Composable
private fun Divider() {
    Box(Modifier.width(2.dp).fillMaxHeight().background(Outline))
}
