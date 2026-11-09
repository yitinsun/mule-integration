# File map — spec → code

Worked example paths are this repo. For a new app, keep the **roles**; change product names.

## Greenfield (first build)

| Spec | Typical outputs |
|:-----|:----------------|
| `PRD.md` | Controllers, DTOs (`record`), error JSON, HTTP codes |
| `SYSTEM_DESIGN.md` | `schema.sql`, JPA entity, service write/read path, Base62 (or whatever id rule the design names) |
| `TECH_STACK.md` | `pom.xml`, `application.yml` profiles, JaCoCo plugin |
| `TESTING_PLAN.md` | `*Test.java`, `*IT.java`, Surefire includes |
| `ORCHESTRATION_MANIFEST.md` | Stage order, Gate 2 file, retry cap — not Java packages |
| `INSTRUCTIONS.md` | `SDLC_AUDIT_LOG.json`, `SDLC_METRICS.json` |
| `AGENTS.md` | Refusal rules (no src before Gate 2) |

## Brownfield (this repo)

| Playbook | Modules / routes |
|:---------|:-----------------|
| `ADD_SWAGGER.md` | springdoc, `OpenApiConfig`, `/swagger-ui.html`, `/v3/api-docs` |
| `REFACTOR.md` | `GET /short/{code}`, allowlist filter, HTML 404 |
| `ENHANCE.md` | `SecurityConfig` permitAll demo, jsoup filter |
| `REFINE.md` | README, scenarios, actuator `health,info`, honest summary — not a new domain |

## Evidence screenshots (not specs)

`.assets/*.JPG` — Swagger UI, POST shorten, GET short, JUnit. Do not generate code from images.
