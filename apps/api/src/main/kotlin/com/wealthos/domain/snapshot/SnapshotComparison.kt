package com.wealthos.domain.snapshot

import com.wealthos.domain.asset.AssetId
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import java.time.Instant

data class SnapshotComparison(
    val earlierSnapshotId: SnapshotId,
    val laterSnapshotId: SnapshotId,
    val totalAssetsChange: Money,
    val totalLiabilitiesChange: Money,
    val netWorthChange: Money,
    val assetChanges: List<AssetPositionChange>,
    val liabilityChanges: List<LiabilityPositionChange>,
)

enum class PositionChangeType {
    ADDED,
    REMOVED,
    CHANGED,
}

data class AssetPositionChange(
    val assetId: AssetId,
    val type: PositionChangeType,
    val previousName: String?,
    val currentName: String?,
    val previousValue: Money?,
    val currentValue: Money?,
    val previousEffectiveAt: Instant?,
    val currentEffectiveAt: Instant?,
    val valueChange: Money,
)

data class LiabilityPositionChange(
    val liabilityId: LiabilityId,
    val type: PositionChangeType,
    val previousName: String?,
    val currentName: String?,
    val previousBalance: Money?,
    val currentBalance: Money?,
    val previousEffectiveAt: Instant?,
    val currentEffectiveAt: Instant?,
    val balanceChange: Money,
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
