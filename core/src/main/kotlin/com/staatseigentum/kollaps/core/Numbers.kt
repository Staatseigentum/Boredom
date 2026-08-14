package com.staatseigentum.kollaps.core

import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/** How the very large numbers are written. */
enum class NumberFormat(val label: String, val example: String) {
    /** `1,23 Mrd` — reads as German and is what the game shipped with. */
    NAMEN("Namen", "1,23 Mrd"),

    /** `1,23e9` — the only one that stays the same width all the way up the ladder. */
    WISSENSCHAFTLICH("Wissenschaftlich", "1,23e9"),

    /** `1,23E9` — short, and the convention most other idle games use. */
    KURZ("Kurzform", "1,23E9"),
    ;

    companion object {
        fun byName(name: String?): NumberFormat =
            entries.firstOrNull { it.name == name } ?: NAMEN
    }
}

/**
 * Formatting helpers for the very large numbers an idle game produces.
 *
 * Everything is rendered with German conventions (comma as decimal separator) and a fixed
 * locale so that the output does not change with the device settings.
 *
 * [format] is the one piece of ambient state in this module, and it is here on purpose. Numbers
 * are written in about forty places — the header, every shop row, the statistics, and the effect
 * texts that content declares about itself — and threading a setting through all of them would put
 * a display preference into the signature of half the rules. It is set once, from the save, on the
 * way into the screen; nothing in the simulation reads it, so no rule can come out different
 * because of it.
 */
object Numbers {

    /** How numbers are written right now. Set from the save; only ever affects what is shown. */
    @Volatile
    var format: NumberFormat = NumberFormat.NAMEN

    /** Short-scale suffixes, one per power of 1000. */
    private val UNITS = listOf(
        "", "K", "Mio", "Mrd", "Bio", "Brd", "Tri", "Trd", "Qua", "Qrd",
        "Qui", "Qid", "Sex", "Sxd", "Sep", "Spd", "Okt", "Okd", "Non", "Nod",
    )

    /** Renders a mass value, e.g. `1,23 Mrd`. */
    fun format(value: Double): String = format(value, format)

    /** The same, in a format named explicitly. Everything else goes through here. */
    fun format(value: Double, using: NumberFormat): String {
        if (value.isNaN()) return "0"
        if (value.isInfinite()) return "unendlich"
        val v = if (value < 0.0) 0.0 else value
        // Below a thousand every format agrees, and writing `4,20e2` for four hundred and twenty
        // would be worse in all three.
        if (v < 1_000.0) return formatBelowThousand(v)

        return when (using) {
            NumberFormat.WISSENSCHAFTLICH -> formatScientific(v, "e")
            NumberFormat.KURZ -> formatScientific(v, "E")
            NumberFormat.NAMEN -> {
                val group = floor(log10(v)).toInt() / 3
                // Past the names the only honest thing left is the exponent.
                if (group >= UNITS.size) return formatScientific(v, "e")
                val mantissa = v / 10.0.pow(group * 3)
                "${withSignificantDigits(mantissa)} ${UNITS[group]}"
            }
        }
    }

    /** Renders a production rate, e.g. `1,23 Mrd kg/s`. */
    fun formatRate(value: Double): String = "${format(value)} kg/s"

    /** Renders a mass with its unit, e.g. `1,23 Mrd kg`. */
    fun formatMass(value: Double): String = "${format(value)} kg"

    /** Renders a duration as a compact German string, e.g. `2 Std 14 Min`. */
    fun formatDuration(seconds: Long): String {
        if (seconds < 60) return "$seconds Sek"
        val minutes = seconds / 60
        if (minutes < 60) return "$minutes Min"
        val hours = minutes / 60
        val restMinutes = minutes % 60
        if (hours < 24) {
            return if (restMinutes == 0L) "$hours Std" else "$hours Std $restMinutes Min"
        }
        val days = hours / 24
        val restHours = hours % 24
        return if (restHours == 0L) "$days Tage" else "$days Tage $restHours Std"
    }

    /** Renders a file size, e.g. `9,0 MB`. */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1_024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1_024
        var index = 0
        while (value >= 1_024 && index < units.lastIndex) {
            value /= 1_024
            index++
        }
        return "${fixed(value, if (value >= 100) 0 else 1)} ${units[index]}"
    }

    /** Renders a multiplier, e.g. `×2,5`. */
    fun formatMultiplier(value: Double): String {
        val rendered = if (value < 1_000.0) {
            if (abs(value - value.roundToLong()) < 1e-9) value.roundToLong().toString()
            else fixed(value, 2)
        } else {
            format(value)
        }
        return "×$rendered"
    }

    /** Renders a percentage, e.g. `15 %`. */
    fun formatPercent(fraction: Double): String {
        val percent = fraction * 100.0
        val rendered = if (abs(percent - percent.roundToLong()) < 1e-9) {
            percent.roundToLong().toString()
        } else {
            fixed(percent, 1)
        }
        return "$rendered %"
    }

    private fun formatBelowThousand(v: Double): String {
        if (v == 0.0) return "0"
        if (v == floor(v)) return v.toLong().toString()
        return when {
            v >= 100.0 -> fixed(v, 0)
            v >= 10.0 -> fixed(v, 1)
            v >= 0.01 -> fixed(v, 2)
            else -> fixed(v, 3)
        }
    }

    private fun withSignificantDigits(mantissa: Double): String = when {
        mantissa >= 100.0 -> fixed(mantissa, 0)
        mantissa >= 10.0 -> fixed(mantissa, 1)
        else -> fixed(mantissa, 2)
    }

    private fun formatScientific(v: Double, marker: String): String {
        val exponent = floor(log10(v)).toInt()
        val mantissa = v / 10.0.pow(exponent)
        return "${fixed(mantissa, 2)}$marker$exponent"
    }

    /**
     * A number with a fixed number of decimals, German-style, written out by hand.
     *
     * This was `String.format(Locale.GERMANY, "%.2f", …)`, and it was the last thing in the whole
     * of the rules that only exists on the JVM — which mattered the moment the same rules had to
     * compile for a phone that is not an Android one. The formatter is not much of a loss here:
     * every number that reaches this point is small by construction (a mantissa below ten, a file
     * size below a thousand, a value below a thousand), so the rounding is one multiplication and
     * a division with integers, and the comma was the only reason the locale was named at all.
     *
     * Rounds half up on the tenth, hundredth or thousandth, as the formatter did.
     */
    private fun fixed(value: Double, places: Int): String {
        val power = POWERS[places]
        // Half up, and negative values do not occur here: everything shown is a mass, a rate or a
        // share, and [format] has already floored the one place a negative could come from.
        val units = floor(value * power + 0.5).toLong()
        if (places == 0) return units.toString()
        return "${units / power}," + (units % power).toString().padStart(places, '0')
    }

    /** Ten to the power of nought through three — the only scales [fixed] is ever asked for. */
    private val POWERS = longArrayOf(1L, 10L, 100L, 1_000L)
}
