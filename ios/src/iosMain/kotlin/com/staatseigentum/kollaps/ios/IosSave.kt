package com.staatseigentum.kollaps.ios

import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.SaveCodec
import platform.Foundation.NSUserDefaults

/**
 * The save for the iPhone build.
 *
 * The same encoder the phone and the PC use, so a block exported on one and pasted into another
 * is simply a save — which is the whole cross-platform story and it needs no server. It matters
 * more here than anywhere else: this build is sideloaded and re-signed by hand every so often,
 * and the export block is what carries a game across a reinstall.
 *
 * Kept in the user defaults rather than in a file. A save is a few kilobytes of text, the defaults
 * are backed up and restored with the device, and the alternative — a file in Documents — buys
 * nothing but error handling. Three slots, like everywhere else, and the first keeps a name of its
 * own so that nothing has to be moved if a fourth is ever added.
 */
object IosSave {

    /** How many places there are to keep a game. Matches the phone and the PC. */
    const val SLOTS = 3

    private val defaults: NSUserDefaults get() = NSUserDefaults.standardUserDefaults

    private fun keyFor(slot: Int): String =
        if (slot <= 0) "spielstand" else "spielstand-${slot + 1}"

    /**
     * Which slot is being played.
     *
     * Under its own key rather than inside a save, because it has to be known before any save is
     * read — a value kept in one of the three could only be found by opening all three and
     * guessing which to believe.
     */
    private const val MARKER = "platz"

    fun activeSlot(): Int = defaults.integerForKey(MARKER).toInt().coerceIn(0, SLOTS - 1)

    fun setActiveSlot(slot: Int) {
        defaults.setInteger(slot.coerceIn(0, SLOTS - 1).toLong(), MARKER)
    }

    fun load(slot: Int = activeSlot()): GameState? {
        val stored = defaults.stringForKey(keyFor(slot)) ?: return null
        return runCatching { SaveCodec.decode(stored) }.getOrNull()
    }

    fun save(state: GameState, slot: Int = activeSlot()) {
        runCatching { defaults.setObject(SaveCodec.encode(state), keyFor(slot)) }
    }

    /** Whether there is anything in a slot at all, for the panel that lists them. */
    fun isEmpty(slot: Int): Boolean = defaults.stringForKey(keyFor(slot)) == null
}
