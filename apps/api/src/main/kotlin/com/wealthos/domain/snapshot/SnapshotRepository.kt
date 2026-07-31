package com.wealthos.domain.snapshot

interface SnapshotRepository {
    fun save(snapshot: Snapshot): Snapshot

    fun findById(id: SnapshotId): Snapshot?

    fun findEffectiveById(id: SnapshotId): Snapshot?
}
