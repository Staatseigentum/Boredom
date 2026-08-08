package com.staatseigentum.kollaps.desktop

import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.SaveCodec
import java.io.File

/**
 * The save file for the desktop build.
 *
 * The same encoder the phone uses, so a block exported on one and pasted into the other is simply
 * a save. That is the whole cross-platform story and it needs no server: the format was already
 * text, and the export button was already there.
 *
 * Written next to the user's own files rather than beside the executable, because a game installed
 * into a read-only directory still has to be able to save.
 */
object DesktopSave {

    private val directory: File
        get() = File(System.getProperty("user.home"), ".kollaps")

    private val file: File get() = File(directory, "spielstand.txt")

    fun load(): GameState? = runCatching {
        if (!file.isFile) null else SaveCodec.decode(file.readText())
    }.getOrNull()

    /**
     * Writes through a temporary file and moves it into place.
     *
     * A save is written every few seconds and on the way out; a crash part way through a direct
     * write would leave half a file, and half a file decodes to nothing at all.
     */
    fun save(state: GameState) {
        runCatching {
            directory.mkdirs()
            val temporary = File(directory, "spielstand.txt.neu")
            temporary.writeText(SaveCodec.encode(state))
            if (!temporary.renameTo(file)) {
                file.writeText(temporary.readText())
                temporary.delete()
            }
        }
    }

    /** Where the file lives, for the line the window prints on startup. */
    fun location(): String = file.absolutePath
}
