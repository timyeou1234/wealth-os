# Wealth OS Web

Independently deployable Next.js application for the Wealth OS dashboard.

## Development

Create a dedicated Auth0 Regular Web Application for local development. Enable only its
Google connection and configure these application URLs:

- Allowed Callback URL: `http://localhost:3000/auth/callback`
- Allowed Logout URL: `http://localhost:3000`
- Allowed Web Origin: `http://localhost:3000`

Create a separate Auth0 API whose audience matches the Spring API configuration. Copy
`.env.example` to `.env.local`, replace every placeholder, and generate the cookie secret
with `openssl rand -hex 32`. Never commit `.env.local` or real identity values.

Run Redis and the authenticated API on port 8080, then start the web application:

```bash
docker compose -f ../../infra/compose.yaml up -d wealthos-redis
pnpm install
pnpm dev
```

The browser reaches product APIs only through the same-origin `/api/*` BFF. Auth0 access
and refresh tokens are stored in Redis; browser JavaScript receives neither token. The
session expires after 30 idle minutes or 12 total hours. Use the in-app **Sign out** link
to delete the server session, clear the cookie, and complete Auth0 logout.

Production must use a different Auth0 application/API, HTTPS callback URLs, an
authenticated `rediss://` Redis endpoint, and an environment-specific Redis prefix.

The dashboard uses an OpenAPI-generated client in `app/api/generated`. Refresh it after
an API-contract change while the API is running:

```bash
pnpm api:generate
```

Generated files contain only API contracts and client code; they never contain database
or demo data.
