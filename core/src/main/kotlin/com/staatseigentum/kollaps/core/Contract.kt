package com.staatseigentum.kollaps.core

import com.staatseigentum.kollaps.core.i18n.Lang

/**
 * Three things worth doing next, drawn from a deck and replaced as they are finished.
 *
 * Achievements are the wrong shape for this and always were: they observe. They notice that
 * something happened, after it happened, and the list is fixed for ever — which makes them a
 * record and not a direction. That was fine while the game had a ladder with an end on it, because
 * the direction was always "up". Above the black hole the direction is "up, for sixteen thousand
 * rungs", and a player who opens the app after a week away has no idea what they were in the middle
 * of.
 *
 * A contract answers that and nothing else. It is small, it is finishable in one sitting, it pays
 * Äonen — the currency the sky already trickles, so a contract is a way to hurry that trickle
 * rather than a fifth kind of money — and when it is done another takes its place.
 *
 * ## Why three, and why they are drawn rather than listed
 *
 * One would be a chore with no choice in it. Everything available at once would be a checklist, and
 * a checklist is what the achievements already are. Three is enough that at least one usually
 * suits what the player was doing anyway, and few enough that they can all be read at a glance.
 *
 * The draw is derived from how many have been finished, exactly like the catalogue finds and the
 * event chains, and for the same reason: the rules have no random source, and a save that is
 * reloaded must not be able to re-roll a hard contract into an easy one.
 */
data class Contract(
    val id: String,
    val germanTitle: String,
    /** What it pays, in Äonen. */
    val reward: Double,
    /** How deep a player has to be before this one is worth offering. */
    val requiredBigBangs: Int,
    /** The raw counter this reads. Never adjusted for anything — see [fromHere]. */
    val counter: (GameState) -> Double,
    /** What the counter has to reach. */
    val target: Double,
    /**
     * Whether the goal is "this many *more*", measured from when the contract was dealt.
     *
     * Some goals are a standing state — a thousand machines at once, six orbits occupied — and
     * read straight off the counter. Others are things you have to go and do, and those have to be
     * measured from somewhere, or a deep save finishes them by opening the tab.
     *
     * Where each contract's zero sits is recorded per contract in [GameState.contractMarks], and
     * that "per contract" is the whole of a bug worth remembering. The first version kept one
     * shared mark for the whole table, set to the *maximum* of the four counters it might need —
     * so a save with thirteen collapses and six research projects got a mark of thirteen, and
     * "drei Projekte durchziehen" quietly asked for sixteen. There are fourteen in the tree. The
     * contract could not be finished, could not be replaced, and sat on the table for ever showing
     * 0 / 3.
     */
    val fromHere: Boolean = false,
    /**
     * How often this may be handed in within one day, or `null` for as often as it comes up.
     *
     * Most of the deck limits itself: there are five alloys, sixteen challenges and fourteen
     * research projects ever, and a pile of gold that only grows — those are finished once and
     * never dealt again. Four are not like that, and one of them was a press. Collapses come every
     * few minutes to somebody deep in the catalogue ladder, so "fünf Kollapse" paid two Äonen on a
     * loop, for ever, faster than anything else in the game earns them.
     *
     * A limit and not a higher target, because a higher target only moves the loop. What was wrong
     * was not the price — it was that the thing being asked for had stopped being an achievement
     * and become a lever.
     */
    val dailyLimit: Int? = null,
    /**
     * The highest this contract's counter can ever go, where there is one.
     *
     * Some counters run out. There are sixteen challenges, five alloys and fourteen research
     * projects in the game, and once a player has all of them the counter stops for good — so
     * "eine Herausforderung bestehen" sat on the table for ever at 0 / 1, asking for a
     * seventeenth. `null` where the counter has no end: collapses, rungs and finds always have
     * another one.
     *
     * The same shape as the mark bug and worth naming as such: a contract is only a direction if
     * the thing it points at is still ahead of the player.
     */
    val ceiling: ((GameState) -> Double)? = null,
) {
    val title: String get() = Lang.t(germanTitle)

    /** Where this contract stands, with its own zero subtracted where it has one. */
    fun progressOf(state: GameState): Double {
        val now = counter(state)
        if (!fromHere) return now
        val mark = state.contractMarks[id] ?: now
        return (now - mark).coerceAtLeast(0.0)
    }

    fun isMetBy(state: GameState): Boolean = progressOf(state) >= target

    /**
     * Whether this can still be finished from where it is now.
     *
     * Counted against the counter's own ceiling and its own zero, so a contract that was dealt
     * while there was still room stays honest as the room runs out.
     */
    fun isPossibleFor(state: GameState): Boolean {
        val top = ceiling?.invoke(state) ?: return true
        val zero = if (fromHere) state.contractMarks[id] ?: counter(state) else 0.0
        return zero + target <= top
    }

    /** How far along, in `0f..1f`. */
    fun fractionOf(state: GameState): Double {
        if (target <= 0.0) return 1.0
        return (progressOf(state) / target).coerceIn(0.0, 1.0)
    }

    /** The line under the title: where you are against where you are going. */
    fun statusOf(state: GameState): String =
        "${Numbers.format(progressOf(state).coerceAtMost(target))} / ${Numbers.format(target)}"

    companion object {

        /** How many are on the table at once. */
        const val SLOTS = 3

        val all: List<Contract> = listOf(
            Contract(
                id = "ct_rungs",
                germanTitle = "Fünfzig Sprossen über dem Tor",
                reward = 2.0,
                requiredBigBangs = Multiverse.SLOTS,
                counter = { (GameEngine.tierOf(it).index - Tiers.last.index).coerceAtLeast(0).toDouble() },
                target = 50.0,
                dailyLimit = 8,
            ),
            Contract(
                id = "ct_alloys",
                germanTitle = "Zwei Legierungen schmieden",
                reward = 3.0,
                requiredBigBangs = 1,
                counter = { it.alloys.size.toDouble() },
                target = 2.0,
                fromHere = true,
                ceiling = { Alloy.entries.size.toDouble() },
            ),
            Contract(
                id = "ct_collapses",
                germanTitle = "Fünf Kollapse",
                reward = 2.0,
                requiredBigBangs = 0,
                counter = { it.collapses.toDouble() },
                target = 5.0,
                fromHere = true,
                dailyLimit = 5,
            ),
            Contract(
                id = "ct_fleet",
                germanTitle = "Tausend Maschinen gleichzeitig",
                reward = 2.0,
                requiredBigBangs = 0,
                counter = { it.collectors.values.sum().toDouble() },
                target = 1_000.0,
                // Die Flotte fällt mit jedem Kollaps auf null und ist danach wieder zu holen.
                dailyLimit = 5,
            ),
            Contract(
                id = "ct_finds",
                germanTitle = "Drei Katalogfunde beantworten",
                reward = 3.0,
                requiredBigBangs = Multiverse.SLOTS,
                counter = { it.findsAnswered.toDouble() },
                target = 3.0,
                fromHere = true,
                dailyLimit = 8,
            ),
            Contract(
                id = "ct_sky",
                germanTitle = "Vier Galaxien gleichzeitig am Rechnen",
                reward = 4.0,
                requiredBigBangs = 4,
                counter = {
                    Multiverse.parked(it).count { g -> g.job == GalaxyJob.RECHNEN && !g.isRamping }.toDouble()
                },
                target = 4.0,
                dailyLimit = 5,
                ceiling = { Multiverse.count(it).toDouble() },
            ),
            Contract(
                id = "ct_challenge",
                germanTitle = "Eine Herausforderung bestehen",
                reward = 3.0,
                requiredBigBangs = 0,
                counter = { it.challengesDone.size.toDouble() },
                target = 1.0,
                fromHere = true,
                ceiling = { Challenge.entries.size.toDouble() },
            ),
            Contract(
                id = "ct_orbits",
                germanTitle = "Sechs Bahnen gleichzeitig besetzt",
                reward = 2.0,
                requiredBigBangs = 0,
                counter = { Orbits.occupiedCount(it).toDouble() },
                target = 6.0,
                // Dasselbe für die Bahnen: nach dem Kollaps sind sie leer und neu besetzbar.
                dailyLimit = 5,
            ),
            Contract(
                id = "ct_research",
                germanTitle = "Drei Projekte durchziehen",
                reward = 3.0,
                requiredBigBangs = 1,
                counter = { it.research.size.toDouble() },
                target = 3.0,
                fromHere = true,
                ceiling = { ResearchTree.all.size.toDouble() },
            ),
            Contract(
                id = "ct_metal",
                germanTitle = "Fünfhundert Gramm Gold im Lager",
                reward = 2.0,
                requiredBigBangs = 1,
                counter = { Heavy.amountOf(it, HeavyElement.GOLD) },
                target = 500.0,
                // Gold überlebt zwar alles, aber ein Limit kostet hier nichts und schließt die Lücke.
                dailyLimit = 5,
            ),
        )

        fun byId(id: String?): Contract? = all.firstOrNull { it.id == id }

        /**
         * Which day it is, for the daily limits.
         *
         * Whole days since the epoch, so no timezone database and no clock arithmetic. It rolls
         * over at midnight UTC rather than at the player's midnight, which is a real difference
         * and the right trade: a fixed, checkable moment beats a correct one that needs a timezone
         * table shipped into a game about rocks.
         */
        fun dayOf(nowMillis: Long): Long = nowMillis / 86_400_000L

        /** How often [contract] has been handed in today. */
        fun doneToday(state: GameState, contract: Contract): Int =
            state.contractsToday[contract.id] ?: 0

        /** Whether the day's allowance for [contract] is used up. */
        fun isSpentToday(state: GameState, contract: Contract): Boolean {
            val limit = contract.dailyLimit ?: return false
            return doneToday(state, contract) >= limit
        }

        /** Contracts appear once there is a game deep enough to have directions worth giving. */
        fun isUnlocked(state: GameState): Boolean = state.collapses >= 1 || state.contractsDone > 0

        /** What this state is deep enough to be given. */
        private fun eligible(state: GameState): List<Contract> =
            all.filter { state.bigBangs >= it.requiredBigBangs }

        /**
         * The three on the table.
         *
         * Held in the save as ids so they survive a restart, and refilled here rather than at some
         * event — a player who has never opened the tab still has three waiting when they do.
         */
        fun offered(state: GameState): List<Contract> =
            state.contracts.mapNotNull(::byId).take(SLOTS)

        /**
         * Whether an empty table means "done for today" rather than "done for good".
         *
         * These are two very different things and they used to look identical: the panel simply
         * vanished. That was survivable while three contracts had no daily allowance and so could
         * always be drawn — but the Äonen audit found those three were exactly the ones that could
         * be farmed, and closing that hole made an empty table reachable. A player who clears every
         * allowance in a day would have watched the whole section disappear with no word about why
         * or whether it comes back.
         *
         * So the state gets a name and the panel gets a sentence. Something the clock will hand
         * back tomorrow is a reward for a thorough day, not an absence.
         */
        fun restingUntilTomorrow(state: GameState): Boolean =
            isUnlocked(state) &&
                offered(state).isEmpty() &&
                all.any { it.requiredBigBangs <= state.bigBangs && isSpentToday(state, it) }

        /** The ids the table should hold, given what is on it and what has been finished. */
        fun refilled(state: GameState): List<String> {
            val eligible = eligible(state)
            if (eligible.isEmpty()) return emptyList()

            // Anything whose allowance is gone leaves the table rather than sitting on it
            // unclaimable. A row that cannot be finished today is not a direction, it is furniture.
            val kept = state.contracts
                .filter { id -> eligible.any { it.id == id } }
                .filterNot { id -> byId(id)?.let { isSpentToday(state, it) } == true }
                // And nothing whose counter can no longer reach its target. A row asking for a
                // seventeenth challenge is not a hard contract, it is a permanent piece of
                // furniture on the one screen whose whole job is to say what to do next.
                .filterNot { id -> byId(id)?.isPossibleFor(state) == false }
                .distinct()
            if (kept.size >= SLOTS) return kept.take(SLOTS)

            // Drawn by counting forward from how many have been finished, so the deck advances
            // rather than repeating, and two saves at the same point are dealt the same hand.
            //
            // Anything already satisfied by the state it is being dealt into is skipped. Without
            // that the contract just handed in comes straight back — it is still met, so it would
            // be a free Äon and no direction at all — and so would every contract whose target a
            // deep save has long passed.
            val filled = kept.toMutableList()
            var cursor = state.contractsDone
            var tries = 0
            while (filled.size < SLOTS && tries < eligible.size) {
                val candidate = eligible[cursor.mod(eligible.size)]
                val fits = candidate.id !in filled &&
                    !candidate.isMetBy(state) &&
                    !isSpentToday(state, candidate) &&
                    candidate.isPossibleFor(state)
                if (fits) {
                    filled += candidate.id
                }
                cursor++
                tries++
            }
            return filled
        }
    }
}
