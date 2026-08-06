package com.wealthos.financialhealth.adapter.http

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityBalance
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.identity.domain.UserId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:financial-health-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class FinancialHealthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var snapshotRepository: SnapshotRepository

    @MockitoBean
    private lateinit var currentUser: CurrentUserIdProvider

    private val ownerId = UserId(UUID.fromString("a2c73231-2b53-4373-8fb3-c1681d936d6e"))

    @BeforeEach
    fun provideCurrentUser() {
        `when`(currentUser.get()).thenReturn(ownerId)
    }

    @Test
    fun `missing snapshot returns a not found problem`() {
        val missingId = "0f27e4fa-99f8-4c5e-87da-527488cbe515"

        mockMvc.get("/api/v1/financial-health/$missingId")
            .andExpect {
                status { isNotFound() }
                content { contentType("application/problem+json") }
                jsonPath("$.type") { value("urn:wealthos:problem:snapshot-not-found") }
                jsonPath("$.title") { value("Snapshot not found") }
                jsonPath("$.status") { value(404) }
                jsonPath("$.detail") { value("Snapshot $missingId was not found") }
                jsonPath("$.instance") { value("/api/v1/financial-health/$missingId") }
            }
    }

    @Test
    fun `returns calculated financial health for a snapshot`() {
        val snapshot = snapshotWithBalanceSheet()
        snapshotRepository.save(ownerId, snapshot)

        mockMvc.get("/api/v1/financial-health/${snapshot.id.value}")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
                jsonPath("$.totalAssets.amount") { value("1000.00") }
                jsonPath("$.status") { value("CALCULATED") }
                jsonPath("$.totalAssets.currency") { value("USD") }
                jsonPath("$.totalLiabilities.amount") { value("250.00") }
                jsonPath("$.netWorth.amount") { value("750.00") }
                jsonPath("$.debtRatio") { value("0.250000") }
                jsonPath("$.liquidityRatio") { value("1.000000") }
                jsonPath("$.explanations.debtRatioFormula") { value("Total Liabilities / Total Assets") }
                jsonPath("$.explanations.liquidityRatioFormula") { value("Liquid Assets / Total Assets") }
                jsonPath("$.explanations.assetContributors[0].name") { value("Cash") }
            }
    }

    @Test
    fun `returns explicit insufficient data for an empty snapshot`() {
        val asOf = Instant.parse("2026-08-01T00:00:00Z")
        val snapshot = Snapshot.capture(SnapshotId.new(), asOf, asOf)
        snapshotRepository.save(ownerId, snapshot)

        mockMvc.get("/api/v1/financial-health/${snapshot.id.value}")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("INSUFFICIENT_DATA") }
                jsonPath("$.reason") { value("EMPTY_SNAPSHOT") }
                jsonPath("$.totalAssets") { doesNotExist() }
            }
    }

    private fun snapshotWithBalanceSheet(): Snapshot {
        val currency = Currency.of("USD")
        val asOf = Instant.parse("2026-08-01T00:00:00Z")
        val asset = Asset(AssetId.new(), "Cash", AssetType.CASH, Liquidity.LIQUID)
        val liability = Liability(LiabilityId.new(), "Loan")

        return Snapshot.capture(
            id = SnapshotId.new(),
            asOf = asOf,
            recordedAt = asOf,
            assets = listOf(asset),
            assetValuations = listOf(AssetValuation(asset.id, Money.of(BigDecimal("1000.00"), currency), asOf, ValuationSource.of("bank"))),
            liabilities = listOf(liability),
            liabilityBalances = listOf(LiabilityBalance(liability.id, Money.of(BigDecimal("250.00"), currency), asOf, LiabilitySource.of("bank"))),
        )
    }
}
