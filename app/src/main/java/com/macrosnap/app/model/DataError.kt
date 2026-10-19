package com.macrosnap.app.model

sealed interface DataError {
    data object NoApiKey : DataError
    data object InvalidApiKey : DataError
    data object Network : DataError

    /** [daily] when the day's quota is gone; otherwise a per-minute limit that clears on its own. */
    data class RateLimited(val daily: Boolean) : DataError
    data object ModelUnavailable : DataError
    data object Server : DataError
    data object BadImage : DataError
    data object NotFood : DataError
    data object ProductNotFound : DataError
    data class Unexpected(val detail: String?) : DataError
}

/**
 * Failures worth waiting out: the request was fine, the service just couldn't take it now. A photo
 * that fails this way is kept to resubmit later rather than thrown away.
 */
val DataError.isTemporary: Boolean
    get() = this is DataError.Server || this is DataError.RateLimited || this is DataError.Network

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DataError) : Outcome<Nothing>
}
