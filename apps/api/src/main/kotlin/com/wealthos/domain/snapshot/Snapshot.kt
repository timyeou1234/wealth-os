package com.wealthos.domain.snapshot

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
            assetPositions: List<SnapshotAssetPosition> = emptyList(),
            liabilityPositions: List<SnapshotLiabilityPosition> = emptyList(),
        ): Snapshot =
            Snapshot(
                id = id,
                asOf = asOf,
                assetPositions = assetPositions,
                liabilityPositions = liabilityPositions,
                supersedes = null,
            )

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
    }
}
