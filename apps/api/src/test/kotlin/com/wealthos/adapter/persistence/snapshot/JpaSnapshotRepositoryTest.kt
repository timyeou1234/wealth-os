package com.wealthos.adapter.persistence.snapshot

import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.AssetValuation
import com.wealthos.asset.domain.Liquidity
import com.wealthos.asset.domain.ValuationSource
import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.CorrectionReason
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@Import(JpaSnapshotRepository::class)
class JpaSnapshotRepositoryTest
    @Autowired
    constructor(
        private val repository: SnapshotRepository,
    ) {
        private val asOf = Instant.parse("2026-07-31T00:00:00Z")

        @Test
        fun `persists and restores a self contained snapshot`() {
            val snapshot = snapshot()

            repository.save(snapshot)

            val restored = requireNotNull(repository.findById(snapshot.id))
            assertEquals(snapshot.id, restored.id)
            assertEquals(snapshot.asOf, restored.asOf)
            assertEquals(snapshot.recordedAt, restored.recordedAt)
            assertEquals(snapshot.assetPositions, restored.assetPositions)
            assertEquals(snapshot.liabilityPositions, restored.liabilityPositions)
            assertNull(restored.correction)
        }

        @Test
        fun `follows the correction chain to its effective snapshot`() {
            val original = snapshot()
            val correction = correction(original, "Corrected balance")

            repository.save(original)
            repository.save(correction)

            assertEquals(original.id, repository.findById(original.id)?.id)
            assertEquals(correction.id, repository.findEffectiveById(original.id)?.id)
            assertEquals(correction.correction, repository.findEffectiveById(original.id)?.correction)
        }

        @Test
        fun `returns effective snapshots within a financial time range`() {
            val first = snapshot(asOf.minusSeconds(120))
            val second = snapshot(asOf.minusSeconds(60))
            val correctedSecond = correction(second, "Corrected timeline balance")
            val outsideRange = snapshot(asOf)
            listOf(first, second, correctedSecond, outsideRange).forEach(repository::save)

            val history = repository.findEffectiveBetween(first.asOf, outsideRange.asOf)

            assertEquals(listOf(first.id, correctedSecond.id), history.map(Snapshot::id))
            assertFailsWith<IllegalArgumentException> {
                repository.findEffectiveBetween(outsideRange.asOf, outsideRange.asOf)
            }
        }

        @Test
        fun `rejects identity reuse missing predecessors and correction branches`() {
            val original = snapshot()
            repository.save(original)

            assertFailsWith<IllegalArgumentException> {
                repository.save(original)
            }

            val missingPredecessor = snapshot()
            assertFailsWith<IllegalArgumentException> {
                repository.save(correction(missingPredecessor, "Missing predecessor"))
            }

            repository.save(correction(original, "First correction"))
            assertFailsWith<IllegalArgumentException> {
                repository.save(correction(original, "Competing correction"))
            }
        }

        private fun snapshot(snapshotAsOf: Instant = asOf): Snapshot {
            val asset = Asset(AssetId.new(), "Emergency fund", AssetType.CASH, Liquidity.LIQUID)
            val liability = Liability(LiabilityId.new(), "Mortgage")
            return Snapshot.capture(
                id = SnapshotId.new(),
                asOf = snapshotAsOf,
                recordedAt = snapshotAsOf,
                assets = listOf(asset),
                assetValuations =
                    listOf(
                        AssetValuation(
                            assetId = asset.id,
                            value = Money.of(BigDecimal("1200.00"), Currency.of("USD")),
                            effectiveAt = snapshotAsOf,
                            source = ValuationSource.of("Bank statement"),
                        ),
                    ),
                liabilities = listOf(liability),
                liabilityBalances =
                    listOf(
                        LiabilityBalance(
                            liabilityId = liability.id,
                            balance = Money.of(BigDecimal("800.00"), Currency.of("USD")),
                            effectiveAt = snapshotAsOf,
                            source = LiabilitySource.of("Loan statement"),
                        ),
                    ),
            )
        }

        private fun correction(
            predecessor: Snapshot,
            reason: String,
        ): Snapshot =
            Snapshot.correction(
                id = SnapshotId.new(),
                supersedes = predecessor,
                recordedAt = predecessor.recordedAt.plusSeconds(60),
                reason = CorrectionReason.of(reason),
                replacementAssetPositions = predecessor.assetPositions,
                replacementLiabilityPositions = predecessor.liabilityPositions,
            )
    }
