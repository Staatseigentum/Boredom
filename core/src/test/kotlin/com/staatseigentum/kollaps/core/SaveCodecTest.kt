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
    fun `a save from before the ladder grew still points at the same bodies`() {
        // Version 3 pushed seven bodies into the middle of the ladder. A version 2 save stores
        // raw indices, so without the remap a player who had reached the neutron star would find
        // their record demoted to the red supergiant and be shown celebrations again.
        val v2 = """
            {"version":2,"mass":1.0,"runMass":1.0,"totalMass":1.0,
             "bestTier":16,"celebratedTier":13,"taps":5}
        """.trimIndent()

        val decoded = SaveCodec.decode(v2)!!
        assertEquals(GameState.SAVE_VERSION, decoded.version)
        assertEquals(Tiers.indexOf("Neutronenstern"), decoded.bestTier)
        assertEquals(Tiers.indexOf("Sonne"), decoded.celebratedTier)
    }

    @Test
    fun `a save written by this build is left alone`() {
        val current = sample.copy(bestTier = 22, celebratedTier = 20)
        val decoded = SaveCodec.decode(SaveCodec.encode(current))!!
        assertEquals(22, decoded.bestTier)
        assertEquals(20, decoded.celebratedTier)
    }

    @Test
    fun `saves stay small`() {
        // The save goes into DataStore on every autosave, so it must not balloon.
        assertTrue(SaveCodec.encode(sample).length < 2_000)
    }
}
