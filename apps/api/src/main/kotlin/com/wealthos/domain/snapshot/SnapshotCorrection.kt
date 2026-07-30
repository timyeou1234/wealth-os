package com.wealthos.domain.snapshot

data class SnapshotCorrection(
    val supersedes: SnapshotId,
    val reason: CorrectionReason,
)
