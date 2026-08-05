package com.wealthos.snapshot.adapter.http

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
        "spring.datasource.url=jdbc:h2:mem:snapshot-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class SnapshotControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var currentUser: CurrentUserIdProvider

    @BeforeEach
    fun provideCurrentUser() {
        `when`(currentUser.get()).thenReturn(
            UserId(UUID.fromString("32f72804-0574-48a9-8d83-581501ac3adc")),
        )
    }

    @Test
    fun `user can create an empty snapshot and retrieve it`() {
        val result = mockMvc.post("/api/v1/snapshots") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"asOf":"2026-08-01T00:00:00Z","recordedAt":"2026-08-01T00:00:00Z","assets":[],"liabilities":[]}
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            header { string("Location", matchesPattern("/api/v1/snapshots/[0-9a-f-]{36}")) }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.asOf") { value("2026-08-01T00:00:00Z") }
            jsonPath("$.assets.length()") { value(0) }
            jsonPath("$.liabilities.length()") { value(0) }
        }.andReturn()

        val location = requireNotNull(result.response.getHeader("Location"))
        mockMvc.get(location)
            .andExpect {
                status { isOk() }
                jsonPath("$.asOf") { value("2026-08-01T00:00:00Z") }
                jsonPath("$.assets.length()") { value(0) }
                jsonPath("$.liabilities.length()") { value(0) }
            }
    }

    @Test
    fun `lists effective snapshots in chronological order`() {
        createSnapshot("2026-07-01T00:00:00Z")
        createSnapshot("2026-08-01T00:00:00Z")

        mockMvc.get("/api/v1/snapshots")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].asOf") { value("2026-07-01T00:00:00Z") }
                jsonPath("$[1].asOf") { value("2026-08-01T00:00:00Z") }
            }
    }

    @Test
    fun `current position lifecycle does not rewrite a saved snapshot`() {
        val asset = createResource("/api/v1/assets", """{"name":"Original Cash","type":"CASH","liquidity":"LIQUID"}""")
        val liability = createResource("/api/v1/liabilities", """{"name":"Original Loan"}""")
        val snapshot =
            mockMvc.post("/api/v1/snapshots") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "asOf":"2026-08-01T00:00:00Z",
                      "recordedAt":"2026-08-01T00:00:00Z",
                      "assets":[{"id":"${asset.substringAfterLast('/')}","name":"Original Cash","type":"CASH","liquidity":"LIQUID","money":{"amount":"1000.00","currency":"USD"},"effectiveAt":"2026-08-01T00:00:00Z","source":"Bank statement"}],
                      "liabilities":[{"id":"${liability.substringAfterLast('/')}","name":"Original Loan","money":{"amount":"250.00","currency":"USD"},"effectiveAt":"2026-08-01T00:00:00Z","source":"Lender statement"}]
                    }
                    """.trimIndent()
            }.andReturn().response.getHeader("Location")!!

        mockMvc.put(asset) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Renamed Fund","type":"INVESTMENT","liquidity":"SEMI_LIQUID"}"""
        }.andExpect { status { isOk() } }
        mockMvc.post("$asset/archive").andExpect { status { isNoContent() } }
        mockMvc.put(liability) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Renamed Loan"}"""
        }.andExpect { status { isOk() } }
        mockMvc.post("$liability/archive").andExpect { status { isNoContent() } }

        mockMvc.get(snapshot)
            .andExpect {
                status { isOk() }
                jsonPath("$.assets[0].name") { value("Original Cash") }
                jsonPath("$.assets[0].type") { value("CASH") }
                jsonPath("$.assets[0].liquidity") { value("LIQUID") }
                jsonPath("$.liabilities[0].name") { value("Original Loan") }
            }
    }

    @Test
    fun `manual conversion rejects non-decimal amount syntax`() {
        mockMvc.post("/api/v1/snapshots") {
            contentType = MediaType.APPLICATION_JSON
            content = manualConversionSnapshot(originalAmount = "1e3", originalCurrency = "EUR")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets[0].manualConversion.originalMoney.amount") }
        }
    }

    @Test
    fun `manual conversion rejects a lowercase currency`() {
        mockMvc.post("/api/v1/snapshots") {
            contentType = MediaType.APPLICATION_JSON
            content = manualConversionSnapshot(originalAmount = "1000.00", originalCurrency = "eur")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets[0].manualConversion.originalMoney.currency") }
        }
    }

    private fun createSnapshot(asOf: String) {
        mockMvc.post("/api/v1/snapshots") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"asOf":"$asOf","recordedAt":"$asOf","assets":[],"liabilities":[]}"""
        }.andExpect {
            status { isCreated() }
        }
    }

    private fun manualConversionSnapshot(
        originalAmount: String,
        originalCurrency: String,
    ): String =
        """
        {
          "asOf":"2026-08-01T00:00:00Z","recordedAt":"2026-08-01T00:00:00Z",
          "assets":[{
            "id":"0f27e4fa-99f8-4c5e-87da-527488cbe515","name":"Cash","type":"CASH","liquidity":"LIQUID",
            "money":{"amount":"1250.00","currency":"USD"},"effectiveAt":"2026-08-01T00:00:00Z","source":"Bank",
            "manualConversion":{
              "originalMoney":{"amount":"$originalAmount","currency":"$originalCurrency"},
              "exchangeRateBasis":"Declared rate","effectiveAt":"2026-08-01T00:00:00Z"
            }
          }],"liabilities":[]
        }
        """.trimIndent()

    private fun createResource(
        path: String,
        body: String,
    ): String =
        requireNotNull(
            mockMvc.post(path) {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect { status { isCreated() } }
                .andReturn().response.getHeader("Location"),
        )
}
