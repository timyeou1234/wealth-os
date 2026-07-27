package com.wealthos.domain.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CurrencyTest {
    @Test
    fun `normalizes a supported ISO currency code`() {
        assertEquals("USD", Currency.of(" usd ").code)
    }

    @Test
    fun `rejects an unsupported currency code`() {
        assertFailsWith<IllegalArgumentException> {
            Currency.of("ZZZ")
        }
    }
}
