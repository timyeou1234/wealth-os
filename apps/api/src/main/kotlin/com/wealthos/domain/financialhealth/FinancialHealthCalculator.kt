package com.wealthos.domain.financialhealth

import com.wealthos.domain.asset.Liquidity
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.Snapshot

object FinancialHealthCalculator {
    fun calculate(snapshot: Snapshot): FinancialHealthResult {
        val currencies =
            buildSet {
                snapshot.assetPositions.mapTo(this) { it.valuation.value.currency }
                snapshot.liabilityPositions.mapTo(this) { it.balance.balance.currency }
            }

        if (currencies.isEmpty()) {
            return insufficient(InsufficientDataReason.EmptySnapshot)
        }
        if (currencies.size > 1) {
            return insufficient(InsufficientDataReason.MixedCurrencies(currencies))
        }

        val currency = currencies.single()
        val totalAssets = snapshot.assetPositions.sumOfMoney(currency) { it.valuation.value }
        val totalLiabilities = snapshot.liabilityPositions.sumOfMoney(currency) { it.balance.balance }
        val liquidAssets =
            snapshot.assetPositions
                .filter { it.liquidity == Liquidity.LIQUID }
                .sumOfMoney(currency) { it.valuation.value }

        return FinancialHealthResult.Calculated(
            FinancialHealth.calculate(
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                debtRatio = FinancialRatio.divide(totalLiabilities.amount, totalAssets.amount),
                liquidityRatio = FinancialRatio.divide(liquidAssets.amount, totalAssets.amount),
            ),
        )
    }

    private fun insufficient(reason: InsufficientDataReason): FinancialHealthResult =
        FinancialHealthResult.InsufficientData(reason)

    private inline fun <T> Iterable<T>.sumOfMoney(
        currency: Currency,
        amount: (T) -> Money,
    ): Money = fold(Money.zero(currency)) { total, item -> total + amount(item) }
}
