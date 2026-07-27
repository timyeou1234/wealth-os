package com.wealthos.domain.asset

import com.wealthos.domain.shared.Money
import java.time.Instant
import java.math.BigDecimal

data class AssetValuation(
    val assetId: AssetId,
    val value: Money,
    val effectiveAt: Instant,
    val source: ValuationSource,
) {
    init {
        require(value.amount >= BigDecimal.ZERO) { "Asset valuation must not be negative" }
    }
}
