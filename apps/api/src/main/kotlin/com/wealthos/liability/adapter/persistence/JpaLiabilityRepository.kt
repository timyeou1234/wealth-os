package com.wealthos.liability.adapter.persistence

import com.wealthos.identity.domain.UserId
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilityRepository
import org.springframework.stereotype.Repository

@Repository
class JpaLiabilityRepository(
    private val jpa: LiabilityJpaRepository,
) : LiabilityRepository {
    override fun save(
        ownerId: UserId,
        liability: Liability,
    ): Liability = jpa.save(liability.toEntity(ownerId)).toDomain()

    override fun findById(
        ownerId: UserId,
        id: LiabilityId,
    ): Liability? = jpa.findByIdAndOwnerId(id.value, ownerId.value)?.toDomain()

    override fun findAll(ownerId: UserId): List<Liability> = jpa.findAllByOwnerId(ownerId.value).map { it.toDomain() }

    private fun Liability.toEntity(ownerId: UserId): LiabilityJpaEntity =
        LiabilityJpaEntity(
            id = id.value,
            ownerId = ownerId.value,
            name = name,
            archived = archived,
        )

    private fun LiabilityJpaEntity.toDomain(): Liability =
        Liability(
            id = LiabilityId(id),
            name = name,
            archived = archived,
        )
}
