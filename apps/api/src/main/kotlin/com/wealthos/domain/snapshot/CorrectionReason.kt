package com.wealthos.domain.snapshot

@JvmInline
value class CorrectionReason private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        fun of(value: String): CorrectionReason {
            val normalized = value.trim()
            require(normalized.isNotEmpty()) { "Snapshot correction reason must not be blank" }
            return CorrectionReason(normalized)
        }
    }
}
