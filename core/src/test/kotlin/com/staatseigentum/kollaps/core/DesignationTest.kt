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

    private fun deep(mass: Double): GameState = GameState.new(0).copy(
        bigBangs = Multiverse.SLOTS,
        universes = fullSky(),
        runMass = mass,
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
        val far = Tiers.last.threshold * Designations.ENTRY_STEP * 1_000.0

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
            assertEquals(rung.index, Designations.forMass(rung.threshold).index, "bei Schritt $step")
            assertEquals(
                rung.index,
                Designations.forMass(rung.threshold * 1.001).index,
                "knapp über Schritt $step",
            )
            val below = Designations.forMass(rung.threshold * 0.999).index
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
        assertEquals(top.index, Designations.forMass(Double.MAX_VALUE).index)
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
        val farUp = deep(Tiers.last.threshold * Designations.ENTRY_STEP * 1e6)

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
        val far = deep(Tiers.last.threshold * Designations.ENTRY_STEP * 1e10)
        assertTrue(GameEngine.canCollapse(far), "Über dem Loch lässt sich nicht mehr kollabieren")
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

    /**
     * The two things about the catalogue's price that must not drift.
     *
     * The ladder is a geometric series over sixteen thousand rungs, and a `Double` stops near
     * 1e308. That makes the growth rate a number with a hard ceiling rather than a taste
     * decision: raise it far enough and the top of the ladder becomes `Infinity`, at which point
     * [Tiers.next] promises a rung nobody can reach and the mass on screen reads as nonsense.
     *
     * The entry step is the other half. Anybody who has earned the eight galaxies the catalogue
     * costs is producing many orders of magnitude past the black hole, so without a wall at the
     * bottom the first hundreds of rungs go by unread.
     */
    @Test
    fun `the catalogue stays expensive and stays finite`() {
        val anchor = Tiers.last.threshold

        // Nothing between the black hole and the entry step counts as catalogue.
        assertFalse(Tiers.forMass(anchor * 100, deep = true).isDesignated, "Der Katalog fängt zu früh an")
        assertFalse(
            Tiers.forMass(anchor * Designations.ENTRY_STEP * 0.99, deep = true).isDesignated,
            "Knapp unter der Einstiegsstufe steht schon eine Kennung",
        )
        // The first designation sits one growth step above the entry, exactly as every rung sits
        // one above the rung below it — the entry step moves the foot of the ladder, it is not
        // itself a rung.
        assertTrue(
            Tiers.forMass(
                anchor * Designations.ENTRY_STEP * Designations.THRESHOLD_GROWTH * 1.01,
                deep = true,
            ).isDesignated,
            "Über der Einstiegsstufe fängt der Katalog nicht an",
        )

        // And the far end is a number rather than infinity.
        val top = Designations.at(Designations.TOTAL - 1)
        assertTrue(top.threshold.isFinite(), "Die letzte Sprosse kostet unendlich viel")
        assertTrue(
            top.productionMultiplier.isFinite(),
            "Die letzte Sprosse produziert unendlich viel",
        )

        // Each rung must cost more than it pays, or the ladder would get easier as it went.
        assertTrue(
            Designations.THRESHOLD_GROWTH > Designations.PRODUCTION_GROWTH,
            "Die Leiter wird nach oben hin leichter statt schwerer",
        )
    }
}
