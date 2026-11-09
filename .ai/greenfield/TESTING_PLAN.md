# Automated Test Verification Suite Plan

Mandatory tests and the agent loop for Gate 4. The Boot suite must pass `mvn -f public-api-edge/pom.xml clean test` on **JDK 21** at **100%** success and JaCoCo **line coverage >= 80%**. Never use `@Ignore` or `@Disabled`.

Coverage is enforced by the JaCoCo plugin in `TECH_STACK.md`. Do not type a coverage percentage into `MULE_INTEGRATION_SUMMARY.md` until `public-api-edge/target/site/jacoco/jacoco.csv` exists.

Tests must not open sockets to IPify, Agify, HTTPBin, Zippopotam, or CoinMarketCap. They stub Mule with `MockRestServiceServer` and read bodies from `src/test/resources/fixtures/`.

Fixture files (commit them with the tests):

| File | Role |
|:-----|:-----|
| `fixtures/ipify.json` | `{ "ip": "203.0.113.10" }` |
| `fixtures/agify.json` | name `michael`, age `69`, count `298219` |
| `fixtures/agify-null-age.json` | same name, `age` JSON `null`, count present |
| `fixtures/httpbin.json` | `url`, `origin`, `headers`, `args` as in `PRD.md` §2.5 |
| `fixtures/zippopotam-90210.json` | Beverly Hills object from `PRD.md` §2.6 |
| `fixtures/cmc-price.json` | `currency` `USD`, one price `id` `1` |
| `fixtures/cmc-error.json` | a body whose `status.error_code` is not 0, for the Mule contract example only. The Boot client sees Mule’s **502** `ErrorResponse`, not this raw body, unless a test is explicitly about a 2xx unreadable payload. |

MUnit is out of v1. Mule flow behavior is locked by `SYSTEM_DESIGN.md` and reviewed in Gate 4C. It is not a JaCoCo target.

## 1. Unit tests (`RequestRulesTest.java`)

No Spring context. Class `com.yitinsun.muleintegration.domain.RequestRulesTest`.

| Case | Expect |
|:-----|:-------|
| `name("michael")` | returns `michael` |
| `name(null)` and `name("  ")` | message `Query parameter name is required.` |
| `name` longer than 64 or starting with a digit | message `Query parameter name is invalid.` |
| `format(null)` | `json` |
| `format("text")` | `text` |
| `format("xml")` | message `Query parameter format must be json or text.` |
| `country("US")` | `us` |
| `country("USA")` | message `Postal path is invalid.` |
| `postalCode("90210")`, `postalCode("A0A")`, `postalCode("100 00")` | accepted unchanged aside from no trim of an internal space |
| `postalCode("../etc")` and `postalCode("90/210")` | message `Postal path is invalid.` |
| `coinIds("1,1027")` | `[1, 1027]` |
| `coinIds("BTC")`, `coinIds("1, 2")`, 51 ids | message `Query parameter ids must be 1 to 50 numeric CoinMarketCap ids.` |
| `flag(null)` | `false` |
| `flag("yes")` | message `Include flag must be true or false.` |
| `provider("Agify")` | message `Integration request is invalid.` |
| `provider("ipify")` | `IPIFY` |
| `operation(AGIFY, null)` | `predict-age` |
| `operation(AGIFY, "echo-get")` | message `Integration request is invalid.` |
| params map with key `url` | message `Integration request is invalid.` |
| `rejectUnknownQuery` with key `extra` | message `Unknown query parameter.` |

## 2. Client tests (`MuleClientTest.java`)

Spring test slice or a `@SpringBootTest` with `@ActiveProfiles("test")` and `MockRestServiceServer` bound to the application `RestClient.Builder`. No Mockito stand-in for `MuleClient`.

| Case | Expect |
|:-----|:-------|
| IP lookup | `GET` path `/providers/ipify` and query `format=json`; body matches `IpResponse.ip` |
| Text format still asks Mule for `format=text` | query `format=text`; JSON body still `{ "ip" }` |
| Age lookup | query `name` is URL-encoded (`mary anne` → `mary%20anne` or `mary+anne`) |
| Postal lookup | path `/providers/zippopotam/us/90210` |
| Price lookup with `includeMarketCap=true` | query contains `ids=1` and `includeMarketCap=true` |
| Integrate | `POST /integrate` with the JSON body from `PRD.md` §2.1 |
| Connection failure from the mock server | `UpstreamException` status **503**, message `Integration layer is unavailable.` |
| Mock responds **404** with the postal error JSON | status **404**, message `Postal code was not found.` |
| Mock responds **504** | status **504**, message `Upstream request timed out.` |
| Mock responds **200** with `{` | status **502**, message `Upstream response was not usable.` |

## 3. Web integration tests (`PublicApiEdgeIT.java`)

`@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`. Real controllers, real `RequestRules`, real `MuleClient`, stubbed Mule HTTP. Seed responses from the fixture files.

Assert error JSON fields `status`, `error`, `message`, and `provider`.

### 3.1 Specific routes

| Call | Stub | Assert |
|:-----|:-----|:-------|
| `GET /api/v1/ip` | `ipify.json` | **200** `application/json`, `ip` is `203.0.113.10` |
| `GET /api/v1/ip?format=text` | `ipify.json` | **200** `text/plain`, body `203.0.113.10` |
| `GET /api/v1/ip?format=xml` | none (must not call Mule) | **400** format message |
| `GET /api/v1/age` | none | **400** `Query parameter name is required.`, `provider` `agify` |
| `GET /api/v1/age?name=michael` | `agify.json` | **200** age `69`, count `298219` |
| `GET /api/v1/age?name=nobody` | `agify-null-age.json` | **200** and JSON `age` is `null` |
| `GET /api/v1/echo` | `httpbin.json` | **200** and `origin` present |
| `GET /api/v1/postal/us/90210` | `zippopotam-90210.json` | **200** `places[0].placeName` `Beverly Hills` |
| `GET /api/v1/postal/us/00000` | Mule **404** postal message | **404** `Postal code was not found.` |
| `GET /api/v1/postal/usa/90210` | none | **400** `Postal path is invalid.` |
| `GET /api/v1/crypto/price` | none | **400** ids message, `provider` `coinmarketcap` |
| `GET /api/v1/crypto/price?ids=BTC` | none | **400** ids message |
| `GET /api/v1/crypto/price?ids=1` | `cmc-price.json` | **200** `currency` `USD`, `prices[0].id` `1` |
| `GET /api/v1/crypto/price?ids=1` when Mule returns **502** | error JSON | **502** `Upstream response was not usable.` |
| `GET /api/v1/ip` when the stub drops the connection | — | **503** `Integration layer is unavailable.` |

### 3.2 General route

| Call | Assert |
|:-----|:-------|
| `POST /api/v1/integrate` with provider `agify`, name `michael`, stub `agify.json` inside `data` | **200** `operation` `predict-age`, `data.name` `michael` |
| `POST` with provider `nope` | **400** `Integration request is invalid.` and Mule is not called |
| `POST` zippopotam without `postalCode` | **400** and Mule is not called |
| `POST` with `Content-Type: text/plain` | **415** `Content-Type must be application/json.` |
| `POST` `{}` | **400** `Integration request is invalid.` |

## 4. Gate 4C review (same tree, not a JUnit case)

Before Stage 6, confirm all of the following. A miss is a compliance failure, not a coverage waiver.

* `public-api-edge/src/main` has no `api.ipify.org`, `agify.io`, `httpbin.org`, `zippopotam.us`, `coinmarketcap.com`, or `X-CMC_PRO_API_KEY`.
* No `schema.sql`, JDBC URL, or Spring Data repository.
* No `@Disabled` or `@Ignore`.
* `mule-public-api` properties match `SYSTEM_DESIGN.md` §6.2, and each provider flow from §6.4 exists.
* The CoinMarketCap requester does not set an API-key header.

## 5. Automated agent workflow cycle

JaCoCo `check` runs as part of the Boot `mvn clean test`. Do not skip those tests except inside the Docker **image build** of a jar that already passed Gate 4.

```
[ Compile on JDK 21 ] ──> [ mvn -f public-api-edge/pom.xml clean test ]
                                │
               ┌────────────────┴────────────────┐
               ▼                                 ▼
[ All tests pass + coverage ]          [ Failures or coverage < 80% ]
               │                                 │
               ▼                                 ▼
[ Gate 5: container phase ]            [ Read stack trace ]
         (Stage 6)                               │
                                                 ▼
                                        [ Patch failing class ]
                                                 │
                                                 ▼
                                        [ Retry; max 3 times ]
                                                 │
                                  exhausted ──> [ Safe-Stop / rollback to Gate 2 ]
```

This loop **is** the Bounded Retry in `ORCHESTRATION_MANIFEST.md`. Passing tests do **not** rewrite classes. Gate 4C still has to pass before Stage 6.
