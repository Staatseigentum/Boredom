package com.staatseigentum.kollaps.core

/** What one collector contributes right now, and what share of the whole that is. */
data class CollectorShare(
    val collector: Collector,
    val owned: Int,
    val output: Double,
    /** `0f..1f` of the total production. */
    val share: Float,
)

/** A line in the statistics list. */
data class StatLine(val label: String, val value: String)

/**
 * The numbers the save has always carried and never showed.
 *
 * Almost nothing here is new: taps, collapses, best tier and total mass were all being written
 * to disk from the first version. An idle game is a game about numbers going up, and not showing
 * the player their own is a strange thing to have done.
 */
object Statistics {

    fun lines(state: GameState): List<StatLine> {
        val stats = GameEngine.stats(state)
        return listOf(
            StatLine("Spielzeit", Numbers.formatDuration(state.playedSeconds.toLong())),
            StatLine("Tipps", Numbers.format(state.taps.toDouble())),
            StatLine("Masse insgesamt", Numbers.formatMass(state.totalMass)),
            StatLine("Bester Lauf", Numbers.formatMass(state.bestRunMass)),
            StatLine("Höchste Stufe", Tiers.all[state.bestTier].name),
            StatLine("Kollapse", Numbers.format(state.collapses.toDouble())),
            StatLine("Singularitäten", Numbers.format(state.singularities)),
            StatLine("Kometen gefangen", Numbers.format(state.cometsCaught.toDouble())),
            StatLine("Ereignisse entschieden", Numbers.format(state.eventsAnswered.toDouble())),
            StatLine("Erfolge", "${state.achievements.size} / ${Achievements.all.size}"),
            StatLine("Erfolgsbonus", Numbers.formatMultiplier(stats.achievementMultiplier)),
            StatLine("Kollektoren gesamt", Numbers.format(state.collectors.values.sum().toDouble())),
            StatLine("Upgrades im Lauf", "${state.upgrades.size} / ${Upgrades.all.size}"),
            StatLine("Prestige-Upgrades", "${state.prestigeUpgrades.size} / ${PrestigeUpgrades.all.size}"),
            StatLine("Herausforderungen", "${state.challengesDone.size} / ${Challenge.entries.size}"),
            StatLine("Urknalle", Numbers.format(state.bigBangs.toDouble())),
            StatLine("Äonen", Numbers.format(state.aeons)),
            StatLine("Äonen-Upgrades", "${state.aeonUpgrades.size} / ${AeonUpgrades.all.size}"),
            StatLine("Meilensteine", Numbers.format(
                state.collectors.values.sumOf { Milestones.reached(it) }.toDouble(),
            )),
        )
    }

    /**
     * Which collectors actually carry the run, largest first.
     *
     * The interesting answer is usually not the newest one: a shop full of things bought once
     * hides the fact that a hundred of something cheap is still doing most of the work.
     */
    fun shares(state: GameState): List<CollectorShare> {
        val owned = Collectors.all.filter { state.ownedOf(it.id) > 0 }
        if (owned.isEmpty()) return emptyList()

        val outputs = owned.map { collector ->
            collector to GameEngine.collectorOutput(state, collector)
        }
        val total = outputs.sumOf { it.second }
        return outputs
            .map { (collector, output) ->
                CollectorShare(
                    collector = collector,
                    owned = state.ownedOf(collector.id),
                    output = output,
                    share = if (total > 0.0) (output / total).toFloat() else 0f,
                )
            }
            .sortedByDescending { it.output }
    }
}
