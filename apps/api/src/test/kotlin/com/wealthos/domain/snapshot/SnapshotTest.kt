package com.wealthos.domain.snapshot

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
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SnapshotTest {
    private val asOf = Instant.parse("2026-07-27T00:00:00Z")
    private val usd = Currency.of("USD")

    @Test
    fun `defensively copies its captured positions`() {
        val assets = mutableListOf(assetPosition())
        val liabilities = mutableListOf(liabilityPosition())
        val snapshot = Snapshot.capture(SnapshotId.new(), asOf, assets, liabilities)

        assets.clear()
        liabilities.clear()

        assertEquals(1, snapshot.assetPositions.size)
        assertEquals(1, snapshot.liabilityPositions.size)
        assertNull(snapshot.supersedes)
    }

    @Test
    fun `rejects facts effective after the snapshot`() {
        val future = asOf.plusSeconds(1)

        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                assetPositions = listOf(assetPosition(effectiveAt = future)),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                liabilityPositions = listOf(liabilityPosition(effectiveAt = future)),
            )
        }
    }

    @Test
    fun `rejects duplicate positions for the same identity`() {
        val asset = assetPosition()
        val liability = liabilityPosition()

        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                assetPositions = listOf(asset, asset),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                liabilityPositions = listOf(liability, liability),
            )
        }
    }

    @Test
    fun `captures point in time display and liquidity metadata`() {
        val asset = Asset(AssetId.new(), "Cash", AssetType.CASH, Liquidity.LIQUID)
        val liability = Liability(LiabilityId.new(), "Mortgage")
        val assetPosition =
            SnapshotAssetPosition.capture(asset, valuation(asset.id))
        val liabilityPosition =
            SnapshotLiabilityPosition.capture(liability, balance(liability.id))

        assertEquals("Cash", assetPosition.name)
        assertEquals(AssetType.CASH, assetPosition.type)
        assertEquals(Liquidity.LIQUID, assetPosition.liquidity)
        assertEquals("Mortgage", liabilityPosition.name)
    }

    @Test
    fun `rejects line items whose identities do not match their facts`() {
        assertFailsWith<IllegalArgumentException> {
            SnapshotAssetPosition.of(
                assetId = AssetId.new(),
                name = "Cash",
                type = AssetType.CASH,
                liquidity = Liquidity.LIQUID,
                valuation = valuation(AssetId.new()),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            SnapshotLiabilityPosition.of(
                liabilityId = LiabilityId.new(),
                name = "Mortgage",
                balance = balance(LiabilityId.new()),
            )
        }
    }

    @Test
    fun `normalizes and validates captured names`() {
        val assetId = AssetId.new()
        val liabilityId = LiabilityId.new()

        val asset =
            SnapshotAssetPosition.of(
                assetId = assetId,
                name = "  Cash  ",
                type = AssetType.CASH,
                liquidity = Liquidity.LIQUID,
                valuation = valuation(assetId),
            )
        val liability =
            SnapshotLiabilityPosition.of(
                liabilityId = liabilityId,
                name = "  Mortgage  ",
                balance = balance(liabilityId),
            )

        assertEquals("Cash", asset.name)
        assertEquals("Mortgage", liability.name)

        assertFailsWith<IllegalArgumentException> {
            SnapshotAssetPosition.of(
                assetId = assetId,
                name = " ",
                type = AssetType.CASH,
                liquidity = Liquidity.LIQUID,
                valuation = valuation(assetId),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SnapshotLiabilityPosition.of(
                liabilityId = liabilityId,
                name = " ",
                balance = balance(liabilityId),
            )
        }
    }

    @Test
    fun `creates an immutable correction that supersedes a prior snapshot`() {
        val original = Snapshot.capture(SnapshotId.new(), asOf)
        val correction =
            Snapshot.correction(
                id = SnapshotId.new(),
                supersedes = original,
                assetPositions = listOf(assetPosition()),
            )

        assertEquals(original.id, correction.supersedes)
        assertEquals(original.asOf, correction.asOf)
    }

    @Test
    fun `rejects a correction that supersedes itself`() {
        val id = SnapshotId.new()
        val original = Snapshot.capture(id, asOf)

        assertFailsWith<IllegalArgumentException> {
            Snapshot.correction(
                id = id,
                supersedes = original,
            )
        }
    }

    private fun assetPosition(
        assetId: AssetId = AssetId.new(),
        effectiveAt: Instant = asOf,
    ): SnapshotAssetPosition {
        val asset = Asset(assetId, "Cash", AssetType.CASH, Liquidity.LIQUID)
        return SnapshotAssetPosition.capture(asset, valuation(assetId, effectiveAt))
    }

    private fun liabilityPosition(
        liabilityId: LiabilityId = LiabilityId.new(),
        effectiveAt: Instant = asOf,
    ): SnapshotLiabilityPosition {
        val liability = Liability(liabilityId, "Mortgage")
        return SnapshotLiabilityPosition.capture(liability, balance(liabilityId, effectiveAt))
    }

    private fun valuation(
        assetId: AssetId,
        effectiveAt: Instant = asOf,
    ): AssetValuation =
        AssetValuation(
            assetId = assetId,
            value = Money.of(BigDecimal("100.00"), usd),
            effectiveAt = effectiveAt,
            source = ValuationSource.of("manual"),
        )

    private fun balance(
        liabilityId: LiabilityId,
        effectiveAt: Instant = asOf,
    ): LiabilityBalance =
        LiabilityBalance(
            liabilityId = liabilityId,
            balance = Money.of(BigDecimal("50.00"), usd),
            effectiveAt = effectiveAt,
            source = LiabilitySource.of("manual"),
        )
}
