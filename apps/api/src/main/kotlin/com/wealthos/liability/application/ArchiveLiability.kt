package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class ArchiveLiability(
    private val liabilityRepository: LiabilityRepository,
) {
    fun execute(id: LiabilityId) {
        val liability = liabilityRepository.findById(id) ?: throw LiabilityNotFoundException(id)
        liabilityRepository.save(Liability(liability.id, liability.name, archived = true))
    }
}
