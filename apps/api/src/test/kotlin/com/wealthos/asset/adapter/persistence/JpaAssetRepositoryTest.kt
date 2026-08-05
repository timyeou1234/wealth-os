package com.wealthos.asset.adapter.persistence

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.identity.domain.UserId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import java.util.UUID

@DataJpaTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
    ],
)
@Import(JpaAssetRepository::class)
class JpaAssetRepositoryTest
    @Autowired
    constructor(
        private val repository: AssetRepository,
    ) {
        private val ownerId = UserId(UUID.fromString("88de3a39-fe37-493b-814e-93871379ee68"))

        @Test
        fun `persists and restores an asset through the domain port`() {
            val asset =
                Asset(
                    id = AssetId.new(),
                    name = "Cash",
                    type = AssetType.CASH,
                    liquidity = Liquidity.LIQUID,
                )

            repository.save(ownerId, asset)

            val restored = repository.findById(ownerId, asset.id)
            requireNotNull(restored)
            assertEquals(asset.id, restored.id)
            assertEquals(asset.name, restored.name)
            assertEquals(asset.type, restored.type)
            assertEquals(asset.liquidity, restored.liquidity)
        }

        @Test
        fun `finds every persisted asset through the domain port`() {
            val cash =
                Asset(
                    id = AssetId.new(),
                    name = "Cash",
                    type = AssetType.CASH,
                    liquidity = Liquidity.LIQUID,
                )
            val home =
                Asset(
                    id = AssetId.new(),
                    name = "Home",
                    type = AssetType.REAL_ESTATE,
                    liquidity = Liquidity.ILLIQUID,
                )
            repository.save(ownerId, cash)
            repository.save(ownerId, home)

            assertEquals(setOf(cash, home), repository.findAll(ownerId).toSet())
        }
    }
