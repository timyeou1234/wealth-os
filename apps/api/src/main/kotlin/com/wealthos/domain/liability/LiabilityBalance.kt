package com.wealthos.domain.liability

import com.wealthos.domain.shared.Money
import com.wealthos.domain.shared.ManualConversion
import java.math.BigDecimal
import java.time.Instant

data class LiabilityBalance(
    val liabilityId: LiabilityId,
    val balance: Money,
    val effectiveAt: Instant,
    val source: LiabilitySource,
    val manualConversion: ManualConversion? = null,
) {
    init {
        require(balance.amount >= BigDecimal.ZERO) { "Liability balance must not be negative" }
        require(manualConversion?.originalValue?.currency != balance.currency) {
            "Manual conversion original currency must differ from balance currency"
        }
    }
}
