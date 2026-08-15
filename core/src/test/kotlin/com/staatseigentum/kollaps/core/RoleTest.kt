package com.staatseigentum.kollaps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoleTest {

    private val now = 1_700_000_000_000L

    private fun fleet(collapses: Int = 0): GameState = GameState(
        bestTier = Tiers.indexOf(Roles.UNLOCK_TIER),
        runMass = Tiers.byName(Roles.UNLOCK_TIER).threshold,
        mass = 1e15,
        collapses = collapses,
        collectors = mapOf("dust" to 100, "net" to 50, "drone" to 50),
    )

    // ------------------------------------------------------------------ assigning

    @Test
    fun `roles stay shut until the fleet is worth arranging`() {
        val early = GameState(bestTier = 0, collectors = mapOf("dust" to 10))
        assertFalse(Roles.isUnlocked(early))
        assertEquals(early, Roles.set(early, "dust", Role.GUETE))
    }

    @Test
    fun `a role already assigned keeps the panel open`() {
        assertTrue(Roles.isUnlocked(GameState(bestTier = 0, roles = mapOf("dust" to Role.MENGE.id))))
    }

    @Test
    fun `there are only so many slots`() {
        val state = fleet()
        assertEquals(Roles.BASE_SLOTS, Roles.slots(state))

        var filled = state
        for (collector in Collectors.all.take(Roles.BASE_SLOTS)) {
            filled = Roles.set(filled, collector.id, Role.GUETE)
        }
        assertEquals(Roles.BASE_SLOTS, Roles.assignedCount(filled))
        assertFalse(Roles.hasFreeSlot(filled))

        val extra = Collectors.all[Roles.BASE_SLOTS]
        assertEquals(filled, Roles.set(filled, extra.id, Role.GUETE), "Ein Platz zu viel")
    }

    /** Changing what a slot holds is free; only taking a new one can be refused. */
    @Test
    fun `a full board can still be rearranged`() {
        var state = fleet()
        for (collector in Collectors.all.take(Roles.BASE_SLOTS)) {
            state = Roles.set(state, collector.id, Role.GUETE)
        }
        assertFalse(Roles.hasFreeSlot(state))

        val first = Collectors.all.first().id
        val changed = Roles.set(state, first, Role.NETZ)
        assertEquals(Role.NETZ, Roles.roleOf(changed, first))

        val cleared = Roles.set(changed, first, null)
        assertNull(Roles.roleOf(cleared, first))
        assertTrue(Roles.hasFreeSlot(cleared))
    }

    @Test
    fun `slots grow with the collapses behind the player`() {
        assertTrue(Roles.slots(fleet(collapses = 3)) > Roles.slots(fleet(collapses = 0)))
        assertEquals(Roles.MAX_SLOTS, Roles.slots(fleet(collapses = 99)))
    }

    @Test
    fun `tapping walks through every role and then off`() {
        var state = fleet()
        for (role in Role.entries) {
            state = Roles.cycle(state, "dust")
            assertEquals(role, Roles.roleOf(state, "dust"))
        }
        state = Roles.cycle(state, "dust")
        assertNull(Roles.roleOf(state, "dust"), "Nach der letzten Rolle bleibt eine stehen")
    }

    // ------------------------------------------------------------------ what they do

    /**
     * The property the whole file rests on.
     *
     * A role that only gives is not a decision, it is a button nobody would leave unpressed —
     * and switching the entire fleet to it would be the answer to every question.
     */
    @Test
    fun `every role gives something up`() {
        for (role in Role.entries) {
            val gives = role.output > 1.0 || role.cost < 1.0 || role.networkPerStep > 0.0
            val takes = role.output < 1.0 || role.cost > 1.0
            assertTrue(gives, "${role.label} bringt nichts")
            assertTrue(takes, "${role.label} kostet nichts")
        }
    }

    @Test
    fun `quality trades price for output and bulk trades the other way`() {
        val plain = fleet()
        val quality = Roles.set(plain, "dust", Role.GUETE)
        val bulk = Roles.set(plain, "dust", Role.MENGE)

        val dust = Collectors.require("dust")
        assertTrue(GameEngine.collectorOutput(quality, dust) > GameEngine.collectorOutput(plain, dust))
        assertTrue(GameEngine.collectorOutput(bulk, dust) < GameEngine.collectorOutput(plain, dust))

        val price = { state: GameState -> GameEngine.collectorCost(state, dust, 100, 1) }
        assertTrue(price(quality) > price(plain))
        assertTrue(price(bulk) < price(plain))
    }

    @Test
    fun `the shop row quotes the price the purchase charges`() {
        val state = Roles.set(fleet(), "dust", Role.GUETE)
        val offer = GameEngine.collectorOffers(state, BuyAmount.TEN).first { it.collector.id == "dust" }

        val after = GameEngine.buyCollector(state, "dust", BuyAmount.TEN)
        assertEquals(state.mass - offer.cost, after.mass, state.mass * 1e-12)
        assertEquals(110, after.ownedOf("dust"))
    }

    /** A cheaper role has to make the max button reach further, not merely cost less per copy. */
    @Test
    fun `the max button spends the whole purse at the role's price`() {
        // Big enough that the fifteen percent shows up as whole copies. A smaller purse buys
        // the same rounded-down number either way, which proves nothing about the price.
        val purse = 1e9
        val plain = fleet().copy(mass = purse)
        val bulk = Roles.set(plain, "dust", Role.MENGE)

        val plainCount = GameEngine.buyCollector(plain, "dust", BuyAmount.MAX).ownedOf("dust")
        val bulkCount = GameEngine.buyCollector(bulk, "dust", BuyAmount.MAX).ownedOf("dust")

        assertTrue(bulkCount > plainCount, "Die billige Rolle kauft nicht mehr Stück")
        assertTrue(
            GameEngine.buyCollector(bulk, "dust", BuyAmount.MAX).mass >= 0.0,
            "Max hat mehr ausgegeben, als da war",
        )
    }

    @Test
    fun `a network collector lifts the others and not itself`() {
        val plain = fleet()
        val networked = Roles.set(plain, "dust", Role.NETZ)

        val dust = Collectors.require("dust")
        val other = Collectors.require("net")

        assertTrue(
            GameEngine.collectorOutput(networked, other) > GameEngine.collectorOutput(plain, other),
            "Das Netz hebt die anderen nicht",
        )
        assertTrue(
            GameEngine.collectorOutput(networked, dust) < GameEngine.collectorOutput(plain, dust),
            "Das Netz arbeitet noch genauso viel selbst",
        )
        assertEquals(
            1.0,
            Roles.networkFactor(networked, "dust"),
            1e-12,
            "Das Netz koordiniert sich selbst",
        )
    }

    @Test
    fun `a network collector with too few copies co-ordinates nothing`() {
        val sparse = fleet().copy(collectors = mapOf("dust" to Milestones.STEP - 1, "net" to 50))
        val networked = Roles.set(sparse, "dust", Role.NETZ)

        assertEquals(1.0, Roles.networkFactor(networked, "net"), 1e-12)
    }

    // ------------------------------------------------------------------ what survives

    @Test
    fun `a collapse takes the arrangement with the fleet`() {
        val ready = Roles.set(fleet(), "dust", Role.GUETE).copy(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
        )
        assertTrue(GameEngine.canCollapse(ready))
        assertTrue(GameEngine.collapse(ready, now).roles.isEmpty())
    }

    @Test
    fun `a save keeps the arrangement`() {
        val state = Roles.set(fleet(), "dust", Role.NETZ)
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertEquals(Role.NETZ, Roles.roleOf(back, "dust"))
    }

    @Test
    fun `a save drops a role on something that no longer exists`() {
        val state = fleet().copy(
            roles = mapOf("dust" to Role.NETZ.id, "warpkern" to Role.GUETE.id, "net" to "r_teleport"),
        )
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertEquals(mapOf("dust" to Role.NETZ.id), back.roles)
    }

    // ------------------------------------------------------------------ the catalogue

    @Test
    fun `every role says what it is and what it does`() {
        for (role in Role.entries) {
            assertTrue(role.label.isNotBlank(), role.id)
            assertTrue(role.flavor.isNotBlank(), role.id)
            assertTrue(role.effectText.isNotBlank(), "${role.label} beschreibt sich nicht")
            assertTrue(role.output > 0.0 && role.cost > 0.0, role.label)
        }
        assertEquals(Role.entries.size, Role.entries.map { it.id }.toSet().size, "Doppelte Rollen-ID")
    }
}
