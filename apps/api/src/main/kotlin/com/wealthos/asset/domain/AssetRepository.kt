package com.wealthos.asset.domain

import com.wealthos.identity.domain.UserId

interface AssetRepository {
    fun save(
        ownerId: UserId,
        asset: Asset,
    ): Asset

    fun findById(
        ownerId: UserId,
        id: AssetId,
    ): Asset?

    fun findAll(ownerId: UserId): List<Asset>
}
