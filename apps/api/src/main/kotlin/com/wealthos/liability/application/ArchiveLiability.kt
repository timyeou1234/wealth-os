package com.wealthos.liability.application

import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class ArchiveLiability(
    private val liabilityRepository: LiabilityRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(id: LiabilityId) {
        val ownerId = currentUser.get()
        val liability = liabilityRepository.findById(ownerId, id) ?: throw LiabilityNotFoundException(id)
        liabilityRepository.save(ownerId, Liability(liability.id, liability.name, archived = true))
    }
}
