package com.wealthos.snapshot.application

import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class SnapshotApplication(
    private val repository: SnapshotRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun save(snapshot: Snapshot): Snapshot = repository.save(currentUser.get(), snapshot)

    fun get(id: SnapshotId): Snapshot =
        repository.findEffectiveById(currentUser.get(), id) ?: throw SnapshotNotFoundException(id)

    fun list(): List<Snapshot> = repository.findAllEffective(currentUser.get())
}

class SnapshotNotFoundException(val snapshotId: SnapshotId) : RuntimeException("Snapshot $snapshotId was not found")
