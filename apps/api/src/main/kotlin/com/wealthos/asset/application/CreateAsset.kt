package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import org.springframework.stereotype.Service

@Service
class CreateAsset(
    private val assetRepository: AssetRepository,
) {
    fun execute(
        name: String,
        type: AssetType,
        liquidity: Liquidity,
    ): Asset =
        assetRepository.save(
            Asset(
                id = AssetId.new(),
                name = name,
                type = type,
                liquidity = liquidity,
            ),
        )
}
