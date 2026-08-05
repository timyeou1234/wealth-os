package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class GetAsset(
    private val assetRepository: AssetRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(id: AssetId): Asset =
        assetRepository.findById(currentUser.get(), id)
            ?: throw AssetNotFoundException(id)
}

class AssetNotFoundException(
    val assetId: AssetId,
) : RuntimeException("Asset $assetId was not found")
