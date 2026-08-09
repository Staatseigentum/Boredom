package com.staatseigentum.kollaps.data

import android.content.Context

/**
 * Which of the three saves this installation is currently playing.
 *
 * Deliberately not inside a save file. Whichever slot is active has to be known *before* any save
 * is read — a value stored in one of them could only be found by opening all three and guessing,
 * and two of them disagreeing would be unrecoverable.
 */
object SlotPreference {

    private const val PREFS = "kollaps-slots"
    private const val KEY = "active"

    fun active(context: Context): Int =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY, 0)
            // A number from a future version, or an edited file, must not leave the game unable
            // to open anything at all.
            .coerceIn(0, 2)

    fun setActive(context: Context, slot: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY, slot.coerceIn(0, 2))
            .apply()
    }
}
