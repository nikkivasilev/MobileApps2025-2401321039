package com.macrosnap.app.ui

import android.util.Log
import kotlinx.coroutines.CancellationException

const val SAVE_FAILED_MESSAGE = "Couldn't save. Try again."

/**
 * Runs a local write and reports whether it went through, so a database failure puts the screen
 * back in an editable state instead of leaving it stuck on "saving".
 */
suspend fun trySave(block: suspend () -> Unit): Boolean = try {
    block()
    true
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.e("MacroSnap", "Save failed", e)
    false
}
