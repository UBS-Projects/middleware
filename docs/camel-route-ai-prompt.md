# MoH Middleware — Camel YAML Route AI Generator

Use this file as the **single source of truth** when asking any AI to generate a new Apache Camel YAML route for this middleware (Kaoto Designer / Dynamic Routes).

---

## How to use

1. Attach **this entire file** to the AI chat.
2. Send a short request, for example:

```text
Generate ONE new Camel YAML route using ONLY the rules and patterns in this file.

Requirement:
- id: <route-id>
- description: <clear description>
- method: GET|POST|PUT|DELETE
- path: /v1/...
- behavior: <what the route must do>
- systems/codes (if any): <e.g. NEON.TEST, DHIS2.DEV, default>
- auth (if any): <ApiToken / Basic / none — use placeholders, never invent secrets>
- notifications (if any): channel/group/template codes

Output rules:
- Return ONLY a YAML document: a single-item list starting with `- route:`
- No markdown fences, no commentary, no extra routes
- Must pass every HARD RULE below
```

---

## SYSTEM PROMPT (copy this block)

```text
You are a senior Apache Camel YAML DSL route engineer for MoH Middleware (Kaoto Designer + DynamicRouteService).

Your ONLY job: generate ONE valid Camel YAML route that matches this project's designer format and passes server-side validation.

ABSOLUTE RULES:
1. Output ONLY valid YAML: a YAML list with exactly one `- route:` document. No markdown, no prose, no explanations.
2. Clone structure, indentation, and EIP usage from REFERENCE ROUTES. Prefer existing patterns over inventing new styles.
3. Never invent component URIs, bean refs, system codes, channel codes, template codes, or auth schemes that are not present in this file (unless the user explicitly provides them).
4. Never invent or hardcode real secrets. Use clear placeholders like REPLACE_WITH_API_TOKEN or REPLACE_WITH_BASIC_AUTH.
5. Do not create duplicate path+method combinations against REFERENCE ROUTES.
6. Every route MUST have non-empty `id` and `description`.
7. `id` MUST match: ^[a-zA-Z0-9_-]+$
8. `steps` MUST be nested under `from` (Kaoto/Camel YAML style used in this project).
9. Prefer Preferred Style A for REST (`uri: rest` + parameters.method/path) unless the closest matching reference uses Style B.
10. If the request is ambiguous, choose the closest reference pattern and keep the route minimal.
11. Self-check against HARD VALIDATION RULES before answering. If a rule would fail, fix the YAML — do not explain.
```

---

## HARD VALIDATION RULES (from DynamicRouteService)

These are enforced by `DynamicRouteService.validateRoute` / `checkRouteMandatoryFields` / `extractRouteMetadata` / create duplicate checks.

### Mandatory fields
| Field | Rule |
|---|---|
| `route.id` | Required, non-blank |
| `route.description` | Required, non-blank |
| `route.id` format | Only letters, numbers, `_`, `-` → `^[a-zA-Z0-9_-]+$` |
| YAML root | Must be a **list** (SnakeYAML loads a non-empty `List`) |
| Camel load | YAML must load into Camel YAML DSL (`RoutesBuilderLoader`) and start |

### From URI schemes recognized by metadata extraction
| Style | Example | Extracted method/path |
|---|---|---|
| A — Kaoto REST | `uri: rest` + `parameters.method` + `parameters.path` | method + path |
| B — compact REST | `uri: rest:get:/api/v1/x` | method=`get`, path=`/api/v1/x` |
| Servlet | `uri: servlet:/v1/datasets` + `httpMethodRestrict` | method from restrict (default POST), path from URI |
| Platform HTTP | `uri: platform-http:/...` + `httpMethodRestrict` | same idea as servlet |
| direct / seda | `uri: direct:foo` | uri only (no HTTP path/method) |

Unrecognized `from.uri` schemes are treated as errors in metadata extraction.

### Create-time uniqueness
On **create**, `(path + httpMethod)` must be unique across existing dynamic routes. Do not reuse any path+method from REFERENCE ROUTES.

### Validation behavior notes (do not break these shapes)
- REST Style A path lives in `from.parameters.path`
- REST Style B path is inside `from.uri` after the second `:`
- Servlet/platform-http path is inside `from.uri`
- For HTTP outbound calls that need dynamic URLs, use `toD` (not static `to`)
- For DB routes: call `integratedSystem` with `code` **before** `sql`
- For mapping routes: use `integrationmapping` with `code`
- For notifications: use `notification` with `channelCode`, `groupCodes`, `templateCode`

---

## PREFERRED YAML SHAPE

```yaml
- route:
    id: example-route-id
    description: Clear human description of what this route does
    from:
      uri: rest
      parameters:
        method: get
        path: /v1/example
      steps:
        - to:
            uri: integrationmapping
            parameters:
              code: default
```

### Style A (preferred for new REST routes)
```yaml
from:
  uri: rest
  parameters:
    method: post
    path: /v1/example
  steps:
    - ...
```

### Style B (allowed; already used in several references)
```yaml
from:
  uri: rest:get:/api/v1/example
  steps:
    - ...
```

### Servlet (file upload / restricted HTTP method)
```yaml
from:
  uri: servlet:/v1/datasets
  parameters:
    httpMethodRestrict: POST
  steps:
    - ...
```

---

## ALLOWED COMPONENTS & PATTERNS

Use only these unless the user explicitly adds a new one.

### Custom MoH components
| URI | Typical parameters | When to use |
|---|---|---|
| `integrationmapping` | `code` (e.g. `default`) | Mapping/transform via Integration Mapping |
| `integratedSystem` | `code` (system code) | Select target integrated system context (DB/HTTP/etc.) |
| `notification` | `channelCode`, `groupCodes`, `templateCode` | Send notification |
| `sql` | `query` | Run SQL after `integratedSystem` selected a DB |
| `log` | `loggerName` | Debug/log body/result |

Known system codes seen in references:
- `DHIS2.DEV`
- `NEON.TEST`
- `MYSQL_DB`
- `LAST_MIDDLEWARE_DB`

Known mapping codes:
- `default`

Known notification codes:
- channel: `GMAIL_CHANNEL`
- group: `TECH_TEAM`
- templates: `TASK_ROUTE_EXECUTON`, `TASK_ROUTE_FAILURE`

Known process bean refs:
- `csvGuard`
- `dhis2CsvDryRunThenCommit`
- `dhis2ImportSummarizer`

### Standard Camel EIPs / endpoints used in references
- `unmarshal` → `json.library: Jackson`
- `setHeader` / `setHeaders`
- `setBody` with `simple.expression`
- `to` (static endpoint)
- `toD` (dynamic endpoint)
- `convertBodyTo` → `type: java.lang.String`
- `choice` / `when` / `otherwise`
- `process` with `ref`
- HTTP/HTTPS outbound:
  - static: `to.uri: https://host/path` + `bridgeEndpoint: true` + `throwExceptionOnFailure: false`
  - dynamic full URI string in `toD.uri`
  - dynamic via `toD.uri: https` + `parameters.httpUri`

### Header / expression conventions
```yaml
# Flat constant (common)
- setHeader:
    name: Content-Type
    constant: application/json

# Nested constant expression (also used)
- setHeader:
    name: Content-Type
    constant:
      expression: application/json

# Simple language
- setHeader:
    name: taskId
    simple:
      expression: /${header.id}

# JSON body field
- setBody:
    simple:
      expression: Hello ${body[name]}
```

### Path params
REST path params become headers, e.g. `/task/{id}` → `${header.id}`.

### Choice pattern
```yaml
- choice:
    when:
      - simple:
          expression: ${header.CamelHttpResponseCode} == 404
        steps:
          - to:
              uri: notification
              parameters:
                channelCode: GMAIL_CHANNEL
                groupCodes: TECH_TEAM
                templateCode: TASK_ROUTE_FAILURE
    otherwise:
      steps:
        - to:
            uri: notification
            parameters:
              channelCode: GMAIL_CHANNEL
              groupCodes: TECH_TEAM
              templateCode: TASK_ROUTE_EXECUTON
```

---

## PATTERN CATALOG (choose the closest)

| Need | Clone from route id | Core steps |
|---|---|---|
| Integration mapping GET | `HealthCenter-mapping` / `Hospital_HealthMap_Mapping` | rest GET → `integrationmapping` |
| External HTTPS GET + auth header | `dhis2-system-info` | set Authorization → `to` https → `convertBodyTo` |
| POST JSON → dynamic upstream GET | `get-tei-info` | unmarshal JSON → setHeaders → `toD` → convertBodyTo |
| Simple JSON transform response | `hello-name-route` | unmarshal → setBody simple → Content-Type |
| CSV upload via servlet + beans | `dhis2-upload-csv` | servlet POST → process refs + `integratedSystem` |
| DB SELECT via integrated system | `neon_error_mapping` / `my-sql` / `db` | `integratedSystem` → `sql` → setBody → log → Content-Type |
| External HTTPS + notification on result | `external_user` | setHeader path → `toD` https → choice → notification |

---

## SELF-CHECKLIST (AI must pass all before output)

- [ ] Root is a YAML list with exactly one `- route:`
- [ ] `id` present and matches `^[a-zA-Z0-9_-]+$`
- [ ] `description` present and non-empty (not "New route description" unless user asked for that)
- [ ] `from.uri` is a recognized scheme
- [ ] For Style A REST: `parameters.method` and `parameters.path` exist
- [ ] `steps` is under `from`
- [ ] path+method does not collide with REFERENCE ROUTES
- [ ] DB flow includes `integratedSystem` before `sql`
- [ ] Dynamic URL uses `toD`
- [ ] No real secrets hardcoded
- [ ] Indentation is 2 spaces; no tabs
- [ ] No markdown fences in the final answer

---

## REFERENCE ROUTES (golden examples)

> Secrets below are redacted placeholders. Replace only when the user supplies real values.

```yaml
- route:
    id: HealthCenter-mapping
    description: HealthCenter mapping
    from:
      uri: rest
      parameters:
        method: get
        path: /HealthCenter
      steps:
        - to:
            id: to-3694
            uri: integrationmapping
            parameters:
              code: default

- route:
    id: dhis2-system-info
    description: GET DHIS2 production system info
    from:
      uri: rest
      parameters:
        method: get
        path: /v1/system-info
      steps:
        - setHeader:
            constant: ApiToken REPLACE_WITH_DHIS2_API_TOKEN
            name: Authorization
        - to:
            uri: https://hmis.moh.gov.jo/dwh/api/system/info
            parameters:
              bridgeEndpoint: true
              throwExceptionOnFailure: false
        - convertBodyTo:
            type: java.lang.String

- route:
    id: get-tei-info
    description: Get tracked entity information by TEI UID from DHIS2 Test
    from:
      uri: rest
      parameters:
        method: post
        path: /v1/tei-info
      steps:
        - unmarshal:
            json:
              library: Jackson
        - setHeaders:
            id: setHeaders-2114
            headers:
              - constant:
                  expression: GET
                name: CamelHttpMethod
              - constant:
                  expression: Basic REPLACE_WITH_BASIC_AUTH_BASE64
                name: Authorization
              - id: ""
                name: tei
                simple:
                  expression: ${body[tei]}
        - toD:
            uri: https://play.im.dhis2.org/dev-2-42/api/tracker/trackedEntities/${header.tei}?fields=*&bridgeEndpoint=true&throwExceptionOnFailure=false
        - convertBodyTo:
            type: java.lang.String

- route:
    id: hello-name-route
    description: Return hello message using name from request body
    from:
      uri: rest
      parameters:
        method: post
        path: /v1/hello
      steps:
        - unmarshal:
            json:
              library: Jackson
        - setBody:
            simple:
              expression: Hello ${body[name]}
        - setHeader:
            constant: text/plain
            name: Content-Type

- route:
    id: dhis2-upload-csv
    description: POST /v1/datasets
    from:
      uri: servlet:/v1/datasets
      parameters:
        httpMethodRestrict: POST
      steps:
        - process:
            ref: csvGuard
        - to:
            id: to-3233
            uri: integratedSystem
            parameters:
              code: DHIS2.DEV
        - process:
            ref: dhis2CsvDryRunThenCommit
        - process:
            ref: dhis2ImportSummarizer

- route:
    id: Hospital_HealthMap_Mapping
    description: Hospital HealthMap
    from:
      uri: rest
      parameters:
        method: get
        path: /v1/hospitales
      steps:
        - to:
            id: to-3694
            uri: integrationmapping
            parameters:
              code: default

- route:
    id: neon_error_mapping
    description: Get Neon error mapping rows ordered by id
    from:
      uri: rest:get:/api/v1/error-mapping
      steps:
        - to:
            id: to-3383
            uri: integratedSystem
            parameters:
              code: NEON.TEST
        - to:
            id: to-3803
            uri: sql
            parameters:
              query: SELECT * FROM public.error_mapping ORDER BY id ASC
        - setBody:
            id: setBody-2385
            simple:
              expression: ${body}
        - to:
            id: to-2101
            uri: log
            parameters:
              loggerName: db-result
        - setHeader:
            id: setHeader-2101
            constant:
              expression: application/json
            name: Content-Type

- route:
    id: my-sql
    description: Query MySQL employee table
    from:
      uri: rest:post:/api/sql
      steps:
        - to:
            id: to-2447
            uri: integratedSystem
            parameters:
              code: MYSQL_DB
        - to:
            id: to-3113
            uri: sql
            parameters:
              query: SELECT * FROM employee;
        - setBody:
            simple:
              expression: ${body}
        - to:
            uri: log
            parameters:
              loggerName: output

- route:
    id: db
    description: Query middleware users by id=1
    from:
      uri: rest:get:/api/db
      steps:
        - to:
            id: to-3383
            uri: integratedSystem
            parameters:
              code: LAST_MIDDLEWARE_DB
        - to:
            id: to-3803
            uri: sql
            parameters:
              query: SELECT * FROM users where id = 1
        - setBody:
            id: setBody-2385
            simple:
              expression: ${body}
        - to:
            id: to-2101
            uri: log
            parameters:
              loggerName: db-result
        - setHeader:
            id: setHeader-2101
            constant:
              expression: application/json
            name: Content-Type

- route:
    id: external_user
    description: Fetch external todo by id and notify tech team
    from:
      uri: rest:get:/task/{id}
      steps:
        - setHeader:
            id: setHeader-5680
            name: taskId
            simple:
              expression: /${header.id}
        - toD:
            id: to-2566
            uri: https
            parameters:
              bridgeEndpoint: true
              httpUri: jsonplaceholder.typicode.com/todos${header.taskId}
              throwExceptionOnFailure: false
        - setBody:
            id: setBody-7188
            simple:
              expression: ${body}
        - choice:
            id: choice-1273
            otherwise:
              id: otherwise-6097
              steps:
                - to:
                    id: to-2513
                    uri: notification
                    parameters:
                      channelCode: GMAIL_CHANNEL
                      groupCodes: TECH_TEAM
                      templateCode: TASK_ROUTE_EXECUTON
            when:
              - id: when-2185
                steps:
                  - to:
                      id: to-2581
                      uri: notification
                      parameters:
                        channelCode: GMAIL_CHANNEL
                        groupCodes: TECH_TEAM
                        templateCode: TASK_ROUTE_FAILURE
                simple:
                  expression: ${header.CamelHttpResponseCode} == 404
```

---

## OCCUPIED PATH + METHOD MAP (do not collide)

| Method | Path | Route id |
|---|---|---|
| GET | `/HealthCenter` | HealthCenter-mapping |
| GET | `/v1/system-info` | dhis2-system-info |
| POST | `/v1/tei-info` | get-tei-info |
| POST | `/v1/hello` | hello-name-route |
| POST | `/v1/datasets` | dhis2-upload-csv |
| GET | `/v1/hospitales` | Hospital_HealthMap_Mapping |
| GET | `/api/v1/error-mapping` | neon_error_mapping |
| POST | `/api/sql` | my-sql |
| GET | `/api/db` | db |
| GET | `/task/{id}` | external_user |

---

## FEW-SHOT REQUEST → EXPECTED BEHAVIOR

### Example request
```text
Create a GET route /v1/facilities that uses integration mapping code default.
id: facilities-mapping
description: Facilities HealthMap mapping
```

### Expected output shape
```yaml
- route:
    id: facilities-mapping
    description: Facilities HealthMap mapping
    from:
      uri: rest
      parameters:
        method: get
        path: /v1/facilities
      steps:
        - to:
            uri: integrationmapping
            parameters:
              code: default
```

### Example request
```text
Create a GET route /api/v1/employees that queries MySQL_DB with SELECT * FROM employee ORDER BY id
```

### Expected output shape
```yaml
- route:
    id: employees-list
    description: List employees from MySQL
    from:
      uri: rest
      parameters:
        method: get
        path: /api/v1/employees
      steps:
        - to:
            uri: integratedSystem
            parameters:
              code: MYSQL_DB
        - to:
            uri: sql
            parameters:
              query: SELECT * FROM employee ORDER BY id
        - setBody:
            simple:
              expression: ${body}
        - to:
            uri: log
            parameters:
              loggerName: db-result
        - setHeader:
            constant:
              expression: application/json
            name: Content-Type
```

---

## SECURITY NOTE

Do not paste production tokens into prompts or commit them.
If a real token was shared earlier, rotate it and keep only placeholders in this file.
