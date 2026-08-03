package com.wealthos.snapshot.adapter.http

import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.ManualConversion
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.snapshot.application.SnapshotApplication
import com.wealthos.snapshot.application.SnapshotNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/snapshots")
@Tag(name = "Snapshots")
class SnapshotController(private val snapshots: SnapshotApplication) {
    @GetMapping
    @Operation(summary = "List effective snapshots", operationId = "listSnapshots")
    fun list(): List<SnapshotResponse> = snapshots.list().map(SnapshotResponse::from)

    @PostMapping
    @Operation(summary = "Create a snapshot", operationId = "createSnapshot")
    fun create(@Valid @RequestBody request: CreateSnapshotRequest): ResponseEntity<SnapshotResponse> {
        val snapshot = snapshots.save(request.toDomain())
        return ResponseEntity.created(URI.create("/api/v1/snapshots/${snapshot.id.value}")).body(SnapshotResponse.from(snapshot))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a snapshot", operationId = "getSnapshot")
    fun get(@PathVariable id: UUID): SnapshotResponse = SnapshotResponse.from(snapshots.get(SnapshotId(id)))
}

data class CreateSnapshotRequest(
    @field:NotNull @field:Schema(example = "2026-08-01T00:00:00Z") val asOf: Instant,
    @field:NotNull @field:Schema(example = "2026-08-01T00:00:00Z") val recordedAt: Instant,
    @field:Valid val assets: List<AssetFactRequest> = emptyList(),
    @field:Valid val liabilities: List<LiabilityFactRequest> = emptyList(),
) {
    fun toDomain(): Snapshot {
        val assetIds = assets.map { AssetId(UUID.fromString(it.id)) }
        val liabilityIds = liabilities.map { LiabilityId(UUID.fromString(it.id)) }
        return Snapshot.capture(
            id = SnapshotId.new(), asOf = asOf, recordedAt = recordedAt,
            assets = assets.mapIndexed { i, a -> com.wealthos.asset.domain.Asset(assetIds[i], a.name, a.type, a.liquidity) },
            assetValuations = assets.mapIndexed { i, a -> AssetValuation(assetIds[i], a.money.toDomain("assets[$i].money"), a.effectiveAt, ValuationSource.of(a.source), a.manualConversion?.toDomain("assets[$i].manualConversion")) },
            liabilities = liabilities.mapIndexed { i, l -> com.wealthos.domain.liability.Liability(liabilityIds[i], l.name) },
            liabilityBalances = liabilities.mapIndexed { i, l -> LiabilityBalance(liabilityIds[i], l.money.toDomain("liabilities[$i].money"), l.effectiveAt, LiabilitySource.of(l.source), l.manualConversion?.toDomain("liabilities[$i].manualConversion")) },
        )
    }
}

data class MoneyRequest(
    @field:NotBlank @field:Schema(example = "1000.00") val amount: String,
    @field:NotBlank @field:Schema(example = "USD") val currency: String,
)
data class AssetFactRequest(
    @field:Pattern(regexp = "^[0-9a-fA-F-]{36}$") @field:Schema(example = "0f27e4fa-99f8-4c5e-87da-527488cbe515") val id: String,
    @field:NotBlank @field:Schema(example = "Cash") val name: String,
    @field:NotNull val type: AssetType,
    @field:NotNull val liquidity: Liquidity,
    @field:Valid @field:NotNull val money: MoneyRequest,
    @field:NotNull @field:Schema(example = "2026-08-01T00:00:00Z") val effectiveAt: Instant,
    @field:NotBlank @field:Schema(example = "bank") val source: String,
    @field:Valid val manualConversion: ManualConversionRequest? = null,
)
data class LiabilityFactRequest(
    @field:Pattern(regexp = "^[0-9a-fA-F-]{36}$") @field:Schema(example = "0f27e4fa-99f8-4c5e-87da-527488cbe515") val id: String,
    @field:NotBlank @field:Schema(example = "Home Mortgage") val name: String,
    @field:Valid @field:NotNull val money: MoneyRequest,
    @field:NotNull @field:Schema(example = "2026-08-01T00:00:00Z") val effectiveAt: Instant,
    @field:NotBlank @field:Schema(example = "bank") val source: String,
    @field:Valid val manualConversion: ManualConversionRequest? = null,
)
data class SnapshotResponse(val id: String, val asOf: Instant, val recordedAt: Instant, val assets: List<AssetFactResponse>, val liabilities: List<LiabilityFactResponse>) {
    companion object {
        fun from(s: Snapshot) = SnapshotResponse(s.id.value.toString(), s.asOf, s.recordedAt, s.assetPositions.map { AssetFactResponse(it.assetId.value.toString(), it.name, it.type, it.liquidity, MoneyResponse(it.valuation.value.amount.toPlainString(), it.valuation.value.currency.code), it.valuation.effectiveAt, it.valuation.source.value, it.valuation.manualConversion?.let(ManualConversionResponse::from)) }, s.liabilityPositions.map { LiabilityFactResponse(it.liabilityId.value.toString(), it.name, MoneyResponse(it.balance.balance.amount.toPlainString(), it.balance.balance.currency.code), it.balance.effectiveAt, it.balance.source.value, it.balance.manualConversion?.let(ManualConversionResponse::from)) })
    }
}
data class MoneyResponse(val amount: String, val currency: String)
data class ManualConversionResponse(val originalMoney: MoneyResponse, val exchangeRateBasis: String, val effectiveAt: Instant) {
    companion object {
        fun from(conversion: ManualConversion) = ManualConversionResponse(MoneyResponse(conversion.originalValue.amount.toPlainString(), conversion.originalValue.currency.code), conversion.exchangeRateBasis, conversion.effectiveAt)
    }
}
data class AssetFactResponse(val id: String, val name: String, val type: AssetType, val liquidity: Liquidity, val money: MoneyResponse, val effectiveAt: Instant, val source: String, val manualConversion: ManualConversionResponse? = null)
data class LiabilityFactResponse(val id: String, val name: String, val money: MoneyResponse, val effectiveAt: Instant, val source: String, val manualConversion: ManualConversionResponse? = null)
