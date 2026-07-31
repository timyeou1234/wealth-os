package com.wealthos.adapter.persistence.snapshot

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SnapshotJpaRepository : JpaRepository<SnapshotJpaEntity, UUID> {
    fun findBySupersedesId(supersedesId: UUID): SnapshotJpaEntity?
}
