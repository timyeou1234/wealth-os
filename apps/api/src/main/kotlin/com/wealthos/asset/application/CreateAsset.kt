package com.wealthos.asset.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class CreateAsset(
    private val assetRepository: AssetRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(
        name: String,
        type: AssetType,
        liquidity: Liquidity,
    ): Asset =
        assetRepository.save(
            currentUser.get(),
            Asset(
                id = AssetId.new(),
                name = name,
                type = type,
                liquidity = liquidity,
            ),
        )
}
