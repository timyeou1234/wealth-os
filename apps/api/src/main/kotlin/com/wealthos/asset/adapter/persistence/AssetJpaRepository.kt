package com.wealthos.asset.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AssetJpaRepository : JpaRepository<AssetJpaEntity, UUID> {
    fun findByIdAndOwnerId(
        id: UUID,
        ownerId: UUID,
    ): AssetJpaEntity?

    fun findAllByOwnerId(ownerId: UUID): List<AssetJpaEntity>
}
