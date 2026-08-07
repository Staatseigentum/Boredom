package com.staatseigentum.kollaps.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.ui.SpriteCache
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Renders the game to PNG files without a display.
 *
 * This is what makes the harness useful in a place with no screen: the real screen, composed and
 * drawn by the real Compose runtime, ends up as an image that can be looked at. A window would
 * be nicer to play in, but it cannot be inspected from a build log.
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "screenshots").apply { mkdirs() }
    val width = 411
    val height = 891
    val density = Density(2.75f)

    // The sheet loads on a background coroutine, and a still frame will not wait for it. Warming
    // the cache first means the first composition already has the sprite in hand.
    for (tier in Tiers.all) runBlocking { SpriteCache.sheet(tier, DesktopSprites) }

    fun shoot(name: String, tier: Int, prepare: (DesktopGame) -> Unit = {}) {
        val game = DesktopGame().apply { seekToTier(tier); prepare(this) }
        ImageComposeScene(
            width = (width * density.density).toInt(),
            height = (height * density.density).toInt(),
            density = density,
        ) {
            DesktopPlatform { StillGame(game, width, height) }
        }.let { scene ->
            try {
                // A couple of frames so layout settles and the spin animation has a value.
                scene.render(0)
                val image = scene.render(16_000_000)
                File(out, "$name.png").writeBytes(
                    image.encodeToData(EncodedImageFormat.PNG)!!.bytes,
                )
            } finally {
                scene.close()
            }
        }
        println("  $name.png  (${Tiers.all[tier].name})")
    }

    println("Bildschirme:")
    shoot("01-start", 0)
    shoot("02-erde", 6)
    shoot("03-saturn", 9)
    shoot("04-ueberriese", 15)
    shoot("05-neutronenstern", 16)
    shoot("06-schwarzes-loch", 17)

    // The reported bug lives in the *change* of tier, not in any single one: the sheet was kept
    // from the previous body while the edge length was recomputed for the new one. So this shot
    // walks the ladder inside one composition, exactly as playing does.
    println("Stufenwechsel in einer laufenden Komposition:")
    val game = DesktopGame().apply { seekToTier(15) }
    ImageComposeScene(
        width = (width * density.density).toInt(),
        height = (height * density.density).toInt(),
        density = density,
    ) {
        DesktopPlatform { StillGame(game, width, height) }
    }.let { scene ->
      try {
        var time = 0L
        for ((step, tier) in listOf(15, 16, 17).withIndex()) {
            game.seekToTier(tier)
            repeat(3) {
                time += 16_000_000
                scene.render(time)
            }
            time += 16_000_000
            val image = scene.render(time)
            val name = "wechsel-${step + 1}-${Tiers.all[tier].name.lowercase().replace(' ', '-')}"
            File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            println("  $name.png")
        }
      } finally {
        scene.close()
      }
    }

    println("geschrieben nach ${out.absolutePath}")
}
