import type { SessionData } from "@auth0/nextjs-auth0/types";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RedisSessionStore, redisRuntimeConfiguration } from "./redis-session-store";

const session = {
  user: { sub: "auth0|user" },
  tokenSet: { accessToken: "secret", expiresAt: 2_000_000_000 },
  internal: { sid: "auth0-session", createdAt: 1_700_000_000 },
} as SessionData;

describe("RedisSessionStore", () => {
  const redis = {
    get: vi.fn(),
    set: vi.fn(),
    del: vi.fn(),
  };

  beforeEach(() => vi.clearAllMocks());

  it("stores the complete session server-side with the idle TTL", async () => {
    redis.set.mockResolvedValue("OK");
    const store = new RedisSessionStore(async () => redis);

    await store.set("opaque-id", session);

    expect(redis.set).toHaveBeenCalledWith(
      "wealthos:dev:session:opaque-id",
      JSON.stringify(session),
      { expiration: { type: "EX", value: 1800 } },
    );
  });

  it("returns null for a missing session and propagates Redis failures", async () => {
    const store = new RedisSessionStore(async () => redis);
    redis.get.mockResolvedValueOnce(null);
    expect(await store.get("missing")).toBeNull();

    redis.get.mockRejectedValueOnce(new Error("redis unavailable"));
    await expect(store.get("opaque-id")).rejects.toThrow("redis unavailable");
  });

  it("deletes the backing record on logout", async () => {
    redis.del.mockResolvedValue(1);
    const store = new RedisSessionStore(async () => redis);

    await store.delete("opaque-id");

    expect(redis.del).toHaveBeenCalledWith("wealthos:dev:session:opaque-id");
  });

  it("rejects missing or insecure production Redis configuration", () => {
    expect(() => redisRuntimeConfiguration({ NODE_ENV: "production" }))
      .toThrow("REDIS_URL");
    expect(() => redisRuntimeConfiguration({
      NODE_ENV: "production",
      REDIS_SESSION_PREFIX: "wealthos:prod:session:",
      REDIS_URL: "redis://user:password@redis.internal:6379",
    })).toThrow("rediss://");
    expect(redisRuntimeConfiguration({
      NODE_ENV: "production",
      REDIS_SESSION_PREFIX: "wealthos:prod:session:",
      REDIS_URL: "rediss://user:password@redis.internal:6380",
    })).toEqual({
      keyPrefix: "wealthos:prod:session:",
      url: "rediss://user:password@redis.internal:6380",
    });
  });
});
