"use client";

import { useRouter } from "next/navigation";
import React, { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import { archiveAsset, archiveLiability, captureSnapshot, getSnapshot, listAssets, listLiabilities, listSnapshots } from "../api/client";
import type { AssetFactResponse, AssetResponse, CaptureAssetRequest, CaptureLiabilityRequest, CaptureSnapshotRequest, LiabilityFactResponse, LiabilityResponse, SnapshotResponse, ValidationProblemResponse } from "../api/client";
import { AppNavigation } from "../app-navigation";
import { isSupportedCurrency, parseAgentImport } from "./import";
import type { AgentImport, AgentImportAsset, AgentImportLiability, AgentImportManualConversion } from "./import";

type AssetType = CaptureAssetRequest["type"];
type Liquidity = CaptureAssetRequest["liquidity"];

type ManualConversionDraft = {
  originalAmount: string;
  originalCurrency: string;
  exchangeRateBasis: string;
  effectiveDate: string;
};

type AssetDraft = {
  key: string;
  id?: string;
  name: string;
  type: AssetType | "";
  liquidity: Liquidity | "";
  amount: string;
  effectiveDate: string;
  source: string;
  manualConversion?: ManualConversionDraft;
  carriedFrom?: string;
};

type LiabilityDraft = {
  key: string;
  id?: string;
  name: string;
  amount: string;
  effectiveDate: string;
  source: string;
  manualConversion?: ManualConversionDraft;
  carriedFrom?: string;
};

type ArchiveTarget = { kind: "asset" | "liability"; id: string; key: string; name: string };
type EntryStep = "assets" | "liabilities" | "review";
type InputMode = "manual" | "ai";

const instant = (day: string) => new Date(`${day}T00:00:00Z`).toISOString();
const day = (value?: string) => value?.slice(0, 10) ?? "";
const today = () => new Date().toISOString().slice(0, 10);
const displayDate = (value: string) => new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeZone: "UTC" }).format(new Date(value));
const assetTypes = ["CASH", "INVESTMENT", "REAL_ESTATE", "VEHICLE", "BUSINESS", "OTHER"] as const;
const liquidities = ["LIQUID", "SEMI_LIQUID", "ILLIQUID"] as const;
const decimal = /^(?:0|[1-9]\d*)(?:\.\d+)?$/;

export default function EntryPage() {
  const router = useRouter();
  const nextKey = useRef(0);
  const formRef = useRef<HTMLFormElement>(null);
  const manualModeTabRef = useRef<HTMLButtonElement>(null);
  const aiModeTabRef = useRef<HTMLButtonElement>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [mode, setMode] = useState<InputMode>("ai");
  const [step, setStep] = useState<EntryStep>("assets");
  const [validationFocusRequest, setValidationFocusRequest] = useState(0);
  const [apiFocus, setApiFocus] = useState<{ field: string; request: number }>();
  const [showValidation, setShowValidation] = useState(false);
  const [error, setError] = useState<string>();
  const [snapshotDate, setSnapshotDate] = useState(today);
  const [baseCurrency, setBaseCurrency] = useState("");
  const [assets, setAssets] = useState<AssetDraft[]>([]);
  const [liabilities, setLiabilities] = useState<LiabilityDraft[]>([]);
  const [archiveTarget, setArchiveTarget] = useState<ArchiveTarget>();
  const [includeDraftInPrompt, setIncludeDraftInPrompt] = useState(false);
  const [agentJson, setAgentJson] = useState("");
  const [importError, setImportError] = useState<string>();
  const [importReview, setImportReview] = useState<{ data: AgentImport; changes: string[] }>();

  useEffect(() => {
    let cancelled = false;
    Promise.all([listAssets(), listLiabilities(), listSnapshots()])
      .then(async ([assetResponse, liabilityResponse, snapshotResponse]) => {
        const currentAssets = assetResponse.data ?? [];
        const currentLiabilities = liabilityResponse.data ?? [];
        const latest = snapshotResponse.data?.at(-1);
        const saved = latest?.id ? (await getSnapshot({ path: { id: latest.id } })).data : undefined;
        if (cancelled) return;
        hydrate(currentAssets, currentLiabilities, saved, setAssets, setLiabilities, setBaseCurrency);
        setLoading(false);
      })
      .catch(() => {
        if (!cancelled) {
          setError("Unable to load the current balance sheet. Try again.");
          setLoading(false);
        }
      });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (validationFocusRequest > 0) formRef.current?.querySelector<HTMLElement>(":invalid")?.focus();
  }, [validationFocusRequest]);

  useEffect(() => {
    if (!apiFocus) return;
    const candidates = Array.from(formRef.current?.querySelectorAll<HTMLElement>("[data-api-field]") ?? []);
    const exact = candidates.find((element) => element.dataset.apiField === apiFocus.field);
    const collection = apiFocus.field.match(/^(assets|liabilities)\[(\d+)]/);
    const fallbackField = collection ? `${collection[1]}[${collection[2]}].name` : apiFocus.field === "recordedAt" ? "asOf" : undefined;
    (exact ?? candidates.find((element) => element.dataset.apiField === fallbackField) ?? formRef.current?.querySelector<HTMLElement>(".entry-step-panel h2"))?.focus();
  }, [apiFocus]);

  const addAsset = () => {
    const key = `new-asset-${nextKey.current++}`;
    setAssets((items) => [...items, { key, name: "", type: "", liquidity: "", amount: "", effectiveDate: snapshotDate, source: "" }]);
  };

  const addLiability = () => {
    const key = `new-liability-${nextKey.current++}`;
    setLiabilities((items) => [...items, { key, name: "", amount: "", effectiveDate: snapshotDate, source: "" }]);
  };

  const changeModeWithKeyboard = (event: KeyboardEvent<HTMLButtonElement>) => {
    const nextMode = event.key === "Home" ? "manual" : event.key === "End" ? "ai" : event.key === "ArrowLeft" || event.key === "ArrowRight" ? (mode === "ai" ? "manual" : "ai") : undefined;
    if (!nextMode) return;
    event.preventDefault();
    setMode(nextMode);
    (nextMode === "manual" ? manualModeTabRef : aiModeTabRef).current?.focus();
  };

  const confirmArchive = async () => {
    if (!archiveTarget) return;
    try {
      if (archiveTarget.kind === "asset") {
        await archiveAsset({ path: { id: archiveTarget.id } });
        setAssets((items) => items.filter((item) => item.key !== archiveTarget.key));
      } else {
        await archiveLiability({ path: { id: archiveTarget.id } });
        setLiabilities((items) => items.filter((item) => item.key !== archiveTarget.key));
      }
      setArchiveTarget(undefined);
    } catch {
      setError(`${archiveTarget.name} could not be archived. Try again.`);
    }
  };

  const previewImport = () => {
    setImportError(undefined);
    try {
      const data = parseAgentImport(
        agentJson,
        new Set(assets.flatMap((item) => item.id ? [item.id] : [])),
        new Set(liabilities.flatMap((item) => item.id ? [item.id] : [])),
      );
      if (data.baseCurrency !== baseCurrency) {
        throw new Error(`baseCurrency must match the shared Base currency ${baseCurrency}.`);
      }
      if (data.snapshotDate !== snapshotDate) {
        throw new Error(`snapshotDate must match the shared Snapshot date ${snapshotDate}.`);
      }
      const changes = [
        ...data.assets.flatMap((item) => item.id ? describeAssetUpdate(assets.find((asset) => asset.id === item.id)!, item) : [`Add asset: ${item.name}`]),
        ...data.liabilities.flatMap((item) => item.id ? describeLiabilityUpdate(liabilities.find((liability) => liability.id === item.id)!, item) : [`Add liability: ${item.name}`]),
      ];
      setImportReview({ data, changes });
    } catch (exception) {
      setImportReview(undefined);
      setImportError(exception instanceof Error ? exception.message : "Agent output could not be validated.");
    }
  };

  const applyImport = () => {
    if (!importReview) return;
    const data = importReview.data;
    setAssets((current) => mergeAssets(current, data, nextKey));
    setLiabilities((current) => mergeLiabilities(current, data, nextKey));
    setImportReview(undefined);
    setAgentJson("");
    setStep("review");
    setMode("manual");
  };

  const prompt = buildPrompt(includeDraftInPrompt, snapshotDate, baseCurrency, assets, liabilities);
  const baseCurrencyLocked = hasMonetaryFacts(assets, liabilities);
  const settingsComplete = Boolean(snapshotDate && isSupportedCurrency(baseCurrency));
  const assetsComplete = validAssets(assets, snapshotDate, baseCurrency);
  const liabilitiesComplete = validLiabilities(liabilities, snapshotDate, baseCurrency);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(undefined);
    if (!settingsComplete || !assetsComplete || !liabilitiesComplete) {
      const invalidStep: EntryStep = !assetsComplete ? "assets" : "liabilities";
      setShowValidation(true);
      setError("Complete every required field before saving the Snapshot.");
      if (settingsComplete) setStep(invalidStep);
      setValidationFocusRequest((request) => request + 1);
      return;
    }
    const body: CaptureSnapshotRequest = {
      asOf: instant(snapshotDate),
      recordedAt: new Date().toISOString(),
      baseCurrency,
      assets: assets.map((asset) => ({
        ...(asset.id ? { id: asset.id } : {}),
        name: asset.name,
        type: asset.type as AssetType,
        liquidity: asset.liquidity as Liquidity,
        money: { amount: asset.amount, currency: baseCurrency },
        effectiveAt: instant(asset.effectiveDate),
        source: asset.source,
        ...(asset.manualConversion ? { manualConversion: {
          originalMoney: { amount: asset.manualConversion.originalAmount, currency: asset.manualConversion.originalCurrency },
          exchangeRateBasis: asset.manualConversion.exchangeRateBasis,
          effectiveAt: instant(asset.manualConversion.effectiveDate),
        } } : {}),
      })),
      liabilities: liabilities.map((liability) => ({
        ...(liability.id ? { id: liability.id } : {}),
        name: liability.name,
        money: { amount: liability.amount, currency: baseCurrency },
        effectiveAt: instant(liability.effectiveDate),
        source: liability.source,
        ...(liability.manualConversion ? { manualConversion: {
          originalMoney: { amount: liability.manualConversion.originalAmount, currency: liability.manualConversion.originalCurrency },
          exchangeRateBasis: liability.manualConversion.exchangeRateBasis,
          effectiveAt: instant(liability.manualConversion.effectiveDate),
        } } : {}),
      })),
    };
    setSaving(true);
    try {
      const response = await captureSnapshot({ body });
      if (response.error) {
        const validation = response.error as ValidationProblemResponse;
        const validationErrors = validation.errors?.filter((item) => item.field) ?? [];
        const first = validationErrors[0];
        const firstField = first?.field;
        if (firstField) {
          setError(validationErrors.map(formatApiError).join(" "));
          setStep(stepForApiField(firstField));
          setApiFocus((current) => ({ field: firstField, request: (current?.request ?? 0) + 1 }));
          setSaving(false);
          return;
        }
        throw new Error("Snapshot request failed");
      }
      if (!response.data?.id) throw new Error("Snapshot response did not include an ID");
      router.push(`/?snapshot=${response.data.id}`);
    } catch {
      setError("The Snapshot could not be saved. Your entries are still here; try again.");
      setSaving(false);
    }
  };

  return (
    <main className="entry-page">
      <AppNavigation current="input" />
      <header className="entry-header">
        <div><p className="eyebrow">Balance-sheet entry</p><h1>Update balance sheet</h1></div>
      </header>
      {loading ? <p>Loading current positions…</p> : <form ref={formRef} onSubmit={submit} noValidate>
        <section className="snapshot-context" aria-labelledby="snapshot-context-title">
          <div className="step-heading"><p className="eyebrow">Shared settings</p><h2 id="snapshot-context-title">Snapshot context</h2><p>These values apply to both AI-assisted import and manual entry.</p></div>
          <div className="entry-settings">
            <label>Snapshot date<input data-api-field="asOf" aria-label="Snapshot date" aria-invalid={showValidation && !snapshotDate || undefined} type="date" value={snapshotDate} onChange={(event) => setSnapshotDate(event.target.value)} required />{showValidation && !snapshotDate && <span className="field-error">Snapshot date is required.</span>}</label>
            <label>Base currency<input data-api-field="baseCurrency" aria-label="Base currency" aria-invalid={showValidation && !isSupportedCurrency(baseCurrency) || undefined} aria-describedby={baseCurrencyLocked ? "base-currency-locked" : showValidation && !baseCurrency ? "base-currency-error" : undefined} value={baseCurrency} onChange={(event) => setBaseCurrency(event.target.value.toUpperCase())} readOnly={baseCurrencyLocked} maxLength={3} pattern="[A-Z]{3}" placeholder="TWD" required />{baseCurrencyLocked && <span id="base-currency-locked" className="field-help">Clear all position amounts before changing the base currency.</span>}{showValidation && !baseCurrency && <span id="base-currency-error" className="field-error">Base currency is required.</span>}{showValidation && baseCurrency && !isSupportedCurrency(baseCurrency) && <span className="field-error">Base currency must be a supported ISO 4217 currency.</span>}</label>
          </div>
        </section>

        <div className="input-mode-tabs" role="tablist" aria-label="Input mode">
          <button ref={manualModeTabRef} id="input-mode-manual-tab" role="tab" type="button" aria-selected={mode === "manual"} aria-controls="input-mode-manual-panel" tabIndex={mode === "manual" ? 0 : -1} onClick={() => setMode("manual")} onKeyDown={changeModeWithKeyboard}>Manual entry</button>
          <button ref={aiModeTabRef} id="input-mode-ai-tab" role="tab" type="button" aria-selected={mode === "ai"} aria-controls="input-mode-ai-panel" tabIndex={mode === "ai" ? 0 : -1} onClick={() => setMode("ai")} onKeyDown={changeModeWithKeyboard}>AI-assisted import</button>
        </div>

        {mode === "manual" && <div id="input-mode-manual-panel" role="tabpanel" aria-labelledby="input-mode-manual-tab">
          <EntryStepper
            current={step}
            completed={{ assets: assetsComplete, liabilities: liabilitiesComplete, review: settingsComplete && assetsComplete && liabilitiesComplete }}
            onChange={setStep}
          />

        {step === "assets" && <section className="entry-step-panel" aria-labelledby="assets-step-title">
          <div className="step-heading"><p className="eyebrow">Step 1 of 3</p><h2 id="assets-step-title" tabIndex={-1}>Assets</h2><p>Review every active asset and its value for this Snapshot.</p></div>
          <EntrySection title="Assets" apiField="assets" onAdd={addAsset} addLabel="Add asset">
            {assets.map((asset, index) => <AssetFields key={asset.key} index={index} draft={asset} isNew={!asset.id} snapshotDate={snapshotDate} baseCurrency={baseCurrency} showValidation={showValidation} onChange={(next) => setAssets((items) => items.map((item, itemIndex) => itemIndex === index ? next : item))} onArchive={asset.id ? () => setArchiveTarget({ kind: "asset", id: asset.id!, key: asset.key, name: asset.name }) : undefined} />)}
          </EntrySection>
          <StepActions next="liabilities" onChange={setStep} />
        </section>}

        {step === "liabilities" && <section className="entry-step-panel" aria-labelledby="liabilities-step-title">
          <div className="step-heading"><p className="eyebrow">Step 2 of 3</p><h2 id="liabilities-step-title" tabIndex={-1}>Liabilities</h2><p>Review every active liability and its balance for this Snapshot.</p></div>
          <EntrySection title="Liabilities" apiField="liabilities" onAdd={addLiability} addLabel="Add liability">
            {liabilities.map((liability, index) => <LiabilityFields key={liability.key} index={index} draft={liability} isNew={!liability.id} snapshotDate={snapshotDate} baseCurrency={baseCurrency} showValidation={showValidation} onChange={(next) => setLiabilities((items) => items.map((item, itemIndex) => itemIndex === index ? next : item))} onArchive={liability.id ? () => setArchiveTarget({ kind: "liability", id: liability.id!, key: liability.key, name: liability.name }) : undefined} />)}
          </EntrySection>
          <StepActions previous="assets" next="review" onChange={setStep} />
        </section>}

        {step === "review" && <section className="entry-step-panel review-step" aria-labelledby="review-step-title">
          <div className="step-heading"><p className="eyebrow">Step 3 of 3</p><h2 id="review-step-title" tabIndex={-1}>Review and save</h2><p>Confirm the complete balance sheet before creating the immutable Snapshot.</p></div>
          <dl className="review-summary">
            <div><dt>Snapshot date</dt><dd>{snapshotDate || "Missing"}</dd></div>
            <div><dt>Base currency</dt><dd>{baseCurrency || "Missing"}</dd></div>
            <div><dt>Assets</dt><dd>{assets.length}</dd></div>
            <div><dt>Liabilities</dt><dd>{liabilities.length}</dd></div>
          </dl>
          {(!settingsComplete || !assetsComplete || !liabilitiesComplete) && <p className="review-warning">Some steps still contain missing or invalid values. Saving will take you to the first field to fix.</p>}
          <div className="step-actions"><button type="button" onClick={() => setStep("liabilities")}>Back</button><button className="primary-action" type="submit" disabled={saving}>{saving ? "Saving…" : "Save Snapshot"}</button></div>
        </section>}
        </div>}

        {mode === "ai" && <section id="input-mode-ai-panel" role="tabpanel" aria-labelledby="input-mode-ai-tab" className="ai-import">
          <div><p className="eyebrow">Optional accelerator</p><h2>AI-assisted import</h2></div>
          <p>Copy the Prompt to your own AI agent, then paste its JSON response here. Nothing is sent automatically.</p>
          <label className="checkbox-label"><input type="checkbox" checked={includeDraftInPrompt} onChange={(event) => setIncludeDraftInPrompt(event.target.checked)} />Include current draft in Prompt</label>
          {includeDraftInPrompt && <p className="sensitive-warning">This Prompt contains sensitive financial data.</p>}
          <label>AI Prompt<textarea aria-label="AI Prompt" value={prompt} readOnly rows={12} /></label>
          <button type="button" onClick={() => navigator.clipboard?.writeText(prompt)}>Copy Prompt</button>
          <label>Agent JSON<textarea aria-label="Agent JSON" value={agentJson} onChange={(event) => setAgentJson(event.target.value)} rows={12} placeholder="Paste raw JSON or one JSON code block" /></label>
          {importError && <p className="form-error" role="alert">{importError}</p>}
          <button type="button" onClick={previewImport}>Preview import</button>
        </section>}

        {archiveTarget && <ModalDialog label={`Archive ${archiveTarget.name}?`} onDismiss={() => setArchiveTarget(undefined)}>
          <h2>Archive {archiveTarget.name}?</h2>
          <p>It will leave the current entry list. Saved Snapshots will not change.</p>
          <div><button type="button" onClick={() => setArchiveTarget(undefined)}>Cancel</button><button type="button" className="danger-action" onClick={confirmArchive}>Confirm archive</button></div>
        </ModalDialog>}
        {importReview && <ModalDialog label="Review AI import" onDismiss={() => setImportReview(undefined)}>
          <h2>Review AI import</h2>
          {importReview.changes.length === 0 ? <p>No additions or changes were supplied.</p> : <ul>{importReview.changes.map((change) => <li key={change}>{change}</li>)}</ul>}
          <p>This changes only the current form. It does not call the API or archive omitted positions.</p>
          <div><button type="button" onClick={() => setImportReview(undefined)}>Cancel</button><button type="button" onClick={applyImport}>Apply import</button></div>
        </ModalDialog>}
        {error && <p className="form-error" role="alert">{error}</p>}
      </form>}
    </main>
  );
}

const entrySteps: Array<{ id: EntryStep; label: string }> = [
  { id: "assets", label: "Assets" },
  { id: "liabilities", label: "Liabilities" },
  { id: "review", label: "Review" },
];

function EntryStepper({ current, completed, onChange }: { current: EntryStep; completed: Record<EntryStep, boolean>; onChange: (step: EntryStep) => void }) {
  return <nav className="entry-stepper" aria-label="Entry steps">{entrySteps.map((item, index) =>
    <button key={item.id} type="button" aria-label={item.label} aria-current={current === item.id ? "step" : undefined} data-complete={completed[item.id] || undefined} onClick={() => onChange(item.id)}>
      <span aria-hidden="true" className="step-number">{completed[item.id] ? "✓" : index + 1}</span>
      <span>{item.label}</span>
    </button>,
  )}</nav>;
}

function StepActions({ previous, next, onChange }: { previous?: EntryStep; next: EntryStep; onChange: (step: EntryStep) => void }) {
  return <div className="step-actions">{previous && <button type="button" onClick={() => onChange(previous)}>Back</button>}<button className="primary-action" type="button" onClick={() => onChange(next)}>Continue</button></div>;
}

function EntrySection({ title, apiField, onAdd, addLabel, children }: { title: string; apiField: "assets" | "liabilities"; onAdd: () => void; addLabel: string; children: React.ReactNode }) {
  return <section className="entry-section"><div className="section-heading"><h2>{title}</h2><button data-api-field={apiField} type="button" onClick={onAdd}>{addLabel}</button></div><div className="entry-list">{children}</div></section>;
}

function ModalDialog({ label, onDismiss, children }: { label: string; onDismiss: () => void; children: React.ReactNode }) {
  const dialogRef = useRef<HTMLElement>(null);
  const invokingElement = useRef<HTMLElement | null>(typeof document === "undefined" ? null : document.activeElement as HTMLElement | null);

  useEffect(() => {
    const dialog = dialogRef.current;
    const first = dialog?.querySelector<HTMLElement>(focusableSelector);
    first?.focus();
    return () => invokingElement.current?.focus();
  }, []);

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (event.key === "Escape") {
      event.preventDefault();
      onDismiss();
      return;
    }
    if (event.key !== "Tab") return;
    const focusable = Array.from(dialogRef.current?.querySelectorAll<HTMLElement>(focusableSelector) ?? []);
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable.at(-1)!;
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  return <section ref={dialogRef} className="confirm-dialog" role="dialog" aria-modal="true" aria-label={label} onKeyDown={handleKeyDown}>{children}</section>;
}

const focusableSelector = "button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex='-1'])";

function AssetFields({ index, draft, isNew, snapshotDate, baseCurrency, showValidation, onChange, onArchive }: { index: number; draft: AssetDraft; isNew: boolean; snapshotDate: string; baseCurrency: string; showValidation: boolean; onChange: (draft: AssetDraft) => void; onArchive?: () => void }) {
  const label = isNew ? "New asset" : draft.name;
  const invalidName = showValidation && !draft.name.trim();
  const invalidType = showValidation && !draft.type;
  const invalidLiquidity = showValidation && !draft.liquidity;
  const invalidAmount = showValidation && !decimal.test(draft.amount);
  const invalidDate = showValidation && (!draft.effectiveDate || draft.effectiveDate > snapshotDate);
  const invalidSource = showValidation && !draft.source.trim();
  return <fieldset className="position-editor" aria-label={label}><legend>{label}</legend>
    <label>Name<input data-api-field={`assets[${index}].name`} aria-label={isNew ? "Name" : `${label} name`} aria-invalid={invalidName || undefined} value={draft.name} maxLength={200} onChange={(event) => onChange({ ...draft, name: event.target.value })} required />{invalidName && <span className="field-error">Name is required.</span>}</label>
    <label>Type<select data-api-field={`assets[${index}].type`} aria-label={isNew ? "Type" : `${label} type`} aria-invalid={invalidType || undefined} value={draft.type} onChange={(event) => onChange({ ...draft, type: event.target.value as AssetType | "" })} required><option value="">Choose type</option>{assetTypes.map((value) => <option key={value}>{value}</option>)}</select>{invalidType && <span className="field-error">Type is required.</span>}</label>
    <label>Liquidity<select data-api-field={`assets[${index}].liquidity`} aria-label={isNew ? "Liquidity" : `${label} liquidity`} aria-invalid={invalidLiquidity || undefined} value={draft.liquidity} onChange={(event) => onChange({ ...draft, liquidity: event.target.value as Liquidity | "" })} required><option value="">Choose liquidity</option>{liquidities.map((value) => <option key={value}>{value}</option>)}</select>{invalidLiquidity && <span className="field-error">Liquidity is required.</span>}</label>
    <label>Amount<input data-api-field={`assets[${index}].money.amount`} aria-label={isNew ? "Amount" : `${label} amount`} aria-invalid={invalidAmount || undefined} inputMode="decimal" pattern="(?:0|[1-9][0-9]*)(?:\.[0-9]+)?" value={draft.amount} onChange={(event) => onChange({ ...draft, amount: event.target.value })} required />{invalidAmount && <span className="field-error">Amount must be a non-negative decimal.</span>}</label>
    <label>Effective date<input data-api-field={`assets[${index}].effectiveAt`} aria-label={isNew ? "Effective date" : `${label} effective date`} aria-invalid={invalidDate || undefined} type="date" max={snapshotDate} value={draft.effectiveDate} onChange={(event) => onChange({ ...draft, effectiveDate: event.target.value })} required />{invalidDate && <span className="field-error">Effective date is required and cannot be after the Snapshot date.</span>}</label>
    <label>Source<input data-api-field={`assets[${index}].source`} aria-label={isNew ? "Source" : `${label} source`} aria-invalid={invalidSource || undefined} value={draft.source} maxLength={100} onChange={(event) => onChange({ ...draft, source: event.target.value })} required />{invalidSource && <span className="field-error">Source is required.</span>}</label>
    <ManualConversionFields label={label} apiField={`assets[${index}].manualConversion`} value={draft.manualConversion} baseCurrency={baseCurrency} snapshotDate={snapshotDate} showValidation={showValidation} onChange={(manualConversion) => onChange({ ...draft, manualConversion })} />
    {draft.carriedFrom && <p className="carried-note">Carried forward from {displayDate(draft.carriedFrom)}</p>}
    {onArchive && <button type="button" className="archive-action" aria-label={`Archive ${label}`} onClick={onArchive}>Archive</button>}
  </fieldset>;
}

function LiabilityFields({ index, draft, isNew, snapshotDate, baseCurrency, showValidation, onChange, onArchive }: { index: number; draft: LiabilityDraft; isNew: boolean; snapshotDate: string; baseCurrency: string; showValidation: boolean; onChange: (draft: LiabilityDraft) => void; onArchive?: () => void }) {
  const label = isNew ? "New liability" : draft.name;
  const invalidName = showValidation && !draft.name.trim();
  const invalidAmount = showValidation && !decimal.test(draft.amount);
  const invalidDate = showValidation && (!draft.effectiveDate || draft.effectiveDate > snapshotDate);
  const invalidSource = showValidation && !draft.source.trim();
  return <fieldset className="position-editor" aria-label={label}><legend>{label}</legend>
    <label>Name<input data-api-field={`liabilities[${index}].name`} aria-label={isNew ? "Name" : `${label} name`} aria-invalid={invalidName || undefined} value={draft.name} maxLength={200} onChange={(event) => onChange({ ...draft, name: event.target.value })} required />{invalidName && <span className="field-error">Name is required.</span>}</label>
    <label>Amount<input data-api-field={`liabilities[${index}].money.amount`} aria-label={isNew ? "Amount" : `${label} amount`} aria-invalid={invalidAmount || undefined} inputMode="decimal" pattern="(?:0|[1-9][0-9]*)(?:\.[0-9]+)?" value={draft.amount} onChange={(event) => onChange({ ...draft, amount: event.target.value })} required />{invalidAmount && <span className="field-error">Amount must be a non-negative decimal.</span>}</label>
    <label>Effective date<input data-api-field={`liabilities[${index}].effectiveAt`} aria-label={isNew ? "Effective date" : `${label} effective date`} aria-invalid={invalidDate || undefined} type="date" max={snapshotDate} value={draft.effectiveDate} onChange={(event) => onChange({ ...draft, effectiveDate: event.target.value })} required />{invalidDate && <span className="field-error">Effective date is required and cannot be after the Snapshot date.</span>}</label>
    <label>Source<input data-api-field={`liabilities[${index}].source`} aria-label={isNew ? "Source" : `${label} source`} aria-invalid={invalidSource || undefined} value={draft.source} maxLength={100} onChange={(event) => onChange({ ...draft, source: event.target.value })} required />{invalidSource && <span className="field-error">Source is required.</span>}</label>
    <ManualConversionFields label={label} apiField={`liabilities[${index}].manualConversion`} value={draft.manualConversion} baseCurrency={baseCurrency} snapshotDate={snapshotDate} showValidation={showValidation} onChange={(manualConversion) => onChange({ ...draft, manualConversion })} />
    {draft.carriedFrom && <p className="carried-note">Carried forward from {displayDate(draft.carriedFrom)}</p>}
    {onArchive && <button type="button" className="archive-action" aria-label={`Archive ${label}`} onClick={onArchive}>Archive</button>}
  </fieldset>;
}

function ManualConversionFields({ label, apiField, value, baseCurrency, snapshotDate, showValidation, onChange }: { label: string; apiField: string; value?: ManualConversionDraft; baseCurrency: string; snapshotDate: string; showValidation: boolean; onChange: (value?: ManualConversionDraft) => void }) {
  const invalidAmount = showValidation && value && !decimal.test(value.originalAmount);
  const invalidCurrency = showValidation && value && (!isSupportedCurrency(value.originalCurrency) || value.originalCurrency === baseCurrency);
  const invalidBasis = showValidation && value && !value.exchangeRateBasis.trim();
  const invalidDate = showValidation && value && (!value.effectiveDate || value.effectiveDate > snapshotDate);
  return <div className="manual-conversion">
    <label className="checkbox-label"><input
      aria-label={`${label} converted from another currency`}
      type="checkbox"
      checked={Boolean(value)}
      onChange={(event) => onChange(event.target.checked ? { originalAmount: "", originalCurrency: "", exchangeRateBasis: "", effectiveDate: snapshotDate } : undefined)}
    />Converted from another currency</label>
    {value && <div className="manual-conversion-fields">
      <label>Original amount<input data-api-field={`${apiField}.originalMoney.amount`} aria-label={`${label} original amount`} aria-invalid={invalidAmount || undefined} inputMode="decimal" pattern="(?:0|[1-9][0-9]*)(?:\.[0-9]+)?" value={value.originalAmount} onChange={(event) => onChange({ ...value, originalAmount: event.target.value })} required />{invalidAmount && <span className="field-error">Original amount must be a non-negative decimal.</span>}</label>
      <label>Original currency<input data-api-field={`${apiField}.originalMoney.currency`} aria-label={`${label} original currency`} aria-invalid={invalidCurrency || undefined} value={value.originalCurrency} onChange={(event) => onChange({ ...value, originalCurrency: event.target.value.toUpperCase() })} maxLength={3} pattern="[A-Z]{3}" required />{invalidCurrency && <span className="field-error">Original currency must be a supported ISO 4217 code different from the base currency.</span>}</label>
      <label>Exchange-rate basis<input data-api-field={`${apiField}.exchangeRateBasis`} aria-label={`${label} exchange-rate basis`} aria-invalid={invalidBasis || undefined} value={value.exchangeRateBasis} onChange={(event) => onChange({ ...value, exchangeRateBasis: event.target.value })} maxLength={200} required />{invalidBasis && <span className="field-error">Exchange-rate basis is required.</span>}</label>
      <label>Conversion effective date<input data-api-field={`${apiField}.effectiveAt`} aria-label={`${label} conversion effective date`} aria-invalid={invalidDate || undefined} type="date" max={snapshotDate} value={value.effectiveDate} onChange={(event) => onChange({ ...value, effectiveDate: event.target.value })} required />{invalidDate && <span className="field-error">Conversion effective date is required and cannot be after the Snapshot date.</span>}</label>
    </div>}
  </div>;
}

function hydrate(
  currentAssets: AssetResponse[],
  currentLiabilities: LiabilityResponse[],
  snapshot: SnapshotResponse | undefined,
  setAssets: (items: AssetDraft[]) => void,
  setLiabilities: (items: LiabilityDraft[]) => void,
  setBaseCurrency: (currency: string) => void,
) {
  const assetFacts = new Map((snapshot?.assets ?? []).map((fact) => [fact.id, fact]));
  const liabilityFacts = new Map((snapshot?.liabilities ?? []).map((fact) => [fact.id, fact]));
  const currency = snapshot?.assets?.[0]?.money?.currency ?? snapshot?.liabilities?.[0]?.money?.currency ?? "";
  setBaseCurrency(currency);
  setAssets(currentAssets.map((asset, index) => assetDraft(asset, assetFacts.get(asset.id), snapshot?.asOf, index)));
  setLiabilities(currentLiabilities.map((liability, index) => liabilityDraft(liability, liabilityFacts.get(liability.id), snapshot?.asOf, index)));
}

function assetDraft(asset: AssetResponse, fact: AssetFactResponse | undefined, carriedFrom: string | undefined, index: number): AssetDraft {
  return { key: asset.id ?? `asset-${index}`, id: asset.id, name: asset.name ?? "", type: assetTypes.find((value) => value === asset.type) ?? "", liquidity: liquidities.find((value) => value === asset.liquidity) ?? "", amount: fact?.money?.amount ?? "", effectiveDate: day(fact?.effectiveAt) || today(), source: fact?.source ?? "", manualConversion: conversionDraft(fact?.manualConversion), carriedFrom: fact ? carriedFrom : undefined };
}

function liabilityDraft(liability: LiabilityResponse, fact: LiabilityFactResponse | undefined, carriedFrom: string | undefined, index: number): LiabilityDraft {
  return { key: liability.id ?? `liability-${index}`, id: liability.id, name: liability.name ?? "", amount: fact?.money?.amount ?? "", effectiveDate: day(fact?.effectiveAt) || today(), source: fact?.source ?? "", manualConversion: conversionDraft(fact?.manualConversion), carriedFrom: fact ? carriedFrom : undefined };
}

function conversionDraft(conversion: AssetFactResponse["manualConversion"]): ManualConversionDraft | undefined {
  if (!conversion) return undefined;
  return { originalAmount: conversion.originalMoney?.amount ?? "", originalCurrency: conversion.originalMoney?.currency ?? "", exchangeRateBasis: conversion.exchangeRateBasis ?? "", effectiveDate: day(conversion.effectiveAt) };
}

function validAssets(assets: AssetDraft[], snapshotDate: string, baseCurrency: string): boolean {
  return assets.every((item) => Boolean(item.name.trim() && item.type && item.liquidity && decimal.test(item.amount) && item.effectiveDate && item.effectiveDate <= snapshotDate && item.source.trim() && validConversion(item.manualConversion, snapshotDate, baseCurrency)));
}

function validLiabilities(liabilities: LiabilityDraft[], snapshotDate: string, baseCurrency: string): boolean {
  return liabilities.every((item) => Boolean(item.name.trim() && decimal.test(item.amount) && item.effectiveDate && item.effectiveDate <= snapshotDate && item.source.trim() && validConversion(item.manualConversion, snapshotDate, baseCurrency)));
}

function hasMonetaryFacts(assets: AssetDraft[], liabilities: LiabilityDraft[]): boolean {
  return assets.some((item) => item.amount.trim()) || liabilities.some((item) => item.amount.trim());
}

function describeAssetUpdate(current: AssetDraft, imported: AgentImportAsset): string[] {
  return describeChangedFields(`Asset ${current.name}`, [
    ["Name", current.name, imported.name],
    ["Type", current.type, imported.type],
    ["Liquidity", current.liquidity, imported.liquidity],
    ["Amount", current.amount, imported.amount],
    ["Effective date", current.effectiveDate, imported.effectiveDate],
    ["Source", current.source, imported.source],
    ["Manual conversion", describeManualConversion(current.manualConversion), describeManualConversion(imported.manualConversion)],
  ]);
}

function describeLiabilityUpdate(current: LiabilityDraft, imported: AgentImportLiability): string[] {
  return describeChangedFields(`Liability ${current.name}`, [
    ["Name", current.name, imported.name],
    ["Amount", current.amount, imported.amount],
    ["Effective date", current.effectiveDate, imported.effectiveDate],
    ["Source", current.source, imported.source],
    ["Manual conversion", describeManualConversion(current.manualConversion), describeManualConversion(imported.manualConversion)],
  ]);
}

function describeChangedFields(subject: string, fields: Array<[string, string, string]>): string[] {
  return fields.flatMap(([label, before, after]) => before === after ? [] : [`${subject} — ${label}: ${before} → ${after}`]);
}

function describeManualConversion(value?: ManualConversionDraft | AgentImportManualConversion): string {
  return value ? `${value.originalAmount} ${value.originalCurrency}; ${value.exchangeRateBasis}; effective ${value.effectiveDate}` : "None";
}

function validConversion(value: ManualConversionDraft | undefined, snapshotDate: string, baseCurrency: string): boolean {
  return !value || Boolean(decimal.test(value.originalAmount) && isSupportedCurrency(value.originalCurrency) && value.originalCurrency !== baseCurrency && value.exchangeRateBasis.trim() && value.effectiveDate && value.effectiveDate <= snapshotDate);
}

function stepForApiField(field: string): EntryStep {
  if (field.startsWith("assets")) return "assets";
  if (field.startsWith("liabilities")) return "liabilities";
  return "assets";
}

function formatApiError(error: { field?: string; message?: string }): string {
  const field = error.field ?? "request";
  const collection = field.match(/^(assets|liabilities)\[(\d+)](?:\.(.+))?$/);
  const suffix = collection?.[3]
    ?.replace("money.amount", "amount")
    .replace("money.currency", "currency")
    .replace("effectiveAt", "effective date")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .toLowerCase();
  const label = collection
    ? `${collection[1] === "assets" ? "Asset" : "Liability"} ${Number(collection[2]) + 1}${suffix ? ` ${suffix}` : ""}`
    : field === "assets" ? "Assets"
      : field === "liabilities" ? "Liabilities"
        : field === "baseCurrency" ? "Base currency"
      : field === "asOf" ? "Snapshot date"
        : field === "recordedAt" ? "Recorded time"
          : "Request";
  const message = error.message ?? "is invalid";
  return `${label} ${message}${/[.!?]$/.test(message) ? "" : "."}`;
}

function mergeAssets(current: AssetDraft[], imported: AgentImport, nextKey: { current: number }): AssetDraft[] {
  const updates = new Map(imported.assets.flatMap((item) => item.id ? [[item.id, item] as const] : []));
  const updated = current.map((draft) => {
    const item = draft.id ? updates.get(draft.id) : undefined;
    return item ? { ...draft, name: item.name, type: item.type, liquidity: item.liquidity, amount: item.amount, effectiveDate: item.effectiveDate, source: item.source, manualConversion: item.manualConversion, carriedFrom: undefined } : draft;
  });
  const additions = imported.assets.filter((item) => !item.id).map((item) => ({ key: `imported-asset-${nextKey.current++}`, name: item.name, type: item.type, liquidity: item.liquidity, amount: item.amount, effectiveDate: item.effectiveDate, source: item.source, manualConversion: item.manualConversion }));
  return [...updated, ...additions];
}

function mergeLiabilities(current: LiabilityDraft[], imported: AgentImport, nextKey: { current: number }): LiabilityDraft[] {
  const updates = new Map(imported.liabilities.flatMap((item) => item.id ? [[item.id, item] as const] : []));
  const updated = current.map((draft) => {
    const item = draft.id ? updates.get(draft.id) : undefined;
    return item ? { ...draft, name: item.name, amount: item.amount, effectiveDate: item.effectiveDate, source: item.source, manualConversion: item.manualConversion, carriedFrom: undefined } : draft;
  });
  const additions = imported.liabilities.filter((item) => !item.id).map((item) => ({ key: `imported-liability-${nextKey.current++}`, name: item.name, amount: item.amount, effectiveDate: item.effectiveDate, source: item.source, manualConversion: item.manualConversion }));
  return [...updated, ...additions];
}

function buildPrompt(includeDraft: boolean, snapshotDate: string, baseCurrency: string, assets: AssetDraft[], liabilities: LiabilityDraft[]): string {
  const contextReady = isSupportedCurrency(baseCurrency) && Boolean(snapshotDate);
  const exampleBaseCurrency = contextReady ? baseCurrency : "TWD";
  const exampleOriginalCurrency = exampleBaseCurrency === "USD" ? "EUR" : "USD";
  const shape = {
    schemaVersion: 1,
    baseCurrency: exampleBaseCurrency,
    snapshotDate: snapshotDate || "YYYY-MM-DD",
    assets: [{ name: "Example foreign-currency asset", type: "CASH", liquidity: "LIQUID", amount: "1250.00", effectiveDate: snapshotDate, source: "Statement description", manualConversion: { originalAmount: "1000.00", originalCurrency: exampleOriginalCurrency, exchangeRateBasis: `Declared ${exampleOriginalCurrency}/${exampleBaseCurrency} rate 1.25`, effectiveDate: snapshotDate } }],
    liabilities: [{ name: "Example liability", amount: "250.00", effectiveDate: snapshotDate, source: "Statement description" }],
  };
  const instructions = [
    "You are conducting a complete personal balance-sheet inventory interview. Do not assume the user's first answer is the full inventory.",
    "Ask one concise question at a time and wait for the user's answer before continuing.",
    contextReady
      ? `Use this exact Snapshot context: Base currency ${baseCurrency}; Snapshot date ${snapshotDate}. Do not ask the user to choose different values, and return these exact values in the final JSON.`
      : "Set a valid Base currency in Wealth OS before using this Prompt.",
    "Explicitly cover every asset category: Cash and bank accounts; Investments and retirement accounts; Real estate; Vehicles; Business ownership; and Other assets such as valuables or receivables. Ask about every category even when the user has not mentioned it.",
    "Explicitly cover Mortgages, credit cards, personal or business loans, taxes owed, and other liabilities. Ask whether jointly held, foreign-currency, or easily forgotten positions remain.",
    "For every asset, collect name, amount, effective date, source, type, and liquidity. For every liability, collect name, amount, effective date, and source. Ask follow-up questions whenever a required value is missing.",
    "For each foreign-currency position, manually convert it to the base currency. Put the converted value in amount and include manualConversion with originalAmount, originalCurrency, exchangeRateBasis, and effectiveDate. Omit manualConversion for positions already expressed in the base currency.",
    "Use type CASH, INVESTMENT, REAL_ESTATE, VEHICLE, BUSINESS, or OTHER. Use liquidity LIQUID, SEMI_LIQUID, or ILLIQUID based on how readily the asset can be converted to cash.",
    "Summarize the inventory and ask the user to confirm that it is complete. Do not return the final JSON until the user confirms the inventory is complete.",
    "After confirmation, return only valid JSON matching this schema. Do not add Markdown or explanations to the final response.",
    "schemaVersion must be 1. Preserve every provided id exactly; omit id for new positions and never invent one.",
    "Amounts must be non-negative decimal strings. Dates must be YYYY-MM-DD. Do not add unknown fields or archive instructions.",
    `FORMAT EXAMPLE:\n${JSON.stringify(shape, null, 2)}`,
  ];
  if (includeDraft) {
    instructions.push("Treat CURRENT_DRAFT as the starting inventory. Verify every existing position, preserve each provided id exactly, and continue interviewing for missing positions.");
    instructions.push(`CURRENT_DRAFT:\n${JSON.stringify({ schemaVersion: 1, baseCurrency, snapshotDate, assets: assets.map(({ id, name, type, liquidity, amount, effectiveDate, source, manualConversion }) => ({ ...(id ? { id } : {}), name, type, liquidity, amount, effectiveDate, source, ...(manualConversion ? { manualConversion } : {}) })), liabilities: liabilities.map(({ id, name, amount, effectiveDate, source, manualConversion }) => ({ ...(id ? { id } : {}), name, amount, effectiveDate, source, ...(manualConversion ? { manualConversion } : {}) })) }, null, 2)}`);
  }
  return instructions.join("\n\n");
}
