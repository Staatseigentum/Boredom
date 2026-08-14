package com.staatseigentum.kollaps.core

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
    val title: String,
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
) {
    /** Where this contract stands, with its own zero subtracted where it has one. */
    fun progressOf(state: GameState): Double {
        val now = counter(state)
        if (!fromHere) return now
        val mark = state.contractMarks[id] ?: now
        return (now - mark).coerceAtLeast(0.0)
    }

    fun isMetBy(state: GameState): Boolean = progressOf(state) >= target

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
                title = "Fünfzig Sprossen über dem Tor",
                reward = 2.0,
                requiredBigBangs = Multiverse.SLOTS,
                counter = { (GameEngine.tierOf(it).index - Tiers.last.index).coerceAtLeast(0).toDouble() },
                target = 50.0,
            ),
            Contract(
                id = "ct_alloys",
                title = "Zwei Legierungen schmieden",
                reward = 3.0,
                requiredBigBangs = 1,
                counter = { it.alloys.size.toDouble() },
                target = 2.0,
                fromHere = true,
            ),
            Contract(
                id = "ct_collapses",
                title = "Fünf Kollapse",
                reward = 2.0,
                requiredBigBangs = 0,
                counter = { it.collapses.toDouble() },
                target = 5.0,
                fromHere = true,
            ),
            Contract(
                id = "ct_fleet",
                title = "Tausend Maschinen gleichzeitig",
                reward = 2.0,
                requiredBigBangs = 0,
                counter = { it.collectors.values.sum().toDouble() },
                target = 1_000.0,
            ),
            Contract(
                id = "ct_finds",
                title = "Drei Katalogfunde beantworten",
                reward = 3.0,
                requiredBigBangs = Multiverse.SLOTS,
                counter = { it.findsAnswered.toDouble() },
                target = 3.0,
                fromHere = true,
            ),
            Contract(
                id = "ct_sky",
                title = "Vier Galaxien gleichzeitig am Rechnen",
                reward = 4.0,
                requiredBigBangs = 4,
                counter = {
                    Multiverse.parked(it).count { g -> g.job == GalaxyJob.RECHNEN && !g.isRamping }.toDouble()
                },
                target = 4.0,
            ),
            Contract(
                id = "ct_challenge",
                title = "Eine Herausforderung bestehen",
                reward = 3.0,
                requiredBigBangs = 0,
                counter = { it.challengesDone.size.toDouble() },
                target = 1.0,
                fromHere = true,
            ),
            Contract(
                id = "ct_orbits",
                title = "Sechs Bahnen gleichzeitig besetzt",
                reward = 2.0,
                requiredBigBangs = 0,
                counter = { Orbits.occupiedCount(it).toDouble() },
                target = 6.0,
            ),
            Contract(
                id = "ct_research",
                title = "Drei Projekte durchziehen",
                reward = 3.0,
                requiredBigBangs = 1,
                counter = { it.research.size.toDouble() },
                target = 3.0,
                fromHere = true,
            ),
            Contract(
                id = "ct_metal",
                title = "Fünfhundert Gramm Gold im Lager",
                reward = 2.0,
                requiredBigBangs = 1,
                counter = { Heavy.amountOf(it, HeavyElement.GOLD) },
                target = 500.0,
            ),
        )

        fun byId(id: String?): Contract? = all.firstOrNull { it.id == id }

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

        /** The ids the table should hold, given what is on it and what has been finished. */
        fun refilled(state: GameState): List<String> {
            val eligible = eligible(state)
            if (eligible.isEmpty()) return emptyList()

            val kept = state.contracts.filter { id -> eligible.any { it.id == id } }.distinct()
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
                if (candidate.id !in filled && !candidate.isMetBy(state)) filled += candidate.id
                cursor++
                tries++
            }
            return filled
        }
    }
}
