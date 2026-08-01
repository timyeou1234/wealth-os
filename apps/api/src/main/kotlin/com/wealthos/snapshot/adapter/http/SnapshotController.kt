package com.wealthos.snapshot.adapter.http

import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.snapshot.application.SnapshotApplication
import com.wealthos.snapshot.application.SnapshotNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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
    @PostMapping
    @Operation(summary = "Create a snapshot")
    fun create(@Valid @RequestBody request: CreateSnapshotRequest): ResponseEntity<SnapshotResponse> {
        val snapshot = snapshots.save(request.toDomain())
        return ResponseEntity.created(URI.create("/api/v1/snapshots/${snapshot.id.value}")).body(SnapshotResponse.from(snapshot))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a snapshot")
    fun get(@PathVariable id: UUID): SnapshotResponse = SnapshotResponse.from(snapshots.get(SnapshotId(id)))
}

data class CreateSnapshotRequest(val asOf: Instant, val recordedAt: Instant, @field:Valid val assets: List<AssetFactRequest>, @field:Valid val liabilities: List<LiabilityFactRequest>) {
    fun toDomain(): Snapshot {
        val assetIds = assets.map { AssetId(UUID.fromString(it.id)) }
        val liabilityIds = liabilities.map { LiabilityId(UUID.fromString(it.id)) }
        return Snapshot.capture(
            id = SnapshotId.new(), asOf = asOf, recordedAt = recordedAt,
            assets = assets.mapIndexed { i, a -> com.wealthos.asset.domain.Asset(assetIds[i], a.name, a.type, a.liquidity) },
            assetValuations = assets.mapIndexed { i, a -> AssetValuation(assetIds[i], Money.of(a.money.amount.toBigDecimal(), Currency.of(a.money.currency)), a.effectiveAt, ValuationSource.of(a.source)) },
            liabilities = liabilities.mapIndexed { i, l -> com.wealthos.domain.liability.Liability(liabilityIds[i], l.name) },
            liabilityBalances = liabilities.mapIndexed { i, l -> LiabilityBalance(liabilityIds[i], Money.of(l.money.amount.toBigDecimal(), Currency.of(l.money.currency)), l.effectiveAt, LiabilitySource.of(l.source)) },
        )
    }
}

data class MoneyRequest(@field:NotBlank val amount: String, @field:NotBlank val currency: String)
data class AssetFactRequest(@field:Pattern(regexp = "^[0-9a-fA-F-]{36}$") val id: String, @field:NotBlank val name: String, val type: AssetType, val liquidity: Liquidity, @field:Valid val money: MoneyRequest, val effectiveAt: Instant, @field:NotBlank val source: String)
data class LiabilityFactRequest(@field:Pattern(regexp = "^[0-9a-fA-F-]{36}$") val id: String, @field:NotBlank val name: String, @field:Valid val money: MoneyRequest, val effectiveAt: Instant, @field:NotBlank val source: String)
data class SnapshotResponse(val id: String, val asOf: Instant, val recordedAt: Instant, val assets: List<AssetFactResponse>, val liabilities: List<LiabilityFactResponse>) {
    companion object {
        fun from(s: Snapshot) = SnapshotResponse(s.id.value.toString(), s.asOf, s.recordedAt, s.assetPositions.map { AssetFactResponse(it.assetId.value.toString(), it.name, it.type, it.liquidity, MoneyResponse(it.valuation.value.amount.toPlainString(), it.valuation.value.currency.code), it.valuation.effectiveAt, it.valuation.source.value) }, s.liabilityPositions.map { LiabilityFactResponse(it.liabilityId.value.toString(), it.name, MoneyResponse(it.balance.balance.amount.toPlainString(), it.balance.balance.currency.code), it.balance.effectiveAt, it.balance.source.value) })
    }
}
data class MoneyResponse(val amount: String, val currency: String)
data class AssetFactResponse(val id: String, val name: String, val type: AssetType, val liquidity: Liquidity, val money: MoneyResponse, val effectiveAt: Instant, val source: String)
data class LiabilityFactResponse(val id: String, val name: String, val money: MoneyResponse, val effectiveAt: Instant, val source: String)
