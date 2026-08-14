package com.staatseigentum.kollaps.core

import kotlin.concurrent.Volatile

/**
 * What time it is, as one named thing the whole game can ask.
 *
 * The lab, the two automation rules that watch it and the offline report all run on the real
 * clock rather than on played seconds, and the panels that count them down have to read the same
 * clock the rules were fed — a bar drawn against a different clock is a bar that disagrees with
 * the thing it is drawing.
 *
 * Until now that reading was `System.currentTimeMillis()`, written out at every site. Which was
 * fine while every platform was a JVM. It stopped being fine the moment the same screen had to
 * compile for an iPhone, where there is no `System` at all — and Kotlin has no common way to ask
 * for the epoch either, so the reading has to come from outside.
 *
 * So it does: each platform installs its own on the way in. That is one line in each entry point,
 * and it buys something the scattered calls never had — a single place where the game's idea of
 * "now" can be replaced, which is what a test that wants to stand at a particular moment needs.
 */
object Wallclock {

    /**
     * Milliseconds since the epoch, from whoever was asked to supply them.
     *
     * The fallback is a standing zero rather than a throw, because the thing that reads this is a
     * countdown on a screen: a platform that forgot to install its clock should draw a bar that
     * does not move, not take the whole game down. It is not a quiet failure either — every
     * countdown in the lab freezes at its full length, which is about as visible as it gets.
     */
    @Volatile
    private var reading: () -> Long = { 0L }

    /** Installs the platform's clock. Called once, before the first frame. */
    fun readFrom(clock: () -> Long) {
        reading = clock
    }

    /** What time it is now, in milliseconds since the epoch. */
    fun millis(): Long = reading()
}
