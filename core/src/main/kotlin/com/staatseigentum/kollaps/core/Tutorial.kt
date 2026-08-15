package com.staatseigentum.kollaps.core

/**
 * Where a step wants the player to be.
 *
 * The single most useful thing the first version was missing. "Kauf deinen ersten Kollektor" is a
 * clear instruction and a useless one if you do not yet know that there is a shop, that it has
 * tabs, or which of them holds collectors — and on a phone the shop is not even on screen while
 * the body is. So a step now names a place, the hint can offer to take you there, and the button
 * that leads there is marked while the step is open.
 *
 * Deliberately coarse. These are the four places the opening ever needs; the systems that unlock
 * later are introduced by [Unlocks] instead, each naming its own spot in words.
 */
enum class TutorialSpot(val label: String) {
    /** The body itself: the tap area, and what the phone opens on. */
    BODY("Körper"),

    /** The collectors, and the upgrades behind the switch on the same panel. */
    FLOTTE("Flotte"),

    /** The upgrade list. On a phone it shares the fleet area; on a wide screen it is its own tab. */
    UPGRADES("Upgrades"),

    /** Everything that is not buying something: the collapse, the lab, the rules, the settings. */
    KOSMOS("Kosmos"),
}

/**
 * One thing to do, where to do it, and how the game knows it has been done.
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
    /** Where this happens, or `null` for a step that is about the screen you are already on. */
    val spot: TutorialSpot?,
    val isDone: (GameState) -> Boolean,
)

/**
 * The opening, walked through rather than hinted at.
 *
 * ## What the first version got wrong
 *
 * Six steps that named actions and not places. That reads fine to somebody who already knows the
 * screen and is nearly useless to somebody who does not: "Reiter Upgrades" assumes you have found
 * the shop, know it has tabs, and can see them — and on a phone the shop is a different screen
 * entirely, reached by a bar the tutorial never mentioned. The steps were also thin on the things
 * that decide whether an idle game clicks at all: that a purchase gets cheaper per ten, that the
 * body keeps producing with the app shut, that collapsing is the point rather than a punishment.
 *
 * ## What this is instead
 *
 * Ten steps that each name a place, in the order playing brings them up anyway. It is still a
 * strip in the corner and it still never blocks anything — an idle game is played with one thumb
 * and no patience for being taught — but the strip can now take you where the step happens, and
 * the way there is marked while it is open.
 *
 * Everything past the opening is somebody else's job: see [Unlocks], which introduces each system
 * at the moment it first exists.
 */
object Tutorial {

    val steps: List<TutorialStep> = listOf(
        TutorialStep(
            id = "tut_tap",
            title = "Tipp den Körper an",
            text = "Jeder Tipp bringt Masse. Am Anfang ist das die einzige Quelle — und die " +
                "Zahl oben zählt mit, was du hast.",
            spot = TutorialSpot.BODY,
            isDone = { it.taps >= 10 },
        ),
        TutorialStep(
            id = "tut_collector",
            title = "Kauf deinen ersten Kollektor",
            text = "Kollektoren sammeln Masse, ohne dass du etwas tust — auch dann, wenn die " +
                "App zu ist. Ab hier läuft das Spiel auch ohne dich weiter.",
            spot = TutorialSpot.FLOTTE,
            isDone = { state -> state.collectors.values.sum() >= 1 },
        ),
        TutorialStep(
            id = "tut_more",
            title = "Und noch ein paar davon",
            text = "Alle zehn Stück wird ein Kollektor dauerhaft besser. Der Knopf oben in der " +
                "Flotte kauft gleich zehn oder hundert auf einmal, statt hundertmal zu tippen.",
            spot = TutorialSpot.FLOTTE,
            isDone = { state -> state.collectors.values.sum() >= 10 },
        ),
        TutorialStep(
            id = "tut_second",
            title = "Nimm eine zweite Sorte dazu",
            text = "Weiter unten in der Flotte stehen teurere Maschinen. Eine neue Sorte bringt " +
                "fast immer mehr als die zehnte Kopie der alten — und schaltet später eigene " +
                "Upgrades frei.",
            spot = TutorialSpot.FLOTTE,
            isDone = { state -> state.collectors.count { it.value > 0 } >= 2 },
        ),
        TutorialStep(
            id = "tut_upgrade",
            title = "Kauf ein Upgrade",
            text = "Kollektoren machen mehr Masse, Upgrades machen jede davon mehr wert — und " +
                "sie sind es, die über einen Lauf den Unterschied ausmachen.",
            spot = TutorialSpot.UPGRADES,
            isDone = { it.upgrades.isNotEmpty() },
        ),
        TutorialStep(
            id = "tut_tier",
            title = "Werde größer",
            text = "Genug Masse, und aus dem Gestein wird ein größerer Körper. Der Balken oben " +
                "zeigt, wie weit es noch ist.",
            spot = TutorialSpot.BODY,
            isDone = { it.bestTier >= 1 },
        ),
        TutorialStep(
            id = "tut_fleet_seen",
            title = "Deine Flotte fliegt mit",
            text = "Was du kaufst, siehst du: die Maschinen kreisen um deinen Körper, eine " +
                "Bahn je Sorte. Wer viel besitzt, sieht es, ohne in den Laden zu gehen.",
            spot = TutorialSpot.BODY,
            isDone = { it.bestTier >= 2 },
        ),
        TutorialStep(
            id = "tut_offline",
            title = "Es läuft auch ohne dich",
            text = "Leg das Spiel ruhig weg. Beim Öffnen bekommst du die Zeit gutgeschrieben — " +
                "ein Bericht sagt dir dann, was in der Zwischenzeit angefallen ist.",
            spot = null,
            isDone = { it.bestTier >= 3 },
        ),
        TutorialStep(
            id = "tut_cosmos",
            title = "Sieh dir den Kosmos an",
            text = "Dort steht alles, was nicht gekauft wird: der Kollaps, das Labor, die " +
                "Automatik, deine Erfolge und die Einstellungen. Kein Grund zur Eile — aber gut " +
                "zu wissen, wo es liegt.",
            spot = TutorialSpot.KOSMOS,
            isDone = { it.bestTier >= 4 },
        ),
        TutorialStep(
            id = "tut_collapse_soon",
            title = "Der Kollaps kommt noch",
            text = "Irgendwann ist dein Körper schwer genug, um zusammenzufallen. Das setzt den " +
                "Lauf zurück und macht dich dauerhaft schneller. Es meldet sich von selbst, " +
                "wenn es so weit ist.",
            spot = null,
            isDone = { it.bestTier >= 6 || it.collapses > 0 },
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
     *
     * The tier bound is measured against the *last* step rather than against the number of steps,
     * which is not the same thing and was a real bug waiting to happen: the list grew from six to
     * ten, and a bound of "fewer tiers than there are steps" would have started showing the
     * opening again to saves that had left it behind.
     */
    fun appliesTo(state: GameState): Boolean =
        !state.tutorialDone && state.collapses == 0 && state.bestTier < LAST_TIER

    /** The rung past which the opening has nothing left to say. Matches the final step. */
    private const val LAST_TIER = 6
}
