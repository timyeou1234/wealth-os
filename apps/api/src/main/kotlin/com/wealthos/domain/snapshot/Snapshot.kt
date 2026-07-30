package com.wealthos.domain.snapshot

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.AssetValuation
import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityBalance
import java.time.Instant

class Snapshot private constructor(
    val id: SnapshotId,
    val asOf: Instant,
    assetPositions: List<SnapshotAssetPosition>,
    liabilityPositions: List<SnapshotLiabilityPosition>,
    val supersedes: SnapshotId?,
) {
    val assetPositions: List<SnapshotAssetPosition> = assetPositions.toList()
    val liabilityPositions: List<SnapshotLiabilityPosition> = liabilityPositions.toList()

    init {
        require(this.assetPositions.all { !it.valuation.effectiveAt.isAfter(asOf) }) {
            "Asset valuation cannot be effective after the snapshot"
        }
        require(this.liabilityPositions.all { !it.balance.effectiveAt.isAfter(asOf) }) {
            "Liability balance cannot be effective after the snapshot"
        }
        require(this.assetPositions.map { it.assetId }.distinct().size == this.assetPositions.size) {
            "Snapshot must contain at most one position for each asset"
        }
        require(this.liabilityPositions.map { it.liabilityId }.distinct().size == this.liabilityPositions.size) {
            "Snapshot must contain at most one position for each liability"
        }
        require(supersedes != id) { "Snapshot cannot supersede itself" }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is Snapshot && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun capture(
            id: SnapshotId,
            asOf: Instant,
            assets: Collection<Asset> = emptyList(),
            assetValuations: Collection<AssetValuation> = emptyList(),
            liabilities: Collection<Liability> = emptyList(),
            liabilityBalances: Collection<LiabilityBalance> = emptyList(),
        ): Snapshot {
            val assetsById = assets.associateBy(Asset::id)
            require(assetsById.size == assets.size) {
                "Snapshot capture requires unique asset identities"
            }
            val valuationsByAssetId = assetValuations.associateBy(AssetValuation::assetId)
            require(valuationsByAssetId.size == assetValuations.size) {
                "Snapshot capture requires at most one valuation for each asset"
            }
            require(assetsById.keys == valuationsByAssetId.keys) {
                captureMismatchMessage(
                    positionType = "asset",
                    missingIds = assetsById.keys - valuationsByAssetId.keys,
                    unknownIds = valuationsByAssetId.keys - assetsById.keys,
                )
            }

            val liabilitiesById = liabilities.associateBy(Liability::id)
            require(liabilitiesById.size == liabilities.size) {
                "Snapshot capture requires unique liability identities"
            }
            val balancesByLiabilityId = liabilityBalances.associateBy(LiabilityBalance::liabilityId)
            require(balancesByLiabilityId.size == liabilityBalances.size) {
                "Snapshot capture requires at most one balance for each liability"
            }
            require(liabilitiesById.keys == balancesByLiabilityId.keys) {
                captureMismatchMessage(
                    positionType = "liability",
                    missingIds = liabilitiesById.keys - balancesByLiabilityId.keys,
                    unknownIds = balancesByLiabilityId.keys - liabilitiesById.keys,
                )
            }

            return Snapshot(
                id = id,
                asOf = asOf,
                assetPositions =
                    assets.map { asset ->
                        SnapshotAssetPosition.capture(
                            asset = asset,
                            valuation = valuationsByAssetId.getValue(asset.id),
                        )
                    },
                liabilityPositions =
                    liabilities.map { liability ->
                        SnapshotLiabilityPosition.capture(
                            liability = liability,
                            balance = balancesByLiabilityId.getValue(liability.id),
                        )
                    },
                supersedes = null,
            )
        }

        fun correction(
            id: SnapshotId,
            supersedes: Snapshot,
            assetPositions: List<SnapshotAssetPosition> = emptyList(),
            liabilityPositions: List<SnapshotLiabilityPosition> = emptyList(),
        ): Snapshot =
            Snapshot(
                id = id,
                asOf = supersedes.asOf,
                assetPositions = assetPositions,
                liabilityPositions = liabilityPositions,
                supersedes = supersedes.id,
            )

        private fun captureMismatchMessage(
            positionType: String,
            missingIds: Set<*>,
            unknownIds: Set<*>,
        ): String =
            "Snapshot capture has incomplete $positionType facts: " +
                "missing=$missingIds, unknown=$unknownIds"
    }
}
