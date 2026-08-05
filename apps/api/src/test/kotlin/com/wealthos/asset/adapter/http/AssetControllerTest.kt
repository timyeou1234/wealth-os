package com.wealthos.asset.adapter.http

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.identity.domain.UserId
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:asset-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AssetControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var assetRepository: AssetRepository

    @MockitoBean
    private lateinit var currentUser: CurrentUserIdProvider

    private val ownerId = UserId(UUID.fromString("0d7df138-40f2-4a54-b06a-1216ef2d8801"))

    @BeforeEach
    fun provideCurrentUser() {
        `when`(currentUser.get()).thenReturn(ownerId)
    }

    @Test
    fun `lists assets as transport responses`() {
        val home =
            assetRepository.save(
                ownerId,
                Asset(
                    id = AssetId.new(),
                    name = "Home",
                    type = AssetType.REAL_ESTATE,
                    liquidity = Liquidity.ILLIQUID,
                ),
            )
        val cash =
            assetRepository.save(
                ownerId,
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

    @Test
    fun `user can create an asset and retrieve it`() {
        val creationResult = mockMvc.post("/api/v1/assets") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "name": "Emergency Fund",
                  "type": "CASH",
                  "liquidity": "LIQUID"
                }
                """.trimIndent()
        }.andExpect {
            status { isCreated() }
            header { string("Location", matchesPattern("/api/v1/assets/[0-9a-f-]{36}")) }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Emergency Fund") }
            jsonPath("$.type") { value("CASH") }
            jsonPath("$.liquidity") { value("LIQUID") }
        }.andReturn()

        val location = requireNotNull(creationResult.response.getHeader("Location"))
        mockMvc.get(location)
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { isNotEmpty() }
                jsonPath("$.name") { value("Emergency Fund") }
                jsonPath("$.type") { value("CASH") }
                jsonPath("$.liquidity") { value("LIQUID") }
            }

        mockMvc.get("/api/v1/assets")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].name") { value("Emergency Fund") }
                jsonPath("$[0].type") { value("CASH") }
                jsonPath("$[0].liquidity") { value("LIQUID") }
            }
    }

    @Test
    fun `user can update an asset metadata`() {
        val asset =
            assetRepository.save(
                ownerId,
                Asset(AssetId.new(), "Savings", AssetType.CASH, Liquidity.LIQUID),
            )

        mockMvc.put("/api/v1/assets/${asset.id.value}") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "name": "Brokerage Account",
                  "type": "INVESTMENT",
                  "liquidity": "SEMI_LIQUID"
                }
                """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(asset.id.value.toString()) }
            jsonPath("$.name") { value("Brokerage Account") }
            jsonPath("$.type") { value("INVESTMENT") }
            jsonPath("$.liquidity") { value("SEMI_LIQUID") }
        }

        mockMvc.get("/api/v1/assets/${asset.id.value}")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Brokerage Account") }
                jsonPath("$.type") { value("INVESTMENT") }
                jsonPath("$.liquidity") { value("SEMI_LIQUID") }
            }
    }

    @Test
    fun `user can archive an asset without removing its resource`() {
        val asset =
            assetRepository.save(
                ownerId,
                Asset(AssetId.new(), "Old Account", AssetType.CASH, Liquidity.LIQUID),
            )

        mockMvc.post("/api/v1/assets/${asset.id.value}/archive")
            .andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/assets")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }

        mockMvc.get("/api/v1/assets/${asset.id.value}")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(asset.id.value.toString()) }
                jsonPath("$.archived") { value(true) }
            }
    }

    @Test
    fun `blank asset name identifies the invalid field`() {
        mockMvc.post("/api/v1/assets") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "name": "   ",
                  "type": "CASH",
                  "liquidity": "LIQUID"
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            content { contentType("application/problem+json") }
            jsonPath("$.type") { value("urn:wealthos:problem:validation-error") }
            jsonPath("$.title") { value("Request validation failed") }
            jsonPath("$.status") { value(400) }
            jsonPath("$.detail") { value("One or more fields are invalid") }
            jsonPath("$.instance") { value("/api/v1/assets") }
            jsonPath("$.errors.length()") { value(1) }
            jsonPath("$.errors[0].field") { value("name") }
            jsonPath("$.errors[0].message") { value("must not be blank") }
        }
    }

    @Test
    fun `missing asset returns a not found problem`() {
        val missingId = "0f27e4fa-99f8-4c5e-87da-527488cbe515"

        mockMvc.get("/api/v1/assets/$missingId")
            .andExpect {
                status { isNotFound() }
                content { contentType("application/problem+json") }
                jsonPath("$.type") { value("urn:wealthos:problem:asset-not-found") }
                jsonPath("$.title") { value("Asset not found") }
                jsonPath("$.status") { value(404) }
                jsonPath("$.detail") { value("Asset $missingId was not found") }
                jsonPath("$.instance") { value("/api/v1/assets/$missingId") }
            }
    }
}
