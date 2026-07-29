package com.wealthos.domain.snapshot

import com.wealthos.domain.asset.AssetId
import com.wealthos.domain.asset.AssetValuation
import com.wealthos.domain.asset.ValuationSource
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

class SnapshotTest {
    private val asOf = Instant.parse("2026-07-27T00:00:00Z")
    private val usd = Currency.of("USD")

    @Test
    fun `defensively copies its financial facts`() {
        val valuations = mutableListOf(valuation(AssetId.new()))
        val balances = mutableListOf(balance(LiabilityId.new()))
        val snapshot = Snapshot.create(SnapshotId.new(), asOf, valuations, balances)

        valuations.clear()
        balances.clear()

        assertEquals(1, snapshot.assetValuations.size)
        assertEquals(1, snapshot.liabilityBalances.size)
    }

    @Test
    fun `rejects facts effective after the snapshot`() {
        val future = asOf.plusSeconds(1)

        assertFailsWith<IllegalArgumentException> {
            Snapshot.create(
                id = SnapshotId.new(),
                asOf = asOf,
                assetValuations = listOf(valuation(AssetId.new(), future)),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Snapshot.create(
                id = SnapshotId.new(),
                asOf = asOf,
                liabilityBalances = listOf(balance(LiabilityId.new(), future)),
            )
        }
    }

    @Test
    fun `rejects duplicate facts for the same position`() {
        val assetId = AssetId.new()
        val liabilityId = LiabilityId.new()

        assertFailsWith<IllegalArgumentException> {
            Snapshot.create(
                id = SnapshotId.new(),
                asOf = asOf,
                assetValuations = listOf(valuation(assetId), valuation(assetId)),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Snapshot.create(
                id = SnapshotId.new(),
                asOf = asOf,
                liabilityBalances = listOf(balance(liabilityId), balance(liabilityId)),
            )
        }
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
