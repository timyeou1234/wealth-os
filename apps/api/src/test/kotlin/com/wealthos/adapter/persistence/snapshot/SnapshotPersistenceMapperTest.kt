package com.wealthos.adapter.persistence.snapshot

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.AssetId
import com.wealthos.domain.asset.AssetType
import com.wealthos.domain.asset.AssetValuation
import com.wealthos.domain.asset.Liquidity
import com.wealthos.domain.asset.ValuationSource
import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.CorrectionReason
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotAssetPosition
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotLiabilityPosition
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotPersistenceMapperTest {
    @Test
    fun `round trips a corrected snapshot without losing historical facts`() {
        val asOf = Instant.parse("2026-07-31T00:00:00Z")
        val original = Snapshot.capture(SnapshotId.new(), asOf, asOf)
        val asset = Asset(AssetId.new(), "Emergency fund", AssetType.CASH, Liquidity.LIQUID)
        val liability = Liability(LiabilityId.new(), "Mortgage")
        val assetPosition =
            SnapshotAssetPosition.capture(
                asset,
                AssetValuation(
                    assetId = asset.id,
                    value = Money.of(BigDecimal("1200.00"), Currency.of("USD")),
                    effectiveAt = asOf,
                    source = ValuationSource.of("Bank statement"),
                ),
            )
        val liabilityPosition =
            SnapshotLiabilityPosition.capture(
                liability,
                LiabilityBalance(
                    liabilityId = liability.id,
                    balance = Money.of(BigDecimal("800.00"), Currency.of("USD")),
                    effectiveAt = asOf,
                    source = LiabilitySource.of("Loan statement"),
                ),
            )
        val corrected =
            Snapshot.correction(
                id = SnapshotId.new(),
                supersedes = original,
                recordedAt = asOf.plusSeconds(60),
                reason = CorrectionReason.of("Corrected balances"),
                replacementAssetPositions = listOf(assetPosition),
                replacementLiabilityPositions = listOf(liabilityPosition),
            )

        val restored =
            SnapshotPersistenceMapper.domain(
                snapshot = SnapshotPersistenceMapper.snapshotEntity(corrected),
                assets = SnapshotPersistenceMapper.assetEntities(corrected),
                liabilities = SnapshotPersistenceMapper.liabilityEntities(corrected),
            )

        assertEquals(corrected.id, restored.id)
        assertEquals(corrected.asOf, restored.asOf)
        assertEquals(corrected.recordedAt, restored.recordedAt)
        assertEquals(corrected.correction, restored.correction)
        assertEquals(corrected.assetPositions, restored.assetPositions)
        assertEquals(corrected.liabilityPositions, restored.liabilityPositions)
    }
}
