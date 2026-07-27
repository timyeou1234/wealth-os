package com.wealthos.domain.liability

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
            )
        }
    }
}
