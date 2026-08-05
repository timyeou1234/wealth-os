package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class ArchiveAsset(
    private val assetRepository: AssetRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(id: AssetId) {
        val ownerId = currentUser.get()
        val asset = assetRepository.findById(ownerId, id) ?: throw AssetNotFoundException(id)
        assetRepository.save(ownerId, Asset(asset.id, asset.name, asset.type, asset.liquidity, archived = true))
    }
}
