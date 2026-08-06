package com.wealthos.liability.application

import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class UpdateLiability(
    private val liabilityRepository: LiabilityRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(
        id: LiabilityId,
        name: String,
    ): Liability {
        val ownerId = currentUser.get()
        val existing = liabilityRepository.findById(ownerId, id) ?: throw LiabilityNotFoundException(id)
        return liabilityRepository.save(ownerId, Liability(id, name, existing.archived))
    }
}
