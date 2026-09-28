package com.macrosnap.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DataErrorTest {

    @Test
    fun `only failures that pass on their own keep a photo for later`() {
        val kept = listOf(
            DataError.Server,
            DataError.Network,
            DataError.RateLimited(daily = false),
            DataError.RateLimited(daily = true),
        )
        // Retrying these later can't help: the photo or the setup is the problem.
        val dropped = listOf(
            DataError.NoApiKey,
            DataError.InvalidApiKey,
            DataError.ModelUnavailable,
            DataError.BadImage,
            DataError.NotFood,
            DataError.ProductNotFound,
            DataError.Unexpected("boom"),
        )
        assertEquals(kept.map { true }, kept.map { it.isTemporary })
        assertEquals(dropped.map { false }, dropped.map { it.isTemporary })
    }
}
