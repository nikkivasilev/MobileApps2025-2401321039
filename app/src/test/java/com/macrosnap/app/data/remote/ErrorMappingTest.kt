package com.macrosnap.app.data.remote

import com.macrosnap.app.model.DataError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException
import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErrorMappingTest {

    private fun http(code: Int, body: String = ""): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody(null)))

    @Test
    fun `io failures are network errors`() {
        assertEquals(DataError.Network, IOException("reset").toDataError())
        assertEquals(DataError.Network, SocketTimeoutException().toDataError())
        assertEquals(DataError.Network, UnknownHostException("generativelanguage.googleapis.com").toDataError())
    }

    @Test
    fun `unreadable responses get a fixed message`() {
        assertEquals(
            DataError.Unexpected("Unreadable response from server"),
            SerializationException("Unexpected JSON token at offset 0").toDataError(),
        )
    }

    @Test
    fun `anything else keeps its message`() {
        assertEquals(DataError.Unexpected("boom"), IllegalStateException("boom").toDataError())
        assertEquals(DataError.Unexpected(null), RuntimeException().toDataError())
    }

    @Test
    fun `bad request naming the API key is an invalid key`() {
        val body = """{"error": {"code": 400, "message": "API key not valid. Please pass a valid API key."}}"""
        assertEquals(DataError.InvalidApiKey, http(400, body).toDataError())
        assertEquals(DataError.InvalidApiKey, http(400, """{"reason": "API_KEY_INVALID"}""").toDataError())
    }

    @Test
    fun `other bad requests carry the body as detail`() {
        val body = """{"error": {"message": "Invalid JSON payload"}}"""
        assertEquals(DataError.Unexpected(body), http(400, body).toDataError())
    }

    @Test
    fun `bad request detail is cut to 300 characters`() {
        val body = "x".repeat(1_000)
        assertEquals(DataError.Unexpected("x".repeat(300)), http(400, body).toDataError())
    }

    @Test
    fun `unauthorized and forbidden are invalid keys`() {
        assertEquals(DataError.InvalidApiKey, http(401).toDataError())
        assertEquals(DataError.InvalidApiKey, http(403).toDataError())
    }

    @Test
    fun `not found means the model is unavailable`() {
        assertEquals(DataError.ModelUnavailable, http(404).toDataError())
    }

    @Test
    fun `rate limit tells daily quota from per-minute`() {
        val daily = """{"quotaId": "GenerateRequestsPerDayPerProjectPerModel-FreeTier"}"""
        val perMinute = """{"quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier"}"""
        assertEquals(DataError.RateLimited(daily = true), http(429, daily).toDataError())
        assertEquals(DataError.RateLimited(daily = false), http(429, perMinute).toDataError())
        assertEquals(DataError.RateLimited(daily = false), http(429).toDataError())
    }

    @Test
    fun `every 5xx is a server error`() {
        listOf(500, 502, 503, 504, 599).forEach { assertEquals("$it", DataError.Server, http(it).toDataError()) }
    }

    @Test
    fun `other status codes report the code`() {
        assertEquals(DataError.Unexpected("HTTP 418"), http(418, "I'm a teapot").toDataError())
        assertEquals(DataError.Unexpected("HTTP 409"), http(409).toDataError())
        assertEquals(DataError.Unexpected("HTTP 600"), http(600).toDataError())
    }

    @Test
    fun `an error body that cannot be read is treated as empty`() {
        val broken = object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = -1
            override fun source(): BufferedSource = throw IOException("stream closed")
        }
        val error = HttpException(Response.error<Any>(429, broken))
        assertEquals(DataError.RateLimited(daily = false), error.toDataError())
        assertEquals(DataError.Unexpected(""), HttpException(Response.error<Any>(400, broken)).toDataError())
    }
}
