package com.wealthos.domain.asset

@JvmInline
value class ValuationSource private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        fun of(value: String): ValuationSource {
            val normalized = value.trim()
            require(normalized.isNotEmpty()) { "Valuation source must not be blank" }
            require(normalized.length <= 100) { "Valuation source must not exceed 100 characters" }
            return ValuationSource(normalized)
        }
    }
}
