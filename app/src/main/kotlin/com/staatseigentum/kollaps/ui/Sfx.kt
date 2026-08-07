package com.staatseigentum.kollaps.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The click.
 *
 * An interface rather than a class, because playing a short sample is the second thing (after
 * turning pixels into a bitmap) that every platform does its own way. The interface itself has
 * to stay free of any platform type so the whole screen can be compiled for the desktop harness.
 */
interface Sounds {
    fun click()
}

/**
 * Reaches every button and row without threading a parameter through the whole tree. Null when
 * nothing provides it, so previews, tests and the harness stay silent instead of needing audio.
 */
val LocalSfx = staticCompositionLocalOf<Sounds?> { null }
