package com.staatseigentum.kollaps.core

/**
 * Which of the three shells a body is mostly made of.
 *
 * Its own small type rather than a nullable [Shell], because "no lane at all" is a real answer and
 * one of the better ones: a body built evenly is a layered world, which is a thing to be and not
 * the absence of being anything.
 */
enum class Lane(val id: String, val label: String) {
    METALL("ln_core", "Kern"),
    GESTEIN("ln_mantle", "Mantel"),
    EIS("ln_crust", "Kruste"),
    GESCHICHTET("ln_layered", "Geschichtet"),
    ;

    companion object {
        /** The shell this lane is read off, or `null` for the layered one. */
        fun shellOf(lane: Lane): Shell? = when (lane) {
            METALL -> Shell.KERN
            GESTEIN -> Shell.MANTEL
            EIS -> Shell.KRUSTE
            GESCHICHTET -> null
        }
    }
}

/**
 * How far a body has been built, as a band rather than a number.
 *
 * Three bands and not a curve, because this is what the record is written in: "you built a
 * metal world and you built it deep" is a sentence, and a body that is 31 levels deep instead of
 * 30 is not a different sentence.
 */
enum class Depth(val id: String, val label: String, val atLeast: Int) {
    JUNG("dp_young", "jung", 6),
    GEREIFT("dp_grown", "gereift", 18),
    VOLLENDET("dp_whole", "vollendet", 36),
    ;

    companion object {
        /** The deepest band this many levels reaches, or `null` below the first. */
        fun of(levels: Int): Depth? = entries.lastOrNull { levels >= it.atLeast }
    }
}

/**
 * One kind of world a body can turn out to be.
 *
 * The third pillar, and the one that makes the other two into a decision rather than a shop. The
 * impacts give material and the shells spend it; without this, spending it one way rather than
 * another is only a question of which number the player would prefer to be larger, and the answer
 * to that never changes across runs.
 *
 * A type is *recorded*, not bought. It costs nothing, it is not spent, and it cannot be lost — so
 * the reason to build a metal world this run and an ice world the next is that the ice world is
 * one you have not been yet. That is the whole loop: a collapse takes the body, and what it leaves
 * behind is the note that you were once this.
 */
data class WorldType(
    val id: String,
    val label: String,
    val lane: Lane,
    val depth: Depth,
    val flavor: String,
)

/**
 * The twelve worlds, and what having been them is worth.
 *
 * Four lanes by three depths, laid out as a grid on purpose: a player who has built two metal
 * worlds can see at a glance that the row is a row and that the thing missing from it is depth,
 * not a different material. A hand-written list of twelve unrelated names would hide that.
 */
object Worlds {

    /**
     * How much each recorded world adds to production, for good.
     *
     * Small, and it has to be: twelve of them is the whole collection, and a collection that ends
     * at a quarter more production is a nice thing to have finished rather than a wall the game
     * expects you to climb before the real content starts.
     */
    const val BONUS_EACH = 0.02

    /** How much of the body one shell must be before the world is named after it. */
    const val DOMINANT_SHARE = 0.45

    val all: List<WorldType> = buildList {
        for (lane in Lane.entries) {
            for (depth in Depth.entries) {
                add(
                    WorldType(
                        id = "wt_${lane.id.removePrefix("ln_")}_${depth.id.removePrefix("dp_")}",
                        label = labelOf(lane, depth),
                        lane = lane,
                        depth = depth,
                        flavor = flavorOf(lane, depth),
                    ),
                )
            }
        }
    }

    fun byId(id: String?): WorldType? = all.firstOrNull { it.id == id }

    fun of(lane: Lane, depth: Depth): WorldType =
        all.first { it.lane == lane && it.depth == depth }

    /** Whether the record is worth showing at all. Never in the shipped game; see [Dev]. */
    fun isUnlocked(state: GameState): Boolean =
        Dev.enabled && (state.worldTypes.isNotEmpty() || Accretion.isUnlocked(state))

    /**
     * Which lane the body is currently in.
     *
     * The layered answer is the default and the other three have to earn their way past it, which
     * is the right way round: a body is only a metal world once somebody decided to make it one.
     */
    fun laneOf(state: GameState): Lane {
        val leader = Shell.entries.maxByOrNull { Shells.levelOf(state, it) } ?: return Lane.GESCHICHTET
        if (Shells.shareOf(state, leader) < DOMINANT_SHARE) return Lane.GESCHICHTET
        return when (leader) {
            Shell.KERN -> Lane.METALL
            Shell.MANTEL -> Lane.GESTEIN
            Shell.KRUSTE -> Lane.EIS
        }
    }

    /**
     * What the body is right now, or `null` while it is still too shallow to be anything.
     *
     * Deliberately reads the *current* body rather than the deepest one ever built: the record is
     * written from this every tick, so a body that passes through "young metal world" on its way to
     * "grown metal world" records both. Nothing is missed by building past a band.
     */
    fun current(state: GameState): WorldType? {
        if (!Dev.enabled) return null
        val depth = Depth.of(Shells.total(state)) ?: return null
        return of(laneOf(state), depth)
    }

    /** Everything the player has ever been, in grid order. */
    fun recorded(state: GameState): List<WorldType> =
        all.filter { it.id in state.worldTypes }

    /** What the whole record is worth to production. */
    fun multiplier(state: GameState): Double {
        if (!Dev.enabled) return 1.0
        return 1.0 + BONUS_EACH * state.worldTypes.count { byId(it) != null }
    }

    private fun labelOf(lane: Lane, depth: Depth): String = when (lane) {
        Lane.METALL -> when (depth) {
            Depth.JUNG -> "Eisenkiesel"
            Depth.GEREIFT -> "Metallwelt"
            Depth.VOLLENDET -> "Eisenherz"
        }

        Lane.GESTEIN -> when (depth) {
            Depth.JUNG -> "Geröllhaufen"
            Depth.GEREIFT -> "Gesteinswelt"
            Depth.VOLLENDET -> "Titanenfels"
        }

        Lane.EIS -> when (depth) {
            Depth.JUNG -> "Frostklumpen"
            Depth.GEREIFT -> "Eiswelt"
            Depth.VOLLENDET -> "Gletscherleib"
        }

        Lane.GESCHICHTET -> when (depth) {
            Depth.JUNG -> "Mischling"
            Depth.GEREIFT -> "Schichtwelt"
            Depth.VOLLENDET -> "Dreiklang"
        }
    }

    private fun flavorOf(lane: Lane, depth: Depth): String = when (lane) {
        Lane.METALL -> when (depth) {
            Depth.JUNG -> "Schwer für seine Größe. Ein Magnet würde es merken."
            Depth.GEREIFT -> "Der Kern ist größer als alles, was darauf liegt."
            Depth.VOLLENDET -> "Fast nur Kern. Es klingt, wenn etwas darauf fällt."
        }

        Lane.GESTEIN -> when (depth) {
            Depth.JUNG -> "Lose zusammengehalten, aber es hält."
            Depth.GEREIFT -> "Ein Mantel, dick genug, um sich selbst zu tragen."
            Depth.VOLLENDET -> "Stein bis fast nach unten. Nichts hier bewegt sich schnell."
        }

        Lane.EIS -> when (depth) {
            Depth.JUNG -> "Außen hart, innen noch nicht entschieden."
            Depth.GEREIFT -> "Eine Kruste, unter der es sehr lange dunkel bleibt."
            Depth.VOLLENDET -> "Ein Panzer aus Eis, und darunter beinahe nichts."
        }

        Lane.GESCHICHTET -> when (depth) {
            Depth.JUNG -> "Von allem ein bisschen. Noch keine Entscheidung getroffen."
            Depth.GEREIFT -> "Drei Schichten, keine davon im Weg."
            Depth.VOLLENDET -> "Kern, Mantel, Kruste — und keine davon zu knapp."
        }
    }
}
