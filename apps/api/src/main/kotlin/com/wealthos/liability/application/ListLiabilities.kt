package com.wealthos.liability.application

import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class ListLiabilities(
    private val liabilityRepository: LiabilityRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(): List<Liability> =
        liabilityRepository.findAll(currentUser.get()).filterNot { it.archived }.sortedBy { it.name.lowercase() }
}
