package com.wealthos.domain.asset

import java.util.UUID

@JvmInline
value class AssetId(val value: UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun new(): AssetId = AssetId(UUID.randomUUID())

        fun from(value: String): AssetId = AssetId(UUID.fromString(value))
    }
}
