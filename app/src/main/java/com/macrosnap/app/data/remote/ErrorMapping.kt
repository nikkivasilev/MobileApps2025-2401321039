package com.macrosnap.app.data.remote

import com.macrosnap.app.model.DataError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Maps transport failures to the domain error. Callers must rethrow CancellationException first. */
fun Throwable.toDataError(): DataError = when (this) {
    is HttpException -> toDataError()
    is IOException -> DataError.Network
    is SerializationException -> DataError.Unexpected("Unreadable response from server")
    else -> DataError.Unexpected(message)
}

private fun HttpException.toDataError(): DataError {
    val body = try {
        response()?.errorBody()?.string().orEmpty()
    } catch (e: IOException) {
        ""
    }
    return when (code()) {
        // Gemini reports a bad key as 400 INVALID_ARGUMENT rather than 401.
        400 -> if ("API key" in body || "API_KEY" in body) DataError.InvalidApiKey else DataError.Unexpected(body.take(300))
        401, 403 -> DataError.InvalidApiKey
        404 -> DataError.ModelUnavailable
        429 -> DataError.RateLimited
        in 500..599 -> DataError.Server
        else -> DataError.Unexpected("HTTP ${code()}")
    }
}
