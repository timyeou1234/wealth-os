import { describe, expect, it, vi } from "vitest";

const auth0Client = vi.hoisted(() => vi.fn(function Auth0Client() {}));
const sessionStore = vi.hoisted(() => ({}));

vi.mock("@auth0/nextjs-auth0/server", () => ({ Auth0Client: auth0Client }));
vi.mock("./redis-session-store", () => ({ redisSessionStore: sessionStore }));

await import("./auth0");

describe("Auth0 server client", () => {
  it("uses only server-side sessions with the accepted timeout policy", async () => {
    expect(auth0Client).toHaveBeenCalledWith(expect.objectContaining({
      authorizationParameters: {
        audience: process.env.AUTH0_AUDIENCE,
        scope: "openid profile email offline_access wealth:access",
      },
      enableAccessTokenEndpoint: false,
      sessionStore,
      session: expect.objectContaining({
        rolling: true,
        inactivityDuration: 1800,
        absoluteDuration: 43200,
        cookie: expect.objectContaining({ sameSite: "lax" }),
      }),
    }));
  });
});
