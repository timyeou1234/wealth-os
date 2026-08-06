package com.wealthos.domain.snapshot

import com.wealthos.liability.domain.Liability
import com.wealthos.liability.domain.LiabilityBalance
import com.wealthos.liability.domain.LiabilityId
import kotlin.ConsistentCopyVisibility

@ConsistentCopyVisibility
data class SnapshotLiabilityPosition private constructor(
    val liabilityId: LiabilityId,
    val name: String,
    val balance: LiabilityBalance,
) {
    init {
        require(name.isNotEmpty()) { "Snapshot liability name must not be blank" }
        require(liabilityId == balance.liabilityId) {
            "Snapshot liability identity must match its balance identity"
        }
    }

    companion object {
        fun capture(
            liability: Liability,
            balance: LiabilityBalance,
        ): SnapshotLiabilityPosition =
            of(
                liabilityId = liability.id,
                name = liability.name,
                balance = balance,
            )

        fun of(
            liabilityId: LiabilityId,
            name: String,
            balance: LiabilityBalance,
        ): SnapshotLiabilityPosition =
            SnapshotLiabilityPosition(
                liabilityId = liabilityId,
                name = name.trim(),
                balance = balance,
            )
    }
}
