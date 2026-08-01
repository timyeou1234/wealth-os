package com.wealthos.financialhealth.adapter.http

import com.wealthos.domain.financialhealth.FinancialHealth
import com.wealthos.domain.financialhealth.FinancialHealthResult
import com.wealthos.domain.financialhealth.FinancialRatio
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.SnapshotId
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
    val totalAssets: MoneyResponse,
    val totalLiabilities: MoneyResponse,
    val netWorth: MoneyResponse,
    val debtRatio: String?,
    val liquidityRatio: String?,
) {
    companion object {
        fun from(result: FinancialHealthResult): FinancialHealthResponse =
            when (result) {
                is FinancialHealthResult.Calculated -> from(result.financialHealth)
                is FinancialHealthResult.InsufficientData ->
                    throw IllegalStateException("Financial health is incomplete: ${result.reason}")
            }

        private fun from(health: FinancialHealth): FinancialHealthResponse =
            FinancialHealthResponse(
                totalAssets = MoneyResponse.from(health.totalAssets),
                totalLiabilities = MoneyResponse.from(health.totalLiabilities),
                netWorth = MoneyResponse.from(health.netWorth),
                debtRatio = health.debtRatio.asString(),
                liquidityRatio = health.liquidityRatio.asString(),
            )

        private fun FinancialRatio.asString(): String? =
            when (this) {
                is FinancialRatio.Defined -> value.toPlainString()
                FinancialRatio.Undefined -> null
            }
    }
}

data class MoneyResponse(
    val amount: String,
    val currency: String,
) {
    companion object {
        fun from(money: Money): MoneyResponse = MoneyResponse(money.amount.toPlainString(), money.currency.code)
    }
}
