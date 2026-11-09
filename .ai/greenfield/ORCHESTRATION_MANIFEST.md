# Agentic Orchestration & Lifecycle Governance Manifest

This manifest defines the stateful execution graph, gate criteria, and governance constraints for automating the public-API integration SDLC.

Product binding specs override the url-shortener examples in `.assets/sop/spring-boot-agentic-sdlc/`.

## 1. Lifecycle State Machine Graph (DAG)

```
[1. REQUIREMENTS] ──(Gate 1)──> [2. ARCHITECTURE] ──(Gate 2: HITL)──> [3. IMPLEMENTATION]
       ▲                                    │                              │
       │                                    │                         (Gate 3: sync)
       │                                    ▼                              ▼
       │                         [FALLBACK & REPAIR] <── (Gate 4 fail)── [4 & 5. TEST & COMPLIANCE]
       │                                                                   │
       │                                                              (Gate 4 pass)
       │                                                                   ▼
[7. RELEASE READY] <──(Gate 6)── [6. OBSERVABILITY / CONTAINERS] <──(Gate 5)┘
       ▲
       └── (Dynamic Re-plan Trigger: return to Stage 1)
```

## 2. Lifecycle Stage Specifications & Gate Governance

### Stage 1: Requirements Analysis (`PRD.md`)

* **Agent Context:** Product Manager Agent.
* **Exit Gate 1 (Automated):** Every provider in `.notes/mule-project.docx` has a specific endpoint and a general `provider` row. Java 21, Spring Boot 3.x, and local Mule are explicit. v1 exclusions are listed.

### Stage 2: Architecture & Design (`SYSTEM_DESIGN.md` / `TECH_STACK.md`)

* **Agent Context:** System Architect Agent.
* **Dependencies:** Wait until Gate 1 passes.
* **Exit Gate 2 (CRITICAL — Human-in-the-Loop):**
  * Halt Stage 3. The structural delta below is the approval baseline (greenfield; there is no previous schema).
  * Human writes `GATE2_APPROVAL.txt` in the workspace root with `[APPROVED: HUMAN_ENGINEER_ID]`.
  * On approval, the orchestrator records a **Gate 2 snapshot** (git tag `gate2-approved` or a documented workspace snapshot) used as the Safe-Stop rollback point. Then unlock Stage 3.

#### Gate 2 structural delta (empty baseline → v1)

| Item | v1 footprint |
|:-----|:-------------|
| SQL schema / indexes | **None.** No database. |
| Processes | `public-api-edge` on 8080 (Java 21). `mule-public-api` on 8081 (Mule 4, Java 17). |
| Boot `pom.xml` | `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-test`, JaCoCo 0.8.12. Parent `spring-boot-starter-parent` 3.3.5. |
| Mule `pom.xml` | `packaging` `mule-application`. HTTP connector only (`mule-plugin`). Plugin/runtime pair from the installed Studio, minimum runtime 4.9.0. |
| Secrets | None. Keyless CoinMarketCap. No `X-CMC_PRO_API_KEY`. |
| Tests | JDK 21 `mvn -f public-api-edge/pom.xml clean test`. Line coverage ≥ 80%. No live public HTTP. |

### Stage 3: Implementation (Parallel Path Execution)

* **Agent Context:** Software Engineer Agents (parallel).
  * *Sub-Path 3A:* `mule-public-api/` from `SYSTEM_DESIGN.md` (properties, listener, five provider flows, `POST /integrate` choice router, error handler). JDK 17 when packaging.
  * *Sub-Path 3B:* `RequestRules`, `Provider`, and the response `record`s in `public-api-edge`.
  * *Sub-Path 3C:* Spring Boot module (`pom.xml`, `application.yml`, `RestClient` bean, controllers, `GlobalExceptionHandler`).
* **Exit Gate 3 (Synchronization Barrier):** 3A, 3B, and 3C must all be present. Controllers without Mule flows, or flows without the Boot module, do not pass. Partial delivery does not pass Gate 3.
* **Mule repository access:** `mvn -f mule-public-api/pom.xml package` must be attempted on JDK 17 when `repository.mulesoft.org` is reachable with the operator’s Studio settings. If resolution fails for credentials, stop and ask the human. Do not replace the flows with stubs and do not treat that failure as a Boot test retry.

### Stage 4 & 5: Testing, Validation & Policy Compliance (`TESTING_PLAN.md`)

* **Agent Context:** QA Engineer Agent and Compliance/Security Guardrail Agent.
* **Exit Gate 4 (Automated):**
  * Check 4A: `mvn -f public-api-edge/pom.xml clean test` on JDK 21 completes with **100%** test success (zero failures, zero `@Disabled` / `@Ignore`).
  * Check 4B: JaCoCo fails the Boot build unless **line coverage >= 80%**.
  * Check 4C: Boot `src/main` contains no upstream public hosts and no API-key header; no SQL; tests do not call the public internet; Mule flows use the hosts in `SYSTEM_DESIGN.md`.

### Stage 6: Observability & Containers

* **Agent Context:** Release / Platform Agent.
* **Exit Gate 5 (Automated):**
  * `SDLC_AUDIT_LOG.json` and `SDLC_METRICS.json` exist and contain Stage 3–5 entries.
  * `Dockerfile` builds the Boot jar with Java 21 and starts it with `--spring.profiles.active=prod`.
  * `docker-compose.yml` sets `MULE_BASE_URL`. It does **not** bundle a Mule image (trial license). The operator starts Mule locally.
  * Boot listens on **8080**. Mule listens on **8081** when the operator has started it.
  * Do not claim a Docker build in the summary unless this stage actually ran it.

### Stage 7: Release Ready

* **Agent Context:** Orchestrator + human operator.
* **Exit Gate 6:** `MULE_INTEGRATION_SUMMARY.md` matches **actual** artifacts and measured coverage (no invented percentages). Gate 6 fails if the summary claims files, a green Maven run, or a container image that are not in the workspace.

## 3. Resilience, Self-Correction & Fault Management

### Bounded Retries & Fallback Strategy

* **Scope:** Compilation or test failures in Stage 4 (the Boot module).
* **Maximum Loop Count:** **3 retries**.
* **Self-Correction Policy:** On failure, the QA Agent pipes Maven logs and stack traces to the Software Engineer Agent. The Engineer Agent patches the failing class, appends `SDLC_AUDIT_LOG.json`, increments `retry_rollback_frequency`, and records MTTR when the suite passes.

### Fallback, Rollback & Safe-Stop Control

* **Trigger:** Three retries exhausted without a green Boot `mvn clean test`.
* **Rollback Protocol:**
  1. Halt all code write streams immediately.
  2. Reset the workspace to the **Gate 2 snapshot** (`gate2-approved` tag, or the recorded pre-Stage-3 tree). Do not reset `.ai/greenfield` specs unless they changed mid-run.
  3. Enter **Safe-Stop Mode:** freeze background agent loops, write a high-severity entry to `SDLC_AUDIT_LOG.json`, and emit the full error trace. Do not start Stage 6.

A Mule Maven credential failure is an environment stop (ask the human), not one of the three Boot retries.

## 4. Dynamic Re-Planning Loop

* **Trigger:** An upstream spec changes during a run (a sixth provider, a database, CloudHub, or an authenticated CoinMarketCap key).
* **Behavior:** Invalidate downstream file hashes recorded in `SDLC_AUDIT_LOG.json`, halt generation, keep the historical log, and restart at **Stage 1**.

## 5. Controlled Autonomy Rules & Approval Gates

```
[Agent Action Pipeline] ──> [Impact Evaluation]
                                   │
         ┌─────────────────────────┴─────────────────────────┐
         ▼ (High Impact)                                     ▼ (Low Impact)
[HALT Execution Loop]                               [Execute Autonomously]
         │                                                   │
         ▼                                                   ▼
[Verify GATE2 / human signature]                    [Log Action & Telemetry]
         │                                                   │
         └─────────────────────────┬─────────────────────────┘
                                   ▼
                       [Commit State Changes]
```

### 5.1 High-Impact Threshold Actions (Human Intervention Required)

Pause workers and wait for a human signature file for:

* **Contract changes:** listener paths, upstream hosts, or normalized JSON fields after Gate 2.
* **Persistence:** any database, cache, or SQL file.
* **Dependency modifiers:** packages in either `pom.xml` outside `TECH_STACK.md`.
* **Boundary bypass:** Spring code that calls a public API host directly, or a Mule flow that sends `X-CMC_PRO_API_KEY`.
* **Test-bar reduction:** lowering coverage below 80% or deleting mandatory cases in `TESTING_PLAN.md`.

### 5.2 Low-Impact Autonomous Actions (Machine Execution Approved)

Allowed without halt if logged in `SDLC_AUDIT_LOG.json`:

* Refactoring method bodies to fix compile or test failures.
* Adding tests **above** the mandatory matrix.
* Adding log lines that do not change API contracts or log request headers.
