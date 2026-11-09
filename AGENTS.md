# Agent governance (mule-integration)

This repo is a gated SDLC for a **public-API integration**: Spring Boot microservices in front of a local MuleSoft integration layer. Specs in `.ai/greenfield/` are **binding**. Do not invent APIs, dependencies, upstream hosts, or coverage numbers.

The reusable playbook copied from the url-shortener blueprint lives in `.assets/sop/spring-boot-agentic-sdlc/`. How to use it: `.assets/HOW_TO_USE.md`. Cursor skill loader: `.cursor/skills/spring-boot-agentic-sdlc/SKILL.md`.

That playbook’s url-shortener routes, `url_mappings` table, Base62 encoder, and `AUTO_INCREMENT=56800235` seed are a **worked example**. They are not this product. If a playbook example and `.ai/greenfield/` disagree, `.ai/greenfield/` wins.

## Before any work

1. Read `.ai/greenfield/INSTRUCTIONS.md` and `.ai/greenfield/ORCHESTRATION_MANIFEST.md`.
2. Use the other specs only for their role:
   - Product: `.ai/greenfield/PRD.md`
   - Design: `.ai/greenfield/SYSTEM_DESIGN.md`
   - Stack: `.ai/greenfield/TECH_STACK.md`
   - Tests: `.ai/greenfield/TESTING_PLAN.md`
   - File map: `.ai/greenfield/ORCHESTRATION_ARCHITECTURE.md`
   - Status: `.ai/greenfield/MULE_INTEGRATION_SUMMARY.md` (update only with real artifacts and measured metrics)

Source requirements: `.notes/mule-project.docx`.

## Gate 2 lock (Stage 3)

Do **not** create or edit `pom.xml`, `public-api-edge/**`, `mule-public-api/**`, `src/**`, or other functional Java / Mule sources until the workspace root contains `GATE2_APPROVAL.txt` with a line `[APPROVED: HUMAN_ENGINEER_ID]`.

Allowed before approval: `.ai/**`, `.assets/**`, `.cursor/**`, `AGENTS.md`, `README.md`, `SDLC_AUDIT_LOG.json`, `SDLC_METRICS.json`.

If the approval file is missing, refuse to implement and tell the operator to write it (and optionally `git tag gate2-approved`).

## After approval

- Stage 3, all three before tests:
  - **3A** `mule-public-api/` from `SYSTEM_DESIGN.md` (listener, five provider flows, general router).
  - **3B** provider catalog, `RequestRules`, and response records.
  - **3C** Spring Boot module (`public-api-edge/`): `pom.xml`, `application.yml`, `RestClient`, controllers, exception handler.
- Stage 4–5: `TESTING_PLAN.md`. `mvn -f public-api-edge/pom.xml clean test` on **JDK 21**. JaCoCo line coverage ≥ 80%. No `@Disabled` / `@Ignore`. Max **3** repair retries, then Safe-Stop.
- Mule packaging uses **JDK 17**. Do not compile the Mule app with the Boot JDK target, and do not compile Boot as Java 17.
- Stage 6–7: Spring Boot container only, then an honest `MULE_INTEGRATION_SUMMARY.md`.
- Log every mutation in `SDLC_AUDIT_LOG.json`; update `SDLC_METRICS.json` (`INSTRUCTIONS.md`).

## High impact — stop and ask

After Gate 2, any of these needs a new human signature:

- Changing Mule listener paths, upstream hosts, or the normalized JSON fields.
- Adding a database, cache, or SQL.
- Adding a `pom.xml` dependency that `TECH_STACK.md` does not list.
- Letting Spring Boot call IPify, Agify, HTTPBin, Zippopotam, or CoinMarketCap directly.
- Adding an `X-CMC_PRO_API_KEY` header or any hardcoded secret.
- Lowering the 80% line-coverage bar or deleting a mandatory test.

v1 out of scope: CloudHub, a second Spring deployable per API, MySQL, Redis, Flyway, Spring Security, rate-limit stores, Swagger, MUnit, and any public API not named in `PRD.md`.
