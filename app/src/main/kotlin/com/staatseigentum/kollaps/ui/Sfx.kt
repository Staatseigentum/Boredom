package com.staatseigentum.kollaps.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Everything the game makes a noise about.
 *
 * An interface rather than a class, because playing a short sample is the second thing (after
 * turning pixels into a bitmap) that every platform does its own way. The interface itself has
 * to stay free of any platform type so the whole screen can be compiled for the desktop harness.
 *
 * The cues beyond [click] have default no-op bodies so a platform can grow into them rather than
 * having to answer for all of them at once.
 */
interface Sounds {
    /** Any tap: the body, a button, a row of the shop. */
    fun click()

    /** A new body on the ladder. */
    fun levelUp() = Unit

    /** A comet caught in time. */
    fun comet() = Unit

    /** Something bought. */
    fun purchase() = Unit

    /** A challenge handed in. */
    fun success() = Unit
}

/**
 * Reaches every button and row without threading a parameter through the whole tree. Null when
 * nothing provides it, so previews, tests and the harness stay silent instead of needing audio.
 */
val LocalSfx = staticCompositionLocalOf<Sounds?> { null }
