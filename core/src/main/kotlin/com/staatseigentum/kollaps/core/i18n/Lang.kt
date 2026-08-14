package com.staatseigentum.kollaps.core.i18n

import kotlin.concurrent.Volatile

/** The languages the game speaks. */
enum class Language(val id: String, val label: String) {
    DE("de", "Deutsch"),
    EN("en", "English"),
    ;

    companion object {
        fun byId(id: String?): Language = entries.firstOrNull { it.id == id } ?: DE
    }
}

/**
 * Translation, keyed by the German text itself.
 *
 * The usual way round is a key per string — `upgrade_dust_1_name` and a table per language. That
 * was rejected here for a specific reason: this game has about seven hundred texts and every one
 * of them is written *inside* the content that owns it, next to the numbers it belongs to. Keys
 * would mean inventing seven hundred names, changing seven hundred definitions, and reading the
 * content catalogue from then on with the words missing.
 *
 * Using the German as the key costs one thing — two identical German strings can only have one
 * English translation — and buys three:
 *
 * - the content files stay readable, with the text where the thing is defined;
 * - a missing translation falls back to German instead of showing `upgrade_dust_1_name`, which is
 *   the difference between an unfinished translation and a broken screen;
 * - nothing can drift, because there is no second list of keys to keep in step.
 *
 * [current] is ambient for the same reason [com.staatseigentum.kollaps.core.Numbers.format] is:
 * text is written in hundreds of places and a language parameter in all of them would put a
 * display preference into the signature of the whole game. Nothing in the simulation reads it.
 */
object Lang {

    @Volatile
    var current: Language = Language.DE

    /** The text in the current language, or the German it was given if there is no translation. */
    fun t(german: String): String =
        if (current == Language.DE) german else Translations.EN[german] ?: german

    /**
     * A template with one value in it, e.g. `"+%s pro Tipp"`.
     *
     * Separate from [t] so that composite texts are translated as a whole sentence rather than
     * glued together from translated fragments — word order is exactly the thing that differs
     * between the two languages, and a sentence assembled from pieces can only ever have German
     * word order with English words in it.
     */
    fun t(german: String, vararg values: Any?): String {
        var out = t(german)
        for (value in values) out = out.replaceFirst("%s", value.toString())
        return out
    }

    /** Whether every text the game can show has an English version. */
    fun isComplete(germanTexts: Collection<String>): Boolean =
        germanTexts.all { it.isBlank() || it in Translations.EN }

    /** The ones still missing, for the test that gates the language switch. */
    fun missing(germanTexts: Collection<String>): List<String> =
        germanTexts.filter { it.isNotBlank() && it !in Translations.EN }.distinct()
}
