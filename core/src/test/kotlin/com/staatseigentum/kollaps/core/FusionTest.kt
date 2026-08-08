package com.staatseigentum.kollaps.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FusionTest {

    /** A run heavy enough to have ignited, with mass to spend. */
    private fun ignited(mass: Double = 1e18): GameState = GameState(
        runMass = Tiers.byName(Fusion.UNLOCK_TIER).threshold,
        mass = mass,
    )

    private fun stage(id: String): FusionStage = Fusion.byId(id) ?: error("Stufe fehlt: $id")

    @Test
    fun `nothing fuses before the body is hot enough`() {
        val cold = GameState(runMass = 1_000.0, mass = 1e18)
        assertFalse(Fusion.isUnlocked(cold))

        val built = cold.copy(fusers = mapOf("intake" to 5))
        // Owning a furnace is itself proof the player got there once, so the panel stays reachable
        // for the rest of the run rather than blinking out on the way up.
        assertTrue(Fusion.isUnlocked(built))

        val ticked = Fusion.advance(cold.copy(fusers = emptyMap()), seconds = 10.0)
        assertTrue(ticked.elements.isEmpty(), "Ein kalter Kern hat fusioniert")
    }

    @Test
    fun `the intake makes hydrogen out of nothing`() {
        val state = ignited().copy(fusers = mapOf("intake" to 3))
        val after = Fusion.advance(state, seconds = 10.0)

        val expected = 3 * stage("intake").baseRate * 10.0
        assertEquals(expected, Fusion.amountOf(after, Element.WASSERSTOFF), 1e-9)
    }

    @Test
    fun `a furnace burns its input at the stated ratio`() {
        val pp = stage("pp")
        val state = ignited().copy(
            fusers = mapOf("pp" to 1),
            elements = mapOf(Element.WASSERSTOFF.id to 100.0),
        )
        val after = Fusion.advance(state, seconds = 2.0)

        val made = Fusion.amountOf(after, Element.HELIUM)
        val burnt = 100.0 - Fusion.amountOf(after, Element.WASSERSTOFF)

        assertEquals(pp.baseRate * 2.0, made, 1e-9)
        assertEquals(made * pp.ratio, burnt, 1e-9, "Das Verhältnis stimmt nicht")
    }

    @Test
    fun `a furnace with no fuel makes nothing and takes nothing`() {
        val state = ignited().copy(fusers = mapOf("pp" to 40))
        val after = Fusion.advance(state, seconds = 60.0)

        assertEquals(0.0, Fusion.amountOf(after, Element.HELIUM))
        assertEquals(0.0, Fusion.amountOf(after, Element.WASSERSTOFF))
    }

    @Test
    fun `a furnace never burns more than is in the tank`() {
        val state = ignited().copy(
            fusers = mapOf("pp" to 1_000),
            elements = mapOf(Element.WASSERSTOFF.id to 10.0),
        )
        val after = Fusion.advance(state, seconds = 60.0)

        assertTrue(
            Fusion.amountOf(after, Element.WASSERSTOFF) >= -1e-9,
            "Wasserstoff ins Minus verbrannt",
        )
        assertEquals(10.0 / stage("pp").ratio, Fusion.amountOf(after, Element.HELIUM), 1e-9)
    }

    /**
     * The reason [Fusion.advance] walks the stages in order: a chain built in one go should
     * deliver in the same tick rather than filling one rung per tick like a bucket brigade.
     */
    @Test
    fun `a whole chain delivers iron in a single tick`() {
        val state = ignited().copy(fusers = Fusion.stages.associate { it.id to 500 })
        val after = Fusion.advance(state, seconds = 1.0)

        assertTrue(Fusion.amountOf(after, Element.EISEN) > 0.0, "Kein Eisen im ersten Tick")
    }

    @Test
    fun `holding nothing costs nothing`() {
        val empty = ignited()
        for (element in Element.entries) {
            assertEquals(1.0, Fusion.factor(empty, element), 1e-12)
        }
        for (bonus in FusionBonus.entries) {
            assertEquals(1.0, Fusion.factorFor(empty, bonus), 1e-12)
        }
    }

    @Test
    fun `the bonus grows by the stated amount per decade`() {
        val element = Element.EISEN
        val ten = ignited().copy(elements = mapOf(element.id to 9.0))
        val hundred = ignited().copy(elements = mapOf(element.id to 99.0))

        assertEquals(1.0 + element.perDecade, Fusion.factor(ten, element), 1e-9)
        assertEquals(1.0 + 2 * element.perDecade, Fusion.factor(hundred, element), 1e-9)
    }

    @Test
    fun `two elements on the same lever multiply`() {
        val onGlobal = Element.entries.filter { it.bonus == FusionBonus.GLOBAL }
        assertTrue(onGlobal.size >= 2, "Der Test prüft nichts mehr")

        val state = ignited().copy(elements = onGlobal.associate { it.id to 999.0 })
        val expected = onGlobal.fold(1.0) { acc, e -> acc * (1.0 + 3 * e.perDecade) }

        assertEquals(expected, Fusion.factorFor(state, FusionBonus.GLOBAL), 1e-9)
    }

    @Test
    fun `elements actually move the levers they claim`() {
        val plain = ignited().copy(collectors = mapOf("dust" to 100))
        val fused = plain.copy(elements = mapOf(Element.WASSERSTOFF.id to 1e6))

        assertTrue(
            GameEngine.massPerSecond(fused) > GameEngine.massPerSecond(plain),
            "Wasserstoff erhöht die Produktion nicht",
        )

        val tapped = plain.copy(elements = mapOf(Element.HELIUM.id to 1e6))
        assertTrue(GameEngine.massPerTap(tapped) > GameEngine.massPerTap(plain))

        val cometed = plain.copy(elements = mapOf(Element.SILIZIUM.id to 1e6))
        assertTrue(GameEngine.cometFrequency(cometed) > GameEngine.cometFrequency(plain))
    }

    /**
     * Offline yield is a share, and a share above one would pay more for being away than for
     * playing. Silicon and oxygen are both unbounded, so the cap has to be real.
     */
    @Test
    fun `the offline share never passes one`() {
        val state = ignited().copy(
            collectors = mapOf("dust" to 100),
            elements = mapOf(Element.SAUERSTOFF.id to 1e30),
        )
        assertTrue(GameEngine.stats(state).offlineEfficiency <= 1.0)
    }

    @Test
    fun `a level costs what the row says it costs`() {
        val intake = stage("intake")
        val summed = (0 until 7).sumOf { intake.costAt(it) }
        assertEquals(summed, Fusion.costForLevels(intake, 0, 7), summed * 1e-9)
    }

    @Test
    fun `the max button never offers a level the player cannot pay for`() {
        val intake = stage("intake")
        for (mass in listOf(0.0, intake.baseCost - 1.0, intake.baseCost, 5e12, 1e20)) {
            val count = Fusion.affordableLevels(intake, level = 0, mass = mass)
            assertTrue(
                Fusion.costForLevels(intake, 0, count) <= mass + 1e-6,
                "Bei $mass wurden $count Stufen angeboten",
            )
        }
    }

    @Test
    fun `buying takes the mass and gives the levels`() {
        val state = ignited(mass = 1e20)
        val bought = GameEngine.buyFuser(state, "intake", BuyAmount.TEN)

        assertEquals(10, Fusion.levelOf(bought, stage("intake")))
        assertEquals(
            1e20 - Fusion.costForLevels(stage("intake"), 0, 10),
            bought.mass,
            1e20 * 1e-12,
        )
    }

    @Test
    fun `buying is refused when the mass is not there`() {
        val broke = ignited(mass = 1.0)
        assertEquals(broke, GameEngine.buyFuser(broke, "intake", BuyAmount.ONE))
    }

    @Test
    fun `buying is refused before the body ignites`() {
        val cold = GameState(runMass = 1_000.0, mass = 1e20)
        assertEquals(cold, GameEngine.buyFuser(cold, "intake", BuyAmount.ONE))
    }

    /** The panel promises a rate; the simulation has to deliver exactly it. */
    @Test
    fun `the quoted rate is the rate the tick produces`() {
        val state = ignited().copy(
            fusers = mapOf("intake" to 4, "pp" to 2, "triple" to 30),
            elements = mapOf(Element.HELIUM.id to 5.0),
        )
        val offers = GameEngine.fusionOffers(state, BuyAmount.ONE).associateBy { it.stage.id }
        val after = Fusion.advance(state, seconds = 1.0)

        // Helium is both made and eaten in the same second, so its tank alone cannot confirm the
        // quote. What the furnace above consumed is added back.
        val heliumMade = Fusion.amountOf(after, Element.HELIUM) - 5.0 +
            Fusion.amountOf(after, Element.KOHLENSTOFF) * stage("triple").ratio

        assertEquals(heliumMade, offers.getValue("pp").outputPerSecond, 1e-9)
        assertEquals(
            Fusion.amountOf(after, Element.KOHLENSTOFF),
            offers.getValue("triple").outputPerSecond,
            1e-9,
        )
    }

    @Test
    fun `a starved furnace says so`() {
        val state = ignited().copy(fusers = mapOf("intake" to 1, "triple" to 20))
        val offers = GameEngine.fusionOffers(state, BuyAmount.ONE).associateBy { it.stage.id }

        assertTrue(offers.getValue("triple").starving, "Der leere Ofen meldet sich nicht")
        assertFalse(offers.getValue("intake").starving, "Die Zapfung kann gar nicht hungern")
        assertFalse(offers.getValue("pp").starving, "Ein Ofen ohne Stufen hungert nicht, er fehlt")
    }

    @Test
    fun `a collapse takes the elements along`() {
        val ready = GameState(
            runMass = Tiers.last.threshold,
            elements = mapOf(Element.EISEN.id to 5_000.0),
            fusers = mapOf("intake" to 20),
        )
        assertTrue(GameEngine.canCollapse(ready))

        val after = GameEngine.collapse(ready, nowMillis = 1)
        assertTrue(after.elements.isEmpty(), "Elemente haben den Kollaps überlebt")
        assertTrue(after.fusers.isEmpty(), "Öfen haben den Kollaps überlebt")
    }

    /**
     * Iron pays in singularities, which is the whole reason it sits at the end of a chain that
     * gives production everywhere else.
     */
    @Test
    fun `iron is worth more singularities`() {
        val plain = GameState(runMass = Tiers.last.threshold * 4)
        val ironed = plain.copy(elements = mapOf(Element.EISEN.id to 1e6))

        assertTrue(
            GameEngine.pendingSingularities(ironed) > GameEngine.pendingSingularities(plain),
            "Eisen bringt nichts ein",
        )
    }

    @Test
    fun `a save keeps the chain`() {
        val state = ignited().copy(
            elements = mapOf(Element.SILIZIUM.id to 1234.5),
            fusers = mapOf("intake" to 12, "silicon" to 3),
        )
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(1234.5, Fusion.amountOf(back, Element.SILIZIUM), 1e-9)
        assertEquals(12, Fusion.levelOf(back, stage("intake")))
        assertEquals(3, Fusion.levelOf(back, stage("silicon")))
    }

    @Test
    fun `a save drops elements and furnaces that no longer exist`() {
        val state = ignited().copy(
            elements = mapOf(Element.EISEN.id to 7.0, "unobtainium" to 99.0),
            fusers = mapOf("intake" to 1, "warp" to 4),
        )
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)

        assertEquals(mapOf(Element.EISEN.id to 7.0), back.elements)
        assertEquals(mapOf("intake" to 1), back.fusers)
    }

    @Test
    fun `the chain is a chain`() {
        val intake = Fusion.stages.first()
        assertEquals(null, intake.input, "Die erste Stufe braucht keinen Eingang")

        for ((below, above) in Fusion.stages.zipWithNext()) {
            assertEquals(
                below.output,
                above.input,
                "${above.name} verbrennt nicht, was ${below.name} herstellt",
            )
        }
        assertEquals(
            Element.entries.toSet(),
            Fusion.stages.map { it.output }.toSet(),
            "Ein Element wird von keiner Stufe hergestellt",
        )
    }

    @Test
    fun `every furnace is more expensive and slower than the one below it`() {
        for ((below, above) in Fusion.stages.zipWithNext()) {
            assertTrue(above.baseCost > below.baseCost, "${above.name} ist zu billig")
            assertTrue(above.baseRate <= below.baseRate, "${above.name} ist zu schnell")
        }
    }

    /** Long ticks happen: the app was closed, or the phone was busy. They must not pay differently. */
    @Test
    fun `one long tick equals many short ones`() {
        val start = ignited().copy(fusers = Fusion.stages.associate { it.id to 8 })

        val once = Fusion.advance(start, seconds = 60.0)
        var stepwise = start
        repeat(60) { stepwise = Fusion.advance(stepwise, seconds = 1.0) }

        for (element in Element.entries) {
            val a = Fusion.amountOf(once, element)
            val b = Fusion.amountOf(stepwise, element)
            // Not equal, and cannot be: a long tick lets every rung see the whole minute's fuel at
            // once, where sixty short ones ration it. What matters is that neither runs away.
            assertTrue(
                abs(a - b) <= maxOf(a, b) * 0.5 + 1e-9,
                "${element.label} läuft auseinander: $a gegenüber $b",
            )
        }
    }
}
