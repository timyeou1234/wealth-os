package com.wealthos.asset.domain

import com.wealthos.domain.shared.Money
import com.wealthos.domain.shared.ManualConversion
import com.wealthos.domain.shared.AppliedConversion
import java.time.Instant
import java.math.BigDecimal

data class AssetValuation(
    val assetId: AssetId,
    val value: Money,
    val effectiveAt: Instant,
    val source: ValuationSource,
    val manualConversion: ManualConversion? = null,
    val appliedConversion: AppliedConversion? = null,
) {
    init {
        require(value.amount >= BigDecimal.ZERO) { "Asset valuation must not be negative" }
        require(manualConversion?.originalValue?.currency != value.currency) {
            "Manual conversion original currency must differ from valuation currency"
        }
        require(manualConversion == null || appliedConversion == null) { "Use one conversion provenance model" }
        require(appliedConversion?.originalMoney?.currency != value.currency) {
            "Applied conversion original currency must differ from valuation currency"
        }
        require(appliedConversion == null || value == appliedConversion.toTwdMoney()) {
            "Asset valuation must equal its applied TWD conversion"
        }
    }
}
