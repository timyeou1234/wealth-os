package com.wealthos.liability.application

import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class GetLiability(
    private val liabilityRepository: LiabilityRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(id: LiabilityId): Liability =
        liabilityRepository.findById(currentUser.get(), id)
            ?: throw LiabilityNotFoundException(id)
}

class LiabilityNotFoundException(
    val liabilityId: LiabilityId,
) : RuntimeException("Liability $liabilityId was not found")
