# SOP — Agentic Spring Boot SDLC

Playbook distilled from this workspace (url-shortener). Use it to **specify first, then generate** a Spring Boot service. Do not copy url-shortener APIs, schema seed, or class names unless the new PRD says so.

Worked example in this repo: `.ai/greenfield/`, `.ai/brownfield/`, root `AGENTS.md`.

**How to use later:** [../../HOW_TO_USE.md](../../HOW_TO_USE.md)

## Is `.assets` the skill folder?

**SOP / screenshots: yes. Auto-discovered Cursor skill: no.**

- `.assets/` is a fine place for a **human + agent playbook** and JPG evidence.
- Cursor Agent Skills must live in `.cursor/skills/<name>/SKILL.md` (this repo) or the user Agent Store (`skills/<name>/SKILL.md`).
- This SOP is the source of truth; the skill file is a short loader. See [SKILL.md](SKILL.md) and [FILE_MAP.md](FILE_MAP.md).

## Copy into a new repo

1. Copy `AGENTS.md` pattern, `.ai/greenfield/` **templates** (rewrite product content), optional `.ai/brownfield/` playbooks.
2. Copy `.cursor/rules/` if you want the same Gate 2 lock.
3. Copy `.cursor/skills/spring-boot-agentic-sdlc/` or this folder’s `SKILL.md`.
4. Rewrite PRD / design / stack / tests for the **new** product. Leave gates and audit format.

## Markdown that generates the app

Write these **before** `pom.xml` / `src/**`:

| File | Agent uses it to generate |
|:-----|:--------------------------|
| `AGENTS.md` | When to stop, what is binding, out of scope |
| `INSTRUCTIONS.md` | Audit log + metrics schema, no `@Disabled`, secrets via `${PROPERTY:DEFAULT}` |
| `ORCHESTRATION_MANIFEST.md` | Stages, Gate 2 HITL, 3-retry Safe-Stop |
| `PRD.md` | HTTP APIs, payloads, status codes, v1 exclusions |
| `SYSTEM_DESIGN.md` | Data flow, DDL, encoding/id rules |
| `TECH_STACK.md` | Java, Boot parent, **only** allowed `pom` deps, JaCoCo bar |
| `TESTING_PLAN.md` | Mandatory test classes and cases |
| `GATE2_APPROVAL.txt` | `[APPROVED: HUMAN_ENGINEER_ID]` unlocks Java |

Optional after v1: `.ai/brownfield/` playbooks (Swagger, path/allowlist, Security demo, refine). Those are **change** specs, not the first build.

Do **not** treat scenario indexes, orchestration-architecture pages, or engineering summaries as generators. They record decisions after code exists.

## Execution order (do not skip)

```
PRD → design + stack → Gate 2 human file → 3A schema ∥ 3B domain/util ∥ 3C Boot/JPA/HTTP
  → TESTING_PLAN + mvn clean test (JaCoCo ≥ 80%) → Dockerfile/compose → honest summary
```

- **Gate 2:** no `pom.xml` / `src/**` until `GATE2_APPROVAL.txt` exists.
- **Gate 3:** schema + encoder/domain + Spring module all present before tests.
- **Gate 4:** fix production/test code; never `@Disabled` / `@Ignore`; max **3** retries then Safe-Stop.
- **High impact (new human signature):** schema after Gate 2, undeclared `pom` deps, lowering the coverage bar.

## Agent loop

After every mutation: append `SDLC_AUDIT_LOG.json` (SHA-256 of the governing markdown). Update `SDLC_METRICS.json` with **measured** JaCoCo from `target/site/jacoco/jacoco.csv`. Do not invent coverage or claim Docker if it was not run.

## Implementation defaults (this blueprint’s stack)

Override in the new `TECH_STACK.md` if the product differs.

- Java **21**, Spring Boot **3.3.x**, Maven, JPA, MySQL 8, H2 **test** scope
- API bodies: Java `record`s
- Parameterized JPA only (no string-built SQL)
- Secrets: `${PROPERTY:DEFAULT}` lab defaults only
- Redirects (if any): **302**, not 301
- JVM is not an LLM; orchestration stays in markdown + the coding agent

## Brownfield order (optional)

Only after a green Gate 4: `ADD_SWAGGER.md` → `REFACTOR.md` → `ENHANCE.md` → `REFINE.md`. One playbook per change set. Schema extras still need a new signature.

## Out of v1 unless the new PRD lists them

Redis, Flyway, JWT/auth, rate limiting, Safe Browsing, LangGraph inside the JAR.
