package com.wealthos.fxrate.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "fx_rates",
    uniqueConstraints = [UniqueConstraint(columnNames = ["original_currency", "valuation_currency", "provider", "rate_date"])],
)
class FxRateJpaEntity(
    @Id val id: UUID,
    @Column(name = "original_currency", nullable = false, length = 3) val originalCurrency: String,
    @Column(name = "valuation_currency", nullable = false, length = 3) val valuationCurrency: String,
    @Column(nullable = false, precision = 38, scale = 12) val rate: BigDecimal,
    @Column(name = "rate_date", nullable = false) val rateDate: LocalDate,
    @Column(nullable = false, length = 32) val provider: String,
)
