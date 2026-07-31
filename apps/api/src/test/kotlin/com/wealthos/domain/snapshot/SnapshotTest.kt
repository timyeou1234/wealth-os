package com.wealthos.domain.snapshot

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
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
    private val recordedAt = asOf.plusSeconds(60)
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
                recordedAt = recordedAt,
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
        assertEquals(recordedAt, snapshot.recordedAt)
        assertNull(snapshot.supersedes)
        assertNull(snapshot.correction)
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
                    recordedAt = recordedAt,
                    assets = listOf(asset),
                )
            }
        val unknown =
            assertFailsWith<IllegalArgumentException> {
                Snapshot.capture(
                    id = SnapshotId.new(),
                    asOf = asOf,
                    recordedAt = recordedAt,
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
                    recordedAt = recordedAt,
                    liabilities = listOf(liability),
                )
            }
        val unknown =
            assertFailsWith<IllegalArgumentException> {
                Snapshot.capture(
                    id = SnapshotId.new(),
                    asOf = asOf,
                    recordedAt = recordedAt,
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
                recordedAt = recordedAt,
                assets = listOf(asset, asset),
                assetValuations = listOf(valuation(asset.id)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                recordedAt = recordedAt,
                assets = listOf(asset),
                assetValuations = listOf(valuation(asset.id), valuation(asset.id)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                recordedAt = recordedAt,
                liabilities = listOf(liability, liability),
                liabilityBalances = listOf(balance(liability.id)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                recordedAt = recordedAt,
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
                recordedAt = recordedAt,
                assets = listOf(asset),
                assetValuations = listOf(valuation(asset.id, future)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                recordedAt = recordedAt,
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
    fun `creates an auditable full replacement correction`() {
        val asset = asset()
        val liability = liability()
        val original =
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                recordedAt = recordedAt,
                assets = listOf(asset),
                assetValuations = listOf(valuation(asset.id)),
                liabilities = listOf(liability),
                liabilityBalances = listOf(balance(liability.id)),
            )
        val replacementAsset = asset(name = "Corrected cash")
        val replacementLiability = liability(name = "Corrected mortgage")
        val replacementAssets =
            mutableListOf(
                SnapshotAssetPosition.capture(
                    replacementAsset,
                    valuation(replacementAsset.id),
                ),
            )
        val replacementLiabilities =
            mutableListOf(
                SnapshotLiabilityPosition.capture(
                    replacementLiability,
                    balance(replacementLiability.id),
                ),
            )
        val correctionRecordedAt = recordedAt.plusSeconds(60)
        val correction =
            Snapshot.correction(
                id = SnapshotId.new(),
                supersedes = original,
                recordedAt = correctionRecordedAt,
                reason = CorrectionReason.of("  Corrected omitted positions  "),
                replacementAssetPositions = replacementAssets,
                replacementLiabilityPositions = replacementLiabilities,
            )

        replacementAssets.clear()
        replacementLiabilities.clear()

        assertEquals(original.id, correction.supersedes)
        assertEquals(original.asOf, correction.asOf)
        assertEquals(correctionRecordedAt, correction.recordedAt)
        assertEquals("Corrected omitted positions", correction.correction?.reason?.value)
        assertEquals("Corrected cash", correction.assetPositions.single().name)
        assertEquals("Corrected mortgage", correction.liabilityPositions.single().name)
        assertEquals("Cash", original.assetPositions.single().name)
        assertEquals("Mortgage", original.liabilityPositions.single().name)
    }

    @Test
    fun `rejects a correction that supersedes itself`() {
        val id = SnapshotId.new()
        val original = Snapshot.capture(id, asOf, recordedAt)

        assertFailsWith<IllegalArgumentException> {
            Snapshot.correction(
                id = id,
                supersedes = original,
                recordedAt = recordedAt,
                reason = CorrectionReason.of("Correction"),
                replacementAssetPositions = emptyList(),
                replacementLiabilityPositions = emptyList(),
            )
        }
    }

    @Test
    fun `reconstitutes an immutable snapshot from persisted facts`() {
        val asset = asset()
        val liability = liability()
        val assetPositions =
            mutableListOf(
                SnapshotAssetPosition.capture(asset, valuation(asset.id)),
            )
        val liabilityPositions =
            mutableListOf(
                SnapshotLiabilityPosition.capture(liability, balance(liability.id)),
            )
        val predecessorId = SnapshotId.new()

        val restored =
            Snapshot.reconstitute(
                id = SnapshotId.new(),
                asOf = asOf,
                recordedAt = recordedAt,
                assetPositions = assetPositions,
                liabilityPositions = liabilityPositions,
                correction =
                    SnapshotCorrection(
                        supersedes = predecessorId,
                        reason = CorrectionReason.of("Corrected persisted facts"),
                    ),
            )

        assetPositions.clear()
        liabilityPositions.clear()

        assertEquals(predecessorId, restored.supersedes)
        assertEquals("Corrected persisted facts", restored.correction?.reason?.value)
        assertEquals("Cash", restored.assetPositions.single().name)
        assertEquals("Mortgage", restored.liabilityPositions.single().name)
    }

    @Test
    fun `rejects blank correction reasons`() {
        assertFailsWith<IllegalArgumentException> {
            CorrectionReason.of(" ")
        }
    }

    @Test
    fun `rejects impossible recording times`() {
        assertFailsWith<IllegalArgumentException> {
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = asOf,
                recordedAt = asOf.minusSeconds(1),
            )
        }

        val original = Snapshot.capture(SnapshotId.new(), asOf, recordedAt)
        assertFailsWith<IllegalArgumentException> {
            Snapshot.correction(
                id = SnapshotId.new(),
                supersedes = original,
                recordedAt = recordedAt.minusSeconds(1),
                reason = CorrectionReason.of("Correction"),
                replacementAssetPositions = emptyList(),
                replacementLiabilityPositions = emptyList(),
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
