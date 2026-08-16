package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The ladder above the black hole: every body again, six hundred and seventy-six times each. */
class DesignationTest {

    /** A full sky, which is what the catalogue ladder waits behind. */
    private fun fullSky(): List<ParkedUniverse> = (0 until Multiverse.SLOTS).map {
        ParkedUniverse(slot = it, bestTier = Tiers.all.lastIndex, collapses = 20)
    }

    /**
     * A save on the catalogue ladder.
     *
     * The run clock is set as well as the mass, because a designation asks for both — see
     * [Designations.forRun]. A year of play is past every rung any of these tests reach.
     */
    private fun deep(mass: Double): GameState = GameState.new(0).copy(
        bigBangs = Multiverse.SLOTS,
        universes = fullSky(),
        runMass = mass,
        runSeconds = 365.0 * 86_400.0,
        collectors = mapOf("dust" to 100),
    )

    @Test
    fun `designations read as a catalogue`() {
        assertEquals("AA", Designations.label(0))
        assertEquals("AB", Designations.label(1))
        assertEquals("AZ", Designations.label(25))
        assertEquals("BA", Designations.label(26))
        assertEquals("ZZ", Designations.label(Designations.PER_BODY - 1))
    }

    @Test
    fun `every body gets its own run of designations`() {
        // The whole point of the shape: not one body catalogued and then a jump, but all of them.
        for (body in Tiers.all) {
            val first = Designations.at(Designations.FIRST_INDEX + body.index * Designations.PER_BODY)
            val last = Designations.at(
                Designations.FIRST_INDEX + (body.index + 1) * Designations.PER_BODY - 1,
            )
            assertEquals(body.name, first.name, "Der Block bei ${body.name} fängt woanders an")
            assertEquals(body.name, last.name, "Der Block bei ${body.name} hört woanders auf")
            assertEquals("${body.label} AA", first.label)
            assertEquals("${body.label} ZZ", last.label)
        }
        assertEquals(Tiers.all.size * Designations.PER_BODY, Designations.COUNT)
    }

    @Test
    fun `the ladder is locked until the sky is full`() {
        val far = Tiers.last.threshold * 1_000.0

        // Seven galaxies is not a full sky, however many big bangs the counter claims.
        val shallow = GameState.new(0).copy(
            bigBangs = Multiverse.SLOTS,
            universes = fullSky().dropLast(1),
            runMass = far,
        )
        assertEquals(Tiers.last, Tiers.forState(shallow), "Die Kennungen sind zu früh offen")
        assertNull(Tiers.next(Tiers.last), "Ohne Freischaltung darf über dem Loch nichts stehen")

        val earned = deep(far)
        assertTrue(Tiers.forState(earned).isDesignated, "Die Kennungen bleiben zu")
        assertNotNull(Tiers.next(Tiers.last, deep = true))
    }

    @Test
    fun `the rungs climb in both mass and production`() {
        var previous = Tiers.last
        var index = Designations.FIRST_INDEX
        repeat(400) {
            val rung = Designations.at(index)
            assertTrue(rung.threshold > previous.threshold, "Stufe $index verlangt nicht mehr")
            assertTrue(
                rung.productionMultiplier > previous.productionMultiplier,
                "Stufe $index bringt nicht mehr",
            )
            previous = rung
            index++
        }
    }

    @Test
    fun `mass finds the same rung the ladder says it should`() {
        // Walking the thresholds and taking the logarithm have to agree, because the logarithm is
        // the only one that actually runs and there are sixteen thousand chances for it to be off
        // by one.
        for (step in listOf(1, 2, 3, 50, 677, 1_000, 5_000, Designations.COUNT - 1)) {
            val rung = Designations.at(Designations.FIRST_INDEX + step - 1)
            assertEquals(rung.index, Designations.forNominalMass(rung.threshold).index, "bei Schritt $step")
            assertEquals(
                rung.index,
                Designations.forNominalMass(rung.threshold * 1.001).index,
                "knapp über Schritt $step",
            )
            val below = Designations.forNominalMass(rung.threshold * 0.999).index
            assertTrue(below < rung.index, "knapp unter Schritt $step liegt nicht darunter")
        }
    }

    @Test
    fun `the whole ladder stays inside a Double`() {
        val top = Designations.at(Designations.TOTAL - 1)
        assertTrue(top.threshold.isFinite(), "Die oberste Stufe verlangt unendlich viel")
        assertTrue(top.productionMultiplier.isFinite(), "Die oberste Stufe bringt unendlich viel")
        assertNull(Tiers.next(top), "Über der obersten Stufe steht noch etwas")
        // And nothing above it, however much mass is thrown at it.
        assertEquals(top.index, Designations.forNominalMass(Double.MAX_VALUE).index)
    }

    @Test
    fun `a designated rung is still the body it is`() {
        val saturn = Tiers.indexOf("Saturn")
        val catalogued = Designations.at(Designations.FIRST_INDEX + saturn * Designations.PER_BODY + 5)

        // Everything that points at a rung points at it by name, and must keep finding the real
        // one rather than one of its six hundred and seventy-six catalogue entries.
        assertEquals(saturn, Tiers.indexOf("Saturn"), "Der Name zeigt jetzt auf eine Kennung")
        assertEquals("Saturn", catalogued.name)
        assertEquals(Tiers.byName("Saturn").kind, catalogued.kind)
        assertEquals(Tiers.byName("Saturn").primaryColor, catalogued.primaryColor)
        assertFalse(catalogued.isFinal, "Eine Kennungsstufe gibt sich als Ende der Leiter aus")
    }

    @Test
    fun `reaching a catalogue rung actually pays more`() {
        val atHole = deep(Tiers.last.threshold)
        val farUp = deep(Tiers.last.threshold * 1e6)

        assertTrue(
            GameEngine.massPerSecond(farUp) > GameEngine.massPerSecond(atHole),
            "Über dem Loch bringt die Flotte nicht mehr als darauf",
        )
        assertTrue(GameEngine.tierOf(farUp).isDesignated)
        assertEquals(GameEngine.tierOf(farUp).label, GameEngine.stats(farUp).tier.label)
    }

    @Test
    fun `a record set on the catalogue ladder is recorded`() {
        val credited = GameEngine.tick(deep(Tiers.last.threshold * 1e8), 1.0)
        assertTrue(
            credited.bestTier >= Designations.FIRST_INDEX,
            "Die Bestmarke bleibt beim Schwarzen Loch stehen",
        )
    }

    @Test
    fun `the collapse gate stays at the black hole`() {
        // The ladder growing past it must not move where a collapse becomes possible, or every
        // save above the hole would suddenly be unable to collapse at all.
        assertTrue(Tiers.last.isFinal)
        assertEquals(Tiers.all.lastIndex, Tiers.last.index)
        val far = deep(Tiers.last.threshold * 1e10)
        assertTrue(GameEngine.canCollapse(far), "Über dem Loch lässt sich nicht mehr kollabieren")
    }

    /**
     * The catalogue is priced in time, and that is the whole of its difficulty.
     *
     * It used to be priced in kilograms, and a bot built to the point where the ladder unlocks
     * walked the entire AA–ZZ round — two thousand seven hundred rungs — in three seconds. No
     * threshold could have fixed that: thresholds are a geometric series bounded by what a
     * `Double` holds, and production is bounded by nothing at all.
     */
    @Test
    fun `a rung is a wait, and production does not shorten it`() {
        val step = 200
        val slow = GameState.new(0).copy(
            bigBangs = Multiverse.SLOTS,
            universes = fullSky(),
            collectors = mapOf("dust" to 10),
            runSeconds = Designations.secondsFor(step) * 1.001,
            runMass = Designations.massFor(step) * 1.001,
        )
        val fast = slow.copy(collectors = Collectors.all.associate { it.id to 500 })

        assertTrue(GameEngine.massPerSecond(fast) > GameEngine.massPerSecond(slow) * 1_000)
        assertEquals(
            Tiers.forState(slow).index,
            Tiers.forState(fast).index,
            "Die tausendfache Produktion kauft Sprossen — dann pacet sich die Leiter nicht selbst",
        )
    }

    /**
     * Time away counts towards the climb, which is what makes this an idle game still.
     *
     * The ladder is priced in [GameState.runSeconds], and until this was wired the clock only ran
     * while the app was open — a five-day ladder would have meant five days of staring at it.
     */
    @Test
    fun `the run clock runs while the app is shut`() {
        val start = GameState.new(0).copy(
            bigBangs = Multiverse.SLOTS,
            universes = fullSky(),
            collectors = mapOf("dust" to 100),
            runMass = Designations.massFor(50),
            runSeconds = Designations.secondsFor(1),
            // A real moment: `applyOffline` treats a zero here as "never seen" and bails.
            lastSeenAt = 1_700_000_000_000L,
        )
        val day = 1_700_000_000_000L + 24L * 3_600 * 1_000
        val after = GameEngine.applyOffline(start, day).state

        assertTrue(
            after.runSeconds >= start.runSeconds + 86_000,
            "Ein Tag Abwesenheit bringt der Laufuhr nur ${after.runSeconds - start.runSeconds}s",
        )
        assertTrue(
            Tiers.forState(after).index > Tiers.forState(start).index,
            "Ein Tag Abwesenheit bringt keine einzige Sprosse",
        )
    }

    /** And a full round of designations is measured in days of play rather than in seconds. */
    @Test
    fun `one full round of the catalogue takes days`() {
        val days = Designations.secondsFor(Designations.PER_BODY) / 86_400.0
        assertTrue(days > 2.0, "Eine volle AA-ZZ-Runde dauert nur %.2f Tage".format(days))
        assertTrue(
            days < 30.0,
            "Eine volle AA-ZZ-Runde dauert %.1f Tage — das ist keine Leiter mehr".format(days),
        )
    }

    /**
     * A run on a full sky, holding plenty of mass, with the clock wherever the test wants it.
     *
     * Which is what a save looks like the second after a collapse: the head start hands the mass
     * straight back and only the clock actually starts over.
     */
    private fun afterCollapse(seconds: Double): GameState = GameState.new(0).copy(
        bigBangs = Multiverse.SLOTS,
        universes = fullSky(),
        collectors = mapOf("dust" to 100),
        runMass = Designations.massFor(500),
        runSeconds = seconds,
    )

    /**
     * The bar reads the gate that is actually holding, and says so in that gate's unit.
     *
     * This is the screen it fixes. Collapse on the catalogue ladder and the head start puts the
     * run's mass hundreds of rungs past what the next designation asks for while the clock is back
     * at zero. Drawn from mass alone the bar stood full at "noch 0 kg" and stayed there for a
     * minute — an interface reporting that nothing is happening while something is.
     */
    @Test
    fun `after a collapse the catalogue bar counts the wait, not the mass`() {
        val stats = GameEngine.stats(afterCollapse(0.0))

        assertEquals(Tiers.last.index, stats.tier.index, "Ohne Laufzeit steht der Lauf am Loch")
        assertTrue(stats.tierRemainingIsTime, "Der Rest steht in Kilogramm statt in Sekunden")
        assertEquals(Designations.ENTRY_SECONDS, stats.tierRemaining, 1.0)
        assertEquals(0f, stats.tierProgress, 0.001f)
    }

    /** And it fills as the wait is served, which is the whole point of a bar. */
    @Test
    fun `the catalogue bar fills as the wait is served`() {
        assertEquals(
            0.5f,
            GameEngine.stats(afterCollapse(Designations.ENTRY_SECONDS / 2.0)).tierProgress,
            0.01f,
        )
    }

    /**
     * The mass gate still gets to hold, and still gets to say so in kilograms.
     *
     * Time is what binds for anybody climbing, but a run that has sat at the black hole for a year
     * without the mass to go on is held by the other gate, and the line has to name that one.
     */
    @Test
    fun `a run short of mass is told about mass`() {
        val patient = afterCollapse(365.0 * 86_400.0).copy(runMass = Tiers.last.threshold)
        val stats = GameEngine.stats(patient)

        assertFalse(stats.tierRemainingIsTime, "Es fehlt Masse, angezeigt wird eine Wartezeit")
        assertEquals(0f, stats.tierProgress, 0.001f)
        assertTrue(stats.tierRemaining > 0.0)
    }
}

/** The road to a full sky, which is what the catalogue ladder waits behind. */
class BigBangPacingTest {

    @Test
    fun `each big bang asks for more than the one before it`() {
        val steps = (0..5).map { BigBang.requiredFor(it) }
        assertEquals(BigBang.REQUIRED_COLLAPSES, steps.first(), "Der erste Urknall hat sich verteuert")
        assertTrue(steps.zipWithNext().all { (a, b) -> b > a }, "Die Anforderung steigt nicht: $steps")
    }

    @Test
    fun `the road to the catalogue ladder is a real stretch`() {
        val total = (0 until Multiverse.SLOTS).sumOf { BigBang.requiredFor(it) }
        val flat = BigBang.REQUIRED_COLLAPSES * Multiverse.SLOTS
        assertTrue(total > flat, "Acht Urknalle kosten immer noch $total statt mehr als $flat")
        // Long, but not silly: the whole road has to stay inside what an endgame can ask for.
        assertTrue(total < flat * 3, "Acht Urknalle kosten $total Kollapse — das ist keine Streckung mehr")
    }

    @Test
    fun `filling the sky is what opens the ladder`() {
        val sky = (0 until Multiverse.SLOTS).map { ParkedUniverse(slot = it) }
        val almost = GameState.new(0).copy(bigBangs = 99, universes = sky.dropLast(1))
        val full = GameState.new(0).copy(bigBangs = Multiverse.SLOTS, universes = sky)

        assertFalse(Designations.isUnlocked(almost), "Sieben Galaxien reichen schon")
        assertTrue(Designations.isUnlocked(full), "Ein voller Himmel reicht nicht")
    }

    @Test
    fun `the button refuses until its own requirement is met`() {
        val onePast = GameState.new(0).copy(
            bigBangs = 2,
            collapses = BigBang.requiredFor(2) - 1,
            runMass = Tiers.last.threshold,
        )
        assertFalse(BigBang.canBang(onePast), "Der Urknall geht eine Stufe zu früh")
        assertTrue(BigBang.canBang(onePast.copy(collapses = BigBang.requiredFor(2))))
    }

    /** And the far end stays a number rather than an infinity. */
    @Test
    fun `the whole ladder stays priceable`() {
        val top = Designations.at(Designations.TOTAL - 1)
        assertTrue(top.threshold.isFinite(), "Die letzte Sprosse kostet unendlich viel")
        assertTrue(top.productionMultiplier.isFinite(), "Die letzte Sprosse produziert unendlich viel")
        assertTrue(
            Designations.secondsFor(Designations.COUNT).isFinite(),
            "Die letzte Sprosse verlangt unendlich lange",
        )
    }
}

