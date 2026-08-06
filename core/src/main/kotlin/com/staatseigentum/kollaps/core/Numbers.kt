package com.staatseigentum.kollaps.core

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Formatting helpers for the very large numbers an idle game produces.
 *
 * Everything is rendered with German conventions (comma as decimal separator) and a fixed
 * locale so that the output does not change with the device settings.
 */
object Numbers {

    private val LOCALE: Locale = Locale.GERMANY

    /** Short-scale suffixes, one per power of 1000. */
    private val UNITS = listOf(
        "", "K", "Mio", "Mrd", "Bio", "Brd", "Tri", "Trd", "Qua", "Qrd",
        "Qui", "Qid", "Sex", "Sxd", "Sep", "Spd", "Okt", "Okd", "Non", "Nod",
    )

    /** Renders a mass value, e.g. `1,23 Mrd`. */
    fun format(value: Double): String {
        if (value.isNaN()) return "0"
        if (value.isInfinite()) return "∞"
        val v = if (value < 0.0) 0.0 else value
        if (v < 1_000.0) return formatBelowThousand(v)

        val group = floor(log10(v)).toInt() / 3
        if (group >= UNITS.size) return formatScientific(v)

        val mantissa = v / 10.0.pow(group * 3)
        return "${withSignificantDigits(mantissa)} ${UNITS[group]}"
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

    /** Renders a multiplier, e.g. `×2,5`. */
    fun formatMultiplier(value: Double): String {
        val rendered = if (value < 1_000.0) {
            if (abs(value - value.roundToLong()) < 1e-9) value.roundToLong().toString()
            else String.format(LOCALE, "%.2f", value)
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
            String.format(LOCALE, "%.1f", percent)
        }
        return "$rendered %"
    }

    private fun formatBelowThousand(v: Double): String {
        if (v == 0.0) return "0"
        if (v == floor(v)) return v.toLong().toString()
        return when {
            v >= 100.0 -> String.format(LOCALE, "%.0f", v)
            v >= 10.0 -> String.format(LOCALE, "%.1f", v)
            v >= 0.01 -> String.format(LOCALE, "%.2f", v)
            else -> String.format(LOCALE, "%.3f", v)
        }
    }

    private fun withSignificantDigits(mantissa: Double): String = when {
        mantissa >= 100.0 -> String.format(LOCALE, "%.0f", mantissa)
        mantissa >= 10.0 -> String.format(LOCALE, "%.1f", mantissa)
        else -> String.format(LOCALE, "%.2f", mantissa)
    }

    private fun formatScientific(v: Double): String {
        val exponent = floor(log10(v)).toInt()
        val mantissa = v / 10.0.pow(exponent)
        return String.format(LOCALE, "%.2fe%d", mantissa, exponent)
    }
}
