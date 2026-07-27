package com.wealthos.domain.asset

import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AssetTest {
    @Test
    fun `entity equality is based on identity`() {
        val id = AssetId(UUID.randomUUID())
        val first = Asset(id, "Cash", AssetType.CASH, Liquidity.LIQUID)
        val renamed = Asset(id, "Emergency cash", AssetType.CASH, Liquidity.LIQUID)

        assertEquals(first, renamed)
    }

    @Test
    fun `rejects a blank name and negative valuation`() {
        assertFailsWith<IllegalArgumentException> {
            Asset(AssetId.new(), " ", AssetType.OTHER, Liquidity.ILLIQUID)
        }

        assertFailsWith<IllegalArgumentException> {
            AssetValuation(
                assetId = AssetId.new(),
                value = Money.of(BigDecimal("-1.00"), Currency.of("USD")),
                effectiveAt = Instant.parse("2026-07-27T00:00:00Z"),
                source = ValuationSource.of("manual"),
            )
        }
    }
}
