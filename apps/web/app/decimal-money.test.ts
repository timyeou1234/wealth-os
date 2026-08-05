import { describe, expect, it } from "vitest";
import { divideMoney, formatMoney } from "./decimal-money";

describe("decimal money presentation", () => {
  it("preserves amounts larger than JavaScript's safe integer range", () => {
    expect(formatMoney("9007199254740993", "TWD")).toBe("NT$9,007,199,254,740,993");
    expect(divideMoney("9007199254740993", "1", "USD")).toBe("9007199254740993.00");
  });

  it("uses HALF_EVEN when a display conversion reaches a midpoint", () => {
    expect(divideMoney("1", "8", "USD")).toBe("0.12");
    expect(divideMoney("3", "8", "USD")).toBe("0.38");
  });
});
