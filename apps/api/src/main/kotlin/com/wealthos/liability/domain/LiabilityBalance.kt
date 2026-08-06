package com.wealthos.liability.domain

import com.wealthos.domain.shared.AppliedConversion
import com.wealthos.domain.shared.ManualConversion
import com.wealthos.domain.shared.Money
import java.math.BigDecimal
import java.time.Instant

data class LiabilityBalance(
    val liabilityId: LiabilityId,
    val balance: Money,
    val effectiveAt: Instant,
    val source: LiabilitySource,
    val manualConversion: ManualConversion? = null,
    val appliedConversion: AppliedConversion? = null,
) {
    init {
        require(balance.amount >= BigDecimal.ZERO) { "Liability balance must not be negative" }
        require(manualConversion?.originalValue?.currency != balance.currency) {
            "Manual conversion original currency must differ from balance currency"
        }
        require(manualConversion == null || appliedConversion == null) { "Use one conversion provenance model" }
        require(appliedConversion?.originalMoney?.currency != balance.currency) {
            "Applied conversion original currency must differ from balance currency"
        }
        require(appliedConversion == null || balance == appliedConversion.toTwdMoney()) {
            "Liability balance must equal its applied TWD conversion"
        }
    }
}
