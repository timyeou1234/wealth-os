# Authentication and Authorization

## Purpose

Wealth OS stores personal financial data. Issue #13 therefore makes authenticated owner
isolation a prerequisite for deployment rather than an optional account feature. This
document defines the human login, BFF session, API authorization, ownership, and machine
administration boundaries.

## Trust boundaries

```text
Browser
  │ opaque HttpOnly session cookie
  ▼
Next.js BFF ── user Bearer access token ──▶ Spring resource server
  │ Redis session store                         │
  │                                             ▼
  └── Auth0 / Google                       PostgreSQL

Operational service ── M2M Bearer token with fx:sync ──▶ Spring operations endpoint
```

- The browser is an untrusted presentation client.
- Next.js is the OAuth client and BFF, not the final authorization authority.
- Redis owns only revocable, expiring BFF sessions. It is never a financial source of
  truth.
- Spring validates every Bearer token and owns application authorization.
- PostgreSQL owns local Users and personal financial records.
- Auth0 brokers identity and issues tokens; Google is the only first-release human
  identity provider.

## Human login

1. The browser follows the Next.js login route.
2. Next.js starts the Auth0 Authorization Code flow and validates transaction state,
   nonce, and PKCE material on callback.
3. Auth0 authenticates with Google. Only configured, verified email identities may
   continue.
4. Next.js exchanges the authorization code server-side and stores the resulting session
   in Redis.
5. The browser receives only an opaque session key in an HttpOnly cookie. The Auth0
   client-side access-token endpoint is disabled.
6. A same-origin BFF route resolves the server session, obtains a current access token,
   and calls Spring with `Authorization: Bearer ...`.
7. Spring validates signature, issuer, audience, expiry, and authorities, then resolves
   issuer plus subject to one local User.

The backend repeats the allowlist decision during local User provisioning. A verified
email is allowlist and display metadata only; issuer plus subject is the stable external
identity. A rejected or unverified identity creates no local User.

## Server-side session

The BFF uses the Auth0 Next.js SDK with a custom Redis session store. The default complete
encrypted-cookie session is not used.

- Idle timeout is 30 minutes and absolute lifetime is 12 hours.
- There is no remember-me mode.
- Cookies are HttpOnly, Secure in HTTPS environments, and use an explicit SameSite
  policy.
- Mutating BFF routes additionally validate origin and CSRF state.
- Logout deletes the Redis session and clears the browser cookie; coordinated Auth0
  logout prevents an immediately reusable provider session where supported.
- Redis unavailability fails closed as unauthenticated. There is no cookie-token or
  direct-to-Spring fallback.
- Session values, including tokens, are never logged. Redis uses authenticated TLS,
  environment-specific credentials and namespaces, bounded TTLs, and least-privilege
  access.

## Owner resolution and isolation

Product APIs do not contain a user or owner selector. Spring resolves the current local
`UserId` from the validated security context and supplies it internally to application
use cases. Financial repositories require that `UserId` for every query and mutation.

Conceptually:

```sql
SELECT *
FROM snapshot
WHERE owner_id = :sessionDerivedUserId
  AND id = :requestedSnapshotId;
```

The caller supplies only the requested Snapshot identifier. A record owned by a different
User produces the same `404` as a nonexistent record. This rule applies to Assets,
Liabilities, Snapshots, corrections, comparisons, and derived Financial Health views.

Official CBC FX rates are shared system reference data. User-declared conversion evidence
is stored inside an owned Snapshot and inherits that Snapshot's access boundary.

## Machine administration

Human sessions and machine administration are separate principals. A front-end User has
no administrator role.

- The in-process FX scheduler calls the synchronization use case directly.
- An external operational tool uses Auth0 Client Credentials with a dedicated M2M client.
- Manual synchronization requires the `fx:sync` scope and an approved machine client.
- M2M credentials live only in the operational secret store.
- An M2M token cannot read personal financial endpoints merely because it can synchronize
  reference rates.

## HTTP failure semantics

| Condition | Result |
| --- | --- |
| Missing, malformed, expired, or invalid-audience token | `401 Unauthorized` |
| Valid human identity that is not allowed | `403 Forbidden`, no User created |
| Valid User requests a missing or cross-owner resource | `404 Not Found` |
| Human token calls manual FX sync | `403 Forbidden` |
| Approved M2M token lacks `fx:sync` | `403 Forbidden` |
| Approved M2M token has `fx:sync` | Synchronization use case may run |

## Environment and exposure

Development and production use separate Auth0 applications, API audiences, callback and
logout URLs, M2M clients, cookie secrets, Redis namespaces, and credentials. No real
email, token, secret, or financial data is committed.

Production exposes Next.js over HTTPS. Spring, Redis, PostgreSQL, and Swagger remain on
private networks. Local development may expose Spring and Swagger and may use a dedicated
development OAuth client.

## Verification seams

- Spring HTTP behavior for unauthenticated, invalid-token, and cross-owner requests.
- First-use local User provisioning and stable issuer-plus-subject resolution.
- Asset, Liability, Snapshot, comparison, and Financial Health owner isolation through
  their public HTTP APIs.
- Same-origin BFF behavior, logout, timeout, CSRF rejection, and token non-disclosure.
- M2M scope enforcement at the manual FX synchronization endpoint.
- Hands-on browser verification of login, import, Snapshot capture, Dashboard access,
  logout, re-login, and failed unauthorized access.
