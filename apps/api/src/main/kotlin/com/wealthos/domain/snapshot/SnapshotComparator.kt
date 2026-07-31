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
                assetChanges = assetChanges(earlier, later, currency),
                liabilityChanges = liabilityChanges(earlier, later, currency),
            ),
        )
    }

    private fun currenciesIn(snapshot: Snapshot): Set<Currency> =
        buildSet {
            snapshot.assetPositions.mapTo(this) { it.valuation.value.currency }
            snapshot.liabilityPositions.mapTo(this) { it.balance.balance.currency }
        }

    private fun assetChanges(
        earlier: Snapshot,
        later: Snapshot,
        currency: Currency,
    ): List<AssetPositionChange> {
        val before = earlier.assetPositions.associateBy(SnapshotAssetPosition::assetId)
        val after = later.assetPositions.associateBy(SnapshotAssetPosition::assetId)
        return (before.keys + after.keys)
            .sortedBy { it.value.toString() }
            .mapNotNull { id ->
                val previous = before[id]
                val current = after[id]
                val previousValue = previous?.valuation?.value
                val currentValue = current?.valuation?.value
                if (previousValue == currentValue) {
                    null
                } else {
                    AssetPositionChange(
                        assetId = id,
                        type = changeType(previous, current),
                        previousName = previous?.name,
                        currentName = current?.name,
                        previousValue = previousValue,
                        currentValue = currentValue,
                        previousEffectiveAt = previous?.valuation?.effectiveAt,
                        currentEffectiveAt = current?.valuation?.effectiveAt,
                        valueChange =
                            (currentValue ?: Money.zero(currency)) -
                                (previousValue ?: Money.zero(currency)),
                    )
                }
            }
    }

    private fun liabilityChanges(
        earlier: Snapshot,
        later: Snapshot,
        currency: Currency,
    ): List<LiabilityPositionChange> {
        val before = earlier.liabilityPositions.associateBy(SnapshotLiabilityPosition::liabilityId)
        val after = later.liabilityPositions.associateBy(SnapshotLiabilityPosition::liabilityId)
        return (before.keys + after.keys)
            .sortedBy { it.value.toString() }
            .mapNotNull { id ->
                val previous = before[id]
                val current = after[id]
                val previousBalance = previous?.balance?.balance
                val currentBalance = current?.balance?.balance
                if (previousBalance == currentBalance) {
                    null
                } else {
                    LiabilityPositionChange(
                        liabilityId = id,
                        type = changeType(previous, current),
                        previousName = previous?.name,
                        currentName = current?.name,
                        previousBalance = previousBalance,
                        currentBalance = currentBalance,
                        previousEffectiveAt = previous?.balance?.effectiveAt,
                        currentEffectiveAt = current?.balance?.effectiveAt,
                        balanceChange =
                            (currentBalance ?: Money.zero(currency)) -
                                (previousBalance ?: Money.zero(currency)),
                    )
                }
            }
    }

    private fun changeType(
        previous: Any?,
        current: Any?,
    ): PositionChangeType =
        when {
            previous == null -> PositionChangeType.ADDED
            current == null -> PositionChangeType.REMOVED
            else -> PositionChangeType.CHANGED
        }

    private inline fun <T> Iterable<T>.sumOfMoney(
        currency: Currency,
        amount: (T) -> Money,
    ): Money = fold(Money.zero(currency)) { total, item -> total + amount(item) }
}
