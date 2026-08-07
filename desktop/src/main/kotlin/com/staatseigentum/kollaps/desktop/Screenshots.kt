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

    // By name rather than by number, so inserting a body into the ladder does not silently
    // repoint every screenshot at its neighbour.
    fun at(name: String): DesktopGame = DesktopGame().apply { seekToTier(Tiers.indexOf(name)) }

    println("Bildschirme:")
    for ((file, body) in LADDER_SHOTS) shoot(file, at(body), note = "($body)")

    // The new panels are all about a long game: an empty save shows an empty prestige shop, no
    // achievements and no collectors in orbit, which is exactly the state that proves nothing.
    val veteran = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Saturn"))
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
    shoot("20-kollektoren", veteran, tab = 0, note = "(gespieltes Spiel, mit Meilensteinen)")
    shoot("21-erfolge", veteran, tab = 2)
    shoot("22-kosmos", veteran, tab = 3)

    // The Kosmos tab shows either the list of challenges or the one being run, never both, so
    // it takes two states to see the whole feature.
    val challenging = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Saturn"))
        edit { it.copy(collapses = 6, singularities = 34.0) }
        startChallenge("c_hand")
        edit { GameEngine.tick(it, 11 * 60.0) }
    }
    shoot("23-herausforderung", challenging, tab = 3, note = "(Herausforderung läuft)")

    // The tier change is its own bug surface, so it gets walked inside one composition rather
    // than photographed rung by rung — see [COLLAPSE_WALK].
    println("Stufenwechsel in einer laufenden Komposition:")
    val game = DesktopGame().apply { seekToTier(Tiers.indexOf(COLLAPSE_WALK.first())) }
    ImageComposeScene(
        width = (width * density.density).toInt(),
        height = (height * density.density).toInt(),
        density = density,
    ) {
        DesktopPlatform { StillGame(game, width, height) }
    }.let { scene ->
      try {
        var time = 0L
        for ((step, body) in COLLAPSE_WALK.withIndex()) {
            game.seekToTier(Tiers.indexOf(body))
            repeat(3) {
                time += 16_000_000
                scene.render(time)
            }
            time += 16_000_000
            val image = scene.render(time)
            val name = "wechsel-${step + 1}-${body.lowercase().replace(' ', '-')}"
            File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            println("  $name.png")
        }
      } finally {
        scene.close()
      }
    }

    println("geschrieben nach ${out.absolutePath}")
}

/** One shot per interesting rung: file name, and the body it should be standing on. */
private val LADDER_SHOTS = listOf(
    "01-start" to "Meteorit",
    "02-mond" to "Mond",
    "03-erde" to "Erde",
    "04-saturn" to "Saturn",
    "05-heisser-jupiter" to "Heißer Jupiter",
    "06-ueberriese" to "Roter Überriese",
    "07-hyperriese" to "Hyperriese",
    "08-weisser-zwerg" to "Weißer Zwerg",
    "09-neutronenstern" to "Neutronenstern",
    "10-magnetar" to "Magnetar",
    "11-schwarzes-loch" to "Schwarzes Loch",
)

/**
 * The stretch where the star collapses, walked inside one composition.
 *
 * This is where the reported sprite bug lived: the sheet was kept from the previous body while
 * the edge length was recomputed for the new one, and these four rungs change size the hardest.
 */
private val COLLAPSE_WALK = listOf("Hyperriese", "Weißer Zwerg", "Neutronenstern", "Magnetar", "Schwarzes Loch")
