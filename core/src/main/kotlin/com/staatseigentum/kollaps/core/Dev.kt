package com.staatseigentum.kollaps.core

import kotlin.concurrent.Volatile

/**
 * The switch that keeps an unfinished system out of the finished game.
 *
 * Everything the Akkretion update adds asks this before it does anything: impacts do not arrive,
 * layers cannot be built, world types are not recorded. Off by default, so the APK, the iPhone
 * build and the ordinary PC build are exactly the game that shipped — the new code is compiled in
 * and never reached.
 *
 * ## Why a flag and not a branch
 *
 * A branch would be the obvious answer and it is the worse one. A long-lived branch means the new
 * systems stop compiling against the game the moment anything else moves, and the divergence is
 * only discovered at merge time — which is precisely when it is most expensive. A flag keeps one
 * tree: every test runs against both states, and a change that breaks the new systems breaks the
 * build today rather than in a fortnight.
 *
 * It also gives the thing the update is *for*: a build somebody can play with, next to a build
 * they can trust, from the same source.
 *
 * ## How it goes on
 *
 * The desktop entry point turns it on when started with `--dev`. Nothing else does, anywhere.
 */
object Dev {

    /**
     * Whether the unfinished systems are live.
     *
     * `@Volatile` because it is written once from the entry point and read from the tick, and
     * those are not the same thread on any of the three platforms.
     */
    @Volatile
    var enabled: Boolean = false
        private set

    /** Turns the unfinished systems on. Called from the dev entry point and from tests. */
    fun enable() {
        enabled = true
    }

    /** Turns them off again. Only tests need this — a running game never goes back. */
    fun disable() {
        enabled = false
    }

    /**
     * Runs [block] with the new systems on, then puts the flag back where it was.
     *
     * For tests, which have to be able to check both states without leaking one into the next —
     * a test that left this on would silently change every test that ran after it.
     */
    fun <T> on(block: () -> T): T {
        val before = enabled
        enabled = true
        try {
            return block()
        } finally {
            enabled = before
        }
    }
}
