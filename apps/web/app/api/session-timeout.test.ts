import { describe, expect, it, vi } from "vitest";
import { handleSessionResponse } from "./session-timeout";

describe("session timeout handling", () => {
  it("sends an expired session back through login and preserves the current location", () => {
    const navigate = vi.fn();
    const response = new Response(null, { status: 401 });

    expect(handleSessionResponse(response, "/entry?mode=ai", navigate)).toBe(response);

    expect(navigate).toHaveBeenCalledWith("/auth/login?returnTo=%2Fentry%3Fmode%3Dai");
  });

  it("does not navigate for a non-authentication response", () => {
    const navigate = vi.fn();
    const response = new Response(null, { status: 403 });

    handleSessionResponse(response, "/entry", navigate);

    expect(navigate).not.toHaveBeenCalled();
  });
});
