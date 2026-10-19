package com.macrosnap.app.ui

import android.util.Log
import com.macrosnap.app.R
import kotlinx.coroutines.CancellationException

val SAVE_FAILED_MESSAGE: UiText = UiText.Res(R.string.save_failed)

/**
 * Runs a local write and reports whether it went through, so a database failure puts the screen
 * back in an editable state instead of leaving it stuck on "saving".
 */
@Suppress("TooGenericExceptionCaught") // Whatever the database throws, the screen must recover.
suspend fun trySave(block: suspend () -> Unit): Boolean = try {
    block()
    true
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.e("MacroSnap", "Save failed", e)
    false
}
