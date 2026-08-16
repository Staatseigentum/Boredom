package com.staatseigentum.kollaps.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CometAndRuleTest {

    private val now = 1_700_000_000_000L

    // ------------------------------------------------------------------ comets

    @Test
    fun `every comet says what it is and what it pays`() {
        for (comet in Comet.entries) {
            assertTrue(comet.title.isNotBlank(), comet.id)
            assertTrue(comet.flavor.isNotBlank(), comet.id)
            assertTrue(comet.weight > 0, "${comet.title} kann nie erscheinen")
            assertTrue(comet.hits >= 1, "${comet.title} braucht keinen Treffer")
        }
        assertEquals(
            Comet.entries.size,
            Comet.entries.map { it.id }.toSet().size,
            "Doppelte Kometen-ID",
        )
    }

    /**
     * A hard core costs more attention, so it has to be worth more.
     *
     * Otherwise the correct play is to ignore anything that does not break on the first tap,
     * which would make the whole idea a punishment for noticing.
     */
    @Test
    fun `a core that takes more hits pays more than one that does not`() {
        val soft = Comet.entries.filter { it.hits == 1 }
        val hard = Comet.entries.filter { it.hits > 1 }
        assertTrue(hard.isNotEmpty(), "Es gibt keinen harten Kern")

        val softWindfall = soft.mapNotNull { (it.reward as? CometReward.Windfall)?.secondsOfProduction }
        val hardWindfall = hard.mapNotNull { (it.reward as? CometReward.Windfall)?.secondsOfProduction }
        if (softWindfall.isNotEmpty() && hardWindfall.isNotEmpty()) {
            assertTrue(
                hardWindfall.max() > softWindfall.max(),
                "Der harte Brocken zahlt nicht mehr als der weiche",
            )
        }

        // Compared against the buff on the same lever, not against the loudest number in the
        // enum: the frenzy is a hundredfold on taps and the inferno a fifteenfold on everything,
        // and those two numbers have nothing to say to each other.
        assertTrue(
            Buff.INFERNO.seconds * Buff.INFERNO.factor > Buff.SURGE.seconds * Buff.SURGE.factor,
            "Der Glutkern gibt keinen besseren Schub als der Sternwind",
        )
    }

    @Test
    fun `a rare comet is rarer than a common one`() {
        assertTrue(
            Comet.WINDFALL.weight > Comet.EMBER_CORE.weight,
            "Der seltenste ist nicht seltener als der häufigste",
        )
    }

    @Test
    fun `picking one always returns a comet, and eventually all of them`() {
        val random = Random(1234)
        val seen = mutableSetOf<Comet>()
        repeat(5_000) { seen += Comets.pick(random) }

        assertEquals(Comet.entries.toSet(), seen, "Ein Komet wird nie gezogen")
    }

    @Test
    fun `every buff a comet grants actually does something`() {
        val fleet = GameState(collectors = mapOf("dust" to 100), runMass = 1e9)
        val plain = GameEngine.massPerSecond(fleet)
        val plainTap = GameEngine.massPerTap(fleet)

        for (buff in Buff.entries) {
            val under = fleet.copy(buffId = buff.id, buffSecondsLeft = buff.seconds)
            val moved = GameEngine.massPerSecond(under) > plain ||
                GameEngine.massPerTap(under) > plainTap
            assertTrue(moved, "${buff.label} verändert nichts")
        }
    }

    @Test
    fun `catching a comet pays what it promised`() {
        val fleet = GameState(collectors = mapOf("dust" to 100), runMass = 1e9)

        val windfall = GameEngine.catchComet(fleet, Comet.ICE_CORE)
        assertTrue(windfall.mass > fleet.mass, "Der Eiskern zahlt nichts aus")
        assertEquals(1L, windfall.cometsCaught)

        val burning = GameEngine.catchComet(fleet, Comet.EMBER_CORE)
        assertEquals(Buff.INFERNO, burning.buff)
    }

    // ------------------------------------------------------------------ the new rules

    private fun underRule(challenge: Challenge): GameState = GameState(
        collapses = challenge.requiredCollapses,
        activeChallenge = challenge.id,
        mass = 1e22,
        runMass = Tiers.byName(Orbits.UNLOCK_TIER).threshold,
        bestTier = Tiers.last.index,
        collectors = mapOf("dust" to 200, "net" to 100),
    )

    /**
     * A shut shop has to be shut, not merely pointless.
     *
     * Taking the mass for an upgrade that then does nothing is a bug wearing a rule's clothes.
     */
    @Test
    fun `the upgrade rule closes the shop rather than emptying the shelves`() {
        val state = underRule(Challenge.ROHBAU)

        assertTrue(GameEngine.upgradeOffers(state).isEmpty(), "Der Laden hat noch offen")

        val anything = Upgrades.all.first()
        val tried = GameEngine.buyUpgrade(state, anything.id)
        assertEquals(state, tried, "Ein Upgrade wurde unter der Regel verkauft")
    }

    @Test
    fun `an upgrade already owned counts for nothing while the shop is shut`() {
        val boost = Upgrades.all.first { it.effect is UpgradeEffect.GlobalMultiplier }
        val free = underRule(Challenge.ROHBAU).copy(activeChallenge = null, upgrades = setOf(boost.id))
        val barred = free.copy(activeChallenge = Challenge.ROHBAU.id)

        assertTrue(GameEngine.massPerSecond(barred) < GameEngine.massPerSecond(free))
    }

    @Test
    fun `the orbit rule refuses both the slot and the body`() {
        val state = underRule(Challenge.ALLEIN)

        assertEquals(state, GameEngine.openOrbit(state), "Eine Bahn wurde unter der Regel geöffnet")

        val withSlots = state.copy(orbits = 2)
        assertEquals(
            withSlots,
            GameEngine.seedSatellite(withSlots, 0),
            "Ein Trabant wurde unter der Regel angesetzt",
        )
    }

    @Test
    fun `a system built before the rule counts for nothing under it`() {
        val built = underRule(Challenge.ALLEIN).copy(
            activeChallenge = null,
            orbits = 3,
            satellites = mapOf(0 to 1e9, 1 to 1e9, 2 to 1e9),
        )
        val barred = built.copy(activeChallenge = Challenge.ALLEIN.id)

        assertTrue(GameEngine.massPerSecond(barred) < GameEngine.massPerSecond(built))
    }

    /**
     * The rule is the only thing shutting the shop.
     *
     * Compared against the same run without the challenge rather than against one that has been
     * aborted: aborting wipes the run, and an empty shop afterwards would prove nothing except
     * that a player with no collectors has nothing to buy.
     */
    @Test
    fun `the shop is shut by the rule and by nothing else`() {
        val barred = underRule(Challenge.ROHBAU)
        val free = barred.copy(activeChallenge = null)

        assertTrue(GameEngine.upgradeOffers(barred).isEmpty(), "Der Laden hat unter der Regel offen")
        assertFalse(GameEngine.upgradeOffers(free).isEmpty(), "Ohne Regel ist der Laden auch leer")

        val anything = GameEngine.upgradeOffers(free).first().upgrade
        assertTrue(GameEngine.buyUpgrade(free, anything.id).owns(anything.id))
    }

    // ------------------------------------------------------------------ the catalogue

    @Test
    fun `every challenge is worth the trouble and reachable in order`() {
        for (challenge in Challenge.entries) {
            assertTrue(challenge.title.isNotBlank(), challenge.id)
            assertTrue(challenge.flavor.isNotBlank(), challenge.id)
            assertTrue(challenge.ruleText.isNotBlank(), challenge.title)
            assertTrue(challenge.goalText.isNotBlank(), challenge.title)
            assertTrue(challenge.reward.text.isNotBlank(), challenge.title)
            assertTrue(challenge.requiredCollapses >= 1, challenge.title)
        }
        assertEquals(
            Challenge.entries.size,
            Challenge.entries.map { it.id }.toSet().size,
            "Doppelte Herausforderungs-ID",
        )
    }

    @Test
    fun `every rule shows up in at least one challenge`() {
        val used = Challenge.entries.map { it.rule::class }.toSet()
        val handicaps = Challenge.entries.count { it.rule is ChallengeRule.Handicap }

        assertTrue(ChallengeRule.NoCollectors::class in used)
        assertTrue(ChallengeRule.NoTaps::class in used)
        assertTrue(ChallengeRule.NoUpgrades::class in used)
        assertTrue(ChallengeRule.NoOrbits::class in used)
        assertTrue(handicaps >= 1)
    }

    @Test
    fun `a challenge under a rule that hides its own goal is not offered`() {
        // Nothing may ask the player to reach a body the rule itself makes unreachable.
        for (challenge in Challenge.entries) {
            val goalTier = when (val goal = challenge.goal) {
                is ChallengeGoal.ReachTier -> goal.tierName
                is ChallengeGoal.ReachTierWithin -> goal.tierName
            }
            // Would throw on a body that is not on the ladder any more.
            Tiers.byName(goalTier)
        }
    }

    /**
     * However much prestige is stacked on the frequency, the sky keeps a floor of quiet.
     *
     * [GameEngine.cometFrequency] is a product of five independent multipliers and nothing ever
     * bounded the product. Fully built it reaches about thirty-eight, which put a comet on screen
     * two thirds of the time and fired one of the two comet sounds every nine seconds for the
     * rest of the session.
     */
    @Test
    fun `the gap between comets has a floor`() {
        val random = Random(7)
        for (frequency in listOf(1.0, 5.0, 20.0, 38.5, 1_000.0)) {
            repeat(50) {
                val delay = Comets.nextDelay(random, frequency)
                assertTrue(
                    delay >= Comets.MIN_GAP_SECONDS,
                    "Bei Frequenz $frequency kommt der nächste Komet nach ${delay}s",
                )
            }
        }

        // And the floor only binds where it is meant to: an ordinary sky is untouched.
        val plain = Comets.nextDelay(Random(7), frequency = 1.0)
        assertTrue(plain > Comets.MIN_GAP_SECONDS, "Der Boden greift schon ohne Prestige")
    }

    /** And a miss stops being news once they arrive that often. */
    @Test
    fun `a busy sky says nothing when one gets away`() {
        assertFalse(Comets.isBusySky(1.0), "Am Anfang wäre ein verpasster Komet keine Nachricht")
        assertFalse(Comets.isBusySky(5.0), "Fünffach ist noch kein Fließband")
        assertTrue(Comets.isBusySky(38.5), "Voll ausgebaut meldet sich jeder verpasste Komet")
    }
}
