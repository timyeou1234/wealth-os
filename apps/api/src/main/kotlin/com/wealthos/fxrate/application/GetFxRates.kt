package com.wealthos.fxrate.application

import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.CanonicalValuationCurrency
import com.wealthos.fxrate.domain.FxRate
import com.wealthos.fxrate.domain.FxRateRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class GetFxRates(
    private val rates: FxRateRepository,
    private val cache: FxRateReadCache,
) {
    fun execute(asOf: LocalDate, currencies: List<Currency>): FxRatesView {
        val resolved =
            currencies.distinct().mapNotNull { currency ->
                if (currency == CanonicalValuationCurrency.TWD) {
                    ResolvedFxRate(currency, BigDecimal.ONE, asOf, IDENTITY)
                } else {
                    cache.getOrLoad(currency, asOf) { rates.findLatestOnOrBefore(currency, asOf, CBC) }?.let {
                        ResolvedFxRate(it.originalCurrency, it.rate, it.rateDate, it.provider)
                    }
                }
            }
        val resolvedCurrencies = resolved.map(ResolvedFxRate::originalCurrency).toSet()
        return FxRatesView(asOf, resolved, currencies.distinct().filterNot(resolvedCurrencies::contains))
    }

    companion object {
        const val CBC = "CBC"
        const val IDENTITY = "IDENTITY"
    }
}

data class FxRatesView(
    val asOf: LocalDate,
    val rates: List<ResolvedFxRate>,
    val missingCurrencies: List<Currency>,
)

data class ResolvedFxRate(
    val originalCurrency: Currency,
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val provider: String,
)
