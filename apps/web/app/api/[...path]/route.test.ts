import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const auth = vi.hoisted(() => ({
  getAccessToken: vi.fn(),
  getSession: vi.fn(),
}));

vi.mock("../../server/auth0", () => ({ auth0: auth }));

import { GET, POST } from "./route";

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal("fetch", vi.fn());
  vi.stubEnv("APP_BASE_URL", "http://localhost:3000");
});

afterEach(() => vi.unstubAllEnvs());

describe("authenticated BFF", () => {
  it("fails closed without a server-side session and never calls the API", async () => {
    auth.getSession.mockResolvedValue(null);

    const response = await GET(
      new Request("http://localhost:3000/api/v1/assets"),
      { params: Promise.resolve({ path: ["v1", "assets"] }) },
    );

    expect(response.status).toBe(401);
    expect(await response.json()).toEqual({ error: "unauthorized" });
    expect(fetch).not.toHaveBeenCalled();
    expect(auth.getAccessToken).not.toHaveBeenCalled();
  });

  it("fails closed when the session store is unavailable", async () => {
    auth.getSession.mockRejectedValue(new Error("redis unavailable"));

    const response = await GET(
      new Request("http://localhost:3000/api/v1/assets"),
      { params: Promise.resolve({ path: ["v1", "assets"] }) },
    );

    expect(response.status).toBe(503);
    expect(await response.json()).toEqual({ error: "authentication_unavailable" });
    expect(fetch).not.toHaveBeenCalled();
  });

  it("forwards an allowed read with a server-only bearer token", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });
    auth.getAccessToken.mockResolvedValue({ token: "server-secret-token" });
    vi.mocked(fetch).mockResolvedValue(new Response(
      JSON.stringify([{ id: "asset-1" }]),
      { status: 200, headers: { "content-type": "application/json" } },
    ));

    const response = await GET(
      new Request("http://localhost:3000/api/v1/assets?includeArchived=true"),
      { params: Promise.resolve({ path: ["v1", "assets"] }) },
    );

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/assets?includeArchived=true",
      expect.objectContaining({
        method: "GET",
        headers: expect.objectContaining({ authorization: "Bearer server-secret-token" }),
      }),
    );
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual([{ id: "asset-1" }]);
    expect(JSON.stringify([...response.headers])).not.toContain("server-secret-token");
  });

  it("rejects a mutation without both same-origin and the CSRF header", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });

    const response = await POST(
      new Request("http://localhost:3000/api/v1/assets", {
        method: "POST",
        headers: { origin: "http://localhost:3000", "content-type": "application/json" },
        body: JSON.stringify({ name: "Cash" }),
      }),
      { params: Promise.resolve({ path: ["v1", "assets"] }) },
    );

    expect(response.status).toBe(403);
    expect(fetch).not.toHaveBeenCalled();
    expect(auth.getAccessToken).not.toHaveBeenCalled();
  });

  it("rejects a cross-origin mutation even when it carries the CSRF header", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });

    const response = await POST(
      new Request("http://localhost:3000/api/v1/assets", {
        method: "POST",
        headers: {
          cookie: "wealthos_csrf=csrf-token",
          origin: "https://attacker.example",
          "x-wealthos-csrf": "csrf-token",
        },
      }),
      { params: Promise.resolve({ path: ["v1", "assets"] }) },
    );

    expect(response.status).toBe(403);
    expect(fetch).not.toHaveBeenCalled();
  });

  it("forwards an allowed same-origin mutation with its body", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });
    auth.getAccessToken.mockResolvedValue({ token: "server-secret-token" });
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 201 }));
    const body = JSON.stringify({ name: "Cash" });

    const response = await POST(
      new Request("http://localhost:3000/api/v1/assets", {
        method: "POST",
        headers: {
          origin: "http://localhost:3000",
          cookie: "wealthos_csrf=csrf-token",
          "content-type": "application/json",
          "x-wealthos-csrf": "csrf-token",
        },
        body,
      }),
      { params: Promise.resolve({ path: ["v1", "assets"] }) },
    );

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/assets",
      expect.objectContaining({
        method: "POST",
        body,
        headers: expect.objectContaining({
          authorization: "Bearer server-secret-token",
          "content-type": "application/json",
        }),
      }),
    );
    expect(response.status).toBe(201);
  });

  it("does not expose the operational FX sync route to a user session", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });

    const response = await POST(
      new Request("http://localhost:3000/api/v1/fx-rates/sync", {
        method: "POST",
        headers: {
          cookie: "wealthos_csrf=csrf-token",
          origin: "http://localhost:3000",
          "x-wealthos-csrf": "csrf-token",
        },
      }),
      { params: Promise.resolve({ path: ["v1", "fx-rates", "sync"] }) },
    );

    expect(response.status).toBe(404);
    expect(fetch).not.toHaveBeenCalled();
  });

  it("keeps direct Snapshot creation reachable through the BFF", async () => {
    auth.getSession.mockResolvedValue({ user: { sub: "auth0|user" } });
    auth.getAccessToken.mockResolvedValue({ token: "server-secret-token" });
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 201 }));

    const response = await POST(
      new Request("http://localhost:3000/api/v1/snapshots", {
        method: "POST",
        headers: {
          cookie: "wealthos_csrf=csrf-token",
          origin: "http://localhost:3000",
          "x-wealthos-csrf": "csrf-token",
        },
      }),
      { params: Promise.resolve({ path: ["v1", "snapshots"] }) },
    );

    expect(response.status).toBe(201);
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/snapshots",
      expect.objectContaining({ method: "POST" }),
    );
  });
});
