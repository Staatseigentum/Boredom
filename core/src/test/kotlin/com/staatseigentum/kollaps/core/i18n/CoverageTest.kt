package com.staatseigentum.kollaps.core.i18n

import com.staatseigentum.kollaps.core.Collectors
import com.staatseigentum.kollaps.core.Investments
import com.staatseigentum.kollaps.core.PathTrees
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.Upgrades
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * What the language switch is waiting for.
 *
 * The German is the key, so "is this translated" is a question that can simply be asked of every
 * catalogue — no list of keys to keep in step, no way for an entry to be quietly orphaned. This
 * collects everything the converted catalogues can put on screen and reports what is still German.
 *
 * It does **not** fail on missing entries. That would make an unfinished translation into a red
 * build for as long as it takes to finish, which helps nobody; what it does instead is print the
 * count, so the number going down is visible, and assert the things that would be *wrong* rather
 * than merely incomplete.
 */
class CoverageTest {

    @AfterTest
    fun reset() {
        Lang.current = Language.DE
    }

    /** Every German string the converted catalogues can show. */
    private fun everything(): List<String> = buildList {
        for (tier in Tiers.all) {
            add(tier.name)
            add(tier.flavor)
        }
        for (collector in Collectors.all) {
            add(collector.germanName)
            add(collector.germanFlavor)
        }
        for (upgrade in Upgrades.all) {
            add(upgrade.germanName)
            add(upgrade.germanFlavor)
        }
        for (project in ResearchTree.all) {
            add(project.germanName)
            add(project.germanFlavor)
        }
        for (investment in Investments.all) {
            add(investment.germanName)
            add(investment.germanFlavor)
        }
        for (node in PathTrees.all) {
            add(node.germanName)
            add(node.germanFlavor)
        }
    }

    @Test
    fun `report how much of the converted content is translated`() {
        val texts = everything().filter { it.isNotBlank() }.distinct()
        val missing = Lang.missing(texts)
        val done = texts.size - missing.size

        println("Übersetzt: $done von ${texts.size} (${missing.size} fehlen)")
        assertTrue(texts.size > 300, "Die Erhebung hat zu wenig gefunden — greift sie noch?")
    }

    /**
     * The one thing that must never be true.
     *
     * Switching language may change what is written; it may not change what is *there*. A blank
     * where a name used to be is worse than the German name.
     */
    @Test
    fun `nothing goes blank in english`() {
        Lang.current = Language.EN
        for (collector in Collectors.all) {
            assertTrue(collector.name.isNotBlank(), collector.id)
            assertTrue(collector.flavor.isNotBlank(), collector.id)
        }
        for (upgrade in Upgrades.all) {
            assertTrue(upgrade.name.isNotBlank(), upgrade.id)
            assertTrue(upgrade.effectText.isNotBlank(), upgrade.id)
        }
        for (tier in Tiers.all) {
            assertTrue(tier.label.isNotBlank(), tier.name)
        }
    }

    /**
     * The trap found while converting the tiers.
     *
     * Half the game refers to a rung by its German name. If the language ever changed what
     * `CelestialTier.name` returns, every one of those lookups would fail — and only in English.
     */
    @Test
    fun `a tier keeps its key whatever language is on`() {
        val german = Tiers.all.map { it.name }
        Lang.current = Language.EN
        assertTrue(Tiers.all.map { it.name } == german, "Die Stufennamen sind Schlüssel")
        assertTrue(Tiers.byName("Erde").index > 0, "Die Suche nach dem Namen ist gebrochen")
    }
}
