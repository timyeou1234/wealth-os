package com.wealthos.liability.application

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityRepository
import org.springframework.stereotype.Service

@Service
class ListLiabilities(
    private val liabilityRepository: LiabilityRepository,
) {
    fun execute(): List<Liability> = liabilityRepository.findAll().sortedBy { it.name.lowercase() }
}
