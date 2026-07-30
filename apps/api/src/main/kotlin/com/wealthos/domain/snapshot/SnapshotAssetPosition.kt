package com.wealthos.domain.snapshot

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.AssetId
import com.wealthos.domain.asset.AssetType
import com.wealthos.domain.asset.AssetValuation
import com.wealthos.domain.asset.Liquidity
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
