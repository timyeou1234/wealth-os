package com.wealthos.fxrate.domain

import com.wealthos.domain.shared.Currency
import java.time.LocalDate

interface FxRateRepository {
    fun findExact(originalCurrency: Currency, rateDate: LocalDate, provider: String): FxRate?

    fun findLatestOnOrBefore(originalCurrency: Currency, asOf: LocalDate, provider: String): FxRate?

    fun save(rate: FxRate): FxRate

    fun latestRateDate(originalCurrency: Currency, provider: String): LocalDate?

    fun latestRateDate(provider: String): LocalDate?
}
