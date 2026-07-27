package com.wealthos.domain.snapshot

import java.util.UUID

@JvmInline
value class SnapshotId(val value: UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun new(): SnapshotId = SnapshotId(UUID.randomUUID())

        fun from(value: String): SnapshotId = SnapshotId(UUID.fromString(value))
    }
}
