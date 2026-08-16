package com.staatseigentum.kollaps.core.pixel

import com.staatseigentum.kollaps.core.Tiers
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The store cover, drawn out of the game rather than around it.
 *
 * Every planet on it is the sprite the game renders at runtime, from the same [PixelPlanet], and
 * the sky behind them is the same [Sky] the starfield is built from. That is the whole point of
 * generating it here instead of drawing one: a cover made in an image editor is a promise, and
 * this one is a screenshot of the renderer. If the art changes, the cover changes with it.
 *
 * ## Why this lives in a test
 *
 * It needs the renderer, `java.awt` and a file to write, and the test source set is the one place
 * in this project that has all three. It asserts what it produced — the size itch.io asks for, and
 * that the picture is neither empty nor a single flat colour — so a renderer that broke would fail
 * here rather than quietly shipping a black rectangle to a store page.
 */
class CoverTest {

    /** What itch.io asks for. The thumbnail is scaled from this, so it has to read small. */
    private val width = 630
    private val height = 500

    /** One pixel of the art is this many pixels of the cover — the whole look depends on it. */
    private val block = 2

    @Test
    fun `the cover is drawn from the game's own renderer`() {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

        paintSky(g)
        paintLadder(g)
        paintHero(g)
        paintTitle(g)

        g.dispose()

        val out = File(System.getProperty("kollaps.cover") ?: "/tmp/kollaps-cover-630x500.png")
        ImageIO.write(image, "png", out)

        assertTrue(out.length() > 0, "Das Titelbild wurde nicht geschrieben")
        assertTrue(image.width == width && image.height == height, "Falsche Größe")
        assertPainted(image)
    }

    // ------------------------------------------------------------------ the sky

    /**
     * Space, the nebula and the stars, in that order.
     *
     * Taken from [Sky] rather than scattered by hand here, so the cover's sky is the sky. The depth
     * is set near the top of the ladder because that is the thicker, more colourful field — a
     * meteorite's sky is almost empty, which is right in the game and dull on a thumbnail.
     */
    private fun paintSky(g: java.awt.Graphics2D) {
        g.color = Color(0x05, 0x06, 0x0F)
        g.fillRect(0, 0, width, height)

        /*
         * The nebula, drawn twice as wide as its own grid and considerably brighter than in the
         * game.
         *
         * In the game this field sits behind a whole interface and has to stay out of the way; on
         * a cover it is the only thing between the planets and a black rectangle. The first pass
         * at cover brightness produced 158 distinct colours in the whole image, which is what a
         * starfield on black looks like — technically a picture, and nothing to look at.
         */
        for (cloud in Sky.clouds(depth = 0.85f)) {
            val warm = cloud.warmth
            val r = (0x3A + 0x9A * warm).toInt().coerceIn(0, 255)
            val gr = (0x22 + 0x38 * warm).toInt().coerceIn(0, 255)
            val b = (0x78 + 0x87 * (1f - warm)).toInt().coerceIn(0, 255)
            g.color = Color(r, gr, b, (cloud.alpha * 210).toInt().coerceIn(0, 255))
            val side = if (cloud.wide) block * 5 else block * 3
            g.fillRect((cloud.x * width).toInt(), (cloud.y * height).toInt(), side, side)
        }

        for (dust in Sky.dust()) {
            g.color = Color(0x07, 0x08, 0x16, (dust.alpha * 200).toInt().coerceIn(0, 255))
            g.fillRect((dust.x * width).toInt(), (dust.y * height).toInt(), block * 3, block * 3)
        }

        for (star in Sky.stars()) {
            val bright = when (star.layer) {
                SkyLayer.FERN -> 0.45f
                SkyLayer.MITTE -> 0.7f
                SkyLayer.NAH -> 1f
            }
            g.color = if (star.warm) {
                Color(255, (200 * bright).toInt(), (140 * bright).toInt())
            } else {
                Color((220 * bright).toInt(), (230 * bright).toInt(), 255)
            }
            val side = if (star.layer == SkyLayer.NAH) block * 2 else block
            g.fillRect((star.x * width).toInt(), (star.y * height).toInt(), side, side)
        }
    }

    // ------------------------------------------------------------------ the bodies

    /**
     * The climb, as four small bodies stepping up from the lower left.
     *
     * Four and not twenty-five: the cover has to say "this ladder goes somewhere" at thumbnail
     * size, and a row of two dozen dots says nothing at all. They rise as they grow, because that
     * is the one thing about this game a picture can carry without a word on it.
     */
    private fun paintLadder(g: java.awt.Graphics2D) {
        val steps = listOf(
            Triple(Tiers.all[0], 46 to 404, 22),    // Meteorit
            Triple(Tiers.all[3], 118 to 372, 34),   // Mond
            Triple(Tiers.all[8], 202 to 330, 50),   // Erde
            Triple(Tiers.all[12], 300 to 276, 72),  // Saturn
        )
        for ((tier, at, side) in steps) {
            drawSprite(g, PixelPlanet.frame(tier, index = 6, size = side), at.first, at.second, side)
        }
    }

    /**
     * The black hole, large, on the right — the thing the game is named after.
     *
     * Off centre and cropped by the edge on purpose: a body that fits inside the frame is a
     * picture of a ball, and one that runs off it is a picture of something too big for the frame,
     * which is what the last rung of this ladder is.
     */
    private fun paintHero(g: java.awt.Graphics2D) {
        val side = 210
        val hole = PixelPlanet.frame(Tiers.last, index = 12, size = side)
        drawSprite(g, hole, 402, 150, side)
    }

    /** One sprite, blown up to whole blocks so nothing is ever interpolated. */
    private fun drawSprite(g: java.awt.Graphics2D, pixels: IntArray, left: Int, top: Int, side: Int) {
        for (y in 0 until side) {
            for (x in 0 until side) {
                val argb = pixels[y * side + x]
                val alpha = argb ushr 24 and 0xFF
                if (alpha == 0) continue
                g.color = Color(argb, true)
                g.fillRect(left + x, top + y, 1, 1)
            }
        }
    }

    // ------------------------------------------------------------------ the title

    /**
     * The name, in the game's own face, snapped to the block grid.
     *
     * Silkscreen is what the interface is set in, so the cover is set in it too. It is drawn with
     * anti-aliasing off and then quantised to whole blocks, because a smooth letter next to a
     * pixel planet is the one thing that would give the whole picture away as made elsewhere.
     */
    private fun paintTitle(g: java.awt.Graphics2D) {
        val file = File("../app/src/main/res/font/silkscreen_bold.ttf")
        assertTrue(file.isFile, "Die Schrift des Spiels fehlt: ${file.absolutePath}")
        val face = Font.createFont(Font.TRUETYPE_FONT, file)

        // English, because this is the store page the cover is for. The game itself says it in
        // whichever language the player is in.
        drawBlocky(g, "KOLLAPS", face.deriveFont(62f), 40, 98, Color(0xE9, 0xEC, 0xFF))
        drawBlocky(
            g,
            "from meteoroid to black hole",
            face.deriveFont(16f),
            44,
            130,
            Color(0xFF, 0xB7, 0x4D),
        )
    }

    /**
     * Text rendered once, then re-drawn as blocks.
     *
     * The two-pass shape is what keeps it in the same world as the sprites: pass one puts the
     * glyphs on a scratch image, pass two reads that image at every [block]th pixel and paints a
     * square wherever it found ink. Anything finer than a block simply does not survive, which is
     * exactly what a pixel font does anyway.
     */
    private fun drawBlocky(
        g: java.awt.Graphics2D,
        text: String,
        font: Font,
        left: Int,
        baseline: Int,
        colour: Color,
    ) {
        val scratch = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val sg = scratch.createGraphics()
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        sg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        sg.font = font
        sg.color = Color.WHITE
        sg.drawString(text, left, baseline)
        sg.dispose()

        // A shadow one block down and right, so the letters hold against the nebula behind them.
        for (pass in 0..1) {
            g.color = if (pass == 0) Color(0, 0, 0, 190) else colour
            val shift = if (pass == 0) block else 0
            for (y in 0 until height step block) {
                for (x in 0 until width step block) {
                    if (scratch.getRGB(x, y) ushr 24 == 0) continue
                    g.fillRect(x + shift, y + shift, block, block)
                }
            }
        }
    }

    // ------------------------------------------------------------------ the check

    /**
     * That the cover is a picture and not an accident.
     *
     * The first version of this asserted that no single colour covered more than three quarters
     * of the image, which is the check the screenshot harness uses to catch a screen that never
     * rendered. On a picture of space it is simply wrong: eighty-two per cent of this one is deep
     * black, and that is the subject rather than a fault.
     *
     * So it checks the two things that would actually be broken. That there is *variety* — a
     * renderer returning nothing leaves a starfield on black and barely a hundred colours. And
     * that the bodies are where they were put, which is the failure a flat colour count would
     * never see: every sprite could come back empty and the sky alone would still pass.
     */
    private fun assertPainted(image: BufferedImage) {
        val seen = HashSet<Int>()
        val random = Random(1)
        repeat(20_000) {
            seen += image.getRGB(random.nextInt(width), random.nextInt(height))
        }
        // Measured rather than guessed: the cover as it stands samples 309 distinct colours, and
        // the same picture without its nebula sampled 158. The floor sits between the two.
        assertTrue(seen.size > 250, "Nur ${seen.size} Farben — da wurde kaum etwas gerendert")

        // The middle of the black hole's ring, and the middle of the Earth on the ladder. Both are
        // sprite, not sky, and both are far from any edge the layout might shift by a pixel.
        for ((name, at) in listOf("Schwarzes Loch" to (500 to 210), "Erde" to (227 to 355))) {
            val (x, y) = at
            val patch = (0 until 12).flatMap { dy -> (0 until 12).map { dx -> image.getRGB(x + dx, y + dy) } }
            assertTrue(
                patch.toSet().size > 3,
                "Bei $name steht nur ${patch.toSet().size} Farbe — der Sprite fehlt",
            )
        }
    }
}
