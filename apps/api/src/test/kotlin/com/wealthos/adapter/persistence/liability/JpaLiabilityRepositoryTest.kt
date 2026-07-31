package com.wealthos.adapter.persistence.liability

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@Import(JpaLiabilityRepository::class)
class JpaLiabilityRepositoryTest
    @Autowired
    constructor(
        private val repository: LiabilityRepository,
    ) {
        @Test
        fun `persists and restores a liability through the domain port`() {
            val liability =
                Liability(
                    id = LiabilityId.new(),
                    name = "Mortgage",
                )

            repository.save(liability)

            val restored = repository.findById(liability.id)
            requireNotNull(restored)
            assertEquals(liability.id, restored.id)
            assertEquals(liability.name, restored.name)
        }
    }
