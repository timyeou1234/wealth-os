package com.wealthos.asset.domain

import com.wealthos.domain.shared.Money
import com.wealthos.domain.shared.ManualConversion
import java.time.Instant
import java.math.BigDecimal

data class AssetValuation(
    val assetId: AssetId,
    val value: Money,
    val effectiveAt: Instant,
    val source: ValuationSource,
    val manualConversion: ManualConversion? = null,
) {
    init {
        require(value.amount >= BigDecimal.ZERO) { "Asset valuation must not be negative" }
        require(manualConversion?.originalValue?.currency != value.currency) {
            "Manual conversion original currency must differ from valuation currency"
        }
    }
}
