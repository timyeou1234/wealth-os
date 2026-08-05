package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class ListAssets(
    private val assetRepository: AssetRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(): List<Asset> =
        assetRepository.findAll(currentUser.get()).filterNot { it.archived }.sortedBy { it.name.lowercase() }
}
