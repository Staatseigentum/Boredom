package com.staatseigentum.kollaps.core.pixel

import com.staatseigentum.kollaps.core.BodyKind
import com.staatseigentum.kollaps.core.Tiers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelPlanetTest {

    private fun IntArray.at(x: Int, y: Int): Int = this[y * PixelPlanet.SIZE + x]

    private fun IntArray.alphaAt(x: Int, y: Int): Int = at(x, y) ushr 24

    @Test
    fun `every tier renders a full sprite sheet`() {
        for (tier in Tiers.all) {
            val frames = PixelPlanet.frames(tier)
            assertEquals(PixelPlanet.FRAMES, frames.size, "${tier.name} hat zu wenige Frames")
            for (frame in frames) {
                assertEquals(
                    PixelPlanet.SIZE * PixelPlanet.SIZE,
                    frame.size,
                    "${tier.name} hat die falsche Puffergröße",
                )
            }
        }
    }

    @Test
    fun `every body is solid in the middle and clear at the corners`() {
        val centre = PixelPlanet.SIZE / 2
        for (tier in Tiers.all) {
            val frame = PixelPlanet.frame(tier, 0)
            assertTrue(
                frame.alphaAt(centre, centre) > 0,
                "${tier.name} ist in der Mitte durchsichtig",
            )
            assertEquals(0, frame.alphaAt(0, 0), "${tier.name} malt in die Ecke")
            assertEquals(
                0,
                frame.alphaAt(PixelPlanet.SIZE - 1, PixelPlanet.SIZE - 1),
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
        val frame = PixelPlanet.frame(Tiers.last, 0)
        val centre = PixelPlanet.SIZE / 2
        val colour = frame.at(centre, centre)
        val red = colour shr 16 and 0xFF
        val green = colour shr 8 and 0xFF
        val blue = colour and 0xFF
        assertTrue(red + green + blue < 60, "Der Ereignishorizont leuchtet: $red/$green/$blue")
    }

    @Test
    fun `ringed planets paint outside their own disc`() {
        val ringed = Tiers.all.first { it.hasRing }
        val frame = PixelPlanet.frame(ringed, 0)
        val centre = PixelPlanet.SIZE / 2
        // Far out on the horizontal axis there is nothing but ring.
        assertTrue(
            frame.alphaAt(4, centre) > 0 || frame.alphaAt(PixelPlanet.SIZE - 5, centre) > 0,
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
        // The whole promise of the ladder is that you get bigger. The neutron star is the one
        // deliberate exception: the supergiant collapses, and that step is supposed to be small.
        val collapse = Tiers.all.first { it.kind == BodyKind.EXOTIC }
        var previous = 0f
        for (tier in Tiers.all) {
            if (tier.index == collapse.index) continue
            val fraction = PixelPlanet.spriteFraction(tier)
            assertTrue(
                fraction >= previous,
                "${tier.name} ist kleiner als die Stufe davor ($fraction < $previous)",
            )
            previous = fraction
        }
        assertTrue(
            PixelPlanet.spriteFraction(collapse) < PixelPlanet.spriteFraction(Tiers.all[collapse.index - 1]),
            "Der Neutronenstern ist nicht kleiner als der Überriese davor",
        )
    }

    @Test
    fun `the ladder spends its size range instead of clamping the top half together`() {
        // Guards the mistake this replaced: a multiplier on top of the tier's own size pushed
        // everything from Jupiter upwards into the clamp, so six steps rendered identically.
        val sizes = Tiers.all.map { PixelPlanet.spriteFraction(it) }.toSet()
        assertTrue(sizes.size >= 16, "Nur ${sizes.size} verschiedene Größen auf ${Tiers.all.size} Stufen")
    }

    @Test
    fun `every body kind has a spin duration`() {
        for (kind in BodyKind.entries) {
            assertTrue(PixelPlanet.spinMillis(kind) > 0, "$kind dreht sich nicht")
        }
    }
}
