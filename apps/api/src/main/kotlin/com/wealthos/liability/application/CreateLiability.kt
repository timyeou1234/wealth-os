package com.wealthos.liability.application

import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilityRepository
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
