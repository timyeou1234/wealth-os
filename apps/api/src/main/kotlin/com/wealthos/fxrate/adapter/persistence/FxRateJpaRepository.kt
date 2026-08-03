package com.wealthos.fxrate.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface FxRateJpaRepository : JpaRepository<FxRateJpaEntity, UUID> {
    fun findByOriginalCurrencyAndValuationCurrencyAndRateDateAndProvider(
        originalCurrency: String,
        valuationCurrency: String,
        rateDate: LocalDate,
        provider: String,
    ): FxRateJpaEntity?

    fun findFirstByOriginalCurrencyAndValuationCurrencyAndProviderAndRateDateLessThanEqualOrderByRateDateDesc(
        originalCurrency: String,
        valuationCurrency: String,
        provider: String,
        rateDate: LocalDate,
    ): FxRateJpaEntity?

    fun findFirstByProviderOrderByRateDateDesc(provider: String): FxRateJpaEntity?

    fun findFirstByOriginalCurrencyAndProviderOrderByRateDateDesc(
        originalCurrency: String,
        provider: String,
    ): FxRateJpaEntity?
}
