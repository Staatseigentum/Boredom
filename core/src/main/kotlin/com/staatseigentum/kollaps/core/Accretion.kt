package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
import com.staatseigentum.kollaps.core.pixel.Skin
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * What a body is made of.
 *
 * Four, and not more, because they have to be told apart at a glance in a sprite seven pixels
 * across and named without a legend. Each one is also the start of something the game already
 * has: silicate becomes silicon in the fusion chain, metal becomes iron, and iron is what the
 * collapse forges gold out of. The early game was the one part of that story nobody told.
 */
enum class Material(val id: String, val germanLabel: String, val colour: Int) {
    EIS("mat_ice", "Eis", 0xFF6EC6FF.toInt()),
    SILIKAT("mat_silicate", "Silikat", 0xFF8E95C4.toInt()),
    METALL("mat_metal", "Metall", 0xFFFFB74D.toInt()),
    KOHLENSTOFF("mat_carbon", "Kohlenstoff", 0xFF7C5CFF.toInt()),
    ;

    val label: String get() = Lang.t(germanLabel)

    companion object {
        fun byId(id: String?): Material? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One thing on its way into you.
 *
 * [worth] is a share of a second's production rather than a fixed mass, for the same reason the
 * comets are: a number that is generous at the meteorite is a rounding error at the sun, and a
 * reward that stops mattering is worse than no reward.
 */
data class Impact(
    val id: String,
    val germanLabel: String,
    val material: Material,
    /** Seconds of current production this is worth. */
    val worth: Double,
    /** How many units of [material] it leaves behind. */
    val yield_: Int,
    /** Relative frequency among the impacts that can appear at all. */
    val weight: Int,
    /**
     * Whether ignoring it costs something.
     *
     * The game has never had a downside, and this is a very small one on purpose: what a missed
     * boulder takes is worth a few seconds. It exists to make the tap a decision rather than a
     * free reward, not to punish somebody who put the phone down.
     */
    val heavy: Boolean = false,
) {
    val label: String get() = Lang.t(germanLabel)
}

/**
 * Impacts: the inflow the early game never had.
 *
 * You are a rock and you grow because things hit you. The game said that nowhere and showed it
 * never — the first hour was a shop with a picture above it.
 *
 * ## Why it stops
 *
 * Above [LAST_TIER] impacts hand over to the comets that already exist. Two systems that both
 * mean "tap the thing crossing the screen" would be two systems competing for the same gesture,
 * and the comets are the better one once production is large enough for a windfall to matter.
 * So this one is the early half of a single idea rather than a second copy of it.
 */
object Accretion {

    /** The last rung on which anything still drifts in. Mars, where the comets take over. */
    const val LAST_TIER = 6

    /** Seconds between arrivals at the bottom of the ladder. */
    const val BASE_INTERVAL = 9.0

    /**
     * How much sooner the next one arrives per rung climbed.
     *
     * Gravity is the fiction and the mechanism at once: a bigger body sweeps up more, so the loop
     * feeds itself — climb a rung, get hit more often, build faster, climb again.
     */
    const val GRAVITY_STEP = 0.88

    /** The least time between two arrivals, however heavy you get. */
    const val MIN_INTERVAL = 3.0

    /**
     * How long one takes to fall in, in seconds.
     *
     * Twice as long as it needs to be to notice and half as long as it takes to be annoying. A
     * comet crosses in eleven because it is a windfall worth chasing; this is the ordinary
     * heartbeat of the early game, and something that has to be chased eleven seconds at a time
     * every nine seconds would be a job.
     */
    const val APPROACH_SECONDS = 4.0

    val all: List<Impact> = listOf(
        Impact(
            id = "im_dust",
            germanLabel = "Staubschwade",
            material = Material.SILIKAT,
            worth = 4.0,
            yield_ = 1,
            weight = 30,
        ),
        Impact(
            id = "im_shard",
            germanLabel = "Eisscherbe",
            material = Material.EIS,
            worth = 5.0,
            yield_ = 1,
            weight = 26,
        ),
        Impact(
            id = "im_nugget",
            germanLabel = "Metallklumpen",
            material = Material.METALL,
            worth = 7.0,
            yield_ = 1,
            weight = 18,
        ),
        Impact(
            id = "im_tar",
            germanLabel = "Teerbrocken",
            material = Material.KOHLENSTOFF,
            worth = 6.0,
            yield_ = 1,
            weight = 14,
        ),
        Impact(
            id = "im_boulder",
            germanLabel = "Felsbrocken",
            material = Material.SILIKAT,
            worth = 16.0,
            yield_ = 3,
            weight = 8,
            heavy = true,
        ),
        Impact(
            id = "im_core",
            germanLabel = "Kernfragment",
            material = Material.METALL,
            worth = 20.0,
            yield_ = 4,
            weight = 4,
            heavy = true,
        ),
    )

    fun byId(id: String?): Impact? = all.firstOrNull { it.id == id }

    /** Whether anything still drifts in at this point on the ladder. */
    fun isActive(state: GameState): Boolean =
        Rollout.accretion && GameEngine.tierOf(state).index <= LAST_TIER

    /**
     * Whether the panel is worth showing at all — which, once the update is live, is always.
     *
     * It was gated on having material or still being in range, and that was wrong in exactly one
     * case, which happens to be everybody who was already playing: a save halfway up the ladder
     * has no material, is long past Mars, and would have been shown nothing at all until its next
     * collapse. A player who installs an update and cannot find it has not been given it.
     *
     * There is no case left where the panel says nothing useful. Below Mars it is where the
     * material goes; above it, it says why nothing is arriving any more, what the layers already
     * built are still doing, and which of the twelve worlds are still missing.
     */
    fun isUnlocked(state: GameState): Boolean = Rollout.accretion

    /**
     * Seconds until the next arrival, at this point on the ladder.
     *
     * The mantle's pull is folded in here rather than at the one place that schedules them, so
     * there is exactly one answer to "how often does something arrive" and the panel that promises
     * it and the overlay that delivers it cannot disagree.
     */
    fun interval(state: GameState): Double {
        val rung = GameEngine.tierOf(state).index.coerceIn(0, LAST_TIER)
        val pulled = BASE_INTERVAL * GRAVITY_STEP.pow(rung) * Shells.gravityFactor(state)
        return pulled.coerceAtLeast(MIN_INTERVAL)
    }

    /** One of them, drawn by weight. */
    fun pick(random: Random): Impact {
        val total = all.sumOf { it.weight }
        var roll = random.nextInt(total)
        for (impact in all) {
            roll -= impact.weight
            if (roll < 0) return impact
        }
        return all.first()
    }

    /**
     * What absorbing one is worth in mass.
     *
     * Measured against production rather than against a table, and floored so that the very first
     * one — before a single collector exists — is still worth taking. Without the floor the whole
     * system would be invisible for the two minutes it matters most.
     */
    fun massOf(state: GameState, impact: Impact): Double {
        val perSecond = GameEngine.stats(state).massPerSecond
        return maxOf(impact.worth * perSecond, impact.worth * FLOOR_PER_SECOND)
    }

    /** What a heavy one takes if it is left alone. Deliberately small: see [Impact.heavy]. */
    fun lossOf(state: GameState, impact: Impact): Double =
        if (!impact.heavy) 0.0 else massOf(state, impact) * MISS_SHARE

    /** How much of a second's production the floor stands in for, before there is any. */
    private const val FLOOR_PER_SECOND = 0.6

    /** A missed boulder costs a fifth of what catching it would have paid. */
    private const val MISS_SHARE = 0.2
}

/**
 * The three shells a body is built out of, and what each one is for.
 *
 * The pillar the whole update stands on: for the first time the player decides *what* they are
 * becoming rather than only how big. Material is scarce within a run, the three shells want
 * different sorts of it, and the mix shows on the body itself.
 */
enum class Shell(
    val id: String,
    val germanLabel: String,
    /** What it mostly eats. Written as a share so a shell is cheaper when its sort is common. */
    val wants: Material,
    val second: Material,
    val germanEffect: String,
) {
    KERN(
        id = "sh_core",
        germanLabel = "Kern",
        wants = Material.METALL,
        second = Material.SILIKAT,
        germanEffect = "Produktion, und die Fusion startet heißer",
    ),
    MANTEL(
        id = "sh_mantle",
        germanLabel = "Mantel",
        wants = Material.SILIKAT,
        second = Material.KOHLENSTOFF,
        germanEffect = "Tippwert und Gravitation",
    ),
    KRUSTE(
        id = "sh_crust",
        germanLabel = "Kruste",
        wants = Material.EIS,
        second = Material.KOHLENSTOFF,
        germanEffect = "Offline-Anteil und Materialausbeute",
    ),
    ;

    val label: String get() = Lang.t(germanLabel)

    val effect: String get() = Lang.t(germanEffect)

    companion object {
        fun byId(id: String?): Shell? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Building the body out of what hit it.
 *
 * Every shell costs both of its materials — the one it is made of and one that binds it — so no
 * single sort of impact can build everything, and what the sky happens to throw at you nudges
 * what is cheap to build. The costs grow geometrically, which is the same shape the shop uses and
 * the one this game's players already read fluently.
 */
object Shells {

    /** The most any one shell can be raised to. Reached, not approached: it is a goal. */
    const val MAX_LEVEL = 20

    /** What the first level of a shell costs of its main material. */
    const val BASE_COST = 3.0

    /** What each further level multiplies that by. */
    const val COST_GROWTH = 1.35

    /** The binding material costs this share of the main one. */
    const val SECOND_SHARE = 0.5

    fun levelOf(state: GameState, shell: Shell): Int = state.shells[shell.id] ?: 0

    /** What raising [shell] by one costs right now, per material. */
    fun costOf(state: GameState, shell: Shell): Map<Material, Double> {
        val level = levelOf(state, shell)
        val main = BASE_COST * COST_GROWTH.pow(level)
        return mapOf(
            shell.wants to floor(main),
            shell.second to floor(main * SECOND_SHARE).coerceAtLeast(1.0),
        )
    }

    fun canBuild(state: GameState, shell: Shell): Boolean {
        if (!Rollout.accretion) return false
        if (levelOf(state, shell) >= MAX_LEVEL) return false
        return costOf(state, shell).all { (material, cost) -> amountOf(state, material) >= cost }
    }

    fun amountOf(state: GameState, material: Material): Double = state.materials[material.id] ?: 0.0

    /** Total depth, which is what the world types are read off. */
    fun total(state: GameState): Int = Shell.entries.sumOf { levelOf(state, it) }

    /**
     * A shell's share of the whole body, in `0f..1f`.
     *
     * The number the picture is drawn from and the world types are named after. An empty body is
     * an even third each rather than nothing, so a fresh run has a defined composition instead of
     * a division by zero — and so the first shell built visibly *shifts* something.
     */
    fun shareOf(state: GameState, shell: Shell): Double {
        val total = total(state)
        if (total <= 0) return 1.0 / Shell.entries.size
        return levelOf(state, shell).toDouble() / total
    }

    /** What the core multiplies production by. */
    fun productionFactor(state: GameState): Double = 1.0 + CORE_GAIN * levelOf(state, Shell.KERN)

    /** What the mantle multiplies a tap by. */
    fun tapFactor(state: GameState): Double = 1.0 + MANTLE_GAIN * levelOf(state, Shell.MANTEL)

    /** What the mantle does to how often something arrives. Below one is sooner. */
    fun gravityFactor(state: GameState): Double =
        (1.0 - MANTLE_PULL * levelOf(state, Shell.MANTEL)).coerceAtLeast(0.45)

    /** What the crust adds to the offline share, as a fraction of a fraction. */
    fun offlineBonus(state: GameState): Double = CRUST_OFFLINE * levelOf(state, Shell.KRUSTE)

    /** What the crust multiplies material yields by. */
    fun yieldFactor(state: GameState): Double = 1.0 + CRUST_YIELD * levelOf(state, Shell.KRUSTE)

    /**
     * The palette, bent towards whatever the body is mostly made of.
     *
     * This is the whole reason the composition is worth having on screen rather than in a panel:
     * a metal world *looks* like one. And it costs nothing to draw — [Skin.apply] already
     * desaturates and tints every pixel of every sprite, so bending the skin bends the body, the
     * satellites and the ladder pictures at once, with no renderer touched.
     *
     * Layered onto the player's chosen scheme rather than replacing it: the tint is mixed towards
     * the leading material and the strength is added to, so somebody playing in Asche still gets
     * Asche — a slightly icy Asche. A palette the player picked must never be overruled by
     * something they did not.
     *
     * Only what is built past an even third counts, so a fresh body is untinted and the first
     * shell is a visible change rather than the twentieth.
     */
    fun tintOver(base: Skin, state: GameState): Skin {
        if (!Rollout.accretion) return base
        val leader = Shell.entries.maxByOrNull { levelOf(state, it) } ?: return base
        if (levelOf(state, leader) <= 0) return base

        val even = 1.0 / Shell.entries.size
        val lead = ((shareOf(state, leader) - even) / (1.0 - even)).coerceIn(0.0, 1.0)
        if (lead <= 0.0) return base

        val strength = (TINT_MAX * lead).toFloat()
        return base.copy(
            tint = blend(base.tint, leader.wants.colour, base.tintStrength / (base.tintStrength + strength)),
            tintStrength = (base.tintStrength + strength).coerceAtMost(1f),
        )
    }

    /** [towards] at `0f` is all of [b]; at `1f` all of [a]. Alpha is taken from [a]. */
    private fun blend(a: Int, b: Int, towards: Float): Int {
        val keep = towards.coerceIn(0f, 1f)
        val alpha = a ushr 24 and 0xFF
        var packed = alpha shl 24
        for (shift in intArrayOf(16, 8, 0)) {
            val from = a shr shift and 0xFF
            val to = b shr shift and 0xFF
            val mixed = (to + (from - to) * keep).roundToInt().coerceIn(0, 255)
            packed = packed or (mixed shl shift)
        }
        return packed
    }

    /** How far the palette may be pulled by a body made entirely of one thing. */
    private const val TINT_MAX = 0.30

    private const val CORE_GAIN = 0.09
    private const val MANTLE_GAIN = 0.16
    private const val MANTLE_PULL = 0.018
    private const val CRUST_OFFLINE = 0.012
    private const val CRUST_YIELD = 0.07
}
