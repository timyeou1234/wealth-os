import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Dashboard from "./page";

const api = vi.hoisted(() => ({
  getFinancialHealth: vi.fn(),
  getSnapshot: vi.fn(),
  listSnapshots: vi.fn(),
}));

vi.mock("./api/client", () => api);

describe("Dashboard", () => {
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
    expect(screen.getByText("Total Assets = Sum of all asset values in the base currency")).toBeTruthy();
    expect(screen.getByText("Net Worth = Total Assets - Total Liabilities")).toBeTruthy();
    expect(screen.getByText("Included in immediately liquid assets: assets classified as LIQUID. SEMI_LIQUID and ILLIQUID assets are excluded.")).toBeTruthy();
  });
});
