package com.wealthos.domain.financialhealth

import com.wealthos.domain.shared.Currency

sealed interface FinancialHealthResult {
    data class Calculated(
        val financialHealth: FinancialHealth,
    ) : FinancialHealthResult

    data class InsufficientData(
        val reason: InsufficientDataReason,
    ) : FinancialHealthResult
}

sealed interface InsufficientDataReason {
    data object EmptySnapshot : InsufficientDataReason

    data class MixedCurrencies(
        val currencies: Set<Currency>,
    ) : InsufficientDataReason
}
