package com.wealthos.snapshot.adapter.http

import com.wealthos.fxrate.application.FxRateProvider
import com.wealthos.fxrate.application.ProvidedFxRate
import com.wealthos.fxrate.application.SupportedFxCurrency
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:snapshot-capture-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SnapshotCaptureControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var fxRateProvider: FxRateProvider

    @Test
    fun `capture converts original USD money with the nearest non-future CBC rate and preserves evidence`() {
        val rateDate = LocalDate.parse("2026-07-31")
        `when`(fxRateProvider.name).thenReturn("CBC")
        `when`(fxRateProvider.supportedCurrencies())
            .thenReturn(listOf(SupportedFxCurrency("USD", rateDate, rateDate)))
        `when`(fxRateProvider.fetch("USD", rateDate, rateDate))
            .thenReturn(listOf(ProvidedFxRate("USD", BigDecimal("32.292"), rateDate)))
        mockMvc.post("/api/v1/fx-rates/sync") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"from":"2026-07-31","to":"2026-07-31"}"""
        }.andExpect { status { isOk() } }

        val capture = mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-03T00:00:00Z",
                  "recordedAt":"2026-08-03T08:00:00Z",
                  "baseCurrency":"TWD",
                  "assets":[{
                    "name":"US cash","type":"CASH","liquidity":"LIQUID",
                    "originalMoney":{"amount":"100.00","currency":"USD"},
                    "effectiveAt":"2026-08-03T00:00:00Z","source":"Bank statement"
                  }],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.baseCurrency") { value("TWD") }
            jsonPath("$.assets[0].money.amount") { value("3229") }
            jsonPath("$.assets[0].money.currency") { value("TWD") }
            jsonPath("$.assets[0].appliedConversion.originalMoney.amount") { value("100.00") }
            jsonPath("$.assets[0].appliedConversion.originalMoney.currency") { value("USD") }
            jsonPath("$.assets[0].appliedConversion.rate") { value("32.292") }
            jsonPath("$.assets[0].appliedConversion.rateDate") { value("2026-07-31") }
            jsonPath("$.assets[0].appliedConversion.provider") { value("CBC") }
            jsonPath("$.assets[0].appliedConversion.rateType") { value("REFERENCE_RATE") }
            jsonPath("$.assets[0].appliedConversion.roundingMode") { value("HALF_EVEN") }
        }.andReturn()

        mockMvc.get(requireNotNull(capture.response.getHeader("Location"))).andExpect {
            status { isOk() }
            jsonPath("$.assets[0].money.amount") { value("3229") }
            jsonPath("$.assets[0].appliedConversion.rate") { value("32.292") }
            jsonPath("$.assets[0].appliedConversion.rateDate") { value("2026-07-31") }
        }
    }

    @Test
    fun `capture uses an explicit user-declared rate with a required basis`() {
        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-03T00:00:00Z",
                  "recordedAt":"2026-08-03T08:00:00Z",
                  "baseCurrency":"TWD",
                  "assets":[],
                  "liabilities":[{
                    "name":"Swiss loan",
                    "originalMoney":{"amount":"100.00","currency":"CHF"},
                    "declaredRate":{"rate":"37","rateDate":"2026-08-01","basis":"User bank quote"},
                    "effectiveAt":"2026-08-03T00:00:00Z","source":"Bank statement"
                  }]
                }
                """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.liabilities[0].money.amount") { value("3700") }
            jsonPath("$.liabilities[0].appliedConversion.rate") { value("37") }
            jsonPath("$.liabilities[0].appliedConversion.provider") { value("USER") }
            jsonPath("$.liabilities[0].appliedConversion.rateType") { value("USER_DECLARED") }
            jsonPath("$.liabilities[0].appliedConversion.basis") { value("User bank quote") }
        }
    }

    @Test
    fun `capture rejects lowercase base currency`() {
        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"usd",
                  "assets":[],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("baseCurrency") }
            jsonPath("$.errors[0].message") { value("must be an uppercase ISO 4217 currency code") }
        }
    }

    @Test
    fun `capture rejects an overlong asset name`() {
        val overlongName = "a".repeat(201)

        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"USD",
                  "assets":[{
                    "name":"$overlongName","type":"CASH","liquidity":"LIQUID",
                    "money":{"amount":"1.00","currency":"USD"},
                    "effectiveAt":"2026-08-02T00:00:00Z","source":"Statement"
                  }],
                  "liabilities":[]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("assets[0].name") }
            jsonPath("$.errors[0].message") { value("must contain at most 200 characters") }
        }
    }

    @Test
    fun `capture rejects an overlong liability name`() {
        val overlongName = "l".repeat(201)

        mockMvc.post("/api/v1/snapshot-captures") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "asOf":"2026-08-02T00:00:00Z",
                  "recordedAt":"2026-08-02T08:00:00Z",
                  "baseCurrency":"USD",
                  "assets":[],
                  "liabilities":[{
                    "name":"$overlongName",
                    "money":{"amount":"1.00","currency":"USD"},
                    "effectiveAt":"2026-08-02T00:00:00Z","source":"Statement"
                  }]
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("liabilities[0].name") }
            jsonPath("$.errors[0].message") { value("must contain at most 200 characters") }
        }
    }

    @Test
    fun `empty capture persists and returns its base currency`() {
        val capture =
            mockMvc.post("/api/v1/snapshot-captures") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "asOf":"2026-08-02T00:00:00Z",
                      "recordedAt":"2026-08-02T08:00:00Z",
                      "baseCurrency":"TWD",
                      "assets":[],
                      "liabilities":[]
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.baseCurrency") { value("TWD") }
            }.andReturn()

        mockMvc.get(requireNotNull(capture.response.getHeader("Location")))
            .andExpect {
                status { isOk() }
                jsonPath("$.baseCurrency") { value("TWD") }
                jsonPath("$.assets") { isEmpty() }
                jsonPath("$.liabilities") { isEmpty() }
            }
    }

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
