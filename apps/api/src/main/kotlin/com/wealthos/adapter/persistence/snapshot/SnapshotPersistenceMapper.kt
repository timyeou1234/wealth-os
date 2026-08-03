package com.wealthos.adapter.persistence.snapshot

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
import com.wealthos.domain.shared.ManualConversion
import com.wealthos.domain.snapshot.CorrectionReason
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotAssetPosition
import com.wealthos.domain.snapshot.SnapshotCorrection
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotLiabilityPosition

internal object SnapshotPersistenceMapper {
    fun snapshotEntity(snapshot: Snapshot): SnapshotJpaEntity =
        SnapshotJpaEntity(
            id = snapshot.id.value,
            asOf = snapshot.asOf,
            recordedAt = snapshot.recordedAt,
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
}
