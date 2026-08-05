# Infrastructure

`compose.yaml` runs the local Redis dependency on the loopback interface only:

```bash
docker compose -f infra/compose.yaml up -d wealthos-redis
docker compose -f infra/compose.yaml ps
```

Local session data is intentionally ephemeral. Production Redis is an external private
dependency and must require authenticated TLS, an environment-isolated namespace, bounded
TTL, and least-privilege credentials. Infrastructure must not contain committed secrets
or real financial data.
