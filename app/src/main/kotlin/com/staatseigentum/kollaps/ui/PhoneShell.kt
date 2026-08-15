package com.staatseigentum.kollaps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.staatseigentum.kollaps.core.BuyAmount
import com.staatseigentum.kollaps.core.Designations
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Multiverse
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Orbits
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Stats
import com.staatseigentum.kollaps.core.Wallclock
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.theme.Ember
import com.staatseigentum.kollaps.ui.theme.Muted
import com.staatseigentum.kollaps.ui.theme.Nebula
import com.staatseigentum.kollaps.ui.theme.Outline
import com.staatseigentum.kollaps.ui.theme.Positive
import com.staatseigentum.kollaps.ui.theme.SpaceCard
import com.staatseigentum.kollaps.ui.theme.SpaceElevated
import com.staatseigentum.kollaps.ui.theme.Starlight
import kotlinx.coroutines.delay

/**
 * Which area of the game a phone is showing.
 *
 * Five, where there used to be two.
 *
 * Two was the right answer to the problem it solved: the phone had been stacking a header, the
 * body and the shop into one screen and none of the three had room, so the body and the shop were
 * split into separate screens with a bar to choose between them. That worked, and it left the shop
 * carrying a six-tab strip *inside* a screen that was itself reached through a bar — two levels of
 * navigation, one on top of the other, for one list.
 *
 * So the bar becomes the strip. Each area is one place the game has, the shop no longer has tabs of
 * its own on a phone, and everything is one tap away instead of two.
 *
 * Upgrades are the exception, and deliberately: they live under [FLEET] behind a switch rather than
 * as a sixth entry, because five targets across 360 dp is 72 dp each — enough for an icon and a
 * label — and six is not. They also belong there. What you do in that panel is spend mass on
 * production, and whether that mass buys another machine or makes the machines you have better is a
 * smaller decision than which panel you are in.
 */
enum class PhoneView(val label: String) {
    BODY("Körper"),
    FLEET("Flotte"),
    ORBITS("Bahnen"),
    FUSION("Fusion"),
    COSMOS("Kosmos"),
    ;

    companion object {
        /**
         * The areas worth offering for this state.
         *
         * The same rule the shop's own strip used: an area for a system that has not been unlocked
         * is a tab onto an explanation of why it is empty. When one drops out the rest share the
         * width, so an early game has three wide targets rather than five with two dead ones.
         */
        fun availableIn(state: GameState): List<PhoneView> = entries.filter {
            when (it) {
                ORBITS -> Orbits.isUnlocked(state)
                FUSION -> Fusion.isUnlocked(state)
                else -> true
            }
        }
    }
}

/**
 * The bar along the bottom, which is the phone's whole navigation.
 *
 * At the bottom because that is where a thumb rests. The selected entry is marked with a bar along
 * its *top* edge rather than by filling the whole cell: at five entries a filled cell is a large
 * block of colour under the reading eye, and the top edge is the side facing the screen it selects.
 *
 * Icons are block shapes rather than drawn symbols — a glyph from an icon font in the middle of
 * this game would be the one anti-aliased thing on screen.
 */
@Composable
fun PhoneNav(
    state: GameState,
    stats: Stats,
    current: PhoneView,
    onSelect: (PhoneView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sfx = LocalSfx.current
    Row(modifier = modifier.fillMaxWidth().background(SpaceElevated)) {
        for (entry in PhoneView.availableIn(state)) {
            val selected = entry == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) SpaceCard else SpaceElevated)
                    .clickable {
                        sfx?.click()
                        onSelect(entry)
                    }
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The selected marker, drawn as part of the cell rather than as a border, so it
                // sits flush against the top edge with nothing between it and the screen above.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (selected) Nebula else SpaceElevated),
                )
                Spacer(Modifier.height(7.dp))
                NavIcon(
                    view = entry,
                    tint = if (selected) Starlight else Muted,
                    glow = Color(stats.tier.glowColor),
                )
                Spacer(Modifier.height(6.dp))
                PixelLabel(
                    text = entry.label,
                    color = if (selected) Starlight else Muted,
                    size = 9,
                )
            }
        }
    }
}

/** One block-shaped icon. Nothing here is a glyph; every one of them is rectangles and circles. */
@Composable
private fun NavIcon(view: PhoneView, tint: Color, glow: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val side = size.minDimension
        when (view) {
            // The body itself, in the colour of the rung the player is standing on — the one icon
            // that says something about the state rather than only about where it leads.
            PhoneView.BODY -> drawCircle(color = glow, radius = side / 2f)

            // Three machines stacked.
            PhoneView.FLEET -> {
                val bar = side * 3f / 14f
                val gap = (side - bar * 3f) / 2f
                for (index in 0 until 3) {
                    drawRect(
                        color = tint,
                        topLeft = Offset(0f, index * (bar + gap)),
                        size = Size(side, bar),
                    )
                }
            }

            // A ring with a body on it.
            PhoneView.ORBITS -> {
                drawCircle(
                    color = tint,
                    radius = side / 2f - side / 14f,
                    style = Stroke(width = side * 2f / 14f),
                )
                drawRect(
                    color = tint,
                    topLeft = Offset(size.width / 2f - side * 2f / 14f, -side / 14f),
                    size = Size(side * 4f / 14f, side * 4f / 14f),
                )
            }

            // A nucleus, stood on its corner.
            PhoneView.FUSION -> rotate(degrees = 45f) {
                val edge = side * 11f / 14f
                drawRect(
                    color = tint,
                    topLeft = Offset((size.width - edge) / 2f, (size.height - edge) / 2f),
                    size = Size(edge, edge),
                )
            }

            // Four bodies in a field, one of them lit — the sky, and the one universe in it that
            // is yours.
            PhoneView.COSMOS -> {
                val block = side * 4f / 14f
                val gap = side - block
                for (row in 0 until 2) {
                    for (column in 0 until 2) {
                        drawRect(
                            color = if (row == 1 && column == 1) Ember else tint,
                            topLeft = Offset(column * gap, row * gap),
                            size = Size(block, block),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The three lines above everything, on a phone.
 *
 * Replaces a header that had grown to seven rows and half a screen. What is left is what a player
 * glances up for while tapping: how much mass there is, how fast it is arriving, and how far it is
 * to the next body. Everything else that used to live here — the buff, the lab, the singularities —
 * became a [StatusMarks] chip down in the body area, which is what keeps this exactly three lines
 * tall no matter how much is going on at once.
 */
@Composable
fun StatusBand(state: GameState, stats: Stats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SpaceElevated)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)
            .sog(SogDepth.SHELL, Ember),
    ) {
        /*
         * The mass, at whatever size it still fits at.
         *
         * Both labels were fixed sizes in a row that shares its width by pushing them apart, and
         * that held for as long as a mass was five characters. It is not: past the named units a
         * mass reads `1,23 Sxd kg` and the rate under it is no shorter, and together at
         * twenty-six and eleven points they are wider than a 360 point phone. What happened then
         * was not a clip but a wrap — the band that is documented above as being exactly three
         * lines tall quietly became four, and the rate ended up under the mass.
         *
         * Two things fix it and both are needed. The mass steps down through three whole sizes as
         * it grows, because a pixel face may only ever be drawn at whole sizes; and each label is
         * held to one line, so nothing can take the height even if a translation lands longer than
         * anything measured here.
         */
        val mass = Numbers.formatMass(state.mass)
        Row(
            modifier = Modifier.fillMaxWidth().sog(SogDepth.CONTENT, Starlight).urknall(Starlight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            PixelLabel(
                text = mass,
                color = Starlight,
                size = massSize(mass),
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            PixelLabel(
                text = "+${Numbers.formatRate(stats.massPerSecond)}",
                color = Positive,
                size = 11,
                maxLines = 1,
            )
        }

        val glow = Color(stats.tier.glowColor)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().sog(SogDepth.CONTENT, glow).urknall(glow),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelLabel(
                text = climbLine(state, stats),
                color = glow,
                size = 10,
                modifier = Modifier.weight(1f, fill = false),
            )
            val remaining = stats.nextTier?.let {
                "${stats.tier.index + 1}/${Tiers.all.size} · noch " +
                    Numbers.formatMass(it.threshold - state.runMass)
            }
            if (remaining != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = remaining,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        Spacer(Modifier.height(5.dp))
        PixelBar(
            progress = stats.tierProgress,
            color = glow,
            // Sixteen rather than the twenty-four the wide bar uses: on 360 dp, twenty-four cells
            // put each block under a device pixel and the bar turns into a smear.
            cells = 16,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .sog(SogDepth.CONTENT, glow)
                .urknall(Ember),
        )
    }
}

/**
 * How large the mass may be drawn, given how long it has become.
 *
 * Whole sizes and nothing between them: the display face is a pixel font, and a pixel font at a
 * fractional size is the one thing this whole look cannot survive. Three steps is enough — the
 * string only grows by the unit suffix, and it stops growing entirely once the exponent form
 * takes over.
 */
private fun massSize(mass: String): Int = when {
    mass.length <= 10 -> 26
    mass.length <= 13 -> 21
    else -> 17
}

/**
 * Where the player is and what is next, in one line.
 *
 * Shared by the phone band and the wide HUD, because it is one sentence about one thing and the two
 * had already drifted apart once.
 */
fun climbLine(state: GameState, stats: Stats): String {
    val next = stats.nextTier
    return when {
        next != null -> "${stats.tier.label} > ${next.label}"
        // Above the gate with the catalogue still shut. Not the end of anything — there are
        // sixteen thousand rungs over this one — and saying "das Ende der Leiter" to somebody
        // fifty orders of magnitude past it is the interface calling a locked door a wall.
        !Designations.isUnlocked(state) ->
            "${stats.tier.label} · Katalog ab ${Multiverse.SLOTS} Galaxien (${Multiverse.count(state)})"

        else -> "${stats.tier.label} — das Ende der Leiter"
    }
}

/**
 * The small outlined chips: what is running right now, and nothing that is not.
 *
 * Only ever what applies. These replaced three fixed rows in the header — a singularity count that
 * was zero for the first hour, a buff panel that was empty most of the time, and a research ticker
 * that took two rows to say a project has four minutes left. As chips they cost nothing when there
 * is nothing to say, which is what let the band above become three lines.
 */
@Composable
fun StatusMarks(state: GameState, stats: Stats, size: Int = 10, modifier: Modifier = Modifier) {
    // Ticked once a second, and only while something is actually counting down — a `while (true)`
    // that runs when there is no lab and no buff is a wakeup per second for nothing.
    val running = ResearchTree.active(state)
    var now by remember { mutableLongStateOf(Wallclock.millis()) }
    LaunchedEffect(running?.id) {
        if (running == null) return@LaunchedEffect
        while (true) {
            now = Wallclock.millis()
            delay(1_000)
        }
    }

    // Scrolls, because all three can be up at once — a singularity count, a buff and a lab
    // ticking down — and three chips of running text is wider than a phone. Without this the last
    // of them was simply cut off at the edge, which is the one that says what is happening *now*.
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.singularities > 0) {
            Mark("${Numbers.format(state.singularities)} Sing.", Ember, size)
        }
        stats.buff?.let { buff ->
            Mark(
                "${buff.label} ${Numbers.formatMultiplier(buff.factor)} · ${stats.buffSecondsLeft.toInt()}s",
                Positive,
                size,
            )
        }
        if (running != null) {
            val left = ResearchTree.secondsLeft(state, now)
            Mark(
                "Labor · " + if (left <= 0.0) "fertig" else Numbers.formatDuration(left.toLong()),
                Muted,
                size,
            )
        }
    }
}

@Composable
private fun Mark(text: String, colour: Color, size: Int) {
    Box(
        modifier = Modifier
            .background(SpaceElevated)
            .border(2.dp, colour, RectangleShape)
            .padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        PixelLabel(text = text, color = colour, size = size)
    }
}

/**
 * One line under the body: the cheapest thing worth buying, and a tap that buys it.
 *
 * The shop is one tap away, which is one tap more than the most common action in the game deserves.
 * Nine times out of ten what a player wants from that panel is the next machine at the current buy
 * amount — so it is offered here, next to the body they are already looking at, and the panel is
 * for the tenth time.
 *
 * No state of its own: it asks the engine the same question the shop asks and takes the cheapest
 * answer it can afford. When nothing is affordable it is not there.
 */
@Composable
fun NextBuyRow(
    state: GameState,
    buyAmount: BuyAmount,
    onBuy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sfx = LocalSfx.current
    val offer = remember(state, buyAmount) {
        GameEngine.collectorOffers(state, buyAmount)
            .filter { it.visible && it.lockedReason == null && it.affordable && it.amount > 0 }
            .minByOrNull { it.cost }
    } ?: return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SpaceElevated)
            .border(2.dp, Outline, RectangleShape)
            .clickable {
                sfx?.purchase()
                onBuy(offer.collector.id)
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            PixelLabel(text = "Nächster Kauf", color = Positive, size = 10)
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (offer.amount > 1) {
                    "${offer.collector.name} ×${offer.amount}"
                } else {
                    offer.collector.name
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Starlight,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(10.dp))
        PixelLabel(
            text = Numbers.formatMass(offer.cost),
            color = Positive,
            size = 11,
        )
    }
}
