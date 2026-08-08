package com.staatseigentum.kollaps.core

/**
 * Turning what someone typed into something worth comparing.
 *
 * The game is written in German, and German is exactly the language where a literal `contains`
 * disappoints: a player hunting for "Erdöl" on a phone keyboard types "erdol", and a player who
 * remembers "Straße" types "strasse". Both are the same search as far as anyone asking is
 * concerned, and a shop that answers "nichts gefunden" to a word that is plainly on the screen
 * reads as broken rather than as strict.
 *
 * Folding both sides of the comparison through here is what makes that work. It is deliberately
 * not a general-purpose normaliser — no accent stripping for languages the game does not speak,
 * no stemming, nothing that would need a table. Lowercase, the three umlauts, the sharp s.
 */
object Search {

    /** Lowercased, with umlauts and ß folded onto their plain letters. */
    fun fold(text: String): String {
        val out = StringBuilder(text.length)
        for (character in text.lowercase()) {
            when (character) {
                'ä' -> out.append('a')
                'ö' -> out.append('o')
                'ü' -> out.append('u')
                'ß' -> out.append("ss")
                else -> out.append(character)
            }
        }
        return out.toString().trim()
    }
}
