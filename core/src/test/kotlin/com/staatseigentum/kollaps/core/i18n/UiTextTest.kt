package com.staatseigentum.kollaps.core.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Keeps the interface's list of texts honest by reading the interface.
 *
 * [UiTexts] is the one part of the translation that cannot be gathered at runtime: the screens are
 * in the app module, `:core` cannot see them, and it must never learn to. So the list is written
 * down — and a written-down copy of something a machine can read is a copy that goes stale on the
 * first busy afternoon. This is the machine reading it.
 *
 * Two failures, and they are different problems:
 *
 * - a `Lang.t("…")` in a screen whose text is not in the list. That text is not in [Texts], so the
 *   coverage test never asked for it, so it has no English and ships German with nobody noticing.
 *   This is the one that matters.
 * - an entry in the list that no screen says any more. Harmless to a player and untidy for
 *   everyone else: it keeps a translation alive for a sentence that was deleted, and the next
 *   person to read the file believes the game still says it.
 *
 * ## Why it reads the source rather than running the screens
 *
 * Running them would mean a Compose test, an Android runtime and a device — for a question about
 * string literals. The sources are right there and they are the truth about what the code says.
 */
class UiTextTest {

    /**
     * `Lang.t(` followed by a string literal, including one split across lines with `+`.
     *
     * Deliberately not a full Kotlin parse. It has to agree with the *generator* that produced
     * [UiTexts], and both of them only ever look at literals — anything cleverer here would find
     * texts the generator cannot, which would fail this test for a difference that is not a bug.
     */
    private val call = Regex("""Lang\.t\(\s*("(?:[^"\\]|\\.)*"(?:\s*\+\s*"(?:[^"\\]|\\.)*")*)""")

    private val literal = Regex(""""((?:[^"\\]|\\.)*)"""")

    private fun screens(): List<File> {
        // Tests run with the module directory as the working directory, so the app module is one
        // level up. Asserted rather than skipped: in this repository it is always there, and a
        // test that quietly passes when it cannot find its subject is worse than no test.
        val root = File("../app/src/main/kotlin/com/staatseigentum/kollaps/ui")
        assertTrue(root.isDirectory, "Die Oberflächenquellen liegen nicht unter ${root.absolutePath}")
        return root.listFiles { f -> f.extension == "kt" }?.sortedBy { it.name } ?: emptyList()
    }

    /** Every text a screen asks for, exactly as it will ask for it at runtime. */
    private fun asked(): Set<String> = buildSet {
        for (file in screens()) {
            val source = file.readText()
            for (match in call.findAll(source)) {
                val parts = literal.findAll(match.groupValues[1]).map { it.groupValues[1] }
                add(parts.joinToString("").unescape())
            }
        }
    }

    /** The escapes the generator wrote out, read back the way the compiler will read them. */
    private fun String.unescape(): String =
        replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\")

    @Test
    fun `every text a screen shows is listed`() {
        val listed = UiTexts.all.toSet()
        val missing = (asked() - listed).sorted()
        assertTrue(
            missing.isEmpty(),
            "${missing.size} Lang.t-Texte stehen nicht in UiTexts und werden darum nie " +
                "übersetzt: ${missing.take(5)}",
        )
    }

    @Test
    fun `nothing is listed that no screen says any more`() {
        val stale = (UiTexts.all.toSet() - asked()).sorted()
        assertTrue(
            stale.isEmpty(),
            "${stale.size} Einträge in UiTexts sagt kein Bildschirm mehr: ${stale.take(5)}",
        )
    }

    /** And the list has to be a list, not a bag with the same sentence in it twice. */
    @Test
    fun `no text is listed twice`() {
        val duplicated = UiTexts.all.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue(duplicated.isEmpty(), "Doppelte Einträge in UiTexts: ${duplicated.keys.take(5)}")
    }
}
