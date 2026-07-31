package com.wealthos.domain.snapshot

import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money

object SnapshotComparator {
    fun compare(
        earlier: Snapshot,
        later: Snapshot,
    ): SnapshotComparisonResult {
        require(!earlier.asOf.isAfter(later.asOf)) {
            "Snapshot comparison requires chronological order"
        }
        val currencies = currenciesIn(earlier) + currenciesIn(later)
        if (currencies.isEmpty()) {
            return SnapshotComparisonResult.InsufficientData(
                SnapshotComparisonInsufficientReason.EmptySnapshots,
            )
        }
        if (currencies.size > 1) {
            return SnapshotComparisonResult.InsufficientData(
                SnapshotComparisonInsufficientReason.MixedCurrencies(currencies),
            )
        }

        val currency = currencies.single()
        val earlierAssets = earlier.assetPositions.sumOfMoney(currency) { it.valuation.value }
        val laterAssets = later.assetPositions.sumOfMoney(currency) { it.valuation.value }
        val earlierLiabilities = earlier.liabilityPositions.sumOfMoney(currency) { it.balance.balance }
        val laterLiabilities = later.liabilityPositions.sumOfMoney(currency) { it.balance.balance }

        return SnapshotComparisonResult.Compared(
            SnapshotComparison(
                earlierSnapshotId = earlier.id,
                laterSnapshotId = later.id,
                totalAssetsChange = laterAssets - earlierAssets,
                totalLiabilitiesChange = laterLiabilities - earlierLiabilities,
                netWorthChange =
                    (laterAssets - laterLiabilities) - (earlierAssets - earlierLiabilities),
            ),
        )
    }

    private fun currenciesIn(snapshot: Snapshot): Set<Currency> =
        buildSet {
            snapshot.assetPositions.mapTo(this) { it.valuation.value.currency }
            snapshot.liabilityPositions.mapTo(this) { it.balance.balance.currency }
        }

    private inline fun <T> Iterable<T>.sumOfMoney(
        currency: Currency,
        amount: (T) -> Money,
    ): Money = fold(Money.zero(currency)) { total, item -> total + amount(item) }
}
