package com.staatseigentum.kollaps.core.i18n

import com.staatseigentum.kollaps.core.Accretion
import com.staatseigentum.kollaps.core.Achievements
import com.staatseigentum.kollaps.core.AeonUpgrades
import com.staatseigentum.kollaps.core.Alloy
import com.staatseigentum.kollaps.core.AutomationRule
import com.staatseigentum.kollaps.core.Buff
import com.staatseigentum.kollaps.core.CatalogueFind
import com.staatseigentum.kollaps.core.Chains
import com.staatseigentum.kollaps.core.Challenge
import com.staatseigentum.kollaps.core.Collectors
import com.staatseigentum.kollaps.core.Comet
import com.staatseigentum.kollaps.core.Contract
import com.staatseigentum.kollaps.core.CosmicEvent
import com.staatseigentum.kollaps.core.Depth
import com.staatseigentum.kollaps.core.Element
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.FusionBonus
import com.staatseigentum.kollaps.core.GalaxyJob
import com.staatseigentum.kollaps.core.HeavyElement
import com.staatseigentum.kollaps.core.Investments
import com.staatseigentum.kollaps.core.Lane
import com.staatseigentum.kollaps.core.Lore
import com.staatseigentum.kollaps.core.Material
import com.staatseigentum.kollaps.core.Path
import com.staatseigentum.kollaps.core.PathTrees
import com.staatseigentum.kollaps.core.PrestigeUpgrades
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Role
import com.staatseigentum.kollaps.core.Shell
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.Tutorial
import com.staatseigentum.kollaps.core.Unlocks
import com.staatseigentum.kollaps.core.Upgrades
import com.staatseigentum.kollaps.core.Worlds
import com.staatseigentum.kollaps.core.pixel.Skins

/**
 * Every German text the game can put on a screen, gathered from the catalogues that own it.
 *
 * This exists to answer one question and it is the only question worth asking of a translation:
 * *what is still missing?* A translation without it is done when somebody has clicked through the
 * game and not noticed anything German, which is a different thing from done.
 *
 * ## Why it reads the catalogues rather than the source files
 *
 * Scanning the source for string literals would find them all, and it would also find log lines,
 * ids, format markers and the comments around them — and it would go on finding them after a text
 * had stopped being shown. Walking the catalogues finds exactly what the game can *display*, which
 * is what has to be translated, and it goes stale the moment a catalogue grows rather than the
 * moment somebody forgets to re-run a script.
 *
 * The cost is that a new catalogue has to be added here by hand. That is the point: the test that
 * reads this fails loudly for a catalogue nobody added, because its texts are then missing from
 * the count and the count is checked against [Translations].
 *
 * ## What is deliberately not in here
 *
 * The interface's own words — buttons, headings, the sentences that explain a mechanism — do not
 * live in a catalogue. They are written in the screens, and the screens are not in `:core`. Those
 * go through [Lang.t] at the point they are written and are listed in [UI_ONLY] so that this file
 * still knows the whole vocabulary.
 */
object Texts {

    /** Everything, deduplicated, in catalogue order. */
    val all: List<String> get() = (content + UI_ONLY).filter { it.isNotBlank() }.distinct()

    /** What is still untranslated. Empty is the whole goal. */
    val missing: List<String> get() = Lang.missing(all)

    /** Every text that comes out of a content catalogue. */
    private val content: List<String>
        get() = buildList {
            // ---- the ladder
            for (tier in Tiers.all) add(tier.germanFlavor)

            // ---- the fleet and the shop
            for (collector in Collectors.all) {
                add(collector.germanName)
                add(collector.germanFlavor)
            }
            for (upgrade in Upgrades.all) {
                add(upgrade.germanName)
                add(upgrade.germanFlavor)
            }
            for (role in Role.entries) {
                add(role.germanLabel)
                add(role.germanFlavor)
            }

            // ---- prestige, in all three currencies
            for (investment in Investments.all) {
                add(investment.germanName)
                add(investment.germanFlavor)
            }
            for (path in Path.entries) {
                add(path.germanLabel)
                add(path.germanFlavor)
            }
            for (node in PathTrees.all) {
                add(node.germanName)
                add(node.germanFlavor)
            }

            // ---- the systems that unlock one after another
            for (element in Element.entries) {
                add(element.germanLabel)
                add(element.germanFlavor)
            }
            for (stage in Fusion.stages) {
                add(stage.germanName)
                add(stage.germanFlavor)
            }
            for (bonus in FusionBonus.entries) add(bonus.germanLabel)
            for (metal in HeavyElement.entries) {
                add(metal.germanLabel)
                add(metal.germanFlavor)
            }
            for (alloy in Alloy.entries) {
                add(alloy.germanLabel)
                add(alloy.germanFlavor)
            }
            for (project in ResearchTree.all) {
                add(project.germanName)
                add(project.germanFlavor)
            }
            for (rule in AutomationRule.entries) {
                add(rule.germanLabel)
                add(rule.germanFlavor)
                for (option in rule.options) add(option.germanLabel)
            }
            for (job in GalaxyJob.entries) {
                add(job.germanLabel)
                add(job.germanFlavor)
            }

            // ---- everything that interrupts and asks something
            for (comet in Comet.entries) {
                add(comet.germanTitle)
                add(comet.germanFlavor)
            }
            for (buff in Buff.entries) add(buff.germanLabel)
            for (event in CosmicEvent.entries) {
                add(event.germanTitle)
                add(event.germanFlavor)
                add(event.first.germanLabel)
                add(event.first.germanFlavor)
                add(event.second.germanLabel)
                add(event.second.germanFlavor)
            }
            for (chain in Chains.all) {
                add(chain.germanTitle)
                for (station in chain.stations) {
                    add(station.germanTitle)
                    add(station.germanFlavor)
                    add(station.first.germanLabel)
                    add(station.first.germanFlavor)
                    add(station.second.germanLabel)
                    add(station.second.germanFlavor)
                }
            }
            for (find in CatalogueFind.all) {
                add(find.germanTitle)
                add(find.germanFlavor)
                add(find.germanFragment)
            }
            for (answer in com.staatseigentum.kollaps.core.FindAnswer.entries) {
                add(answer.germanLabel)
                add(answer.germanFlavor)
            }

            // ---- what the game asks of you
            for (contract in Contract.all) add(contract.germanTitle)
            for (achievement in Achievements.all) {
                add(achievement.germanName)
                add(achievement.germanFlavor)
            }

            // ---- the accretion update
            for (material in Material.entries) add(material.germanLabel)
            for (impact in Accretion.all) add(impact.germanLabel)
            for (shell in Shell.entries) {
                add(shell.germanLabel)
                add(shell.germanEffect)
            }
            for (lane in Lane.entries) add(lane.germanLabel)
            for (depth in Depth.entries) add(depth.germanLabel)
            for (world in Worlds.all) {
                add(world.germanLabel)
                add(world.germanFlavor)
            }

            // ---- what explains all of it
            for (step in Tutorial.steps) {
                add(step.germanTitle)
                add(step.germanText)
            }
            for (intro in Unlocks.all) {
                add(intro.germanTitle)
                add(intro.germanWhere)
                add(intro.germanText)
            }
            for (skin in Skins.all) {
                add(skin.germanName)
                add(skin.germanFlavor)
            }
        }

    /**
     * The words that belong to the screens rather than to a catalogue.
     *
     * Listed rather than gathered because the screens cannot be imported here — `:core` knows
     * nothing about Compose and must not start. Every entry is a string that appears verbatim
     * inside a [Lang.t] call somewhere in the interface, and `UiTextTest` in the app module is
     * what keeps the two in step: it reads the sources and fails on a `Lang.t` whose text is not
     * in here, and on an entry here that no screen uses any more.
     */
    val UI_ONLY: List<String> = UiTexts.all

    /**
     * The catalogues that still have to be routed through [Lang] before they can be listed above.
     *
     * Kept as a written list rather than as commented-out code, because commented-out code goes
     * stale silently and a list that names things is checked by `TextsCoverageTest`, which fails
     * once one of them *is* routed and nobody moved it into [content].
     *
     * Every one of them is a text the player can read today, in German, in both languages.
     */
    val PENDING: List<String> = listOf(
        "CelestialTier.name — the twenty-five body names",
        "PrestigeUpgrade / AeonUpgrade — name, flavor",
        "Challenge — title, flavor, ruleText, goalText",
        "Lore — the twenty-five ladder lines and the reset lines",
        "PrestigeEffect.text — every effect sentence, and the templates in them",
        "Upgrade / Role / PathNode / Alloy / Research effectText",
        "Contract — the status line and its units",
    )
}
