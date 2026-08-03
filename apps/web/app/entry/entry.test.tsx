import React from "react";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import EntryPage from "./page";

const assetId = "0f27e4fa-99f8-4c5e-87da-527488cbe515";

const api = vi.hoisted(() => ({
  archiveAsset: vi.fn(),
  archiveLiability: vi.fn(),
  captureSnapshot: vi.fn(),
  getSnapshot: vi.fn(),
  listAssets: vi.fn(),
  listLiabilities: vi.fn(),
  listSnapshots: vi.fn(),
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
    id: "snapshot-1",
    asOf: "2026-07-31T00:00:00Z",
    recordedAt: "2026-07-31T08:00:00Z",
    assets: [{ id: assetId, name: "Cash", type: "CASH", liquidity: "LIQUID", money: { amount: "1000.00", currency: "USD" }, effectiveAt: "2026-07-30T00:00:00Z", source: "Bank statement" }],
    liabilities: [],
  } });
  api.captureSnapshot.mockResolvedValue({ data: { id: "snapshot-created" } });
  api.archiveAsset.mockResolvedValue({});
  api.archiveLiability.mockResolvedValue({});
});

afterEach(() => {
  vi.useRealTimers();
  cleanup();
});

async function selectManualEntry() {
  fireEvent.click(await screen.findByRole("tab", { name: "Manual entry" }));
}

describe("Balance-sheet entry", () => {
  it("shows Input as the current app-level destination", async () => {
    render(<EntryPage />);

    await screen.findByRole("heading", { name: "Update balance sheet" });
    expect(screen.getByRole("link", { name: "Dashboard" }).getAttribute("href")).toBe("/");
    expect(screen.getByRole("link", { name: "Input" }).getAttribute("aria-current")).toBe("page");
  });

  it("opens AI-assisted import as the default Input mode", async () => {
    render(<EntryPage />);

    const aiTab = await screen.findByRole("tab", { name: "AI-assisted import" });
    expect(aiTab.getAttribute("aria-selected")).toBe("true");
    expect(screen.getByRole("tab", { name: "Manual entry" }).getAttribute("aria-selected")).toBe("false");
    expect(screen.getByRole("heading", { name: "AI-assisted import" })).toBeTruthy();
    expect(screen.queryByRole("navigation", { name: "Entry steps" })).toBeNull();
  });

  it("shares Snapshot context above both modes and starts Manual entry at Assets", async () => {
    render(<EntryPage />);

    await screen.findByRole("tab", { name: "AI-assisted import" });
    expect((screen.getByLabelText("Snapshot date") as HTMLInputElement).value).toBe("2026-08-02");
    expect((screen.getByLabelText("Base currency") as HTMLInputElement).value).toBe("USD");
    await selectManualEntry();

    const steps = screen.getByRole("navigation", { name: "Entry steps" });
    expect(within(steps).queryByRole("button", { name: "Settings" })).toBeNull();
    expect(within(steps).getByRole("button", { name: "Assets" }).getAttribute("aria-current")).toBe("step");
    expect(screen.getByText("Step 1 of 3")).toBeTruthy();
    expect(screen.getByLabelText("Base currency")).toBeTruthy();
  });

  it("offers prioritized ISO base currencies on first use", async () => {
    api.listSnapshots.mockResolvedValue({ data: [] });
    render(<EntryPage />);

    const currency = await screen.findByRole("combobox", { name: "Base currency" }) as HTMLSelectElement;
    const options = within(currency).getAllByRole("option").map((option) => option.textContent);
    expect(options.slice(0, 11)).toEqual(["Select currency", "TWD", "USD", "EUR", "JPY", "CNY", "HKD", "GBP", "AUD", "CAD", "SGD"]);
    expect(options).toContain("CHF");
    expect(options).toContain("ZAR");

    fireEvent.change(currency, { target: { value: "CHF" } });
    expect(currency.value).toBe("CHF");
    expect((screen.getByLabelText("AI Prompt") as HTMLTextAreaElement).value).toContain("Base currency CHF");
  });

  it("switches Input modes with standard tab keyboard controls", async () => {
    render(<EntryPage />);

    const aiTab = await screen.findByRole("tab", { name: "AI-assisted import" });
    const manualTab = screen.getByRole("tab", { name: "Manual entry" });
    aiTab.focus();
    fireEvent.keyDown(aiTab, { key: "ArrowLeft" });

    expect(manualTab.getAttribute("aria-selected")).toBe("true");
    expect(document.activeElement).toBe(manualTab);
    fireEvent.keyDown(manualTab, { key: "ArrowRight" });
    expect(aiTab.getAttribute("aria-selected")).toBe("true");
    expect(document.activeElement).toBe(aiTab);
  });

  it("updates the AI Prompt from the shared Snapshot context", async () => {
    api.listSnapshots.mockResolvedValue({ data: [] });
    render(<EntryPage />);

    const prompt = await screen.findByLabelText("AI Prompt") as HTMLTextAreaElement;
    expect(prompt.value).toContain("Set a valid Base currency in Wealth OS before using this Prompt.");
    fireEvent.change(screen.getByLabelText("Snapshot date"), { target: { value: "2026-08-01" } });
    fireEvent.change(screen.getByLabelText("Base currency"), { target: { value: "ZZZ" } });
    expect(prompt.value).toContain("Set a valid Base currency in Wealth OS before using this Prompt.");
    fireEvent.change(screen.getByLabelText("Base currency"), { target: { value: "TWD" } });

    expect(prompt.value).toContain("Use this exact Snapshot context: Base currency TWD; Snapshot date 2026-08-01.");
    expect(prompt.value).toContain('"baseCurrency": "TWD"');
    expect(prompt.value).toContain('"snapshotDate": "2026-08-01"');
    expect(prompt.value).not.toContain("First confirm the base currency and Snapshot date");
  });

  it("moves freely through entry steps without losing the draft", async () => {
    render(<EntryPage />);
    await selectManualEntry();

    const steps = screen.getByRole("navigation", { name: "Entry steps" });
    expect(within(steps).getByRole("button", { name: "Assets" }).getAttribute("aria-current")).toBe("step");
    expect(screen.getByLabelText("Base currency")).toBeTruthy();
    expect(screen.getByLabelText("Cash amount")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Save Snapshot" })).toBeNull();

    fireEvent.change(screen.getByLabelText("Cash name"), { target: { value: "Emergency reserve" } });
    fireEvent.click(screen.getByRole("tab", { name: "AI-assisted import" }));
    expect(screen.getByLabelText("Agent JSON")).toBeTruthy();
    fireEvent.click(screen.getByRole("tab", { name: "Manual entry" }));
    const restoredSteps = screen.getByRole("navigation", { name: "Entry steps" });
    fireEvent.click(within(restoredSteps).getByRole("button", { name: "Liabilities" }));
    expect(screen.getByRole("button", { name: "Add liability" })).toBeTruthy();
    expect(screen.queryByLabelText("Emergency reserve amount")).toBeNull();

    fireEvent.click(within(restoredSteps).getByRole("button", { name: "Assets" }));
    expect(screen.getByLabelText("Emergency reserve amount")).toBeTruthy();
    fireEvent.click(within(restoredSteps).getByRole("button", { name: "Review" }));
    expect(screen.getByRole("heading", { name: "Review and save" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Save Snapshot" })).toBeTruthy();
  });

  it("previews and merges fenced agent JSON without calling the API", async () => {
    render(<EntryPage />);

    const prompt = await screen.findByLabelText("AI Prompt") as HTMLTextAreaElement;
    expect(prompt.value).not.toContain(assetId);
    expect(prompt.value).not.toContain("Bank statement");
    expect(prompt.value).toContain('"baseCurrency": "USD"');
    expect(prompt.value).toContain('"originalCurrency": "EUR"');
    expect(prompt.value).not.toContain('"originalCurrency": "USD"');
    expect(prompt.value).toContain("Use this exact Snapshot context: Base currency USD; Snapshot date 2026-08-02.");
    expect(prompt.value).toContain("Ask one concise question at a time and wait for the user's answer");
    expect(prompt.value).toContain("Cash and bank accounts");
    expect(prompt.value).toContain("Investments and retirement accounts");
    expect(prompt.value).toContain("Real estate");
    expect(prompt.value).toContain("Vehicles");
    expect(prompt.value).toContain("Business ownership");
    expect(prompt.value).toContain("Other assets");
    expect(prompt.value).toContain("Mortgages, credit cards, personal or business loans, taxes owed, and other liabilities");
    expect(prompt.value).toContain("Do not return the final JSON until the user confirms the inventory is complete");
    expect(prompt.value).toContain("For each foreign-currency position, manually convert it to the base currency");
    expect(prompt.value).toContain('"manualConversion"');
    fireEvent.click(screen.getByLabelText("Include current draft in Prompt"));
    expect(prompt.value).toContain(assetId);
    expect(screen.getByText("This Prompt contains sensitive financial data.")).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `\`\`\`json
{
  "schemaVersion": 1,
  "baseCurrency": "USD",
  "snapshotDate": "2026-08-02",
  "assets": [{
    "id": "${assetId}", "name": "Updated cash", "type": "CASH", "liquidity": "LIQUID",
    "amount": "1200.00", "effectiveDate": "2026-08-02", "source": "New bank statement"
  }],
  "liabilities": [{
    "name": "Mortgage", "amount": "400.00", "effectiveDate": "2026-08-02", "source": "Lender statement",
    "manualConversion": {
      "originalAmount": "320.00", "originalCurrency": "EUR",
      "exchangeRateBasis": "ECB EUR/USD reference rate 1.25", "effectiveDate": "2026-08-01"
    }
  }]
}
\`\`\`` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect(screen.getByRole("dialog", { name: "Review AI import" })).toBeTruthy();
    expect(screen.getByText("Asset Cash — Name: Cash → Updated cash")).toBeTruthy();
    expect(screen.getByText("Add liability: Mortgage")).toBeTruthy();
    expect(api.captureSnapshot).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Apply import" }));

    expect(screen.getByRole("tab", { name: "Manual entry" }).getAttribute("aria-selected")).toBe("true");
    expect(screen.getByRole("heading", { name: "Review and save" })).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Assets" }));
    expect((screen.getByLabelText("Updated cash amount") as HTMLInputElement).value).toBe("1200.00");
    fireEvent.click(screen.getByRole("button", { name: "Liabilities" }));
    expect(screen.getByRole("group", { name: "New liability" })).toBeTruthy();
    expect((screen.getByLabelText("New liability original amount") as HTMLInputElement).value).toBe("320.00");
    expect((screen.getByLabelText("New liability original currency") as HTMLInputElement).value).toBe("EUR");
    expect(api.captureSnapshot).not.toHaveBeenCalled();
  });

  it("previews every changed financial field before applying an agent update", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");
    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "baseCurrency": "USD",
      "snapshotDate": "2026-08-02",
      "assets": [{
        "id": "${assetId}",
        "name": "Cash",
        "type": "INVESTMENT",
        "liquidity": "SEMI_LIQUID",
        "amount": "1200.00",
        "effectiveDate": "2026-08-01",
        "source": "New statement",
        "manualConversion": {
          "originalAmount": "1000.00",
          "originalCurrency": "EUR",
          "exchangeRateBasis": "ECB EUR/USD reference rate 1.20",
          "effectiveDate": "2026-08-01"
        }
      }],
      "liabilities": []
    }` } });

    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect(screen.getByText("Asset Cash — Type: CASH → INVESTMENT")).toBeTruthy();
    expect(screen.getByText("Asset Cash — Liquidity: LIQUID → SEMI_LIQUID")).toBeTruthy();
    expect(screen.getByText("Asset Cash — Amount: 1000.00 → 1200.00")).toBeTruthy();
    expect(screen.getByText("Asset Cash — Effective date: 2026-07-30 → 2026-08-01")).toBeTruthy();
    expect(screen.getByText("Asset Cash — Source: Bank statement → New statement")).toBeTruthy();
    expect(screen.getByText("Asset Cash — Manual conversion: None → 1000.00 EUR; ECB EUR/USD reference rate 1.20; effective 2026-08-01")).toBeTruthy();
    expect(api.captureSnapshot).not.toHaveBeenCalled();
  });

  it("rejects agent Snapshot context that differs from the shared settings", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "baseCurrency": "USD",
      "snapshotDate": "2026-08-01",
      "assets": [],
      "liabilities": []
    }` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect((await screen.findByRole("alert")).textContent).toContain("snapshotDate must match the shared Snapshot date 2026-08-02");
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
    expect((screen.getByLabelText("Snapshot date") as HTMLInputElement).value).toBe("2026-08-02");
    expect((screen.getByLabelText("Base currency") as HTMLInputElement).value).toBe("USD");
  });

  it("rejects agent base currency that differs from the shared settings", async () => {
    render(<EntryPage />);
    await selectManualEntry();
    const baseCurrency = screen.getByLabelText("Base currency") as HTMLSelectElement;
    expect(baseCurrency.disabled).toBe(true);
    fireEvent.click(screen.getByRole("tab", { name: "AI-assisted import" }));

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "baseCurrency": "EUR",
      "snapshotDate": "2026-08-02",
      "assets": [],
      "liabilities": []
    }` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect((await screen.findByRole("alert")).textContent).toContain("baseCurrency must match the shared Base currency USD");
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
    expect(baseCurrency.value).toBe("USD");
  });

  it("keeps keyboard focus inside dialogs and restores it when dismissed", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");
    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "baseCurrency": "USD",
      "snapshotDate": "2026-08-02",
      "assets": [],
      "liabilities": []
    }` } });
    const preview = screen.getByRole("button", { name: "Preview import" });
    preview.focus();
    fireEvent.click(preview);

    const dialog = screen.getByRole("dialog", { name: "Review AI import" });
    const cancel = within(dialog).getByRole("button", { name: "Cancel" });
    const apply = within(dialog).getByRole("button", { name: "Apply import" });
    await waitFor(() => expect(document.activeElement).toBe(cancel));
    apply.focus();
    fireEvent.keyDown(dialog, { key: "Tab" });
    expect(document.activeElement).toBe(cancel);
    fireEvent.keyDown(dialog, { key: "Escape" });

    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
    expect(document.activeElement).toBe(preview);
  });

  it("accepts one unlabeled fenced JSON block", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `\`\`\`
{
  "schemaVersion": 1,
  "baseCurrency": "USD",
  "snapshotDate": "2026-08-02",
  "assets": [],
  "liabilities": []
}
\`\`\`` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect(screen.getByRole("dialog", { name: "Review AI import" })).toBeTruthy();
  });

  it("requires a validated currency in agent output", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "assets": [],
      "liabilities": []
    }` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect((await screen.findByRole("alert")).textContent).toContain("baseCurrency is required");
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
  });

  it("rejects an unsupported ISO base currency in agent output", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "baseCurrency": "ZZZ",
      "snapshotDate": "2026-08-02",
      "assets": [],
      "liabilities": []
    }` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect((await screen.findByRole("alert")).textContent).toContain("baseCurrency must be a supported ISO 4217 currency");
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
  });

  it("rejects an unsupported ISO original currency in agent output", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "baseCurrency": "USD",
      "snapshotDate": "2026-08-02",
      "assets": [{
        "name": "Foreign cash",
        "type": "CASH",
        "liquidity": "LIQUID",
        "amount": "100.00",
        "effectiveDate": "2026-08-02",
        "source": "Statement",
        "manualConversion": {
          "originalAmount": "100.00",
          "originalCurrency": "ZZZ",
          "exchangeRateBasis": "Declared rate",
          "effectiveDate": "2026-08-02"
        }
      }],
      "liabilities": []
    }` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect((await screen.findByRole("alert")).textContent).toContain("assets[0].manualConversion.originalCurrency must be a supported ISO 4217 currency");
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
  });

  it("rejects unknown IDs and injection-shaped fields without changing the draft", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");
    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `{
      "schemaVersion": 1,
      "baseCurrency": "USD",
      "assets": [{
        "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
        "name": "<img src=x onerror=alert(1)>",
        "type": "CASH",
        "liquidity": "LIQUID",
        "amount": "999.00",
        "effectiveDate": "2026-08-02",
        "source": "agent",
        "__proto__": { "polluted": true }
      }],
      "liabilities": []
    }` } });

    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect((await screen.findByRole("alert")).textContent).toMatch(/not allowed|unknown/i);
    await selectManualEntry();
    fireEvent.click(screen.getByRole("button", { name: "Assets" }));
    expect((screen.getByLabelText("Cash amount") as HTMLInputElement).value).toBe("1000.00");
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
  });

  it("archives an existing position only after explicit confirmation", async () => {
    render(<EntryPage />);
    await selectManualEntry();

    const steps = screen.getByRole("navigation", { name: "Entry steps" });
    fireEvent.click(within(steps).getByRole("button", { name: "Assets" }));
    fireEvent.click(await screen.findByRole("button", { name: "Archive Cash" }));
    expect(api.archiveAsset).not.toHaveBeenCalled();
    expect(screen.getByRole("dialog", { name: "Archive Cash?" })).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Confirm archive" }));

    await waitFor(() => expect(api.archiveAsset).toHaveBeenCalledWith({ path: { id: assetId } }));
    expect(screen.queryByRole("group", { name: "Cash" })).toBeNull();
  });

  it("identifies and focuses the first invalid field without clearing the draft", async () => {
    api.listSnapshots.mockResolvedValue({ data: [] });

    render(<EntryPage />);
    await selectManualEntry();

    const steps = screen.getByRole("navigation", { name: "Entry steps" });
    fireEvent.click(within(steps).getByRole("button", { name: "Assets" }));
    const name = screen.getByLabelText("Cash name") as HTMLInputElement;
    fireEvent.change(name, { target: { value: "Updated cash" } });
    fireEvent.click(within(steps).getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));

    expect(await screen.findByText("Base currency is required.")).toBeTruthy();
    expect(document.activeElement).toBe(screen.getByLabelText("Base currency"));
    fireEvent.click(within(steps).getByRole("button", { name: "Assets" }));
    expect((screen.getByLabelText("Updated cash name") as HTMLInputElement).value).toBe("Updated cash");
    expect(api.captureSnapshot).not.toHaveBeenCalled();
  });

  it("maps an API validation error back to its step and first affected field", async () => {
    api.captureSnapshot.mockResolvedValue({ error: { errors: [{ field: "assets[0].money.amount", message: "must use the currency's supported precision" }] } });
    render(<EntryPage />);
    await selectManualEntry();

    const steps = screen.getByRole("navigation", { name: "Entry steps" });
    fireEvent.click(within(steps).getByRole("button", { name: "Assets" }));
    fireEvent.change(screen.getByLabelText("Cash name"), { target: { value: "Emergency cash" } });
    fireEvent.click(within(steps).getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));

    expect(await screen.findByText("Asset 1 amount must use the currency's supported precision.")).toBeTruthy();
    expect(within(steps).getByRole("button", { name: "Assets" }).getAttribute("aria-current")).toBe("step");
    await waitFor(() => expect(document.activeElement).toBe(screen.getByLabelText("Emergency cash amount")));
    expect((screen.getByLabelText("Emergency cash name") as HTMLInputElement).value).toBe("Emergency cash");
  });

  it("identifies and focuses a collection-level API validation error", async () => {
    api.captureSnapshot.mockResolvedValue({ error: { errors: [{ field: "assets", message: "must include every active asset exactly once" }] } });
    render(<EntryPage />);
    await selectManualEntry();

    const steps = screen.getByRole("navigation", { name: "Entry steps" });
    fireEvent.click(within(steps).getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));

    expect(await screen.findByText("Assets must include every active asset exactly once.")).toBeTruthy();
    await waitFor(() => expect(document.activeElement).toBe(screen.getByRole("button", { name: "Add asset" })));
  });

  it("captures structured manual-conversion provenance for a foreign-currency asset", async () => {
    render(<EntryPage />);
    await selectManualEntry();

    fireEvent.click(screen.getByRole("button", { name: "Assets" }));
    fireEvent.click(screen.getByLabelText("Cash converted from another currency"));
    fireEvent.change(screen.getByLabelText("Cash original amount"), { target: { value: "800.00" } });
    fireEvent.change(screen.getByLabelText("Cash original currency"), { target: { value: "EUR" } });
    fireEvent.change(screen.getByLabelText("Cash exchange-rate basis"), { target: { value: "ECB EUR/USD reference rate 1.25" } });
    fireEvent.change(screen.getByLabelText("Cash conversion effective date"), { target: { value: "2026-07-30" } });
    fireEvent.click(screen.getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));

    await waitFor(() => expect(api.captureSnapshot).toHaveBeenCalledOnce());
    expect(api.captureSnapshot.mock.calls[0][0].body.assets[0].manualConversion).toEqual({
      originalMoney: { amount: "800.00", currency: "EUR" },
      exchangeRateBasis: "ECB EUR/USD reference rate 1.25",
      effectiveAt: "2026-07-30T00:00:00.000Z",
    });
  });

  it("carries forward saved manual-conversion provenance", async () => {
    api.getSnapshot.mockResolvedValue({ data: {
      id: "snapshot-1",
      asOf: "2026-07-31T00:00:00Z",
      recordedAt: "2026-07-31T08:00:00Z",
      assets: [{
        id: assetId, name: "Cash", type: "CASH", liquidity: "LIQUID",
        money: { amount: "1250.00", currency: "USD" }, effectiveAt: "2026-07-30T00:00:00Z", source: "Bank statement",
        manualConversion: {
          originalMoney: { amount: "1000.00", currency: "EUR" },
          exchangeRateBasis: "ECB EUR/USD reference rate 1.25",
          effectiveAt: "2026-07-30T00:00:00Z",
        },
      }],
      liabilities: [],
    } });
    render(<EntryPage />);
    await selectManualEntry();

    fireEvent.click(screen.getByRole("button", { name: "Assets" }));

    expect((screen.getByLabelText("Cash converted from another currency") as HTMLInputElement).checked).toBe(true);
    expect((screen.getByLabelText("Cash original amount") as HTMLInputElement).value).toBe("1000.00");
    expect((screen.getByLabelText("Cash original currency") as HTMLInputElement).value).toBe("EUR");
  });

  it("prefills the latest facts and captures all active positions with a new asset", async () => {
    render(<EntryPage />);
    await selectManualEntry();

    expect((screen.getByLabelText("Base currency") as HTMLInputElement).value).toBe("USD");
    fireEvent.click(screen.getByRole("button", { name: "Assets" }));
    expect((screen.getByLabelText("Cash amount") as HTMLInputElement).value).toBe("1000.00");
    expect(screen.getByText("Carried forward from Jul 31, 2026")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Add asset" }));
    const newAsset = screen.getByRole("group", { name: "New asset" });
    fireEvent.change(within(newAsset).getByLabelText("Name"), { target: { value: "Brokerage" } });
    fireEvent.change(within(newAsset).getByLabelText("Type"), { target: { value: "INVESTMENT" } });
    fireEvent.change(within(newAsset).getByLabelText("Liquidity"), { target: { value: "SEMI_LIQUID" } });
    fireEvent.change(within(newAsset).getByLabelText("Amount"), { target: { value: "2500.00" } });
    fireEvent.change(within(newAsset).getByLabelText("Source"), { target: { value: "Broker statement" } });

    fireEvent.click(screen.getByRole("button", { name: "Review" }));
    fireEvent.click(screen.getByRole("button", { name: "Save Snapshot" }));

    await waitFor(() => expect(api.captureSnapshot).toHaveBeenCalledOnce());
    expect(api.captureSnapshot).toHaveBeenCalledWith({ body: {
      asOf: "2026-08-02T00:00:00.000Z",
      recordedAt: expect.stringMatching(/^2026-08-02T08:00:00\.\d{3}Z$/),
      baseCurrency: "USD",
      assets: [
        { id: assetId, name: "Cash", type: "CASH", liquidity: "LIQUID", money: { amount: "1000.00", currency: "USD" }, effectiveAt: "2026-07-30T00:00:00.000Z", source: "Bank statement" },
        { name: "Brokerage", type: "INVESTMENT", liquidity: "SEMI_LIQUID", money: { amount: "2500.00", currency: "USD" }, effectiveAt: "2026-08-02T00:00:00.000Z", source: "Broker statement" },
      ],
      liabilities: [],
    } });
    expect(navigation.push).toHaveBeenCalledWith("/?snapshot=snapshot-created");
  });
});
