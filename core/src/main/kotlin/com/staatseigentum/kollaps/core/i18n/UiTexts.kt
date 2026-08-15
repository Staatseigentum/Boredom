package com.staatseigentum.kollaps.core.i18n

/**
 * The words that belong to the screens rather than to a content catalogue.
 *
 * Buttons, headings, the sentences that explain a mechanism, the empty-list apologies. They are
 * written where they are drawn, and where they are drawn is the app module — which `:core` cannot
 * see and must never learn to. So they are listed here, and the list is kept honest from the other
 * side: `UiTextTest` in the app module reads the interface sources and fails on a `Lang.t` whose
 * text is missing from here, and on an entry here that no screen says any more.
 *
 * That is the whole reason this file is a list of bare strings and not a set of constants. A
 * constant would have to be referenced from the screen, which means the screen would read
 * `Texts.BUY` instead of `Lang.t("Kaufen")` — and then the interface source stops saying what it
 * puts on the screen, which is the one property that makes this codebase readable.
 *
 * Filled as the screens are converted. Empty means no screen has been converted yet, which is a
 * true statement about the translation and not a missing file.
 */
internal object UiTexts {

    val all: List<String> = emptyList()
}
