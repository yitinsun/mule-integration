# System Design Specification: Public API Integration

Architecture for two local processes. Spring Boot is the edge. Mule is the only caller of the public APIs. There is no database and no cache.

v1 **does not** include a second Spring deployable, CloudHub, SQL, Redis, Swagger, or an API key. Those remain future re-plans.

Package for all Java types: `com.yitinsun.muleintegration`.

## 1. Component Data Flow

```
[ Client ]
    │
    ├── specific GET /api/v1/... ──> [ public-api-edge :8080 ]
    │                                      │  RequestRules
    │                                      ▼
    └── POST /api/v1/integrate ──> [ RestClient MULE_BASE_URL ]
                                         │
                                         ▼
                               [ mule-public-api :8081 ]
                                    │  same rules, then HTTP requester
                                    ▼
                     ipify | agify | httpbin | zippopotam | coinmarketcap
```

Edge paths and the Mule paths they call:

| Edge | Mule |
|:-----|:-----|
| `GET /api/v1/ip` | `GET /providers/ipify?format=json\|text` |
| `GET /api/v1/age?name=` | `GET /providers/agify?name=` |
| `GET /api/v1/echo` | `GET /providers/httpbin` |
| `GET /api/v1/postal/{country}/{postalCode}` | `GET /providers/zippopotam/{country}/{postalCode}` |
| `GET /api/v1/crypto/price?...` | `GET /providers/coinmarketcap/price?...` |
| `POST /api/v1/integrate` | `POST /integrate` |

Mule specific flows return the **task JSON** from `PRD.md` (IPify always `{ "ip" }` even when upstream was plain text). The edge turns that into `text/plain` only for `GET /api/v1/ip?format=text`. Mule `POST /integrate` returns the general envelope. The edge passes that envelope through after its own validation.

Connect budget **2000 ms**. Read budget **5000 ms**. No automatic retry. One attempt per client call.

## 2. Module layout (Stage 3 outputs)

```
public-api-edge/
  pom.xml
  src/main/java/com/yitinsun/muleintegration/
    PublicApiEdgeApplication.java
    config/MuleProperties.java
    config/MuleClientConfig.java          RestClient bean
    domain/Provider.java                   enum
    domain/RequestRules.java
    integration/MuleClient.java
    integration/UpstreamException.java
    web/IpController.java
    web/AgeController.java
    web/EchoController.java
    web/PostalController.java
    web/CryptoPriceController.java
    web/IntegrateController.java
    web/GlobalExceptionHandler.java
    web/dto/   records named in §4
  src/main/resources/application.yml
  src/test/java/...                        classes in TESTING_PLAN.md
  src/test/resources/fixtures/             JSON files named in TESTING_PLAN.md

mule-public-api/
  pom.xml
  mule-artifact.json
  src/main/mule/public-api.xml
  src/main/resources/properties/public-api.yaml
```

No root aggregator pom. The two builds do not share a `java.version`.

## 3. Edge validation (`RequestRules`)

Pure static methods. No Spring. Controllers and the Mule general flow both follow this table; Java unit tests lock the Java copy.

| Method | Behavior |
|:-------|:---------|
| `format(String raw)` | `null` → `json`. `json` or `text` only. Else invalid format. |
| `name(String raw)` | Trim. Pattern `^[\p{L}][\p{L} .'\-]{0,63}$`. Else invalid or missing. |
| `country(String raw)` | Two ASCII letters, returned lowercase. |
| `postalCode(String raw)` | Pattern `^[A-Za-z0-9][A-Za-z0-9 -]{0,19}$`. |
| `coinIds(String raw)` | Split on `,`. 1–50 tokens. Each token `\d{1,10}`. No spaces. |
| `flag(String raw)` | `null` → `false`. `true` or `false` only. |
| `provider(String raw)` | Exact enum name: `ipify`, `agify`, `httpbin`, `zippopotam`, `coinmarketcap`. |
| `operation(Provider, String raw)` | `null` → that provider’s default (`PRD.md` §2.1). Else must equal the default. |
| `params(Provider, Map)` | Keys must be the required and optional set for that provider. Values are strings or booleans (flags). Reject nested objects and unknown keys. |
| `rejectUnknownQuery(Map, Set)` | Specific endpoints: any key outside the allowed set is a 400. |

`Provider` defaults:

| Enum | Operation | Query keys allowed on the specific edge route |
|:-----|:----------|:----------------------------------------------|
| `IPIFY` | `public-ip` | `format` |
| `AGIFY` | `predict-age` | `name` |
| `HTTPBIN` | `echo-get` | (none) |
| `ZIPPOPOTAM` | `lookup-postal` | (path only) |
| `COINMARKETCAP` | `simple-price` | `ids`, `includeMarketCap`, `includeVolume24h`, `includePercentChange24h`, `includeLastUpdated` |

Failures throw `IllegalArgumentException` with the **exact** `message` string from `PRD.md` §3. `GlobalExceptionHandler` maps that exception to **400** and the matching provider when the controller knows it.

## 4. Records (HTTP boundary)

Immutable Java `record`s. Jackson property names are the JSON names in `PRD.md`.

| Record | Fields |
|:-------|:-------|
| `IpResponse` | `String ip` |
| `AgeResponse` | `String name`, `Integer age`, `long count` |
| `EchoResponse` | `String url`, `String origin`, `Map<String, String> headers`, `Map<String, String> args` |
| `PostalResponse` | `String postalCode`, `String country`, `String countryAbbreviation`, `List<Place> places` |
| `Place` | `String placeName`, `String state`, `String stateAbbreviation`, `String latitude`, `String longitude` |
| `CryptoPriceResponse` | `String currency`, `List<CryptoPrice> prices` |
| `CryptoPrice` | `long id`, `BigDecimal price`, `BigDecimal marketCap`, `BigDecimal volume24h`, `BigDecimal percentChange24h`, `String lastUpdated`. Annotate this record with `@JsonInclude(JsonInclude.Include.NON_NULL)` so unrequested flags are omitted. |
| `IntegrateRequest` | `String provider`, `String operation`, `Map<String, Object> params` |
| `IntegrateResponse` | `String provider`, `String operation`, `JsonNode data` |
| `ErrorResponse` | `int status`, `String error`, `String message`, `String provider` |

`MuleClient` deserializes Mule’s JSON into these records. It does not re-map upstream field names. That mapping is DataWeave in §6.

## 5. `MuleClient` and errors

`MuleClientConfig` exposes:

```java
@Bean
RestClient muleRestClient(RestClient.Builder builder, MuleProperties props)
```

Base URL is `props.baseUrl()`. Connect and read timeouts are the budgets in §1. Tests bind `MockRestServiceServer` to that same `RestClient.Builder`.

`UpstreamException` carries HTTP status, the `PRD.md` message, and the provider id. Mapping:

| Condition | Status | Message |
|:----------|:-------|:--------|
| Connect exception, or no response | 503 | `Integration layer is unavailable.` |
| Timeout (`ResourceAccessException` that is a timeout, or Mule status 504) | 504 | `Upstream request timed out.` |
| Mule status 400, 404, 415, 429, 502 | same status | message body from Mule when it matches §3; otherwise the §3 default for that status |
| Any other non-2xx | 502 | `Upstream response was not usable.` |
| 2xx body that fails to parse | 502 | `Upstream response was not usable.` |

`GlobalExceptionHandler` writes `ErrorResponse` for `UpstreamException`, `IllegalArgumentException`, `HttpMessageNotReadableException` (empty or broken JSON → 400 `Integration request is invalid.`), and `HttpMediaTypeNotSupportedException` (415). Unhandled exceptions become **502** with `Upstream response was not usable.` and a logged stack trace. Do not return a Whitelabel HTML page for these API routes.

## 6. Mule application

### 6.1 `mule-artifact.json`

```json
{
  "minMuleVersion": "4.9.0",
  "javaSpecificationVersions": ["17"]
}
```

Raise `minMuleVersion` only to match the Studio runtime actually used (for example `4.13.0`). Record that choice in the audit log.

### 6.2 `properties/public-api.yaml`

```yaml
http:
  listener:
    host: "0.0.0.0"
    port: "8081"

ipify:
  protocol: "HTTPS"
  host: "api.ipify.org"
  port: "443"
  path: "/"

agify:
  protocol: "HTTPS"
  host: "api.agify.io"
  port: "443"
  path: "/"

httpbin:
  protocol: "HTTP"
  host: "httpbin.org"
  port: "80"
  path: "/get"

zippopotam:
  protocol: "HTTP"
  host: "api.zippopotam.us"
  port: "80"

coinmarketcap:
  protocol: "HTTPS"
  host: "pro-api.coinmarketcap.com"
  port: "443"
  path: "/public-api/v1/simple/price"
```

`api.agify.io` and `api.zippopotam.us` are the JSON hosts. `agify.io` and `zippopotam.us` in the brief are the product sites and must not be the HTTP requester hosts.

Requester `responseTimeout` is `5000`. Do not follow a caller-supplied host. Do not add the CoinMarketCap API-key header.

### 6.3 Listener pattern

Every listener sets `Content-Type: application/json` and `statusCode = vars.httpStatus default 200`. A shared `error-handler`:

| Error type | `httpStatus` | Body `message` |
|:-----------|:-------------|:---------------|
| `HTTP:NOT_FOUND` on the zippopotam requester | 404 | `Postal code was not found.` |
| Other `HTTP:NOT_FOUND` | 404 | `Upstream resource was not found.` |
| `HTTP:TIMEOUT`, `HTTP:CONNECTIVITY` | 504 | `Upstream request timed out.` |
| `HTTP:TOO_MANY_REQUESTS` or upstream status 429 | 429 | `Upstream rate limit was reached.` |
| `HTTP:BAD_REQUEST`, `HTTP:CLIENT_SECURITY`, `HTTP:FORBIDDEN`, `HTTP:METHOD_NOT_ALLOWED`, `HTTP:INTERNAL_SERVER_ERROR`, `HTTP:SERVICE_UNAVAILABLE` | 502 | `Upstream response was not usable.` |
| `VALIDATION:INVALID_BOOLEAN` or a choice-router `else` | 400 | `Integration request is invalid.` |

Error body is the `ErrorResponse` object from `PRD.md` §3. `provider` is a flow variable set at the start of each provider sub-flow.

Validate path and query **before** the requester, using the same accept/reject rules as §3. On failure set status 400 and the matching message; do not call upstream.

### 6.4 Flows in `public-api.xml`

| Flow | Listener | Then |
|:-----|:---------|:-----|
| `get-ipify` | `GET /providers/ipify` | Requester query `format` default `json`. When `text`, omit the query so IPify returns plain text. |
| `get-agify` | `GET /providers/agify` | Query `name`. Reject a missing name before the call. |
| `get-httpbin` | `GET /providers/httpbin` | `GET` the configured path. Send no client headers. |
| `get-zippopotam` | `GET /providers/zippopotam/{country}/{postalCode}` | Path `/{country}/{postalCode}` on the zippopotam host. |
| `get-coinmarketcap` | `GET /providers/coinmarketcap/price` | Query `ids` plus the include flags that are `true`. |
| `post-integrate` | `POST /integrate` | Read `provider`. Choice router to the same sub-flow logic as the specific flow. Respond with the envelope `{ provider, operation, data }`. |

Specific flows respond with `data` only (the task object). `post-integrate` wraps it.

IPify transform (object or raw text):

```dataweave
%dw 2.0
output application/json
---
{ ip: if (payload is Object) payload.ip else (payload as String) }
```

Agify transform. Keep a JSON `null` age:

```dataweave
%dw 2.0
output application/json
---
{
  name: payload.name,
  age: payload.age,
  count: payload.count
}
```

HTTPBin transform:

```dataweave
%dw 2.0
output application/json
---
{
  url: payload.url,
  origin: payload.origin,
  headers: payload.headers default {},
  args: payload.args default {}
}
```

Zippopotam transform:

```dataweave
%dw 2.0
output application/json
---
{
  postalCode: payload."post code",
  country: payload.country,
  countryAbbreviation: payload."country abbreviation",
  places: (payload.places default []) map (p) -> {
    placeName: p."place name",
    state: p.state,
    stateAbbreviation: p."state abbreviation",
    latitude: p.latitude,
    longitude: p.longitude
  }
}
```

CoinMarketCap transform. Include an optional field only when the matching query flag is `true` and the upstream value is present. Success requires `status.error_code` to be `0` or `"0"`; otherwise raise an error that the handler maps to 502.

```dataweave
%dw 2.0
output application/json
var rows = payload.data default []
---
{
  currency: "USD",
  prices: rows map (row) -> {
    id: row.id,
    price: row.price
  }
}
```

Extend each price object with `marketCap`, `volume24h`, `percentChange24h`, and `lastUpdated` under the flag rule above. Do not invent a price when `data` is empty: return `prices: []` when `error_code` is success.

### 6.5 One complete flow skeleton (copy this shape)

```xml
<flow name="get-ipify">
    <http:listener config-ref="listener-config" path="/providers/ipify" allowedMethods="GET">
        <http:response statusCode="#[vars.httpStatus default 200]">
            <http:headers>#[{'Content-Type': 'application/json'}]</http:headers>
        </http:response>
        <http:error-response statusCode="#[vars.httpStatus default 502]">
            <http:body>#[payload]</http:body>
            <http:headers>#[{'Content-Type': 'application/json'}]</http:headers>
        </http:error-response>
    </http:listener>
    <set-variable variableName="provider" value="ipify"/>
    <!-- validate format, call ipify-request, DataWeave, or route to the shared error handler -->
</flow>
```

The other four flows and `post-integrate` use this listener and error-response shape. HTTP request configs are one per upstream host, bound only to §6.2 properties.

## 7. Failure modes

| Risk | v1 control |
|:-----|:-----------|
| Caller supplies an upstream URL (SSRF) | No URL field. Provider enum. Hosts live only in `public-api.yaml`. |
| Path traversal in postal codes | `RequestRules` reject list, applied again in the zippopotam flow before the requester. |
| Secret leakage | Keyless CoinMarketCap. No key header. Do not log inbound `Authorization` or `Cookie`. |
| Edge bypasses Mule | Gate 4C scan of `public-api-edge/src/main`. |
| Whitelabel HTML on API errors | `GlobalExceptionHandler` covers the exception types in §5. |
| Stuck outbound call | 2s connect, 5s read, no retry. |
| CoinMarketCap id treated as a ticker | `ids` digit rule. Symbols are a re-plan. |

## 8. Runtime view

```
operator
   │  starts Mule on JDK 17 (:8081) and the Boot jar on JDK 21 (:8080)
   ▼
client ──HTTP──► public-api-edge ──HTTP──► mule-public-api ──HTTPS/HTTP──► public APIs
```

The Boot JVM is not an integration engine. Mule is not a Java 21 Spring process. Orchestration of the SDLC stays in these markdown files and the coding agent.
