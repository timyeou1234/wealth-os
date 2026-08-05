package com.wealthos.adapter.persistence.snapshot

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface SnapshotJpaRepository : JpaRepository<SnapshotJpaEntity, UUID> {
    fun findByIdAndOwnerId(
        id: UUID,
        ownerId: UUID,
    ): SnapshotJpaEntity?

    fun findBySupersedesIdAndOwnerId(
        supersedesId: UUID,
        ownerId: UUID,
    ): SnapshotJpaEntity?

    fun findAllByOwnerIdAndSupersedesIdIsNullAndAsOfGreaterThanEqualAndAsOfLessThanOrderByAsOfAsc(
        ownerId: UUID,
        fromInclusive: Instant,
        toExclusive: Instant,
    ): List<SnapshotJpaEntity>

    fun findAllByOwnerIdAndSupersedesIdIsNullOrderByAsOfAsc(ownerId: UUID): List<SnapshotJpaEntity>
}
