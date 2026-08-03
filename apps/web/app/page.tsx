"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { getFinancialHealth, getFxRates, getSnapshot, listSnapshots } from "./api/client";
import type { AssetFactResponse, FinancialHealthResponse, LiabilityFactResponse, MoneyResponse, SnapshotResponse } from "./api/client";
import { AppNavigation } from "./app-navigation";

const displayCurrencies = ["TWD", "USD", "JPY", "EUR", "HKD"];
const currencyCode = (value: string | null) => value?.toUpperCase().match(/^[A-Z]{3}$/)?.[0];
const formatMoney = (amount: number, currency: string) => new Intl.NumberFormat("en-US", {
  style: "currency",
  currency,
  ...(currency === "TWD" ? { maximumFractionDigits: 0 } : {}),
}).format(amount);
const money = (value: MoneyResponse | null | undefined, displayCurrency: string, rate?: number) => {
  if (!value?.amount || !value.currency) return "—";
  if (value.currency === displayCurrency) return formatMoney(Number(value.amount), displayCurrency);
  if (value.currency !== "TWD" || displayCurrency === "TWD") return formatMoney(Number(value.amount), value.currency);
  return rate ? formatMoney(Number(value.amount) / rate, displayCurrency) : "—";
};

const percent = (value: string | null | undefined) => (value ? `${(Number(value) * 100).toFixed(2)}%` : "—");
const date = (value: string | undefined) => value ? new Intl.DateTimeFormat("en-US", { dateStyle: "medium" }).format(new Date(value)) : "—";

export default function Dashboard() {
  const [snapshots, setSnapshots] = useState<SnapshotResponse[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [health, setHealth] = useState<FinancialHealthResponse>();
  const [snapshot, setSnapshot] = useState<SnapshotResponse>();
  const [displayCurrency, setDisplayCurrency] = useState("TWD");
  const [displayRate, setDisplayRate] = useState<{ rate: number; rateDate?: string }>();
  const [rateUnavailable, setRateUnavailable] = useState(false);

  useEffect(() => {
    const queryCurrency = currencyCode(new URLSearchParams(window.location.search).get("displayCurrency"));
    const savedCurrency = currencyCode(window.localStorage.getItem("wealthos.displayCurrency"));
    const selectedCurrency = queryCurrency ?? savedCurrency ?? "TWD";
    setDisplayCurrency(selectedCurrency);
    window.localStorage.setItem("wealthos.displayCurrency", selectedCurrency);
    listSnapshots()
      .then(({ data: items }) => {
        if (!items) return;
        setSnapshots(items);
        const requestedId = new URLSearchParams(window.location.search).get("snapshot");
        setSelectedId(items.some((item) => item.id === requestedId) ? requestedId ?? undefined : items.at(-1)?.id);
      });
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    let cancelled = false;
    setHealth(undefined);
    setSnapshot(undefined);

    Promise.all([
      getFinancialHealth({ path: { snapshotId: selectedId } }),
      getSnapshot({ path: { id: selectedId } }),
    ]).then(([healthResponse, snapshotResponse]) => {
      if (cancelled) return;
      if (healthResponse.data) setHealth(healthResponse.data);
      if (snapshotResponse.data) setSnapshot(snapshotResponse.data);
    });

    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  useEffect(() => {
    if (!snapshot?.asOf) return;
    if (displayCurrency === "TWD") {
      setDisplayRate({ rate: 1, rateDate: snapshot.asOf.slice(0, 10) });
      setRateUnavailable(false);
      return;
    }
    let cancelled = false;
    setDisplayRate(undefined);
    setRateUnavailable(false);
    getFxRates({ query: { asOf: snapshot.asOf.slice(0, 10), currencies: [displayCurrency] } })
      .then(({ data }) => {
        if (cancelled) return;
        const item = data?.rates?.find((candidate) => candidate.originalCurrency === displayCurrency);
        if (item?.rate) setDisplayRate({ rate: Number(item.rate), rateDate: item.rateDate });
        else setRateUnavailable(true);
      })
      .catch(() => { if (!cancelled) setRateUnavailable(true); });
    return () => { cancelled = true; };
  }, [displayCurrency, snapshot?.asOf]);

  const changeDisplayCurrency = (currency: string) => {
    setDisplayCurrency(currency);
    window.localStorage.setItem("wealthos.displayCurrency", currency);
  };

  if (snapshots.length === 0) {
    return <main className="dashboard"><AppNavigation current="dashboard" /><h1>Wealth OS</h1><p>Save a Snapshot to see your financial position.</p><Link className="primary-link" href="/entry">Enter balance sheet</Link></main>;
  }

  return (
    <main className="dashboard">
      <AppNavigation current="dashboard" />
      <header>
        <div><p className="eyebrow">Financial position</p><h1>Wealth OS</h1></div>
        <div className="dashboard-actions"><Link href="/entry">Update balance sheet</Link><label>Display currency<select aria-label="Display currency" value={displayCurrency} onChange={(event) => changeDisplayCurrency(event.target.value)}>{Array.from(new Set([...displayCurrencies, displayCurrency])).map((currency) => <option key={currency}>{currency}</option>)}</select></label><label>Snapshot<select aria-label="Snapshot" value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>{snapshots.map((snapshot) => <option key={snapshot.id} value={snapshot.id}>{date(snapshot.asOf)}</option>)}</select></label></div>
      </header>
      {displayCurrency !== "TWD" && displayRate?.rateDate && <p className="rate-evidence">Rate date {date(displayRate.rateDate)}</p>}
      {rateUnavailable && <p role="alert">No historical {displayCurrency}/TWD rate is available for this Snapshot.</p>}
      {health?.status === "INSUFFICIENT_DATA" ? <p>Financial health is incomplete: {health.reason}.</p> : <section className="cards">
        <Card label="Total assets" value={money(health?.totalAssets, displayCurrency, displayRate?.rate)} />
        <Card label="Total liabilities" value={money(health?.totalLiabilities, displayCurrency, displayRate?.rate)} />
        <Card label="Net worth" value={money(health?.netWorth, displayCurrency, displayRate?.rate)} emphasis />
        <Card label="Debt ratio" value={percent(health?.debtRatio)} />
        <Card label="Liquidity ratio" value={percent(health?.liquidityRatio)} />
      </section>}
      {health?.status === "CALCULATED" && <section className="explanations" aria-label="Metric explanations">
        <h2>How these metrics are calculated</h2>
        <p>Total Assets = Sum of all asset values in canonical TWD</p>
        <p>Total Liabilities = Sum of all liability balances in canonical TWD</p>
        <p>Net Worth = Total Assets - Total Liabilities</p>
        <p>Debt Ratio = Total Liabilities / Total Assets</p>
        <p>Immediately Liquid Asset Share = Liquid Assets / Total Assets</p>
        <p>Included in immediately liquid assets: assets classified as LIQUID. SEMI_LIQUID and ILLIQUID assets are excluded.</p>
      </section>}
      {snapshot && <section className="positions" aria-label="Snapshot details">
        <AssetsTable rows={snapshot.assets ?? []} displayCurrency={displayCurrency} rate={displayRate?.rate} />
        <LiabilitiesTable rows={snapshot.liabilities ?? []} displayCurrency={displayCurrency} rate={displayRate?.rate} />
      </section>}
    </main>
  );
}

function Card({ label, value, emphasis = false }: { label: string; value: string; emphasis?: boolean }) {
  return <article className={emphasis ? "card emphasis" : "card"}><p>{label}</p><strong>{value}</strong></article>;
}

function Amount({ item, displayCurrency, rate }: { item: AssetFactResponse | LiabilityFactResponse; displayCurrency: string; rate?: number }) {
  const original = item.appliedConversion?.originalMoney;
  return <><span>{money(item.money, displayCurrency, rate)}</span>{displayCurrency !== "TWD" && item.money?.amount && <small>{formatMoney(Number(item.money.amount), "TWD")} canonical</small>}{original?.amount && original.currency && original.currency !== "TWD" && <small>{formatMoney(Number(original.amount), original.currency)} original</small>}</>;
}

function AssetsTable({ rows, displayCurrency, rate }: { rows: AssetFactResponse[]; displayCurrency: string; rate?: number }) {
  return <section className="position-table">
    <h2>Assets</h2>
    <div className="table-scroll"><table>
      <thead><tr><th>Name</th><th>Type</th><th>Liquidity</th><th>Amount</th><th>Effective date</th><th>Source</th></tr></thead>
      <tbody>{rows.map((item) => <tr key={item.id}><td>{item.name}</td><td>{item.type}</td><td>{item.liquidity}</td><td className="money-detail"><Amount item={item} displayCurrency={displayCurrency} rate={rate} /></td><td>{date(item.effectiveAt)}</td><td>{item.source}</td></tr>)}</tbody>
    </table></div>
  </section>;
}

function LiabilitiesTable({ rows, displayCurrency, rate }: { rows: LiabilityFactResponse[]; displayCurrency: string; rate?: number }) {
  return <section className="position-table"><h2>Liabilities</h2><div className="table-scroll"><table>
    <thead><tr><th>Name</th><th>Amount</th><th>Effective date</th><th>Source</th></tr></thead>
    <tbody>{rows.map((item) => <tr key={item.id}><td>{item.name}</td><td className="money-detail"><Amount item={item} displayCurrency={displayCurrency} rate={rate} /></td><td>{date(item.effectiveAt)}</td><td>{item.source}</td></tr>)}</tbody>
  </table></div></section>;
}
