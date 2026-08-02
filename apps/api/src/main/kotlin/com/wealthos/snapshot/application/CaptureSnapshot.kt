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
        val activeAssets = assets.findAll().filterNot(Asset::archived)
        val activeLiabilities = liabilities.findAll().filterNot(Liability::archived)
        requireCompleteActiveSet(activeAssets.map(Asset::id).toSet(), command.assets.mapNotNull(CaptureAsset::id), "Asset")
        requireCompleteActiveSet(activeLiabilities.map(Liability::id).toSet(), command.liabilities.mapNotNull(CaptureLiability::id), "Liability")

        val capturedAssets =
            command.assets.map { input ->
                input.id?.let { id ->
                    require(assets.findById(id)?.archived == false) { "Asset must exist and be active: $id" }
                }
                assets.save(Asset(input.id ?: AssetId.new(), input.name, input.type, input.liquidity))
            }
        val capturedLiabilities =
            command.liabilities.map { input ->
                input.id?.let { id ->
                    require(liabilities.findById(id)?.archived == false) { "Liability must exist and be active: $id" }
                }
                liabilities.save(Liability(input.id ?: LiabilityId.new(), input.name))
            }

        val snapshot =
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = command.asOf,
                recordedAt = command.recordedAt,
                assets = capturedAssets,
                assetValuations =
                    capturedAssets.zip(command.assets).map { (asset, input) ->
                        require(input.money.currency == command.baseCurrency) { "Asset currency must match the base currency" }
                        AssetValuation(asset.id, input.money, input.effectiveAt, input.source)
                    },
                liabilities = capturedLiabilities,
                liabilityBalances =
                    capturedLiabilities.zip(command.liabilities).map { (liability, input) ->
                        require(input.money.currency == command.baseCurrency) { "Liability currency must match the base currency" }
                        LiabilityBalance(liability.id, input.money, input.effectiveAt, input.source)
                    },
            )
        return snapshots.save(snapshot)
    }

    private fun <T> requireCompleteActiveSet(
        activeIds: Set<T>,
        submittedIds: List<T>,
        resource: String,
    ) {
        require(submittedIds.size == submittedIds.toSet().size) { "$resource IDs must not be duplicated" }
        require(submittedIds.toSet() == activeIds) { "Capture must include every active $resource" }
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
