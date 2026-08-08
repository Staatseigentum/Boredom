package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrbitTest {

    private val now = 1_700_000_000_000L

    /** A run big enough to hold satellites, with mass to spend. */
    private fun grown(mass: Double = 1e18): GameState = GameState(
        runMass = Tiers.byName(Orbits.UNLOCK_TIER).threshold,
        bestTier = Tiers.indexOf(Orbits.UNLOCK_TIER),
        mass = mass,
        collectors = mapOf("dust" to 100),
    )

    /** A state with [count] slots open and a body on each. */
    private fun withBodies(count: Int, each: Double = 1e9): GameState =
        grown().copy(
            orbits = count,
            satellites = (0 until count).associateWith { each },
        )

    // ------------------------------------------------------------------ opening slots

    @Test
    fun `no orbit before the body is big enough`() {
        val small = GameState(runMass = 1_000.0, mass = 1e20)
        assertFalse(Orbits.isUnlocked(small))
        assertEquals(small, GameEngine.openOrbit(small))
    }

    @Test
    fun `an open slot keeps the panel reachable`() {
        assertTrue(Orbits.isUnlocked(GameState(bestTier = 0, orbits = 1)))
    }

    @Test
    fun `opening a slot costs what the slot says`() {
        val state = grown()
        val first = Orbits.at(0)!!
        val after = GameEngine.openOrbit(state)

        assertEquals(1, after.orbits)
        assertEquals(state.mass - first.cost, after.mass, state.mass * 1e-12)
    }

    @Test
    fun `slots open one at a time and stop at the last`() {
        var state = grown(mass = 1e30)
        repeat(Orbits.MAX + 5) { state = GameEngine.openOrbit(state) }

        assertEquals(Orbits.MAX, state.orbits)
        assertEquals(null, Orbits.next(state))
    }

    @Test
    fun `opening is refused without the mass`() {
        val broke = grown(mass = Orbits.at(0)!!.cost - 1.0)
        assertEquals(broke, GameEngine.openOrbit(broke))
    }

    @Test
    fun `every slot costs more than the one inside it`() {
        for ((inner, outer) in Orbits.all.zipWithNext()) {
            assertTrue(outer.cost > inner.cost, "Bahn ${outer.index} ist nicht teurer")
        }
    }

    // ------------------------------------------------------------------ placing bodies

    @Test
    fun `a body needs an open slot`() {
        val state = grown()
        assertEquals(state, GameEngine.seedSatellite(state, 0))

        val opened = GameEngine.openOrbit(state)
        assertTrue(GameEngine.seedSatellite(opened, 0).satellites.isNotEmpty())
    }

    @Test
    fun `a slot holds one body`() {
        val opened = GameEngine.openOrbit(grown())
        val seeded = GameEngine.seedSatellite(opened, 0)

        assertEquals(seeded, GameEngine.seedSatellite(seeded, 0))
    }

    @Test
    fun `a new body starts with a minute of production`() {
        val opened = GameEngine.openOrbit(grown())
        val perSecond = GameEngine.massPerSecond(opened)
        val seeded = GameEngine.seedSatellite(opened, 0)

        assertEquals(perSecond * 60.0, Orbits.massOn(seeded, Orbits.at(0)!!), perSecond * 1e-6)
    }

    // ------------------------------------------------------------------ feeding and tides

    @Test
    fun `a body is fed by what the central mass makes`() {
        val state = withBodies(1, each = 0.0).copy(satellites = mapOf(0 to 100.0))
        val perSecond = 1_000.0

        val after = Orbits.advance(state, seconds = 1.0, perSecond = perSecond)
        val orbit = Orbits.at(0)!!
        val expected = 100.0 + perSecond * orbit.feed - 100.0 * orbit.tide

        assertEquals(expected, Orbits.massOn(after, orbit), 1e-9)
    }

    /**
     * The trade the whole system is built on.
     *
     * Inside, a body is fed hard and pulled apart just as hard, so it settles somewhere and stays.
     * Outside, nothing takes anything away and it grows for as long as the run does.
     */
    @Test
    fun `the inner orbits settle and the outer ones do not`() {
        val inner = Orbits.at(0)!!
        val outer = Orbits.all.last()

        assertTrue(inner.tide > 0.0, "Die innerste Bahn zerrt nicht")
        assertEquals(0.0, outer.tide, "Die äußerste Bahn zerrt")

        assertTrue(inner.equilibrium(1_000.0).isFinite())
        assertFalse(outer.equilibrium(1_000.0).isFinite())
    }

    @Test
    fun `a body settles at the equilibrium of its orbit`() {
        val orbit = Orbits.at(0)!!
        val perSecond = 1e6
        var state = withBodies(1, each = 1.0)

        // An hour, in one-second steps: long enough to arrive from far below.
        repeat(3_600) { state = Orbits.advance(state, 1.0, perSecond) }

        val settled = Orbits.massOn(state, orbit)
        assertEquals(orbit.equilibrium(perSecond), settled, orbit.equilibrium(perSecond) * 0.02)
    }

    @Test
    fun `a body above its equilibrium shrinks towards it`() {
        val orbit = Orbits.at(0)!!
        val perSecond = 1e6
        val heavy = withBodies(1, each = orbit.equilibrium(perSecond) * 4)

        val after = Orbits.advance(heavy, seconds = 60.0, perSecond = perSecond)
        assertTrue(
            Orbits.massOn(after, orbit) < Orbits.massOn(heavy, orbit),
            "Ein zu schwerer Trabant wächst weiter",
        )
    }

    @Test
    fun `nothing is ever fed into the ground`() {
        val state = withBodies(3, each = 1.0)
        val after = Orbits.advance(state, seconds = 1e6, perSecond = 0.0)

        for (mass in after.satellites.values) {
            assertTrue(mass >= 0.0, "Ein Trabant hat negative Masse: $mass")
        }
    }

    // ------------------------------------------------------------------ what they are worth

    @Test
    fun `an empty sky is worth nothing`() {
        assertEquals(1.0, Orbits.multiplier(grown()), 1e-12)
        assertEquals(1.0, Orbits.multiplier(grown().copy(orbits = Orbits.MAX)), 1e-12)
    }

    @Test
    fun `bodies add up rather than multiplying each other`() {
        val one = withBodies(1)
        val two = withBodies(2)

        val first = Orbits.totalYieldOf(one, Orbits.at(0)!!)
        assertTrue(first > 0.0)

        // Two bodies are the sum of what each is worth, whatever their resonance adds.
        val expected = 1.0 + Orbits.all.take(2).sumOf { Orbits.totalYieldOf(two, it) }
        assertEquals(expected, Orbits.multiplier(two), 1e-9)
    }

    @Test
    fun `the system actually raises production`() {
        val bare = grown()
        val orbited = withBodies(3)

        assertTrue(
            GameEngine.massPerSecond(orbited) > GameEngine.massPerSecond(bare),
            "Drei Trabanten ändern nichts",
        )
        assertTrue(GameEngine.stats(orbited).orbitMultiplier > 1.0)
    }

    /**
     * A moon larger than its planet is not a moon.
     *
     * Without the cap the quickest way up the ladder would be to pour everything into a satellite
     * and let it out-produce the body the player is actually playing.
     */
    @Test
    fun `a satellite never counts as more than one rung below the body`() {
        val state = grown().copy(orbits = 1, satellites = mapOf(0 to Tiers.last.threshold * 10))
        val ceiling = GameEngine.tierOf(state).index - 1

        assertEquals(ceiling, Orbits.tierOn(state, Orbits.at(0)!!).index)
    }

    @Test
    fun `a satellite on a meteorite has nowhere to go`() {
        val tiny = GameState(runMass = 0.0, orbits = 1, satellites = mapOf(0 to 1e20))
        assertEquals(0, Orbits.tierOn(tiny, Orbits.at(0)!!).index)
    }

    // ------------------------------------------------------------------ resonance

    @Test
    fun `slots in a small whole ratio lock together`() {
        // Slots are numbered from one, so index 0 and index 1 are the 1:2 pair.
        assertTrue(Orbits.isResonant(Orbits.at(0)!!, Orbits.at(1)!!))
        assertTrue(Orbits.isResonant(Orbits.at(1)!!, Orbits.at(2)!!))
        assertTrue(Orbits.isResonant(Orbits.at(0)!!, Orbits.at(2)!!))
        assertTrue(Orbits.isResonant(Orbits.at(2)!!, Orbits.at(3)!!))
        assertTrue(Orbits.isResonant(Orbits.at(1)!!, Orbits.at(4)!!))

        // Four to five and one to four divide into nothing useful.
        assertFalse(Orbits.isResonant(Orbits.at(0)!!, Orbits.at(3)!!))
        assertFalse(Orbits.isResonant(Orbits.at(3)!!, Orbits.at(4)!!))
    }

    @Test
    fun `a slot is never in resonance with itself`() {
        for (orbit in Orbits.all) {
            assertFalse(Orbits.isResonant(orbit, orbit), "Bahn ${orbit.index}")
        }
    }

    @Test
    fun `resonance only counts against an occupied slot`() {
        val lonely = grown().copy(orbits = 4, satellites = mapOf(0 to 1e9))
        assertTrue(Orbits.resonantWith(lonely, Orbits.at(0)!!).isEmpty())

        val paired = lonely.copy(satellites = mapOf(0 to 1e9, 1 to 1e9))
        assertEquals(listOf(1), Orbits.resonantWith(paired, Orbits.at(0)!!).map { it.index })
    }

    @Test
    fun `a resonant pair is worth more than two bodies apart`() {
        val paired = grown().copy(orbits = 5, satellites = mapOf(0 to 1e9, 1 to 1e9))
        val apart = grown().copy(orbits = 5, satellites = mapOf(0 to 1e9, 3 to 1e9))

        assertTrue(
            Orbits.multiplier(paired) > Orbits.multiplier(apart),
            "Die Resonanz bringt nichts",
        )
    }

    // ------------------------------------------------------------------ merging

    @Test
    fun `merging drops the outer body onto the inner one`() {
        val state = grown().copy(orbits = 4, satellites = mapOf(1 to 2e9, 3 to 5e9))
        val merged = GameEngine.mergeSatellites(state, 3, 1)

        assertEquals(
            (2e9 + 5e9) * Orbits.MERGE_BONUS,
            Orbits.massOn(merged, Orbits.at(1)!!),
            1.0,
        )
        assertFalse(Orbits.isOccupied(merged, Orbits.at(3)!!), "Die äußere Bahn ist noch belegt")
    }

    @Test
    fun `merging needs two bodies`() {
        val state = grown().copy(orbits = 4, satellites = mapOf(1 to 2e9))

        assertEquals(state, GameEngine.mergeSatellites(state, 1, 3))
        assertEquals(state, GameEngine.mergeSatellites(state, 1, 1))
    }

    // ------------------------------------------------------------------ what survives

    @Test
    fun `a collapse scatters the system`() {
        val ready = withBodies(3).copy(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
        )
        assertTrue(GameEngine.canCollapse(ready))

        val after = GameEngine.collapse(ready, now)
        assertEquals(0, after.orbits)
        assertTrue(after.satellites.isEmpty())
    }

    @Test
    fun `a save keeps the system`() {
        val state = withBodies(3, each = 1234.5)
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(3, back.orbits)
        assertEquals(state.satellites, back.satellites)
    }

    @Test
    fun `a save drops bodies on slots that were never opened`() {
        val state = grown().copy(orbits = 2, satellites = mapOf(0 to 1e9, 5 to 1e9, -1 to 1e9))
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(mapOf(0 to 1e9), back.satellites)
    }

    @Test
    fun `a save never keeps more slots than there are`() {
        val state = grown().copy(orbits = Orbits.MAX + 40)
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(Orbits.MAX, back.orbits)
    }

    // ------------------------------------------------------------------ the catalogue

    @Test
    fun `every slot is further out, calmer and worth less than the one inside it`() {
        for ((inner, outer) in Orbits.all.zipWithNext()) {
            assertTrue(outer.radius > inner.radius, "Bahn ${outer.index} liegt nicht weiter außen")
            assertTrue(outer.feed < inner.feed, "Bahn ${outer.index} bekommt nicht weniger")
            assertTrue(outer.tide <= inner.tide, "Bahn ${outer.index} zerrt stärker")
            assertTrue(outer.yield < inner.yield, "Bahn ${outer.index} zählt nicht weniger")
        }
    }

    @Test
    fun `the outermost orbit still fits on the screen`() {
        assertTrue(Orbits.all.last().radius <= 1.0, "Die äußerste Bahn liegt außerhalb des Bildes")
        assertTrue(Orbits.all.first().radius > 0.3f, "Die innerste Bahn liegt im Körper")
    }
}
