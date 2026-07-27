package com.wealthos.domain.shared

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class Money private constructor(
    val amount: BigDecimal,
    val currency: Currency,
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return of(amount + other.amount, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return of(amount - other.amount, currency)
    }

    operator fun unaryMinus(): Money = of(-amount, currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Money arithmetic requires the same currency: $currency != ${other.currency}"
        }
    }

    companion object {
        fun zero(currency: Currency): Money = of(BigDecimal.ZERO, currency)

        fun of(
            amount: BigDecimal,
            currency: Currency,
        ): Money =
            Money(
                amount = amount.setScale(currency.fractionDigits, RoundingMode.UNNECESSARY),
                currency = currency,
            )

        fun rounded(
            amount: BigDecimal,
            currency: Currency,
            roundingMode: RoundingMode,
        ): Money =
            Money(
                amount = amount.setScale(currency.fractionDigits, roundingMode),
                currency = currency,
            )
    }
}
