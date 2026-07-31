package com.wealthos.domain.asset

interface AssetRepository {
    fun save(asset: Asset): Asset

    fun findById(id: AssetId): Asset?

    fun findAll(): List<Asset>
}
