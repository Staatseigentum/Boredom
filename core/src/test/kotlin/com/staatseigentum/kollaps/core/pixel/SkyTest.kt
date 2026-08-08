package com.staatseigentum.kollaps.core.pixel

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkyTest {

    @Test
    fun `the sky is the same sky every time`() {
        assertEquals(Sky.stars(), Sky.stars())
        assertEquals(Sky.clouds(0.5f), Sky.clouds(0.5f))
        assertEquals(Sky.dust(), Sky.dust())
    }

    @Test
    fun `every layer brings the stars it promises`() {
        val stars = Sky.stars()
        for (layer in SkyLayer.entries) {
            assertEquals(layer.count, stars.count { it.layer == layer }, "$layer")
        }
        assertEquals(SkyLayer.entries.sumOf { it.count }, stars.size)
    }

    /** Everything is a fraction of the screen, so nothing may sit outside it. */
    @Test
    fun `nothing lies off the screen`() {
        for (star in Sky.stars()) {
            assertTrue(star.x in 0f..1f && star.y in 0f..1f, "Stern bei ${star.x}/${star.y}")
        }
        for (cloud in Sky.clouds(1f)) {
            assertTrue(cloud.x in 0f..1f && cloud.y in 0f..1f)
            assertTrue(cloud.warmth in 0f..1f)
        }
        for (grain in Sky.dust()) {
            assertTrue(grain.x in 0f..1f && grain.y in 0f..1f)
        }
    }

    /**
     * The background has to stay background.
     *
     * Every one of these blocks sits behind the body, the numbers and the shop. A nebula that
     * reads as a shape in its own right is a nebula competing with the thing the player is
     * tapping.
     */
    @Test
    fun `nothing in the background is opaque`() {
        for (cloud in Sky.clouds(1f)) {
            assertTrue(cloud.alpha in 0.01f..0.25f, "Wolke mit Deckkraft ${cloud.alpha}")
        }
        for (grain in Sky.dust()) {
            assertTrue(grain.alpha in 0.01f..0.25f, "Staub mit Deckkraft ${grain.alpha}")
        }
    }

    @Test
    fun `the sky thickens as the ladder climbs`() {
        val thin = Sky.clouds(0f).size
        val thick = Sky.clouds(1f).size

        assertTrue(thin > 0, "Ganz unten ist gar keine Wolke")
        assertTrue(thick > thin * 1.2, "Oben ist der Himmel kaum voller: $thin gegenüber $thick")
    }

    /**
     * A cloud has to be a cloud.
     *
     * Blocks scattered evenly over the screen are static, not nebula, and the difference does not
     * show up in any count — only in whether they gather anywhere. Sixteen cells, and the busiest
     * has to hold several times what the emptiest does.
     */
    @Test
    fun `the clouds gather instead of spreading out`() {
        val cells = IntArray(16)
        for (cloud in Sky.clouds(1f)) {
            val column = (cloud.x * 4f).toInt().coerceAtMost(3)
            val row = (cloud.y * 4f).toInt().coerceAtMost(3)
            cells[row * 4 + column]++
        }

        val busiest = cells.max()
        val quietest = cells.min()
        assertTrue(
            busiest > quietest * 3 + 3,
            "Die Wolken sind gleichmäßig verteilt: dichteste $busiest, dünnste $quietest",
        )
    }

    @Test
    fun `the dust stays in its lane`() {
        val dust = Sky.dust()
        assertTrue(dust.isNotEmpty())

        for (grain in dust) {
            val offset = abs(grain.y - Sky.spineAt(grain.x))
            assertTrue(
                offset < Sky.LANE_WIDTH,
                "Staubkorn ${offset} von der Bahn entfernt, erlaubt ist ${Sky.LANE_WIDTH}",
            )
        }
    }

    @Test
    fun `the densest dust sits on the spine`() {
        val dust = Sky.dust()
        val onSpine = dust.filter { abs(it.y - Sky.spineAt(it.x)) < Sky.LANE_WIDTH * 0.25f }
        val atEdge = dust.filter { abs(it.y - Sky.spineAt(it.x)) > Sky.LANE_WIDTH * 0.75f }

        assertTrue(onSpine.isNotEmpty() && atEdge.isNotEmpty(), "Die Bahn hat keine Ränder")
        assertTrue(
            onSpine.size > atEdge.size,
            "Am Rand liegt mehr Staub als in der Mitte: ${atEdge.size} gegen ${onSpine.size}",
        )
        assertTrue(onSpine.maxOf { it.alpha } > atEdge.maxOf { it.alpha })
    }

    /** The drift runs forever; without the wrap the field walks off the screen and never returns. */
    @Test
    fun `a drifting position always comes back around`() {
        for (value in listOf(-3.4f, -1f, -0.25f, 0f, 0.5f, 1f, 2.75f, 99.9f)) {
            val wrapped = Sky.wrap(value)
            assertTrue(wrapped >= 0f && wrapped < 1f, "$value wurde zu $wrapped")
        }
        assertEquals(0.25f, Sky.wrap(4.25f), 1e-5f)
        assertEquals(0.75f, Sky.wrap(-0.25f), 1e-5f)
    }

    @Test
    fun `the layers really are three different distances`() {
        val drifts = SkyLayer.entries.map { it.drift }
        assertEquals(drifts.size, drifts.toSet().size, "Zwei Ebenen ziehen gleich schnell")
        assertEquals(
            drifts.sorted(),
            drifts,
            "Die Ebenen stehen nicht von fern nach nah",
        )
        assertTrue(
            SkyLayer.entries.first().brightness < SkyLayer.entries.last().brightness,
            "Der ferne Stern ist nicht der blassere",
        )
    }

    /** Cheap enough to draw every frame is the whole reason any of this is precomputed. */
    @Test
    fun `the sky stays within a sane number of blocks`() {
        val total = Sky.stars().size + Sky.clouds(1f).size + Sky.dust().size
        assertTrue(total < 1_500, "Der Himmel kostet $total Blöcke pro Bild")
    }
}
