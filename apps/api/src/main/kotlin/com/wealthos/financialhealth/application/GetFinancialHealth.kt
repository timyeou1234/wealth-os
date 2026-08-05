package com.wealthos.financialhealth.application

import com.wealthos.domain.financialhealth.FinancialHealthCalculator
import com.wealthos.domain.financialhealth.FinancialHealthResult
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import com.wealthos.identity.application.CurrentUserIdProvider
import org.springframework.stereotype.Service

@Service
class GetFinancialHealth(
    private val snapshotRepository: SnapshotRepository,
    private val currentUser: CurrentUserIdProvider,
) {
    fun execute(snapshotId: SnapshotId): FinancialHealthView {
        val snapshot = snapshotRepository.findEffectiveById(currentUser.get(), snapshotId)
            ?: throw SnapshotNotFoundException(snapshotId)
        return FinancialHealthView(snapshot, FinancialHealthCalculator.calculate(snapshot))
    }
}

data class FinancialHealthView(
    val snapshot: com.wealthos.domain.snapshot.Snapshot,
    val result: FinancialHealthResult,
)

class SnapshotNotFoundException(
    val snapshotId: SnapshotId,
) : RuntimeException("Snapshot $snapshotId was not found")
