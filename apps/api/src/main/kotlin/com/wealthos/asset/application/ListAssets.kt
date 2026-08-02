package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetRepository
import org.springframework.stereotype.Service

@Service
class ListAssets(
    private val assetRepository: AssetRepository,
) {
    fun execute(): List<Asset> = assetRepository.findAll().filterNot { it.archived }.sortedBy { it.name.lowercase() }
}
