package com.wealthos.domain.financialhealth

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.AssetId
import com.wealthos.domain.asset.AssetType
import com.wealthos.domain.asset.AssetValuation
import com.wealthos.domain.asset.Liquidity
import com.wealthos.domain.asset.ValuationSource
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotId
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FinancialHealthCalculatorTest {
    private val asOf = Instant.parse("2026-07-27T00:00:00Z")
    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")

    @Test
    fun `calculates totals net worth debt ratio and liquidity ratio`() {
        val cash = asset("Cash", Liquidity.LIQUID)
        val home = asset("Home", Liquidity.ILLIQUID)
        val snapshot =
            snapshot(
                valuations =
                    listOf(
                        valuation(cash, "1000.00"),
                        valuation(home, "3000.00"),
                    ),
                balances = listOf(balance("1000.00")),
            )

        val health = calculated(snapshot, listOf(cash, home))

        assertEquals(money("4000.00"), health.totalAssets)
        assertEquals(money("1000.00"), health.totalLiabilities)
        assertEquals(money("3000.00"), health.netWorth)
        assertEquals(definedRatio("0.250000"), health.debtRatio)
        assertEquals(definedRatio("0.250000"), health.liquidityRatio)
    }

    @Test
    fun `represents zero liabilities as a numeric zero debt ratio`() {
        val cash = asset("Cash", Liquidity.LIQUID)
        val health = calculated(snapshot(listOf(valuation(cash, "100.00"))), listOf(cash))

        assertEquals(money("0.00"), health.totalLiabilities)
        assertEquals(money("100.00"), health.netWorth)
        assertEquals(definedRatio("0.000000"), health.debtRatio)
    }

    @Test
    fun `calculates negative net worth`() {
        val cash = asset("Cash", Liquidity.LIQUID)
        val health =
            calculated(
                snapshot(
                    valuations = listOf(valuation(cash, "100.00")),
                    balances = listOf(balance("150.00")),
                ),
                listOf(cash),
            )

        assertEquals(money("-50.00"), health.netWorth)
        assertEquals(definedRatio("1.500000"), health.debtRatio)
    }

    @Test
    fun `counts only liquid assets toward liquidity`() {
        val liquid = asset("Cash", Liquidity.LIQUID)
        val semiLiquid = asset("Bonds", Liquidity.SEMI_LIQUID)
        val illiquid = asset("Home", Liquidity.ILLIQUID)

        val fullyLiquid =
            calculated(
                snapshot(listOf(valuation(liquid, "100.00"))),
                listOf(liquid),
            )
        val noLiquid =
            calculated(
                snapshot(
                    listOf(
                        valuation(semiLiquid, "50.00"),
                        valuation(illiquid, "50.00"),
                    ),
                ),
                listOf(semiLiquid, illiquid),
            )

        assertEquals(definedRatio("1.000000"), fullyLiquid.liquidityRatio)
        assertEquals(definedRatio("0.000000"), noLiquid.liquidityRatio)
    }

    @Test
    fun `represents ratios as undefined when total assets are zero`() {
        val health =
            calculated(
                snapshot(balances = listOf(balance("100.00"))),
                emptyList(),
            )

        assertEquals(money("0.00"), health.totalAssets)
        assertEquals(money("-100.00"), health.netWorth)
        assertEquals(FinancialRatio.Undefined, health.debtRatio)
        assertEquals(FinancialRatio.Undefined, health.liquidityRatio)
    }

    @Test
    fun `returns insufficient data for an empty snapshot`() {
        val result = FinancialHealthCalculator.calculate(snapshot(), emptyList())

        val insufficient = assertIs<FinancialHealthResult.InsufficientData>(result)
        assertEquals(InsufficientDataReason.EmptySnapshot, insufficient.reason)
    }

    @Test
    fun `returns missing asset identities when valuations are absent`() {
        val cash = asset("Cash", Liquidity.LIQUID)

        val result = FinancialHealthCalculator.calculate(snapshot(), listOf(cash))

        val insufficient = assertIs<FinancialHealthResult.InsufficientData>(result)
        assertEquals(
            InsufficientDataReason.MissingAssetValuations(setOf(cash.id)),
            insufficient.reason,
        )
    }

    @Test
    fun `returns unknown asset identities when snapshot metadata is absent`() {
        val cash = asset("Cash", Liquidity.LIQUID)

        val result =
            FinancialHealthCalculator.calculate(
                snapshot(listOf(valuation(cash, "100.00"))),
                emptyList(),
            )

        val insufficient = assertIs<FinancialHealthResult.InsufficientData>(result)
        assertEquals(
            InsufficientDataReason.UnknownAssetValuations(setOf(cash.id)),
            insufficient.reason,
        )
    }

    @Test
    fun `does not combine different currencies`() {
        val cash = asset("Cash", Liquidity.LIQUID)
        val snapshot =
            snapshot(
                valuations = listOf(valuation(cash, "100.00")),
                balances = listOf(balance("50.00", eur)),
            )

        val result = FinancialHealthCalculator.calculate(snapshot, listOf(cash))

        val insufficient = assertIs<FinancialHealthResult.InsufficientData>(result)
        assertEquals(
            InsufficientDataReason.MixedCurrencies(setOf(usd, eur)),
            insufficient.reason,
        )
    }

    @Test
    fun `rounds ratios deterministically to six decimal places`() {
        val cash = asset("Cash", Liquidity.LIQUID)
        val health =
            calculated(
                snapshot(
                    valuations = listOf(valuation(cash, "3.00")),
                    balances = listOf(balance("1.00")),
                ),
                listOf(cash),
            )

        assertEquals(definedRatio("0.333333"), health.debtRatio)
    }

    @Test
    fun `uses half even rounding at ratio boundaries`() {
        val asset = asset("Investment", Liquidity.ILLIQUID)
        val roundsToEvenZero =
            calculated(
                snapshot(
                    valuations = listOf(valuation(asset, "2000000.00")),
                    balances = listOf(balance("1.00")),
                ),
                listOf(asset),
            )
        val roundsToEvenTwo =
            calculated(
                snapshot(
                    valuations = listOf(valuation(asset, "2000000.00")),
                    balances = listOf(balance("3.00")),
                ),
                listOf(asset),
            )

        assertEquals(definedRatio("0.000000"), roundsToEvenZero.debtRatio)
        assertEquals(definedRatio("0.000002"), roundsToEvenTwo.debtRatio)
    }

    @Test
    fun `produces the same result for the same snapshot and metadata`() {
        val cash = asset("Cash", Liquidity.LIQUID)
        val snapshot =
            snapshot(
                valuations = listOf(valuation(cash, "100.00")),
                balances = listOf(balance("25.00")),
            )

        val first = FinancialHealthCalculator.calculate(snapshot, listOf(cash))
        val second = FinancialHealthCalculator.calculate(snapshot, listOf(cash))

        assertEquals(first, second)
    }

    @Test
    fun `does not mutate its source snapshot`() {
        val cash = asset("Cash", Liquidity.LIQUID)
        val snapshot = snapshot(listOf(valuation(cash, "100.00")))
        val originalValuations = snapshot.assetValuations
        val originalBalances = snapshot.liabilityBalances

        FinancialHealthCalculator.calculate(snapshot, listOf(cash))

        assertEquals(originalValuations, snapshot.assetValuations)
        assertEquals(originalBalances, snapshot.liabilityBalances)
    }

    private fun calculated(
        snapshot: Snapshot,
        assets: Collection<Asset>,
    ): FinancialHealth =
        assertIs<FinancialHealthResult.Calculated>(
            FinancialHealthCalculator.calculate(snapshot, assets),
        ).financialHealth

    private fun snapshot(
        valuations: List<AssetValuation> = emptyList(),
        balances: List<LiabilityBalance> = emptyList(),
    ): Snapshot = Snapshot.create(SnapshotId.new(), asOf, valuations, balances)

    private fun asset(
        name: String,
        liquidity: Liquidity,
    ): Asset =
        Asset(
            id = AssetId.new(),
            name = name,
            type = AssetType.OTHER,
            liquidity = liquidity,
        )

    private fun valuation(
        asset: Asset,
        amount: String,
        currency: Currency = usd,
    ): AssetValuation =
        AssetValuation(
            assetId = asset.id,
            value = money(amount, currency),
            effectiveAt = asOf,
            source = ValuationSource.of("manual"),
        )

    private fun balance(
        amount: String,
        currency: Currency = usd,
    ): LiabilityBalance =
        LiabilityBalance(
            liabilityId = LiabilityId.new(),
            balance = money(amount, currency),
            effectiveAt = asOf,
            source = LiabilitySource.of("manual"),
        )

    private fun money(
        amount: String,
        currency: Currency = usd,
    ): Money = Money.of(BigDecimal(amount), currency)

    private fun definedRatio(value: String): FinancialRatio =
        FinancialRatio.Defined.divide(BigDecimal(value), BigDecimal.ONE)
}
