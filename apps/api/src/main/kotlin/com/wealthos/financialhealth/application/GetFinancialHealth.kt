package com.wealthos.financialhealth.application

import com.wealthos.domain.financialhealth.FinancialHealthCalculator
import com.wealthos.domain.financialhealth.FinancialHealthResult
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import org.springframework.stereotype.Service

@Service
class GetFinancialHealth(
    private val snapshotRepository: SnapshotRepository,
) {
    fun execute(snapshotId: SnapshotId): FinancialHealthResult {
        val snapshot = snapshotRepository.findEffectiveById(snapshotId) ?: throw SnapshotNotFoundException(snapshotId)
        return FinancialHealthCalculator.calculate(snapshot)
    }
}

class SnapshotNotFoundException(
    val snapshotId: SnapshotId,
) : RuntimeException("Snapshot $snapshotId was not found")
