package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A sweep for the kinds of thing that only go wrong at an edge.
 *
 * Not aimed at any one feature. Every one of these asks a question that would have caught a real
 * defect in this codebase at some point: does everything survive a save, does a reset keep what it
 * promises, does anything go negative, is any catalogue quietly inconsistent.
 */
class RegressionSweepTest {

    private val now = 1_700_000_000_000L

    /** A save with something in every field. A dropped field silently resets on the next launch. */
    private fun rich() = GameState(
        mass = 1e30,
        runMass = 1e28,
        totalMass = 1e40,
        collectors = mapOf("dust" to 300, "net" to 120),
        roles = mapOf("dust" to Role.entries.first().id),
        upgrades = Upgrades.all.take(20).map { it.id }.toSet(),
        singularities = 4_000.0,
        collapses = 31,
        taps = 90_000,
        missedTaps = 12,
        achievements = Achievements.all.take(30).map { it.id }.toSet(),
        investments = mapOf(Investments.all.first().id to 4),
        aeons = 9.0,
        aeonUpgrades = setOf("ae_global_1"),
        bigBangs = 3,
        path = Path.KERN.id,
        pathNodes = setOf("path_core_root"),
        challengesDone = setOf(Challenge.entries.first().id),
        challengeDuos = setOf(Challenge.duoId(Challenge.entries.take(2).map { it.id })),
        chainsDone = setOf(Chains.all.first().id),
        research = setOf(ResearchTree.all.first().id),
        heavy = mapOf(HeavyElement.entries.first().id to 5.0),
        orbits = 3,
        skinId = "skin_rost",
        numberFormat = "KURZ",
        tutorialDone = true,
        statusOn = false,
        bestTier = 20,
    )

    @Test
    fun `every field of a populated save survives a round trip`() {
        val back = SaveCodec.decode(SaveCodec.encode(rich()))
        assertEquals(rich(), back, "Ein Feld geht beim Speichern verloren")
    }

    @Test
    fun `the exported block reads back as the same save`() {
        val back = SaveCodec.import(SaveCodec.export(rich()))
        assertEquals(rich(), back, "Der Kopieren-Knopf verliert etwas")
    }

    /** Nothing the player owns may be lost by a collapse except what the collapse is for. */
    @Test
    fun `a collapse keeps every permanent thing`() {
        val before = rich().copy(runMass = Tiers.last.threshold)
        val after = GameEngine.collapse(before, now)

        // Contains, not equals: a collapse crosses thresholds of its own, so the set grows.
        assertTrue(after.achievements.containsAll(before.achievements), "Ein Erfolg ging verloren")
        assertEquals(before.challengesDone, after.challengesDone)
        assertEquals(before.challengeDuos, after.challengeDuos)
        assertEquals(before.chainsDone, after.chainsDone)
        assertEquals(before.research, after.research)
        assertEquals(before.pathNodes, after.pathNodes)
        assertEquals(before.investments, after.investments)
        assertEquals(before.skinId, after.skinId)
        assertEquals(before.numberFormat, after.numberFormat)
        assertEquals(before.tutorialDone, after.tutorialDone)
        assertEquals(before.taps, after.taps)
        assertEquals(before.missedTaps, after.missedTaps)
        assertTrue(after.collapses > before.collapses)
    }

    @Test
    fun `a big bang keeps what it promises and takes what it says`() {
        val before = rich().copy(runMass = Tiers.last.threshold, collapses = 40)
        val after = GameEngine.bigBang(before, now)

        // Kept. Contains rather than equals, for the same reason the collapse test says so.
        assertTrue(after.achievements.containsAll(before.achievements), "Ein Erfolg ging verloren")
        assertEquals(before.challengesDone, after.challengesDone)
        assertEquals(before.aeonUpgrades, after.aeonUpgrades)
        assertEquals(before.pathNodes, after.pathNodes)
        assertEquals(before.heavy, after.heavy)
        assertEquals(before.skinId, after.skinId)

        // Taken.
        assertEquals(0, after.collapses)
        assertEquals(emptyMap(), after.investments, "Investitionen überleben den Urknall")
    }

    /** Nothing anywhere may go negative, whatever the state. */
    @Test
    fun `production and prices never go negative`() {
        val states = listOf(
            GameState.new(now),
            rich(),
            rich().copy(collectors = emptyMap(), upgrades = emptySet()),
            rich().copy(activeChallenges = setOf(Challenge.entries.first().id)),
        )
        for (state in states) {
            assertTrue(GameEngine.massPerSecond(state) >= 0.0, "Produktion negativ")
            assertTrue(GameEngine.massPerTap(state) >= 0.0, "Tippwert negativ")
            for (offer in GameEngine.collectorOffers(state, BuyAmount.ONE)) {
                assertTrue(offer.cost >= 0.0, "${offer.collector.id} kostet negativ")
            }
        }
    }

    /** A tick of zero, or of an absurd length, must not corrupt anything. */
    @Test
    fun `odd tick lengths are survivable`() {
        val state = rich()
        assertEquals(state.mass, GameEngine.tick(state, 0.0).mass, 1e-6)

        val long = GameEngine.tick(state, 86_400.0)
        assertTrue(long.mass.isFinite(), "Ein langer Tick hat die Masse zerstört")
        assertTrue(long.mass >= state.mass)
    }

    /** Every id a save can hold has to resolve, or a reload drops the thing silently. */
    @Test
    fun `no catalogue has a duplicate or unresolvable id`() {
        fun unique(name: String, ids: List<String>) =
            assertEquals(ids.size, ids.toSet().size, "$name hat eine doppelte Kennung")

        unique("Kollektoren", Collectors.all.map { it.id })
        unique("Upgrades", Upgrades.all.map { it.id })
        unique("Erfolge", Achievements.all.map { it.id })
        unique("Forschung", ResearchTree.all.map { it.id })
        unique("Investitionen", Investments.all.map { it.id })
        unique("Prestige", PrestigeUpgrades.all.map { it.id })
        unique("Äonen", AeonUpgrades.all.map { it.id })
        unique("Herausforderungen", Challenge.entries.map { it.id })
        unique("Pfad-Knoten", PathTrees.all.map { it.id })
        unique("Ketten", Chains.all.map { it.id })

        for (upgrade in Upgrades.all) {
            when (val effect = upgrade.effect) {
                is UpgradeEffect.CollectorMultiplier ->
                    assertTrue(Collectors.byId(effect.collectorId) != null, upgrade.id)
                is UpgradeEffect.CollectorSynergy -> {
                    assertTrue(Collectors.byId(effect.sourceId) != null, upgrade.id)
                    assertTrue(Collectors.byId(effect.targetId) != null, upgrade.id)
                }
                is UpgradeEffect.FleetSynergy ->
                    assertTrue(Collectors.byId(effect.sourceId) != null, upgrade.id)
                else -> Unit
            }
        }
    }

    /** Every text a player can read has to be there. An empty one is a hole on the screen. */
    @Test
    fun `nothing in any catalogue is blank`() {
        for (collector in Collectors.all) {
            assertTrue(collector.name.isNotBlank(), collector.id)
            assertTrue(collector.flavor.isNotBlank(), collector.id)
        }
        for (upgrade in Upgrades.all) {
            assertTrue(upgrade.name.isNotBlank(), upgrade.id)
            assertTrue(upgrade.effectText.isNotBlank(), upgrade.id)
        }
        for (achievement in Achievements.all) {
            assertTrue(achievement.name.isNotBlank(), achievement.id)
            assertTrue(achievement.flavor.isNotBlank(), achievement.id)
        }
        for (tier in Tiers.all) {
            assertTrue(tier.label.isNotBlank(), tier.name)
            assertTrue(tier.flavor.isNotBlank(), tier.name)
        }
    }

    /** Buying with nothing in hand must change nothing at all. */
    @Test
    fun `a purchase that cannot be afforded is a no-op`() {
        val broke = GameState.new(now)
        assertEquals(broke, GameEngine.buyCollector(broke, Collectors.all.first().id, BuyAmount.ONE))
        assertEquals(broke, GameEngine.buyUpgrade(broke, Upgrades.all.first().id))
        assertEquals(broke, GameEngine.buyPrestigeUpgrade(broke, PrestigeUpgrades.all.first().id))
        assertEquals(broke, GameEngine.buyAeonUpgrade(broke, AeonUpgrades.all.first().id))
    }

    /** An id nobody knows must be refused rather than crash or half-apply. */
    @Test
    fun `an unknown id changes nothing`() {
        val state = rich()
        assertEquals(state, GameEngine.buyCollector(state, "gibtsnicht", BuyAmount.ONE))
        assertEquals(state, GameEngine.buyUpgrade(state, "gibtsnicht"))
        assertEquals(state, GameEngine.buyPathNode(state, "gibtsnicht"))
        assertEquals(state, GameEngine.startChallenges(state, setOf("gibtsnicht"), now))
        assertEquals(state, GameEngine.startResearch(state, "gibtsnicht", now))
    }

    @Test
    fun `the achievement bonus matches what the list claims`() {
        val none = GameState.new(now)
        val all = none.copy(achievements = Achievements.all.map { it.id }.toSet())

        assertEquals(1.0, Achievements.multiplier(none), 1e-9)
        assertEquals(
            1.0 + Achievements.all.size * Achievements.BONUS_EACH,
            Achievements.multiplier(all),
            1e-9,
        )
    }

    /** A save from before a field existed has to load, which is the whole default-value promise. */
    @Test
    fun `a save with only the oldest fields still loads`() {
        val ancient = """{"version":1,"mass":1000.0,"runMass":1000.0,"taps":5}"""
        val loaded = SaveCodec.decode(ancient)

        assertTrue(loaded != null, "Ein alter Spielstand lässt sich nicht mehr lesen")
        assertEquals(1000.0, loaded.mass)
        assertFalse(loaded.tutorialDone, "Ein neues Feld kam nicht auf seinem Standard an")
    }
}
