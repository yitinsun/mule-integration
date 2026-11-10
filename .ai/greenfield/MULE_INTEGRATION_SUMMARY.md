# Public API integration — status

Measured on 2026-11-10. Figures below come from commands run in this workspace.

| Item | State |
|:-----|:------|
| Gate 2 | `GATE2_APPROVAL.txt` contains `[APPROVED: yitin]`. |
| `public-api-edge` | Spring Boot 3.3.5 module. `pom.xml` includes `springdoc-openapi-starter-webmvc-ui` 2.6.0. Latest command was `mvn -f public-api-edge/pom.xml clean test` (JDK 21.0.12.1, Maven 3.9.15), which deletes `target` and does not repackage the boot jar. |
| `mule-public-api` | Flows, `public-api.yaml`, and `pom.xml` are present (runtime 4.13.0, Mule Maven Plugin 4.10.3, HTTP connector 1.12.2, `minMuleVersion` 4.9.0). `mvn -f mule-public-api/pom.xml clean install` on JDK 17.0.1 with Studio’s Maven **3.9.15** **succeeded**. Jar: `mule-public-api/target/mule-public-api-0.1.0-SNAPSHOT-mule-application.jar`. The earlier failure on Maven 3.6.3 (`NoSuchMethodError` `setBlocked`) does not apply to this Maven. |
| Gate 4 command | `mvn -f public-api-edge/pom.xml clean test` on JDK 21.0.12.1. **BUILD SUCCESS.** Tests run: **44**. Failures: **0**. Errors: **0**. Skipped: **0**. |
| JaCoCo line coverage | From `public-api-edge/target/site/jacoco/jacoco.csv` after that test: **300** lines covered, **38** missed, **338** total, **88.76%**. The JaCoCo check (minimum 0.80) passed. `OpenApiConfig` is 11/11 lines. |
| API catalog | Edge only. Dev default serves `/v3/api-docs` and `/swagger-ui.html`. The `prod` profile leaves both off unless `SPRINGDOC_ENABLED` or `SPRINGDOC_SWAGGER_UI_ENABLED` is set. Swagger UI was not opened in a browser. Mule was not changed. |
| Gate 4C | `public-api-edge/src/main` has no upstream host names and no `X-CMC_PRO_API_KEY`. No `@Disabled` or `@Ignore`. Mule listener flows `get-ipify`, `get-agify`, `get-httpbin`, `get-zippopotam`, `get-coinmarketcap`, and `post-integrate` are in `public-api.xml`. Hosts match `SYSTEM_DESIGN.md` §6.2. |
| Docker image | `public-api-edge/Dockerfile` and `docker-compose.yml` exist. `docker` is not on PATH, so the image was **not** built. |
| Mule local runtime | Not started. |
