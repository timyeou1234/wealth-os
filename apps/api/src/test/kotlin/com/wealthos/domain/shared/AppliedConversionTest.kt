package com.wealthos.domain.shared

import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.ValuationSource
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppliedConversionTest {
    @Test
    fun `conversion owns canonical HALF_EVEN TWD valuation and evidence cannot disagree`() {
        val conversion = AppliedConversion.of(
            originalMoney = Money.of(BigDecimal("100.00"), Currency.of("USD")),
            rate = BigDecimal("32.292"),
            rateDate = LocalDate.parse("2026-07-31"),
            provider = "CBC",
            rateType = FxRateType.REFERENCE_RATE,
        )

        assertEquals(Money.of(BigDecimal("3229"), CanonicalValuationCurrency.TWD), conversion.toTwdMoney())
        assertEquals(RoundingMode.HALF_EVEN, conversion.roundingMode)
        assertFailsWith<IllegalArgumentException> {
            AssetValuation(
                AssetId.new(),
                Money.of(BigDecimal("3230"), CanonicalValuationCurrency.TWD),
                Instant.parse("2026-08-03T00:00:00Z"),
                ValuationSource.of("test"),
                appliedConversion = conversion,
            )
        }
    }
}
