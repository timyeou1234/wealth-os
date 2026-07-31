package com.wealthos.domain.snapshot

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class SnapshotAssetPosition private constructor(
    val assetId: AssetId,
    val name: String,
    val type: AssetType,
    val liquidity: Liquidity,
    val valuation: AssetValuation,
) {
    init {
        require(name.isNotEmpty()) { "Snapshot asset name must not be blank" }
        require(assetId == valuation.assetId) {
            "Snapshot asset identity must match its valuation identity"
        }
    }

    companion object {
        fun capture(
            asset: Asset,
            valuation: AssetValuation,
        ): SnapshotAssetPosition =
            of(
                assetId = asset.id,
                name = asset.name,
                type = asset.type,
                liquidity = asset.liquidity,
                valuation = valuation,
            )

        fun of(
            assetId: AssetId,
            name: String,
            type: AssetType,
            liquidity: Liquidity,
            valuation: AssetValuation,
        ): SnapshotAssetPosition =
            SnapshotAssetPosition(
                assetId = assetId,
                name = name.trim(),
                type = type,
                liquidity = liquidity,
                valuation = valuation,
            )
    }
}
