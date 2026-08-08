package com.staatseigentum.kollaps.core

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HeavyTest {

    private val now = 1_700_000_000_000L

    /** A run standing on the black hole, with [iron] in the core. */
    private fun readyToCollapse(iron: Double): GameState = GameState(
        runMass = Tiers.last.threshold,
        bestTier = Tiers.last.index,
        elements = if (iron > 0.0) mapOf(Element.EISEN.id to iron) else emptyMap(),
    )

    // ------------------------------------------------------------------ what a collapse forges

    @Test
    fun `no iron, nothing forged`() {
        assertTrue(Heavy.yieldFrom(0.0).isEmpty())
        assertTrue(Heavy.yieldFrom(-5.0).isEmpty())

        val after = GameEngine.collapse(readyToCollapse(0.0), now)
        assertTrue(after.heavy.isEmpty(), "Aus nichts wurde Gold")
    }

    @Test
    fun `a collapse turns the iron in the core into heavy elements`() {
        val state = readyToCollapse(10_000.0)
        val after = GameEngine.collapse(state, now)

        assertEquals(
            sqrt(10_000.0) * HeavyElement.GOLD.perRoot,
            Heavy.amountOf(after, HeavyElement.GOLD),
            1.0,
        )
        assertTrue(Heavy.amountOf(after, HeavyElement.URAN) >= 1.0)
    }

    @Test
    fun `what the card promises is what the collapse pays`() {
        val state = readyToCollapse(50_000.0)
        val promised = Heavy.pending(state)
        val after = GameEngine.collapse(state, now)

        assertEquals(promised, after.heavy)
    }

    /**
     * The square root is the whole shape of it.
     *
     * Without it one enormous run would be worth more than every other run the player will ever
     * have, and "should I collapse now" would stop being a question worth asking.
     */
    @Test
    fun `four times the iron is twice the gold`() {
        val small = Heavy.yieldFrom(10_000.0).getValue(HeavyElement.GOLD.id)
        val large = Heavy.yieldFrom(40_000.0).getValue(HeavyElement.GOLD.id)

        assertEquals(2.0, large / small, 0.02)
    }

    @Test
    fun `the rarer an element, the more iron it takes`() {
        for ((common, rare) in HeavyElement.entries.zipWithNext()) {
            assertTrue(rare.perRoot < common.perRoot, "${rare.label} ist nicht seltener")
            assertTrue(rare.perDecade > common.perDecade, "${rare.label} ist nicht wertvoller")
        }
    }

    @Test
    fun `a trace of iron is rounded down to nothing rather than to a fraction`() {
        val trace = Heavy.yieldFrom(1.0)
        for (amount in trace.values) {
            assertEquals(amount, kotlin.math.floor(amount), "Bruchstücke eines Atoms")
            assertTrue(amount >= 1.0, "Ein Eintrag über null Einheiten")
        }
    }

    @Test
    fun `collapses add up`() {
        val once = GameEngine.collapse(readyToCollapse(10_000.0), now)
        val twice = Heavy.forge(once.heavy, 10_000.0)

        assertEquals(
            2 * Heavy.amountOf(once, HeavyElement.GOLD),
            twice.getValue(HeavyElement.GOLD.id),
            1e-9,
        )
    }

    // ------------------------------------------------------------------ what they are worth

    @Test
    fun `holding nothing costs nothing`() {
        val empty = GameState()
        for (element in HeavyElement.entries) {
            assertEquals(1.0, Heavy.factor(empty, element), 1e-12)
        }
        for (bonus in FusionBonus.entries) {
            assertEquals(1.0, Heavy.factorFor(empty, bonus), 1e-12)
        }
    }

    @Test
    fun `the bonus grows by the stated amount per decade`() {
        val element = HeavyElement.GOLD
        val ten = GameState(heavy = mapOf(element.id to 9.0))
        val thousand = GameState(heavy = mapOf(element.id to 999.0))

        assertEquals(1.0 + element.perDecade, Heavy.factor(ten, element), 1e-9)
        assertEquals(1.0 + 3 * element.perDecade, Heavy.factor(thousand, element), 1e-9)
    }

    @Test
    fun `each one moves the lever it names`() {
        val fleet = GameState(collectors = mapOf("dust" to 100))

        val platinum = fleet.copy(heavy = mapOf(HeavyElement.PLATIN.id to 1e5))
        assertTrue(GameEngine.massPerSecond(platinum) > GameEngine.massPerSecond(fleet))

        val uranium = fleet.copy(heavy = mapOf(HeavyElement.URAN.id to 1e5))
        assertTrue(GameEngine.massPerTap(uranium) > GameEngine.massPerTap(fleet))

        val golden = GameState(
            runMass = Tiers.last.threshold * 4,
            heavy = mapOf(HeavyElement.GOLD.id to 1e5),
        )
        val plain = golden.copy(heavy = emptyMap())
        assertTrue(
            GameEngine.pendingSingularities(golden) > GameEngine.pendingSingularities(plain),
            "Gold bringt keine Singularitäten",
        )
    }

    // ------------------------------------------------------------------ what survives

    /** The point of the whole file: this is the one holding nothing takes away. */
    @Test
    fun `heavy elements survive every reset there is`() {
        val forged = GameEngine.collapse(readyToCollapse(90_000.0), now)
        assertTrue(forged.heavy.isNotEmpty())

        val collapsedAgain = GameEngine.collapse(
            forged.copy(runMass = Tiers.last.threshold, bestTier = Tiers.last.index),
            now,
        )
        assertTrue(
            Heavy.amountOf(collapsedAgain, HeavyElement.GOLD) >=
                Heavy.amountOf(forged, HeavyElement.GOLD),
            "Ein zweiter Kollaps hat das Gold des ersten verschluckt",
        )

        val banging = forged.copy(
            collapses = BigBang.REQUIRED_COLLAPSES,
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
        )
        assertTrue(BigBang.canBang(banging))
        assertEquals(
            forged.heavy,
            GameEngine.bigBang(banging, now).heavy,
            "Der Urknall hat die schweren Elemente vernichtet",
        )
    }

    @Test
    fun `a challenge neither pays them nor takes them`() {
        val state = GameState(
            heavy = mapOf(HeavyElement.GOLD.id to 500.0),
            collapses = 9,
        )
        val challenge = Challenge.entries.first { state.collapses >= it.requiredCollapses }

        val started = GameEngine.startChallenge(state, challenge.id, now)
        assertEquals(state.heavy, started.heavy)
    }

    @Test
    fun `the light chain is scattered by a collapse and the heavy one is not`() {
        val after = GameEngine.collapse(readyToCollapse(10_000.0), now)

        assertEquals(0.0, Fusion.amountOf(after, Element.EISEN), "Das Eisen ist noch da")
        assertTrue(after.heavy.isNotEmpty(), "Und dafür ist nichts entstanden")
    }

    // ------------------------------------------------------------------ persistence

    @Test
    fun `a save keeps them`() {
        val state = GameState(heavy = mapOf(HeavyElement.GOLD.id to 42.0, HeavyElement.URAN.id to 3.0))
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertEquals(state.heavy, back.heavy)
    }

    @Test
    fun `a save drops elements that no longer exist`() {
        val state = GameState(heavy = mapOf(HeavyElement.GOLD.id to 42.0, "kryptonit" to 9.0))
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertEquals(mapOf(HeavyElement.GOLD.id to 42.0), back.heavy)
    }

    // ------------------------------------------------------------------ the catalogue

    @Test
    fun `nothing is shown before the first one is forged`() {
        assertFalse(Heavy.isUnlocked(GameState()))
        assertFalse(Heavy.isUnlocked(GameState(heavy = mapOf(HeavyElement.GOLD.id to 0.0))))
        assertTrue(Heavy.isUnlocked(GameState(heavy = mapOf(HeavyElement.GOLD.id to 1.0))))
    }

    @Test
    fun `every heavy element says what it is and what it does`() {
        for (element in HeavyElement.entries) {
            assertTrue(element.label.isNotBlank(), element.id)
            assertTrue(element.symbol.isNotBlank(), element.id)
            assertTrue(element.flavor.isNotBlank(), element.id)
            assertTrue(element.perRoot > 0.0, element.label)
            assertTrue(element.perDecade > 0.0, element.label)
        }
        assertEquals(
            HeavyElement.entries.size,
            HeavyElement.entries.map { it.id }.toSet().size,
            "Doppeltes Elementkürzel",
        )
    }

    /** They ride the same three levers the fusion chain does, so nothing here is a new concept. */
    @Test
    fun `every lever they pull is one the game already had`() {
        for (element in HeavyElement.entries) {
            assertTrue(element.bonus in FusionBonus.entries, element.label)
        }
    }
}
