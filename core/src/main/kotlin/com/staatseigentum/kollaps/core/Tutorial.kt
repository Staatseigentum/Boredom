package com.staatseigentum.kollaps.core

/**
 * One thing to do, and how the game knows it has been done.
 *
 * The condition is a function of the state rather than something the screen reports, which is what
 * keeps the whole thing honest: a step is finished because the game actually is in that shape, not
 * because a particular button was pressed. A player who buys their first collector without ever
 * reading the step has finished it, and one who does it by accident is not told to do it again.
 */
data class TutorialStep(
    val id: String,
    val title: String,
    val text: String,
    val isDone: (GameState) -> Boolean,
)

/**
 * The first five minutes, and nothing beyond them.
 *
 * An idle game explains itself badly on its own: the first tap is obvious, the first collector is
 * nearly obvious, and everything after that — that upgrades exist, that a body becomes a bigger
 * body, that collapsing is a good idea and not a failure — is a thing this game simply never said.
 *
 * Six steps, in the order they come up by playing anyway. It is a nudge in the corner rather than
 * a sequence of dialogs, and it can be sent away at any point: somebody who has played an idle
 * game before knows all of this, and holding them up would be a worse first impression than
 * saying nothing.
 */
object Tutorial {

    val steps: List<TutorialStep> = listOf(
        TutorialStep(
            id = "tut_tap",
            title = "Tipp den Körper an",
            text = "Jeder Tipp bringt Masse. Am Anfang ist das die einzige Quelle — später " +
                "läuft es auch ohne dich weiter.",
            isDone = { it.taps >= 10 },
        ),
        TutorialStep(
            id = "tut_collector",
            title = "Kauf deinen ersten Kollektor",
            text = "Im Laden rechts, Reiter Kollektoren. Ein Kollektor sammelt Masse, ohne " +
                "dass du etwas tust — auch dann, wenn die App zu ist.",
            isDone = { state -> state.collectors.values.sum() >= 1 },
        ),
        TutorialStep(
            id = "tut_more",
            title = "Und noch ein paar davon",
            text = "Alle zehn Stück wird ein Kollektor dauerhaft besser. Der Knopf oben im " +
                "Laden kauft gleich zehn oder hundert auf einmal.",
            isDone = { state -> state.collectors.values.sum() >= 10 },
        ),
        TutorialStep(
            id = "tut_upgrade",
            title = "Kauf ein Upgrade",
            text = "Reiter Upgrades. Kollektoren machen mehr Masse, Upgrades machen jede " +
                "davon mehr wert — und sie sind es, die den Unterschied ausmachen.",
            isDone = { it.upgrades.isNotEmpty() },
        ),
        TutorialStep(
            id = "tut_tier",
            title = "Werde größer",
            text = "Genug Masse, und aus dem Gestein wird ein größerer Körper. Fünfundzwanzig " +
                "Stufen gibt es, von einem Meteoriten bis zu etwas, aus dem nichts zurückkommt.",
            isDone = { it.bestTier >= 1 },
        ),
        TutorialStep(
            id = "tut_cosmos",
            title = "Sieh dir den Kosmos-Reiter an",
            text = "Dort steht, was noch kommt: der Kollaps, der alles zurücksetzt und dich " +
                "dafür dauerhaft schneller macht, das Labor, die Automatik. Kein Grund zur " +
                "Eile — aber gut zu wissen, dass es das gibt.",
            isDone = { it.bestTier >= 3 },
        ),
    )

    /** The step the player is on, or `null` once there is nothing left to say. */
    fun current(state: GameState): TutorialStep? {
        if (state.tutorialDone) return null
        return steps.firstOrNull { !it.isDone(state) }
    }

    /** How far along the nudge is, for the little counter on it. */
    fun position(state: GameState): Int =
        steps.indexOfFirst { !it.isDone(state) }.let { if (it < 0) steps.size else it + 1 }

    val total: Int get() = steps.size

    /**
     * Whether the tutorial should be offered at all.
     *
     * Anyone who has already collapsed, or who is loading a save from before this existed and is
     * plainly past it, is not shown a word. A game explaining the first tap to somebody four
     * hours in would be worse than never explaining it.
     */
    fun appliesTo(state: GameState): Boolean =
        !state.tutorialDone && state.collapses == 0 && state.bestTier < steps.size
}
