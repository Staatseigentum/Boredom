package com.staatseigentum.kollaps.core.pixel

import com.staatseigentum.kollaps.core.BodyKind
import com.staatseigentum.kollaps.core.Tiers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelPlanetTest {

    private fun IntArray.at(side: Int, x: Int, y: Int): Int = this[y * side + x]

    private fun IntArray.alphaAt(side: Int, x: Int, y: Int): Int = at(side, x, y) ushr 24

    @Test
    fun `every tier renders a full sprite sheet`() {
        for (tier in Tiers.all) {
            val side = PixelPlanet.size(tier)
            val frames = PixelPlanet.frames(tier)
            assertEquals(PixelPlanet.FRAMES, frames.size, "${tier.name} hat zu wenige Frames")
            for (frame in frames) {
                assertEquals(side * side, frame.size, "${tier.name} hat die falsche Puffergröße")
            }
        }
    }

    @Test
    fun `every body is solid in the middle and clear at the corners`() {
        for (tier in Tiers.all) {
            val side = PixelPlanet.size(tier)
            val centre = side / 2
            val frame = PixelPlanet.frame(tier, 0)
            assertTrue(
                frame.alphaAt(side, centre, centre) > 0,
                "${tier.name} ist in der Mitte durchsichtig",
            )
            assertEquals(0, frame.alphaAt(side, 0, 0), "${tier.name} malt in die Ecke")
            assertEquals(
                0,
                frame.alphaAt(side, side - 1, side - 1),
                "${tier.name} malt in die Ecke",
            )
        }
    }

    @Test
    fun `a body uses a small palette rather than a gradient`() {
        // The whole point of the pixel look: a handful of colours, not thousands.
        for (tier in Tiers.all) {
            val colours = PixelPlanet.frame(tier, 0).filter { it ushr 24 != 0 }.toSet()
            assertTrue(colours.size in 2..24, "${tier.name} nutzt ${colours.size} Farben")
        }
    }

    @Test
    fun `rendering is deterministic`() {
        // Frames are regenerated whenever the player reaches a tier again, so the same tier has
        // to come back looking identical — otherwise craters would wander between runs.
        for (tier in Tiers.all) {
            assertTrue(
                PixelPlanet.frame(tier, 3).contentEquals(PixelPlanet.frame(tier, 3)),
                "${tier.name} rendert nicht reproduzierbar",
            )
        }
    }

    @Test
    fun `the body actually turns between frames`() {
        for (tier in Tiers.all) {
            val first = PixelPlanet.frame(tier, 0)
            val later = PixelPlanet.frame(tier, PixelPlanet.FRAMES / 2)
            assertTrue(
                !first.contentEquals(later),
                "${tier.name} sieht in jedem Frame gleich aus",
            )
        }
    }

    @Test
    fun `the black hole keeps its event horizon dark`() {
        val side = PixelPlanet.size(Tiers.last)
        val frame = PixelPlanet.frame(Tiers.last, 0)
        val centre = side / 2
        val colour = frame.at(side, centre, centre)
        val red = colour shr 16 and 0xFF
        val green = colour shr 8 and 0xFF
        val blue = colour and 0xFF
        assertTrue(red + green + blue < 60, "Der Ereignishorizont leuchtet: $red/$green/$blue")
    }

    @Test
    fun `ringed planets paint outside their own disc`() {
        val ringed = Tiers.all.first { it.hasRing }
        val side = PixelPlanet.size(ringed)
        val frame = PixelPlanet.frame(ringed, 0)
        val centre = side / 2
        // Far out on the horizontal axis there is nothing but ring.
        val edge = (side * 0.04f).toInt()
        assertTrue(
            frame.alphaAt(side, edge, centre) > 0 || frame.alphaAt(side, side - 1 - edge, centre) > 0,
            "${ringed.name} hat keinen sichtbaren Ring",
        )
    }

    @Test
    fun `sprite size grows with the tier`() {
        val meteorite = PixelPlanet.spriteFraction(Tiers.first)
        val supergiant = PixelPlanet.spriteFraction(Tiers.all.first { it.name == "Roter Überriese" })
        assertTrue(supergiant > meteorite, "Der Überriese ist nicht größer als der Meteorit")
        assertTrue(meteorite in 0.25f..1f)
        assertTrue(supergiant in 0.25f..1f)
    }

    @Test
    fun `the body never shrinks on the way up`() {
        // The whole promise of the ladder is that you get bigger, and it holds all the way to
        // the hypergiant. Then the star collapses, and the three remnants after it are supposed
        // to be small — that shrinking is the point of that stretch, not a bug in it.
        val remnants = Tiers.all.filter { it.kind in COLLAPSED }
        var previous = 0f
        for (tier in Tiers.all) {
            if (tier.kind in COLLAPSED) continue
            val fraction = PixelPlanet.spriteFraction(tier)
            assertTrue(
                fraction >= previous,
                "${tier.name} ist kleiner als die Stufe davor ($fraction < $previous)",
            )
            previous = fraction
        }

        val peak = PixelPlanet.spriteFraction(Tiers.all[remnants.first().index - 1])
        for (remnant in remnants) {
            assertTrue(
                PixelPlanet.spriteFraction(remnant) < peak,
                "${remnant.name} ist nicht kleiner als der Stern, aus dem er wurde",
            )
        }
    }

    @Test
    fun `the ladder spends its size range instead of clamping the top half together`() {
        // Guards the mistake this replaced: a multiplier on top of the tier's own size pushed
        // everything from Jupiter upwards into the clamp, so six steps rendered identically.
        val sizes = Tiers.all.map { PixelPlanet.spriteFraction(it) }.toSet()
        assertTrue(
            sizes.size >= Tiers.all.size - 2,
            "Nur ${sizes.size} verschiedene Größen auf ${Tiers.all.size} Stufen",
        )
    }

    @Test
    fun `no two neighbouring tiers land on the same sprite resolution`() {
        // Sizes snap to an eight pixel grid, so fractions that differ on paper can still round
        // to the same buffer. Twenty-five rungs share the range that eighteen used to, which is
        // exactly the condition under which that starts happening.
        Tiers.all.zipWithNext { a, b ->
            assertTrue(
                PixelPlanet.size(a) != PixelPlanet.size(b),
                "${a.name} und ${b.name} rendern beide auf ${PixelPlanet.size(a)} Pixeln",
            )
        }
    }

    @Test
    fun `resolution follows the tier so every body scales up by the same whole number`() {
        // Sprites are blown up by a whole-number factor. If one resolution served every tier,
        // the small bodies would land on factor one and lose their pixel blocks entirely. The
        // buffer grows with the body instead, which has to keep the factor equal across the
        // ladder — that shared factor is what makes the pixels one size in the whole game.
        val box = 894f // a 411dp phone, short side of the tap area
        val factors = Tiers.all.map { tier ->
            val available = box * PixelPlanet.spriteFraction(tier)
            (available / PixelPlanet.size(tier)).toInt().coerceAtLeast(1)
        }.toSet()
        assertEquals(1, factors.size, "Uneinheitliche Pixelgröße: Faktoren $factors")
        assertTrue(factors.first() >= 2, "Faktor ${factors.first()}x — die Pixelblöcke wären unsichtbar")
    }

    @Test
    fun `the buffer grows with the body`() {
        val meteorite = PixelPlanet.size(Tiers.first)
        val biggest = PixelPlanet.size(Tiers.all.maxBy { PixelPlanet.spriteFraction(it) })
        assertTrue(biggest > meteorite, "$biggest ist nicht größer als $meteorite")
        assertEquals(PixelPlanet.BASE_SIZE, biggest, "Die größte Stufe schöpft die Auflösung nicht aus")
        for (tier in Tiers.all) {
            assertEquals(0, PixelPlanet.size(tier) % 8, "${tier.name} liegt nicht auf dem Raster")
        }
    }

    @Test
    fun `the white dwarf has no jets, and the neutron star does`() {
        // The jets are the whole reason these are two kinds. A pulsar throws light far out along
        // one axis; a white dwarf is a hot sphere and nothing more. Sampling a band just outside
        // the body separates them: the pulsar paints there on some frame, the dwarf never does.
        fun paintsFarOut(name: String): Boolean {
            val tier = Tiers.byName(name)
            val side = PixelPlanet.size(tier)
            val centre = side / 2
            val far = (side * 0.06f).toInt()
            return (0 until PixelPlanet.FRAMES).any { index ->
                val frame = PixelPlanet.frame(tier, index)
                frame.alphaAt(side, far, centre) > 0 ||
                    frame.alphaAt(side, side - 1 - far, centre) > 0 ||
                    frame.alphaAt(side, centre, far) > 0 ||
                    frame.alphaAt(side, centre, side - 1 - far) > 0
            }
        }

        assertTrue(paintsFarOut("Neutronenstern"), "Der Neutronenstern hat keine Jets mehr")
        assertTrue(!paintsFarOut("Weißer Zwerg"), "Der Weiße Zwerg hat immer noch Jets")
    }

    @Test
    fun `the white dwarf is drawn as a sphere rather than as a pulsar`() {
        val dwarf = Tiers.byName("Weißer Zwerg")
        assertEquals(BodyKind.REMNANT, dwarf.kind)

        // A sphere is a solid disc: a horizontal cut through the middle is opaque from edge to
        // edge of the body. A pulsar's core is a fraction of that width.
        val side = PixelPlanet.size(dwarf)
        val frame = PixelPlanet.frame(dwarf, 0)
        val centre = side / 2
        val opaque = (0 until side).count { frame.alphaAt(side, it, centre) > 0 }
        assertTrue(opaque > side / 2, "Der Weiße Zwerg ist nur $opaque von $side Pixeln breit")
    }

    @Test
    fun `every body kind has a spin duration`() {
        for (kind in BodyKind.entries) {
            assertTrue(PixelPlanet.spinMillis(kind) > 0, "$kind dreht sich nicht")
        }
    }

    private companion object {
        /** The kinds a star leaves behind. Everything here is small on purpose. */
        val COLLAPSED = setOf(BodyKind.EXOTIC, BodyKind.REMNANT)
    }
}
