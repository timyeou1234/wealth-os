import type { SessionData, SessionDataStore } from "@auth0/nextjs-auth0/types";
import { createClient } from "redis";

const IDLE_TTL_SECONDS = 30 * 60;

type SessionRedisClient = {
  get(key: string): Promise<string | null>;
  set(
    key: string,
    value: string,
    options: { expiration: { type: "EX"; value: number }; condition?: "XX" },
  ): Promise<string | null>;
  del(key: string): Promise<number>;
};

type RedisProvider = () => Promise<SessionRedisClient>;
type RuntimeEnvironment = Record<string, string | undefined>;

export function redisRuntimeConfiguration(environment: RuntimeEnvironment = process.env): {
  keyPrefix: string;
  url: string;
} {
  const production = environment.NODE_ENV === "production";
  const url = environment.REDIS_URL ?? (production ? undefined : "redis://localhost:6379");
  const keyPrefix = environment.REDIS_SESSION_PREFIX ?? (production ? undefined : "wealthos:dev:session:");
  if (!url) throw new Error("REDIS_URL is required in production");
  if (!keyPrefix) throw new Error("REDIS_SESSION_PREFIX is required in production");
  if (production) {
    const parsed = new URL(url);
    if (parsed.protocol !== "rediss:") throw new Error("Production REDIS_URL must use rediss://");
    if (!parsed.password) throw new Error("Production REDIS_URL must include authentication credentials");
  }
  return { keyPrefix, url };
}

export class RedisSessionStore implements SessionDataStore {
  constructor(
    private readonly getRedis: RedisProvider,
    private readonly keyPrefix?: string,
  ) {}

  private key(id: string): string {
    return `${this.keyPrefix ?? redisRuntimeConfiguration().keyPrefix}${id}`;
  }

  async get(id: string): Promise<SessionData | null> {
    const value = await (await this.getRedis()).get(this.key(id));
    return value === null ? null : JSON.parse(value) as SessionData;
  }

  async set(id: string, session: SessionData): Promise<void> {
    await (await this.getRedis()).set(this.key(id), JSON.stringify(session), {
      expiration: { type: "EX", value: IDLE_TTL_SECONDS },
    });
  }

  async update(id: string, session: SessionData): Promise<boolean> {
    const result = await (await this.getRedis()).set(this.key(id), JSON.stringify(session), {
      condition: "XX",
      expiration: { type: "EX", value: IDLE_TTL_SECONDS },
    });
    return result === "OK";
  }

  async delete(id: string): Promise<void> {
    await (await this.getRedis()).del(this.key(id));
  }
}

let redis: ReturnType<typeof createClient> | undefined;
let connection: Promise<SessionRedisClient> | undefined;

async function connectedRedis(): Promise<SessionRedisClient> {
  if (!connection) {
    redis ??= createClient({ url: redisRuntimeConfiguration().url });
    redis.on("error", () => undefined);
    connection = redis.connect()
      .then(() => redis as SessionRedisClient)
      .catch((error: unknown) => {
        connection = undefined;
        throw error;
      });
  }
  return connection;
}

export const redisSessionStore = new RedisSessionStore(connectedRedis);
