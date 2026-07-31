package com.wealthos.domain.snapshot

import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SnapshotComparatorTest {
    private val earlierTime = Instant.parse("2026-06-30T00:00:00Z")
    private val laterTime = Instant.parse("2026-07-31T00:00:00Z")
    private val usd = Currency.of("USD")

    @Test
    fun `compares changes in assets liabilities and net worth`() {
        val changedAssetId = AssetId.new()
        val removedAssetId = AssetId.new()
        val unchangedAssetId = AssetId.new()
        val changedLiabilityId = LiabilityId.new()
        val earlier =
            snapshot(
                earlierTime,
                assets =
                    listOf(
                        asset(changedAssetId, "Cash", "100.00", earlierTime),
                        asset(removedAssetId, "Old investment", "50.00", earlierTime),
                        asset(unchangedAssetId, "Home", "25.00", earlierTime),
                    ),
                liabilities = listOf(liability(changedLiabilityId, "Mortgage", "40.00", earlierTime)),
            )
        val later =
            snapshot(
                laterTime,
                assets =
                    listOf(
                        asset(changedAssetId, "Cash", "120.00", laterTime),
                        asset(AssetId.new(), "New investment", "30.00", laterTime),
                        asset(unchangedAssetId, "Home", "25.00", laterTime),
                    ),
                liabilities =
                    listOf(
                        liability(changedLiabilityId, "Mortgage", "50.00", laterTime),
                        liability(LiabilityId.new(), "New loan", "10.00", laterTime),
                    ),
            )

        val comparison = compared(SnapshotComparator.compare(earlier, later))

        assertEquals(money("0.00"), comparison.totalAssetsChange)
        assertEquals(money("20.00"), comparison.totalLiabilitiesChange)
        assertEquals(money("-20.00"), comparison.netWorthChange)
        assertEquals(
            setOf(PositionChangeType.ADDED, PositionChangeType.REMOVED, PositionChangeType.CHANGED),
            comparison.assetChanges.map(AssetPositionChange::type).toSet(),
        )
        assertEquals(
            setOf(PositionChangeType.ADDED, PositionChangeType.CHANGED),
            comparison.liabilityChanges.map(LiabilityPositionChange::type).toSet(),
        )
        assertEquals(
            money("20.00"),
            comparison.assetChanges.single { it.assetId == changedAssetId }.valueChange,
        )
        assertEquals(
            earlierTime,
            comparison.assetChanges.single { it.assetId == changedAssetId }.previousEffectiveAt,
        )
        assertEquals(
            laterTime,
            comparison.assetChanges.single { it.assetId == changedAssetId }.currentEffectiveAt,
        )
        assertEquals(
            money("-50.00"),
            comparison.assetChanges.single { it.assetId == removedAssetId }.valueChange,
        )
        assertEquals(false, comparison.assetChanges.any { it.assetId == unchangedAssetId })
    }

    @Test
    fun `returns insufficient data instead of combining currencies or currencyless snapshots`() {
        val emptyResult = SnapshotComparator.compare(snapshot(earlierTime), snapshot(laterTime))
        assertEquals(
            SnapshotComparisonInsufficientReason.EmptySnapshots,
            assertIs<SnapshotComparisonResult.InsufficientData>(emptyResult).reason,
        )

        val eur = Currency.of("EUR")
        val mixedResult =
            SnapshotComparator.compare(
                snapshot(earlierTime, assets = listOf(asset(AssetId.new(), "Cash", "10.00", earlierTime))),
                snapshot(
                    laterTime,
                    assets = listOf(asset(AssetId.new(), "Euro cash", "10.00", laterTime, eur)),
                ),
            )
        assertEquals(
            SnapshotComparisonInsufficientReason.MixedCurrencies(setOf(usd, eur)),
            assertIs<SnapshotComparisonResult.InsufficientData>(mixedResult).reason,
        )
    }

    @Test
    fun `rejects snapshots supplied out of chronological order`() {
        assertFailsWith<IllegalArgumentException> {
            SnapshotComparator.compare(snapshot(laterTime), snapshot(earlierTime))
        }
    }

    private fun snapshot(
        asOf: Instant,
        assets: List<SnapshotAssetPosition> = emptyList(),
        liabilities: List<SnapshotLiabilityPosition> = emptyList(),
    ): Snapshot =
        Snapshot.reconstitute(
            id = SnapshotId.new(),
            asOf = asOf,
            recordedAt = asOf,
            assetPositions = assets,
            liabilityPositions = liabilities,
            correction = null,
        )

    private fun asset(
        id: AssetId,
        name: String,
        amount: String,
        effectiveAt: Instant,
        currency: Currency = usd,
    ): SnapshotAssetPosition =
        SnapshotAssetPosition.of(
            assetId = id,
            name = name,
            type = AssetType.OTHER,
            liquidity = Liquidity.LIQUID,
            valuation =
                AssetValuation(
                    assetId = id,
                    value = money(amount, currency),
                    effectiveAt = effectiveAt,
                    source = ValuationSource.of("manual"),
                ),
        )

    private fun liability(
        id: LiabilityId,
        name: String,
        amount: String,
        effectiveAt: Instant,
    ): SnapshotLiabilityPosition =
        SnapshotLiabilityPosition.of(
            liabilityId = id,
            name = name,
            balance =
                LiabilityBalance(
                    liabilityId = id,
                    balance = money(amount),
                    effectiveAt = effectiveAt,
                    source = LiabilitySource.of("manual"),
                ),
        )

    private fun money(
        amount: String,
        currency: Currency = usd,
    ): Money = Money.of(BigDecimal(amount), currency)

    private fun compared(result: SnapshotComparisonResult): SnapshotComparison =
        assertIs<SnapshotComparisonResult.Compared>(result).comparison
}
