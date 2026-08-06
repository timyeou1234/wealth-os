package com.wealthos.liability.domain

import com.wealthos.identity.domain.UserId

interface LiabilityRepository {
    fun save(
        ownerId: UserId,
        liability: Liability,
    ): Liability

    fun findById(
        ownerId: UserId,
        id: LiabilityId,
    ): Liability?

    fun findAll(ownerId: UserId): List<Liability>
}
