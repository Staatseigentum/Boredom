package com.staatseigentum.kollaps.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.staatseigentum.kollaps.core.AutomationRule
import com.staatseigentum.kollaps.core.Collectors
import com.staatseigentum.kollaps.core.Fusion
import com.staatseigentum.kollaps.core.GameEngine
import com.staatseigentum.kollaps.core.Heavy
import com.staatseigentum.kollaps.core.ResearchTree
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.Wallclock
import com.staatseigentum.kollaps.ui.PhoneView
import com.staatseigentum.kollaps.ui.SpriteCache
import com.staatseigentum.kollaps.ui.sectionsFor
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
    // Same clock the window installs. The harness renders a lab mid-project, and the bar it
    // draws is counted against this.
    Wallclock.readFrom(System::currentTimeMillis)

    val out = File(args.firstOrNull() ?: "screenshots").apply { mkdirs() }
    val width = 411
    val height = 891
    val density = Density(2.75f)

    // Building a body is synchronous now, so this is no longer load-bearing for the first frame —
    // but it still keeps the cost out of the composition being measured, and it is the one place
    // that proves every rung on the ladder can actually be built.
    for (tier in Tiers.all) SpriteCache.sprite(tier)

    /**
     * What a rendered screen has to be true of, checked from the pixels themselves.
     *
     * The sweep added earlier proves a screen does not *throw*. That is a real class of fault and
     * it caught a real crash — but it is a low bar, and the overhaul it was written for had already
     * produced a fault it would have sailed past: the whole-number scaling rule that would have
     * drawn every body at two thirds of its size. Nothing about that throws. Everything about it is
     * wrong.
     *
     * These are intrinsic properties rather than a stored reference image, and that is a deliberate
     * trade. A golden image catches more — every stray pixel — at the cost of a baseline that has to
     * be regenerated for every legitimate change, on a machine that can run the renderer, which is
     * not the one this is usually developed on. These catch the failures that matter (a blank
     * screen, a screen that is one flat colour, a body that stopped filling its area) and cost
     * nothing to maintain.
     */
    fun inspect(name: String, image: org.jetbrains.skia.Image) {
        val bitmap = org.jetbrains.skia.Bitmap().apply {
            allocPixels(org.jetbrains.skia.ImageInfo.makeN32Premul(image.width, image.height))
        }
        check(image.readPixels(bitmap, 0, 0)) { "$name: konnte nicht ausgelesen werden" }

        var lit = 0
        var total = 0
        val histogram = HashMap<Int, Int>()
        // Every eighth pixel in each direction: a sixty-fourth of the work for an answer that is
        // identical at this granularity.
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                val colour = bitmap.getColor(x, y)
                histogram[colour] = (histogram[colour] ?: 0) + 1
                // Anything that is not the background counts as something being drawn.
                if (colour != BACKGROUND) lit++
                total++
                x += 8
            }
            y += 8
        }
        bitmap.close()

        val drawn = lit.toFloat() / total
        check(drawn > 0.04f) {
            "$name: praktisch leer, nur ${(drawn * 100).toInt()} % der Fläche ist bemalt"
        }
        val commonest = histogram.values.max().toFloat() / total
        check(commonest < 0.97f) {
            "$name: zu ${(commonest * 100).toInt()} % eine einzige Farbe — vermutlich nichts gerendert"
        }
    }

    fun shoot(
        name: String,
        game: DesktopGame,
        tab: Int = 0,
        /** Which section of the Kosmos tab, for the four it was split into. */
        section: Int = 0,
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
            DesktopPlatform(withAudio = false) { StillGame(game, w, h, tab, section) }
        }.let { scene ->
            try {
                // A couple of frames so layout settles and the spin animation has a value.
                scene.render(0)
                val image = scene.render(16_000_000)
                val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
                File(out, "$name.png").writeBytes(bytes)
                inspect(name, image)
            } finally {
                scene.close()
            }
        }
        println("  $name.png  $note")
    }

    /**
     * Which position a Kosmos section sits at *for this save*.
     *
     * The list is filtered — no sky before the first big bang, no lab before it is unlocked — so a
     * written-down index means a different panel from one state to the next, and it did: the shot
     * labelled "Erfolge" had been photographing the orbits since the tab list changed length.
     */
    fun sectionOf(game: DesktopGame, name: String): Int =
        sectionsFor(game.state, game.stats).indexOfFirst { it.name == name }.coerceAtLeast(0)

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
    shoot("20-kollektoren", veteran, tab = 1, note = "(gespieltes Spiel, mit Meilensteinen)")
    shoot("21-erfolge", veteran, tab = 4, section = sectionOf(veteran, "ACHIEVEMENTS"))
    shoot("22-kosmos", veteran, tab = 4)

    // The Kosmos tab shows either the list of challenges or the one being run, never both, so
    // it takes two states to see the whole feature.
    val challenging = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Saturn"))
        edit { it.copy(collapses = 6, singularities = 34.0) }
        startChallenges(setOf("c_hand"))
        edit { GameEngine.tick(it, 11 * 60.0) }
    }
    shoot("23-herausforderung", challenging, tab = 4, section = sectionOf(challenging, "RULES"), note = "(Herausforderung läuft)")
    shoot("24-querformat", veteran, tab = 1, note = "(Tablet, zweispaltig)", wide = true)
    shoot("25-querformat-kosmos", veteran, tab = 4, note = "(Tablet, Kosmos)", wide = true)

    // A star with the whole chain lit. The tab only exists once the body has ignited, which is
    // why this needs its own state rather than another photograph of the veteran above.
    val fusing = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Roter Zwerg"))
        edit { it.copy(mass = it.mass * 400) }
        Fusion.stages.forEach { stage -> repeat(3) { buyFuser(stage.id) } }
        // Long enough for every tank to have something in it, so no chip reads as a dash.
        edit { GameEngine.tick(it, 25 * 60.0) }
        // And a few collapses' worth of heavy elements, which is the strip underneath.
        edit {
            it.copy(heavy = Heavy.forge(Heavy.forge(emptyMap(), 40_000.0), 90_000.0))
        }
    }
    shoot("26-fusion", fusing, tab = 3, note = "(Fusionskette läuft)")

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
    shoot("27-labor", researching, tab = 4, section = sectionOf(researching, "LAB"), note = "(Forschung läuft)")

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
        cycleAutomation(AutomationRule.RESEARCH.id)
    }
    shoot("28-automatik", automated, tab = 4, section = sectionOf(automated, "RULES"), note = "(Tablet, Regeln)", wide = true)

    shoot("29-system", veteran, tab = 4, section = sectionOf(veteran, "SYSTEM"), note = "(Statistik und Einstellungen)")

    // The star system, with bodies on most slots and two of them in resonance. Grown by ticking
    // rather than by setting masses directly, so the picture is one the game can actually reach.
    val orbiting = DesktopGame().apply {
        seekToTier(Tiers.indexOf("Roter Zwerg"))
        edit { it.copy(mass = it.mass * 3_000) }
        repeat(6) { openOrbit() }
        listOf(0, 1, 2, 4, 5).forEach { seedSatellite(it) }
        edit { GameEngine.tick(it, 45 * 60.0) }
    }
    shoot("30-bahnen", orbiting, tab = 2, note = "(Bahnen und Trabanten)")
    shoot("31-system-koerper", orbiting, tab = 0, note = "(Trabanten um den Körper)")

    // The tier change is its own bug surface, so it gets walked inside one composition rather
    // than photographed rung by rung — see [COLLAPSE_WALK].
    println("Stufenwechsel in einer laufenden Komposition:")
    val game = DesktopGame().apply { seekToTier(Tiers.indexOf(COLLAPSE_WALK.first())) }
    ImageComposeScene(
        width = (width * density.density).toInt(),
        height = (height * density.density).toInt(),
        density = density,
    ) {
        DesktopPlatform(withAudio = false) { StillGame(game, width, height) }
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

    /*
     * Every area and every section, rendered once.
     *
     * This exists because of a crash that shipped. Moving the achievements into the Kosmos panel
     * nested one lazy list inside another, which Compose does not draw badly — it throws — and the
     * whole section took the game down the first time anybody opened it. Nothing here caught it,
     * because the shots above are a hand-picked list of interesting screens and that section was
     * not on it.
     *
     * So the list stops being hand-picked. Rendering a screen is enough to prove it *can* be
     * rendered, which is exactly the class of fault a layout error is; anything that throws fails
     * this job and never reaches a release. The pictures are written out too, because a screen
     * nobody has ever looked at is its own kind of untested.
     */
    println("Durchlauf durch alle Bereiche:")
    val sections = sectionsFor(veteran.state, veteran.stats)
    for ((index, view) in PhoneView.availableIn(veteran.state).withIndex()) {
        if (view == PhoneView.COSMOS) {
            for ((sectionIndex, section) in sections.withIndex()) {
                shoot("90-${view.name.lowercase()}-${section.name.lowercase()}", veteran,
                    tab = index, section = sectionIndex)
            }
        } else {
            shoot("90-${view.name.lowercase()}", veteran, tab = index)
        }
    }
    // And the wide layout's own sections, which are a different panel entirely.
    for ((sectionIndex, section) in sections.withIndex()) {
        shoot("91-breit-${section.name.lowercase()}", veteran, section = sectionIndex, wide = true)
    }

    /*
     * What one frame of a body costs to compute.
     *
     * The phone renders these live, eight times a second, and the resolution was doubled on the
     * strength of an argument rather than a measurement. This is the measurement. It is printed for
     * every rung and bounded for the worst of them, so raising the resolution again is a decision
     * somebody takes with a number in front of them.
     *
     * A generous ceiling: a build machine is not a phone and this is not a benchmark. It is here to
     * notice an order of magnitude, which is what a change to the renderer would cost.
     */
    println("Renderkosten je Sprosse:")
    var worst = 0L
    for (tier in Tiers.all) {
        val sprite = SpriteCache.sprite(tier)
        val buffer = sprite.buffer()
        sprite.render(0, buffer)
        val nanos = (0 until 4).minOf {
            var taken = 0L
            taken = kotlin.system.measureNanoTime { sprite.render(it + 1, buffer) }
            taken
        }
        worst = maxOf(worst, nanos)
        println("  ${tier.name.padEnd(18)} ${sprite.side}px  ${nanos / 1_000_000.0} ms")
    }
    check(worst < FRAME_BUDGET_MILLIS * 1_000_000) {
        "Ein Frame braucht ${worst / 1_000_000.0} ms — über dem Budget von $FRAME_BUDGET_MILLIS ms"
    }

    println("geschrieben nach ${out.absolutePath}")
}

/**
 * The window's own background, as Skia reports it. See [Space] — everything else on screen is
 * something the game drew.
 */
private const val BACKGROUND = 0xFF05060F.toInt()

/**
 * How long one sprite frame may take to compute, in milliseconds.
 *
 * Deliberately loose. At a six second turn the phone asks for eight of these a second, so even a
 * tenth of this would be comfortable — the number is here to catch a renderer change that costs an
 * order of magnitude, not to police a few per cent on a shared build machine.
 */
private const val FRAME_BUDGET_MILLIS = 120

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
