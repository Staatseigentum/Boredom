package com.staatseigentum.kollaps.core.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * That every field the catalogues *display* is a field [Texts] actually *asks about*.
 *
 * ## The hole this closes
 *
 * The translation has one convention and it works: a catalogue stores `germanName`, and exposes
 * `val name get() = Lang.t(germanName)`. Every read site goes through the getter, so nothing can
 * quietly stay German at the point it is drawn.
 *
 * What that convention cannot do is notice a field nobody collects. [Texts] walks the catalogues
 * by hand — it has to, because `:core` cannot import the screens and a scanner would find ids and
 * log lines along with the words — and a walk written by hand is a walk with things missing from
 * it. Three fields were: an investment's per-level line, an automation rule's unit, and one that
 * turned out to be covered from another direction. All three were routed correctly, drawn
 * correctly, and never translated, because the coverage test only ever checks what the walk
 * returns. A green suite over a prestige panel with thirteen German lines in the middle of it.
 *
 * So this asks the question from the other end: here is every `german…` property that exists;
 * which of them does [Texts] never mention? It is a crude question and it is the right one — the
 * failure mode is *forgetting*, and you cannot catch forgetting by reading the thing that was
 * written.
 *
 * ## Why it reads the source
 *
 * The alternative is reflection over every catalogue object, which needs a list of the catalogues
 * — and that list going stale is the very thing being tested for.
 */
class TextFieldTest {

    /** A property declaration like `val germanFlavor: String`. */
    private val declaration = Regex("""\bval (german[A-Za-z]*)\b""")

    private fun coreSources(): List<File> {
        val root = File("src/main/kotlin/com/staatseigentum/kollaps/core")
        assertTrue(root.isDirectory, "Die Kernquellen liegen nicht unter ${root.absolutePath}")
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun `every german field a catalogue carries is gathered by Texts`() {
        val declared = buildSet {
            for (file in coreSources()) {
                if (file.path.contains("/i18n/")) continue
                for (match in declaration.findAll(file.readText())) add(match.groupValues[1])
            }
        }
        assertTrue(declared.isNotEmpty(), "Es wurde kein einziges german-Feld gefunden")

        val walk = File("src/main/kotlin/com/staatseigentum/kollaps/core/i18n/Texts.kt").readText()
        val ungathered = declared.filterNot { walk.contains(".$it") }.sorted()

        assertTrue(
            ungathered.isEmpty(),
            "${ungathered.size} Felder werden angezeigt, aber von Texts nie eingesammelt — sie " +
                "bleiben deutsch, ohne dass ein Test es merkt: $ungathered",
        )
    }
}
