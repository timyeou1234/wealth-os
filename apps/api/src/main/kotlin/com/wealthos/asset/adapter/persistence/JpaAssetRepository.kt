package com.wealthos.asset.adapter.persistence

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.identity.domain.UserId
import org.springframework.stereotype.Repository

@Repository
class JpaAssetRepository(
    private val repository: AssetJpaRepository,
) : AssetRepository {
    override fun save(
        ownerId: UserId,
        asset: Asset,
    ): Asset = repository.save(asset.toEntity(ownerId)).toDomain()

    override fun findById(
        ownerId: UserId,
        id: AssetId,
    ): Asset? = repository.findByIdAndOwnerId(id.value, ownerId.value)?.toDomain()

    override fun findAll(ownerId: UserId): List<Asset> =
        repository.findAllByOwnerId(ownerId.value).map { it.toDomain() }

    private fun Asset.toEntity(ownerId: UserId): AssetJpaEntity =
        AssetJpaEntity(
            id = id.value,
            ownerId = ownerId.value,
            name = name,
            assetType = type.name,
            liquidity = liquidity.name,
            archived = archived,
        )

    private fun AssetJpaEntity.toDomain(): Asset =
        Asset(
            id = AssetId(id),
            name = name,
            type = AssetType.valueOf(assetType),
            liquidity = Liquidity.valueOf(liquidity),
            archived = archived,
        )
}
