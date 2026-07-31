package com.wealthos.domain.liability

interface LiabilityRepository {
    fun save(liability: Liability): Liability

    fun findById(id: LiabilityId): Liability?
}
