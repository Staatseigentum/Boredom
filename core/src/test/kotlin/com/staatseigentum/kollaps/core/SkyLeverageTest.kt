package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The sky's pull on production, and the three things that must not have moved with it.
 *
 * A full sky used to be worth about five times production — eight finished universes and every
 * Äon spent on them, against an active universe multiplied by the designation ladder, the orbits,
 * the path tree and the singularity shelf. Fördern carries leverage now. The point of putting that
 * leverage in [Multiverse.multiplier] alone is that nothing else in the economy notices.
 */
class SkyLeverageTest {

    private fun galaxy(slot: Int, level: Int, job: GalaxyJob = GalaxyJob.FOERDERN) = ParkedUniverse(
        slot = slot,
        bestTier = Tiers.indexOf("Schwarzes Loch"),
        collapses = 20,
        singularities = 500.0,
        level = level,
        jobId = job.id,
    )

    private fun sky(level: Int, slots: Int = Multiverse.SLOTS, job: GalaxyJob = GalaxyJob.FOERDERN) =
        GameState(universes = (0 until slots).map { galaxy(it, level, job) })

    @Test
    fun `a full sky is worth several times what it used to be`() {
        val bare = Multiverse.multiplier(sky(level = 0))
        val built = Multiverse.multiplier(sky(level = Multiverse.MAX_LEVEL))

        // Eight galaxies at the black hole, which is the shallowest a *finished* sky can be —
        // one built on the designation ladder above it is worth a good deal more again.
        assertTrue(bare > 10.0, "Ein voller Himmel bringt immer noch fast nichts: ×$bare")
        assertTrue(built > 50.0, "Ein fertig ausgebauter Himmel bringt nur ×$built")
        // Roughly a quadrupling for the hundred-and-twenty levels, which are close to a thousand
        // Äonen all told — the sky earning its own build-out back several times over is the point.
        assertTrue(
            built > bare * 3.5,
            "Der Ausbau lohnt sich kaum: ×$bare ohne, ×$built mit",
        )
    }

    @Test
    fun `building the sky out is what moves it, not merely filling it`() {
        // Three galaxies, every one of them finished, is still three eighths of a sky.
        val partial = Multiverse.completion(sky(level = Multiverse.MAX_LEVEL, slots = 3))
        val whole = Multiverse.completion(sky(level = Multiverse.MAX_LEVEL))

        assertEquals(1.0, Multiverse.completion(sky(level = 0)), 1e-9)
        assertEquals(1.0 + Multiverse.COMPLETION_BONUS, whole, 1e-9)
        assertTrue(partial < whole, "Ein Drittel Himmel zählt wie ein ganzer")
    }

    /**
     * The guard that matters. Leverage lives in the production multiplier and nowhere else, so a
     * galaxy told to think, look or dig earns exactly what it earned before this existed.
     */
    @Test
    fun `the other three jobs are untouched by the leverage`() {
        val thinking = sky(level = Multiverse.MAX_LEVEL, job = GalaxyJob.RECHNEN)
        val looking = sky(level = Multiverse.MAX_LEVEL, job = GalaxyJob.SUCHEN)
        val digging = sky(level = Multiverse.MAX_LEVEL, job = GalaxyJob.GRABEN)

        val weight = Multiverse.parked(thinking).sumOf { Multiverse.weightedYieldOf(thinking, it) }

        assertEquals(
            weight * Multiverse.AEON_FOCUS * Multiverse.AEONS_PER_HOUR / 3_600.0,
            Multiverse.aeonsPerSecond(thinking),
            1e-12,
            "Die Äonen sind mitgewachsen",
        )
        assertEquals(
            1.0 + weight * Multiverse.COMET_PER_YIELD,
            Multiverse.cometFactor(looking),
            1e-9,
            "Die Kometen sind mitgewachsen",
        )
        assertEquals(
            weight * HeavyElement.entries.first().perRoot * Multiverse.METAL_PER_HOUR / 3_600.0,
            Multiverse.metalPerSecond(digging).getValue(HeavyElement.entries.first().id),
            1e-12,
            "Das Metall ist mitgewachsen",
        )
        // And none of the three pulls on production at all.
        assertEquals(1.0, Multiverse.multiplier(thinking), 1e-9)
    }

    @Test
    fun `a galaxy in changeover contributes nothing, leverage included`() {
        val ramping = GameState(
            universes = listOf(galaxy(0, Multiverse.MAX_LEVEL).copy(rampSeconds = 60.0)),
        )
        assertEquals(1.0, Multiverse.multiplier(ramping), 1e-9)
        assertEquals(0.0, Multiverse.productionShareOf(ramping, ramping.universes.single()), 1e-9)
    }
}
