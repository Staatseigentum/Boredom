package com.staatseigentum.kollaps.core

/**
 * What kind of universe this one is.
 *
 * Chosen when the big bang is pressed and kept until the next one. The point is to give repeated
 * universes an identity: without it the tenth big bang plays exactly like the first, only with
 * larger numbers, and the second-deepest reset in the game is a button that means "faster".
 *
 * Every path is a lean, not a bonus. Each one is strong at one thing and says nothing about the
 * rest, so choosing is a decision about how the next few hours are going to feel rather than an
 * arithmetic problem with a right answer.
 */
enum class Path(
    val id: String,
    val label: String,
    val flavor: String,
    val effects: List<PrestigeEffect>,
) {
    HAND(
        id = "path_hand",
        label = "Die Hand",
        flavor = "Ein Universum, in dem Masse dorthin kommt, wo jemand hinschlägt.",
        effects = listOf(
            PrestigeEffect.TapMultiplier(8.0),
            PrestigeEffect.AutoTap(4.0),
            PrestigeEffect.CometFrequency(1.5),
        ),
    ),
    MASCHINE(
        id = "path_machine",
        label = "Die Maschine",
        flavor = "Eines, das ohne dich weiterläuft und dabei kaum langsamer wird.",
        effects = listOf(
            PrestigeEffect.GlobalMultiplier(2.5),
            PrestigeEffect.MilestoneBonus(0.03),
            PrestigeEffect.AutoBuy,
        ),
    ),
    LABOR(
        id = "path_lab",
        label = "Das Labor",
        flavor = "Eines, in dem Wissen schneller entsteht als Materie — und über Nacht am meisten.",
        effects = listOf(
            PrestigeEffect.ResearchSpeed(3.0),
            PrestigeEffect.OfflineEfficiency(0.95),
            PrestigeEffect.OfflineCapHours(48.0),
        ),
    ),
    KERN(
        id = "path_core",
        label = "Der Kern",
        flavor = "Eines, in dem Sterne heißer brennen und schwerer sterben.",
        effects = listOf(
            PrestigeEffect.FusionRate(4.0),
            PrestigeEffect.SingularityGain(1.4),
        ),
    ),
    ;

    /** What this path does, in words, for the card that asks the player to pick one. */
    val effectTexts: List<String> get() = effects.map { it.text }

    companion object {
        fun byId(id: String?): Path? = entries.firstOrNull { it.id == id }

        /** The path this state is running under, or `null` before the first big bang. */
        fun of(state: GameState): Path? = byId(state.path)
    }
}
