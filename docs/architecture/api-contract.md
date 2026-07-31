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

- Unexpected server failures return `5xx` without exposing stack traces, credentials, or
  personal financial data.

## Collections

Pagination is deferred until the first collection endpoint needs it. Its query
parameters and response metadata must be added here before that endpoint is published;
individual controllers must not invent incompatible pagination formats.

## Security boundary

The first API is for local development and a single-user product. Authentication and the
deployment threat model remain undecided. Swagger UI and product endpoints must not be
made publicly reachable with real financial data until those decisions are implemented.
