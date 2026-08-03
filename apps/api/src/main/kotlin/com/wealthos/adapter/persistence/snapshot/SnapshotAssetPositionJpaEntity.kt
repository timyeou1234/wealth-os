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
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "snapshot_asset_positions")
class SnapshotAssetPositionJpaEntity(
    @EmbeddedId
    val id: SnapshotAssetPositionJpaId,
    @Column(nullable = false, length = 255)
    val name: String,
    @Column(name = "asset_type", nullable = false, length = 64)
    val assetType: String,
    @Column(nullable = false, length = 32)
    val liquidity: String,
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
    @Column(name = "applied_original_amount", precision = 38, scale = 6)
    val appliedOriginalAmount: BigDecimal? = null,
    @Column(name = "applied_original_currency", length = 3)
    val appliedOriginalCurrency: String? = null,
    @Column(name = "applied_rate", precision = 38, scale = 12)
    val appliedRate: BigDecimal? = null,
    @Column(name = "applied_rate_date")
    val appliedRateDate: LocalDate? = null,
    @Column(name = "applied_provider", length = 32)
    val appliedProvider: String? = null,
    @Column(name = "applied_rate_type", length = 32)
    val appliedRateType: String? = null,
    @Column(name = "applied_basis", length = 200)
    val appliedBasis: String? = null,
    @Column(name = "applied_rounding_mode", length = 32)
    val appliedRoundingMode: String? = null,
)

@Embeddable
data class SnapshotAssetPositionJpaId(
    @Column(name = "snapshot_id")
    val snapshotId: UUID,
    @Column(name = "asset_id")
    val assetId: UUID,
) : Serializable
