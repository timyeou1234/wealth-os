package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import com.wealthos.identity.application.CurrentUserIdProvider
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
