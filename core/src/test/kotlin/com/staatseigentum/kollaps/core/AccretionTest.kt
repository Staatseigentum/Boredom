package com.staatseigentum.kollaps.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.random.Random

/**
 * The two halves of the accretion update: what falls in, and what it is built into.
 *
 * Live in the shipped game, so most of these simply call the rules. The two at the top are the
 * exception: they hold [Rollout.accretion] to the claim it makes — that switching the update off
 * gives back exactly the game that came before it, in one line and with nobody's save harmed.
 * If that ever stopped being true, taking the update back out would stop being a line.
 */
class AccretionTest {

    /** Nothing here may leave the switch where the next test does not expect it. */
    @AfterTest
    fun restore() = assertTrue(Rollout.accretion, "Der Schalter wurde nicht zurückgestellt")

    // ------------------------------------------------------------------ the switch

    @Test
    fun `switching it off gives back the game that came before`() = Rollout.accretion(live = false) {
        val stocked = GameState(
            materials = mapOf(Material.METALL.id to 999.0, Material.SILIKAT.id to 999.0),
            shells = mapOf(Shell.KERN.id to 10),
        )

        assertFalse(Accretion.isActive(stocked), "Es fällt etwas ein, obwohl der Schalter aus ist")
        assertFalse(Accretion.isUnlocked(stocked), "Die Anzeige wäre sichtbar")
        assertFalse(Shells.canBuild(stocked, Shell.KERN), "Man könnte bauen")
        assertEquals(null, Worlds.current(stocked), "Der Körper bekäme einen Typ")
        assertEquals(1.0, Worlds.multiplier(stocked), "Der Typenbonus zählte mit")
        assertEquals(stocked, GameEngine.buildShell(stocked, Shell.KERN.id), "Es wurde gebaut")
        assertEquals(
            stocked,
            GameEngine.absorbImpact(stocked, Accretion.all.first()),
            "Ein Einschlag wurde eingesammelt",
        )
    }

    /**
     * And a save written while it was live still plays, it simply stops being paid for the body.
     *
     * The stronger half of the claim: it is not enough that nothing new *happens*. A player who
     * had built fifteen layers before the update was withdrawn must land in the ordinary game
     * rather than in one that is still quietly multiplying their production by something the
     * interface no longer shows — and their save has to decode either way round.
     */
    @Test
    fun `a save built while it was live still plays with it off`() {
        val bare = GameState(collectors = mapOf(Collectors.all.first().id to 20))
        val built = bare.copy(
            shells = mapOf(Shell.KERN.id to 15, Shell.MANTEL.id to 15),
            worldTypes = Worlds.all.map { it.id }.toSet(),
        )

        Rollout.accretion(live = false) {
            assertEquals(
                GameEngine.stats(bare).massPerSecond,
                GameEngine.stats(built).massPerSecond,
                "Ein Kern im Spielstand hat die Produktion verändert",
            )
            // Still readable and still writable — the fields are simply not read any more.
            assertEquals(built, SaveCodec.decode(SaveCodec.encode(built)))
        }

        // And live, the same save is worth visibly more — otherwise the check above would pass
        // just as well for a system that does nothing at all.
        assertTrue(
            GameEngine.stats(built).massPerSecond > GameEngine.stats(bare).massPerSecond,
            "Der Kern bringt gar nichts",
        )
    }

    // ------------------------------------------------------------------ impacts

    @Test
    fun `absorbing one pays mass and leaves material`() {
        val impact = Accretion.byId("im_nugget")!!
        val before = GameState()
        val after = GameEngine.absorbImpact(before, impact)

        assertTrue(after.mass > before.mass, "Der Einschlag hat keine Masse gebracht")
        assertEquals(
            impact.yield_.toDouble(),
            Shells.amountOf(after, impact.material),
            "Das Material ist nicht angekommen",
        )
        assertEquals(1L, after.impactsAbsorbed)
    }

    /**
     * The floor, which is the whole reason the early game notices this at all.
     *
     * The payout is a share of production, and at the meteorite production is nought — so without
     * a floor every impact in the first two minutes would be worth exactly nothing, which is the
     * two minutes the system exists for.
     */
    @Test
    fun `the very first one is still worth taking`() {
        val fresh = GameState()
        assertEquals(0.0, GameEngine.stats(fresh).massPerSecond, "Der Test misst nicht, was er meint")
        for (impact in Accretion.all) {
            assertTrue(
                Accretion.massOf(fresh, impact) > 0.0,
                "${impact.label} bringt einem frischen Körper nichts",
            )
        }
    }

    /** And it grows with the body rather than staying a rounding error. */
    @Test
    fun `it is worth more once there is production`() {
        val impact = Accretion.all.first()
        val busy = GameState(collectors = mapOf(Collectors.all.first().id to 500))
        assertTrue(
            Accretion.massOf(busy, impact) > Accretion.massOf(GameState(), impact),
            "Ein Einschlag wird mit der Produktion nicht größer",
        )
    }

    @Test
    fun `only the heavy ones cost anything to ignore`() {
        val busy = GameState(
            mass = 1_000_000.0,
            collectors = mapOf(Collectors.all.first().id to 200),
        )
        for (impact in Accretion.all) {
            val after = GameEngine.missImpact(busy, impact)
            if (impact.heavy) {
                assertTrue(after.mass < busy.mass, "${impact.label} zu verpassen kostet nichts")
                assertTrue(
                    busy.mass - after.mass < Accretion.massOf(busy, impact),
                    "Verpassen kostet mehr, als Fangen einbringt",
                )
            } else {
                assertEquals(busy.mass, after.mass, "${impact.label} zu verpassen hat Masse gekostet")
            }
        }
    }

    /** Missing something while broke must not put the player into debt. */
    @Test
    fun `a miss cannot push the mass below nothing`() {
        val heavy = Accretion.all.first { it.heavy }
        val broke = GameState(mass = 0.0)
        assertEquals(0.0, GameEngine.missImpact(broke, heavy).mass)
    }

    /**
     * They thin out where the comets take over, and they never stop.
     *
     * This used to assert the opposite, and the opposite was the bug: `isActive` went false past
     * Mars, the overlay stopped scheduling, and the comets that were supposed to take over carried
     * nothing. Since a collapse takes the shells and the loose material with it, that left a run
     * past Mars with no way to gain a single unit for the rest of its life.
     */
    @Test
    fun `they thin out where the comets take over, and never stop`() {
        val early = GameState(runMass = 0.0)
        assertTrue(Accretion.isActive(early), "Am Anfang fällt nichts ein")
        assertFalse(Accretion.isThinned(early), "Am Anfang regnet es schon ausgedünnt")

        val late = GameState(runMass = Tiers.all[Accretion.DENSE_TIER + 1].threshold)
        assertTrue(Accretion.isActive(late), "Über Mars fällt gar nichts mehr — das war der Fehler")
        assertTrue(Accretion.isThinned(late), "Über Mars regnet es unvermindert weiter")
        assertTrue(
            Accretion.interval(late) > Accretion.interval(early),
            "Über Mars kommt nicht seltener etwas als am Anfang",
        )

        // The panel stays — including for a save that was already far past Mars when the update
        // arrived, which is every save that was being played when it did.
        assertTrue(Accretion.isUnlocked(late), "Ein alter Spielstand sieht vom Update nichts")
        assertTrue(Worlds.isUnlocked(late), "Und von den Weltentypen auch nichts")
    }

    /**
     * The other source, and the promise the panel has been making since 5.0.0.
     *
     * Only the comets with a core carry anything, and the three that cross whole must not — the
     * common ones arrive every few minutes for a single tap, and material that cheap would make
     * the world grid a formality rather than a collection.
     */
    @Test
    fun `only the comets with a core leave material behind`() {
        for (comet in Comet.entries) {
            val before = GameState(runMass = Tiers.all[Accretion.DENSE_TIER + 1].threshold)
            val after = GameEngine.catchComet(before, comet)
            val gained = Material.entries.sumOf { Shells.amountOf(after, it) }

            if (comet.hits > 1 || comet == Comet.FRENZY) {
                assertTrue(gained > 0.0, "${comet.id} bringt kein Material")
            } else {
                assertEquals(0.0, gained, "${comet.id} bringt Material, obwohl ein Tipp reicht")
            }
        }
    }

    /** And the crust raises that load exactly as it raises an impact's. */
    @Test
    fun `the crust raises what a comet spills`() {
        val bare = GameState()
        val crusted = GameState(shells = mapOf(Shell.KRUSTE.id to 5))
        assertTrue(
            Shells.yieldFactor(crusted) > Shells.yieldFactor(bare),
            "Der Aufbau taugt nicht für den Test",
        )

        val plain = GameEngine.catchComet(bare, Comet.ICE_CORE)
        val better = GameEngine.catchComet(crusted, Comet.ICE_CORE)
        assertTrue(
            Shells.amountOf(better, Material.EIS) > Shells.amountOf(plain, Material.EIS),
            "Die Kruste erhöht die Ladung eines Kometen nicht",
        )
    }

    @Test
    fun `arrivals come sooner as the body grows, down to a floor`() {
        val early = Accretion.interval(GameState())
        val later = Accretion.interval(GameState(runMass = Tiers.all[Accretion.DENSE_TIER].threshold))
        assertTrue(later < early, "Die Schwerkraft holt nichts schneller herein")
        assertTrue(later >= Accretion.MIN_INTERVAL, "Der Abstand ist unter das Minimum gefallen")

        // And the mantle pulls harder still, without ever going below the floor.
        val heavy = GameState(shells = mapOf(Shell.MANTEL.id to Shells.MAX_LEVEL))
        assertTrue(Accretion.interval(heavy) < early, "Der Mantel zieht nichts an")
        assertTrue(Accretion.interval(heavy) >= Accretion.MIN_INTERVAL)
    }

    /** Every weight has to be reachable, or a rock is in the table and never in the sky. */
    @Test
    fun `every impact can actually turn up`() {
        val random = Random(7)
        val seen = mutableSetOf<String>()
        repeat(4_000) { seen += Accretion.pick(random).id }
        assertEquals(Accretion.all.map { it.id }.toSet(), seen, "Nicht jeder Brocken kommt vor")
    }

    // ------------------------------------------------------------------ shells

    @Test
    fun `building spends both materials and raises the level`() {
        val shell = Shell.KERN
        val stocked = GameState(
            materials = mapOf(shell.wants.id to 50.0, shell.second.id to 50.0),
        )
        val cost = Shells.costOf(stocked, shell)
        assertTrue(Shells.canBuild(stocked, shell), "Mit vollem Lager geht es nicht")

        val built = GameEngine.buildShell(stocked, shell.id)
        assertEquals(1, Shells.levelOf(built, shell))
        for ((material, amount) in cost) {
            assertEquals(
                50.0 - amount,
                Shells.amountOf(built, material),
                "${material.label} wurde falsch abgezogen",
            )
        }
    }

    @Test
    fun `a shell without material is refused`() {
        val shell = Shell.KRUSTE
        // Enough of the main material and none of the binder: the case the two-material cost
        // exists for, and the one a single-cost check would have let through.
        val lopsided = GameState(materials = mapOf(shell.wants.id to 500.0))
        assertFalse(Shells.canBuild(lopsided, shell))
        assertEquals(lopsided, GameEngine.buildShell(lopsided, shell.id))
    }

    @Test
    fun `costs grow and the top is reachable`() {
        var state = GameState(
            materials = Material.entries.associate { it.id to 1e9 },
        )
        var previous = 0.0
        repeat(Shells.MAX_LEVEL) {
            val cost = Shells.costOf(state, Shell.KERN).getValue(Shell.KERN.wants)
            assertTrue(cost >= previous, "Eine Stufe war billiger als die davor")
            previous = cost
            state = GameEngine.buildShell(state, Shell.KERN.id)
        }
        assertEquals(Shells.MAX_LEVEL, Shells.levelOf(state, Shell.KERN))
        assertFalse(Shells.canBuild(state, Shell.KERN), "Es geht über das Maximum hinaus")
        assertEquals(state.shells, GameEngine.buildShell(state, Shell.KERN.id).shells)
    }

    @Test
    fun `an empty body is an even third of each`() {
        val fresh = GameState()
        for (shell in Shell.entries) {
            assertEquals(
                1.0 / Shell.entries.size,
                Shells.shareOf(fresh, shell),
                "Ein leerer Körper hat keine Zusammensetzung",
            )
        }
    }

    @Test
    fun `each shell moves the thing it says it moves`() {
        val fleet = mapOf(Collectors.all.first().id to 30)
        val bare = GameState(collectors = fleet)

        val core = GameState(collectors = fleet, shells = mapOf(Shell.KERN.id to 10))
        assertTrue(
            GameEngine.stats(core).massPerSecond > GameEngine.stats(bare).massPerSecond,
            "Der Kern erhöht die Produktion nicht",
        )

        val mantle = GameState(collectors = fleet, shells = mapOf(Shell.MANTEL.id to 10))
        assertTrue(
            GameEngine.stats(mantle).massPerTap > GameEngine.stats(bare).massPerTap,
            "Der Mantel erhöht den Tippwert nicht",
        )

        val crust = GameState(collectors = fleet, shells = mapOf(Shell.KRUSTE.id to 10))
        assertTrue(
            GameEngine.stats(crust).offlineEfficiency > GameEngine.stats(bare).offlineEfficiency,
            "Die Kruste bringt offline nichts",
        )
        assertTrue(
            GameEngine.stats(crust).offlineEfficiency <= 1.0,
            "Die Offline-Ausbeute ist über hundert Prozent gestiegen",
        )
        assertTrue(Shells.yieldFactor(crust) > 1.0, "Die Kruste bringt kein Material mehr ein")
    }

    // ------------------------------------------------------------------ the loop

    /**
     * The whole thing, played rather than asserted at.
     *
     * Everything above checks one rule. This walks the loop the way the first ten minutes of a dev
     * game walk it — something falls in, it is taken, the material becomes a layer, the layers
     * become a world — because a system whose every rule is right and whose loop does not close is
     * a system that passes its tests and is not a game.
     *
     * Deterministic: the impacts are picked from a seeded stream and the clock is a number.
     */
    @Test
    fun `ten minutes of a new game closes the loop`() {
        val random = Random(11)
        var state = GameState(collectors = mapOf(Collectors.all.first().id to 5))
        var caught = 0

        // Ten minutes at the opening interval, taking everything that arrives — which is what a
        // player watching the screen does, and the fastest the loop can possibly run.
        var elapsed = 0.0
        while (elapsed < 600.0) {
            val step = Accretion.interval(state)
            state = GameEngine.tick(state, step)
            state = GameEngine.absorbImpact(state, Accretion.pick(random))
            caught++
            // Spend on whatever is affordable, in the order the panel lists them.
            for (shell in Shell.entries) {
                while (Shells.canBuild(state, shell)) state = GameEngine.buildShell(state, shell.id)
            }
            elapsed += step
        }

        assertTrue(caught > 50, "In zehn Minuten kamen nur $caught Brocken")
        assertEquals(caught.toLong(), state.impactsAbsorbed)
        assertTrue(Shells.total(state) > 0, "Nichts davon ließ sich verbauen")
        assertTrue(
            state.worldTypes.isNotEmpty(),
            "Nach zehn Minuten ist der Körper noch nichts geworden: ${Shells.total(state)} Schichten",
        )
        assertTrue(
            Worlds.multiplier(state) > 1.0,
            "Der Weltentyp steht im Spielstand und zahlt nichts",
        )
    }

    /**
     * And the two cards that explain it are not swallowed on the first tick.
     *
     * [GameEngine.seedIntros] marks everything a freshly loaded save has already reached as read,
     * which is right for eleven systems that unlock hours in and would have been fatal for these
     * two: accretion is live from the first second, so a card gated on "the system exists" would be
     * marked read before the window finished opening — on a new game, for ever.
     */
    @Test
    fun `the accretion cards survive a fresh save`() {
        val fresh = GameEngine.tick(GameState.new(0L), 0.1)
        assertTrue(fresh.introsSeeded, "Der Spielstand wurde gar nicht abgeglichen")
        assertFalse("un_impacts" in fresh.seenIntros, "Die Einschlag-Karte wurde weggeräumt")
        assertFalse("un_shells" in fresh.seenIntros, "Die Schichten-Karte wurde weggeräumt")

        val caught = GameEngine.absorbImpact(fresh, Accretion.all.first())
        assertEquals("un_impacts", Unlocks.pending(caught)?.id, "Die Karte kommt nie")
    }

    /** The crust's yield has to actually reach the material that is handed over. */
    @Test
    fun `the crust brings more material in`() {
        val impact = Accretion.all.first()
        val plain = GameEngine.absorbImpact(GameState(), impact)
        val crusted = GameEngine.absorbImpact(
            GameState(shells = mapOf(Shell.KRUSTE.id to 10)),
            impact,
        )
        assertTrue(
            Shells.amountOf(crusted, impact.material) > Shells.amountOf(plain, impact.material),
            "Die Kruste ändert nichts an der Ausbeute",
        )
    }
}
