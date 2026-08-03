package com.wealthos.domain.shared

import java.util.Locale

@JvmInline
value class Currency private constructor(val code: String) {
    val fractionDigits: Int
        get() = if (code == "TWD") 0 else java.util.Currency.getInstance(code).defaultFractionDigits

    override fun toString(): String = code

    companion object {
        fun of(code: String): Currency {
            val normalized = code.trim().uppercase(Locale.ROOT)
            require(normalized.length == 3) { "Currency code must contain exactly three letters" }

            val javaCurrency =
                runCatching { java.util.Currency.getInstance(normalized) }
                    .getOrElse { throw IllegalArgumentException("Unsupported ISO 4217 currency: $normalized", it) }

            require(javaCurrency.defaultFractionDigits >= 0) {
                "Currency must define decimal fraction digits: $normalized"
            }

            return Currency(javaCurrency.currencyCode)
        }
    }
}
