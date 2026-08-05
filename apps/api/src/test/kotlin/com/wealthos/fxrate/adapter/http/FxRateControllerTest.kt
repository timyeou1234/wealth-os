package com.wealthos.fxrate.adapter.http

import com.wealthos.fxrate.application.FxRateProvider
import com.wealthos.fxrate.application.ProvidedFxRate
import com.wealthos.fxrate.application.SupportedFxCurrency
import com.wealthos.fxrate.application.SyncFxRates
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.http.MediaType
import java.math.BigDecimal
import java.time.LocalDate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers(disabledWithoutDocker = true)
class FxRateControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var syncFxRates: SyncFxRates

    @MockitoBean
    private lateinit var provider: FxRateProvider

    @Test
    fun `as-of query returns TWD identity and explicitly reports a missing currency`() {
        mockMvc.get("/api/v1/fx-rates") {
            param("asOf", "2026-08-03")
            param("currencies", "TWD,CHF")
        }.andExpect {
            status { isOk() }
            jsonPath("$.valuationCurrency") { value("TWD") }
            jsonPath("$.asOf") { value("2026-08-03") }
            jsonPath("$.rates[0].originalCurrency") { value("TWD") }
            jsonPath("$.rates[0].rate") { value("1") }
            jsonPath("$.rates[0].rateDate") { value("2026-08-03") }
            jsonPath("$.rates[0].provider") { value("IDENTITY") }
            jsonPath("$.rates[0].rateType") { value("REFERENCE_RATE") }
            jsonPath("$.missingCurrencies[0]") { value("CHF") }
        }
    }

    @Test
    fun `sync is idempotent and query selects the nearest non-future CBC rate`() {
        `when`(provider.name).thenReturn("CBC")
        `when`(provider.supportedCurrencies())
            .thenReturn(
                listOf(
                    SupportedFxCurrency("USD", LocalDate.parse("1948-06-21"), LocalDate.parse("2026-08-03")),
                ),
            )
        `when`(provider.fetch("USD", LocalDate.parse("2026-07-31"), LocalDate.parse("2026-08-03")))
            .thenReturn(
                listOf(
                    ProvidedFxRate("USD", BigDecimal("32.292"), LocalDate.parse("2026-07-31")),
                    ProvidedFxRate("USD", BigDecimal("32.500"), LocalDate.parse("2026-08-04")),
                ),
            )

        repeat(2) {
            mockMvc.post("/api/v1/fx-rates/sync") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"from":"2026-07-31","to":"2026-08-03"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.provider") { value("CBC") }
                jsonPath("$.latestRateDate") { value("2026-07-31") }
            }
        }

        mockMvc.get("/api/v1/fx-rates") {
            param("asOf", "2026-08-03")
            param("currencies", "USD")
        }.andExpect {
            status { isOk() }
            jsonPath("$.rates[0].originalCurrency") { value("USD") }
            jsonPath("$.rates[0].rate") { value("32.292") }
            jsonPath("$.rates[0].rateDate") { value("2026-07-31") }
            jsonPath("$.rates[0].provider") { value("CBC") }
            jsonPath("$.missingCurrencies") { isEmpty() }
        }
    }

    @Test
    fun `catch-up tracks each currency independently when a prior response omitted one`() {
        val date = LocalDate.parse("2026-07-30")
        val aud = SupportedFxCurrency("AUD", date, date)
        val cad = SupportedFxCurrency("CAD", date, date)
        `when`(provider.name).thenReturn("CBC")
        `when`(provider.supportedCurrencies()).thenReturn(listOf(aud), listOf(aud, cad))
        `when`(provider.fetch("AUD", date, date))
            .thenReturn(listOf(ProvidedFxRate("AUD", BigDecimal("21.25"), date)))
        `when`(provider.fetch("CAD", date, date))
            .thenReturn(listOf(ProvidedFxRate("CAD", BigDecimal("23.50"), date)))

        syncFxRates.execute(date, date)
        syncFxRates.catchUp(date)

        mockMvc.get("/api/v1/fx-rates") {
            param("asOf", date.toString())
            param("currencies", "AUD,CAD")
        }.andExpect {
            status { isOk() }
            jsonPath("$.rates.length()") { value(2) }
            jsonPath("$.missingCurrencies") { isEmpty() }
        }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18-alpine")
    }
}
