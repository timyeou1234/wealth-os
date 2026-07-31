package com.wealthos.asset.adapter.persistence

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import org.springframework.stereotype.Repository

@Repository
class JpaAssetRepository(
    private val repository: AssetJpaRepository,
) : AssetRepository {
    override fun save(asset: Asset): Asset = repository.save(asset.toEntity()).toDomain()

    override fun findById(id: AssetId): Asset? = repository.findById(id.value).orElse(null)?.toDomain()

    override fun findAll(): List<Asset> = repository.findAll().map { it.toDomain() }

    private fun Asset.toEntity(): AssetJpaEntity =
        AssetJpaEntity(
            id = id.value,
            name = name,
            assetType = type.name,
            liquidity = liquidity.name,
        )

    private fun AssetJpaEntity.toDomain(): Asset =
        Asset(
            id = AssetId(id),
            name = name,
            type = AssetType.valueOf(assetType),
            liquidity = Liquidity.valueOf(liquidity),
        )
}
