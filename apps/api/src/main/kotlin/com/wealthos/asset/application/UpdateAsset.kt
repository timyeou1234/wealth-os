package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import org.springframework.stereotype.Service

@Service
class UpdateAsset(
    private val assetRepository: AssetRepository,
) {
    fun execute(
        id: AssetId,
        name: String,
        type: AssetType,
        liquidity: Liquidity,
    ): Asset {
        val existing = assetRepository.findById(id) ?: throw AssetNotFoundException(id)
        return assetRepository.save(Asset(id, name, type, liquidity, existing.archived))
    }
}
