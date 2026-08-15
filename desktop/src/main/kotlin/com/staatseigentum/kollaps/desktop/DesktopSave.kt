package com.staatseigentum.kollaps.desktop

import com.staatseigentum.kollaps.core.Dev
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.SaveCodec
import java.io.File

/**
 * The save files for the desktop build.
 *
 * The same encoder the phone uses, so a block exported on one and pasted into the other is simply
 * a save. That is the whole cross-platform story and it needs no server: the format was already
 * text, and the export button was already there.
 *
 * Written next to the user's own files rather than beside the executable, because a game installed
 * into a read-only directory still has to be able to save.
 *
 * Three slots, like the phone. The first keeps the file name it always had, so a game played
 * before slots existed is slot one and stays exactly where it was.
 */
object DesktopSave {

    /** How many places there are to keep a game. Matches the phone. */
    const val SLOTS = 3

    /**
     * Where the files go, which is somewhere else entirely while the dev systems are on.
     *
     * The whole point of the dev build is to be able to break things, and a save is the one thing
     * in this game that cannot be rebuilt from source. Sharing a directory would mean one crash in
     * an unfinished system costs somebody the four hours they actually played — so the dev build
     * keeps its own three slots, its own marker, and never opens the real ones at all.
     */
    private val directory: File
        get() = File(
            System.getProperty("user.home"),
            if (Dev.enabled) ".kollaps-dev" else ".kollaps",
        )

    private fun fileFor(slot: Int): File =
        File(directory, if (slot <= 0) "spielstand.txt" else "spielstand-${slot + 1}.txt")

    /**
     * Which slot is being played.
     *
     * In a file of its own rather than inside a save, because it has to be known before any save
     * is read — a value kept in one of the three could only be found by opening all three and
     * guessing which to believe.
     */
    private val marker: File get() = File(directory, "platz.txt")

    fun activeSlot(): Int = runCatching {
        marker.takeIf { it.isFile }?.readText()?.trim()?.toIntOrNull()
    }.getOrNull()?.coerceIn(0, SLOTS - 1) ?: 0

    fun setActiveSlot(slot: Int) {
        runCatching {
            directory.mkdirs()
            marker.writeText(slot.coerceIn(0, SLOTS - 1).toString())
        }
    }

    fun load(slot: Int = activeSlot()): GameState? = runCatching {
        val file = fileFor(slot)
        if (!file.isFile) null else SaveCodec.decode(file.readText())
    }.getOrNull()

    /**
     * Writes through a temporary file and moves it into place.
     *
     * A save is written every few seconds and on the way out; a crash part way through a direct
     * write would leave half a file, and half a file decodes to nothing at all.
     */
    fun save(state: GameState, slot: Int = activeSlot()) {
        runCatching {
            directory.mkdirs()
            val file = fileFor(slot)
            val temporary = File(directory, "${file.name}.neu")
            temporary.writeText(SaveCodec.encode(state))
            if (!temporary.renameTo(file)) {
                file.writeText(temporary.readText())
                temporary.delete()
            }
        }
    }

    /** Where the file lives, for the line the window prints on startup. */
    fun location(slot: Int = activeSlot()): String = fileFor(slot).absolutePath
}
