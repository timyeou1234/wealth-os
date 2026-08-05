import React from "react";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import EntryPage from "./page";

const assetId = "0f27e4fa-99f8-4c5e-87da-527488cbe515";
const api = vi.hoisted(() => ({
  archiveAsset: vi.fn(), archiveLiability: vi.fn(), captureSnapshot: vi.fn(), getSnapshot: vi.fn(),
  getFxRates: vi.fn(), listAssets: vi.fn(), listLiabilities: vi.fn(), listSnapshots: vi.fn(),
}));
const navigation = vi.hoisted(() => ({ push: vi.fn() }));

vi.mock("../api/client", () => api);
vi.mock("next/navigation", () => ({ useRouter: () => navigation }));

beforeEach(() => {
  vi.clearAllMocks();
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(new Date("2026-08-02T08:00:00Z"));
  api.listAssets.mockResolvedValue({ data: [{ id: assetId, name: "Cash", type: "CASH", liquidity: "LIQUID", archived: false }] });
  api.listLiabilities.mockResolvedValue({ data: [] });
  api.listSnapshots.mockResolvedValue({ data: [{ id: "snapshot-1", asOf: "2026-07-31T00:00:00Z" }] });
  api.getSnapshot.mockResolvedValue({ data: {
    id: "snapshot-1", asOf: "2026-07-31T00:00:00Z", recordedAt: "2026-07-31T08:00:00Z", baseCurrency: "TWD",
    assets: [{ id: assetId, name: "Cash", type: "CASH", liquidity: "LIQUID", money: { amount: "1000.00", currency: "TWD" }, effectiveAt: "2026-07-30T00:00:00Z", source: "Bank statement" }], liabilities: [],
  } });
  api.captureSnapshot.mockResolvedValue({ data: { id: "snapshot-created" } });
  api.getFxRates.mockResolvedValue({ data: { valuationCurrency: "TWD", asOf: "2026-08-02", rates: [{ originalCurrency: "USD", rate: "32.1", rateDate: "2026-08-01", provider: "CBC", rateType: "CLOSING_SPOT" }], missingCurrencies: [] } });
  api.archiveAsset.mockResolvedValue({});
  api.archiveLiability.mockResolvedValue({});
});

afterEach(() => { vi.useRealTimers(); cleanup(); });
async function selectManualEntry() { fireEvent.click(await screen.findByRole("tab", { name: "Manual entry" })); }

describe("Balance-sheet entry", () => {
  it("shows app navigation and opens AI-assisted import by default", async () => {
    render(<EntryPage />);
    expect((await screen.findByRole("tab", { name: "AI-assisted import" })).getAttribute("aria-selected")).toBe("true");
    expect(screen.getByRole("link", { name: "Dashboard" }).getAttribute("href")).toBe("/");
    expect(screen.getByRole("link", { name: "Input" }).getAttribute("aria-current")).toBe("page");
  });

  it("uses fixed TWD valuation context and updates the Prompt date", async () => {
    render(<EntryPage />);
    const valuation = await screen.findByLabelText("Valuation currency") as HTMLInputElement;
    expect(valuation.value).toBe("TWD");
    expect(valuation.disabled).toBe(true);
    const prompt = screen.getByLabelText("AI Prompt") as HTMLTextAreaElement;
    expect(prompt.value).toContain('"schemaVersion": 2');
    expect(prompt.value).toContain('"currency": "USD"');
    expect(prompt.value).toContain("Do not calculate or invent an exchange rate");
    expect(prompt.value).not.toContain("manualConversion");
    fireEvent.change(screen.getByLabelText("Snapshot date"), { target: { value: "2026-08-01" } });
    expect(prompt.value).toContain("Snapshot date 2026-08-01");
  });

  it("switches Input modes with standard tab keyboard controls", async () => {
    render(<EntryPage />);
    const ai = await screen.findByRole("tab", { name: "AI-assisted import" });
    const manual = screen.getByRole("tab", { name: "Manual entry" });
    ai.focus(); fireEvent.keyDown(ai, { key: "ArrowLeft" });
    expect(manual.getAttribute("aria-selected")).toBe("true");
    expect(document.activeElement).toBe(manual);
  });

  it("previews and applies schema v2 agent JSON without calling capture", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");
    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: JSON.stringify({
      schemaVersion: 2, baseCurrency: "TWD", snapshotDate: "2026-08-02",
      assets: [{ id: assetId, name: "USD cash", type: "CASH", liquidity: "LIQUID", amount: "1200.00", currency: "USD", effectiveDate: "2026-08-02", source: "New statement" }],
      liabilities: [{ name: "Mortgage", amount: "400.00", currency: "TWD", effectiveDate: "2026-08-02", source: "Lender" }],
    }) } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));
    expect(screen.getByText("Asset Cash — Amount: 1000.00 TWD → 1200.00 USD")).toBeTruthy();
    expect(screen.getByText("Add liability: Mortgage")).toBeTruthy();
    expect(api.captureSnapshot).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Apply import" }));
    fireEvent.click(screen.getByRole("button", { name: "Assets" }));
    expect((screen.getByLabelText("USD cash currency") as HTMLInputElement).value).toBe("USD");
  });

  it("reconciles a uniquely named AI item to its existing ID", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");
    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: JSON.stringify({ schemaVersion: 2, baseCurrency: "TWD", snapshotDate: "2026-08-02", assets: [{ name: "Cash", type: "CASH", liquidity: "LIQUID", amount: "1200", currency: "USD", effectiveDate: "2026-08-02", source: "New statement" }], liabilities: [] }) } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));
    expect(screen.getByText("Asset Cash — Amount: 1000.00 TWD → 1200 USD")).toBeTruthy();
    expect(screen.queryByText("Add asset: Cash")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Apply import" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));
    await waitFor(() => expect(api.captureSnapshot).toHaveBeenCalledOnce());
    expect(api.captureSnapshot.mock.calls[0][0].body.assets[0].id).toBe(assetId);
  });

  it("rejects mismatched context and injection-shaped fields", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");
    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{"schemaVersion":2,"baseCurrency":"TWD","snapshotDate":"2026-08-02","assets":[{"id":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","name":"bad","type":"CASH","liquidity":"LIQUID","amount":"1","currency":"USD","effectiveDate":"2026-08-02","source":"agent","__proto__":{}}],"liabilities":[]}` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));
    expect((await screen.findByRole("alert")).textContent).toMatch(/not allowed|unknown/i);
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
  });

  it("rejects unsupported original currency in agent output", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");
    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: JSON.stringify({ schemaVersion: 2, baseCurrency: "TWD", snapshotDate: "2026-08-02", assets: [{ name: "Cash", type: "CASH", liquidity: "LIQUID", amount: "1", currency: "ZZZ", effectiveDate: "2026-08-02", source: "agent" }], liabilities: [] }) } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));
    expect((await screen.findByRole("alert")).textContent).toContain("assets[0].currency must be a supported ISO 4217 currency");
  });

  it("archives an existing position only after confirmation", async () => {
    render(<EntryPage />); await selectManualEntry();
    fireEvent.click(await screen.findByRole("button", { name: "Archive Cash" }));
    expect(api.archiveAsset).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Confirm archive" }));
    await waitFor(() => expect(api.archiveAsset).toHaveBeenCalledWith({ path: { id: assetId } }));
  });

  it("hydrates the original amount from applied conversion", async () => {
    api.getSnapshot.mockResolvedValue({ data: { id: "snapshot-1", asOf: "2026-07-31T00:00:00Z", baseCurrency: "TWD", assets: [{ id: assetId, name: "Cash", type: "CASH", liquidity: "LIQUID", money: { amount: "32000", currency: "TWD" }, appliedConversion: { originalMoney: { amount: "1000", currency: "USD" }, rate: "32" }, effectiveAt: "2026-07-30T00:00:00Z", source: "Bank" }], liabilities: [] } });
    render(<EntryPage />); await selectManualEntry();
    expect((screen.getByLabelText("Cash amount") as HTMLInputElement).value).toBe("1000");
    expect((screen.getByLabelText("Cash currency") as HTMLInputElement).value).toBe("USD");
  });

  it("maps API validation back to the affected field", async () => {
    api.captureSnapshot.mockResolvedValue({ error: { errors: [{ field: "assets[0].originalMoney.amount", message: "is invalid" }] } });
    render(<EntryPage />); await selectManualEntry();
    fireEvent.click(screen.getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));
    expect(await screen.findByText("Asset 1 amount is invalid.")).toBeTruthy();
    await waitFor(() => expect(document.activeElement).toBe(screen.getByLabelText("Cash amount")));
  });

  it("captures original-currency money with fixed TWD valuation context", async () => {
    api.listAssets.mockResolvedValue({ data: [] }); api.listLiabilities.mockResolvedValue({ data: [] }); api.listSnapshots.mockResolvedValue({ data: [] });
    render(<EntryPage />); await selectManualEntry();
    fireEvent.click(screen.getByRole("button", { name: "Add asset" }));
    const asset = screen.getByRole("group", { name: "New asset" });
    fireEvent.change(within(asset).getByLabelText("Name"), { target: { value: "USD cash" } });
    fireEvent.change(within(asset).getByLabelText("Type"), { target: { value: "CASH" } });
    fireEvent.change(within(asset).getByLabelText("Liquidity"), { target: { value: "LIQUID" } });
    fireEvent.change(within(asset).getByLabelText("Amount"), { target: { value: "100.00" } });
    fireEvent.change(within(asset).getByLabelText("Currency"), { target: { value: "USD" } });
    fireEvent.change(within(asset).getByLabelText("Source"), { target: { value: "Bank statement" } });
    fireEvent.click(screen.getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));
    await waitFor(() => expect(api.captureSnapshot).toHaveBeenCalledOnce());
    const body = api.captureSnapshot.mock.calls[0][0].body;
    expect(body).toMatchObject({ baseCurrency: "TWD", assets: [{ originalMoney: { amount: "100.00", currency: "USD" } }], liabilities: [] });
    expect(body.assets[0]).not.toHaveProperty("money");
    expect(body.assets[0]).not.toHaveProperty("manualConversion");
    expect(navigation.push).toHaveBeenCalledWith("/?snapshot=snapshot-created");
  });

  it("shows the official FX rate that will be used before save", async () => {
    api.getSnapshot.mockResolvedValue({ data: { id: "snapshot-1", asOf: "2026-07-31T00:00:00Z", baseCurrency: "TWD", assets: [{ id: assetId, name: "USD cash", type: "CASH", liquidity: "LIQUID", money: { amount: "3210", currency: "TWD" }, appliedConversion: { originalMoney: { amount: "100", currency: "USD" }, rate: "32.1", rateDate: "2026-07-31", provider: "CBC" }, effectiveAt: "2026-07-30T00:00:00Z", source: "Bank" }], liabilities: [] } });
    render(<EntryPage />); await selectManualEntry();
    fireEvent.click(screen.getByRole("button", { name: "Review" }));
    expect(await screen.findByText("USD → TWD: 32.1 (CBC, Aug 1, 2026)")).toBeTruthy();
    expect(api.getFxRates).toHaveBeenCalledWith({ query: { asOf: "2026-08-02", currencies: ["USD"] } });
  });

  it("lets the user explicitly override an official rate with declared provenance", async () => {
    api.getSnapshot.mockResolvedValue({ data: { id: "snapshot-1", asOf: "2026-07-31T00:00:00Z", baseCurrency: "TWD", assets: [{ id: assetId, name: "USD cash", type: "CASH", liquidity: "LIQUID", money: { amount: "3200", currency: "TWD" }, appliedConversion: { originalMoney: { amount: "100", currency: "USD" }, rate: "32", rateDate: "2026-07-31", provider: "CBC" }, effectiveAt: "2026-07-30T00:00:00Z", source: "Bank" }], liabilities: [] } });
    render(<EntryPage />); await selectManualEntry();
    fireEvent.click(screen.getByLabelText("Cash use declared rate"));
    fireEvent.change(screen.getByLabelText("Cash declared rate"), { target: { value: "32.5" } });
    fireEvent.change(screen.getByLabelText("Cash rate date"), { target: { value: "2026-08-01" } });
    fireEvent.change(screen.getByLabelText("Cash rate basis"), { target: { value: "Bank closing rate" } });
    fireEvent.click(screen.getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));
    await waitFor(() => expect(api.captureSnapshot).toHaveBeenCalledOnce());
    expect(api.captureSnapshot.mock.calls[0][0].body.assets[0].declaredRate).toEqual({ rate: "32.5", rateDate: "2026-08-01", basis: "Bank closing rate" });
  });

  it("offers ISO currency autocomplete while preserving manual entry", async () => {
    api.listAssets.mockResolvedValue({ data: [] }); api.listSnapshots.mockResolvedValue({ data: [] });
    render(<EntryPage />); await selectManualEntry();
    fireEvent.click(screen.getByRole("button", { name: "Add asset" }));
    const currency = screen.getByRole("combobox", { name: "Currency" }) as HTMLInputElement;
    fireEvent.change(currency, { target: { value: "us dollar" } });
    const suggestions = screen.getByRole("listbox", { name: "Currency suggestions" });
    fireEvent.mouseDown(within(suggestions).getByRole("option", { name: "USD — US Dollar" }));
    expect(currency.value).toBe("USD");
  });
});
