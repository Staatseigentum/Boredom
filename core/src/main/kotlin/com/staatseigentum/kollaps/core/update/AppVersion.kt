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

    /**
     * Whether the step from [from] to this one is a big one — a new major or minor number.
     *
     * `2.5.1` to `2.6.0` is big; `2.6.0` to `2.6.1` is not. The distinction is the same one the
     * versions have always been chosen by: a patch is work nobody can see, a minor is features.
     *
     * What hangs on it is whether the update can be put off. A patch can wait — nothing depends
     * on it. A feature release changes what the save contains and what the screen shows, and two
     * versions of the game passing the same exported block between them is a bug report nobody
     * can reproduce. So the big ones are not optional.
     *
     * False when this is not actually newer, so "big" never means "sideways" or "backwards".
     */
    fun isBigStepFrom(from: AppVersion): Boolean {
        if (this <= from) return false
        val mineMajor = numbers.getOrElse(0) { 0 }
        val theirsMajor = from.numbers.getOrElse(0) { 0 }
        if (mineMajor != theirsMajor) return true
        return numbers.getOrElse(1) { 0 } != from.numbers.getOrElse(1) { 0 }
    }

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
