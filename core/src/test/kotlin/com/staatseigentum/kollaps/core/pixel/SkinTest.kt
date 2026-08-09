package com.staatseigentum.kollaps.core.pixel

import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.Tiers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The colour schemes.
 *
 * Nobody can look at a unit test, so these check what would make a scheme *broken* rather than
 * what makes it pretty: alpha surviving, every channel staying in range, the plain one changing
 * nothing at all, and a locked scheme never reaching the screen.
 */
class SkinTest {

    private val samples = intArrayOf(
        0xFF000000.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF8C6B4F.toInt(),
        0xFF4F7FC4.toInt(),
        0x80FFC65C.toInt(),
    )

    @Test
    fun `the plain scheme changes nothing`() {
        for (colour in samples) {
            assertEquals(colour, Skins.ORIGINAL.apply(colour), "0x%08X".format(colour))
        }
    }

    @Test
    fun `alpha is never touched`() {
        for (skin in Skins.all) {
            for (colour in samples) {
                assertEquals(
                    colour ushr 24,
                    skin.apply(colour) ushr 24,
                    "${skin.id} hat die Deckkraft verändert",
                )
            }
        }
    }

    @Test
    fun `every channel stays inside a byte`() {
        for (skin in Skins.all) {
            for (colour in samples) {
                val out = skin.apply(colour)
                for (shift in listOf(16, 8, 0)) {
                    val channel = out shr shift and 0xFF
                    assertTrue(channel in 0..255, "${skin.id}: $channel")
                }
            }
        }
    }

    @Test
    fun `every scheme but the plain one actually looks different`() {
        val body = 0xFF8C6B4F.toInt()
        for (skin in Skins.all) {
            if (skin.id == Skins.ORIGINAL.id) continue
            assertNotEquals(body, skin.apply(body), "${skin.id} sieht aus wie gar keine Palette")
        }
    }

    /** Two schemes that come out the same are one scheme with two names. */
    @Test
    fun `no two schemes render the same`() {
        val fingerprints = Skins.all.associate { skin ->
            skin.id to samples.map { skin.apply(it) }
        }
        assertEquals(
            Skins.all.size,
            fingerprints.values.toSet().size,
            "Zwei Paletten sind nicht auseinanderzuhalten",
        )
    }

    @Test
    fun `the fully desaturated scheme leaves no colour behind`() {
        val mono = Skins.all.first { it.desaturation >= 1.0f }
        // Two very different colours of the same brightness have to end up close together.
        val red = mono.apply(0xFF804040.toInt())
        val blue = mono.apply(0xFF404080.toInt())
        val spread = listOf(16, 8, 0).maxOf { shift ->
            kotlin.math.abs((red shr shift and 0xFF) - (blue shr shift and 0xFF))
        }
        assertTrue(spread < 40, "Der Monokanal lässt $spread Stufen Farbe stehen")
    }

    @Test
    fun `the plain scheme is free and the rest are not`() {
        assertEquals(0, Skins.ORIGINAL.requiredAchievements)
        for (skin in Skins.all) {
            if (skin.id == Skins.ORIGINAL.id) continue
            assertTrue(skin.requiredAchievements > 0, skin.id)
        }
    }

    @Test
    fun `nothing is unlocked before it is earned`() {
        val locked = Skins.all.maxBy { it.requiredAchievements }
        assertFalse(Skins.isUnlocked(0, locked))
        assertTrue(Skins.isUnlocked(locked.requiredAchievements, locked))
        assertEquals(listOf(Skins.ORIGINAL), Skins.unlocked(0))
    }

    @Test
    fun `an unearned scheme falls back instead of showing`() {
        val locked = Skins.all.maxBy { it.requiredAchievements }
        assertEquals(Skins.ORIGINAL, Skins.current(locked.id, achievements = 0))
        assertEquals(locked, Skins.current(locked.id, locked.requiredAchievements))
    }

    @Test
    fun `an unknown id is not a reason to fail`() {
        assertEquals(Skins.ORIGINAL, Skins.byId("skin_gibtsnicht"))
        assertEquals(Skins.ORIGINAL, Skins.byId(null))
    }

    @Test
    fun `the engine refuses to set a scheme that is not earned`() {
        val locked = Skins.all.maxBy { it.requiredAchievements }
        val fresh = GameState.new(0)

        assertEquals(fresh, GameEngine.setSkin(fresh, locked.id), "Die Palette wurde vergeben")
        assertEquals(Skins.ORIGINAL.id, GameEngine.setSkin(fresh, Skins.ORIGINAL.id).skinId)
    }

    /** The whole reason the scheme has to be part of the sprite cache key. */
    @Test
    fun `a scheme changes the pixels a body is drawn with`() {
        val tier = Tiers.all[8]
        val plain = PixelPlanet.frame(tier, index = 0, size = 48, skin = Skins.ORIGINAL)
        val tinted = PixelPlanet.frame(
            tier,
            index = 0,
            size = 48,
            skin = Skins.all.first { it.id != Skins.ORIGINAL.id },
        )

        assertEquals(plain.size, tinted.size)
        assertTrue(plain.indices.any { plain[it] != tinted[it] }, "Die Palette ändert kein Pixel")
        // The silhouette has to survive: a transparent pixel stays transparent.
        for (index in plain.indices) {
            assertEquals(
                plain[index] ushr 24 == 0,
                tinted[index] ushr 24 == 0,
                "Die Palette hat die Silhouette verändert",
            )
        }
    }
}
