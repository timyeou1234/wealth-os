package com.wealthos.fxrate.application

import com.wealthos.domain.shared.Currency
import com.wealthos.fxrate.domain.FxRate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Component
class FxRateReadCache {
    private val entries = ConcurrentHashMap<Key, FxRate?>()

    fun getOrLoad(currency: Currency, asOf: LocalDate, loader: () -> FxRate?): FxRate? =
        entries.computeIfAbsent(Key(currency, asOf)) { loader() }

    fun clear() = entries.clear()

    private data class Key(val currency: Currency, val asOf: LocalDate)
}
