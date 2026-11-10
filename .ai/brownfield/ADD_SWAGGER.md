# Swagger UI (springdoc) — deferred enhancement

Standalone playbook for adding **OpenAPI 3** docs and **Swagger UI** to the Spring Boot edge. Swagger does not change business behavior: it publishes a browsable description of HTTP endpoints that already exist and lets you Try-it-out.

**Allowed on the edge.** A human re-plan named `springdoc-openapi-starter-webmvc-ui` **2.6.0** in `PRD.md`, `SYSTEM_DESIGN.md`, and `TECH_STACK.md`. Apply the steps below to `public-api-edge` only. Do not add APIkit, MUnit, or springdoc to `mule-public-api`. Mule listener paths (`/providers/**`, `POST /integrate`) are not this catalog.

**Library (after the re-plan):** `springdoc-openapi-starter-webmvc-ui` (Jakarta / OpenAPI 3). Do not use Springfox or `io.swagger.annotations` (Swagger 2). Do not add `springdoc-openapi-starter-webflux-ui`.

**This app**

| Item | Value |
|:-----|:------|
| Module | `public-api-edge` only |
| Spring Boot | 3.3.5 |
| Java | 21 |
| Root package | `com.yitinsun.muleintegration` |
| Controllers | `com.yitinsun.muleintegration.web` |
| DTO style | Java `record`s in `...web.dto` |
| Auth | none. Do not add Spring Security or JWT to unlock the UI |
| Upstream calls | still only `MULE_BASE_URL`. Swagger must not call IPify, Agify, HTTPBin, Zippopotam, or CoinMarketCap |

Default after enablement, with the edge on port 8080:

- UI: `http://localhost:8080/swagger-ui.html`
- Spec: `http://localhost:8080/v3/api-docs` and `/v3/api-docs.yaml`

There is no `app.base-url` property. The OpenAPI server entry is `http://localhost:${server.port}` with default port 8080.

---

## 1. Maven (`public-api-edge/pom.xml`)

Add a single dependency after the re-plan names it in `TECH_STACK.md`. Pin only the starter.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

2.6.x matches Spring Boot 3.3. One starter serves both the JSON spec and the UI.

Check: `mvn -f public-api-edge/pom.xml -q dependency:tree -Dincludes=org.springdoc`

---

## 2. YAML (`public-api-edge/src/main/resources/application.yml`)

Append a shared `springdoc` block. Toggle with environment variables. Do not put tokens or passwords here.

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: ${SPRINGDOC_ENABLED:true}
  swagger-ui:
    path: /swagger-ui.html
    enabled: ${SPRINGDOC_SWAGGER_UI_ENABLED:true}
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
    try-it-out-enabled: true
    persist-authorization: false
  show-actuator: false
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
  packages-to-scan: com.yitinsun.muleintegration.web
  paths-to-match:
    - /api/v1/**
```

`paths-to-match` keeps the catalog on the edge API. Mule is a different process on port 8081 and has no Swagger UI.

The `prod` profile used by `public-api-edge/Dockerfile` should leave the UI off unless an env var turns it on:

```yaml
---
spring:
  config:
    activate:
      on-profile: prod
springdoc:
  api-docs:
    enabled: ${SPRINGDOC_ENABLED:false}
  swagger-ui:
    enabled: ${SPRINGDOC_SWAGGER_UI_ENABLED:false}
```

`/swagger-ui.html` redirects to `/swagger-ui/index.html`. Actuator is omitted (`show-actuator: false`).

---

## 3. General API metadata (`OpenAPI` bean)

YAML does not set title, description, or server URL. Add a configuration class. This bean is documentation only. It does not register filters or change controller mappings.

```java
package com.yitinsun.muleintegration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final int serverPort;

    public OpenApiConfig(@Value("${server.port:8080}") int serverPort) {
        this.serverPort = serverPort;
    }

    @Bean
    public OpenAPI publicApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Public API edge")
                        .description(
                                "Specific tasks and POST /api/v1/integrate. "
                                        + "The edge calls local Mule only. "
                                        + "Mule calls IPify, Agify, HTTPBin, Zippopotam, and CoinMarketCap.")
                        .version("0.1.0"))
                .addServersItem(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local edge"));
    }
}
```

---

## 4. Annotating controllers and models

Use `io.swagger.v3.oas.annotations.*`. Annotations must not change paths, status codes, query rules, or validation. Do not add `@Hidden` to a real edge route. Every route below is part of the product.

| Controller | Method and path | Success |
|:-----------|:----------------|:--------|
| `IpController` | `GET /api/v1/ip` | 200 JSON `{ "ip" }`, or 200 `text/plain` when `format=text` |
| `AgeController` | `GET /api/v1/age` | 200 `AgeResponse`. `age` may be null |
| `EchoController` | `GET /api/v1/echo` | 200 `EchoResponse` |
| `PostalController` | `GET /api/v1/postal/{country}/{postalCode}` | 200 `PostalResponse` |
| `CryptoPriceController` | `GET /api/v1/crypto/price` | 200 `CryptoPriceResponse` |
| `IntegrateController` | `POST /api/v1/integrate` | 200 `IntegrateResponse`. Requires `Content-Type: application/json` |

Document the JSON error body as `ErrorResponse` (`status`, `error`, `message`, `provider`). Status codes that already exist: 400, 404, 415, 429, 502, 503, 504. Exact `message` strings stay the constants in `RequestRules` and `PRD.md` §3. Do not invent new messages for the UI.

Example for the general operation. The other controllers get the same `@Tag` / `@Operation` / `@ApiResponse` treatment with their own records.

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Integrate", description = "One operation for all five providers")
public class IntegrateController {

    @Operation(summary = "Call a provider through Mule")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = IntegrateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid provider, operation, or params",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type must be application/json",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/api/v1/integrate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public IntegrateResponse integrate(@RequestBody IntegrateRequest body, HttpServletRequest request) {
        // existing method body
    }
}
```

`GET /api/v1/ip?format=text` is the same mapping as JSON and returns `text/plain`. Say that in the operation description. Leave the default produces as `application/json`.

`@Schema` on records is documentation only. Do not add Bean Validation annotations that `RequestRules` does not already enforce. Annotate `IntegrateRequest`, `IntegrateResponse`, `IpResponse`, `AgeResponse`, `EchoResponse`, `PostalResponse`, `Place`, `CryptoPrice`, `CryptoPriceResponse`, and `ErrorResponse`.

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IntegrateRequest", description = "General provider call")
public record IntegrateRequest(
        @Schema(description = "ipify, agify, httpbin, zippopotam, or coinmarketcap",
                example = "agify", requiredMode = Schema.RequiredMode.REQUIRED)
        String provider,
        @Schema(description = "Omit to use the provider default operation", example = "predict-age")
        String operation,
        @Schema(description = "Flat string or scalar params for that provider")
        Map<String, Object> params
) {}
```

---

## 5. Spring Security

This edge has no Spring Security. Do not add `spring-boot-starter-security`, permit-all matchers, or a JWT scheme so that Swagger can load. Sections 5 and 6 of the url-shortener playbook do not apply.

---

## 6. Smoke check

1. Start Mule on port 8081, then the edge on JDK 21 and port 8080. Try-it-out calls Mule, and Mule calls the public APIs.
2. Open `http://localhost:8080/swagger-ui.html`.
3. Header shows **Public API edge** `0.1.0`.
4. Catalog lists the six `/api/v1/**` operations and no `/providers/**` path.
5. `GET http://localhost:8080/v3/api-docs` is OpenAPI 3 JSON.
6. `mvn -f public-api-edge/pom.xml clean test` on JDK 21 still passes, with JaCoCo line coverage at least 80%. No `@Disabled` or `@Ignore`.

If the UI is blank: the starter must be on the runtime classpath (not `test` scope), and `springdoc.swagger-ui.enabled` must be true for that profile. The `prod` profile defaults it to off.

If Try-it-out returns 503, Mule is not listening on `MULE_BASE_URL`. That is not a Swagger defect.

---

## 7. What this enhancement is not

- Not a new product API, host, or CoinMarketCap API key
- Not a Mule APIkit or MUnit project
- Not a replacement for `TESTING_PLAN.md`
- Not Springfox, Gateway, or WebFlux docs
- Not permission to call the public APIs from Spring

---

## 8. Touch list

| Location | Change |
|:---------|:-------|
| `PRD.md`, `SYSTEM_DESIGN.md`, `TECH_STACK.md` | Name springdoc 2.6.0 and drop the Swagger prohibition |
| `public-api-edge/pom.xml` | springdoc WebMVC UI starter 2.6.0 |
| `public-api-edge/src/main/resources/application.yml` | `springdoc` block; `prod` disabled unless env enables it |
| `...config.OpenApiConfig` | `OpenAPI` `@Bean` |
| Six controllers under `...web` | `@Tag` / `@Operation` / `@ApiResponse` |
| DTO `record`s | `@Schema` |
| `mule-public-api/**` | no change |

---

## 9. File name

This playbook is `.ai/brownfield/ADD_SWAGGER.md`. `.ai/greenfield/` now allows this starter on the edge.
