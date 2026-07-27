package com.wealthos.domain.financialhealth

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.Liquidity
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.Snapshot

object FinancialHealthCalculator {
    fun calculate(
        snapshot: Snapshot,
        assets: Collection<Asset>,
    ): FinancialHealthResult {
        val assetsById = assets.associateBy(Asset::id)
        require(assetsById.size == assets.size) { "Assets must have unique identities" }

        val valuationsByAssetId = snapshot.assetValuations.associateBy { it.assetId }
        val unknownAssetIds = valuationsByAssetId.keys - assetsById.keys
        if (unknownAssetIds.isNotEmpty()) {
            return insufficient(InsufficientDataReason.UnknownAssetValuations(unknownAssetIds))
        }

        val missingAssetIds = assetsById.keys - valuationsByAssetId.keys
        if (missingAssetIds.isNotEmpty()) {
            return insufficient(InsufficientDataReason.MissingAssetValuations(missingAssetIds))
        }

        val currencies =
            buildSet {
                snapshot.assetValuations.mapTo(this) { it.value.currency }
                snapshot.liabilityBalances.mapTo(this) { it.balance.currency }
            }

        if (currencies.isEmpty()) {
            return insufficient(InsufficientDataReason.EmptySnapshot)
        }
        if (currencies.size > 1) {
            return insufficient(InsufficientDataReason.MixedCurrencies(currencies))
        }

        val currency = currencies.single()
        val totalAssets = snapshot.assetValuations.sumOfMoney(currency) { it.value }
        val totalLiabilities = snapshot.liabilityBalances.sumOfMoney(currency) { it.balance }
        val liquidAssetIds =
            assetsById.values
                .filter { it.liquidity == Liquidity.LIQUID }
                .mapTo(mutableSetOf(), Asset::id)
        val liquidAssets =
            snapshot.assetValuations
                .filter { it.assetId in liquidAssetIds }
                .sumOfMoney(currency) { it.value }

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
