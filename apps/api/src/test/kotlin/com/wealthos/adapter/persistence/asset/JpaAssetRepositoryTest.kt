package com.wealthos.adapter.persistence.asset

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.AssetId
import com.wealthos.domain.asset.AssetRepository
import com.wealthos.domain.asset.AssetType
import com.wealthos.domain.asset.Liquidity
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@Import(JpaAssetRepository::class)
class JpaAssetRepositoryTest
    @Autowired
    constructor(
        private val repository: AssetRepository,
    ) {
        @Test
        fun `persists and restores an asset through the domain port`() {
            val asset =
                Asset(
                    id = AssetId.new(),
                    name = "Cash",
                    type = AssetType.CASH,
                    liquidity = Liquidity.LIQUID,
                )

            repository.save(asset)

            val restored = repository.findById(asset.id)
            requireNotNull(restored)
            assertEquals(asset.id, restored.id)
            assertEquals(asset.name, restored.name)
            assertEquals(asset.type, restored.type)
            assertEquals(asset.liquidity, restored.liquidity)
        }
    }
