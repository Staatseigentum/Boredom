package com.staatseigentum.kollaps.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A rendered sprite sheet together with the edge length its frames were rendered at.
 *
 * The two travel together on purpose. Sprite size varies per tier, so a sheet and a separately
 * derived edge length can disagree — and when they did, the draw took a crop of the wrong size
 * out of the previous tier's sheet and showed a piece of the wrong body.
 */
class SpriteSheet(val side: Int, val frames: List<ImageBitmap>)

/**
 * Turning raw pixels into a bitmap is the one step in the whole renderer that each platform does
 * its own way, so it is the only thing the interface has to be handed from outside.
 */
fun interface SpriteFactory {
    fun bitmap(pixels: IntArray, side: Int): ImageBitmap
}

val LocalSpriteFactory = staticCompositionLocalOf<SpriteFactory> {
    error("Kein SpriteFactory bereitgestellt")
}

/**
 * Keeps the last couple of sprite sheets around.
 *
 * Two is enough on purpose: the game only ever shows the current body, and the sheet for the
 * largest tier is several megabytes. Holding all eighteen would cost far more memory than the
 * rendering it saves is worth.
 */
object SpriteCache {

    private const val KEEP = 2

    private val lock = Mutex()
    private val sheets = LinkedHashMap<Int, SpriteSheet>()

    /** The sheet if it has already been built, for showing a body without a blank frame first. */
    fun ready(tier: CelestialTier): SpriteSheet? = synchronized(sheets) { sheets[tier.index] }

    suspend fun sheet(tier: CelestialTier, factory: SpriteFactory): SpriteSheet {
        ready(tier)?.let { return it }
        return lock.withLock {
            // Another caller may have finished it while this one waited for the lock.
            ready(tier) ?: withContext(Dispatchers.Default) {
                val side = PixelPlanet.size(tier)
                SpriteSheet(side, PixelPlanet.frames(tier).map { factory.bitmap(it, side) })
            }.also { built ->
                synchronized(sheets) {
                    sheets[tier.index] = built
                    while (sheets.size > KEEP) sheets.remove(sheets.keys.first())
                }
            }
        }
    }

    /** Drops everything. Only the desktop harness needs this, to measure a cold render. */
    fun clear() = synchronized(sheets) { sheets.clear() }
}
