package com.wealthos.adapter.persistence.snapshot

import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
class JpaSnapshotRepository(
    private val snapshots: SnapshotJpaRepository,
    private val assetPositions: SnapshotAssetPositionJpaRepository,
    private val liabilityPositions: SnapshotLiabilityPositionJpaRepository,
) : SnapshotRepository {
    @Transactional
    override fun save(snapshot: Snapshot): Snapshot {
        require(!snapshots.existsById(snapshot.id.value)) {
            "Snapshot identity already exists: ${snapshot.id}"
        }
        snapshot.supersedes?.let { predecessorId ->
            val predecessor =
                snapshots.findById(predecessorId.value).orElse(null)
                    ?: throw IllegalArgumentException("Superseded Snapshot does not exist: $predecessorId")
            require(snapshots.findBySupersedesId(predecessorId.value) == null) {
                "Snapshot is not the terminal correction: $predecessorId"
            }
            require(snapshot.asOf == predecessor.asOf) {
                "Snapshot correction must preserve the predecessor's as-of time"
            }
            require(!snapshot.recordedAt.isBefore(predecessor.recordedAt)) {
                "Snapshot correction cannot be recorded before its predecessor"
            }
        }

        snapshots.saveAndFlush(SnapshotPersistenceMapper.snapshotEntity(snapshot))
        assetPositions.saveAll(SnapshotPersistenceMapper.assetEntities(snapshot))
        liabilityPositions.saveAll(SnapshotPersistenceMapper.liabilityEntities(snapshot))
        return snapshot
    }

    @Transactional(readOnly = true)
    override fun findById(id: SnapshotId): Snapshot? =
        snapshots.findById(id.value).orElse(null)?.let(::load)

    @Transactional(readOnly = true)
    override fun findEffectiveById(id: SnapshotId): Snapshot? =
        snapshots.findById(id.value).orElse(null)?.let { load(findTerminal(it)) }

    @Transactional(readOnly = true)
    override fun findEffectiveBetween(
        fromInclusive: Instant,
        toExclusive: Instant,
    ): List<Snapshot> {
        require(fromInclusive.isBefore(toExclusive)) {
            "Snapshot history start must be before its end"
        }
        return snapshots
            .findAllBySupersedesIdIsNullAndAsOfGreaterThanEqualAndAsOfLessThanOrderByAsOfAsc(
                fromInclusive,
                toExclusive,
            ).map { load(findTerminal(it)) }
    }

    @Transactional(readOnly = true)
    override fun findAllEffective(): List<Snapshot> =
        snapshots.findAllBySupersedesIdIsNullOrderByAsOfAsc().map { load(findTerminal(it)) }

    private fun findTerminal(start: SnapshotJpaEntity): SnapshotJpaEntity {
        var current = start
        val visited = mutableSetOf<UUID>()

        while (true) {
            check(visited.add(current.id)) { "Snapshot correction chain contains a cycle" }
            current = snapshots.findBySupersedesId(current.id) ?: return current
        }
    }

    private fun load(entity: SnapshotJpaEntity): Snapshot =
        SnapshotPersistenceMapper.domain(
            snapshot = entity,
            assets = assetPositions.findAllByIdSnapshotId(entity.id),
            liabilities = liabilityPositions.findAllByIdSnapshotId(entity.id),
        )
}
