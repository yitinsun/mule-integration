# Product Requirement Document (PRD): Public API Integration

## 1. Executive Summary & Objective

Build a local integration of five public APIs. **Spring Boot 3.x microservices** (Java 21) expose the tasks. **MuleSoft**, running as a local runtime (Anypoint Studio / 30-day trial is enough), is the integration layer and the only component that calls the public internet.

v1 is two local processes:

| Process | Role | Port |
|:--------|:-----|:-----|
| `public-api-edge` | Spring Boot edge. Specific tasks and the general operation. | **8080** |
| `mule-public-api` | Mule 4 integration app. | **8081** |

Source brief: `.notes/mule-project.docx`.

### 1.1 Requirement map (Gate 1)

| Brief | Binding section |
|:------|:----------------|
| MuleSoft and Spring Boot both participate | §3, `SYSTEM_DESIGN.md` §1 |
| MuleSoft may be the local free / 30-day trial runtime | `TECH_STACK.md` §1 |
| Spring Boot 3.x microservices | §2, `TECH_STACK.md` §2 |
| Java 21 | §4 (Boot). Mule’s own runtime stays on Java 17 because current Studio runtimes require it (`TECH_STACK.md` §1). |
| Every API has a specific usage and a general usage | §2.1 and §2.2 |
| IPify, Agify.io, HTTPBin, Zippopotam.us, CoinMarketCap Public | §2.3–§2.7 |

**Out of v1 (re-plan if requested later):** CloudHub and Runtime Fabric, more than one Spring Boot deployable, MySQL or any other database, Redis, authentication, a rate-limit store, Swagger, MUnit, IPv6 IPify, Agify `country_id`, HTTPBin methods other than `GET /get`, Zippopotam lookup-by-city, CoinMarketCap authenticated `/v1` or `/v2` routes, `symbol` lookup, and any sixth provider.

## 2. Functional Requirements

### 2.1 Two usages of every provider

**Specific usage** is a task-shaped endpoint. The path and the response fields match that one microservice task.

**General usage** is one operation for all five providers:

* **Endpoint:** `POST /api/v1/integrate`
* **Content-Type:** `application/json`. Anything else returns **415**.
* **Body:**

```json
{
  "provider": "agify",
  "operation": "predict-age",
  "params": { "name": "michael" }
}
```

`operation` may be omitted. It then defaults to the only operation for that provider. A mismatched operation returns **400**.

| `provider` | Default `operation` | Required `params` | Optional `params` |
|:-----------|:--------------------|:------------------|:------------------|
| `ipify` | `public-ip` | none | `format`: `json` or `text` |
| `agify` | `predict-age` | `name` | none |
| `httpbin` | `echo-get` | none | none |
| `zippopotam` | `lookup-postal` | `country`, `postalCode` | none |
| `coinmarketcap` | `simple-price` | `ids` | `includeMarketCap`, `includeVolume24h`, `includePercentChange24h`, `includeLastUpdated` |

Provider ids are lowercase exact matches. Unknown providers, unknown param keys, and nested param objects return **400**.

**Success (`200`):**

```json
{
  "provider": "agify",
  "operation": "predict-age",
  "data": { "name": "michael", "age": 69, "count": 298219 }
}
```

`data` is the same object the specific endpoint returns for that call. IPify’s general `data` is always `{ "ip": "<address>" }`, including when `format` is `text`.

### 2.2 Shared client rules

* Unknown query parameters on a specific endpoint return **400**.
* The edge validates every request **before** it calls Mule. Mule applies the same rules again before it builds an upstream URL.
* Clients cannot pass a URL. Hosts are fixed in the Mule app. This is the SSRF control.
* The edge does not forward the caller’s `Authorization` or `Cookie` header.
* IPify and HTTPBin report the **Mule process egress** address, because Mule places the outbound call.

### 2.3 IPify — public IP

* **Specific:** `GET /api/v1/ip`
* **Optional query:** `format=json` (default) or `format=text`
* **Upstream:** `GET https://api.ipify.org/?format=json` or, for `text`, `GET https://api.ipify.org/` (plain body). The brief’s sample `https://api.ipify.org?format=json` is this API.
* **`format=json` (`200`, `application/json`):**

```json
{ "ip": "203.0.113.10" }
```

* **`format=text` (`200`, `text/plain`):** the address and nothing else. No JSON wrapper.
* A missing `ip` in the upstream body returns **502**.

### 2.4 Agify.io — age from a name

* **Specific:** `GET /api/v1/age?name=michael`
* **Upstream:** `GET https://api.agify.io/?name=michael`. The brief’s sample `https://agify.io` is the product site; the JSON API used here is `api.agify.io`.
* `name` is required, trimmed, 1–64 characters, and must match `^[\p{L}][\p{L} .'\-]{0,63}$`.
* **Success (`200`):**

```json
{ "name": "michael", "age": 69, "count": 298219 }
```

`age` is a JSON number or `null` when Agify has no prediction. `count` is a non-negative integer. `null` age is still **200**.

### 2.5 HTTPBin — echo

* **Specific:** `GET /api/v1/echo`
* **Upstream:** `GET http://httpbin.org/get` (the brief’s sample).
* **Success (`200`):**

```json
{
  "url": "http://httpbin.org/get",
  "origin": "203.0.113.10",
  "headers": { "Host": "httpbin.org" },
  "args": {}
}
```

Project only `url`, `origin`, `headers`, and `args`. Header values are strings.

### 2.6 Zippopotam.us — postal code

* **Specific:** `GET /api/v1/postal/{country}/{postalCode}`
* **Example:** `GET /api/v1/postal/us/90210`
* **Upstream:** `GET http://api.zippopotam.us/us/90210`. The brief’s sample `http://zippopotam.us` is the product site; the JSON API host is `api.zippopotam.us`.
* `country` is two ASCII letters and is sent lowercase. `postalCode` is 1–20 characters matching `^[A-Za-z0-9][A-Za-z0-9 -]{0,19}$` (covers `90210`, `A0A`, `01000-000`, `100 00`). `/`, `\`, `.`, and `%` are rejected with **400**.
* **Success (`200`):**

```json
{
  "postalCode": "90210",
  "country": "United States",
  "countryAbbreviation": "US",
  "places": [
    {
      "placeName": "Beverly Hills",
      "state": "California",
      "stateAbbreviation": "CA",
      "latitude": "34.0901",
      "longitude": "-118.4065"
    }
  ]
}
```

Latitude and longitude stay strings, as the upstream sends them. An upstream **404** (unknown code) returns **404**.

### 2.7 CoinMarketCap Public — simple price

* **Specific:** `GET /api/v1/crypto/price?ids=1,1027`
* **Upstream:** `GET https://pro-api.coinmarketcap.com/public-api/v1/simple/price?ids=1,1027` (the brief’s sample). This is the **keyless** route. Do not send `X-CMC_PRO_API_KEY`.
* `ids` is required: 1 to 50 comma-separated unsigned integers, no spaces, each 1–10 digits. `BTC` is **400** on this v1 route. Symbol and `convert` are out of v1; prices are USD spot.
* Optional flags, each `true` or `false`: `includeMarketCap`, `includeVolume24h`, `includePercentChange24h`, `includeLastUpdated`. They map to the upstream flags `include_market_cap`, `include_volume_24h`, `include_percent_change_24h`, `include_last_updated`. Omitted means `false`.
* **Success (`200`):**

```json
{
  "currency": "USD",
  "prices": [
    { "id": 1, "price": 65432.12 }
  ]
}
```

When a flag was `true` and the upstream sent the field, include `marketCap`, `volume24h`, `percentChange24h`, and/or `lastUpdated` on that price object. Leave them out when they were not requested. Treat upstream `status.error_code` of `0` or `"0"` as success. Any other error code returns **502**.

The upstream cache window is about 60 seconds. v1 does not add its own cache.

## 3. Error contract

Every JSON error uses:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Query parameter name is required.",
  "provider": "agify"
}
```

`provider` is `null` when the request has no provider yet (malformed general body, or a specific call that failed before the provider was known — in practice specific handlers always set it).

| Situation | HTTP | `error` | `message` |
|:----------|:-----|:--------|:----------|
| Missing `name` | 400 | Bad Request | `Query parameter name is required.` |
| Name fails the pattern or length rule | 400 | Bad Request | `Query parameter name is invalid.` |
| Bad `format` | 400 | Bad Request | `Query parameter format must be json or text.` |
| Bad `country` or `postalCode` | 400 | Bad Request | `Postal path is invalid.` |
| Missing or invalid `ids` | 400 | Bad Request | `Query parameter ids must be 1 to 50 numeric CoinMarketCap ids.` |
| Invalid include flag | 400 | Bad Request | `Include flag must be true or false.` |
| Unknown query parameter | 400 | Bad Request | `Unknown query parameter.` |
| Unknown provider, operation, or param key; extra nesting; empty body | 400 | Bad Request | `Integration request is invalid.` |
| Wrong POST content type | 415 | Unsupported Media Type | `Content-Type must be application/json.` |
| Zippopotam upstream 404 | 404 | Not Found | `Postal code was not found.` |
| Any other upstream 404 | 404 | Not Found | `Upstream resource was not found.` |
| Upstream 429 | 429 | Too Many Requests | `Upstream rate limit was reached.` |
| Upstream 5xx, unreadable body, or CoinMarketCap `error_code` other than 0 | 502 | Bad Gateway | `Upstream response was not usable.` |
| Mule connection refused or no listener | 503 | Service Unavailable | `Integration layer is unavailable.` |
| Upstream or Mule timeout (connect budget 2s, read budget 5s) | 504 | Gateway Timeout | `Upstream request timed out.` |

## 4. Technical grounding

* **Edge runtime:** Java 21, Spring Boot 3.3.x, Maven. Details and the only allowed dependencies: `TECH_STACK.md`.
* **Integration runtime:** local Mule 4 on Java 17. v1 does not deploy to CloudHub.
* **Persistence:** none.
* **Encoding:** UTF-8 JSON except IPify `format=text`.
* Design, flow list, and DataWeave projections: `SYSTEM_DESIGN.md`.
