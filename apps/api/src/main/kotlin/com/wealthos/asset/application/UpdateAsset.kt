package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class UpdateAsset(
    private val assetRepository: AssetRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(
        id: AssetId,
        name: String,
        type: AssetType,
        liquidity: Liquidity,
    ): Asset {
        val ownerId = currentUser.get()
        val existing = assetRepository.findById(ownerId, id) ?: throw AssetNotFoundException(id)
        return assetRepository.save(ownerId, Asset(id, name, type, liquidity, existing.archived))
    }
}
