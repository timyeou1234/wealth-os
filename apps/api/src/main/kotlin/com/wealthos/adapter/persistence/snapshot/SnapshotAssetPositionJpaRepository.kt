package com.wealthos.adapter.persistence.snapshot

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SnapshotAssetPositionJpaRepository :
    JpaRepository<SnapshotAssetPositionJpaEntity, SnapshotAssetPositionJpaId> {
    fun findAllByIdSnapshotId(snapshotId: UUID): List<SnapshotAssetPositionJpaEntity>
}
