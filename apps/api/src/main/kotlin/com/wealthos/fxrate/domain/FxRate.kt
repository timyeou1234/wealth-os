package com.wealthos.fxrate.domain

import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.CanonicalValuationCurrency
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class FxRate(
    val id: UUID,
    val originalCurrency: Currency,
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val provider: String,
) {
    init {
        require(originalCurrency != CanonicalValuationCurrency.TWD) { "TWD uses the identity rate" }
        require(rate.signum() > 0) { "FX rate must be positive" }
        require(rate.scale() <= 12) { "FX rate supports at most 12 decimal places" }
        require(provider.isNotBlank()) { "FX provider must not be blank" }
    }
}
