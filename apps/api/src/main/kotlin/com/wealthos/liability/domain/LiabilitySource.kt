package com.wealthos.liability.domain

@JvmInline
value class LiabilitySource private constructor(
    val value: String,
) {
    override fun toString(): String = value

    companion object {
        fun of(value: String): LiabilitySource {
            val normalized = value.trim()
            require(normalized.isNotEmpty()) { "Liability source must not be blank" }
            require(normalized.length <= 100) { "Liability source must not exceed 100 characters" }
            return LiabilitySource(normalized)
        }
    }
}
