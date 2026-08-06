package com.staatseigentum.kollaps.data

import android.content.Context
import android.util.Log
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.SaveCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads and writes the save file.
 *
 * Writes go to a temporary file first and are then renamed over the real one, so a process that
 * dies mid-write leaves the previous save intact instead of a truncated one.
 */
class SaveStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)
    private val temp = File(context.filesDir, "$FILE_NAME.tmp")

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

    private companion object {
        const val FILE_NAME = "kollaps.save"
        const val TAG = "SaveStore"
    }
}
