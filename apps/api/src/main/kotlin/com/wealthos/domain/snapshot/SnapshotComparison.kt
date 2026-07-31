package com.wealthos.domain.snapshot

import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money

data class SnapshotComparison(
    val earlierSnapshotId: SnapshotId,
    val laterSnapshotId: SnapshotId,
    val totalAssetsChange: Money,
    val totalLiabilitiesChange: Money,
    val netWorthChange: Money,
)

sealed interface SnapshotComparisonResult {
    data class Compared(
        val comparison: SnapshotComparison,
    ) : SnapshotComparisonResult

    data class InsufficientData(
        val reason: SnapshotComparisonInsufficientReason,
    ) : SnapshotComparisonResult
}

sealed interface SnapshotComparisonInsufficientReason {
    data object EmptySnapshots : SnapshotComparisonInsufficientReason

    data class MixedCurrencies(
        val currencies: Set<Currency>,
    ) : SnapshotComparisonInsufficientReason
}
