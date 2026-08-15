package com.staatseigentum.kollaps.core

import kotlin.concurrent.Volatile

/**
 * Which of the newest systems are live.
 *
 * One switch, on, and it exists for exactly one reason: so that taking an update back out is a
 * line rather than an operation. Everything the Akkretion update added asks this before it does
 * anything — impacts do not arrive, layers cannot be built, world types are not recorded, the
 * palette is not bent — so setting [accretion] to `false` returns the game to precisely what
 * 4.2.1 was, in one edit, with no revert and no merge.
 *
 * ## Why it survives the release
 *
 * It was written as a development flag and it earned a place in the shipped build. A system this
 * size lands in the first ten minutes of every save; if it turns out to be wrong, the difference
 * between a one-line release and unpicking a thousand-line change across three platforms is a day.
 * The cost of keeping it is one boolean read per fold.
 *
 * ## What it does not do
 *
 * It does not touch the save. Every field the update added has a default, so a save written with
 * the systems live still decodes with them switched off — the shells and the loose material simply
 * stop being read. Nobody loses a game either way round.
 */
object Rollout {

    /**
     * Whether the Akkretion systems are live.
     *
     * `@Volatile` because it is read from the tick and written, if ever, from a test — and those
     * are not the same thread on any of the three platforms.
     */
    @Volatile
    var accretion: Boolean = true
        private set

    /**
     * Runs [block] with the Akkretion systems switched [live], then puts the flag back.
     *
     * For tests, which have to be able to check both states without leaking one into the next: a
     * test that left this switched off would silently change every test that ran after it.
     */
    fun <T> accretion(live: Boolean, block: () -> T): T {
        val before = accretion
        accretion = live
        try {
            return block()
        } finally {
            accretion = before
        }
    }
}
