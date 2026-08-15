package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
/** One setting a rule can be run at. */
data class AutomationOption(val germanLabel: String, val value: Double) {
    val label: String get() = Lang.t(germanLabel)
}

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
    val germanLabel: String,
    val germanFlavor: String,
    /** What the dial means, for the row's heading. */
    val germanSetting: String,
    val options: List<AutomationOption>,
) {
    COLLECTORS(
        id = "au_collectors",
        germanLabel = "Kollektoren nachkaufen",
        germanFlavor = "Kauft den Kollektor mit der besten Rendite, solange genug übrig bleibt.",
        germanSetting = "Rücklage",
        options = listOf(
            AutomationOption("×2", 2.0),
            AutomationOption("×4", 4.0),
            AutomationOption("×10", 10.0),
            AutomationOption("×50", 50.0),
        ),
    ),
    UPGRADES(
        id = "au_upgrades",
        germanLabel = "Upgrades kaufen",
        germanFlavor = "Nimmt jede Verbesserung mit, sobald sie klein genug gegen dein Vermögen ist.",
        germanSetting = "Höchstens",
        options = listOf(
            AutomationOption("die Hälfte", 0.5),
            AutomationOption("ein Zehntel", 0.1),
            AutomationOption("ein Hundertstel", 0.01),
        ),
    ),
    FUSION(
        id = "au_fusion",
        germanLabel = "Fusionskette ausbauen",
        germanFlavor = "Baut die billigste Stufe aus, damit kein Ofen lange hungert.",
        germanSetting = "Rücklage",
        options = listOf(
            AutomationOption("×5", 5.0),
            AutomationOption("×20", 20.0),
            AutomationOption("×100", 100.0),
        ),
    ),
    ORBITS(
        id = "au_orbits",
        germanLabel = "Bahnen ausbauen",
        germanFlavor = "Setzt Körper auf freie Bahnen und öffnet die nächste, wenn sie leicht drin ist.",
        germanSetting = "Rücklage",
        options = listOf(
            AutomationOption("×2", 2.0),
            AutomationOption("×5", 5.0),
            AutomationOption("×20", 20.0),
        ),
    ),
    RESEARCH(
        id = "au_research",
        germanLabel = "Forschung anstoßen",
        germanFlavor = "Lässt die Bank nie leer stehen.",
        germanSetting = "Nimmt",
        options = listOf(
            AutomationOption("das billigste", 0.0),
            AutomationOption("das teuerste leistbare", 1.0),
        ),
    ),
    /**
     * The rule that collapses for you, for a number of runs you name and no more.
     *
     * This was removed once, and the objection was right: the other rules buy things, taking a
     * decision the player has already made a hundred times and no longer asking. That one *ended
     * the run* — the single moment the whole game builds to, the one with hours behind it and a
     * sequence in front of it, handed to a background loop while nobody was looking. An idle game
     * may play itself; it should not finish itself.
     *
     * What brings it back is the dial. It is not a switch that hands the game over indefinitely,
     * it is an order for five, ten, twenty-five or fifty runs — and when they are done it switches
     * itself off and gives the button back. Setting it is itself the decision to end that many
     * runs, made once, deliberately, in advance. The count left is in [GameState.collapseBudget].
     *
     * Note the id. The old rule's `au_collapse` is deliberately *not* reused: there are saves out
     * there carrying it from before the removal, and answering to it again would switch this on for
     * players who never asked, at a setting they chose for something else years ago.
     */
    COLLAPSE(
        id = "au_collapse_counted",
        germanLabel = "Kollabieren lassen",
        germanFlavor = "Kollabiert, sobald Warten kaum noch etwas bringt — und hört danach von selbst auf.",
        germanSetting = "Läufe",
        options = listOf(
            AutomationOption("5", 5.0),
            AutomationOption("10", 10.0),
            AutomationOption("25", 25.0),
            AutomationOption("50", 50.0),
        ),
    ),
    ;

    val label: String get() = Lang.t(germanLabel)

    val flavor: String get() = Lang.t(germanFlavor)

    val setting: String get() = Lang.t(germanSetting)

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
            AutomationRule.ORBITS -> Orbits.isUnlocked(state)
            AutomationRule.RESEARCH -> ResearchTree.isUnlocked(state)
            // Not before the player has ended a run by hand. Automating a thing nobody has done
            // yet is not saving them the work, it is taking the moment away before they have had it.
            AutomationRule.COLLAPSE -> state.collapses > 0
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
        // Choosing a count *is* placing the order, so it is filled here rather than at the first
        // collapse. Picking a different count refills it outright instead of adding to what is
        // left: "fünfzig" has to mean fifty more runs, whatever was standing before it.
        val budget = when {
            rule != AutomationRule.COLLAPSE -> state.collapseBudget
            option == null -> 0
            else -> rule.optionAt(option).value.toInt()
        }
        return state.copy(automation = updated, autoBuyOn = legacy, collapseBudget = budget)
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
