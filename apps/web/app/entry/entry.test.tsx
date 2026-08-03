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

describe("Balance-sheet entry", () => {
  it("shows Input as the current app-level destination", async () => {
    render(<EntryPage />);

    await screen.findByRole("heading", { name: "Update balance sheet" });
    expect(screen.getByRole("link", { name: "Dashboard" }).getAttribute("href")).toBe("/");
    expect(screen.getByRole("link", { name: "Input" }).getAttribute("aria-current")).toBe("page");
  });

  it("moves freely through entry steps without losing the draft", async () => {
    render(<EntryPage />);

    const steps = await screen.findByRole("navigation", { name: "Entry steps" });
    expect(within(steps).getByRole("button", { name: "Settings" }).getAttribute("aria-current")).toBe("step");
    expect(screen.getByLabelText("Base currency")).toBeTruthy();
    expect(screen.queryByLabelText("Cash amount")).toBeNull();

    fireEvent.click(within(steps).getByRole("button", { name: "Assets" }));
    fireEvent.change(screen.getByLabelText("Cash name"), { target: { value: "Emergency reserve" } });
    fireEvent.click(within(steps).getByRole("button", { name: "Liabilities" }));
    expect(screen.getByRole("button", { name: "Add liability" })).toBeTruthy();
    expect(screen.queryByLabelText("Emergency reserve amount")).toBeNull();

    fireEvent.click(within(steps).getByRole("button", { name: "Assets" }));
    expect(screen.getByLabelText("Emergency reserve amount")).toBeTruthy();
    fireEvent.click(within(steps).getByRole("button", { name: "Review" }));
    expect(screen.getByRole("heading", { name: "Review and save" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Save Snapshot" })).toBeTruthy();
  });

  it("previews and merges fenced agent JSON without calling the API", async () => {
    render(<EntryPage />);

    const prompt = await screen.findByLabelText("AI Prompt") as HTMLTextAreaElement;
    expect(prompt.value).not.toContain(assetId);
    expect(prompt.value).not.toContain("Bank statement");
    expect(prompt.value).not.toContain('"baseCurrency": "USD"');
    expect(prompt.value).toContain("Ask one concise question at a time and wait for the user's answer");
    expect(prompt.value).toContain("Cash and bank accounts");
    expect(prompt.value).toContain("Investments and retirement accounts");
    expect(prompt.value).toContain("Real estate");
    expect(prompt.value).toContain("Vehicles");
    expect(prompt.value).toContain("Business ownership");
    expect(prompt.value).toContain("Other assets");
    expect(prompt.value).toContain("Mortgages, credit cards, personal or business loans, taxes owed, and other liabilities");
    expect(prompt.value).toContain("Do not return the final JSON until the user confirms the inventory is complete");
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
    "name": "Mortgage", "amount": "400.00", "effectiveDate": "2026-08-02", "source": "Lender statement"
  }]
}
\`\`\`` } });
    fireEvent.click(screen.getByRole("button", { name: "Preview import" }));

    expect(screen.getByRole("dialog", { name: "Review AI import" })).toBeTruthy();
    expect(screen.getByText("Update asset: Cash → Updated cash")).toBeTruthy();
    expect(screen.getByText("Add liability: Mortgage")).toBeTruthy();
    expect(api.captureSnapshot).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Apply import" }));

    fireEvent.click(screen.getByRole("button", { name: "Assets" }));
    expect((screen.getByLabelText("Updated cash amount") as HTMLInputElement).value).toBe("1200.00");
    fireEvent.click(screen.getByRole("button", { name: "Liabilities" }));
    expect(screen.getByRole("group", { name: "New liability" })).toBeTruthy();
    expect(api.captureSnapshot).not.toHaveBeenCalled();
  });

  it("accepts one unlabeled fenced JSON block", async () => {
    render(<EntryPage />);
    await screen.findByLabelText("Agent JSON");

    fireEvent.change(screen.getByLabelText("Agent JSON"), { target: { value: `\`\`\`
{
  "schemaVersion": 1,
  "baseCurrency": "USD",
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
    fireEvent.click(screen.getByRole("button", { name: "Assets" }));
    expect((screen.getByLabelText("Cash amount") as HTMLInputElement).value).toBe("1000.00");
    expect(screen.queryByRole("dialog", { name: "Review AI import" })).toBeNull();
  });

  it("archives an existing position only after explicit confirmation", async () => {
    render(<EntryPage />);

    const steps = await screen.findByRole("navigation", { name: "Entry steps" });
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

    const steps = await screen.findByRole("navigation", { name: "Entry steps" });
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

  it("prefills the latest facts and captures all active positions with a new asset", async () => {
    render(<EntryPage />);

    expect(await screen.findByRole("heading", { name: "Update balance sheet" })).toBeTruthy();
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
