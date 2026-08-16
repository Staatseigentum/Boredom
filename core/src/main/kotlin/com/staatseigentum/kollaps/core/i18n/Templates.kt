package com.staatseigentum.kollaps.core.i18n

/**
 * Every sentence the game builds around a number.
 *
 * "×3 auf alles, dauerhaft" is not a text with a number in it — it is a *template* with a value in
 * it, and the difference decides whether this list has fifteen entries or six hundred. The
 * catalogue holds thirteen upgrades that each multiply everything by a different factor; rendered,
 * that is thirteen strings to translate that differ only in a digit. As a template it is one.
 *
 * ## Why they are listed here rather than gathered
 *
 * [Texts] walks the catalogues and asks each thing what it says. For a name that works, because a
 * name *is* the text. For an effect it does not: what comes back is the sentence already filled
 * in, which is the one form that must never be a translation key. So the templates are written
 * down once, here, and [EffectTemplateTest] is what keeps the list honest — it renders every
 * effect in the game in English and fails on any that came back German, which is exactly what
 * happens when a template is used in code and missing from here.
 *
 * ## The rule for writing one
 *
 * Translate the whole sentence, never the pieces. `"Erreiche %s in %s"` becomes
 * `"Reach %s within %s"` — word order is the thing that differs between the two languages, and a
 * sentence assembled from separately translated fragments can only ever have German word order
 * with English words in it.
 */
internal object Templates {

    /** Every template, in the order the systems they belong to unlock. */
    val all: List<String> = listOf(
        // ---- the shop
        "+%s kg pro Tipp",
        "%s Masse pro Tipp",
        "%s %s",
        "Jeder %s gibt %s +%s",
        "Jeder %s gibt allen Kollektoren +%s",
        "%s auf alles",
        "Tippen gibt zusätzlich %s deiner Produktion",
        "Offline-Ertrag auf %s",
        "Offline-Zeit zählt bis zu %s Stunden",
        // The mark upgrades: nine names and one sentence for two hundred rows. See Upgrade.compose.
        "%s Mk II",
        "%s Mk III",
        "%s Mk IV",
        "%s Mk V",
        "%s Mk VI",
        "%s Mk VII",
        "%s Mk VIII",
        "%s Mk IX",
        "%s Mk X",
        "Doppelte Leistung aus jedem %s.",

        // ---- the roles
        "%s Ausstoß",
        "%s Preis",
        "+%s auf alle anderen je %s Stück",

        // ---- everything permanent, in all three currencies
        "Offline-Ertrag mindestens %s",
        "Jeder freigeschaltete Kollektor startet mit %s Stück",
        "Start mit %s",
        "Kometen kommen %s so oft",
        "%s auf alles, dauerhaft",
        "%s Singularitäten je Kollaps",
        "%s Masse pro Tipp, dauerhaft",
        "Tippt %s× pro Sekunde von allein",
        "Jede Singularität gibt %s statt %s",
        "Kauft Kollektoren von allein, sobald du das Vierfache übrig hast",
        "Jeder Meilenstein gibt %s statt %s",
        "Jede Fusionsstufe läuft %s so schnell",
        "Forschung dauert nur noch %s der Zeit",
        "Galaxien wiegen %s so schwer",
        "Trabanten liefern %s",
        "Der Kollaps schmiedet %s Metall",
        "Jeder Auftrag zahlt %s Äonen extra",

        // ---- the challenges
        "Erreiche %s",
        "Erreiche %s in %s",
        "Kollektoren produzieren nichts",
        "Tippen bringt nichts, Kollektoren nur %s",
        "Keine Einschränkung",
        "Alles bringt nur %s",
        "Der Upgrade-Laden bleibt zu",
        "Nichts hält sich auf einer Bahn",
        "Keine Meilenstein-Boni",
        "Geschlossen zählt nicht",
        "Die Fusionskette bleibt kalt",
        "Die Galaxien tragen nichts bei",

        // ---- the chronicle
        "%s. Kollaps",
        "%s. Urknall",

        // ---- what a number is written in. See Numbers, which owns the separator and the
        // spacing; these are only the words.
        "unendlich",
        "%s Sek",
        "%s Min",
        "%s Std",
        "%s Std %s Min",
        "%s Tage",
        "%s Tage %s Std",
        "Namen",
        "Wissenschaftlich",
        "Kurzform",

        // ---- the updater and the save slots, which are interface but not a panel
        "Der Installer ließ sich nicht öffnen",
        "Die heruntergeladene Datei ist leer",
        "Eigene Version nicht lesbar",
        "Einstellungsseite nicht erreichbar",
        "GitHub hat die Anfrage abgelehnt. Später nochmal versuchen.",
        "Installation konnte nicht gestartet werden",
        "Keine Veröffentlichungen gefunden. Ist das Repository öffentlich?",
        "Leer — hier fängt ein neues Spiel an.",
        "Update-Prüfung fehlgeschlagen",
    )
}
