package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class CreateLiability(
    private val liabilityRepository: LiabilityRepository,
) {
    fun execute(name: String): Liability =
        liabilityRepository.save(
            Liability(
                id = LiabilityId.new(),
                name = name,
            ),
        )
}
