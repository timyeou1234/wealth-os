import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Dashboard from "./page";

describe("Dashboard", () => {
  it("shows the latest snapshot financial health and its position details", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce({ ok: true, json: async () => [{ id: "older", asOf: "2026-07-01T00:00:00Z" }, { id: "latest", asOf: "2026-08-01T00:00:00Z" }] })
        .mockResolvedValueOnce({ ok: true, json: async () => ({ status: "CALCULATED", totalAssets: { amount: "1000.00", currency: "USD" }, totalLiabilities: { amount: "250.00", currency: "USD" }, netWorth: { amount: "750.00", currency: "USD" }, debtRatio: "0.250000", liquidityRatio: "0.600000" }) })
        .mockResolvedValueOnce({ ok: true, json: async () => ({
          assets: [{ id: "cash", name: "Cash reserve", type: "CASH", liquidity: "LIQUID", money: { amount: "1000.00", currency: "USD" }, effectiveAt: "2026-08-01T00:00:00Z", source: "Bank statement" }],
          liabilities: [{ id: "loan", name: "Car loan", money: { amount: "250.00", currency: "USD" }, effectiveAt: "2026-08-01T00:00:00Z", source: "Lender statement" }],
        }) }),
    );

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
  });
});
