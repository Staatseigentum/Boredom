package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val HOUR = 3_600.0

/** Some fixed moment. Nothing here reads the clock for anything but a timestamp. */
private const val AT = 1_700_000_000_000L

/** The sky: universes that have been through their big bang and did not stop existing. */
class MultiverseTest {

    /** Somebody who has earned the button: ten collapses, deep, aligned to a path. */
    private fun ready(path: String? = Path.MASCHINE.id): GameState = GameState.new(AT).copy(
        collapses = 12,
        singularities = 90.0,
        bestTier = Tiers.indexOf("Schwarzes Loch"),
        runMass = 1e12,
        mass = 1e12,
        collectors = mapOf("dust" to 200, "net" to 150),
        path = path,
    )

    @Test
    fun `a big bang parks the universe instead of deleting it`() {
        val before = ready()
        val after = GameEngine.bigBang(before, AT, Path.HAND.id)

        assertEquals(1, after.universes.size, "Der Urknall hat das Universum weggeworfen")
        val parked = after.universes.single()
        assertEquals(0, parked.slot)
        assertEquals(Path.MASCHINE.id, parked.pathId, "Die geparkte Galaxie hat den falschen Pfad")
        assertEquals(before.collapses, parked.collapses)
        assertEquals(before.bestTier, parked.bestTier)

        // And the new universe is the one that was chosen, not the one that was parked.
        assertEquals(Path.HAND.id, after.path)
        assertEquals(0.0, after.runMass, "Der neue Lauf fängt trotzdem bei null an")
    }

    @Test
    fun `the sky survives both resets`() {
        val banged = GameEngine.bigBang(ready(), AT, Path.HAND.id)
        val collapsed = GameEngine.collapse(banged.copy(runMass = 1e30, mass = 1e30), AT)
        assertEquals(banged.universes, collapsed.universes, "Ein Kollaps hat die Galaxien geleert")

        val challenged = GameEngine.startChallenge(collapsed.copy(collapses = 4), "c_hand", AT)
        assertEquals(banged.universes, challenged.universes, "Eine Herausforderung hat sie geleert")
    }

    @Test
    fun `eight galaxies fill up and the ninth displaces the weakest`() {
        var state = ready()
        repeat(Multiverse.SLOTS) {
            state = GameEngine.bigBang(state, AT, Path.MASCHINE.id).copy(
                // Earn the next button press, which costs more collapses every time round, and be
                // a little deeper for it.
                collapses = BigBang.requiredFor(it + 1) + it,
                singularities = 90.0,
                bestTier = Tiers.indexOf("Schwarzes Loch"),
            )
        }
        assertEquals(Multiverse.SLOTS, state.universes.size)
        assertNull(Multiverse.nextSlot(state), "Es ist noch ein Platz frei, obwohl acht belegt sind")

        val weakestBefore = state.universes.minByOrNull { Multiverse.yieldOf(it) }!!
        val ninth = GameEngine.bigBang(state.copy(collapses = 60), AT, Path.KERN.id)

        assertEquals(Multiverse.SLOTS, ninth.universes.size, "Der neunte Urknall hat einen Platz erfunden")
        assertTrue(
            ninth.universes.none { it.collapses == weakestBefore.collapses && it.slot == weakestBefore.slot },
            "Die schwächste Galaxie steht noch da",
        )
        assertNotNull(ninth.universes.firstOrNull { it.collapses == 60 }, "Die neue ist nicht eingezogen")
    }

    @Test
    fun `a worse universe does not push a better one out`() {
        var state = ready()
        repeat(Multiverse.SLOTS) {
            state = GameEngine.bigBang(state, AT, Path.MASCHINE.id).copy(
                collapses = BigBang.requiredFor(it + 1) + 40,
                singularities = 400.0,
                bestTier = Tiers.indexOf("Schwarzes Loch"),
            )
        }
        val full = state.universes
        // Just barely enough to press the button, and nothing behind it.
        val shallow = state.copy(collapses = BigBang.REQUIRED_COLLAPSES, singularities = 1.0, bestTier = 0)
        assertEquals(full, GameEngine.bigBang(shallow, AT, Path.HAND.id).universes)
    }

    @Test
    fun `the sky is worth something and stays bounded`() {
        val empty = GameState.new(AT)
        assertEquals(1.0, Multiverse.multiplier(empty), "Ein leerer Himmel darf nichts bringen")

        var state = ready()
        val steps = mutableListOf<Double>()
        repeat(Multiverse.SLOTS) {
            // Eight *identical* galaxies, which is what makes the bound below meaningful — so
            // the collapse count is held flat and simply set high enough to afford all eight
            // presses rather than rising with the requirement.
            state = GameEngine.bigBang(state, AT, Path.MASCHINE.id).copy(
                collapses = BigBang.requiredFor(Multiverse.SLOTS) + 15,
                singularities = 200.0,
                bestTier = Tiers.indexOf("Schwarzes Loch"),
            )
            steps += Multiverse.multiplier(state)
        }

        assertTrue(steps.first() > 1.0, "Die erste Galaxie bringt nichts")
        assertTrue(steps.zipWithNext().all { (a, b) -> b > a }, "Eine weitere Galaxie bringt nichts")

        // The falloff is the whole reason this is safe to stack, and it is a geometric series, so
        // the bound can be stated exactly rather than guessed at: however many galaxies stand
        // there and however good they are, the total can never pass the best one divided by what
        // is left of the falloff. Measured against the *best* galaxy, not the first — the first
        // one parked is the shallowest by construction, and holding the sum of eight against it
        // was measuring the wrong thing.
        val best = state.universes.maxOf { Multiverse.yieldOf(it) }
        val total = Multiverse.multiplier(state) - 1.0
        // Both of Fördern's factors belong in the bound rather than in a looser number. They scale
        // the series; they do not change that it is one, which is the property under test.
        val each = best * Multiverse.FOERDERN_LEVERAGE * Multiverse.completion(state)
        assertTrue(
            total < each / (1.0 - Multiverse.SLOT_FALLOFF),
            "Acht Galaxien sind $total, die Schranke ist ${each / (1.0 - Multiverse.SLOT_FALLOFF)}",
        )
        // And it has to actually bite: undamped this would be eight times the best one.
        assertTrue(
            total < each * Multiverse.SLOTS * 0.75,
            "Acht Galaxien sind $total — die Dämpfung greift kaum",
        )
    }

    @Test
    fun `galaxies earn aeons on their own clock`() {
        val banged = GameEngine.bigBang(ready(), AT, Path.HAND.id)
        assertTrue(Multiverse.aeonsPerSecond(banged) > 0.0, "Eine Galaxie verdient nichts")

        // A single hour is far below one whole Äon, and it must not be rounded away — that was
        // the bug this fraction exists to prevent.
        val anHour = Multiverse.advance(banged, HOUR)
        assertEquals(banged.aeons, anHour.aeons, "Ein Bruchteil wurde vorzeitig ausgezahlt")
        assertTrue(anHour.aeonFraction > 0.0, "Der Bruchteil ist verschwunden")

        var long = banged
        repeat(24 * 30) { long = Multiverse.advance(long, HOUR) }
        assertTrue(long.aeons > banged.aeons, "Nach einem Monat immer noch kein Äon")
    }

    @Test
    fun `an empty sky earns nothing and costs no work`() {
        val fresh = GameState.new(AT)
        assertEquals(0.0, Multiverse.aeonsPerSecond(fresh))
        assertEquals(fresh, Multiverse.advance(fresh, 10 * HOUR))
        assertTrue(Multiverse.effects(fresh).isEmpty())
    }

    @Test
    fun `a parked universe keeps leaning the way it leaned`() {
        val handed = GameEngine.bigBang(ready(Path.HAND.id), AT, Path.LABOR.id)
        val effects = Multiverse.effects(handed)

        assertTrue(effects.isNotEmpty(), "Die geparkte Galaxie trägt nichts bei")
        val tap = effects.filterIsInstance<PrestigeEffect.TapMultiplier>().single()
        val full = Path.HAND.effects.filterIsInstance<PrestigeEffect.TapMultiplier>().single()
        assertTrue(tap.factor > 1.0, "Der Beitrag ist auf nichts geschrumpft")
        assertTrue(tap.factor < full.factor, "Die Galaxie gibt den Pfad in voller Stärke weiter")
    }

    @Test
    fun `the switches and head starts are never handed on`() {
        val machine = GameEngine.bigBang(ready(Path.MASCHINE.id), AT, Path.KERN.id)
        val effects = Multiverse.effects(machine)

        // Auto-buy is a prestige upgrade somebody pays for. A galaxy must not give it away.
        assertTrue(
            effects.none { it is PrestigeEffect.AutoBuy },
            "Eine geparkte Maschine verschenkt den Auto-Kauf",
        )
        assertTrue(
            effects.none {
                it is PrestigeEffect.StartingMass || it is PrestigeEffect.StartingCollectors
            },
            "Der Himmel gibt eine Starthilfe statt einer Rate",
        )
    }

    @Test
    fun `every galaxy has a readable name and stays inside the sky`() {
        var state = ready()
        repeat(Multiverse.SLOTS) {
            state = GameEngine.bigBang(state, AT, Path.MASCHINE.id)
                .copy(collapses = BigBang.requiredFor(it + 1) + 20, bestTier = 10)
        }
        val names = state.universes.map { it.name }
        assertEquals(names.size, names.distinct().size, "Zwei Galaxien heißen gleich: $names")
        assertTrue(state.universes.all { it.slot in 0 until Multiverse.SLOTS })
        assertTrue(names.none { it.isBlank() })
    }
}

/** What happens to a save written before the sky existed. */
class MultiverseMigrationTest {

    @Test
    fun `an old save gets the galaxies it already earned`() {
        val old = GameState.new(AT).copy(
            bigBangs = 4,
            bestTier = Tiers.indexOf("Schwarzes Loch"),
            collapses = 6,
            aeons = 12.0,
        )
        val loaded = SaveCodec.decode(SaveCodec.encode(old))
        assertNotNull(loaded)
        assertEquals(4, loaded.universes.size, "Vier Urknalle, aber kein Himmel")
        assertEquals(listOf(0, 1, 2, 3), loaded.universes.map { it.slot }.sorted())
        // Reconstructed conservatively, and rising: the oldest universe was the shallowest.
        val depths = loaded.universes.sortedBy { it.slot }.map { it.bestTier }
        assertTrue(depths.zipWithNext().all { (a, b) -> b >= a }, "Die Tiefen laufen rückwärts: $depths")
        assertTrue(depths.last() <= old.bestTier, "Eine Galaxie ist tiefer als der Spielstand je war")
        assertTrue(Multiverse.multiplier(loaded) > 1.0, "Der rückwirkende Himmel bringt nichts")
    }

    @Test
    fun `more big bangs than slots still fit`() {
        val old = GameState.new(AT).copy(bigBangs = 40, bestTier = 20)
        val loaded = SaveCodec.decode(SaveCodec.encode(old))
        assertNotNull(loaded)
        assertEquals(Multiverse.SLOTS, loaded.universes.size)
    }

    @Test
    fun `a save that already has a sky is left alone`() {
        val mine = listOf(ParkedUniverse(slot = 3, pathId = Path.KERN.id, bestTier = 9, collapses = 4))
        val state = GameState.new(AT).copy(bigBangs = 5, universes = mine)
        val loaded = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(loaded)
        assertEquals(mine, loaded.universes, "Der Nachbau hat einen echten Himmel überschrieben")
    }

    @Test
    fun `a fresh save has no sky at all`() {
        val loaded = SaveCodec.decode(SaveCodec.encode(GameState.new(AT)))
        assertNotNull(loaded)
        assertTrue(loaded.universes.isEmpty())
    }

    @Test
    fun `nonsense slots are thrown away rather than counted`() {
        val state = GameState.new(AT).copy(
            bigBangs = 2,
            universes = listOf(
                ParkedUniverse(slot = 0, bestTier = 5),
                ParkedUniverse(slot = 0, bestTier = 9),
                ParkedUniverse(slot = 99, bestTier = 9),
                ParkedUniverse(slot = -1, bestTier = 9),
            ),
        )
        val loaded = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(loaded)
        assertEquals(1, loaded.universes.size, "Doppelte oder erfundene Plätze zählen mit")
        assertEquals(0, loaded.universes.single().slot)
    }
}
