package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveCodecTest {

    private val sample = GameState(
        mass = 1234.5,
        runMass = 98_765.0,
        totalMass = 1e9,
        collectors = mapOf("dust" to 12, "net" to 3),
        upgrades = setOf("tap_1", "dust_10"),
        singularities = 7.0,
        collapses = 2,
        taps = 4_242,
        bestTier = 11,
        bestRunMass = 5e12,
        celebratedTier = 9,
        lastSeenAt = 1_700_000_000_000,
        startedAt = 1_600_000_000_000,
    )

    @Test
    fun `a save survives a round trip unchanged`() {
        assertEquals(sample, SaveCodec.decode(SaveCodec.encode(sample)))
    }

    @Test
    fun `unreadable input decodes to null instead of throwing`() {
        assertNull(SaveCodec.decode(null))
        assertNull(SaveCodec.decode(""))
        assertNull(SaveCodec.decode("   "))
        assertNull(SaveCodec.decode("kein json"))
        assertNull(SaveCodec.decode("{\"mass\": \"viel\"}"))
    }

    @Test
    fun `unknown fields from a newer build are ignored`() {
        val raw = """{"version":1,"mass":50.0,"runMass":50.0,"achievements":["was auch immer"]}"""
        val decoded = SaveCodec.decode(raw)
        assertEquals(50.0, decoded?.mass)
    }

    @Test
    fun `references to content that no longer exists are dropped`() {
        val raw = SaveCodec.encode(
            sample.copy(
                collectors = mapOf("dust" to 5, "geloescht" to 9),
                upgrades = setOf("tap_1", "gibtsnichtmehr"),
            ),
        )
        val decoded = SaveCodec.decode(raw)!!
        assertEquals(mapOf("dust" to 5), decoded.collectors)
        assertEquals(setOf("tap_1"), decoded.upgrades)
    }

    @Test
    fun `a decoded save keeps working in the engine`() {
        val decoded = SaveCodec.decode(SaveCodec.encode(sample))!!
        assertTrue(GameEngine.massPerSecond(decoded) > 0.0)
        assertEquals(GameEngine.massPerSecond(sample), GameEngine.massPerSecond(decoded))
    }

    @Test
    fun `saves stay small`() {
        // The save goes into DataStore on every autosave, so it must not balloon.
        assertTrue(SaveCodec.encode(sample).length < 2_000)
    }
}
