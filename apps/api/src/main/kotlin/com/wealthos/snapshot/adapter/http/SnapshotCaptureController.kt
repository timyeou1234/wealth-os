package com.wealthos.snapshot.adapter.http

import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.shared.ManualConversion
import com.wealthos.snapshot.application.CaptureAsset
import com.wealthos.snapshot.application.CaptureLiability
import com.wealthos.snapshot.application.CaptureSnapshot
import com.wealthos.snapshot.application.CaptureSnapshotCommand
import com.wealthos.shared.adapter.http.ValidationProblemResponse
import com.wealthos.shared.application.RequestValidationException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    @field:Valid @field:NotNull val assets: List<CaptureAssetRequest>,
    @field:Valid @field:NotNull val liabilities: List<CaptureLiabilityRequest>,
) {
    fun toCommand(): CaptureSnapshotCommand {
        if (!baseCurrency.matches(UPPERCASE_CURRENCY_CODE)) {
            throw RequestValidationException("baseCurrency", "must be an uppercase ISO 4217 currency code")
        }
        val currency = validated("baseCurrency", "must be a supported ISO 4217 currency") { Currency.of(baseCurrency) }
        return CaptureSnapshotCommand(
            asOf = asOf,
            recordedAt = recordedAt,
            baseCurrency = currency,
            assets = assets.mapIndexed { index, asset -> asset.toCommand(currency, index) },
            liabilities = liabilities.mapIndexed { index, liability -> liability.toCommand(currency, index) },
        )
    }
}

data class CaptureAssetRequest(
    val id: UUID? = null,
    @field:NotBlank @field:Size(max = 200, message = "must contain at most 200 characters") val name: String,
    @field:NotNull val type: AssetType,
    @field:NotNull val liquidity: Liquidity,
    @field:Valid @field:NotNull val money: MoneyRequest,
    @field:NotNull val effectiveAt: Instant,
    @field:NotBlank @field:Size(max = 100) val source: String,
    @field:Valid val manualConversion: ManualConversionRequest? = null,
) {
    fun toCommand(baseCurrency: Currency, index: Int): CaptureAsset =
        CaptureAsset(
            id = id?.let(::AssetId),
            name = name,
            type = type,
            liquidity = liquidity,
            money = money.toDomain(baseCurrency, "assets[$index].money"),
            effectiveAt = effectiveAt,
            source = validated("assets[$index].source", "must contain at most 100 characters") { ValuationSource.of(source) },
            manualConversion = manualConversion?.toDomain("assets[$index].manualConversion"),
        )
}

data class CaptureLiabilityRequest(
    val id: UUID? = null,
    @field:NotBlank @field:Size(max = 200, message = "must contain at most 200 characters") val name: String,
    @field:Valid @field:NotNull val money: MoneyRequest,
    @field:NotNull val effectiveAt: Instant,
    @field:NotBlank @field:Size(max = 100) val source: String,
    @field:Valid val manualConversion: ManualConversionRequest? = null,
) {
    fun toCommand(baseCurrency: Currency, index: Int): CaptureLiability =
        CaptureLiability(
            id = id?.let(::LiabilityId),
            name = name,
            money = money.toDomain(baseCurrency, "liabilities[$index].money"),
            effectiveAt = effectiveAt,
            source = validated("liabilities[$index].source", "must contain at most 100 characters") { LiabilitySource.of(source) },
            manualConversion = manualConversion?.toDomain("liabilities[$index].manualConversion"),
        )
}

data class ManualConversionRequest(
    @field:Valid @field:NotNull val originalMoney: MoneyRequest,
    @field:NotBlank @field:Size(max = 200) val exchangeRateBasis: String,
    @field:NotNull val effectiveAt: Instant,
) {
    fun toDomain(field: String): ManualConversion =
        validated(field, "contains invalid values") {
            ManualConversion.of(
                originalValue = originalMoney.toDomain("$field.originalMoney"),
                exchangeRateBasis = exchangeRateBasis,
                effectiveAt = effectiveAt,
            )
        }
}

private fun MoneyRequest.toDomain(baseCurrency: Currency, field: String): Money {
    if (currency != baseCurrency.code) throw RequestValidationException("$field.currency", "must match baseCurrency")
    if (!amount.matches(Regex("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?$"))) {
        throw RequestValidationException("$field.amount", "must be a decimal string")
    }
    return validated("$field.amount", "must use the currency's supported precision") {
        Money.of(amount.toBigDecimal(), baseCurrency)
    }
}

internal fun MoneyRequest.toDomain(field: String): Money {
    if (!currency.matches(UPPERCASE_CURRENCY_CODE)) {
        throw RequestValidationException("$field.currency", "must be an uppercase ISO 4217 currency code")
    }
    val validatedCurrency = validated("$field.currency", "must be a supported ISO 4217 currency") { Currency.of(currency) }
    if (!amount.matches(Regex("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?$"))) {
        throw RequestValidationException("$field.amount", "must be a decimal string")
    }
    return validated("$field.amount", "must use the currency's supported precision") {
        Money.of(amount.toBigDecimal(), validatedCurrency)
    }
}

private inline fun <T> validated(field: String, message: String, block: () -> T): T =
    try {
        block()
    } catch (_: IllegalArgumentException) {
        throw RequestValidationException(field, message)
    } catch (_: ArithmeticException) {
        throw RequestValidationException(field, message)
    }

private val UPPERCASE_CURRENCY_CODE = Regex("^[A-Z]{3}$")
