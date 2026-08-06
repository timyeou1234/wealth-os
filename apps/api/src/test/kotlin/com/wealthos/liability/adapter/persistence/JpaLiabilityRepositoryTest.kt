package com.wealthos.liability.adapter.persistence

import com.wealthos.identity.domain.UserId
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilityRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.test.assertEquals

@DataJpaTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
    ],
)
@Import(JpaLiabilityRepository::class)
class JpaLiabilityRepositoryTest
    @Autowired
    constructor(
        private val repository: LiabilityRepository,
    ) {
        private val ownerId = UserId(UUID.fromString("81d1d865-ece0-4871-a81f-15a2916176ec"))

        @Test
        fun `persists and restores a liability through the domain port`() {
            val liability =
                Liability(
                    id = LiabilityId.new(),
                    name = "Mortgage",
                )

            repository.save(ownerId, liability)

            val restored = repository.findById(ownerId, liability.id)
            requireNotNull(restored)
            assertEquals(liability.id, restored.id)
            assertEquals(liability.name, restored.name)
        }
    }
