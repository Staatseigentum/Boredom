package com.staatseigentum.kollaps.core

/** One line of the story, and what earned it. */
data class Fragment(val id: String, val source: String, val text: String)

/**
 * The game's only voice.
 *
 * Everything else here is a number with a name on it. These are the lines that say what any of it
 * is for — one per rung of the ladder, a handful for the collapses, four for the big bangs.
 *
 * Derived from the state rather than recorded in it. A set of seen ids would be a second copy of
 * something the save already knows, and the two would drift the first time a rung was inserted
 * into the middle of the ladder — which has happened twice.
 */
object Lore {

    /** One per rung, in ladder order. Indexed by tier, so inserting a body needs a line too. */
    private val LADDER: List<String> = listOf(
        "Es fängt mit einem Stein an, der niemandem gehört. Du legst die Hand darauf.",
        "Groß genug, dass ihn jemand aufschreibt. Ein Name, eine Nummer, ein Eintrag.",
        "Rund. Nicht weil es jemand so wollte, sondern weil Schwerkraft keine Ecken mag.",
        "Grau und still. Und trotzdem hebt unten jemand den Kopf und sieht hinauf.",
        "Ein Planet, offiziell. Verbrannt auf der einen Seite, erfroren auf der anderen.",
        "Es regnet. Nicht Wasser, aber es regnet, und das ist mehr, als die meisten haben.",
        "Rost, soweit man sehen kann. Irgendwo darin ein Rover, der aufgehört hat zu funken.",
        "Von weitem der schönste Punkt am Morgenhimmel. Von nahem 460 Grad und Schwefelsäure.",
        "Blau. Der einzige Ort, von dem irgendjemand je zurückkommen wollte.",
        "Doppelt so schwer. Wer hier aufsteht, meint es ernst.",
        "Kein Boden mehr. Nur noch Wetter, das nach unten immer dichter wird.",
        "Er liegt auf der Seite. Irgendetwas hat ihn getroffen, vor sehr langer Zeit.",
        "Die Ringe sind jünger als die Dinosaurier und werden sie nicht lange überleben.",
        "Ein Sturm, in den die Erde zweimal hineinpasst, und er dreht sich seit Jahrhunderten.",
        "So nah an seinem Stern, dass er von unten glüht. Jahre dauern hier ein paar Tage.",
        "Fast ein Stern. Es fehlt nicht viel, und es wird nie reichen.",
        "Jetzt brennt es. Sparsam, aber es brennt, und es wird länger brennen als alles andere.",
        "Ein ganz gewöhnlicher gelber Zwerg. Acht Planeten halten ihn für den Mittelpunkt.",
        "Zu heiß, zu hell, zu schnell. Wer so brennt, hat es in einer Million Jahren hinter sich.",
        "Die Erdbahn passt bequem hinein. Was darin war, ist längst nicht mehr da.",
        "Größer geht nicht. Was jetzt noch dazukommt, bläst er wieder ab.",
        "Der Rest. Erdgroß, weiß, und er kühlt aus — für länger, als das Universum alt ist.",
        "Ein Teelöffel wiegt ein Gebirge. Die Oberfläche ist einen Zentimeter hoch und aus Eisen.",
        "Sein Feld würde dich aus tausend Kilometern in deine Atome zerlegen. Es ist nicht böse.",
        "Ab hier kommt nichts zurück. Nicht Licht, nicht Information, nicht du.",
    )

    /** For the collapses, cycling once the list runs out. */
    private val COLLAPSES: List<String> = listOf(
        "Alles, was du gesammelt hast, fällt in sich zusammen. Übrig bleibt ein Punkt.",
        "Beim zweiten Mal weißt du schon, was kommt. Es hilft nicht besonders.",
        "Du fängst an, den Weg zu kennen. Die Steine liegen jedes Mal woanders.",
        "Irgendwann hört es auf, ein Verlust zu sein, und wird ein Handgriff.",
        "Ein Punkt ist keine Größe. Er ist eine Stelle, an der die Frage aufhört.",
        "Was am Rand bleibt, lässt sich zählen. Mehr weiß niemand darüber.",
        "Du hast das jetzt oft genug gemacht, dass die Zahl selbst dir egal geworden ist.",
        "Und wieder von vorn. Es ist immer noch dasselbe Universum.",
    )

    /** And for the big bangs. Four, because a fifth would be repeating the third. */
    private val BANGS: List<String> = listOf(
        "Dieses Mal nicht der Körper. Das Universum. Alles davon, auf einmal.",
        "Ein zweites, aus denselben Regeln. Es kommt anders heraus, und das ist der Punkt.",
        "Du legst inzwischen fest, wie es wird, bevor es losgeht. Das ist kein kleiner Schritt.",
        "Irgendwann bleibt nur die Frage, warum überhaupt etwas ist und nicht nichts.",
    )

    /** The line for reaching a rung, or `null` if the ladder has grown past the text. */
    fun forTier(index: Int): Fragment? {
        val text = LADDER.getOrNull(index) ?: return null
        return Fragment("lore_tier_$index", Tiers.byIndex(index).name, text)
    }

    /** The line for the *n*-th collapse, counting from one. */
    fun forCollapse(count: Int): Fragment? {
        if (count < 1) return null
        val text = COLLAPSES[(count - 1).coerceAtMost(COLLAPSES.lastIndex)]
        return Fragment("lore_collapse_$count", "$count. Kollaps", text)
    }

    /** The line for the *n*-th big bang, counting from one. */
    fun forBigBang(count: Int): Fragment? {
        if (count < 1) return null
        val text = BANGS[(count - 1).coerceAtMost(BANGS.lastIndex)]
        return Fragment("lore_bang_$count", "$count. Urknall", text)
    }

    /**
     * Everything this player has earned, oldest first.
     *
     * The ladder is read off [GameState.bestTier] rather than off the current run, so a fragment
     * cannot be un-read by collapsing. Collapses and big bangs only ever count up anyway.
     */
    fun unlocked(state: GameState): List<Fragment> = buildList {
        for (index in 0..state.bestTier.coerceAtMost(LADDER.lastIndex)) {
            forTier(index)?.let { add(it) }
        }
        for (count in 1..state.collapses.coerceAtMost(COLLAPSES.size)) {
            forCollapse(count)?.let { add(it) }
        }
        for (count in 1..state.bigBangs.coerceAtMost(BANGS.size)) {
            forBigBang(count)?.let { add(it) }
        }
    }

    /** How many there are to find in total, for the header of the list. */
    val total: Int get() = LADDER.size + COLLAPSES.size + BANGS.size

    /** Whether there is anything to show yet. One line on the first rung is not a chronicle. */
    fun isWorthShowing(state: GameState): Boolean = unlocked(state).size >= 2
}
