package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import org.springframework.stereotype.Service

@Service
class ArchiveAsset(
    private val assetRepository: AssetRepository,
) {
    fun execute(id: AssetId) {
        val asset = assetRepository.findById(id) ?: throw AssetNotFoundException(id)
        assetRepository.save(Asset(asset.id, asset.name, asset.type, asset.liquidity, archived = true))
    }
}
