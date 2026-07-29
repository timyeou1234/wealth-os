package com.wealthos.domain.financialhealth

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.ConsistentCopyVisibility

sealed interface FinancialRatio {
    @ConsistentCopyVisibility
    data class Defined private constructor(
        val value: BigDecimal,
    ) : FinancialRatio {
        init {
            require(value >= BigDecimal.ZERO) { "Financial ratio must not be negative" }
            require(value.scale() == SCALE) { "Financial ratio must use $SCALE decimal places" }
        }

        companion object {
            internal fun divide(
                numerator: BigDecimal,
                denominator: BigDecimal,
            ): Defined =
                Defined(
                    numerator.divide(denominator, SCALE, ROUNDING_MODE),
                )
        }
    }

    data object Undefined : FinancialRatio

    companion object {
        internal const val SCALE = 6
        internal val ROUNDING_MODE: RoundingMode = RoundingMode.HALF_EVEN

        internal fun divide(
            numerator: BigDecimal,
            denominator: BigDecimal,
        ): FinancialRatio =
            if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                Undefined
            } else {
                Defined.divide(numerator, denominator)
            }
    }
}
