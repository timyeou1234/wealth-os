package com.wealthos.domain.snapshot

import com.wealthos.domain.asset.AssetValuation
import com.wealthos.domain.liability.LiabilityBalance
import java.time.Instant

class Snapshot private constructor(
    val id: SnapshotId,
    val asOf: Instant,
    assetValuations: List<AssetValuation>,
    liabilityBalances: List<LiabilityBalance>,
) {
    val assetValuations: List<AssetValuation> = assetValuations.toList()
    val liabilityBalances: List<LiabilityBalance> = liabilityBalances.toList()

    init {
        require(this.assetValuations.all { !it.effectiveAt.isAfter(asOf) }) {
            "Asset valuation cannot be effective after the snapshot"
        }
        require(this.liabilityBalances.all { !it.effectiveAt.isAfter(asOf) }) {
            "Liability balance cannot be effective after the snapshot"
        }
        require(this.assetValuations.map { it.assetId }.distinct().size == this.assetValuations.size) {
            "Snapshot must contain at most one valuation for each asset"
        }
        require(this.liabilityBalances.map { it.liabilityId }.distinct().size == this.liabilityBalances.size) {
            "Snapshot must contain at most one balance for each liability"
        }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is Snapshot && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun create(
            id: SnapshotId,
            asOf: Instant,
            assetValuations: List<AssetValuation> = emptyList(),
            liabilityBalances: List<LiabilityBalance> = emptyList(),
        ): Snapshot = Snapshot(id, asOf, assetValuations, liabilityBalances)
    }
}
