# API Contract Conventions

## Purpose

This document defines the shared language between the Wealth OS web application and API.
Feature endpoints must follow these rules so that the generated frontend client can use
one predictable contract.

Spring controller code and annotations are the source of truth. `springdoc-openapi`
generates the machine-readable OpenAPI document at `/v3/api-docs` and the human-readable
Swagger UI at `/swagger-ui.html`.

## Paths and compatibility

- Product endpoints use the `/api/v1` prefix.
- The version belongs in the path so incompatible future changes can be introduced
  without silently changing an existing client contract.
- Resource names use lowercase plural nouns, for example `/api/v1/assets`.
- OpenAPI documentation endpoints are development tools and do not use the product API
  prefix.

## JSON representation

- Property names use `lowerCamelCase`.
- Identifiers are UUID strings, for example
  `"0f27e4fa-99f8-4c5e-87da-527488cbe515"`.
- Enumerations use stable uppercase strings, for example `"REAL_ESTATE"`.
- Calendar dates use ISO 8601 `YYYY-MM-DD` strings.
- Instants use ISO 8601 strings with a UTC offset, preferably `Z`, for example
  `"2026-07-31T08:30:00Z"`.
- Optional properties that have no value are represented as `null` or omitted according
  to the endpoint schema. An empty string is not a substitute for a missing value.

## Money and currency

Money is represented as an object:

```json
{
  "amount": "1250000.00",
  "currency": "TWD"
}
```

`amount` is a decimal string rather than a JSON floating-point number. This prevents a
browser from introducing binary rounding errors into financial values. `currency` is an
uppercase ISO 4217 currency code. Feature endpoints must not return a bare amount without
its currency.

The target Snapshot contract in Issue #66 (not implemented by the FX-rate infrastructure
slice) uses `originalMoney` for the submitted fact and TWD
`money` for canonical valuation. Foreign-currency facts include structured
`appliedConversion` evidence: rate, rate date, provider, rate type, optional user basis,
and rounding mode.

## Foreign-exchange rates

These endpoints are implemented by the first Issue #66 vertical slice.

- `POST /api/v1/fx-rates/sync` synchronizes the fixed CBC provider through the configured
  upstream adapter. Optional `from` and `to` dates form a bounded inclusive range.
- `GET /api/v1/fx-rates?asOf=YYYY-MM-DD&currencies=USD,JPY` resolves the nearest rate on
  or before `asOf` for each requested currency.
- Rates are direct original-currency/TWD quotes: `1 originalCurrency = rate TWD`.
- TWD resolves to the identity rate without persistence. Missing currencies are explicit;
  no endpoint silently substitutes a different provider or future date.

## Validation and errors

- Invalid client input returns an appropriate `4xx` status and an
  `application/problem+json` body based on RFC 9457 Problem Details.
- The response includes a stable problem `type`, a short `title`, the HTTP `status`, and
  actionable `detail`.
- Field validation failures use the stable type
  `urn:wealthos:problem:validation-error` and include an `errors` array. Each entry
  identifies the invalid `field` and a user-actionable `message`:

```json
{
  "type": "urn:wealthos:problem:validation-error",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/assets",
  "errors": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

- A request for an Asset or Liability identifier that does not exist returns `404` with
  problem type `urn:wealthos:problem:asset-not-found` or
  `urn:wealthos:problem:liability-not-found`, respectively. The `detail` identifies the
  missing resource, and `instance` contains the requested resource path.
- Unexpected server failures return `5xx` without exposing stack traces, credentials, or
  personal financial data.

## Collections

Pagination is deferred until the first collection endpoint needs it. Its query
parameters and response metadata must be added here before that endpoint is published;
individual controllers must not invent incompatible pagination formats.

## Resource lifecycle

- Asset and Liability metadata is replaced with `PUT /api/v1/<resources>/{id}`.
- Archiving uses `POST /api/v1/<resources>/{id}/archive`; it does not delete historical
  Snapshot facts.
- Archive operations return `204 No Content`.
- Default Asset and Liability collections contain active resources only.
- Direct resource lookup continues to return an archived resource with
  `archived: true`, allowing callers to distinguish it from a missing identifier.
- Updating or archiving current metadata never rewrites an existing Snapshot.

## Atomic snapshot capture

The web entry workflow uses `POST /api/v1/snapshot-captures` to synchronize current
Asset and Liability metadata and create one immutable Snapshot in a single database
transaction.

- Entries with an existing ID update that active resource; entries without an ID create
  a resource and receive an identity before capture.
- The request must include every active Asset and Liability. Missing active identities,
  duplicate identities, unknown identities, and archived identities are invalid.
- Every monetary fact must use the request's base currency.
- The selected base currency is stored on the resulting Snapshot and returned as
  `baseCurrency`, including when the Snapshot has no positions. Snapshots created through
  the general direct API may omit this capture context and return `null`.
- Metadata mutations and Snapshot persistence either commit together or roll back
  together.
- Omission never archives a resource. Archiving remains an explicit, separately
  confirmed lifecycle operation.
- Existing resource CRUD and direct Snapshot creation remain separate public APIs.

## Untrusted assisted-entry data

AI-assisted import is a web-client convenience, not an AI service integration. Wealth OS
does not transmit prompts or financial data to a model. Agent output is pasted by the
user, validated locally, previewed, and merged into the unsaved form only after explicit
confirmation.

Imported data is untrusted. The client accepts only raw JSON or one fenced JSON block,
uses a strict versioned field allowlist, rejects unknown and prototype-pollution keys,
limits payload size and position count, and validates identifiers, enums, dates, decimal
strings, currencies, and field lengths. Imported strings are rendered as text and are
never executed or inserted as HTML. Server validation remains authoritative.

## Security boundary

The first API is for local development and a single-user product. Authentication and the
deployment threat model remain undecided. Swagger UI and product endpoints must not be
made publicly reachable with real financial data until those decisions are implemented.
