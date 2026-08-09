package com.staatseigentum.kollaps.core.pixel

import kotlin.math.roundToInt

/**
 * A colour scheme laid over the bodies.
 *
 * Everything in this game is generated, and the twenty-five bodies already have their own colours
 * — so a skin is a transform of those colours rather than a second set of them. That is what keeps
 * it honest: a scheme cannot make Mars stop being the reddest thing on the ladder or the black
 * hole stop being the darkest, it only decides what the whole ladder is made of.
 *
 * Two knobs, because two are enough to tell six schemes apart and a colour matrix would be a lot
 * of arithmetic nobody can predict the result of: how much of the original colour survives, and
 * what everything is pulled towards.
 */
data class Skin(
    val id: String,
    val name: String,
    val flavor: String,
    /** Achievements needed before it can be picked. Zero for the one everybody starts with. */
    val requiredAchievements: Int,
    /** How far towards grey the colours go first, in `0f..1f`. */
    val desaturation: Float = 0f,
    /** What is left is pulled towards this, by [tintStrength]. */
    val tint: Int = 0xFFFFFFFF.toInt(),
    val tintStrength: Float = 0f,
) {
    /** One colour, put through this scheme. Alpha is never touched. */
    fun apply(argb: Int): Int {
        if (desaturation <= 0f && tintStrength <= 0f) return argb

        val alpha = argb ushr 24 and 0xFF
        var r = argb shr 16 and 0xFF
        var g = argb shr 8 and 0xFF
        var b = argb and 0xFF

        if (desaturation > 0f) {
            // The usual luminance weights: an even third each turns a red and a blue of the same
            // brightness into two different greys, which reads as a mistake rather than a scheme.
            val grey = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
            r = lerp(r, grey, desaturation)
            g = lerp(g, grey, desaturation)
            b = lerp(b, grey, desaturation)
        }
        if (tintStrength > 0f) {
            r = lerp(r, tint shr 16 and 0xFF, tintStrength)
            g = lerp(g, tint shr 8 and 0xFF, tintStrength)
            b = lerp(b, tint and 0xFF, tintStrength)
        }
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerp(from: Int, to: Int, amount: Float): Int =
        (from + (to - from) * amount).roundToInt().coerceIn(0, 255)
}

/**
 * The schemes, and what they cost.
 *
 * Earned with achievements rather than with mass, because a palette is not a step forward — it is
 * something to look at, and the thing worth attaching it to is having played rather than having
 * spent. The thresholds are spread across the whole shelf so one is always a little way off.
 */
object Skins {

    val ORIGINAL = Skin(
        id = "skin_original",
        name = "Wie es ist",
        flavor = "Die Farben, die die Körper ohnehin haben.",
        requiredAchievements = 0,
    )

    val all: List<Skin> = listOf(
        ORIGINAL,
        Skin(
            id = "skin_kalt",
            name = "Kaltlicht",
            flavor = "Alles zwei Kelvin zu blau. So sieht es aus, wenn niemand zusieht.",
            requiredAchievements = 10,
            desaturation = 0.35f,
            tint = 0xFF6FA8FF.toInt(),
            tintStrength = 0.22f,
        ),
        Skin(
            id = "skin_rost",
            name = "Rost",
            flavor = "Eisen, überall, seit sehr langer Zeit.",
            requiredAchievements = 20,
            desaturation = 0.25f,
            tint = 0xFFC2611F.toInt(),
            tintStrength = 0.26f,
        ),
        Skin(
            id = "skin_gift",
            name = "Grünstich",
            flavor = "Ein Himmel, unter dem man besser nicht länger stehen bleibt.",
            requiredAchievements = 30,
            desaturation = 0.30f,
            tint = 0xFF63D66A.toInt(),
            tintStrength = 0.24f,
        ),
        Skin(
            id = "skin_asche",
            name = "Asche",
            flavor = "Was übrig bleibt, wenn die Farbe als Erstes geht.",
            requiredAchievements = 40,
            desaturation = 0.72f,
            tint = 0xFF3A3A44.toInt(),
            tintStrength = 0.18f,
        ),
        Skin(
            id = "skin_monochrom",
            name = "Ein Kanal",
            flavor = "Ein Bildschirm, der nur eine Farbe konnte, und es hat gereicht.",
            requiredAchievements = 55,
            desaturation = 1.0f,
            tint = 0xFF7FE08A.toInt(),
            tintStrength = 0.34f,
        ),
    )

    private val index: Map<String, Skin> = all.associateBy { it.id }

    init {
        require(index.size == all.size) { "Doppelte Paletten-ID" }
    }

    /** The scheme an id names, or the plain one — an unknown id is never a reason to fail. */
    fun byId(id: String?): Skin = index[id] ?: ORIGINAL

    fun isUnlocked(achievements: Int, skin: Skin): Boolean =
        achievements >= skin.requiredAchievements

    /** Everything earned so far, in threshold order. */
    fun unlocked(achievements: Int): List<Skin> = all.filter { isUnlocked(achievements, it) }

    /**
     * The scheme actually in force.
     *
     * A scheme that was chosen and is no longer unlocked — which nothing in the game does today,
     * but a save edited by hand can — falls back rather than showing something unearned.
     *
     * Takes the two numbers rather than a state: this package draws pictures and has managed to
     * know nothing about the game's state so far, which is what lets the renderer be exercised on
     * its own.
     */
    fun current(skinId: String?, achievements: Int): Skin {
        val chosen = byId(skinId)
        return if (isUnlocked(achievements, chosen)) chosen else ORIGINAL
    }
}
