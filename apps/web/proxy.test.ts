import { NextRequest, NextResponse } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";

const auth = vi.hoisted(() => ({
  getSession: vi.fn(),
  middleware: vi.fn(),
}));

vi.mock("./app/server/auth0", () => ({ auth0: auth }));

import { proxy } from "./proxy";

beforeEach(() => {
  vi.clearAllMocks();
  auth.middleware.mockResolvedValue(NextResponse.next());
});

describe("authentication proxy", () => {
  it("redirects an unauthenticated protected page to login with its return path", async () => {
    auth.getSession.mockResolvedValue(null);

    const response = await proxy(new NextRequest("http://localhost:3000/entry?mode=ai"));

    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe(
      "http://localhost:3000/auth/login?returnTo=%2Fentry%3Fmode%3Dai",
    );
  });

  it("lets the Auth0 routes execute without requiring an existing session", async () => {
    const expected = NextResponse.next();
    auth.middleware.mockResolvedValue(expected);

    const response = await proxy(new NextRequest("http://localhost:3000/auth/callback?code=code"));

    expect(response).toBe(expected);
    expect(auth.getSession).not.toHaveBeenCalled();
  });

  it("runs authenticated BFF traffic through the rolling-session middleware", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });

    const response = await proxy(new NextRequest("http://localhost:3000/api/v1/assets"));

    expect(response.status).toBe(200);
    expect(auth.middleware).toHaveBeenCalledOnce();
  });

  it("returns 401 instead of redirecting an unauthenticated BFF request", async () => {
    auth.getSession.mockResolvedValue(null);

    const response = await proxy(new NextRequest("http://localhost:3000/api/v1/assets"));

    expect(response.status).toBe(401);
    expect(response.headers.get("location")).toBeNull();
  });

  it("issues unpredictable CSRF state after authentication", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });

    const response = await proxy(new NextRequest("http://localhost:3000/entry"));

    expect(response.cookies.get("wealthos_csrf")?.value).toMatch(/^[0-9a-f-]{36}$/);
  });

  it("rotates CSRF state on callback and clears it on coordinated logout", async () => {
    const callback = await proxy(new NextRequest("http://localhost:3000/auth/callback?code=code"));
    expect(callback.cookies.get("wealthos_csrf")?.value).toMatch(/^[0-9a-f-]{36}$/);

    const logout = await proxy(new NextRequest("http://localhost:3000/auth/logout", {
      headers: { cookie: "wealthos_csrf=old-token" },
    }));
    expect(logout.headers.get("set-cookie")).toContain("wealthos_csrf=");
    expect(logout.headers.get("set-cookie")).toContain("Expires=Thu, 01 Jan 1970");
  });
});
