package com.wealthos.domain.snapshot

import com.wealthos.identity.domain.UserId
import java.time.Instant

interface SnapshotRepository {
    fun save(
        ownerId: UserId,
        snapshot: Snapshot,
    ): Snapshot

    fun findById(
        ownerId: UserId,
        id: SnapshotId,
    ): Snapshot?

    fun findEffectiveById(
        ownerId: UserId,
        id: SnapshotId,
    ): Snapshot?

    fun findEffectiveBetween(
        ownerId: UserId,
        fromInclusive: Instant,
        toExclusive: Instant,
    ): List<Snapshot>

    fun findAllEffective(ownerId: UserId): List<Snapshot>
}
