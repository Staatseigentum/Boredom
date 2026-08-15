package com.staatseigentum.kollaps.core.pixel

/**
 * The fleet, as things rather than as dots.
 *
 * Every collector in the shop used to appear in orbit as a single three-pixel square in one of six
 * colours. That was enough to say "you own some of these" and nothing else: at a glance a hundred
 * machines were a hundred identical specks, and the only thing separating a dust sifter from a
 * quantum collector was a hue nobody could name.
 *
 * These are seven silhouettes with a light on them. Each is a seven by seven grid — small enough
 * that a ring of them is still a ring and not a crowd, large enough that a dish reads as a dish.
 * The shading is the part that makes them objects: the light comes from the upper left on every
 * one of them, the same direction the bodies are lit from, so a satellite in front of a planet
 * belongs to the same picture rather than sitting on top of it.
 *
 * The accent is handed in rather than baked, because that is where a collector's identity lives:
 * the hull is the same steel on all of them and the panels, dishes and cores take the colour of
 * the ring the machine flies on.
 */
object PixelCraft {

    /** Edge length of every craft, in sprite pixels. */
    const val SIDE = 7

    /** How many different silhouettes there are. Collectors past this many share one. */
    val COUNT: Int get() = SHAPES.size

    /**
     * One craft's pixels, ready to become a bitmap: [SIDE] × [SIDE] in ARGB, row major.
     *
     * Transparent where the silhouette has nothing, which is most of the corners — that is what
     * keeps a round hull round instead of drawing a square with a circle painted on it.
     */
    fun pixels(shape: Int, accent: Int): IntArray {
        val rows = SHAPES[((shape % SHAPES.size) + SHAPES.size) % SHAPES.size]
        val out = IntArray(SIDE * SIDE)
        for (y in 0 until SIDE) {
            val row = rows[y]
            for (x in 0 until SIDE) {
                out[y * SIDE + x] = when (row[x]) {
                    'L' -> HULL_LIT
                    'H' -> HULL
                    'D' -> HULL_DARK
                    'A' -> accent
                    'G' -> lighten(accent)
                    else -> 0
                }
            }
        }
        return out
    }

    /**
     * A brighter version of the accent, for the one or two pixels that are meant to be a light.
     *
     * Halfway to white on each channel rather than a fixed colour: a lamp on a green machine has
     * to still read as green, and a single white pixel on every craft would make all seven of them
     * look like the same craft with a different paint job.
     */
    private fun lighten(argb: Int): Int {
        val r = (argb shr 16 and 0xFF)
        val g = (argb shr 8 and 0xFF)
        val b = (argb and 0xFF)
        return (0xFF shl 24) or
            (((r + 0xFF) / 2) shl 16) or
            (((g + 0xFF) / 2) shl 8) or
            ((b + 0xFF) / 2)
    }

    /** Steel, lit from the upper left. Three tones is all a seven pixel hull can carry. */
    private const val HULL_LIT = 0xFFD8DDF2.toInt()
    private const val HULL = 0xFF98A0C8.toInt()
    private const val HULL_DARK = 0xFF4C5480.toInt()

    /*
     * The silhouettes.
     *
     * Written out as pictures rather than as coordinates, because that is what they are, and a
     * table of offsets would be unreviewable — nobody can see a dish in a list of numbers. The
     * test checks that every row is [SIDE] long and every shape [SIDE] rows tall, so a typo in
     * here fails a build rather than drawing a hole in a hull.
     */
    private val SHAPES: List<List<String>> = listOf(
        // A probe: round hull, a dish either side, a lamp underneath.
        listOf(
            "..LHD..",
            ".LHHHD.",
            "AAHHHAA",
            "AAHHHAA",
            ".DHHHD.",
            "..DDD..",
            "...G...",
        ),
        // A panelled satellite: a narrow body between two long wings.
        listOf(
            "...L...",
            "..LHD..",
            "AALHDAA",
            "AALHDAA",
            "AALHDAA",
            "..DDD..",
            "...G...",
        ),
        // A drum: a cylinder seen from the side, with a mast.
        listOf(
            "...G...",
            "..LLD..",
            ".LHHHD.",
            ".LAAAD.",
            ".LHHHD.",
            ".LHHHD.",
            "..DDD..",
        ),
        // A ring station: a hoop with something bright held in the middle.
        listOf(
            ".LHHHD.",
            "L.....D",
            "H.AGA.D",
            "H.GAG.D",
            "H.AGA.D",
            "L.....D",
            ".LHHHD.",
        ),
        // A lander: a wide hull on legs.
        listOf(
            "..LHD..",
            ".LHHHD.",
            "LHHHHHD",
            ".DHHHD.",
            "..D.D..",
            ".A...A.",
            ".G...G.",
        ),
        // An antenna cluster: a small hull carrying more mast than machine.
        listOf(
            "...A...",
            "...A...",
            ".AAGAA.",
            "...A...",
            "..LHD..",
            ".LHHHD.",
            "..DDD..",
        ),
        // A collector frame: an open square of hull with a lit intake.
        listOf(
            "LHHHHHD",
            "H.....D",
            "H.AAA.D",
            "H.AGA.D",
            "H.AAA.D",
            "H.....D",
            "LDDDDDD",
        ),
    )
}
