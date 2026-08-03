package com.wealthos.snapshot.adapter.http

import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:snapshot-capture-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SnapshotCaptureControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `capture updates every active position and creates one snapshot`() {
        val assetId = createResource("/api/v1/assets", """{"name":"Cash","type":"CASH","liquidity":"LIQUID"}""")
        val liabilityId = createResource("/api/v1/liabilities", """{"name":"Loan"}""")

        val capture =
            mockMvc.post("/api/v1/snapshot-captures") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "asOf":"2026-08-02T00:00:00Z",
                      "recordedAt":"2026-08-02T08:00:00Z",
                      "baseCurrency":"USD",
                      "assets":[{
                        "id":"$assetId",
                        "name":"Emergency fund",
                        "type":"INVESTMENT",
                        "liquidity":"SEMI_LIQUID",
                        "money":{"amount":"1250.00","currency":"USD"},
                        "effectiveAt":"2026-08-01T00:00:00Z",
                        "source":"Bank statement",
                        "manualConversion":{
                          "originalMoney":{"amount":"12.3456","currency":"CLF"},
                          "exchangeRateBasis":"Declared CLF/USD basis",
                          "effectiveAt":"2026-08-01T00:00:00Z"
                        }
                      }],
                      "liabilities":[{
                        "id":"$liabilityId",
                        "name":"Home loan",
                        "money":{"amount":"400.00","currency":"USD"},
                        "effectiveAt":"2026-08-01T00:00:00Z",
                        "source":"Lender statement",
                        "manualConversion":{
                          "originalMoney":{"amount":"320.00","currency":"EUR"},
                          "exchangeRateBasis":"ECB EUR/USD reference rate 1.25",
                          "effectiveAt":"2026-08-01T00:00:00Z"
                        }
                      }]
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
                header { string("Location", matchesPattern("/api/v1/snapshots/[0-9a-f-]{36}")) }
                jsonPath("$.assets[0].id") { value(assetId) }
                jsonPath("$.assets[0].name") { value("Emergency fund") }
                jsonPath("$.assets[0].money.amount") { value("1250.00") }
                jsonPath("$.assets[0].manualConversion.originalMoney.amount") { value("12.3456") }
                jsonPath("$.assets[0].manualConversion.originalMoney.currency") { value("CLF") }
                jsonPath("$.assets[0].manualConversion.exchangeRateBasis") { value("Declared CLF/USD basis") }
                jsonPath("$.assets[0].manualConversion.effectiveAt") { value("2026-08-01T00:00:00Z") }
                jsonPath("$.liabilities[0].id") { value(liabilityId) }
                jsonPath("$.liabilities[0].name") { value("Home loan") }
                jsonPath("$.liabilities[0].manualConversion.originalMoney.amount") { value("320.00") }
            }.andReturn()

        mockMvc.get(requireNotNull(capture.response.getHeader("Location")))
            .andExpect {
                status { isOk() }
                jsonPath("$.assets[0].name") { value("Emergency fund") }
                jsonPath("$.assets[0].manualConversion.originalMoney.amount") { value("12.3456") }
                jsonPath("$.assets[0].manualConversion.originalMoney.currency") { value("CLF") }
                jsonPath("$.assets[0].manualConversion.exchangeRateBasis") { value("Declared CLF/USD basis") }
                jsonPath("$.liabilities[0].name") { value("Home loan") }
                jsonPath("$.liabilities[0].manualConversion.originalMoney.amount") { value("320.00") }
            }
        mockMvc.get("/api/v1/assets/$assetId")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Emergency fund") }
                jsonPath("$.type") { value("INVESTMENT") }
                jsonPath("$.liquidity") { value("SEMI_LIQUID") }
            }
        mockMvc.get("/api/v1/liabilities/$liabilityId")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Home loan") }
            }
    }

    @Test
    fun `capture assigns identities to new positions`() {
        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"TWD",
                  "assets":[{
                    "name":"Checking account","type":"CASH","liquidity":"LIQUID",
                    "money":{"amount":"250000.00","currency":"TWD"},
                    "effectiveAt":"2026-08-02T00:00:00Z","source":"Bank statement"
                  }],
                  "liabilities":[{
                    "name":"Mortgage","money":{"amount":"8000000.00","currency":"TWD"},
                    "effectiveAt":"2026-08-02T00:00:00Z","source":"Lender statement"
                  }]
                }
                """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.assets[0].id", matchesPattern("[0-9a-f-]{36}"))
            jsonPath("$.liabilities[0].id", matchesPattern("[0-9a-f-]{36}"))
        }

        mockMvc.get("/api/v1/assets")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].name") { value("Checking account") }
            }
        mockMvc.get("/api/v1/liabilities")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].name") { value("Mortgage") }
            }
    }

    @Test
    fun `capture rejects a manual conversion from the base currency`() {
        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"USD",
                  "assets":[{
                    "name":"Cash","type":"CASH","liquidity":"LIQUID",
                    "money":{"amount":"1250.00","currency":"USD"},
                    "effectiveAt":"2026-08-01T00:00:00Z","source":"Bank statement",
                    "manualConversion":{
                      "originalMoney":{"amount":"1000.00","currency":"USD"},
                      "exchangeRateBasis":"Declared rate 1.25",
                      "effectiveAt":"2026-08-01T00:00:00Z"
                    }
                  }],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets[0].manualConversion.originalMoney.currency") }
            jsonPath("$.errors[0].message") { value("must differ from baseCurrency") }
        }
    }

    @Test
    fun `capture rejects a manual conversion effective after the snapshot`() {
        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"USD",
                  "assets":[{
                    "name":"Cash","type":"CASH","liquidity":"LIQUID",
                    "money":{"amount":"1250.00","currency":"USD"},
                    "effectiveAt":"2026-08-01T00:00:00Z","source":"Bank statement",
                    "manualConversion":{
                      "originalMoney":{"amount":"1000.00","currency":"EUR"},
                      "exchangeRateBasis":"Declared rate 1.25",
                      "effectiveAt":"2026-08-03T00:00:00Z"
                    }
                  }],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets[0].manualConversion.effectiveAt") }
            jsonPath("$.errors[0].message") { value("must not be after asOf") }
        }
    }

    @Test
    fun `capture rejects a blank exchange-rate basis`() {
        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"USD",
                  "assets":[{
                    "name":"Cash","type":"CASH","liquidity":"LIQUID",
                    "money":{"amount":"1250.00","currency":"USD"},
                    "effectiveAt":"2026-08-01T00:00:00Z","source":"Bank statement",
                    "manualConversion":{
                      "originalMoney":{"amount":"1000.00","currency":"EUR"},
                      "exchangeRateBasis":" ",
                      "effectiveAt":"2026-08-01T00:00:00Z"
                    }
                  }],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets[0].manualConversion.exchangeRateBasis") }
        }
    }

    @Test
    fun `invalid capture rolls back current metadata and snapshot`() {
        val assetId = createResource("/api/v1/assets", """{"name":"Original cash","type":"CASH","liquidity":"LIQUID"}""")

        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"USD",
                  "assets":[{
                    "id":"$assetId","name":"Must roll back","type":"INVESTMENT","liquidity":"SEMI_LIQUID",
                    "money":{"amount":"-1.00","currency":"USD"},
                    "effectiveAt":"2026-08-02T00:00:00Z","source":"Invalid statement"
                  }],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets[0].money.amount") }
            jsonPath("$.errors[0].message") { value("must be a non-negative decimal amount") }
        }

        mockMvc.get("/api/v1/assets/$assetId")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Original cash") }
                jsonPath("$.type") { value("CASH") }
            }
        mockMvc.get("/api/v1/snapshots")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `capture identifies an incomplete active set`() {
        createResource("/api/v1/assets", """{"name":"Cash","type":"CASH","liquidity":"LIQUID"}""")

        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"USD",
                  "assets":[],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets") }
            jsonPath("$.errors[0].message") { value("must include every active asset exactly once") }
        }
    }

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
        ).substringAfterLast('/')
}
