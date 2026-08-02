package com.wealthos.snapshot.application

import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import org.springframework.stereotype.Service

@Service
class SnapshotApplication(
    private val repository: SnapshotRepository,
) {
    fun save(snapshot: Snapshot): Snapshot = repository.save(snapshot)

    fun get(id: SnapshotId): Snapshot = repository.findEffectiveById(id) ?: throw SnapshotNotFoundException(id)

    fun list(): List<Snapshot> = repository.findAllEffective()
}

class SnapshotNotFoundException(val snapshotId: SnapshotId) : RuntimeException("Snapshot $snapshotId was not found")
