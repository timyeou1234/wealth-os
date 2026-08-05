package com.wealthos.adapter.persistence.liability

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import com.wealthos.identity.domain.UserId
import org.springframework.stereotype.Repository

@Repository
class JpaLiabilityRepository(
    private val repository: LiabilityJpaRepository,
) : LiabilityRepository {
    override fun save(
        ownerId: UserId,
        liability: Liability,
    ): Liability = repository.save(liability.toEntity(ownerId)).toDomain()

    override fun findById(
        ownerId: UserId,
        id: LiabilityId,
    ): Liability? = repository.findByIdAndOwnerId(id.value, ownerId.value)?.toDomain()

    override fun findAll(ownerId: UserId): List<Liability> =
        repository.findAllByOwnerId(ownerId.value).map { it.toDomain() }

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
