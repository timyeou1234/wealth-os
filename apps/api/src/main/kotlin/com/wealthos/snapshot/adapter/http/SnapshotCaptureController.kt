package com.wealthos.snapshot.adapter.http

import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.snapshot.application.CaptureAsset
import com.wealthos.snapshot.application.CaptureLiability
import com.wealthos.snapshot.application.CaptureSnapshot
import com.wealthos.snapshot.application.CaptureSnapshotCommand
import com.wealthos.shared.adapter.http.ValidationProblemResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/snapshot-captures")
@Tag(name = "Snapshot Captures")
class SnapshotCaptureController(
    private val captureSnapshot: CaptureSnapshot,
) {
    @PostMapping
    @Operation(summary = "Atomically update the current balance sheet and capture a snapshot", operationId = "captureSnapshot")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Snapshot captured", content = [Content(mediaType = "application/json", schema = Schema(implementation = SnapshotResponse::class))]),
            ApiResponse(responseCode = "400", description = "Request validation failed", content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ValidationProblemResponse::class))]),
        ],
    )
    fun capture(
        @Valid @RequestBody request: CaptureSnapshotRequest,
    ): ResponseEntity<SnapshotResponse> {
        val snapshot = captureSnapshot.execute(request.toCommand())
        return ResponseEntity.created(URI.create("/api/v1/snapshots/${snapshot.id.value}"))
            .body(SnapshotResponse.from(snapshot))
    }
}

data class CaptureSnapshotRequest(
    @field:NotNull val asOf: Instant,
    @field:NotNull val recordedAt: Instant,
    @field:NotBlank val baseCurrency: String,
    @field:Valid val assets: List<CaptureAssetRequest> = emptyList(),
    @field:Valid val liabilities: List<CaptureLiabilityRequest> = emptyList(),
) {
    fun toCommand(): CaptureSnapshotCommand {
        val currency = Currency.of(baseCurrency)
        return CaptureSnapshotCommand(
            asOf = asOf,
            recordedAt = recordedAt,
            baseCurrency = currency,
            assets = assets.map { it.toCommand(currency) },
            liabilities = liabilities.map { it.toCommand(currency) },
        )
    }
}

data class CaptureAssetRequest(
    val id: UUID? = null,
    @field:NotBlank val name: String,
    @field:NotNull val type: AssetType,
    @field:NotNull val liquidity: Liquidity,
    @field:Valid @field:NotNull val money: MoneyRequest,
    @field:NotNull val effectiveAt: Instant,
    @field:NotBlank val source: String,
) {
    fun toCommand(baseCurrency: Currency): CaptureAsset =
        CaptureAsset(
            id = id?.let(::AssetId),
            name = name,
            type = type,
            liquidity = liquidity,
            money = money.toDomain(baseCurrency),
            effectiveAt = effectiveAt,
            source = ValuationSource.of(source),
        )
}

data class CaptureLiabilityRequest(
    val id: UUID? = null,
    @field:NotBlank val name: String,
    @field:Valid @field:NotNull val money: MoneyRequest,
    @field:NotNull val effectiveAt: Instant,
    @field:NotBlank val source: String,
) {
    fun toCommand(baseCurrency: Currency): CaptureLiability =
        CaptureLiability(
            id = id?.let(::LiabilityId),
            name = name,
            money = money.toDomain(baseCurrency),
            effectiveAt = effectiveAt,
            source = LiabilitySource.of(source),
        )
}

private fun MoneyRequest.toDomain(baseCurrency: Currency): Money {
    require(currency == baseCurrency.code) { "Money currency must match the base currency" }
    require(amount.matches(Regex("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?$"))) { "Money amount must be a decimal string" }
    return Money.of(amount.toBigDecimal(), baseCurrency)
}
