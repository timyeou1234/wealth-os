package com.wealthos.fxrate.adapter.persistence

import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.CanonicalValuationCurrency
import com.wealthos.fxrate.domain.FxRate
import com.wealthos.fxrate.domain.FxRateRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class JpaFxRateRepository(
    private val jpa: FxRateJpaRepository,
) : FxRateRepository {
    override fun findExact(originalCurrency: Currency, rateDate: LocalDate, provider: String): FxRate? =
        jpa.findByOriginalCurrencyAndValuationCurrencyAndRateDateAndProvider(
            originalCurrency.code,
            CanonicalValuationCurrency.TWD.code,
            rateDate,
            provider,
        )?.toDomain()

    override fun findLatestOnOrBefore(originalCurrency: Currency, asOf: LocalDate, provider: String): FxRate? =
        jpa.findFirstByOriginalCurrencyAndValuationCurrencyAndProviderAndRateDateLessThanEqualOrderByRateDateDesc(
            originalCurrency.code,
            CanonicalValuationCurrency.TWD.code,
            provider,
            asOf,
        )?.toDomain()

    override fun save(rate: FxRate): FxRate =
        jpa.save(
            FxRateJpaEntity(
                rate.id,
                rate.originalCurrency.code,
                CanonicalValuationCurrency.TWD.code,
                rate.rate,
                rate.rateDate,
                rate.provider,
            ),
        ).toDomain()

    override fun latestRateDate(provider: String): LocalDate? =
        jpa.findFirstByProviderOrderByRateDateDesc(provider)?.rateDate

    override fun latestRateDate(originalCurrency: Currency, provider: String): LocalDate? =
        jpa.findFirstByOriginalCurrencyAndProviderOrderByRateDateDesc(originalCurrency.code, provider)?.rateDate

    private fun FxRateJpaEntity.toDomain() =
        FxRate(id, Currency.of(originalCurrency), rate.stripTrailingZeros(), rateDate, provider)
}
