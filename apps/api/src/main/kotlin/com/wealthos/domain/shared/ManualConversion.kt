package com.wealthos.domain.shared

import java.math.BigDecimal
import java.time.Instant
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class ManualConversion private constructor(
    val originalValue: Money,
    val exchangeRateBasis: String,
    val effectiveAt: Instant,
) {
    init {
        require(originalValue.amount >= BigDecimal.ZERO) { "Original converted value must not be negative" }
        require(exchangeRateBasis.isNotBlank()) { "Exchange-rate basis must not be blank" }
        require(exchangeRateBasis.length <= 200) { "Exchange-rate basis must not exceed 200 characters" }
    }

    companion object {
        fun of(
            originalValue: Money,
            exchangeRateBasis: String,
            effectiveAt: Instant,
        ): ManualConversion =
            ManualConversion(
                originalValue = originalValue,
                exchangeRateBasis = exchangeRateBasis.trim(),
                effectiveAt = effectiveAt,
            )
    }
}
