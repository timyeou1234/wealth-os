package com.wealthos.fxrate.adapter.provider

import com.fasterxml.jackson.annotation.JsonProperty
import com.wealthos.fxrate.application.FxRateProvider
import com.wealthos.fxrate.application.ProvidedFxRate
import com.wealthos.fxrate.application.SupportedFxCurrency
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.LocalDate

@Component
class FrankfurterCbcFxRateProvider(
    @Value("\${wealthos.fx.frankfurter-base-url:https://api.frankfurter.dev}") baseUrl: String,
) : FxRateProvider {
    private val client = RestClient.builder().baseUrl(baseUrl).build()

    override val name: String = "CBC"

    override fun supportedCurrencies(): List<SupportedFxCurrency> =
        client.get()
            .uri { uri ->
                uri.path("/v2/currencies")
                    .queryParam("providers", name)
                    .build()
            }
            .retrieve()
            .body(Array<FrankfurterCurrencyResponse>::class.java)
            .orEmpty()
            .asSequence()
            .map { SupportedFxCurrency(it.isoCode, it.startDate, it.endDate) }
            .toList()

    override fun fetch(originalCurrency: String, from: LocalDate, to: LocalDate): List<ProvidedFxRate> =
        client.get()
            .uri { uri ->
                uri.path("/v2/rates")
                    .queryParam("from", from)
                    .queryParam("to", to)
                    .queryParam("base", originalCurrency)
                    .queryParam("quotes", "TWD")
                    .queryParam("providers", name)
                    .build()
            }
            .retrieve()
            .body(Array<FrankfurterRateResponse>::class.java)
            .orEmpty()
            .asSequence()
            .filter { it.quote == "TWD" && it.base != "TWD" }
            .map { ProvidedFxRate(it.base, it.rate, it.date) }
            .toList()
}

data class FrankfurterCurrencyResponse(
    @param:JsonProperty("iso_code") val isoCode: String,
    @param:JsonProperty("start_date") val startDate: LocalDate,
    @param:JsonProperty("end_date") val endDate: LocalDate,
)

data class FrankfurterRateResponse(
    val date: LocalDate,
    val base: String,
    val quote: String,
    val rate: BigDecimal,
)
