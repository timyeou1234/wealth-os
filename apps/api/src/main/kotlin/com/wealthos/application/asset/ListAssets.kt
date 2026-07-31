package com.wealthos.application.asset

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.AssetRepository
import org.springframework.stereotype.Service

@Service
class ListAssets(
    private val assetRepository: AssetRepository,
) {
    fun execute(): List<Asset> = assetRepository.findAll().sortedBy { it.name.lowercase() }
}
