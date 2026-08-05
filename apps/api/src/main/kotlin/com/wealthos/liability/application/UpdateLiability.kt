package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import com.wealthos.identity.application.CurrentUserIdProvider
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
