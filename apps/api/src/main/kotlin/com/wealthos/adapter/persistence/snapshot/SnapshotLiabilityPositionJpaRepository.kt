package com.wealthos.adapter.persistence.snapshot

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SnapshotLiabilityPositionJpaRepository :
    JpaRepository<SnapshotLiabilityPositionJpaEntity, SnapshotLiabilityPositionJpaId> {
    fun findAllByIdSnapshotId(snapshotId: UUID): List<SnapshotLiabilityPositionJpaEntity>
}
