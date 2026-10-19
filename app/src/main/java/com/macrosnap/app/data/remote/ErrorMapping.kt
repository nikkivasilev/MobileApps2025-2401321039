package com.macrosnap.app.data.remote

import com.macrosnap.app.model.DataError
import java.io.IOException
import java.net.HttpURLConnection
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

private const val HTTP_TOO_MANY_REQUESTS = 429
private const val LAST_SERVER_ERROR = 599
private const val MAX_ERROR_DETAIL_CHARS = 300

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
    } catch (ignored: IOException) {
        ""
    }
    return when (code()) {
        // Gemini reports a bad key as 400 INVALID_ARGUMENT rather than 401.
        HttpURLConnection.HTTP_BAD_REQUEST -> if ("API key" in body || "API_KEY" in body) {
            DataError.InvalidApiKey
        } else {
            DataError.Unexpected(
                body.take(MAX_ERROR_DETAIL_CHARS),
            )
        }
        HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> DataError.InvalidApiKey
        HttpURLConnection.HTTP_NOT_FOUND -> DataError.ModelUnavailable
        // Gemini names the exhausted quota, e.g. GenerateRequestsPerDayPerProjectPerModel-FreeTier.
        HTTP_TOO_MANY_REQUESTS -> DataError.RateLimited(daily = "PerDay" in body)
        in HttpURLConnection.HTTP_INTERNAL_ERROR..LAST_SERVER_ERROR -> DataError.Server
        else -> DataError.Unexpected("HTTP ${code()}")
    }
}
