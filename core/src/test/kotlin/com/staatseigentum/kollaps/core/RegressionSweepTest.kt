package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    // ---------------------------------------------------------------- the collector cap

    /**
     * Five hundred is a limit, not a suggestion.
     *
     * Checked through every door rather than only through [GameEngine.buyCollector], because the
     * shop row, the Max button and the automation rule all reach the count by different routes and
     * a cap that only holds on one of them is not a cap.
     */
    @Test
    fun `no collector can be bought past the cap, whichever way it is asked`() {
        val collector = Collectors.all.first()
        val rich = GameState.new(0).copy(
            mass = 1e300,
            collectors = mapOf(collector.id to Collector.MAX_OWNED),
        )

        for (amount in BuyAmount.entries) {
            assertEquals(
                0,
                GameEngine.resolveAmount(collector, Collector.MAX_OWNED, rich.mass, amount),
                "$amount bietet noch etwas an, obwohl voll",
            )
            val after = GameEngine.buyCollector(rich, collector.id, amount)
            assertEquals(
                Collector.MAX_OWNED,
                after.ownedOf(collector.id),
                "$amount hat über den Deckel hinaus gekauft",
            )
            assertEquals(rich.mass, after.mass, "$amount hat Masse für nichts genommen")
        }
    }

    /** A bulk purchase stops exactly at the cap rather than overshooting and being refused. */
    @Test
    fun `a bulk purchase is trimmed to what is left rather than refused`() {
        val collector = Collectors.all.first()
        val nearly = GameState.new(0).copy(
            mass = 1e300,
            collectors = mapOf(collector.id to Collector.MAX_OWNED - 3),
        )

        val after = GameEngine.buyCollector(nearly, collector.id, BuyAmount.HUNDRED)
        assertEquals(Collector.MAX_OWNED, after.ownedOf(collector.id))
    }

    /**
     * A save from before the cap keeps what it has.
     *
     * Taking copies away would be the rules reaching backwards into a game already played, and a
     * negative amount on offer would be worse still.
     */
    @Test
    fun `a save from over the cap keeps its collectors and is simply offered nothing`() {
        val collector = Collectors.all.first()
        val over = Collector.MAX_OWNED + 120
        val legacy = GameState.new(0).copy(
            mass = 1e300,
            collectors = mapOf(collector.id to over),
        )

        val after = GameEngine.buyCollector(legacy, collector.id, BuyAmount.MAX)
        assertEquals(over, after.ownedOf(collector.id))

        val offer = GameEngine.collectorOffers(legacy, BuyAmount.MAX)
            .first { it.collector.id == collector.id }
        assertEquals(0, offer.amount)
        assertFalse(offer.affordable)
    }

    /** The last milestone lands on the last copy, so nothing points past the end. */
    @Test
    fun `the cap is a whole number of milestones and stops pointing forward`() {
        assertEquals(
            0,
            Collector.MAX_OWNED % Milestones.STEP,
            "Der Deckel liegt zwischen zwei Meilensteinen",
        )

        val collector = Collectors.all.first()
        val full = GameState.new(0).copy(collectors = mapOf(collector.id to Collector.MAX_OWNED))
        val offer = GameEngine.collectorOffers(full, BuyAmount.ONE)
            .first { it.collector.id == collector.id }
        assertNull(offer.nextMilestoneAt, "Der Shop zeigt einen Meilenstein hinter dem Deckel")
    }

    /** And the achievement behind the new palette only lands when every last one is full. */
    @Test
    fun `the full house achievement needs every collector, not just one`() {
        val id = "a_alle_voll"
        val one = Collectors.all.first()

        val partly = GameState.new(0).copy(
            collectors = mapOf(one.id to Collector.MAX_OWNED),
        )
        assertFalse(id in Achievements.newlyEarned(partly))

        val everything = GameState.new(0).copy(
            collectors = Collectors.all.associate { it.id to Collector.MAX_OWNED },
        )
        assertTrue(id in Achievements.newlyEarned(everything))
    }

    // ---------------------------------------------------------------- windfall ceiling

    /**
     * No single payout hands over more than three quarters of an hour.
     *
     * Checked over every catalogue at once rather than over the one that was too generous: the
     * event chains were the offenders, but comets and one-shot events pay from the same type, and
     * the next number somebody adds is as likely to be in one of those.
     */
    @Test
    fun `nothing pays more instant production than the ceiling`() {
        val payouts = buildList {
            for (comet in Comet.entries) add(comet.name to comet.reward)
            for (event in CosmicEvent.entries) {
                add(event.id to event.first.reward)
                add(event.id to event.second.reward)
            }
            for (chain in Chains.all) {
                for (station in chain.stations) {
                    add(station.id to station.first.reward)
                    add(station.id to station.second.reward)
                }
            }
        }

        assertTrue(payouts.isNotEmpty(), "Keine Belohnungen gefunden — der Test misst nichts")

        for ((where, reward) in payouts) {
            if (reward !is CometReward.Windfall) continue
            assertTrue(
                reward.secondsOfProduction <= CometReward.MAX_WINDFALL_SECONDS,
                "$where zahlt ${reward.secondsOfProduction} s, erlaubt sind " +
                    "${CometReward.MAX_WINDFALL_SECONDS}",
            )
        }
    }

    // ---------------------------------------------------------------- the record

    /**
     * A record is the *smallest* time, and zero means there is none yet.
     *
     * Both halves of that get written the wrong way round eventually — a plain `minOf` makes the
     * first collapse a record of nought seconds, which no run can ever beat.
     */
    @Test
    fun `the best run is the fastest one, and nothing is not a record`() {
        assertEquals(900.0, GameEngine.bestRunOf(best = 0.0, seconds = 900.0), "erster Lauf")
        assertEquals(800.0, GameEngine.bestRunOf(best = 900.0, seconds = 800.0), "schneller")
        assertEquals(800.0, GameEngine.bestRunOf(best = 800.0, seconds = 900.0), "langsamer")
        assertEquals(800.0, GameEngine.bestRunOf(best = 800.0, seconds = 0.0), "gar kein Lauf")
    }

    /** And it survives both resets, like every other lifetime number. */
    @Test
    fun `the record outlives a collapse and a big bang`() {
        val done = GameState.new(now).copy(
            runMass = Tiers.last.threshold * 2,
            runSeconds = 4_200.0,
            collapses = 40,
        )

        val after = GameEngine.collapse(done, now)
        assertEquals(4_200.0, after.bestRunSeconds)
        assertEquals(0.0, after.runSeconds, "Die Uhr des neuen Laufs läuft nicht bei null los")

        val banged = GameEngine.bigBang(after.copy(collapses = 40), now, "path_hand")
        assertEquals(4_200.0, banged.bestRunSeconds, "Der Urknall hat den Rekord vergessen")
    }

    /** Every slot's couplings are mutual — a one-sided resonance would pay only one of the two. */
    @Test
    fun `resonance is symmetric and slot seven really does couple to nothing`() {
        for (a in Orbits.all) {
            for (b in Orbits.couplingsOf(a)) {
                assertTrue(a in Orbits.couplingsOf(b), "Bahn ${a.index + 1} koppelt einseitig")
            }
        }
        val lonely = Orbits.all.filter { Orbits.couplingsOf(it).isEmpty() }
        assertEquals(
            listOf(6),
            lonely.map { it.index },
            "Andere Bahnen ohne Kopplung als erwartet — die Anzeige verspricht dann etwas Falsches",
        )
    }

    // ---------------------------------------------------------------- heat

    /** Tapping heats it, waiting cools it, and neither can leave the range. */
    @Test
    fun `heat builds on taps and decays on its own`() {
        var heat = 0.0
        repeat(40) { heat = Heat.afterTap(heat) }
        assertEquals(1.0, heat, "Vierzig Tipps reichen nicht für volle Hitze")

        // The half-life is the promise the constant makes; a test is the only place it is kept.
        val half = Heat.cooled(1.0, Heat.HALF_LIFE_SECONDS)
        assertTrue(half in 0.48..0.52, "Nach einer Halbwertszeit stehen $half statt der Hälfte")

        assertEquals(0.0, Heat.cooled(1.0, 300.0), "Hitze verschwindet nie ganz")
        assertEquals(0.0, Heat.cooled(0.0, 10.0))
    }

    /**
     * Being away is never worth more than being there.
     *
     * The one way this feature could be exploited: tap it hot, close the app, and collect hours of
     * production at a rate the body held for twenty seconds.
     */
    @Test
    fun `time away is credited cold`() {
        val hot = GameState.new(now).copy(
            collectors = mapOf(Collectors.all.first().id to 50),
            heat = 1.0,
            lastSeenAt = now - 3_600_000L,
        )
        val cold = hot.copy(heat = 0.0)

        val fromHot = GameEngine.applyOffline(hot, now)
        val fromCold = GameEngine.applyOffline(cold, now)

        assertEquals(fromCold.gained, fromHot.gained, "Hitze wurde offline mitbezahlt")
        assertEquals(0.0, fromHot.state.heat, "Die Hitze hat die Abwesenheit überlebt")
    }

    /** And it is worth exactly what it says on the tin, which is not much. */
    @Test
    fun `full heat is worth less than a third more`() {
        assertEquals(1.0, Heat.factor(0.0))
        assertTrue(
            Heat.factor(1.0) <= 1.35,
            "Volle Hitze gibt ${Heat.factor(1.0)} — so viel darf Anwesenheit nicht wert sein",
        )
    }

}
