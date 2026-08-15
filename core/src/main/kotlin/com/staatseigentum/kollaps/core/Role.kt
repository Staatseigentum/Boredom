package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang
/**
 * How a collector is set up to work.
 *
 * Every role is a trade, never a bonus. That is the whole design: seventeen shop rows that only
 * differ by a number are seventeen numbers, and seventeen rows that each give something up to get
 * something else are seventeen decisions. It also means switching the whole fleet to the same role
 * cannot be the answer — whatever that role gives up, the fleet gives up with it.
 *
 * Roles cost nothing to change. The limit is how many may be assigned at once, which grows with
 * the collapses behind the player; a fee or a cooldown would tax fiddling rather than reward it.
 */
enum class Role(
    val id: String,
    val germanLabel: String,
    val germanFlavor: String,
    /** Multiplies what one copy of this collector produces. */
    val output: Double,
    /** Multiplies what a copy costs. */
    val cost: Double,
    /** What every *other* collector gains per [Milestones.STEP] copies of this one. */
    val networkPerStep: Double,
) {
    MENGE(
        id = "r_bulk",
        germanLabel = "Menge",
        germanFlavor = "Billiger gebaut, dafür schlampiger. Es zählt, wie viele es sind.",
        output = 0.80,
        cost = 0.85,
        networkPerStep = 0.0,
    ),
    GUETE(
        id = "r_quality",
        germanLabel = "Güte",
        germanFlavor = "Sorgfältig gebaut und entsprechend teuer. Es zählt, was einer leistet.",
        output = 1.50,
        cost = 1.25,
        networkPerStep = 0.0,
    ),
    NETZ(
        id = "r_network",
        germanLabel = "Netz",
        germanFlavor = "Arbeitet kaum noch selbst, sondern koordiniert alle anderen.",
        output = 0.60,
        cost = 1.0,
        networkPerStep = 0.06,
    ),
    ;

    val label: String get() = Lang.t(germanLabel)

    val flavor: String get() = Lang.t(germanFlavor)

    /** What it does, in words, for the row that offers it. */
    val effectText: String
        get() = buildList {
            if (output != 1.0) add(Lang.t("%s Ausstoß", Numbers.formatMultiplier(output)))
            if (cost != 1.0) add(Lang.t("%s Preis", Numbers.formatMultiplier(cost)))
            if (networkPerStep > 0.0) {
                add(
                    Lang.t(
                        "+%s auf alle anderen je %s Stück",
                        Numbers.formatPercent(networkPerStep),
                        Milestones.STEP,
                    ),
                )
            }
        }.joinToString(" · ")

    companion object {
        fun byId(id: String?): Role? = entries.firstOrNull { it.id == id }
    }
}

object Roles {

    /**
     * The body at which the fleet can be given roles.
     *
     * After the first few collectors exist and before the shop gets long, so the idea arrives
     * while there is still something obvious to try it on.
     */
    const val UNLOCK_TIER = "Venus"

    /** How many roles may be assigned before any collapse. */
    const val BASE_SLOTS = 2

    /** And the most there can ever be. Eight of seventeen is a choice; seventeen is a setting. */
    const val MAX_SLOTS = 8

    fun isUnlocked(state: GameState): Boolean =
        state.bestTier >= Tiers.indexOf(UNLOCK_TIER) || state.roles.isNotEmpty()

    /**
     * How many collectors may carry a role at once.
     *
     * Grows with collapses rather than with mass, so it is a reason to reset rather than another
     * thing that arrives on its own halfway through a run.
     */
    fun slots(state: GameState): Int =
        (BASE_SLOTS + state.collapses).coerceAtMost(MAX_SLOTS)

    fun roleOf(state: GameState, collectorId: String): Role? = Role.byId(state.roles[collectorId])

    fun assignedCount(state: GameState): Int =
        state.roles.count { Collectors.byId(it.key) != null && Role.byId(it.value) != null }

    fun hasFreeSlot(state: GameState): Boolean = assignedCount(state) < slots(state)

    /**
     * Sets, changes or clears one collector's role.
     *
     * Changing a role that is already assigned is always allowed — it occupies a slot either way.
     * Only taking up a *new* slot can be refused, and clearing one never can.
     */
    fun set(state: GameState, collectorId: String, role: Role?): GameState {
        if (Collectors.byId(collectorId) == null) return state
        if (!isUnlocked(state)) return state

        if (role == null) return state.copy(roles = state.roles - collectorId)
        if (collectorId !in state.roles && !hasFreeSlot(state)) return state

        return state.copy(roles = state.roles + (collectorId to role.id))
    }

    /** Walks one collector through the roles and back to none. */
    fun cycle(state: GameState, collectorId: String): GameState {
        val current = roleOf(state, collectorId)
        val next = when {
            current == null -> Role.entries.first()
            current.ordinal + 1 < Role.entries.size -> Role.entries[current.ordinal + 1]
            else -> null
        }
        return set(state, collectorId, next)
    }

    /** What one collector's own output is multiplied by, from its own role. */
    fun outputFactor(state: GameState, collectorId: String): Double =
        roleOf(state, collectorId)?.output ?: 1.0

    /** What one copy costs, multiplied by its own role. */
    fun costFactor(state: GameState, collectorId: String): Double =
        roleOf(state, collectorId)?.cost ?: 1.0

    /**
     * What every collector gains from the ones set to co-ordinate, excluding itself.
     *
     * A network collector does not co-ordinate itself: it would be paying its own output penalty
     * to hand itself a bonus, which is a rule nobody could reason about.
     */
    fun networkFactor(state: GameState, collectorId: String): Double {
        var factor = 1.0
        for ((otherId, roleId) in state.roles) {
            if (otherId == collectorId) continue
            val role = Role.byId(roleId) ?: continue
            if (role.networkPerStep <= 0.0) continue
            val steps = state.ownedOf(otherId) / Milestones.STEP
            if (steps > 0) factor *= 1.0 + role.networkPerStep * steps
        }
        return factor
    }
}
