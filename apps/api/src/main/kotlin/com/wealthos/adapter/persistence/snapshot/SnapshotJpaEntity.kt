package com.wealthos.adapter.persistence.snapshot

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "snapshots")
class SnapshotJpaEntity(
    @Id
    val id: UUID,
    @Column(name = "as_of", nullable = false)
    val asOf: Instant,
    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant,
    @Column(name = "base_currency", length = 3)
    val baseCurrency: String?,
    @Column(name = "supersedes_id")
    val supersedesId: UUID?,
    @Column(name = "correction_reason", length = 255)
    val correctionReason: String?,
)
