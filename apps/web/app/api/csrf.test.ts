import { describe, expect, it } from "vitest";
import { csrfTokenFromCookie } from "./csrf";

describe("CSRF cookie", () => {
  it("reads the server-issued token without confusing similarly named cookies", () => {
    expect(csrfTokenFromCookie("other=1; wealthos_csrf=token%20value; wealthos_csrf_old=no"))
      .toBe("token value");
  });

  it("returns null when no CSRF state exists", () => {
    expect(csrfTokenFromCookie("other=1")).toBeNull();
  });
});
