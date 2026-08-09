package com.staatseigentum.kollaps.data

import android.content.Context
import android.util.Log
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.SaveCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads and writes one save file.
 *
 * Writes go to a temporary file first and are then renamed over the real one, so a process that
 * dies mid-write leaves the previous save intact instead of a truncated one.
 *
 * There are three of these now, one per slot. The first keeps the original file name on purpose:
 * every game played before slots existed is slot one and stays exactly where it was, which is the
 * difference between adding a feature and taking somebody's save away.
 */
class SaveStore(context: Context, slot: Int = 0) {

    private val name = fileNameFor(slot)
    private val file = File(context.filesDir, name)
    private val temp = File(context.filesDir, "$name.tmp")

    suspend fun load(): GameState? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext null
            SaveCodec.decode(file.readText())
        } catch (e: Exception) {
            Log.w(TAG, "Spielstand konnte nicht gelesen werden", e)
            null
        }
    }

    suspend fun save(state: GameState) = withContext(Dispatchers.IO) {
        try {
            temp.writeText(SaveCodec.encode(state))
            if (!temp.renameTo(file)) {
                // Some file systems refuse to rename onto an existing file.
                file.delete()
                temp.renameTo(file)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Spielstand konnte nicht geschrieben werden", e)
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        temp.delete()
        Unit
    }

    companion object {
        private const val FILE_NAME = "kollaps.save"
        private const val TAG = "SaveStore"

        /** Slot one keeps the name it always had; the others are numbered after it. */
        fun fileNameFor(slot: Int): String =
            if (slot <= 0) FILE_NAME else "kollaps-${slot + 1}.save"

        /** Whether a slot has anything in it, without decoding what. */
        fun exists(context: Context, slot: Int): Boolean =
            File(context.filesDir, fileNameFor(slot)).length() > 0
    }
}
