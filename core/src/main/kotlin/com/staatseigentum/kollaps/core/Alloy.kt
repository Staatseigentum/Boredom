package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
/**
 * Two heavy elements, welded into something neither of them was.
 *
 * The chain stopped twice. Fusion stops at iron because a star's does; the collapse turns that iron
 * into gold and platinum and uranium, and then that stopped too — six piles of numbers that grew on
 * their own and were never spent on anything. A currency with no sink is a scoreboard, and the
 * fusion tab had become one.
 *
 * An alloy is the sink. It is bought once, with a fixed amount of two heavy elements, and it is
 * kept for good — through the collapse that made the metals and through the big bang after it, for
 * the same reason the metals themselves are: whatever this is, it was forged in a star that died
 * before any of the current arrangements existed.
 *
 * ## Why pairs and not a chain
 *
 * A chain would have been a third ladder to climb in a tab that already has two, and it would have
 * had a correct order to do it in. A pair is a decision with no order: six metals make fifteen
 * possible pairs, the game offers five of them, and the metals they ask for are not evenly spread —
 * so which alloy comes first depends on which collapses a player has actually been having.
 *
 * ## Why the cost is flat
 *
 * Everything else in the game is a geometric price curve, and this deliberately is not. The heavy
 * elements arrive in square roots of the iron in the core, which is already a heavily damped
 * number; putting a growth curve on top of a square root gives something that is either free for
 * ever or unreachable for ever, depending on the third decimal place of the growth factor. A flat
 * price is a target the player can see and work towards, which is the whole point of a sink.
 */
enum class Alloy(
    val id: String,
    val germanLabel: String,
    val germanFlavor: String,
    val first: HeavyElement,
    val second: HeavyElement,
    /** How much of each is consumed. The same for both, so the rarer metal is what gates it. */
    val cost: Double,
    val effect: PrestigeEffect,
) {
    ELEKTRUM(
        id = "al_electrum",
        germanLabel = "Elektrum",
        germanFlavor = "Gold und Platin, in einem Guss. Die Legierung, aus der die ersten Münzen waren.",
        first = HeavyElement.GOLD,
        second = HeavyElement.PLATIN,
        cost = 250.0,
        effect = PrestigeEffect.GlobalMultiplier(2.0),
    ),
    SCHWERGUSS(
        id = "al_dense",
        germanLabel = "Schwerguss",
        germanFlavor = "Osmium in Platin gelöst. Ein Barren davon ist nicht zu tragen, sondern zu schieben.",
        first = HeavyElement.OSMIUM,
        second = HeavyElement.PLATIN,
        cost = 120.0,
        effect = PrestigeEffect.MilestoneBonus(0.04),
    ),
    ZUENDKERN(
        id = "al_ignition",
        germanLabel = "Zündkern",
        germanFlavor = "Uran, mit Iridium umwickelt. Es brennt nicht, es fängt einfach an.",
        first = HeavyElement.URAN,
        second = HeavyElement.IRIDIUM,
        cost = 90.0,
        effect = PrestigeEffect.FusionRate(3.0),
    ),
    STERNSTAHL(
        id = "al_starsteel",
        germanLabel = "Sternstahl",
        germanFlavor = "Gold und Iridium. Weich genug zum Formen, hart genug, um es danach zu bereuen.",
        first = HeavyElement.GOLD,
        second = HeavyElement.IRIDIUM,
        cost = 200.0,
        effect = PrestigeEffect.TapMultiplier(4.0),
    ),
    ENDLEGIERUNG(
        id = "al_final",
        germanLabel = "Endlegierung",
        germanFlavor = "Plutonium und Osmium. Es gibt keinen Grund, warum das halten sollte, und es hält.",
        first = HeavyElement.PLUTONIUM,
        second = HeavyElement.OSMIUM,
        cost = 40.0,
        effect = PrestigeEffect.SingularityGain(1.8),
    ),
    ;

    val label: String get() = Lang.t(germanLabel)

    val flavor: String get() = Lang.t(germanFlavor)

    /** What it does, in the same words every other permanent bonus uses. */
    val effectText: String get() = effect.text

    /** What it costs, as a line the player can read. */
    val costText: String
        get() = "${Numbers.format(cost)} ${first.symbol} + ${Numbers.format(cost)} ${second.symbol}"

    companion object {
        fun byId(id: String?): Alloy? = entries.firstOrNull { it.id == id }

        /** Whether the workshop exists yet. It needs metal, and metal needs a collapse. */
        fun isUnlocked(state: GameState): Boolean =
            state.collapses > 0 || state.alloys.isNotEmpty()

        fun isForged(state: GameState, alloy: Alloy): Boolean = alloy.id in state.alloys

        /** Whether both piles are deep enough right now. */
        fun canForge(state: GameState, alloy: Alloy): Boolean =
            !isForged(state, alloy) &&
                Heavy.amountOf(state, alloy.first) >= alloy.cost &&
                Heavy.amountOf(state, alloy.second) >= alloy.cost

        /** Everything the workshop can show, forged or not, in a fixed order. */
        fun offered(state: GameState): List<Alloy> = if (isUnlocked(state)) entries.toList() else emptyList()

        /** The permanent effects of everything forged so far. */
        fun effects(state: GameState): List<PrestigeEffect> =
            entries.filter { it.id in state.alloys }.map { it.effect }
    }
}
