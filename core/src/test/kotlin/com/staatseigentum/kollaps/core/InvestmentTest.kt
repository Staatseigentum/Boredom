package com.staatseigentum.kollaps.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InvestmentTest {

    private val now = 1_700_000_000_000L

    private fun rich(singularities: Double = 1e6, collapses: Int = 9): GameState =
        GameState(singularities = singularities, collapses = collapses)

    private fun investment(id: String): Investment =
        Investments.byId(id) ?: error("Investition fehlt: $id")

    // ------------------------------------------------------------------ the price curve

    @Test
    fun `a level costs what the row says it costs`() {
        for (item in Investments.all) {
            val summed = (0 until 6).sumOf { item.costAt(it) }
            assertEquals(summed, item.costForLevels(0, 6), summed * 1e-9, item.name)
        }
    }

    @Test
    fun `every level is dearer than the one before it`() {
        for (item in Investments.all) {
            assertTrue(item.growth > 1.0, "${item.name} wird nicht teurer")
            assertTrue(item.costAt(5) > item.costAt(4), item.name)
        }
    }

    @Test
    fun `the max button never offers a level that cannot be paid for`() {
        val item = investment("i_global")
        for (purse in listOf(0.0, item.baseCost - 0.01, item.baseCost, 500.0, 1e9)) {
            val count = item.affordableLevels(level = 0, singularities = purse)
            assertTrue(
                item.costForLevels(0, count) <= purse + 1e-6,
                "Bei $purse wurden $count Stufen angeboten",
            )
        }
    }

    @Test
    fun `the max button stops at the ceiling`() {
        val item = investment("i_offline_share")
        assertEquals(item.maxLevel, item.affordableLevels(level = 0, singularities = 1e12))
        assertEquals(0, item.affordableLevels(level = item.maxLevel, singularities = 1e12))
    }

    // ------------------------------------------------------------------ buying

    @Test
    fun `buying takes the singularities and gives the levels`() {
        val item = investment("i_tap")
        val before = rich()
        val after = GameEngine.buyInvestment(before, item.id, 4)

        assertEquals(4, Investments.levelOf(after, item))
        assertEquals(
            before.singularities - item.costForLevels(0, 4),
            after.singularities,
            before.singularities * 1e-12,
        )
    }

    @Test
    fun `buying with no amount buys as many as are affordable`() {
        val item = investment("i_tap")
        val state = rich(singularities = item.costForLevels(0, 3))

        val after = GameEngine.buyInvestment(state, item.id, 0)
        assertEquals(3, Investments.levelOf(after, item))
        assertTrue(after.singularities < item.costAt(3), "Es blieb genug für noch eine Stufe übrig")
    }

    @Test
    fun `buying is refused without the singularities`() {
        val item = investment("i_tap")
        val broke = rich(singularities = item.baseCost - 0.01)
        assertEquals(broke, GameEngine.buyInvestment(broke, item.id, 1))
    }

    @Test
    fun `buying is refused before enough collapses`() {
        val gated = Investments.all.first { it.requiredCollapses > 0 }
        val early = rich(collapses = gated.requiredCollapses - 1)

        assertEquals(early, GameEngine.buyInvestment(early, gated.id, 1))
        assertTrue(gated !in Investments.offered(early))
    }

    @Test
    fun `the ceiling holds`() {
        val item = investment("i_offline_share")
        val filled = GameEngine.buyInvestment(rich(), item.id, item.maxLevel + 50)

        assertEquals(item.maxLevel, Investments.levelOf(filled, item))
        assertEquals(filled, GameEngine.buyInvestment(filled, item.id, 1))
    }

    @Test
    fun `a level beyond the ceiling in an old save is read as the ceiling`() {
        val item = investment("i_comets")
        val state = rich().copy(investments = mapOf(item.id to item.maxLevel + 999))
        assertEquals(item.maxLevel, Investments.levelOf(state, item))
    }

    // ------------------------------------------------------------------ what they do

    /**
     * The single most important property of the whole file.
     *
     * A repeatable purchase whose effect compounds is a purchase whose price curve decides the
     * balance by accident. Two levels have to be worth exactly twice one level's bonus, not the
     * square of it.
     */
    @Test
    fun `the effect is linear in the level, never exponential`() {
        val item = investment("i_global")
        val base = GameState(collectors = mapOf("dust" to 100))

        val one = base.copy(investments = mapOf(item.id to 1))
        val two = base.copy(investments = mapOf(item.id to 2))
        val ten = base.copy(investments = mapOf(item.id to 10))

        val plain = GameEngine.massPerSecond(base)
        val step = GameEngine.massPerSecond(one) / plain - 1.0

        assertEquals(1.0 + 2 * step, GameEngine.massPerSecond(two) / plain, 1e-9)
        assertEquals(1.0 + 10 * step, GameEngine.massPerSecond(ten) / plain, 1e-9)
    }

    @Test
    fun `each investment moves the lever it names`() {
        val fleet = GameState(collectors = mapOf("dust" to 100))

        assertTrue(
            GameEngine.massPerSecond(fleet.copy(investments = mapOf("i_global" to 5))) >
                GameEngine.massPerSecond(fleet),
        )
        assertTrue(
            GameEngine.massPerTap(fleet.copy(investments = mapOf("i_tap" to 5))) >
                GameEngine.massPerTap(fleet),
        )
        assertTrue(
            GameEngine.cometFrequency(fleet.copy(investments = mapOf("i_comets" to 5))) >
                GameEngine.cometFrequency(fleet),
        )
        assertTrue(
            GameEngine.researchSpeed(fleet.copy(investments = mapOf("i_research" to 5))) >
                GameEngine.researchSpeed(fleet),
        )
        assertTrue(
            GameEngine.fusionRate(fleet.copy(investments = mapOf("i_fusion" to 5))) >
                GameEngine.fusionRate(fleet),
        )
    }

    @Test
    fun `the offline share still never passes one`() {
        val item = investment("i_offline_share")
        val state = rich().copy(investments = mapOf(item.id to item.maxLevel))
        assertTrue(GameEngine.stats(state).offlineEfficiency <= 1.0)
    }

    /** Starting bonuses are read once when a run begins, not by the modifier fold. */
    @Test
    fun `a fresh run starts with what was invested in it`() {
        val ready = GameState(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            investments = mapOf("i_start_mass" to 3, "i_fleet" to 4),
        )
        assertTrue(GameEngine.canCollapse(ready))

        val after = GameEngine.collapse(ready, now)
        assertTrue(after.mass > 0.0, "Der neue Durchlauf beginnt ohne Startmasse")
        assertEquals(
            25 + 3 * 4,
            after.ownedOf(Collectors.all.first().id),
            "Die eingelagerte Flotte fehlt",
        )
    }

    @Test
    fun `the flat upgrade and the investment add up rather than replacing each other`() {
        val ready = GameState(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            prestigeUpgrades = setOf("p_start_mass"),
        )
        val flatOnly = GameEngine.collapse(ready, now).mass
        val both = GameEngine
            .collapse(ready.copy(investments = mapOf("i_start_mass" to 1)), now)
            .mass

        assertTrue(both > flatOnly, "Die erste Stufe bringt nichts über das Upgrade hinaus")
    }

    // ------------------------------------------------------------------ what survives

    @Test
    fun `investments survive a collapse`() {
        val ready = GameState(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            investments = mapOf("i_global" to 7),
        )
        assertEquals(mapOf("i_global" to 7), GameEngine.collapse(ready, now).investments)
    }

    /** Bought with singularities, and the big bang takes those — so it takes these too. */
    @Test
    fun `investments do not survive a big bang`() {
        val ready = GameState(
            runMass = Tiers.last.threshold,
            bestTier = Tiers.last.index,
            collapses = BigBang.REQUIRED_COLLAPSES,
            singularities = 500.0,
            investments = mapOf("i_global" to 7),
        )
        assertTrue(BigBang.canBang(ready))

        val after = GameEngine.bigBang(ready, now)
        assertTrue(after.investments.isEmpty(), "Investitionen haben den Urknall überlebt")
        assertEquals(0.0, after.singularities)
    }

    @Test
    fun `a save keeps the investments`() {
        val state = rich().copy(investments = mapOf("i_global" to 3, "i_comets" to 1))
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertEquals(state.investments, back.investments)
    }

    @Test
    fun `a save drops investments that no longer exist`() {
        val state = rich().copy(investments = mapOf("i_global" to 3, "i_teleport" to 9))
        val back = SaveCodec.decode(SaveCodec.encode(state))
        assertNotNull(back)
        assertEquals(mapOf("i_global" to 3), back.investments)
    }

    // ------------------------------------------------------------------ the catalogue

    @Test
    fun `every investment is worth reading`() {
        for (item in Investments.all) {
            assertTrue(item.name.isNotBlank(), item.id)
            assertTrue(item.flavor.isNotBlank(), item.id)
            assertTrue(item.effectText.isNotBlank(), "${item.name} sagt nicht, was eine Stufe bringt")
            assertTrue(item.baseCost > 0.0, item.name)
            assertTrue(item.maxLevel > 0, item.name)
            assertTrue(item.requiredCollapses >= 0, item.name)
        }
        assertEquals(
            Investments.all.size,
            Investments.all.map { it.id }.toSet().size,
            "Doppelte Investitions-ID",
        )
    }

    /**
     * The one that pays in its own currency has to be the steepest.
     *
     * More singularities per collapse buys more levels of more singularities per collapse. The
     * loop is fine as long as the price outruns it, and the price only outruns it if this is the
     * steepest curve on the list.
     */
    @Test
    fun `the feedback loop is the most expensive thing on the list`() {
        val gain = investment("i_gain")
        for (other in Investments.all) {
            if (other.id == gain.id) continue
            assertTrue(
                gain.growth >= other.growth,
                "${other.name} wird schneller teuer als der Rückkopplungs-Posten",
            )
        }
        assertTrue(gain.maxLevel <= 25, "Die Rückkopplung ist zu tief gedeckelt zu werden")
    }

    @Test
    fun `the first levels are affordable from a first collapse`() {
        // Twelve singularities is what one bare collapse at the threshold pays out.
        val starting = GameEngine.SINGULARITY_SCALE
        val reachable = Investments.all.filter {
            it.requiredCollapses == 0 && it.baseCost <= starting
        }
        assertTrue(
            reachable.size >= 2,
            "Nach dem ersten Kollaps ist fast nichts davon bezahlbar",
        )
    }

    @Test
    fun `nothing costs so much that it can never be bought`() {
        for (item in Investments.all) {
            val ten = item.costForLevels(0, 10)
            assertTrue(
                ten.isFinite() && ten < 1e9,
                "${item.name}: zehn Stufen kosten ${Numbers.format(ten)}",
            )
        }
    }

    // ------------------------------------------------------------------ erasing

    /**
     * What "delete everything" has to mean.
     *
     * The erase button hands the game a brand new [GameState], so anything that arrives non-empty
     * by default would quietly survive a deletion the player asked for. This is the test that
     * fails the day somebody gives a field a default with something in it.
     */
    @Test
    fun `a brand new game carries nothing over`() {
        val fresh = GameState.new(now)

        assertEquals(0.0, fresh.mass)
        assertEquals(0.0, fresh.runMass)
        assertEquals(0.0, fresh.totalMass)
        assertEquals(0.0, fresh.singularities)
        assertEquals(0.0, fresh.aeons)
        assertEquals(0, fresh.collapses)
        assertEquals(0, fresh.bigBangs)
        assertEquals(0L, fresh.taps)
        assertEquals(0L, fresh.cometsCaught)
        assertEquals(0, fresh.bestTier)
        assertEquals(0.0, fresh.playedSeconds)

        assertTrue(fresh.collectors.isEmpty())
        assertTrue(fresh.upgrades.isEmpty())
        assertTrue(fresh.prestigeUpgrades.isEmpty())
        assertTrue(fresh.investments.isEmpty())
        assertTrue(fresh.achievements.isEmpty())
        assertTrue(fresh.challengesDone.isEmpty())
        assertTrue(fresh.aeonUpgrades.isEmpty())
        assertTrue(fresh.elements.isEmpty())
        assertTrue(fresh.fusers.isEmpty())
        assertTrue(fresh.research.isEmpty())
        assertTrue(fresh.automation.isEmpty())
        assertEquals(null, fresh.activeResearch)
        assertEquals(null, fresh.activeChallenge)
        assertEquals(null, fresh.pendingEvent)
    }

    @Test
    fun `a brand new game has nothing bought and nothing unlocked`() {
        val fresh = GameState.new(now)

        assertEquals(Tiers.first, GameEngine.tierOf(fresh))
        assertEquals(0.0, GameEngine.massPerSecond(fresh))
        assertTrue(!Fusion.isUnlocked(fresh))
        assertTrue(!ResearchTree.isUnlocked(fresh))
        assertTrue(!Automation.isUnlocked(fresh))
        assertTrue(!BigBang.isUnlocked(fresh))
        assertTrue(Investments.totalLevels(fresh) == 0)
        assertTrue(abs(GameEngine.stats(fresh).globalMultiplier - 1.0) < 1e-9)
    }
}
