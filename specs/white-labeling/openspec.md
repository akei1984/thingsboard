# openspec.md — ThingsBoard Open Source White Labeling

**Change-ID:** `add-open-source-white-labeling`  
**Repository:** `thingsboard/thingsboard`  
**Target product:** ThingsBoard Community Edition / Open Source  
**Feature:** White Labeling for web UI and login page  
**Status:** Proposed  
**Language:** Java 17 backend, Angular UI (`ui-ngx`)  
**Principle:** Preserve existing ThingsBoard architecture and reuse existing platform libraries/patterns wherever possible.

---

## 0. Source references

This OpenSpec proposal is based on the public ThingsBoard PE white-labeling documentation and the public ThingsBoard architecture documentation.

- ThingsBoard PE White Labeling: https://thingsboard.io/docs/pe/user-guide/white-labeling/
- ThingsBoard PE product feature overview: https://thingsboard.io/products/thingsboard-pe/
- ThingsBoard Community Edition architecture: https://thingsboard.io/docs/reference/
- ThingsBoard monolithic architecture: https://thingsboard.io/docs/reference/monolithic/
- ThingsBoard microservices architecture: https://thingsboard.io/docs/reference/msa/
- ThingsBoard source repository: https://github.com/thingsboard/thingsboard
- OpenSpec: https://openspec.dev/

---

## 1. OpenSpec packaging note

OpenSpec normally stores a change as multiple files:

```text
openspec/
└── changes/
    └── add-open-source-white-labeling/
        ├── proposal.md
        ├── design.md
        ├── tasks.md
        └── specs/
            └── white-labeling/
                └── spec.md
```

This document is a **single-file version** of those artifacts for easier review.  
When using OpenSpec CLI, split the sections below into the target files.

---

# proposal.md

## Why

ThingsBoard PE supports White Labeling, but ThingsBoard Community Edition currently does not expose an equivalent open-source capability. The goal is to add a clean, maintainable White Labeling feature to the open-source ThingsBoard repository without introducing a parallel product architecture or unnecessary dependencies.

The feature should allow platform operators and tenants to configure a branded ThingsBoard experience without rebuilding the UI or restarting the service.

## What changes

Add an open-source White Labeling capability with:

1. Runtime branding for the main ThingsBoard web interface.
2. Runtime branding for the login page, optionally resolved by domain.
3. Inheritance of settings from System → Tenant → Customer.
4. Configurable application title, favicon, logo, logo height, primary/accent color palettes, optional advanced CSS, and platform name/version visibility.
5. REST APIs for reading effective settings and saving scoped settings.
6. Angular UI for System/Tenant-level configuration, following existing ThingsBoard UI conventions.
7. Backend persistence, validation, permission checks, and cache invalidation.
8. No new external runtime service.
9. No new third-party library unless the target branch lacks an already available equivalent.

## Scope

### In scope

- Main web application branding:
  - Browser tab title.
  - Favicon.
  - Header/sidebar logo.
  - Logo height.
  - Primary palette.
  - Accent palette.
  - Advanced CSS.
  - Show/hide platform name and version.
- Login page branding:
  - Domain-specific login configuration.
  - Login title.
  - Login favicon.
  - Login logo.
  - Login primary/accent palette.
  - Login background color.
- Inheritance:
  - System settings apply globally.
  - Tenant settings override system settings for the tenant.
  - Customer settings override tenant settings for customer users where enabled.
- Runtime updates:
  - Save changes without rebuilding Angular UI.
  - Save changes without restarting ThingsBoard.
  - Cache invalidation after update.
- Security:
  - Authority-based write access.
  - Safe upload validation.
  - Strict domain normalization and exact matching.
  - Avoid blindly trusting forwarded host headers.
- Tests:
  - Backend unit tests.
  - REST controller/security tests.
  - Angular unit tests.
  - Minimal end-to-end/manual verification checklist.

### Out of scope for the first implementation

- Reimplementing all ThingsBoard PE-only adjacent features such as custom menu, custom translations, Trendz name override, advanced RBAC, PE mobile app white labeling, or customer hierarchy beyond what exists in CE.
- Adding a new object storage dependency.
- Adding a new microservice.
- Changing ThingsBoard transport, rule engine, telemetry, dashboard, or queue architecture.
- Supporting arbitrary remote asset URLs in uploaded logo/favicon fields.
- Guaranteeing compatibility with private PE implementation details.

## Impact

### Backend

Expected modules/classes follow existing ThingsBoard patterns and should be adjusted to the actual target branch structure:

- `common/data`:
  - Add DTO/model classes for white-labeling settings.
  - Add ID/type classes only if required by existing entity conventions.
- `dao`:
  - Add DAO/service for storing and resolving settings.
  - Add SQL schema migration.
  - Add cache abstraction using existing cache infrastructure.
- `application`:
  - Add REST controller endpoints.
  - Add permission checks using existing security context and authority model.
  - Add no-auth endpoint for login page effective branding.
- `ui-ngx`:
  - Add Angular service, route, forms, validators, and runtime theme application.
  - Reuse existing Angular Material, Reactive Forms, translation, notification/snackbar, and dialog components.

### Data storage

Use existing ThingsBoard persistence conventions:

- SQL-compatible schema.
- JSON stored as text/JSON string, following existing `additionalInfo`-style patterns where possible.
- Use existing file/blob storage if available in the target open-source branch.
- If no reusable blob/file service exists in the target branch, store small image assets as validated data URIs with strict size limits as MVP.

### Deployment

- Works in monolithic mode.
- Works in microservices mode without creating a new service.
- Works behind a reverse proxy if the proxy forwards a canonical host and blocks untrusted host headers.

## Dependencies

Prefer existing project dependencies:

- Backend:
  - Spring Boot / Spring MVC.
  - Spring Security.
  - Jackson.
  - JPA/Hibernate and existing DAO patterns.
  - Existing ThingsBoard cache abstraction or Spring cache if already used.
  - Existing validation utilities.
- Frontend:
  - Angular.
  - Angular Material.
  - RxJS.
  - Existing ThingsBoard UI components, form patterns, page layout, dialogs, and services.
  - Native browser APIs for favicon/title updates.

Do **not** add a new theming framework, CSS-in-JS library, image-processing library, or external asset storage client for the MVP.

---

# specs/white-labeling/spec.md

## ADDED Requirements

### Requirement: White-labeling configuration hierarchy

The system MUST resolve white-labeling settings using a hierarchy of System → Tenant → Customer. A lower level MUST inherit from its parent level and MAY override specific settings within its own scope.

#### Scenario: System default applies globally

- **GIVEN** a System Administrator configured system-level white-labeling settings
- **AND** a tenant has no tenant-level override
- **WHEN** a tenant user opens the ThingsBoard web interface
- **THEN** the effective branding MUST be resolved from system-level settings

#### Scenario: Tenant override applies to tenant users

- **GIVEN** system-level settings exist
- **AND** a tenant has configured tenant-level white-labeling settings
- **WHEN** a tenant user opens the ThingsBoard web interface
- **THEN** the effective branding MUST use the tenant override where defined
- **AND** MUST inherit missing values from system-level settings

#### Scenario: Customer override applies to customer users

- **GIVEN** system-level and tenant-level settings exist
- **AND** customer-level white-labeling is enabled for the customer
- **AND** customer-level settings exist
- **WHEN** a customer user opens the ThingsBoard web interface
- **THEN** the effective branding MUST use the customer override where defined
- **AND** MUST inherit missing values from tenant and system settings

#### Scenario: Reset restores inherited values

- **GIVEN** a tenant or customer has overridden one or more branding fields
- **WHEN** an authorized user resets the scope to default
- **THEN** the system MUST remove scope-specific overrides
- **AND** the UI MUST immediately display inherited values

---

### Requirement: Main web interface branding

The system MUST allow authorized users to configure the main ThingsBoard web interface branding at supported scopes.

Supported fields:

| Field | Type | Required | Notes |
|---|---:|---:|---|
| `applicationTitle` | string | no | Browser tab title |
| `faviconAssetId` or `faviconDataUri` | asset reference/string | no | ICO/PNG/SVG subject to validation |
| `logoAssetId` or `logoDataUri` | asset reference/string | no | Header/sidebar logo |
| `logoHeight` | integer | no | Pixel value with min/max validation |
| `primaryPalette` | object/string | no | Reuse existing Angular Material palette concepts |
| `accentPalette` | object/string | no | Reuse existing Angular Material palette concepts |
| `advancedCss` | string | no | Admin-controlled scoped CSS |
| `showPlatformNameAndVersion` | boolean | no | Controls footer/version visibility |

#### Scenario: Main application title is applied

- **GIVEN** an authorized user saves `applicationTitle = "ACME IoT Platform"`
- **WHEN** another user in the same effective scope opens the web UI
- **THEN** the browser title MUST be `ACME IoT Platform`
- **AND** no service restart MUST be required

#### Scenario: Logo is applied in navigation shell

- **GIVEN** an authorized user uploads a valid logo
- **WHEN** a user opens the authenticated web UI
- **THEN** the header/sidebar logo MUST display the configured logo
- **AND** the logo height MUST follow the configured `logoHeight`

#### Scenario: Platform version can be hidden

- **GIVEN** `showPlatformNameAndVersion = false`
- **WHEN** the user opens the web UI
- **THEN** the footer/sidebar MUST NOT display the platform name and version
- **AND** default behavior MUST remain unchanged when the field is not set

---

### Requirement: Login page branding

The system MUST allow authorized users to configure login page branding, optionally bound to a registered domain.

Supported fields:

| Field | Type | Required | Notes |
|---|---:|---:|---|
| `domain` | string | conditional | Required for domain-specific login branding |
| `applicationTitle` | string | no | Browser/login title |
| `faviconAssetId` or `faviconDataUri` | asset reference/string | no | Login favicon |
| `logoAssetId` or `logoDataUri` | asset reference/string | no | Login logo |
| `primaryPalette` | object/string | no | Login primary palette |
| `accentPalette` | object/string | no | Login accent palette |
| `backgroundColor` | string | no | Hex/RGB color token only |

#### Scenario: Login page uses domain-specific branding

- **GIVEN** a tenant has configured login white-labeling for `iot.example.com`
- **WHEN** an unauthenticated user opens `https://iot.example.com/login`
- **THEN** the login page MUST display the configured tenant login title, favicon, logo, colors, and background
- **AND** the user MUST NOT need to be authenticated before seeing the login branding

#### Scenario: Unknown domain falls back safely

- **GIVEN** no login branding exists for `unknown.example.com`
- **WHEN** an unauthenticated user opens the login page through that domain
- **THEN** the login page MUST display the system default branding
- **AND** the system MUST NOT leak tenant/customer configuration data

#### Scenario: Host header is normalized and matched exactly

- **GIVEN** login branding exists for `iot.example.com`
- **WHEN** the request includes a mixed-case host or a host with a port
- **THEN** the system MUST normalize the host to lowercase and remove the port before matching
- **AND** wildcard matching MUST NOT be used in the MVP

---

### Requirement: White-labeling permissions

The system MUST enforce write permissions according to existing ThingsBoard authorities and security patterns.

#### Scenario: System administrator manages system-level settings

- **GIVEN** the current user has `SYS_ADMIN` authority
- **WHEN** the user saves system-level white-labeling settings
- **THEN** the system MUST persist the settings
- **AND** invalidate the relevant cache

#### Scenario: Tenant administrator manages tenant-level settings

- **GIVEN** the current user has `TENANT_ADMIN` authority
- **WHEN** the user saves tenant-level white-labeling settings
- **THEN** the system MUST persist settings only for the current tenant
- **AND** MUST NOT allow writing settings for another tenant

#### Scenario: Customer user cannot change branding by default

- **GIVEN** the current user has `CUSTOMER_USER` authority
- **WHEN** the user calls a write endpoint
- **THEN** the system MUST reject the request unless an explicit existing permission model allows this in the target branch
- **AND** read-only effective branding resolution MUST remain available

---

### Requirement: Runtime update without rebuild or restart

The system MUST apply white-labeling changes at runtime.

#### Scenario: Saved changes are visible after refresh

- **GIVEN** an authorized user changes tenant branding
- **WHEN** the user saves the settings
- **THEN** the backend MUST invalidate cached effective settings for that tenant
- **AND** the Angular UI MUST apply the latest settings after refresh or next effective-settings fetch
- **AND** the ThingsBoard service MUST NOT require restart

#### Scenario: Multi-node deployments converge

- **GIVEN** ThingsBoard runs in a clustered or microservices deployment
- **WHEN** a node updates white-labeling settings
- **THEN** all nodes MUST eventually serve updated settings
- **AND** the implementation SHOULD use existing cluster/cache invalidation mechanisms where available
- **AND** if no cluster invalidation mechanism is available, cache TTL MUST be short and configurable

---

### Requirement: Advanced CSS

The system MUST support optional advanced CSS for authorized administrators while minimizing security and maintainability risk.

#### Scenario: Advanced CSS is applied to authenticated shell

- **GIVEN** advanced CSS is configured at the effective scope
- **WHEN** a user opens the authenticated web UI
- **THEN** the UI MUST inject the CSS into a dedicated runtime style element
- **AND** replacing branding MUST replace the previous runtime style element rather than appending duplicates

#### Scenario: Advanced CSS is limited

- **GIVEN** a user saves advanced CSS
- **WHEN** the backend validates the payload
- **THEN** the backend MUST enforce a configurable maximum length
- **AND** the UI MUST clearly label advanced CSS as an administrator-controlled customization
- **AND** the system MUST NOT weaken global CSP or add unsafe external resource loading for the MVP

---

### Requirement: Asset upload validation

The system MUST validate uploaded logo and favicon assets.

#### Scenario: Invalid image is rejected

- **GIVEN** an authorized user uploads a file as a logo or favicon
- **WHEN** the file content type, extension, or size violates validation rules
- **THEN** the backend MUST reject the upload
- **AND** the UI MUST show a validation error

#### Scenario: Valid asset is stored using existing storage

- **GIVEN** an authorized user uploads a valid PNG, SVG, ICO, JPG, or WEBP asset
- **WHEN** the file passes validation
- **THEN** the backend MUST store it using existing ThingsBoard file/blob storage if available
- **AND** otherwise MUST store it as a bounded data URI in the white-labeling settings JSON for the MVP

---

### Requirement: REST API

The system MUST expose REST APIs for white-labeling management and effective branding resolution.

Required endpoint intent:

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/whiteLabeling/settings` | yes | Read editable settings for current scope |
| `POST` | `/api/whiteLabeling/settings` | yes | Save editable settings for current scope |
| `DELETE` | `/api/whiteLabeling/settings` | yes | Reset editable settings for current scope |
| `GET` | `/api/whiteLabeling/effective` | yes | Read effective authenticated UI branding |
| `GET` | `/api/noauth/whiteLabeling/login` | no | Read effective login branding by request domain |
| `POST` | `/api/whiteLabeling/asset` | yes | Upload logo/favicon asset, if asset storage is used |
| `DELETE` | `/api/whiteLabeling/asset/{assetId}` | yes | Delete unused white-labeling asset, if asset storage is used |

#### Scenario: No-auth endpoint returns only public branding

- **GIVEN** an unauthenticated request calls `/api/noauth/whiteLabeling/login`
- **WHEN** the backend resolves login branding
- **THEN** the response MUST include only public presentation fields
- **AND** MUST NOT include tenant IDs, customer IDs, internal object IDs where avoidable, audit data, or security-relevant configuration

---

### Requirement: Backward compatibility

The system MUST preserve default ThingsBoard behavior when no white-labeling settings are configured.

#### Scenario: No settings configured

- **GIVEN** no white-labeling settings exist
- **WHEN** any user opens ThingsBoard
- **THEN** the UI MUST display the standard ThingsBoard branding
- **AND** all existing login, authentication, dashboards, widgets, rule engine, transport, and telemetry behavior MUST remain unchanged

---

# design.md

## Architecture approach

Implement White Labeling as a normal ThingsBoard application capability:

```text
Angular UI (ui-ngx)
  ↓ REST
WhiteLabelingController / NoAuthWhiteLabelingController
  ↓
WhiteLabelingService
  ↓
WhiteLabelingDao / existing storage abstractions
  ↓
SQL database + optional existing blob/file storage
```

The feature belongs to the core web application and does **not** require transport-layer, rule-engine, telemetry, dashboard-rendering, or queue changes.

## Data model

Use a dedicated table because white-labeling settings are configuration records with scope, inheritance, domain lookup, cache behavior, and asset references.

Recommended logical model:

```text
white_labeling_settings
- id UUID primary key
- created_time bigint not null
- updated_time bigint not null
- scope varchar not null            -- SYSTEM | TENANT | CUSTOMER
- tenant_id UUID nullable
- customer_id UUID nullable
- type varchar not null             -- GENERAL | LOGIN
- domain varchar nullable           -- normalized login domain
- settings_json text not null       -- serialized WhiteLabelingSettings
- version bigint not null
```

Recommended constraints:

```text
unique(scope, tenant_id, customer_id, type)
unique(domain) where domain is not null
index(tenant_id)
index(customer_id)
index(type, domain)
```

For databases that do not support partial indexes, enforce unique non-null domain at DAO/service level and provide database-specific migration where ThingsBoard already separates SQL dialects.

## Settings DTO

Use explicit DTOs rather than unstructured maps.

```json
{
  "general": {
    "applicationTitle": "ACME IoT Platform",
    "faviconAssetId": null,
    "faviconDataUri": null,
    "logoAssetId": null,
    "logoDataUri": null,
    "logoHeight": 32,
    "primaryPalette": {
      "name": "custom",
      "main": "#1976d2",
      "contrast": "#ffffff"
    },
    "accentPalette": {
      "name": "custom",
      "main": "#ff4081",
      "contrast": "#ffffff"
    },
    "advancedCss": "",
    "showPlatformNameAndVersion": true
  },
  "login": {
    "domain": "iot.example.com",
    "applicationTitle": "ACME IoT Platform",
    "faviconAssetId": null,
    "faviconDataUri": null,
    "logoAssetId": null,
    "logoDataUri": null,
    "primaryPalette": {
      "name": "custom",
      "main": "#1976d2",
      "contrast": "#ffffff"
    },
    "accentPalette": {
      "name": "custom",
      "main": "#ff4081",
      "contrast": "#ffffff"
    },
    "backgroundColor": "#ffffff"
  }
}
```

## Resolution algorithm

### Authenticated UI

```text
resolveEffectiveBranding(user):
  if SYS_ADMIN:
    return system settings merged with defaults

  if TENANT_ADMIN:
    return merge(defaults, system settings, tenant settings)

  if CUSTOMER_USER:
    return merge(defaults, system settings, tenant settings, customer settings if enabled)

  return defaults
```

### Login UI

```text
resolveLoginBranding(request):
  host = normalizeHost(request)
  if host matches configured login domain:
    return public projection of matching settings merged with inherited parent settings
  return public projection of system/default login settings
```

## Domain handling

Domain normalization:

- Lowercase.
- Strip port.
- Trim trailing dot.
- Reject scheme, path, query, wildcard, whitespace, control characters.
- Store internationalized domains in ASCII/Punycode form if Java standard library conversion is already available.
- Use exact match only in MVP.

Security expectation:

- Behind reverse proxies, deployments SHOULD sanitize and explicitly set `Host` / `X-Forwarded-Host`.
- The backend MUST only use forwarded headers if the existing ThingsBoard/Spring proxy configuration marks them as trusted.
- If trust cannot be determined, use the server/request host and safe fallback.

## Cache

Use existing ThingsBoard cache patterns. Suggested cache keys:

```text
wl:effective:system
wl:effective:tenant:<tenantId>
wl:effective:customer:<tenantId>:<customerId>
wl:login:domain:<domain>
```

Cache behavior:

- Cache effective merged settings, not only raw settings.
- Invalidate affected descendants when parent settings change.
- Use configurable TTL for clustered deployments.
- If existing cluster event propagation is available, publish an invalidation event.
- Do not cache failed authorization decisions.

Suggested configuration keys:

```yaml
white_labeling:
  enabled: "${TB_WHITE_LABELING_ENABLED:true}"
  cache:
    ttl: "${TB_WHITE_LABELING_CACHE_TTL:300}"
  assets:
    max_logo_size: "${TB_WHITE_LABELING_MAX_LOGO_SIZE:262144}"
    max_favicon_size: "${TB_WHITE_LABELING_MAX_FAVICON_SIZE:65536}"
  advanced_css:
    max_length: "${TB_WHITE_LABELING_ADVANCED_CSS_MAX_LENGTH:32768}"
  login:
    trust_forwarded_host: "${TB_WHITE_LABELING_TRUST_FORWARDED_HOST:false}"
```

## Backend validation

Validate:

- Scope is allowed for current authority.
- Tenant/customer IDs match current security context.
- Domain is normalized and unique.
- Color values are valid hex or known palette tokens.
- `logoHeight` is within configured min/max, e.g. 16–96 px.
- Image MIME type and size are allowed.
- SVG, if supported, is treated carefully:
  - No script tags.
  - No external references.
  - No event handlers.
  - If safe SVG sanitization is not already available, SVG upload SHOULD be disabled in MVP.

## Frontend design

### Pages

Add a White Labeling configuration page under the existing settings/navigation structure.

Suggested UI sections:

1. General
   - Application title.
   - Favicon upload.
   - Logo upload.
   - Logo height.
   - Primary palette.
   - Accent palette.
   - Advanced CSS dialog/editor.
   - Show platform name/version.
   - Reset to inherited/default.
2. Login
   - Domain.
   - Login title.
   - Login favicon.
   - Login logo.
   - Primary palette.
   - Accent palette.
   - Background color.
   - Preview area.
   - Reset to inherited/default.

### Runtime theming

Add an Angular service that:

- Fetches effective branding at application bootstrap/login page load.
- Updates `document.title`.
- Updates favicon link element.
- Applies logo URL/data URI to existing shell components.
- Adds or replaces a dedicated `<style id="tb-white-labeling-style">`.
- Applies CSS variables or class-level theme tokens that existing components can consume.

Avoid rewriting global theme architecture. Use existing SCSS/CSS variables and Angular Material palette conventions where possible.

## Migration

Add SQL migrations using the existing ThingsBoard migration pattern.

Migration must:

- Create the white-labeling table.
- Add indexes/constraints.
- Not alter existing user, tenant, customer, dashboard, device, telemetry, or rule-chain tables.
- Be reversible in development by dropping only the new objects if local migration rollback tooling is used.

## API response examples

### Effective authenticated branding

```json
{
  "applicationTitle": "ACME IoT Platform",
  "faviconUrl": "/api/whiteLabeling/asset/...",
  "logoUrl": "/api/whiteLabeling/asset/...",
  "logoHeight": 32,
  "primaryPalette": {
    "main": "#1976d2",
    "contrast": "#ffffff"
  },
  "accentPalette": {
    "main": "#ff4081",
    "contrast": "#ffffff"
  },
  "advancedCss": ".tb-side-menu { background: #ffffff; }",
  "showPlatformNameAndVersion": false
}
```

### Public login branding

```json
{
  "applicationTitle": "ACME IoT Platform",
  "faviconUrl": "/api/noauth/whiteLabeling/asset/...",
  "logoUrl": "/api/noauth/whiteLabeling/asset/...",
  "primaryPalette": {
    "main": "#1976d2",
    "contrast": "#ffffff"
  },
  "accentPalette": {
    "main": "#ff4081",
    "contrast": "#ffffff"
  },
  "backgroundColor": "#ffffff"
}
```

## Security considerations

- The no-auth endpoint must return only display-safe fields.
- CSS is powerful and can affect UX. It must be write-restricted to administrators.
- Do not support arbitrary external asset URLs in MVP.
- Do not allow script-capable SVG unless sanitized by existing trusted utilities.
- Enforce size limits at controller and service layer.
- Include audit log entries if an existing audit log mechanism is available for settings changes.
- Do not bypass existing ThingsBoard authority checks.
- Avoid leaking existence of tenant domains through verbose errors.

## Compatibility with ThingsBoard architecture

### Monolithic mode

All components run in the same JVM. White Labeling is implemented in the core application layer and uses normal REST/DAO access. No additional process is required.

### Microservices mode

White Labeling remains part of the ThingsBoard Node/web UI path. Transport microservices, rule engine processing, Kafka topics, telemetry storage, and device protocol handling are unaffected.

### Database modes

The feature stores platform configuration data. It should use the SQL/entity database path, not telemetry storage. It must not require Cassandra/TimescaleDB.

---

# tasks.md

## 1. Repository analysis

- [ ] Inspect target branch module layout: `common/data`, `dao`, `application`, `ui-ngx`.
- [ ] Identify existing cache abstraction used for admin/settings-like entities.
- [ ] Identify existing file/blob storage availability in the open-source target branch.
- [ ] Identify existing REST controller patterns for settings/configuration.
- [ ] Identify existing Angular settings pages and route conventions.
- [ ] Confirm existing test frameworks and naming conventions.

## 2. Backend model and persistence

- [ ] Add white-labeling DTOs and enums.
- [ ] Add persistence entity and DAO model.
- [ ] Add SQL migration for `white_labeling_settings`.
- [ ] Add service interface and implementation.
- [ ] Add settings merge/resolution algorithm.
- [ ] Add domain normalization utility.
- [ ] Add validation utilities.
- [ ] Add asset reference/data URI support depending on existing storage.
- [ ] Add cache and invalidation logic.
- [ ] Add audit logging if existing audit service is available.

## 3. Backend REST API

- [ ] Add authenticated controller for editable settings.
- [ ] Add authenticated endpoint for effective branding.
- [ ] Add unauthenticated login branding endpoint.
- [ ] Add asset upload/download endpoints if required.
- [ ] Add security annotations/checks following existing ThingsBoard style.
- [ ] Add error mapping for invalid domain, invalid image, duplicate domain, and unauthorized scope.

## 4. Frontend UI

- [ ] Add Angular model interfaces.
- [ ] Add Angular white-labeling REST service.
- [ ] Add route/menu entry visible only to authorized users.
- [ ] Add General tab form.
- [ ] Add Login tab form.
- [ ] Add image upload controls with preview.
- [ ] Add palette selector or color controls using existing UI components.
- [ ] Add advanced CSS dialog/editor using existing dialog/form components.
- [ ] Add reset-to-default action.
- [ ] Add runtime branding service for title, favicon, logo, CSS, and palette.
- [ ] Add login-page branding fetch before/while rendering login page.

## 5. Tests

- [ ] Unit test settings merge order.
- [ ] Unit test domain normalization.
- [ ] Unit test permission checks.
- [ ] Unit test invalid image rejection.
- [ ] Unit test no-auth response projection.
- [ ] REST test unauthorized write attempts.
- [ ] REST test tenant isolation.
- [ ] REST test domain-specific login branding.
- [ ] Angular service tests for effective branding application.
- [ ] Angular component tests for form validation.
- [ ] Manual browser test for no-restart update behavior.

## 6. Documentation

- [ ] Add CE documentation page for White Labeling.
- [ ] Document supported scopes and inheritance.
- [ ] Document reverse-proxy/domain requirements.
- [ ] Document asset size/type limits.
- [ ] Document security limitations of Advanced CSS.
- [ ] Document defaults and reset behavior.

## 7. Validation

- [ ] Run backend unit tests.
- [ ] Run backend integration/REST tests.
- [ ] Run frontend unit tests.
- [ ] Build `ui-ngx`.
- [ ] Build ThingsBoard with `mvn clean install -DskipTests` and at least one test-enabled profile in CI.
- [ ] Validate OpenSpec change with `openspec validate add-open-source-white-labeling --strict` after splitting into OpenSpec folder structure.

---

# Implementation notes for coding agents

## Keep existing architecture

Do not introduce a separate White Labeling service, sidecar, or frontend application. The implementation must fit into ThingsBoard’s current Java backend + Angular SPA architecture.

## Prefer existing patterns

Before creating new abstractions, inspect similar existing features:

- Admin settings.
- OAuth/login settings.
- Mail templates.
- Theme/layout settings.
- Blob/file storage.
- Audit logging.
- Cache invalidation.
- Angular settings pages.

## Avoid dependency creep

Only add a dependency if:

1. The target branch has no existing equivalent.
2. The feature is unsafe or impractical without it.
3. The dependency is small, maintained, compatible with ThingsBoard’s license model, and approved in review.

## Default behavior must remain unchanged

If `white_labeling_settings` is empty, the system must behave exactly like ThingsBoard CE before this change.

## MVP decisions

If implementation uncertainty exists, choose the least invasive option:

- Exact domain match instead of wildcard domains.
- Admin-only advanced CSS.
- Small validated image assets.
- Runtime style injection instead of rewriting the SCSS build.
- Configurable cache TTL if distributed invalidation is not immediately available.
- Public login branding endpoint with minimal fields only.

---

# Acceptance checklist

The feature is acceptable when:

- [ ] A System Administrator can configure global branding.
- [ ] A Tenant Administrator can configure tenant branding without affecting other tenants.
- [ ] Customer users see inherited branding.
- [ ] Login page branding resolves by configured domain.
- [ ] Unknown domains fall back safely.
- [ ] No rebuild or service restart is required after saving branding.
- [ ] Invalid assets are rejected.
- [ ] Unauthorized users cannot change branding.
- [ ] Default ThingsBoard CE behavior is unchanged with no settings.
- [ ] Monolithic deployment works.
- [ ] Microservices deployment is not broken.
- [ ] Tests cover resolution, validation, permissions, and public login response.
