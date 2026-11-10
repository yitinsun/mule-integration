# Technology Stack Profile: Public API Integration

This document is the **only** allowed toolchain for v1. Packages not listed here are high-impact (see `ORCHESTRATION_MANIFEST.md` §5.1).

**Explicitly out of v1 (do not add):** MySQL, H2, Spring Data, Flyway/Liquibase, Redis, Spring Security, Resilience4j, Spring WebFlux, RestTemplate, OpenFeign, MUnit, APIkit, a CoinMarketCap API-key connector.

**Allowed API catalog (edge only):** `org.springdoc:springdoc-openapi-starter-webmvc-ui` **2.6.0**. Do not add the WebFlux starter, Springfox, or springdoc on `mule-public-api`.

Two builds. They do not share a parent POM and they do not share a JDK target.

## 1. Core Tooling Versions

### Spring edge

* **Language:** Java 21 LTS (`maven.compiler.release` 21)
* **Framework:** Spring Boot parent **3.3.5**
* **Build:** Apache Maven 3.9+
* **Coverage:** JaCoCo Maven plugin **0.8.12**, minimum **80% line coverage**, enforced on `mvn -f public-api-edge/pom.xml clean test`
* **HTTP client:** Spring Framework `RestClient` (already on `spring-boot-starter-web`). No extra HTTP library.

### Mule integration layer

* **IDE / trial:** Anypoint Studio is the local runtime. Studio 7.22+ runs Mule on **Java 17**. That is a platform constraint of the runtime, separate from the Java 21 edge. A 30-day Anypoint trial is sufficient. v1 does not need CloudHub.
* **Runtime:** the Mule 4 version bundled with the installed Studio, **minimum 4.9.0**. Studio 7.29.0 bundles runtime **4.13.0**, DataWeave 2.13, and Mule Maven Plugin **4.10.3**. Use that pair when that Studio is what is installed. Record the actual pair in `SDLC_AUDIT_LOG.json`.
* **Connectors:** HTTP connector only, classifier `mule-plugin`, at the version Studio binds for that runtime.
* **JDK for `mvn -f mule-public-api/pom.xml package`:** 17. Do not target 21.

## 2. Edge dependency suite (`public-api-edge/pom.xml`)

Coordinates: `groupId` `com.yitinsun`, `artifactId` `public-api-edge`, `version` `0.1.0-SNAPSHOT`.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
</parent>

<properties>
    <java.version>21</java.version>
    <jacoco.version>0.8.12</jacoco.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.6.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Do not pin starter versions separately. Do not add Lombok.

Configure `jacoco-maven-plugin` so `mvn clean test` runs `prepare-agent` and a `check` rule: **LINE covered ratio ≥ 0.80**. No class excludes. Surefire must keep the default includes so `*Test.java` and `*IT.java` both run. Do not set an include that drops `*IT`.

## 3. Mule dependency suite (`mule-public-api/pom.xml`)

Coordinates: `groupId` `com.yitinsun`, `artifactId` `mule-public-api`, `version` `0.1.0-SNAPSHOT`, `packaging` `mule-application`.

Allowed build plugin: `org.mule.tools.maven:mule-maven-plugin` at the Studio-bundled version (4.10.3 with Studio 7.29.0).

Allowed dependency: `org.mule.connectors:mule-http-connector` with `<classifier>mule-plugin</classifier>` and the version Studio selects for the runtime. Set `<app.runtime>` to that same runtime (`4.13.0` or the installed 4.9+ runtime).

No Spring dependency in this POM. No database driver.

## 4. Configuration (`public-api-edge/src/main/resources/application.yml`)

No secrets. Defaults are local.

```yaml
spring:
  application:
    name: public-api-edge
  profiles:
    active: dev

server:
  port: ${SERVER_PORT:8080}

mule:
  base-url: ${MULE_BASE_URL:http://localhost:8081}
  connect-timeout-ms: ${MULE_CONNECT_TIMEOUT_MS:2000}
  read-timeout-ms: ${MULE_READ_TIMEOUT_MS:5000}

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

---
spring:
  config:
    activate:
      on-profile: dev

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

---
spring:
  config:
    activate:
      on-profile: test

mule:
  base-url: ${MULE_BASE_URL:http://localhost:8081}
```

`MuleProperties` is a `@ConfigurationProperties(prefix = "mule")` type with `baseUrl`, `connectTimeoutMs`, and `readTimeoutMs`. A record is allowed. `@EnableConfigurationProperties` registers it. Integration tests use `@ActiveProfiles("test")`.

Bind `RestClient` from `RestClient.Builder` so `MockRestServiceServer.bindTo(builder)` sees the same client the application uses (`SYSTEM_DESIGN.md` §5).

## 5. Stage 6 containers (do not build them before Gate 4)

* `Dockerfile` uses an Eclipse Temurin **21** JRE, copies the Boot jar, and starts `java -jar` with `--spring.profiles.active=prod`.
* `docker-compose.yml` publishes **8080** and passes `MULE_BASE_URL`. It does not include a Mule service image.
* The image build may skip tests only because Gate 4 already ran them: `-DskipTests` is allowed on the image build and forbidden as the way to go green on Gate 4.

## 6. Coding standards

* **API types:** request and response bodies are the `record`s in `SYSTEM_DESIGN.md` §4. `@JsonInclude(NON_NULL)` is allowed on `CryptoPrice` only.
* **Logging:** SLF4J via Spring’s `LoggerFactory`. Log status codes and provider ids. Do not log inbound `Authorization`, `Cookie`, or full upstream header maps at info.
* **Controllers:** one class per specific task plus `IntegrateController`, as named in `SYSTEM_DESIGN.md` §2. They call `RequestRules` then `MuleClient`.
* **Java HTTP:** `RestClient` only.
