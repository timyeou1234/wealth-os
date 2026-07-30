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
import kotlin.test.assertTrue

class SnapshotTest {
    private val asOf = Instant.parse("2026-07-27T00:00:00Z")
    private val usd = Currency.of("USD")

    @Test
    fun `captures a complete self contained balance sheet`() {
        val asset = asset()
        val liability = liability()
        val assets = mutableListOf(asset)
        val valuations = mutableListOf(valuation(asset.id))
        val liabilities = mutableListOf(liability)
        val balances = mutableListOf(balance(liability.id))
        val snapshot =
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                assets = assets,
                assetValuations = valuations,
                liabilities = liabilities,
                liabilityBalances = balances,
            )

        assets.clear()
        valuations.clear()
        liabilities.clear()
        balances.clear()

        assertEquals(1, snapshot.assetPositions.size)
        assertEquals(1, snapshot.liabilityPositions.size)
        assertEquals("Cash", snapshot.assetPositions.single().name)
        assertEquals(Liquidity.LIQUID, snapshot.assetPositions.single().liquidity)
        assertEquals("Mortgage", snapshot.liabilityPositions.single().name)
        assertNull(snapshot.supersedes)
    }

    @Test
    fun `rejects missing and unknown asset valuations`() {
        val asset = asset()
        val unknownAssetId = AssetId.new()

        val missing =
            assertFailsWith<IllegalArgumentException> {
                Snapshot.capture(
                    id = SnapshotId.new(),
                    asOf = asOf,
                    assets = listOf(asset),
                )
            }
        val unknown =
            assertFailsWith<IllegalArgumentException> {
                Snapshot.capture(
                    id = SnapshotId.new(),
                    asOf = asOf,
                    assetValuations = listOf(valuation(unknownAssetId)),
                )
            }

        assertTrue(missing.message.orEmpty().contains("missing=[${asset.id}]"))
        assertTrue(unknown.message.orEmpty().contains("unknown=[$unknownAssetId]"))
    }

    @Test
    fun `rejects missing and unknown liability balances`() {
        val liability = liability()
        val unknownLiabilityId = LiabilityId.new()

        val missing =
            assertFailsWith<IllegalArgumentException> {
                Snapshot.capture(
                    id = SnapshotId.new(),
                    asOf = asOf,
                    liabilities = listOf(liability),
                )
            }
        val unknown =
            assertFailsWith<IllegalArgumentException> {
                Snapshot.capture(
                    id = SnapshotId.new(),
                    asOf = asOf,
                    liabilityBalances = listOf(balance(unknownLiabilityId)),
                )
            }

        assertTrue(missing.message.orEmpty().contains("missing=[${liability.id}]"))
        assertTrue(unknown.message.orEmpty().contains("unknown=[$unknownLiabilityId]"))
    }

    @Test
    fun `rejects duplicate position identities and financial facts`() {
        val asset = asset()
        val liability = liability()

        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                assets = listOf(asset, asset),
                assetValuations = listOf(valuation(asset.id)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                assets = listOf(asset),
                assetValuations = listOf(valuation(asset.id), valuation(asset.id)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                liabilities = listOf(liability, liability),
                liabilityBalances = listOf(balance(liability.id)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                liabilities = listOf(liability),
                liabilityBalances = listOf(balance(liability.id), balance(liability.id)),
            )
        }
    }

    @Test
    fun `rejects facts effective after the snapshot`() {
        val future = asOf.plusSeconds(1)
        val asset = asset()
        val liability = liability()

        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                assets = listOf(asset),
                assetValuations = listOf(valuation(asset.id, future)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                liabilities = listOf(liability),
                liabilityBalances = listOf(balance(liability.id, future)),
            )
        }
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

        val assetPosition =
            SnapshotAssetPosition.of(
                assetId = assetId,
                name = "  Cash  ",
                type = AssetType.CASH,
                liquidity = Liquidity.LIQUID,
                valuation = valuation(assetId),
            )
        val liabilityPosition =
            SnapshotLiabilityPosition.of(
                liabilityId = liabilityId,
                name = "  Mortgage  ",
                balance = balance(liabilityId),
            )

        assertEquals("Cash", assetPosition.name)
        assertEquals("Mortgage", liabilityPosition.name)

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
        val asset = asset()
        val replacement = SnapshotAssetPosition.capture(asset, valuation(asset.id))
        val correction =
            Snapshot.correction(
                id = SnapshotId.new(),
                supersedes = original,
                assetPositions = listOf(replacement),
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

    private fun asset(
        id: AssetId = AssetId.new(),
        name: String = "Cash",
        type: AssetType = AssetType.CASH,
        liquidity: Liquidity = Liquidity.LIQUID,
    ): Asset = Asset(id, name, type, liquidity)

    private fun liability(
        id: LiabilityId = LiabilityId.new(),
        name: String = "Mortgage",
    ): Liability = Liability(id, name)

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
