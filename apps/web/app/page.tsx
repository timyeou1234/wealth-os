"use client";

import React, { useEffect, useState } from "react";

type Snapshot = { id: string; asOf: string };
type Money = { amount: string; currency: string };
type FinancialHealth = {
  status: "CALCULATED" | "INSUFFICIENT_DATA";
  reason: string | null;
  totalAssets: Money | null;
  totalLiabilities: Money | null;
  netWorth: Money | null;
  debtRatio: string | null;
  liquidityRatio: string | null;
  explanations?: {
    debtRatioFormula: string;
    liquidityRatioFormula: string;
    assetContributors: Contributor[];
    liabilityContributors: Contributor[];
  };
};
type Contributor = { id: string; name: string; amount: Money; liquidity: string | null };
type SnapshotDetails = { assets: AssetFact[]; liabilities: LiabilityFact[] };
type AssetFact = { id: string; name: string; type: string; liquidity: string; money: Money; effectiveAt: string; source: string };
type LiabilityFact = { id: string; name: string; money: Money; effectiveAt: string; source: string };

const money = (value: Money | null | undefined) =>
  value ? new Intl.NumberFormat("en-US", { style: "currency", currency: value.currency }).format(Number(value.amount)) : "—";

const percent = (value: string | null | undefined) => (value ? `${(Number(value) * 100).toFixed(2)}%` : "—");
const date = (value: string) => new Intl.DateTimeFormat("en-US", { dateStyle: "medium" }).format(new Date(value));

export default function Dashboard() {
  const [snapshots, setSnapshots] = useState<Snapshot[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [health, setHealth] = useState<FinancialHealth>();
  const [snapshot, setSnapshot] = useState<SnapshotDetails>();

  useEffect(() => {
    fetch("/api/v1/snapshots")
      .then((response) => response.json())
      .then((items: Snapshot[]) => {
        setSnapshots(items);
        setSelectedId(items.at(-1)?.id);
      });
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    fetch(`/api/v1/financial-health/${selectedId}`)
      .then((response) => response.json())
      .then(setHealth);
  }, [selectedId]);

  useEffect(() => {
    if (!selectedId) return;
    setSnapshot(undefined);
    fetch(`/api/v1/snapshots/${selectedId}`)
      .then((response) => response.json())
      .then(setSnapshot);
  }, [selectedId]);

  if (snapshots.length === 0) {
    return <main className="dashboard"><h1>Wealth OS</h1><p>Save a Snapshot to see your financial position.</p></main>;
  }

  return (
    <main className="dashboard">
      <header>
        <div><p className="eyebrow">Financial position</p><h1>Wealth OS</h1></div>
        <label>Snapshot<select aria-label="Snapshot" value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>{snapshots.map((snapshot) => <option key={snapshot.id} value={snapshot.id}>{new Date(snapshot.asOf).toLocaleDateString()}</option>)}</select></label>
      </header>
      {health?.status === "INSUFFICIENT_DATA" ? <p>Financial health is incomplete: {health.reason}.</p> : <section className="cards">
        <Card label="Total assets" value={money(health?.totalAssets)} />
        <Card label="Total liabilities" value={money(health?.totalLiabilities)} />
        <Card label="Net worth" value={money(health?.netWorth)} emphasis />
        <Card label="Debt ratio" value={percent(health?.debtRatio)} />
        <Card label="Liquidity ratio" value={percent(health?.liquidityRatio)} />
      </section>}
      {health?.status === "CALCULATED" && health.explanations && <details className="explanations">
        <summary>How these metrics are calculated</summary>
        <p>Debt ratio: {health.explanations.debtRatioFormula}</p>
        <p>Liquidity ratio: {health.explanations.liquidityRatioFormula}</p>
      </details>}
      {snapshot && <section className="positions" aria-label="Snapshot details">
        <PositionTable title="Assets" rows={snapshot.assets} asset />
        <PositionTable title="Liabilities" rows={snapshot.liabilities} />
      </section>}
    </main>
  );
}

function Card({ label, value, emphasis = false }: { label: string; value: string; emphasis?: boolean }) {
  return <article className={emphasis ? "card emphasis" : "card"}><p>{label}</p><strong>{value}</strong></article>;
}

function PositionTable({ title, rows, asset = false }: { title: string; rows: Array<AssetFact | LiabilityFact>; asset?: boolean }) {
  return <section className="position-table">
    <h2>{title}</h2>
    <div className="table-scroll"><table>
      <thead><tr><th>Name</th>{asset && <><th>Type</th><th>Liquidity</th></>}<th>Amount</th><th>Effective date</th><th>Source</th></tr></thead>
      <tbody>{rows.map((item) => <tr key={item.id}><td>{item.name}</td>{asset && <><td>{(item as AssetFact).type}</td><td>{(item as AssetFact).liquidity}</td></>}<td>{money(item.money)}</td><td>{date(item.effectiveAt)}</td><td>{item.source}</td></tr>)}</tbody>
    </table></div>
  </section>;
}
