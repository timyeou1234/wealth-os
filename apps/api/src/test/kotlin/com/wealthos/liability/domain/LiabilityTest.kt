package com.wealthos.liability.domain

import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LiabilityTest {
    @Test
    fun `entity equality is based on identity`() {
        val id = LiabilityId(UUID.randomUUID())

        assertEquals(Liability(id, "Mortgage"), Liability(id, "Home mortgage"))
    }

    @Test
    fun `rejects a blank name and negative balance`() {
        assertFailsWith<IllegalArgumentException> {
            Liability(LiabilityId.new(), " ")
        }

        assertFailsWith<IllegalArgumentException> {
            LiabilityBalance(
                liabilityId = LiabilityId.new(),
                balance = Money.of(BigDecimal("-1.00"), Currency.of("USD")),
                effectiveAt = Instant.parse("2026-07-27T00:00:00Z"),
                source = LiabilitySource.of("manual"),
            )
        }
    }

    @Test
    fun `normalizes liability provenance`() {
        assertEquals("bank statement", LiabilitySource.of("  bank statement  ").value)
    }

    @Test
    fun `rejects invalid liability provenance`() {
        assertFailsWith<IllegalArgumentException> {
            LiabilitySource.of(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            LiabilitySource.of("a".repeat(101))
        }
    }
}
