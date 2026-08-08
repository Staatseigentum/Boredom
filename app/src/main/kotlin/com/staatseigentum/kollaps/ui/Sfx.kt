package com.staatseigentum.kollaps.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.staatseigentum.kollaps.core.audio.Mood

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

    /** A comet gone by uncaught. */
    fun missed() = Unit

    /** Something bought. */
    fun purchase() = Unit

    /** A challenge handed in. */
    fun success() = Unit

    /** A research project come due. */
    fun research() = Unit

    /** The core catching light. */
    fun ignition() = Unit
}

/**
 * Reaches every button and row without threading a parameter through the whole tree. Null when
 * nothing provides it, so previews, tests and the harness stay silent instead of needing audio.
 */
val LocalSfx = staticCompositionLocalOf<Sounds?> { null }

/**
 * The background loop.
 *
 * Its own seam rather than another method on [Sounds] because it is a different kind of thing:
 * a cue is fired and forgotten, while this one holds a stream open, has to be told to stop, and
 * has to survive the screen recomposing around it.
 */
interface Music {
    /** Starts, or crossfades to, the loop for this mood. Repeated calls with the same mood do nothing. */
    fun play(mood: Mood)

    /** Stops and frees whatever is playing. */
    fun stop()
}

/** Null wherever nobody can play anything — the harness, previews, tests. */
val LocalMusic = staticCompositionLocalOf<Music?> { null }
