package com.wealthos.domain.financialhealth

import com.wealthos.domain.shared.Money
import java.math.BigDecimal
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class FinancialHealth private constructor(
    val totalAssets: Money,
    val totalLiabilities: Money,
    val netWorth: Money,
    val debtRatio: FinancialRatio,
    val liquidityRatio: FinancialRatio,
) {
    init {
        require(totalAssets.currency == totalLiabilities.currency) {
            "Financial health totals must use the same currency"
        }
        require(totalAssets.currency == netWorth.currency) {
            "Financial health net worth must use the totals currency"
        }
        require(totalAssets.amount >= BigDecimal.ZERO) { "Total assets must not be negative" }
        require(totalLiabilities.amount >= BigDecimal.ZERO) { "Total liabilities must not be negative" }
        require(netWorth == totalAssets - totalLiabilities) {
            "Net worth must equal total assets minus total liabilities"
        }
    }

    companion object {
        internal fun calculate(
            totalAssets: Money,
            totalLiabilities: Money,
            debtRatio: FinancialRatio,
            liquidityRatio: FinancialRatio,
        ): FinancialHealth =
            FinancialHealth(
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                netWorth = totalAssets - totalLiabilities,
                debtRatio = debtRatio,
                liquidityRatio = liquidityRatio,
            )
    }
}
