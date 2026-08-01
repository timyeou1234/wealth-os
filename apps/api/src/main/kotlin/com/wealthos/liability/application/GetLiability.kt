package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class GetLiability(
    private val liabilityRepository: LiabilityRepository,
) {
    fun execute(id: LiabilityId): Liability =
        liabilityRepository.findById(id)
            ?: throw LiabilityNotFoundException(id)
}

class LiabilityNotFoundException(
    val liabilityId: LiabilityId,
) : RuntimeException("Liability $liabilityId was not found")
