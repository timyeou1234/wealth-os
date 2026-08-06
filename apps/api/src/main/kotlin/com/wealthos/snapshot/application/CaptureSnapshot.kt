package com.wealthos.snapshot.application

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetRepository
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityBalance
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilityRepository
import com.wealthos.liability.domain.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.ManualConversion
import com.wealthos.domain.shared.Money
import com.wealthos.domain.shared.AppliedConversion
import com.wealthos.domain.shared.CanonicalValuationCurrency
import com.wealthos.domain.shared.FxRateType
import com.wealthos.fxrate.application.GetFxRates
import com.wealthos.identity.application.CurrentUserIdProvider
import com.wealthos.identity.domain.UserId
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import com.wealthos.shared.application.RequestValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId

@Service
class CaptureSnapshot(
    private val assets: AssetRepository,
    private val liabilities: LiabilityRepository,
    private val snapshots: SnapshotRepository,
    private val getFxRates: GetFxRates,
    private val currentUser: CurrentUserIdProvider,
) {
    @Transactional
    fun execute(command: CaptureSnapshotCommand): Snapshot {
        if (command.recordedAt.isBefore(command.asOf)) {
            throw RequestValidationException("recordedAt", "must not be before asOf")
        }
        val ownerId = currentUser.get()
        val activeAssets = assets.findAll(ownerId).filterNot(Asset::archived)
        val activeLiabilities = liabilities.findAll(ownerId).filterNot(Liability::archived)
        validateAssetIds(ownerId, command.assets)
        validateLiabilityIds(ownerId, command.liabilities)
        requireCompleteActiveSet(activeAssets.map(Asset::id).toSet(), command.assets.mapNotNull(CaptureAsset::id), "assets", "asset")
        requireCompleteActiveSet(activeLiabilities.map(Liability::id).toSet(), command.liabilities.mapNotNull(CaptureLiability::id), "liabilities", "liability")

        val capturedAssets =
            command.assets.map { input ->
                assets.save(ownerId, Asset(input.id ?: AssetId.new(), input.name, input.type, input.liquidity))
            }
        val capturedLiabilities =
            command.liabilities.map { input ->
                liabilities.save(ownerId, Liability(input.id ?: LiabilityId.new(), input.name))
            }

        val snapshot =
            Snapshot.capture(
                id = SnapshotId.new(),
                asOf = command.asOf,
                recordedAt = command.recordedAt,
                baseCurrency = command.baseCurrency,
                assets = capturedAssets,
                assetValuations =
                    capturedAssets.zip(command.assets).mapIndexed { index, (asset, input) ->
                        val fact = resolveFact(input.money, input.originalMoney, input.declaredRate, input.effectiveAt, input.manualConversion, command, "assets[$index]")
                        AssetValuation(asset.id, fact.money, input.effectiveAt, input.source, input.manualConversion, fact.appliedConversion)
                    },
                liabilities = capturedLiabilities,
                liabilityBalances =
                    capturedLiabilities.zip(command.liabilities).mapIndexed { index, (liability, input) ->
                        val fact = resolveFact(input.money, input.originalMoney, input.declaredRate, input.effectiveAt, input.manualConversion, command, "liabilities[$index]")
                        LiabilityBalance(liability.id, fact.money, input.effectiveAt, input.source, input.manualConversion, fact.appliedConversion)
                    },
            )
        return snapshots.save(ownerId, snapshot)
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

    private fun validateAssetIds(
        ownerId: UserId,
        inputs: List<CaptureAsset>,
    ) {
        inputs.forEachIndexed { index, input ->
            input.id?.let { id ->
                if (assets.findById(ownerId, id)?.archived != false) {
                    throw RequestValidationException("assets[$index].id", "must identify an active asset")
                }
            }
        }
    }

    private fun validateLiabilityIds(
        ownerId: UserId,
        inputs: List<CaptureLiability>,
    ) {
        inputs.forEachIndexed { index, input ->
            input.id?.let { id ->
                if (liabilities.findById(ownerId, id)?.archived != false) {
                    throw RequestValidationException("liabilities[$index].id", "must identify an active liability")
                }
            }
        }
    }

    private fun resolveFact(
        money: Money?,
        originalMoney: Money?,
        declaredRate: DeclaredFxRate?,
        effectiveAt: Instant,
        manualConversion: ManualConversion?,
        command: CaptureSnapshotCommand,
        field: String,
    ): ResolvedFact {
        if (originalMoney != null) {
            if (command.baseCurrency != CanonicalValuationCurrency.TWD) {
                throw RequestValidationException("baseCurrency", "must be TWD when originalMoney is supplied")
            }
            if (money != null || manualConversion != null) {
                throw RequestValidationException(field, "must use either originalMoney or legacy money conversion fields")
            }
            if (originalMoney.amount.signum() < 0) {
                throw RequestValidationException("$field.originalMoney.amount", "must be a non-negative decimal amount")
            }
            if (effectiveAt.isAfter(command.asOf)) {
                throw RequestValidationException("$field.effectiveAt", "must not be after asOf")
            }
            if (originalMoney.currency == CanonicalValuationCurrency.TWD) {
                if (declaredRate != null) {
                    throw RequestValidationException("$field.declaredRate", "must be omitted for TWD originalMoney")
                }
                return ResolvedFact(originalMoney, null)
            }
            val asOfDate = command.asOf.atZone(TAIPEI).toLocalDate()
            if (declaredRate != null) {
                if (declaredRate.rateDate.isAfter(asOfDate)) {
                    throw RequestValidationException("$field.declaredRate.rateDate", "must not be after the snapshot date")
                }
                val conversion = AppliedConversion.of(
                    originalMoney,
                    declaredRate.rate,
                    declaredRate.rateDate,
                    "USER",
                    FxRateType.USER_DECLARED,
                    declaredRate.basis,
                )
                return ResolvedFact(conversion.toTwdMoney(), conversion)
            }
            val resolved = getFxRates.execute(asOfDate, listOf(originalMoney.currency)).rates.singleOrNull()
                ?: throw RequestValidationException("$field.originalMoney.currency", "has no CBC rate on or before the snapshot date")
            val conversion = AppliedConversion.of(
                originalMoney,
                resolved.rate,
                resolved.rateDate,
                resolved.provider,
                FxRateType.REFERENCE_RATE,
            )
            return ResolvedFact(conversion.toTwdMoney(), conversion)
        }
        if (declaredRate != null) {
            throw RequestValidationException("$field.declaredRate", "requires originalMoney")
        }
        val legacyMoney = money ?: throw RequestValidationException("$field.money", "or originalMoney is required")
        if (legacyMoney.currency != command.baseCurrency) {
            throw RequestValidationException("$field.money.currency", "must match baseCurrency")
        }
        if (legacyMoney.amount.signum() < 0) {
            throw RequestValidationException("$field.money.amount", "must be a non-negative decimal amount")
        }
        if (effectiveAt.isAfter(command.asOf)) {
            throw RequestValidationException("$field.effectiveAt", "must not be after asOf")
        }
        if (manualConversion?.originalValue?.currency == command.baseCurrency) {
            throw RequestValidationException(
                "$field.manualConversion.originalMoney.currency",
                "must differ from baseCurrency",
            )
        }
        if (manualConversion?.effectiveAt?.isAfter(command.asOf) == true) {
            throw RequestValidationException(
                "$field.manualConversion.effectiveAt",
                "must not be after asOf",
            )
        }
        return ResolvedFact(legacyMoney, null)
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
    val money: Money?,
    val originalMoney: Money?,
    val declaredRate: DeclaredFxRate?,
    val effectiveAt: Instant,
    val source: ValuationSource,
    val manualConversion: ManualConversion?,
)

data class CaptureLiability(
    val id: LiabilityId?,
    val name: String,
    val money: Money?,
    val originalMoney: Money?,
    val declaredRate: DeclaredFxRate?,
    val effectiveAt: Instant,
    val source: LiabilitySource,
    val manualConversion: ManualConversion?,
)

private data class ResolvedFact(val money: Money, val appliedConversion: AppliedConversion?)

data class DeclaredFxRate(val rate: java.math.BigDecimal, val rateDate: java.time.LocalDate, val basis: String)

private val TAIPEI: ZoneId = ZoneId.of("Asia/Taipei")
