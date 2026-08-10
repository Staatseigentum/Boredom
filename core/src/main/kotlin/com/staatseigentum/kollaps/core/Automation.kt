package com.staatseigentum.kollaps.core

/** One setting a rule can be run at. */
data class AutomationOption(val label: String, val value: Double)

/**
 * A standing order the game carries out on its own.
 *
 * Deliberately a fixed set of rules with a dial each, rather than a builder with conditions and
 * actions to combine. On a phone a general rule editor is four dropdowns and a keyboard for
 * something the player wanted to express in two words; five named rules with a setting each cover
 * what anybody actually automates, and every one of them can be read at a glance.
 *
 * Every rule is off until switched on. Automation that arrives switched on takes the game away
 * from the player as a reward for progressing, which is backwards.
 */
enum class AutomationRule(
    val id: String,
    val label: String,
    val flavor: String,
    /** What the dial means, for the row's heading. */
    val setting: String,
    val options: List<AutomationOption>,
) {
    COLLECTORS(
        id = "au_collectors",
        label = "Kollektoren nachkaufen",
        flavor = "Kauft den Kollektor mit der besten Rendite, solange genug übrig bleibt.",
        setting = "Rücklage",
        options = listOf(
            AutomationOption("×2", 2.0),
            AutomationOption("×4", 4.0),
            AutomationOption("×10", 10.0),
            AutomationOption("×50", 50.0),
        ),
    ),
    UPGRADES(
        id = "au_upgrades",
        label = "Upgrades kaufen",
        flavor = "Nimmt jede Verbesserung mit, sobald sie klein genug gegen dein Vermögen ist.",
        setting = "Höchstens",
        options = listOf(
            AutomationOption("die Hälfte", 0.5),
            AutomationOption("ein Zehntel", 0.1),
            AutomationOption("ein Hundertstel", 0.01),
        ),
    ),
    FUSION(
        id = "au_fusion",
        label = "Fusionskette ausbauen",
        flavor = "Baut die billigste Stufe aus, damit kein Ofen lange hungert.",
        setting = "Rücklage",
        options = listOf(
            AutomationOption("×5", 5.0),
            AutomationOption("×20", 20.0),
            AutomationOption("×100", 100.0),
        ),
    ),
    RESEARCH(
        id = "au_research",
        label = "Forschung anstoßen",
        flavor = "Lässt die Bank nie leer stehen.",
        setting = "Nimmt",
        options = listOf(
            AutomationOption("das billigste", 0.0),
            AutomationOption("das teuerste leistbare", 1.0),
        ),
    ),
    ;

    /*
     * There was a sixth rule here that collapsed for you.
     *
     * It is gone, and not because it worked badly. The other five buy things: they take a decision
     * the player has already made a hundred times and stop asking. This one *ended the run* — the
     * single moment the whole game builds to, the one with four hours behind it and a sequence in
     * front of it, handed to a background loop while nobody was looking. An idle game may play
     * itself; it should not finish itself.
     *
     * Saves that still carry `au_collapse` in their automation map are unaffected: an id no rule
     * answers to is simply never read.
     */

    fun optionAt(index: Int): AutomationOption = options[index.coerceIn(options.indices)]

    companion object {
        fun byId(id: String?): AutomationRule? = entries.firstOrNull { it.id == id }
    }
}

object Automation {

    /**
     * The rule the old on-off switch used to be.
     *
     * A save from before the rules existed carries `autoBuyOn` and nothing else. Rather than
     * migrate it into the map — which would write a setting the player never chose into their
     * save — the collector rule falls back to it, at the reserve the old switch always used.
     */
    val LEGACY_RULE = AutomationRule.COLLECTORS

    /** Which option of the legacy rule matches what the old switch did. */
    val LEGACY_OPTION: Int =
        AutomationRule.COLLECTORS.options.indexOfFirst { it.value == GameEngine.AUTO_BUY_RESERVE }
            .coerceAtLeast(0)

    /** True once any of this is available at all. */
    fun isUnlocked(state: GameState): Boolean = GameEngine.hasAutoBuy(state)

    /** Whether a single rule can be switched on yet. */
    fun isAvailable(state: GameState, rule: AutomationRule): Boolean {
        if (!isUnlocked(state)) return false
        return when (rule) {
            AutomationRule.COLLECTORS -> true
            AutomationRule.UPGRADES -> true
            AutomationRule.FUSION -> Fusion.isUnlocked(state)
            AutomationRule.RESEARCH -> ResearchTree.isUnlocked(state)
        }
    }

    /** The chosen option index, or `null` when the rule is off. */
    fun settingOf(state: GameState, rule: AutomationRule): Int? {
        state.automation[rule.id]?.let { return it.coerceIn(rule.options.indices) }
        if (rule == LEGACY_RULE && state.autoBuyOn) return LEGACY_OPTION
        return null
    }

    fun isOn(state: GameState, rule: AutomationRule): Boolean =
        isAvailable(state, rule) && settingOf(state, rule) != null

    /** What the dial is set to, or `null` when the rule is off. */
    fun valueOf(state: GameState, rule: AutomationRule): Double? =
        settingOf(state, rule)?.let { rule.optionAt(it).value }

    /** Switches a rule on at [option], or off when [option] is `null`. */
    fun set(state: GameState, rule: AutomationRule, option: Int?): GameState {
        val updated = if (option == null) {
            state.automation - rule.id
        } else {
            state.automation + (rule.id to option.coerceIn(rule.options.indices))
        }
        // The old switch is kept in step so that turning the collector rule off actually turns it
        // off, rather than falling straight back through to the legacy flag.
        val legacy = if (rule == LEGACY_RULE) option != null else state.autoBuyOn
        return state.copy(automation = updated, autoBuyOn = legacy)
    }

    /** Moves a rule to its next setting, switching it on if it was off and off after the last. */
    fun cycle(state: GameState, rule: AutomationRule): GameState {
        val current = settingOf(state, rule)
        val next = when {
            current == null -> 0
            current + 1 < rule.options.size -> current + 1
            else -> null
        }
        return set(state, rule, next)
    }
}
