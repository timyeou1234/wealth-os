package com.wealthos.asset.adapter.http

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:asset-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc
class AssetControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var assetRepository: AssetRepository

    @BeforeEach
    fun clearAssets() {
        // Each test owns its data; repository deletion will be added with write operations.
        check(assetRepository.findAll().isEmpty())
    }

    @Test
    fun `lists assets as transport responses`() {
        val home =
            assetRepository.save(
                Asset(
                    id = AssetId.new(),
                    name = "Home",
                    type = AssetType.REAL_ESTATE,
                    liquidity = Liquidity.ILLIQUID,
                ),
            )
        val cash =
            assetRepository.save(
                Asset(
                    id = AssetId.new(),
                    name = "Cash",
                    type = AssetType.CASH,
                    liquidity = Liquidity.LIQUID,
                ),
            )

        mockMvc.get("/api/v1/assets")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { value(cash.id.value.toString()) }
                jsonPath("$[0].name") { value("Cash") }
                jsonPath("$[0].type") { value("CASH") }
                jsonPath("$[0].liquidity") { value("LIQUID") }
                jsonPath("$[1].id") { value(home.id.value.toString()) }
                jsonPath("$[1].name") { value("Home") }
                jsonPath("$[1].type") { value("REAL_ESTATE") }
                jsonPath("$[1].liquidity") { value("ILLIQUID") }
            }
    }
}
