package com.macrosnap.app.model

sealed interface DataError {
    data object NoApiKey : DataError
    data object InvalidApiKey : DataError
    data object Network : DataError
    data object RateLimited : DataError
    data object ModelUnavailable : DataError
    data object Server : DataError
    data object BadImage : DataError
    data object NotFood : DataError
    data object ProductNotFound : DataError
    data class Unexpected(val detail: String?) : DataError
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DataError) : Outcome<Nothing>
}
