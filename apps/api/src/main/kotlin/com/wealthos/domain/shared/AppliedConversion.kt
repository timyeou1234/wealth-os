package com.wealthos.domain.shared

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.ConsistentCopyVisibility

enum class FxRateType {
    REFERENCE_RATE,
    USER_DECLARED,
}

@ConsistentCopyVisibility
data class AppliedConversion private constructor(
    val originalMoney: Money,
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val provider: String,
    val rateType: FxRateType,
    val basis: String?,
    val roundingMode: RoundingMode = RoundingMode.HALF_EVEN,
) {
    init {
        require(rate.signum() > 0 && rate.scale() <= 12) { "Applied rate must be positive with at most 12 decimals" }
        require(provider.isNotBlank()) { "Applied-rate provider must not be blank" }
        require(originalMoney.currency != CanonicalValuationCurrency.TWD) {
            "Applied conversion requires a foreign original currency"
        }
        require(rateType != FxRateType.USER_DECLARED || !basis.isNullOrBlank()) {
            "User-declared rate requires a basis"
        }
    }

    fun toTwdMoney(): Money =
        Money.rounded(
            originalMoney.amount.multiply(rate),
            CanonicalValuationCurrency.TWD,
            RoundingMode.HALF_EVEN,
        )

    companion object {
        fun of(
            originalMoney: Money,
            rate: BigDecimal,
            rateDate: LocalDate,
            provider: String,
            rateType: FxRateType,
            basis: String? = null,
        ) = AppliedConversion(
            originalMoney,
            rate.stripTrailingZeros(),
            rateDate,
            provider.trim(),
            rateType,
            basis?.trim(),
        )
    }
}
