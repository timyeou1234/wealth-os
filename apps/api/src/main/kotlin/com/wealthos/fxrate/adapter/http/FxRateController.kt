package com.wealthos.fxrate.adapter.http

import com.wealthos.configuration.ApiDocumentationConfiguration
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.CanonicalValuationCurrency
import com.wealthos.fxrate.application.GetFxRates
import com.wealthos.fxrate.application.SyncFxRates
import com.wealthos.shared.application.RequestValidationException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/v1/fx-rates")
@Tag(name = "Foreign Exchange Rates")
class FxRateController(
    private val getFxRates: GetFxRates,
    private val syncFxRates: SyncFxRates,
) {
    @GetMapping
    @Operation(summary = "Resolve TWD valuation rates on or before a date", operationId = "getFxRates")
    fun get(
        @Parameter(example = "2026-07-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) asOf: LocalDate,
        @Parameter(example = "USD,JPY,HKD") @RequestParam currencies: List<String>,
    ): FxRateResponse {
        val requested = currencies.flatMap { it.split(',') }.map(String::trim).distinct()
        requested.forEachIndexed { index, code ->
            try {
                Currency.of(code)
            } catch (_: IllegalArgumentException) {
                throw RequestValidationException("currencies[$index]", "must be a supported uppercase ISO 4217 currency")
            }
        }
        val view = getFxRates.execute(asOf, requested.map(Currency::of))
        return FxRateResponse(
            valuationCurrency = CanonicalValuationCurrency.TWD.code,
            asOf = asOf,
            rates =
                view.rates.map {
                    FxRateItemResponse(
                        it.originalCurrency.code,
                        it.rate.stripTrailingZeros().toPlainString(),
                        it.rateDate,
                        it.provider,
                        "REFERENCE_RATE",
                    )
                },
            missingCurrencies = view.missingCurrencies.map(Currency::code),
        )
    }

    @PostMapping("/sync")
    @Operation(summary = "Synchronize CBC foreign-exchange rates", operationId = "syncFxRates")
    @SecurityRequirement(name = ApiDocumentationConfiguration.OPERATIONAL_M2M_BEARER)
    fun sync(
        @RequestBody(required = false) request: FxRateSyncRequest?,
    ): FxRateSyncResponse {
        val to = request?.to ?: LocalDate.now(TAIPEI)
        val from = request?.from ?: to.minusDays(DEFAULT_MANUAL_SYNC_DAYS)
        if (to.isBefore(from)) throw RequestValidationException("to", "must not be before from")
        if (ChronoUnit.DAYS.between(from, to) > MAX_MANUAL_SYNC_DAYS) {
            throw RequestValidationException("to", "date range must not exceed $MAX_MANUAL_SYNC_DAYS days")
        }
        val result = syncFxRates.execute(from, to)
        return FxRateSyncResponse(result.provider, result.inserted, result.unchanged, result.latestRateDate)
    }

    private companion object {
        const val MAX_MANUAL_SYNC_DAYS = 366L
        const val DEFAULT_MANUAL_SYNC_DAYS = 30L
        val TAIPEI = java.time.ZoneId.of("Asia/Taipei")
    }
}

data class FxRateSyncRequest(
    @field:Schema(example = "2026-07-01") val from: LocalDate? = null,
    @field:Schema(example = "2026-07-31") val to: LocalDate? = null,
)

data class FxRateSyncResponse(
    @field:Schema(example = "CBC") val provider: String,
    @field:Schema(example = "330") val inserted: Int,
    @field:Schema(example = "0") val unchanged: Int,
    @field:Schema(example = "2026-07-31") val latestRateDate: LocalDate?,
)

data class FxRateResponse(
    val valuationCurrency: String,
    val asOf: LocalDate,
    val rates: List<FxRateItemResponse>,
    val missingCurrencies: List<String>,
)

data class FxRateItemResponse(
    @field:Schema(example = "USD") val originalCurrency: String,
    @field:Schema(example = "32.292") val rate: String,
    @field:Schema(example = "2026-07-31") val rateDate: LocalDate,
    @field:Schema(example = "CBC") val provider: String,
    @field:Schema(example = "REFERENCE_RATE") val rateType: String,
)
