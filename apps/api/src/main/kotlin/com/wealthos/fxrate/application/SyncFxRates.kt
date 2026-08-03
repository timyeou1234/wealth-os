package com.wealthos.fxrate.application

import com.wealthos.domain.shared.Currency
import com.wealthos.fxrate.domain.FxRate
import com.wealthos.fxrate.domain.FxRateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class SyncFxRates(
    private val provider: FxRateProvider,
    private val rates: FxRateRepository,
    private val cache: FxRateReadCache,
) {
    @Transactional
    fun execute(from: LocalDate, to: LocalDate): FxRateSyncResult {
        require(!to.isBefore(from)) { "to must not be before from" }
        var inserted = 0
        var unchanged = 0
        provider.supportedCurrencies()
            .filter { it.code != TWD && !it.endDate.isBefore(from) && !it.startDate.isAfter(to) }
            .forEach { supported ->
                val effectiveFrom = maxOf(from, supported.startDate)
                val effectiveTo = minOf(to, supported.endDate)
                val counts = syncCurrency(supported.code, effectiveFrom, effectiveTo)
                inserted += counts.first
                unchanged += counts.second
            }
        if (inserted > 0) cache.clear()
        return FxRateSyncResult(provider.name, inserted, unchanged, rates.latestRateDate(provider.name))
    }

    @Transactional
    fun catchUp(to: LocalDate): FxRateSyncResult {
        var inserted = 0
        var unchanged = 0
        provider.supportedCurrencies()
            .filter { it.code != TWD && !it.startDate.isAfter(to) }
            .forEach { supported ->
                val currency = Currency.of(supported.code)
                val from = rates.latestRateDate(currency, provider.name)?.plusDays(1) ?: supported.startDate
                val effectiveTo = minOf(to, supported.endDate)
                if (!from.isAfter(effectiveTo)) {
                    val counts = syncCurrency(supported.code, from, effectiveTo)
                    inserted += counts.first
                    unchanged += counts.second
                }
            }
        if (inserted > 0) cache.clear()
        return FxRateSyncResult(provider.name, inserted, unchanged, rates.latestRateDate(provider.name))
    }

    private fun syncCurrency(code: String, from: LocalDate, to: LocalDate): Pair<Int, Int> {
        var inserted = 0
        var unchanged = 0
        provider.fetch(code, from, to)
            .filter { it.originalCurrency == code && !it.rateDate.isBefore(from) && !it.rateDate.isAfter(to) }
            .forEach { provided ->
                val currency = Currency.of(provided.originalCurrency)
                val normalizedRate = provided.rate.stripTrailingZeros()
                val existing = rates.findExact(currency, provided.rateDate, provider.name)
                if (existing?.rate?.compareTo(normalizedRate) == 0) {
                    unchanged += 1
                } else {
                    rates.save(
                        FxRate(
                            id = existing?.id ?: UUID.randomUUID(),
                            originalCurrency = currency,
                            rate = normalizedRate,
                            rateDate = provided.rateDate,
                            provider = provider.name,
                        ),
                    )
                    inserted += 1
                }
            }
        return inserted to unchanged
    }

    private companion object {
        const val TWD = "TWD"
    }
}

data class FxRateSyncResult(
    val provider: String,
    val inserted: Int,
    val unchanged: Int,
    val latestRateDate: LocalDate?,
)
