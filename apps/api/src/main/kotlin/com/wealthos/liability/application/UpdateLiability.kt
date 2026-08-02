package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class UpdateLiability(
    private val liabilityRepository: LiabilityRepository,
) {
    fun execute(
        id: LiabilityId,
        name: String,
    ): Liability {
        val existing = liabilityRepository.findById(id) ?: throw LiabilityNotFoundException(id)
        return liabilityRepository.save(Liability(id, name, existing.archived))
    }
}
