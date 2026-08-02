# Wealth OS Web

Independently deployable Next.js application for the Wealth OS dashboard.

## Development

Run the API on port 8080, then start the web application:

```bash
pnpm install
pnpm dev
```

The dashboard uses an OpenAPI-generated client in `app/api/generated`. Refresh it after
an API-contract change while the API is running:

```bash
pnpm api:generate
```

Generated files contain only API contracts and client code; they never contain database
or demo data.
