package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import org.springframework.stereotype.Service

@Service
class GetAsset(
    private val assetRepository: AssetRepository,
) {
    fun execute(id: AssetId): Asset =
        assetRepository.findById(id)
            ?: throw AssetNotFoundException(id)
}

class AssetNotFoundException(
    val assetId: AssetId,
) : RuntimeException("Asset $assetId was not found")
