package com.wealthos.snapshot.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilityRepository
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import com.wealthos.shared.application.RequestValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CaptureSnapshot(
    private val assets: AssetRepository,
    private val liabilities: LiabilityRepository,
    private val snapshots: SnapshotRepository,
) {
    @Transactional
    fun execute(command: CaptureSnapshotCommand): Snapshot {
        if (command.recordedAt.isBefore(command.asOf)) {
            throw RequestValidationException("recordedAt", "must not be before asOf")
        }
        val activeAssets = assets.findAll().filterNot(Asset::archived)
        val activeLiabilities = liabilities.findAll().filterNot(Liability::archived)
        validateAssetIds(command.assets)
        validateLiabilityIds(command.liabilities)
        requireCompleteActiveSet(activeAssets.map(Asset::id).toSet(), command.assets.mapNotNull(CaptureAsset::id), "assets", "asset")
        requireCompleteActiveSet(activeLiabilities.map(Liability::id).toSet(), command.liabilities.mapNotNull(CaptureLiability::id), "liabilities", "liability")

        val capturedAssets =
            command.assets.map { input ->
                assets.save(Asset(input.id ?: AssetId.new(), input.name, input.type, input.liquidity))
            }
        val capturedLiabilities =
            command.liabilities.map { input ->
                liabilities.save(Liability(input.id ?: LiabilityId.new(), input.name))
            }

        val snapshot =
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = command.asOf,
                recordedAt = command.recordedAt,
                assets = capturedAssets,
                assetValuations =
                    capturedAssets.zip(command.assets).mapIndexed { index, (asset, input) ->
                        validateFact(input.money, input.effectiveAt, command, "assets[$index]")
                        AssetValuation(asset.id, input.money, input.effectiveAt, input.source)
                    },
                liabilities = capturedLiabilities,
                liabilityBalances =
                    capturedLiabilities.zip(command.liabilities).mapIndexed { index, (liability, input) ->
                        validateFact(input.money, input.effectiveAt, command, "liabilities[$index]")
                        LiabilityBalance(liability.id, input.money, input.effectiveAt, input.source)
                    },
            )
        return snapshots.save(snapshot)
    }

    private fun <T> requireCompleteActiveSet(
        activeIds: Set<T>,
        submittedIds: List<T>,
        field: String,
        resource: String,
    ) {
        if (submittedIds.size != submittedIds.toSet().size) {
            throw RequestValidationException(field, "must not contain duplicate $resource IDs")
        }
        if (submittedIds.toSet() != activeIds) {
            throw RequestValidationException(field, "must include every active $resource exactly once")
        }
    }

    private fun validateAssetIds(inputs: List<CaptureAsset>) {
        inputs.forEachIndexed { index, input ->
            input.id?.let { id ->
                if (assets.findById(id)?.archived != false) {
                    throw RequestValidationException("assets[$index].id", "must identify an active asset")
                }
            }
        }
    }

    private fun validateLiabilityIds(inputs: List<CaptureLiability>) {
        inputs.forEachIndexed { index, input ->
            input.id?.let { id ->
                if (liabilities.findById(id)?.archived != false) {
                    throw RequestValidationException("liabilities[$index].id", "must identify an active liability")
                }
            }
        }
    }

    private fun validateFact(
        money: Money,
        effectiveAt: Instant,
        command: CaptureSnapshotCommand,
        field: String,
    ) {
        if (money.currency != command.baseCurrency) {
            throw RequestValidationException("$field.money.currency", "must match baseCurrency")
        }
        if (money.amount.signum() < 0) {
            throw RequestValidationException("$field.money.amount", "must be a non-negative decimal amount")
        }
        if (effectiveAt.isAfter(command.asOf)) {
            throw RequestValidationException("$field.effectiveAt", "must not be after asOf")
        }
    }
}

data class CaptureSnapshotCommand(
    val asOf: Instant,
    val recordedAt: Instant,
    val baseCurrency: Currency,
    val assets: List<CaptureAsset>,
    val liabilities: List<CaptureLiability>,
)

data class CaptureAsset(
    val id: AssetId?,
    val name: String,
    val type: AssetType,
    val liquidity: Liquidity,
    val money: Money,
    val effectiveAt: Instant,
    val source: ValuationSource,
)

data class CaptureLiability(
    val id: LiabilityId?,
    val name: String,
    val money: Money,
    val effectiveAt: Instant,
    val source: LiabilitySource,
)
