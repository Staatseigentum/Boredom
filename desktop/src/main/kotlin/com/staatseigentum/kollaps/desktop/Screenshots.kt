package com.staatseigentum.kollaps.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.staatseigentum.kollaps.core.Collectors
import com.staatseigentum.kollaps.core.GameEngine
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

    fun shoot(name: String, game: DesktopGame, tab: Int = 0, note: String = "") {
        ImageComposeScene(
            width = (width * density.density).toInt(),
            height = (height * density.density).toInt(),
            density = density,
        ) {
            DesktopPlatform { StillGame(game, width, height, tab) }
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
        println("  $name.png  $note")
    }

    fun atTier(tier: Int): DesktopGame = DesktopGame().apply { seekToTier(tier) }

    println("Bildschirme:")
    shoot("01-start", atTier(0), note = "(${Tiers.all[0].name})")
    shoot("02-erde", atTier(6), note = "(${Tiers.all[6].name})")
    shoot("03-saturn", atTier(9), note = "(${Tiers.all[9].name})")
    shoot("04-ueberriese", atTier(15), note = "(${Tiers.all[15].name})")
    shoot("05-neutronenstern", atTier(16), note = "(${Tiers.all[16].name})")
    shoot("06-schwarzes-loch", atTier(17), note = "(${Tiers.all[17].name})")

    // The new panels are all about a long game: an empty save shows an empty prestige shop, no
    // achievements and no collectors in orbit, which is exactly the state that proves nothing.
    val veteran = DesktopGame().apply {
        seekToTier(12)
        edit {
            it.copy(
                mass = it.mass * 60,
                collapses = 6,
                singularities = 34.0,
                taps = 4_812,
                cometsCaught = 17,
                playedSeconds = 5.5 * 3600,
            )
        }
        Collectors.all.forEachIndexed { index, collector ->
            repeat((14 - index).coerceAtLeast(1)) { buyCollector(collector.id) }
        }
        edit { GameEngine.award(it) }
    }
    shoot("07-kollektoren", veteran, tab = 0, note = "(gespieltes Spiel)")
    shoot("08-erfolge", veteran, tab = 2)
    shoot("09-kosmos", veteran, tab = 3)

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
