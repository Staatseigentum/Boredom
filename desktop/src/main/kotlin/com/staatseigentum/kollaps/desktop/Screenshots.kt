package com.staatseigentum.kollaps.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.staatseigentum.kollaps.core.AutomationRule
import com.staatseigentum.kollaps.core.Collectors
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.ResearchTree
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

    fun shoot(
        name: String,
        game: DesktopGame,
        tab: Int = 0,
        note: String = "",
        wide: Boolean = false,
    ) {
        // The wide shot is the only way anybody sees the two-column layout: nothing else in the
        // build renders at a size where it switches over.
        val w = if (wide) TABLET_WIDTH else width
        val h = if (wide) TABLET_HEIGHT else height
        ImageComposeScene(
            width = (w * density.density).toInt(),
            height = (h * density.density).toInt(),
            density = density,
        ) {
            DesktopPlatform { StillGame(game, w, h, tab) }
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
    shoot("24-querformat", veteran, tab = 0, note = "(Tablet, zweispaltig)", wide = true)
    shoot("25-querformat-kosmos", veteran, tab = 3, note = "(Tablet, Kosmos)", wide = true)

    // A star with the whole chain lit. The tab only exists once the body has ignited, which is
    // why this needs its own state rather than another photograph of the veteran above.
    val fusing = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Roter Zwerg"))
        edit { it.copy(mass = it.mass * 400) }
        Fusion.stages.forEach { stage -> repeat(3) { buyFuser(stage.id) } }
        // Long enough for every tank to have something in it, so no chip reads as a dash.
        edit { GameEngine.tick(it, 25 * 60.0) }
    }
    shoot("26-fusion", fusing, tab = 2, note = "(Fusionskette läuft)")

    // The lab, with a project part way through. An idle bench shows the catalogue and nothing
    // else, and the countdown is the half of it worth looking at.
    val researching = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Saturn"))
        edit { it.copy(mass = it.mass * 40, research = setOf("r_optics", "r_storage")) }
        startResearch("r_telemetry")
        // Wound back so the bar sits at roughly a third rather than at nothing.
        edit {
            val total = ResearchTree.duration(it, ResearchTree.byId("r_telemetry")!!)
            it.copy(researchDoneAt = System.currentTimeMillis() + (total * 660).toLong())
        }
    }
    shoot("27-labor", researching, tab = 3, note = "(Forschung läuft)")

    // Lab and standing orders in one frame, which only fits on the tablet: on a phone the second
    // card starts below the fold and a still cannot scroll to it.
    val automated = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Saturn"))
        edit {
            it.copy(
                mass = it.mass * 20,
                collapses = 4,
                prestigeUpgrades = setOf("p_autobuy"),
            )
        }
        cycleAutomation(AutomationRule.COLLECTORS.id)
        cycleAutomation(AutomationRule.COLLECTORS.id)
        cycleAutomation(AutomationRule.UPGRADES.id)
        cycleAutomation(AutomationRule.COLLAPSE.id)
        cycleAutomation(AutomationRule.COLLAPSE.id)
    }
    shoot("28-automatik", automated, tab = 3, note = "(Tablet, Regeln)", wide = true)

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

/** A landscape tablet, where the screen puts the body and the shop side by side. */
private const val TABLET_WIDTH = 1_024
private const val TABLET_HEIGHT = 700

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
