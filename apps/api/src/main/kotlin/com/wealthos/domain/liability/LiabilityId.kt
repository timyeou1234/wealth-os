package com.wealthos.domain.liability

import java.util.UUID

@JvmInline
value class LiabilityId(val value: UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun new(): LiabilityId = LiabilityId(UUID.randomUUID())

        fun from(value: String): LiabilityId = LiabilityId(UUID.fromString(value))
    }
}
