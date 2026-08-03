package com.wealthos.fxrate.adapter.provider

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.time.LocalDate
import kotlin.test.assertEquals

class FrankfurterCbcFxRateProviderTest {
    @Test
    fun `fetch requests each CBC currency as the base of a TWD quote`() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/v2/currencies") { exchange ->
            exchange.respond(
                """[{"iso_code":"USD","start_date":"1948-06-21","end_date":"2026-08-03"},{"iso_code":"JPY","start_date":"1969-12-01","end_date":"2026-08-03"},{"iso_code":"TWD","start_date":"1981-01-02","end_date":"2026-08-03"}]""",
            )
        }
        server.createContext("/v2/rates") { exchange ->
            val base = exchange.requestURI.rawQuery.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }["base"]
            val rate = if (base == "USD") "32.292" else "0.216"
            exchange.respond("""[{"date":"2026-07-31","base":"$base","quote":"TWD","rate":$rate}]""")
        }
        server.start()

        try {
            val provider = FrankfurterCbcFxRateProvider("http://localhost:${server.address.port}")
            val result = provider.supportedCurrencies()
                .filter { it.code != "TWD" }
                .flatMap { provider.fetch(it.code, LocalDate.parse("2026-07-31"), LocalDate.parse("2026-08-03")) }

            assertEquals(listOf("JPY", "USD"), result.map { it.originalCurrency }.sorted())
            assertEquals(listOf("0.216", "32.292"), result.sortedBy { it.originalCurrency }.map { it.rate.toPlainString() })
        } finally {
            server.stop(0)
        }
    }

    private fun HttpExchange.respond(body: String) {
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(200, body.toByteArray().size.toLong())
        responseBody.use { it.write(body.toByteArray()) }
    }
}
