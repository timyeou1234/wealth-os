# Security Policy

Wealth OS handles highly sensitive personal financial information. Security and privacy
requirements are design inputs, not post-release hardening tasks.

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability or expose real financial data
in a report. Until a private reporting channel is configured, contact the repository
owner privately through their GitHub profile.

Include the affected component, reproduction conditions, likely impact, and any safe
mitigation you have identified. Do not include credentials, account numbers, tokens, or
other personal data.

## Baseline expectations

- No secrets or production financial data in source control, fixtures, logs, or issues
- Least-privilege authentication and authorization when identity work begins
- Encryption in transit and appropriate encryption at rest
- Explicit audit, retention, deletion, backup, and recovery policies before production
- Dependency, container, and source scanning in CI before deployable artifacts exist
- Threat modeling before selecting the authentication architecture

Supported-version and disclosure timelines will be defined before the first public
release.
