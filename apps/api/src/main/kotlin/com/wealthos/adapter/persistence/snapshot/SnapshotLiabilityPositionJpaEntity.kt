package com.wealthos.adapter.persistence.snapshot

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "snapshot_liability_positions")
class SnapshotLiabilityPositionJpaEntity(
    @EmbeddedId
    val id: SnapshotLiabilityPositionJpaId,
    @Column(nullable = false, length = 255)
    val name: String,
    @Column(nullable = false, precision = 38, scale = 3)
    val amount: BigDecimal,
    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    val currency: String,
    @Column(name = "effective_at", nullable = false)
    val effectiveAt: Instant,
    @Column(nullable = false, length = 100)
    val source: String,
    @Column(name = "conversion_original_amount", precision = 38, scale = 6)
    val conversionOriginalAmount: BigDecimal? = null,
    @Column(name = "conversion_original_currency", columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    val conversionOriginalCurrency: String? = null,
    @Column(name = "conversion_exchange_rate_basis", length = 200)
    val conversionExchangeRateBasis: String? = null,
    @Column(name = "conversion_effective_at")
    val conversionEffectiveAt: Instant? = null,
)

@Embeddable
data class SnapshotLiabilityPositionJpaId(
    @Column(name = "snapshot_id")
    val snapshotId: UUID,
    @Column(name = "liability_id")
    val liabilityId: UUID,
) : Serializable
