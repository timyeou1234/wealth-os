package com.wealthos.fxrate.application

import java.math.BigDecimal
import java.time.LocalDate

interface FxRateProvider {
    val name: String

    fun supportedCurrencies(): List<SupportedFxCurrency>

    fun fetch(originalCurrency: String, from: LocalDate, to: LocalDate): List<ProvidedFxRate>
}

data class SupportedFxCurrency(
    val code: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class ProvidedFxRate(
    val originalCurrency: String,
    val rate: BigDecimal,
    val rateDate: LocalDate,
)
