"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { getFinancialHealth, getSnapshot, listSnapshots } from "./api/client";
import type { AssetFactResponse, FinancialHealthResponse, LiabilityFactResponse, MoneyResponse, SnapshotResponse } from "./api/client";

const money = (value: MoneyResponse | null | undefined) =>
  value?.amount && value.currency ? new Intl.NumberFormat("en-US", { style: "currency", currency: value.currency }).format(Number(value.amount)) : "—";

const percent = (value: string | null | undefined) => (value ? `${(Number(value) * 100).toFixed(2)}%` : "—");
const date = (value: string | undefined) => value ? new Intl.DateTimeFormat("en-US", { dateStyle: "medium" }).format(new Date(value)) : "—";

export default function Dashboard() {
  const [snapshots, setSnapshots] = useState<SnapshotResponse[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [health, setHealth] = useState<FinancialHealthResponse>();
  const [snapshot, setSnapshot] = useState<SnapshotResponse>();

  useEffect(() => {
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

  if (snapshots.length === 0) {
    return <main className="dashboard"><h1>Wealth OS</h1><p>Save a Snapshot to see your financial position.</p><Link className="primary-link" href="/entry">Enter balance sheet</Link></main>;
  }

  return (
    <main className="dashboard">
      <header>
        <div><p className="eyebrow">Financial position</p><h1>Wealth OS</h1></div>
        <div className="dashboard-actions"><Link href="/entry">Update balance sheet</Link><label>Snapshot<select aria-label="Snapshot" value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>{snapshots.map((snapshot) => <option key={snapshot.id} value={snapshot.id}>{date(snapshot.asOf)}</option>)}</select></label></div>
      </header>
      {health?.status === "INSUFFICIENT_DATA" ? <p>Financial health is incomplete: {health.reason}.</p> : <section className="cards">
        <Card label="Total assets" value={money(health?.totalAssets)} />
        <Card label="Total liabilities" value={money(health?.totalLiabilities)} />
        <Card label="Net worth" value={money(health?.netWorth)} emphasis />
        <Card label="Debt ratio" value={percent(health?.debtRatio)} />
        <Card label="Liquidity ratio" value={percent(health?.liquidityRatio)} />
      </section>}
      {health?.status === "CALCULATED" && <section className="explanations" aria-label="Metric explanations">
        <h2>How these metrics are calculated</h2>
        <p>Total Assets = Sum of all asset values in the base currency</p>
        <p>Total Liabilities = Sum of all liability balances in the base currency</p>
        <p>Net Worth = Total Assets - Total Liabilities</p>
        <p>Debt Ratio = Total Liabilities / Total Assets</p>
        <p>Immediately Liquid Asset Share = Liquid Assets / Total Assets</p>
        <p>Included in immediately liquid assets: assets classified as LIQUID. SEMI_LIQUID and ILLIQUID assets are excluded.</p>
      </section>}
      {snapshot && <section className="positions" aria-label="Snapshot details">
        <AssetsTable rows={snapshot.assets ?? []} />
        <LiabilitiesTable rows={snapshot.liabilities ?? []} />
      </section>}
    </main>
  );
}

function Card({ label, value, emphasis = false }: { label: string; value: string; emphasis?: boolean }) {
  return <article className={emphasis ? "card emphasis" : "card"}><p>{label}</p><strong>{value}</strong></article>;
}

function AssetsTable({ rows }: { rows: AssetFactResponse[] }) {
  return <section className="position-table">
    <h2>Assets</h2>
    <div className="table-scroll"><table>
      <thead><tr><th>Name</th><th>Type</th><th>Liquidity</th><th>Amount</th><th>Effective date</th><th>Source</th></tr></thead>
      <tbody>{rows.map((item) => <tr key={item.id}><td>{item.name}</td><td>{item.type}</td><td>{item.liquidity}</td><td>{money(item.money)}</td><td>{date(item.effectiveAt)}</td><td>{item.source}</td></tr>)}</tbody>
    </table></div>
  </section>;
}

function LiabilitiesTable({ rows }: { rows: LiabilityFactResponse[] }) {
  return <section className="position-table"><h2>Liabilities</h2><div className="table-scroll"><table>
    <thead><tr><th>Name</th><th>Amount</th><th>Effective date</th><th>Source</th></tr></thead>
    <tbody>{rows.map((item) => <tr key={item.id}><td>{item.name}</td><td>{money(item.money)}</td><td>{date(item.effectiveAt)}</td><td>{item.source}</td></tr>)}</tbody>
  </table></div></section>;
}
