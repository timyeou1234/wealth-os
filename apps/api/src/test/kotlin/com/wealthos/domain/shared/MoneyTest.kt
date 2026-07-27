package com.wealthos.domain.shared

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    private val usd = Currency.of("USD")
    private val twd = Currency.of("TWD")

    @Test
    fun `normalizes amount to the currency scale`() {
        assertEquals(BigDecimal("12.00"), Money.of(BigDecimal("12"), usd).amount)
    }

    @Test
    fun `requires explicit rounding when precision would be lost`() {
        assertFailsWith<ArithmeticException> {
            Money.of(BigDecimal("12.345"), usd)
        }

        assertEquals(
            BigDecimal("12.34"),
            Money.rounded(BigDecimal("12.345"), usd, RoundingMode.HALF_EVEN).amount,
        )
    }

    @Test
    fun `supports arithmetic within one currency`() {
        val left = Money.of(BigDecimal("12.50"), usd)
        val right = Money.of(BigDecimal("2.25"), usd)

        assertEquals(Money.of(BigDecimal("14.75"), usd), left + right)
        assertEquals(Money.of(BigDecimal("10.25"), usd), left - right)
    }

    @Test
    fun `rejects implicit cross-currency arithmetic and comparison`() {
        val dollars = Money.of(BigDecimal("1.00"), usd)
        val dollarsInTwd = Money.of(BigDecimal("1"), twd)

        assertFailsWith<IllegalArgumentException> { dollars + dollarsInTwd }
        assertFailsWith<IllegalArgumentException> { dollars.compareTo(dollarsInTwd) }
    }
}
