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

    /**
     * Everywhere a text can be written, which is more than the screens.
     *
     * This used to be the one directory `app/.../ui`, and that was the reason the PC's update card
     * shipped German into 5.0.0: it says "Installiert: %s" and "Alles aktuell." from the desktop
     * module, the list never saw those, [Texts] therefore never asked for them, and the coverage
     * test was a clean green over a card nobody had translated. The view model and the two update
     * services were outside for the same reason and had the same hole.
     *
     * So the rule is now the honest one — anywhere in this repository that calls `Lang.t` with a
     * literal is somewhere a player can read that literal.
     */
    private fun screens(): List<File> {
        // Tests run with the module directory as the working directory, so the other modules are
        // one level up. Asserted rather than skipped: in this repository they are always there,
        // and a test that quietly passes when it cannot find its subject is worse than no test.
        val roots = listOf(
            File("../app/src/main/kotlin/com/staatseigentum/kollaps"),
            File("../desktop/src/main/kotlin/com/staatseigentum/kollaps"),
        )
        return roots.flatMap { root ->
            assertTrue(root.isDirectory, "Die Quellen liegen nicht unter ${root.absolutePath}")
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }.sortedBy { it.path }
    }

    /**
     * An `enum class` whose primary constructor takes a `german…` string — the display-label shape.
     *
     * Captures the entries block, which is everything from the opening brace to the `;` that ends
     * it. Enums written this way always have that semicolon, because they always have the getter
     * underneath that the whole convention exists for.
     */
    private val labelledEnum =
        Regex("""enum class \w+\([^)]*german\w*: String[^)]*\)\s*\{(.*?)\n\s*;""", RegexOption.DOT_MATCHES_ALL)

    /** One entry of such an enum: `COLLECTORS("Flotte"),`. */
    private val entry = Regex("""\n\s*[A-Z][A-Z_0-9]*\(([^)]*)\)""")

    /**
     * Every text a screen asks for, exactly as it will ask for it at runtime.
     *
     * Two shapes, because the interface says things two ways. `Lang.t("…")` at the point of use is
     * the common one. The other is a display label on an `enum` — `germanTitle` handed to the
     * constructor and read back through a getter that translates — and the literal there is just
     * as much a text on a screen, while looking nothing like a `Lang.t` call. The tab strips, both
     * of them, are that shape, and they are what shipped German tabs into 5.0.0.
     */
    private fun asked(): Set<String> = buildSet {
        for (file in screens()) {
            val source = file.readText()
            for (match in call.findAll(source)) {
                val parts = literal.findAll(match.groupValues[1]).map { it.groupValues[1] }
                add(parts.joinToString("").unescape())
            }
            for (block in labelledEnum.findAll(source)) {
                for (line in entry.findAll(block.groupValues[1])) {
                    for (text in literal.findAll(line.groupValues[1])) {
                        add(text.groupValues[1].unescape())
                    }
                }
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

    /**
     * The one thing about this codebase that is wrong even when it looks right.
     *
     * `BODY(Lang.t("Körper"))` reads correctly and is a bug: an enum's constructor arguments are
     * evaluated once, when the class is loaded, so that label is whatever language the game
     * started in and stays that for the rest of the session. Switching to English relabels the
     * whole interface and leaves those in German, which is worse than never translating them —
     * a text that is German on a German screen is a gap, and one German tab in an English strip
     * is a fault.
     *
     * It is also invisible to every other check here: it *is* a `Lang.t` call with a literal, so
     * the list has the text and the coverage test has the translation. Only the moment it runs is
     * wrong. Hence a rule about where the call may appear rather than about what it says: never
     * inside an enum entry, always in the getter underneath.
     */
    @Test
    fun `no enum freezes its label into a constructor argument`() {
        val frozen = buildList {
            for (file in screens()) {
                // `NAME(` at the start of a line, with a Lang.t somewhere in its arguments. Enum
                // entries are the only thing in this codebase written in that shape.
                val bad = Regex("""\n\s*[A-Z][A-Z_0-9]*\([^)\n]*Lang\.t\(""").findAll(file.readText())
                for (match in bad) add("${file.name}: ${match.value.trim()}…")
            }
        }
        assertTrue(
            frozen.isEmpty(),
            "${frozen.size} Enum-Einträge übersetzen im Konstruktor und frieren damit die " +
                "Startsprache ein — stattdessen `private val germanX` plus Getter: $frozen",
        )
    }

    /**
     * That a file saying `Lang.t` has actually imported it.
     *
     * A compiler question, and it is here because for four of these files there is no compiler
     * within reach. `:desktop` builds most of the app module from the same sources, which is what
     * catches a mistake in a screen before it is ever pushed — but the view model, the two update
     * services and the phone's update card are excluded from that build, and `:app` needs the
     * Android SDK. So those four are only ever compiled by CI, and a missing import in them costs
     * a full round trip to find out.
     *
     * That is exactly what happened: the view model got six `Lang.t` calls and no import, the
     * desktop build was green because it never reads that file, and CI failed on it minutes later.
     * Reading for the import takes no compiler and catches the whole class before the push.
     */
    @Test
    fun `every file that translates has imported the translator`() {
        val without = screens().filter { file ->
            val source = file.readText()
            source.contains("Lang.t(") &&
                !source.contains("import com.staatseigentum.kollaps.core.i18n.Lang")
        }
        assertTrue(
            without.isEmpty(),
            "${without.size} Dateien rufen Lang.t auf, ohne Lang zu importieren: " +
                without.map { it.name },
        )
    }

    /** And the list has to be a list, not a bag with the same sentence in it twice. */
    @Test
    fun `no text is listed twice`() {
        val duplicated = UiTexts.all.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue(duplicated.isEmpty(), "Doppelte Einträge in UiTexts: ${duplicated.keys.take(5)}")
    }
}
