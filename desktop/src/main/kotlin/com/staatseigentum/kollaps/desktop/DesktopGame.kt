package com.staatseigentum.kollaps.desktop

import com.staatseigentum.kollaps.ui.PlainGame

/**
 * The name this module has always called its driver, kept.
 *
 * The driver itself moved into the shared sources as [PlainGame] when the iPhone build needed it
 * too — forty one-line calls into the engine are not worth writing out a second time, and two
 * copies of them would have drifted the first time a method was added to the interface. Nothing
 * about it was ever specific to a desktop; it was simply the first platform that had no view model.
 *
 * A typealias rather than a rename, because `DesktopGame` reads correctly at every one of its uses
 * here — the window, the slots panel and every screenshot in the harness — and none of them care
 * where the class is declared.
 */
typealias DesktopGame = PlainGame
