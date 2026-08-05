import { Auth0Client } from "@auth0/nextjs-auth0/server";
import { redisSessionStore } from "./redis-session-store";

export const auth0 = new Auth0Client({
  authorizationParameters: {
    audience: process.env.AUTH0_AUDIENCE,
    scope: "openid profile email offline_access",
  },
  enableAccessTokenEndpoint: false,
  session: {
    rolling: true,
    inactivityDuration: 30 * 60,
    absoluteDuration: 12 * 60 * 60,
    cookie: {
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production" || process.env.APP_BASE_URL?.startsWith("https://") === true,
    },
  },
  sessionStore: redisSessionStore,
});
