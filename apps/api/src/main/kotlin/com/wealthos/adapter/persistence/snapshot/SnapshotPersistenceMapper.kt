package com.wealthos.adapter.persistence.snapshot

import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.liability.domain.LiabilityBalance
import com.wealthos.liability.domain.LiabilityId
import com.wealthos.liability.domain.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.shared.ManualConversion
import com.wealthos.domain.shared.AppliedConversion
import com.wealthos.domain.shared.FxRateType
import java.math.RoundingMode
import com.wealthos.domain.snapshot.CorrectionReason
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotAssetPosition
import com.wealthos.domain.snapshot.SnapshotCorrection
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotLiabilityPosition
import com.wealthos.identity.domain.UserId

internal object SnapshotPersistenceMapper {
    fun snapshotEntity(
        ownerId: UserId,
        snapshot: Snapshot,
    ): SnapshotJpaEntity =
        SnapshotJpaEntity(
            id = snapshot.id.value,
            ownerId = ownerId.value,
            asOf = snapshot.asOf,
            recordedAt = snapshot.recordedAt,
            baseCurrency = snapshot.baseCurrency?.code,
            supersedesId = snapshot.supersedes?.value,
            correctionReason = snapshot.correction?.reason?.value,
        )

    fun assetEntities(snapshot: Snapshot): List<SnapshotAssetPositionJpaEntity> =
        snapshot.assetPositions.map { position ->
            SnapshotAssetPositionJpaEntity(
                id =
                    SnapshotAssetPositionJpaId(
                        snapshotId = snapshot.id.value,
                        assetId = position.assetId.value,
                    ),
                name = position.name,
                assetType = position.type.name,
                liquidity = position.liquidity.name,
                amount = position.valuation.value.amount,
                currency = position.valuation.value.currency.code,
                effectiveAt = position.valuation.effectiveAt,
                source = position.valuation.source.value,
                conversionOriginalAmount = position.valuation.manualConversion?.originalValue?.amount,
                conversionOriginalCurrency = position.valuation.manualConversion?.originalValue?.currency?.code,
                conversionExchangeRateBasis = position.valuation.manualConversion?.exchangeRateBasis,
                conversionEffectiveAt = position.valuation.manualConversion?.effectiveAt,
                appliedOriginalAmount = position.valuation.appliedConversion?.originalMoney?.amount,
                appliedOriginalCurrency = position.valuation.appliedConversion?.originalMoney?.currency?.code,
                appliedRate = position.valuation.appliedConversion?.rate,
                appliedRateDate = position.valuation.appliedConversion?.rateDate,
                appliedProvider = position.valuation.appliedConversion?.provider,
                appliedRateType = position.valuation.appliedConversion?.rateType?.name,
                appliedBasis = position.valuation.appliedConversion?.basis,
                appliedRoundingMode = position.valuation.appliedConversion?.roundingMode?.name,
            )
        }

    fun liabilityEntities(snapshot: Snapshot): List<SnapshotLiabilityPositionJpaEntity> =
        snapshot.liabilityPositions.map { position ->
            SnapshotLiabilityPositionJpaEntity(
                id =
                    SnapshotLiabilityPositionJpaId(
                        snapshotId = snapshot.id.value,
                        liabilityId = position.liabilityId.value,
                    ),
                name = position.name,
                amount = position.balance.balance.amount,
                currency = position.balance.balance.currency.code,
                effectiveAt = position.balance.effectiveAt,
                source = position.balance.source.value,
                conversionOriginalAmount = position.balance.manualConversion?.originalValue?.amount,
                conversionOriginalCurrency = position.balance.manualConversion?.originalValue?.currency?.code,
                conversionExchangeRateBasis = position.balance.manualConversion?.exchangeRateBasis,
                conversionEffectiveAt = position.balance.manualConversion?.effectiveAt,
                appliedOriginalAmount = position.balance.appliedConversion?.originalMoney?.amount,
                appliedOriginalCurrency = position.balance.appliedConversion?.originalMoney?.currency?.code,
                appliedRate = position.balance.appliedConversion?.rate,
                appliedRateDate = position.balance.appliedConversion?.rateDate,
                appliedProvider = position.balance.appliedConversion?.provider,
                appliedRateType = position.balance.appliedConversion?.rateType?.name,
                appliedBasis = position.balance.appliedConversion?.basis,
                appliedRoundingMode = position.balance.appliedConversion?.roundingMode?.name,
            )
        }

    fun domain(
        snapshot: SnapshotJpaEntity,
        assets: List<SnapshotAssetPositionJpaEntity>,
        liabilities: List<SnapshotLiabilityPositionJpaEntity>,
    ): Snapshot =
        Snapshot.reconstitute(
            id = SnapshotId(snapshot.id),
            asOf = snapshot.asOf,
            recordedAt = snapshot.recordedAt,
            baseCurrency = snapshot.baseCurrency?.let(Currency::of),
            assetPositions = assets.map(SnapshotPersistenceMapper::assetPosition),
            liabilityPositions = liabilities.map(SnapshotPersistenceMapper::liabilityPosition),
            correction =
                snapshot.supersedesId?.let { predecessorId ->
                    SnapshotCorrection(
                        supersedes = SnapshotId(predecessorId),
                        reason = CorrectionReason.of(requireNotNull(snapshot.correctionReason)),
                    )
                },
        )

    private fun assetPosition(entity: SnapshotAssetPositionJpaEntity): SnapshotAssetPosition {
        val assetId = AssetId(entity.id.assetId)
        return SnapshotAssetPosition.of(
            assetId = assetId,
            name = entity.name,
            type = AssetType.valueOf(entity.assetType),
            liquidity = Liquidity.valueOf(entity.liquidity),
            valuation =
                AssetValuation(
                    assetId = assetId,
                    value = Money.of(entity.amount, Currency.of(entity.currency)),
                    effectiveAt = entity.effectiveAt,
                    source = ValuationSource.of(entity.source),
                    manualConversion = entity.manualConversion(),
                    appliedConversion = entity.appliedConversion(),
                ),
        )
    }

    private fun liabilityPosition(entity: SnapshotLiabilityPositionJpaEntity): SnapshotLiabilityPosition {
        val liabilityId = LiabilityId(entity.id.liabilityId)
        return SnapshotLiabilityPosition.of(
            liabilityId = liabilityId,
            name = entity.name,
            balance =
                LiabilityBalance(
                    liabilityId = liabilityId,
                    balance = Money.of(entity.amount, Currency.of(entity.currency)),
                    effectiveAt = entity.effectiveAt,
                    source = LiabilitySource.of(entity.source),
                    manualConversion = entity.manualConversion(),
                    appliedConversion = entity.appliedConversion(),
                ),
        )
    }

    private fun SnapshotAssetPositionJpaEntity.manualConversion(): ManualConversion? =
        conversionOriginalAmount?.let { amount ->
            ManualConversion.of(
                originalValue = Money.of(amount, Currency.of(requireNotNull(conversionOriginalCurrency))),
                exchangeRateBasis = requireNotNull(conversionExchangeRateBasis),
                effectiveAt = requireNotNull(conversionEffectiveAt),
            )
        }

    private fun SnapshotLiabilityPositionJpaEntity.manualConversion(): ManualConversion? =
        conversionOriginalAmount?.let { amount ->
            ManualConversion.of(
                originalValue = Money.of(amount, Currency.of(requireNotNull(conversionOriginalCurrency))),
                exchangeRateBasis = requireNotNull(conversionExchangeRateBasis),
                effectiveAt = requireNotNull(conversionEffectiveAt),
            )
        }

    private fun SnapshotAssetPositionJpaEntity.appliedConversion(): AppliedConversion? =
        appliedOriginalAmount?.let { amount ->
            require(appliedRoundingMode == RoundingMode.HALF_EVEN.name) { "Unsupported applied rounding mode" }
            AppliedConversion.of(
                Money.of(amount, Currency.of(requireNotNull(appliedOriginalCurrency))),
                requireNotNull(appliedRate),
                requireNotNull(appliedRateDate),
                requireNotNull(appliedProvider),
                FxRateType.valueOf(requireNotNull(appliedRateType)),
                appliedBasis,
            )
        }

    private fun SnapshotLiabilityPositionJpaEntity.appliedConversion(): AppliedConversion? =
        appliedOriginalAmount?.let { amount ->
            require(appliedRoundingMode == RoundingMode.HALF_EVEN.name) { "Unsupported applied rounding mode" }
            AppliedConversion.of(
                Money.of(amount, Currency.of(requireNotNull(appliedOriginalCurrency))),
                requireNotNull(appliedRate),
                requireNotNull(appliedRateDate),
                requireNotNull(appliedProvider),
                FxRateType.valueOf(requireNotNull(appliedRateType)),
                appliedBasis,
            )
        }
}
