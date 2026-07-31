package com.wealthos.asset.domain

interface AssetRepository {
    fun save(asset: Asset): Asset

    fun findById(id: AssetId): Asset?

    fun findAll(): List<Asset>
}
