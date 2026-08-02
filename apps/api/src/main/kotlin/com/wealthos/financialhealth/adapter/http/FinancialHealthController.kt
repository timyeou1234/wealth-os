package com.wealthos.financialhealth.adapter.http

import com.wealthos.domain.financialhealth.FinancialHealth
import com.wealthos.domain.financialhealth.FinancialHealthResult
import com.wealthos.domain.financialhealth.FinancialRatio
import com.wealthos.domain.financialhealth.InsufficientDataReason
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.financialhealth.application.FinancialHealthView
import com.wealthos.financialhealth.application.GetFinancialHealth
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/financial-health")
@Tag(name = "Financial Health")
class FinancialHealthController(
    private val getFinancialHealth: GetFinancialHealth,
) {
    @GetMapping("/{snapshotId}")
    @Operation(summary = "Get financial health for a snapshot")
    fun get(
        @PathVariable snapshotId: UUID,
    ): FinancialHealthResponse = FinancialHealthResponse.from(getFinancialHealth.execute(SnapshotId(snapshotId)))
}

data class FinancialHealthResponse(
    val status: String,
    val reason: String?,
    val totalAssets: MoneyResponse?,
    val totalLiabilities: MoneyResponse?,
    val netWorth: MoneyResponse?,
    val debtRatio: String?,
    val liquidityRatio: String?,
    val explanations: FinancialHealthExplanations,
) {
    companion object {
        fun from(view: FinancialHealthView): FinancialHealthResponse = when (val result = view.result) {
            is FinancialHealthResult.Calculated -> from(view, result.financialHealth)
            is FinancialHealthResult.InsufficientData -> FinancialHealthResponse(
                status = "INSUFFICIENT_DATA",
                reason = result.reason.asCode(),
                totalAssets = null,
                totalLiabilities = null,
                netWorth = null,
                debtRatio = null,
                liquidityRatio = null,
                explanations = FinancialHealthExplanations.from(view),
            )
        }

        private fun from(view: FinancialHealthView, health: FinancialHealth): FinancialHealthResponse =
            FinancialHealthResponse(
                status = "CALCULATED",
                reason = null,
                totalAssets = MoneyResponse.from(health.totalAssets),
                totalLiabilities = MoneyResponse.from(health.totalLiabilities),
                netWorth = MoneyResponse.from(health.netWorth),
                debtRatio = health.debtRatio.asString(),
                liquidityRatio = health.liquidityRatio.asString(),
                explanations = FinancialHealthExplanations.from(view),
            )

        private fun FinancialRatio.asString(): String? =
            when (this) {
                is FinancialRatio.Defined -> value.toPlainString()
                FinancialRatio.Undefined -> null
            }

        private fun InsufficientDataReason.asCode(): String = when (this) {
            InsufficientDataReason.EmptySnapshot -> "EMPTY_SNAPSHOT"
            is InsufficientDataReason.MixedCurrencies -> "MIXED_CURRENCIES"
        }
    }
}

data class FinancialHealthExplanations(
    val debtRatioFormula: String,
    val liquidityRatioFormula: String,
    val assetContributors: List<FinancialHealthContributor>,
    val liabilityContributors: List<FinancialHealthContributor>,
) {
    companion object {
        fun from(view: FinancialHealthView): FinancialHealthExplanations = FinancialHealthExplanations(
            debtRatioFormula = "Total Liabilities / Total Assets",
            liquidityRatioFormula = "Liquid Assets / Total Assets",
            assetContributors = view.snapshot.assetPositions.map {
                FinancialHealthContributor(it.assetId.value.toString(), it.name, MoneyResponse.from(it.valuation.value), it.liquidity.name)
            },
            liabilityContributors = view.snapshot.liabilityPositions.map {
                FinancialHealthContributor(it.liabilityId.value.toString(), it.name, MoneyResponse.from(it.balance.balance), null)
            },
        )
    }
}

data class FinancialHealthContributor(
    val id: String,
    val name: String,
    val amount: MoneyResponse,
    val liquidity: String?,
)

data class MoneyResponse(
    val amount: String,
    val currency: String,
) {
    companion object {
        fun from(money: Money): MoneyResponse = MoneyResponse(money.amount.toPlainString(), money.currency.code)
    }
}
