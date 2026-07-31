package com.wealthos.domain.snapshot

import java.time.Instant

interface SnapshotRepository {
    fun save(snapshot: Snapshot): Snapshot

    fun findById(id: SnapshotId): Snapshot?

    fun findEffectiveById(id: SnapshotId): Snapshot?

    fun findEffectiveBetween(
        fromInclusive: Instant,
        toExclusive: Instant,
    ): List<Snapshot>
}
