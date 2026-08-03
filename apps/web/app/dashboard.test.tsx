import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Dashboard from "./page";

const api = vi.hoisted(() => ({
  getFinancialHealth: vi.fn(),
  getFxRates: vi.fn(),
  getSnapshot: vi.fn(),
  listSnapshots: vi.fn(),
}));

vi.mock("./api/client", () => api);

beforeEach(() => {
  vi.clearAllMocks();
  window.localStorage.clear();
  window.history.replaceState({}, "", "/");
});

afterEach(cleanup);

describe("Dashboard", () => {
  it("shows Dashboard and Input as app-level navigation", async () => {
    api.listSnapshots.mockResolvedValue({ data: [] });

    render(<Dashboard />);

    const navigation = screen.getByRole("navigation", { name: "Primary" });
    expect(screen.getByRole("link", { name: "Dashboard" }).getAttribute("aria-current")).toBe("page");
    expect(screen.getByRole("link", { name: "Input" }).getAttribute("href")).toBe("/entry");
    expect(navigation).toBeTruthy();
  });

  it("opens the snapshot requested by the entry workflow", async () => {
    window.history.replaceState({}, "", "/?snapshot=older");
    api.listSnapshots.mockResolvedValue({ data: [{ id: "older", asOf: "2026-07-01T00:00:00Z" }, { id: "latest", asOf: "2026-08-01T00:00:00Z" }] });
    api.getFinancialHealth.mockResolvedValue({ data: { status: "CALCULATED", netWorth: { amount: "150.00", currency: "USD" } } });
    api.getSnapshot.mockResolvedValue({ data: { assets: [], liabilities: [] } });

    render(<Dashboard />);

    await waitFor(() => expect(api.getSnapshot).toHaveBeenCalledWith({ path: { id: "older" } }));
    expect((screen.getByRole("combobox", { name: "Snapshot" }) as HTMLSelectElement).value).toBe("older");
  });

  it("shows the latest snapshot financial health and its position details", async () => {
    api.listSnapshots.mockResolvedValue({ data: [{ id: "older", asOf: "2026-07-01T00:00:00Z" }, { id: "latest", asOf: "2026-08-01T00:00:00Z" }] });
    api.getFinancialHealth.mockResolvedValue({ data: { status: "CALCULATED", totalAssets: { amount: "1000.00", currency: "USD" }, totalLiabilities: { amount: "250.00", currency: "USD" }, netWorth: { amount: "750.00", currency: "USD" }, debtRatio: "0.250000", liquidityRatio: "0.600000" } });
    api.getSnapshot.mockResolvedValue({ data: {
      assets: [{ id: "cash", name: "Cash reserve", type: "CASH", liquidity: "LIQUID", money: { amount: "1000.00", currency: "USD" }, effectiveAt: "2026-08-01T00:00:00Z", source: "Bank statement" }],
      liabilities: [{ id: "loan", name: "Car loan", money: { amount: "250.00", currency: "USD" }, effectiveAt: "2026-08-01T00:00:00Z", source: "Lender statement" }],
    } });

    render(<Dashboard />);

    expect(await screen.findByText("Net worth")).toBeTruthy();
    expect(await screen.findByText("$750.00")).toBeTruthy();
    expect(screen.getByText("25.00%")).toBeTruthy();
    expect(screen.getByText("60.00%")).toBeTruthy();
    expect(await screen.findByRole("heading", { name: "Assets" })).toBeTruthy();
    expect(screen.getByText("Cash reserve")).toBeTruthy();
    expect(screen.getByText("Bank statement")).toBeTruthy();
    expect(screen.getByText("Car loan")).toBeTruthy();
    expect(screen.getByText("Lender statement")).toBeTruthy();
    expect(screen.getByText("Total Assets = Sum of all asset values in canonical TWD")).toBeTruthy();
    expect(screen.getByText("Net Worth = Total Assets - Total Liabilities")).toBeTruthy();
    expect(screen.getByText("Included in immediately liquid assets: assets classified as LIQUID. SEMI_LIQUID and ILLIQUID assets are excluded.")).toBeTruthy();
  });

  it("uses the snapshot historical rate to display every monetary value without changing ratios", async () => {
    window.localStorage.setItem("wealthos.displayCurrency", "JPY");
    window.history.replaceState({}, "", "/?displayCurrency=USD");
    api.listSnapshots.mockResolvedValue({ data: [{ id: "snapshot-1", asOf: "2026-07-31T00:00:00Z" }] });
    api.getFinancialHealth.mockResolvedValue({ data: {
      status: "CALCULATED",
      totalAssets: { amount: "3229", currency: "TWD" },
      totalLiabilities: { amount: "323", currency: "TWD" },
      netWorth: { amount: "2906", currency: "TWD" },
      debtRatio: "0.100031",
      liquidityRatio: "1.000000",
    } });
    api.getSnapshot.mockResolvedValue({ data: {
      id: "snapshot-1",
      asOf: "2026-07-31T00:00:00Z",
      assets: [{
        id: "cash", name: "EUR cash", type: "CASH", liquidity: "LIQUID",
        money: { amount: "3229", currency: "TWD" },
        appliedConversion: { originalMoney: { amount: "85.00", currency: "EUR" } },
        effectiveAt: "2026-07-31T00:00:00Z", source: "Statement",
      }],
      liabilities: [{ id: "loan", name: "Loan", money: { amount: "323", currency: "TWD" }, effectiveAt: "2026-07-31T00:00:00Z", source: "Statement" }],
    } });
    api.getFxRates.mockResolvedValue({ data: {
      valuationCurrency: "TWD",
      asOf: "2026-07-31",
      rates: [{ originalCurrency: "USD", rate: "32.29", rateDate: "2026-07-30", provider: "CBC", rateType: "REFERENCE_RATE" }],
      missingCurrencies: [],
    } });

    render(<Dashboard />);

    expect((await screen.findAllByText("$100.00")).length).toBeGreaterThan(0);
    expect(screen.getAllByText("$10.00").length).toBeGreaterThan(0);
    expect(screen.getByText("$90.00")).toBeTruthy();
    expect(screen.getByText("10.00%")).toBeTruthy();
    expect(screen.getByText("100.00%")).toBeTruthy();
    expect(screen.getByText("NT$3,229 canonical")).toBeTruthy();
    expect(screen.getByText("€85.00 original")).toBeTruthy();
    expect(screen.getByText("Rate date Jul 30, 2026")).toBeTruthy();
    expect(api.getFxRates).toHaveBeenCalledWith({ query: { asOf: "2026-07-31", currencies: ["USD"] } });
    expect((screen.getByRole("combobox", { name: "Display currency" }) as HTMLSelectElement).value).toBe("USD");
    expect(window.localStorage.getItem("wealthos.displayCurrency")).toBe("USD");
  });

  it("does not let an earlier snapshot response overwrite the selected snapshot", async () => {
    const deferred = <T,>() => { let resolve!: (value: T) => void; return { promise: new Promise<T>((done) => { resolve = done; }), resolve }; };
    const latestHealth = deferred<{ data: any }>(); const latestSnapshot = deferred<{ data: any }>();
    const olderHealth = deferred<{ data: any }>(); const olderSnapshot = deferred<{ data: any }>();
    api.listSnapshots.mockResolvedValue({ data: [{ id: "older", asOf: "2026-07-01T00:00:00Z" }, { id: "latest", asOf: "2026-08-01T00:00:00Z" }] });
    api.getFinancialHealth.mockImplementation(({ path }: { path: { snapshotId: string } }) => path.snapshotId === "latest" ? latestHealth.promise : olderHealth.promise);
    api.getSnapshot.mockImplementation(({ path }: { path: { id: string } }) => path.id === "latest" ? latestSnapshot.promise : olderSnapshot.promise);
    render(<Dashboard />);
    const selector = await screen.findByRole("combobox", { name: "Snapshot" });
    await waitFor(() => expect(api.getFinancialHealth).toHaveBeenCalledWith({ path: { snapshotId: "latest" } }));
    fireEvent.change(selector, { target: { value: "older" } });
    await waitFor(() => expect(api.getFinancialHealth).toHaveBeenCalledWith({ path: { snapshotId: "older" } }));
    olderHealth.resolve({ data: { status: "CALCULATED", netWorth: { amount: "150.00", currency: "USD" } } }); olderSnapshot.resolve({ data: { assets: [], liabilities: [] } });
    expect(await screen.findByText("$150.00")).toBeTruthy();
    latestHealth.resolve({ data: { status: "CALCULATED", netWorth: { amount: "900.00", currency: "USD" } } }); latestSnapshot.resolve({ data: { assets: [], liabilities: [] } });
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(screen.queryByText("$900.00")).toBeNull();
  });

});
