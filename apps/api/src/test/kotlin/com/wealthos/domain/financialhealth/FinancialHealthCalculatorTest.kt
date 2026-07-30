package com.wealthos.domain.financialhealth

import com.wealthos.domain.asset.Asset
import com.wealthos.domain.asset.AssetId
import com.wealthos.domain.asset.AssetType
import com.wealthos.domain.asset.AssetValuation
import com.wealthos.domain.asset.Liquidity
import com.wealthos.domain.asset.ValuationSource
import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityBalance
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.domain.liability.LiabilitySource
import com.wealthos.domain.shared.Currency
import com.wealthos.domain.shared.Money
import com.wealthos.domain.snapshot.Snapshot
import com.wealthos.domain.snapshot.SnapshotAssetPosition
import com.wealthos.domain.snapshot.SnapshotId
import com.wealthos.domain.snapshot.SnapshotLiabilityPosition
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
    fun `calculates position and structure from captured snapshot facts`() {
        val cash = assetPosition("Cash", Liquidity.LIQUID, "1000.00")
        val home = assetPosition("Home", Liquidity.ILLIQUID, "3000.00")
        val mortgage = liabilityPosition("Mortgage", "1000.00")

        val health = calculated(snapshot(listOf(cash, home), listOf(mortgage)))

        assertEquals(money("4000.00"), health.totalAssets)
        assertEquals(money("1000.00"), health.totalLiabilities)
        assertEquals(money("3000.00"), health.netWorth)
        assertEquals(definedRatio("0.250000"), health.debtRatio)
        assertEquals(definedRatio("0.250000"), health.liquidityRatio)
    }

    @Test
    fun `represents zero liabilities as a numeric zero debt ratio`() {
        val health = calculated(snapshot(listOf(assetPosition("Cash", Liquidity.LIQUID, "100.00"))))

        assertEquals(money("0.00"), health.totalLiabilities)
        assertEquals(money("100.00"), health.netWorth)
        assertEquals(definedRatio("0.000000"), health.debtRatio)
    }

    @Test
    fun `calculates negative net worth`() {
        val health =
            calculated(
                snapshot(
                    assets = listOf(assetPosition("Cash", Liquidity.LIQUID, "100.00")),
                    liabilities = listOf(liabilityPosition("Loan", "150.00")),
                ),
            )

        assertEquals(money("-50.00"), health.netWorth)
        assertEquals(definedRatio("1.500000"), health.debtRatio)
    }

    @Test
    fun `counts only captured liquid assets toward immediate liquidity`() {
        val health =
            calculated(
                snapshot(
                    assets =
                        listOf(
                            assetPosition("Cash", Liquidity.LIQUID, "100.00"),
                            assetPosition("Bonds", Liquidity.SEMI_LIQUID, "50.00"),
                            assetPosition("Home", Liquidity.ILLIQUID, "50.00"),
                        ),
                ),
            )

        assertEquals(definedRatio("0.500000"), health.liquidityRatio)
    }

    @Test
    fun `uses historical liquidity even when current asset metadata differs`() {
        val assetId = AssetId.new()
        val historicalAsset =
            Asset(assetId, "Cash", AssetType.CASH, Liquidity.LIQUID)
        val valuation = valuation(assetId, "100.00")
        val captured = SnapshotAssetPosition.capture(historicalAsset, valuation)

        val currentAsset =
            Asset(assetId, "Restricted cash", AssetType.CASH, Liquidity.ILLIQUID)

        val health = calculated(snapshot(listOf(captured)))

        assertEquals(definedRatio("1.000000"), health.liquidityRatio)
        assertEquals("Cash", captured.name)
        assertEquals(Liquidity.LIQUID, captured.liquidity)
        assertEquals(Liquidity.ILLIQUID, currentAsset.liquidity)
    }

    @Test
    fun `represents ratios as undefined when total assets are zero`() {
        val health =
            calculated(
                snapshot(liabilities = listOf(liabilityPosition("Loan", "100.00"))),
            )

        assertEquals(money("0.00"), health.totalAssets)
        assertEquals(money("-100.00"), health.netWorth)
        assertEquals(FinancialRatio.Undefined, health.debtRatio)
        assertEquals(FinancialRatio.Undefined, health.liquidityRatio)
    }

    @Test
    fun `returns insufficient data for an empty snapshot`() {
        val result = FinancialHealthCalculator.calculate(snapshot())

        val insufficient = assertIs<FinancialHealthResult.InsufficientData>(result)
        assertEquals(InsufficientDataReason.EmptySnapshot, insufficient.reason)
    }

    @Test
    fun `does not combine different currencies`() {
        val snapshot =
            snapshot(
                assets = listOf(assetPosition("Cash", Liquidity.LIQUID, "100.00")),
                liabilities = listOf(liabilityPosition("Loan", "50.00", eur)),
            )

        val result = FinancialHealthCalculator.calculate(snapshot)

        val insufficient = assertIs<FinancialHealthResult.InsufficientData>(result)
        assertEquals(
            InsufficientDataReason.MixedCurrencies(setOf(usd, eur)),
            insufficient.reason,
        )
    }

    @Test
    fun `rounds ratios deterministically to six decimal places`() {
        val health =
            calculated(
                snapshot(
                    assets = listOf(assetPosition("Cash", Liquidity.LIQUID, "3.00")),
                    liabilities = listOf(liabilityPosition("Loan", "1.00")),
                ),
            )

        assertEquals(definedRatio("0.333333"), health.debtRatio)
    }

    @Test
    fun `uses half even rounding at ratio boundaries`() {
        val roundsToEvenZero =
            calculated(
                snapshot(
                    assets = listOf(assetPosition("Investment", Liquidity.ILLIQUID, "2000000.00")),
                    liabilities = listOf(liabilityPosition("Loan", "1.00")),
                ),
            )
        val roundsToEvenTwo =
            calculated(
                snapshot(
                    assets = listOf(assetPosition("Investment", Liquidity.ILLIQUID, "2000000.00")),
                    liabilities = listOf(liabilityPosition("Loan", "3.00")),
                ),
            )

        assertEquals(definedRatio("0.000000"), roundsToEvenZero.debtRatio)
        assertEquals(definedRatio("0.000002"), roundsToEvenTwo.debtRatio)
    }

    @Test
    fun `produces the same result without mutating its snapshot`() {
        val snapshot =
            snapshot(
                assets = listOf(assetPosition("Cash", Liquidity.LIQUID, "100.00")),
                liabilities = listOf(liabilityPosition("Loan", "25.00")),
            )
        val originalAssets = snapshot.assetPositions
        val originalLiabilities = snapshot.liabilityPositions

        val first = FinancialHealthCalculator.calculate(snapshot)
        val second = FinancialHealthCalculator.calculate(snapshot)

        assertEquals(first, second)
        assertEquals(originalAssets, snapshot.assetPositions)
        assertEquals(originalLiabilities, snapshot.liabilityPositions)
    }

    private fun calculated(snapshot: Snapshot): FinancialHealth =
        assertIs<FinancialHealthResult.Calculated>(
            FinancialHealthCalculator.calculate(snapshot),
        ).financialHealth

    private fun snapshot(
        assets: List<SnapshotAssetPosition> = emptyList(),
        liabilities: List<SnapshotLiabilityPosition> = emptyList(),
    ): Snapshot =
        Snapshot.capture(
            id = SnapshotId.new(),
            asOf = asOf,
            recordedAt = asOf,
            assets =
                assets.map {
                    Asset(it.assetId, it.name, it.type, it.liquidity)
                },
            assetValuations = assets.map(SnapshotAssetPosition::valuation),
            liabilities =
                liabilities.map {
                    Liability(it.liabilityId, it.name)
                },
            liabilityBalances = liabilities.map(SnapshotLiabilityPosition::balance),
        )

    private fun assetPosition(
        name: String,
        liquidity: Liquidity,
        amount: String,
        currency: Currency = usd,
    ): SnapshotAssetPosition {
        val asset =
            Asset(
                id = AssetId.new(),
                name = name,
                type = AssetType.OTHER,
                liquidity = liquidity,
            )
        return SnapshotAssetPosition.capture(asset, valuation(asset.id, amount, currency))
    }

    private fun liabilityPosition(
        name: String,
        amount: String,
        currency: Currency = usd,
    ): SnapshotLiabilityPosition {
        val liability = Liability(LiabilityId.new(), name)
        val balance =
            LiabilityBalance(
                liabilityId = liability.id,
                balance = money(amount, currency),
                effectiveAt = asOf,
                source = LiabilitySource.of("manual"),
            )
        return SnapshotLiabilityPosition.capture(liability, balance)
    }

    private fun valuation(
        assetId: AssetId,
        amount: String,
        currency: Currency = usd,
    ): AssetValuation =
        AssetValuation(
            assetId = assetId,
            value = money(amount, currency),
            effectiveAt = asOf,
            source = ValuationSource.of("manual"),
        )

    private fun money(
        amount: String,
        currency: Currency = usd,
    ): Money = Money.of(BigDecimal(amount), currency)

    private fun definedRatio(value: String): FinancialRatio =
        FinancialRatio.Defined.divide(BigDecimal(value), BigDecimal.ONE)
}
