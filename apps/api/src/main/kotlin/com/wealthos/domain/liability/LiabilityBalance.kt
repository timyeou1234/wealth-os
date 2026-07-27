package com.wealthos.domain.liability

import com.wealthos.domain.shared.Money
import java.math.BigDecimal
import java.time.Instant

data class LiabilityBalance(
    val liabilityId: LiabilityId,
    val balance: Money,
    val effectiveAt: Instant,
) {
    init {
        require(balance.amount >= BigDecimal.ZERO) { "Liability balance must not be negative" }
    }
}
