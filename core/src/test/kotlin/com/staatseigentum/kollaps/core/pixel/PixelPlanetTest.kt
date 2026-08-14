package com.staatseigentum.kollaps.core.pixel

import com.staatseigentum.kollaps.core.BodyKind
import com.staatseigentum.kollaps.core.Tiers
import kotlin.math.floor
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
        // The whole point of the pixel look: a handful of colours, not thousands. The ceiling is
        // the palette itself and not a taste — three materials at ten shading steps, plus the halo,
        // the bloom and three ring colours. Anything above that would mean a shade came from
        // somewhere other than the ramps, which is exactly what this is here to catch.
        val ceiling = 3 * 10 + 5
        for (tier in Tiers.all) {
            val colours = PixelPlanet.frame(tier, 0).filter { it ushr 24 != 0 }.toSet()
            assertTrue(colours.size in 2..ceiling, "${tier.name} nutzt ${colours.size} Farben")
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
    fun `resolution follows the tier so every body is drawn at the same scale`() {
        // If one resolution served every tier, the small bodies would be blown up further than the
        // large ones and a pixel block would be a different size on every rung. The buffer grows
        // with the body instead, which has to leave the *ratio* equal across the ladder — that
        // shared ratio is what makes a pixel one size in the whole game.
        //
        // Eighths rather than whole numbers, which is what `CelestialBody` draws with and is the
        // point at which this test had to be rewritten: at the doubled resolution the whole-number
        // factor is one for every rung, so it stayed uniform while quietly shrinking every body by
        // a third. Uniformity alone was not saying enough.
        val box = 894f // a 411dp phone, short side of the tap area
        val scales = Tiers.all.map { tier ->
            val available = box * PixelPlanet.spriteFraction(tier)
            floor(available / PixelPlanet.size(tier) * 8f) / 8f
        }.toSet()
        assertEquals(1, scales.size, "Uneinheitliche Pixelgröße: Faktoren $scales")

        // And the body still fills the area it is given. Under 1.0 the sprite would be sampled
        // down — more buffer than screen, which throws away exactly the detail it was raised for.
        assertTrue(scales.first() >= 1f, "Faktor ${scales.first()}x — der Sprite wird verkleinert")
        val filled = PixelPlanet.size(Tiers.last) * scales.first() / box
        assertTrue(filled > 0.9f, "Der Körper füllt nur ${(filled * 100).toInt()} % seiner Fläche")
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
        // one axis and nothing at all across it; a white dwarf is a hot sphere with a halo, which
        // is the same in every direction.
        //
        // So the test is *lopsidedness*, not reach. It used to be reach — "does anything paint far
        // out" — and that stopped separating them the moment the halo grew a bloom shell: the
        // dwarf's halo now reaches the sample ring too. It reaches it evenly, which is the whole
        // difference and is what this asks about instead.
        fun lopsidedOnSomeFrame(name: String): Boolean {
            val tier = Tiers.byName(name)
            val side = PixelPlanet.size(tier)
            val centre = side / 2
            val far = (side * 0.06f).toInt()
            return (0 until PixelPlanet.FRAMES).any { index ->
                val frame = PixelPlanet.frame(tier, index)
                val horizontal = frame.alphaAt(side, far, centre) > 0 ||
                    frame.alphaAt(side, side - 1 - far, centre) > 0
                val vertical = frame.alphaAt(side, centre, far) > 0 ||
                    frame.alphaAt(side, centre, side - 1 - far) > 0
                horizontal != vertical
            }
        }

        assertTrue(lopsidedOnSomeFrame("Neutronenstern"), "Der Neutronenstern hat keine Jets mehr")
        assertTrue(!lopsidedOnSomeFrame("Weißer Zwerg"), "Der Weiße Zwerg hat immer noch Jets")
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

    // ------------------------------------------------------------------ the HD pass

    @Test
    fun `a ringed planet has a gap in its ring`() {
        // The Cassini division. Walking outwards along the ring, painted texels have to stop and
        // start again — one solid band with a dithered stripe in it would not do that.
        val ringed = Tiers.all.first { it.hasRing }
        val side = PixelPlanet.size(ringed)
        val frame = PixelPlanet.frame(ringed, 0)
        val centre = side / 2

        // Left of the body along the middle row, from the outer edge inwards to the disc.
        val row = (centre until side).map { frame.alphaAt(side, it, centre) > 0 }
        val runs = row.zipWithNext().count { (a, b) -> a != b }
        assertTrue(runs >= 4, "Der Ring hat keine Lücke: nur $runs Wechsel entlang der Zeile")
    }

    @Test
    fun `the halo fades out through a second, weaker shell`() {
        // The bloom. Without it the halo ends on one alpha and therefore on a visible circle.
        val star = Tiers.all.first { it.kind == BodyKind.STAR }
        val side = PixelPlanet.size(star)
        val alphas = PixelPlanet.frame(star, 0).map { it ushr 24 }.filter { it in 1..0xFE }.toSet()
        assertTrue(
            alphas.size >= 2,
            "Der Halo hat nur ${alphas.size} Transparenzstufe(n) auf $side Pixeln: $alphas",
        )
    }

    @Test
    fun `shading is quantised into ten steps, not six`() {
        // Counted off a body with one material doing most of the work, so the number that comes
        // back is the ramp rather than the composition. Ten distinct shades have to be reachable.
        val rock = Tiers.first
        val opaque = PixelPlanet.frame(rock, 0).filter { it ushr 24 == 0xFF }.toSet()
        assertTrue(opaque.size >= 10, "Nur ${opaque.size} Farbstufen — die Rampe ist zu kurz")
    }

    @Test
    fun `a gas giant's equator turns faster than its poles`() {
        // Differential rotation. Between two frames the middle of the body has to move further
        // than the top of it — measured as how much of each band actually changed.
        val giant = Tiers.all.first { it.kind == BodyKind.GAS }
        val side = PixelPlanet.size(giant)
        val first = PixelPlanet.frame(giant, 0)
        val later = PixelPlanet.frame(giant, 1)

        fun changedInRow(y: Int): Int =
            (0 until side).count { first.at(side, it, y) != later.at(side, it, y) }

        // A quarter of the way down is well inside the body; the middle is the equator.
        val equator = changedInRow(side / 2)
        val temperate = changedInRow(side * 5 / 16)
        assertTrue(
            equator > temperate,
            "Äquator ($equator) bewegt sich nicht mehr als die Breiten darüber ($temperate)",
        )
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
