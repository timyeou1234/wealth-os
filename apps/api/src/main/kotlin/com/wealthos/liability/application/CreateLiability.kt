package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class CreateLiability(
    private val liabilityRepository: LiabilityRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(name: String): Liability =
        liabilityRepository.save(
            currentUser.get(),
            Liability(
                id = LiabilityId.new(),
                name = name,
            ),
        )
}
