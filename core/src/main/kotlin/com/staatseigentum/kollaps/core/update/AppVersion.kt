package com.staatseigentum.kollaps.core.update

/**
 * A dotted version number, tolerant about how it is written down.
 *
 * `1.2`, `v1.2.0` and `1.2.0-debug` all parse to the same numbers, because the version reaches
 * us from three different places — the release tag, the installed package and the build config —
 * and each of them spells it slightly differently.
 */
data class AppVersion(
    val numbers: List<Int>,
    val raw: String,
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val length = maxOf(numbers.size, other.numbers.size)
        for (position in 0 until length) {
            val mine = numbers.getOrElse(position) { 0 }
            val theirs = other.numbers.getOrElse(position) { 0 }
            if (mine != theirs) return mine.compareTo(theirs)
        }
        return 0
    }

    /** `1.2.0` — the numbers only, without a `v` prefix or a build suffix. */
    fun canonical(): String = numbers.joinToString(".")

    override fun toString(): String = raw

    companion object {
        /** Returns `null` when [text] carries no usable version number at all. */
        fun parse(text: String?): AppVersion? {
            if (text.isNullOrBlank()) return null
            val trimmed = text.trim()
            val withoutPrefix = trimmed.removePrefix("v").removePrefix("V")
            val digitsAndDots = withoutPrefix.takeWhile { it.isDigit() || it == '.' }
            val numbers = digitsAndDots
                .split('.')
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
            if (numbers.isEmpty()) return null
            return AppVersion(numbers, trimmed)
        }
    }
}
