package com.wealthos.adapter.persistence.liability

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import org.springframework.stereotype.Repository

@Repository
class JpaLiabilityRepository(
    private val repository: LiabilityJpaRepository,
) : LiabilityRepository {
    override fun save(liability: Liability): Liability = repository.save(liability.toEntity()).toDomain()

    override fun findById(id: LiabilityId): Liability? = repository.findById(id.value).orElse(null)?.toDomain()

    override fun findAll(): List<Liability> = repository.findAll().map { it.toDomain() }

    private fun Liability.toEntity(): LiabilityJpaEntity =
        LiabilityJpaEntity(
            id = id.value,
            name = name,
        )

    private fun LiabilityJpaEntity.toDomain(): Liability =
        Liability(
            id = LiabilityId(id),
            name = name,
        )
}
