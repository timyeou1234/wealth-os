# ADR-009: Isolate private access through an authenticated BFF

- **Status:** Accepted
- **Date:** 2026-08-05
- **Decision owners:** Wealth OS maintainers

## Context

Wealth OS stores personal Assets, Liabilities, immutable Snapshots, and derived financial
views. The current local-development API has no identity or ownership boundary. Deploying
it with real data would let any reachable caller operate the same global dataset.

The Next.js and Spring applications are independently deployable. Browser JavaScript must
not hold API access or refresh tokens, but Spring must still validate identity rather than
trust an unverified header from the web tier. Scheduled and manual system administration
must not turn the first front-end user into an administrator.

## Decision

Auth0 brokers the first-release Google OIDC login for explicitly allowlisted, verified
identities. Next.js is a mandatory backend-for-frontend and OAuth client. It uses a custom
Redis server-side session store and gives the browser only an opaque HttpOnly session key;
the client-side access-token endpoint is disabled. Sessions expire after 30 idle minutes
and 12 absolute hours.

Next.js obtains a user access token from the server session and calls the private Spring
API. Spring acts as a standard OAuth resource server and independently validates JWT
signature, issuer, audience, expiry, and authority. It maps external issuer plus subject
to a local User. Verified email is allowlist and display metadata, not ownership identity.

Requests never accept an owner selector. Spring supplies the session-derived local UserId
to application services, and every personal financial repository operation requires it.
Cross-owner identifiers behave as not found. Assets, Liabilities, Snapshots, corrections,
comparisons, and derived views are owned; official FX rates remain shared reference data.

The internal FX scheduler calls its application use case directly. External manual
synchronization uses a separate Auth0 M2M client and requires `fx:sync`. A front-end human
principal cannot receive operational authority. Production exposes only Next.js publicly;
Spring, Redis, PostgreSQL, and Swagger remain private. Development and production identity,
session, M2M, and secret configurations are separate.

## Consequences

### Positive

- Browser JavaScript never receives a bearer token.
- A stolen or disabled session can be revoked centrally in Redis.
- Spring remains independently protected if a BFF route is misconfigured.
- Owner isolation is enforced at persistence access rather than repeated ad hoc in
  controllers.
- Human and operational identities cannot silently inherit one another's authority.
- OIDC issuer plus subject permits a future provider migration without making email a
  financial ownership key.

### Negative

- Every browser API request adds a BFF hop.
- Redis becomes required for authenticated web availability and must be operated securely.
- Authentication requires coordinated Auth0, Next.js, Spring, Redis, PostgreSQL, and
  deployment configuration.
- Existing unowned development data cannot satisfy the new invariants and must be reset
  and re-imported.
- Account recovery, collaboration, and administrative user experiences remain deferred.

## Guardrails

- Never return access or refresh tokens to browser code or enable a client access-token
  endpoint.
- Never store bearer tokens in HTML, JavaScript state, localStorage, or sessionStorage.
- Never accept a client-supplied UserId or owner selector for product resources.
- Never expose an owner-free financial repository operation to application code.
- Validate JWT signature, issuer, audience, expiry, and required authority at Spring.
- Return `404` for both missing and cross-owner resource identifiers.
- Fail closed when Redis or identity validation is unavailable; do not fall back to a
  browser token or unauthenticated route.
- Keep official FX reference data global, but treat declared conversion evidence as part
  of its owning Snapshot.
- Do not grant operational scopes to human product sessions.
- Keep production Spring and Swagger private and keep all real secrets and allowlist
  values outside source control.
- Treat authentication claims, tokens, session contents, and personal financial data as
  prohibited log content.

## Alternatives considered

### Browser-held SPA access token

Rejected because it exposes a bearer token to the browser JavaScript execution environment
and weakens the mandatory BFF boundary.

### Auth0 default encrypted session cookie

Rejected because JavaScript cannot read it, but the complete encrypted session and tokens
still travel with the browser. A server-side Redis store keeps only an opaque session key
in the browser and provides direct revocation.

### Spring owns the interactive login session

Rejected because it couples the separate Next.js deployment to Spring cookie, CSRF, and
cross-origin behavior and leaves the intended BFF role unclear.

### Auth.js with a Next.js-issued API token

Rejected for the first release because Wealth OS would need to design and operate an
additional trust or token-issuer boundary for Spring.

### Direct Google ID token API access

Rejected because an ID token authenticates a user to its client; it is not the scoped
access token intended for the Wealth OS API.

### Privileged first user

Rejected because a front-end human identity must not implicitly become a system operator.
Operational access uses a separate machine principal.
