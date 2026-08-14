package com.staatseigentum.kollaps.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.staatseigentum.kollaps.core.CelestialTier
import com.staatseigentum.kollaps.core.pixel.PixelPlanet
import com.staatseigentum.kollaps.core.pixel.Skin
import com.staatseigentum.kollaps.core.pixel.Skins

/**
 * Turning raw sprite pixels into a bitmap is the one step in the whole renderer that each platform
 * does its own way, so it is the only thing the interface has to be handed from outside.
 */
fun interface SpriteFactory {
    fun bitmap(pixels: IntArray, side: Int): ImageBitmap

    /**
     * A target that can be written to over and over, for a body that is being turned.
     *
     * The default allocates a fresh bitmap per write, which is correct everywhere and is what the
     * desktop uses — it has the memory and the churn does not show. A platform where it does show
     * overrides this with something that writes into pixels it already owns; see the Android one.
     */
    fun surface(side: Int): SpriteSurface = object : SpriteSurface {
        override var image: ImageBitmap? = null
            private set

        override fun write(pixels: IntArray) {
            image = bitmap(pixels, side)
        }
    }
}

/**
 * One body's worth of pixels on the screen, rewritten as it turns.
 *
 * [image] is null until the first [write], which is the ordinary state for a body that has just
 * appeared and whose first frame is still being rendered.
 */
interface SpriteSurface {
    val image: ImageBitmap?

    fun write(pixels: IntArray)
}

val LocalSpriteFactory = staticCompositionLocalOf<SpriteFactory> {
    error("Kein SpriteFactory bereitgestellt")
}

/**
 * The colour scheme every body on screen is drawn in.
 *
 * A composition local rather than a parameter because the body appears in three places that have
 * nothing else to do with each other — the tap area, the tier celebration and the orbiting
 * satellites — and threading a palette through all of them would put a decoration into the
 * signature of half the screen. Defaults to the plain one, so previews and the harness need
 * provide nothing.
 */
val LocalSkin = staticCompositionLocalOf { Skins.ORIGINAL }

/**
 * Keeps the expensive half of a body around: its palette and its surface texture.
 *
 * This used to hold rendered sprite *sheets*, and that stopped being possible when the sprites
 * doubled in resolution and in frame count — forty-eight frames of Saturn is thirty-four megabytes,
 * and the cache held two bodies at a time. What is cached now is [PixelPlanet.Sprite], which is the
 * part that is slow to build and identical for every frame: the craters, the cloud bands, the
 * ramps. Drawing a frame from one is a single pass over the pixels, and only the frame actually on
 * screen is ever drawn.
 *
 * Everything else about it is unchanged, including why the key is a data class rather than a
 * string — see [Key].
 */
object SpriteCache {

    /**
     * How many bodies to keep.
     *
     * More than the two sheets used to be, and it still costs a fraction of what they did: a
     * texture is one byte and one float per texel, not four bytes per pixel per frame. Four covers
     * the case the old cache could not — the tap area, the celebration of the rung just reached and
     * a couple of satellites, all different bodies at the same moment.
     */
    private const val KEEP = 4

    private val sprites = LinkedHashMap<Key, PixelPlanet.Sprite>()

    /**
     * What identifies a built body: the rung and the colour scheme baked into its ramps.
     *
     * A data class rather than a formatted string, and that is not a matter of taste. This was a
     * string once, and a mangled escape turned the interpolation into a literal — so every tier
     * and every palette shared one key. The cache then handed back whichever entry it happened to
     * hold: switching palette appeared to do nothing, and climbing a rung could leave the previous
     * body on screen. Neither failed loudly; both just looked wrong.
     *
     * Two fields the compiler checks cannot be got wrong that way.
     */
    private data class Key(val tier: Int, val skin: String)

    /** The body if it has already been built, for showing one without waiting. */
    fun ready(tier: CelestialTier, skin: Skin = Skins.ORIGINAL): PixelPlanet.Sprite? =
        synchronized(sprites) { sprites[Key(tier.index, skin.id)] }

    /**
     * The body, built if this is the first time anybody has asked for it.
     *
     * Call this off the main thread. Building the texture means scattering thousands of craters or
     * cloud bands across a quarter of a million texels, which at the resolution the sprites are
     * drawn at now is tens of milliseconds — and a run climbs twenty-five rungs, so on the main
     * thread that is twenty-five freezes per run. It was cheap enough to do inline before the
     * surfaces doubled; it is not any more.
     */
    fun sprite(tier: CelestialTier, skin: Skin = Skins.ORIGINAL): PixelPlanet.Sprite {
        ready(tier, skin)?.let { return it }
        // Built outside the lock: two threads racing here would each build one and the loser's copy
        // is simply dropped, which is far cheaper than every caller queueing behind whoever is
        // building a body they do not want.
        val built = PixelPlanet.Sprite.of(tier, skin)
        return synchronized(sprites) {
            sprites.getOrPut(Key(tier.index, skin.id)) { built }.also {
                while (sprites.size > KEEP) sprites.remove(sprites.keys.first())
            }
        }
    }

    /** Drops everything. Only the desktop harness needs this, to measure a cold render. */
    fun clear() = synchronized(sprites) { sprites.clear() }
}
