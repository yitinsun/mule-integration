# Orchestration architecture (this repo)

`ORCHESTRATION_MANIFEST.md` is the **policy**. This page maps that DAG onto **files**. It is not a second product spec. Class names, JSON fields, and Maven coordinates live in `SYSTEM_DESIGN.md` and `TECH_STACK.md`.

The Spring Boot process is not an integration engine, and the Mule runtime is not an LLM. Agents write the workspace; Maven runs the Boot tests; the operator runs Mule locally.

```
operator
   │  GATE2_APPROVAL.txt  (HITL) — [APPROVED: yitin]
   ▼
agent (Cursor)  ──reads──►  .ai/greenfield   AGENTS.md   .assets/sop
   │                         writes public-api-edge/ and mule-public-api/ only after Gate 2
   ▼
JDK 21 Maven (edge tests)     JDK 17 Mule runtime (local, port 8081)
```

## DAG

```
[1 REQUIREMENTS] ── Gate 1 ──► [2 ARCHITECTURE] ── Gate 2 HITL ──► [3 IMPLEMENTATION]
                                                                      3A ║ 3B ║ 3C
                                                                      └── Gate 3 sync ──►
[4–5 TEST / COMPLIANCE] ── Gate 4 ──► [6 CONTAINERS] ── Gate 5 ──► [7 RELEASE]
        │
        └── fail ──► FALLBACK (max 3) ──► Safe-Stop
upstream spec change ──► re-plan at Stage 1 (audit log kept)
```

| Gate | Meaning here | Artifact |
|:-----|:-------------|:---------|
| 1 | Five providers, specific and general usage, Java 21, local Mule | `.ai/greenfield/PRD.md` |
| 2 | Human unlock. Delta: no SQL; Boot parent 3.3.5; Mule HTTP connector only | `GATE2_APPROVAL.txt` (`[APPROVED: yitin]`) |
| 3 | Mule flows + rules/records + Boot module together | `mule-public-api/`, `public-api-edge/` |
| 4 | Boot tests + coverage + boundary scan | `mvn -f public-api-edge/pom.xml clean test` on JDK 21, JaCoCo ≥ 0.80 |
| 5 | Boot image only. No Mule image | `public-api-edge/Dockerfile` and `docker-compose.yml` are written. Docker CLI was not on PATH, so the image was not built. |
| 6 | Honest summary | `.ai/greenfield/MULE_INTEGRATION_SUMMARY.md` |

## Spec → planned code

| Spec | What Stage 3 is allowed to generate |
|:-----|:------------------------------------|
| `PRD.md` | Edge controllers, records, status codes, the six Mule listener paths |
| `SYSTEM_DESIGN.md` | `public-api.xml`, `public-api.yaml`, `RequestRules`, `MuleClient`, exception handler |
| `TECH_STACK.md` | Both `pom.xml` files, `application.yml`, JaCoCo check |
| `TESTING_PLAN.md` | `RequestRulesTest`, `MuleClientTest`, `PublicApiEdgeIT`, fixture JSON |
| `INSTRUCTIONS.md` | `SDLC_AUDIT_LOG.json`, `SDLC_METRICS.json` |
| `ORCHESTRATION_MANIFEST.md` | Stage order and the retry cap. Not a Java package. |

## Lineage

`SDLC_AUDIT_LOG.json` is append-only. Fields: `timestamp`, `stage`, `action_taken`, `upstream_dependency`, `upstream_dependency_hash`, `rationale`.

A change to `PRD.md` invalidates downstream hashes and restarts at Stage 1. The historical log stays.

## Human vs agent

| Action | Agent | Human |
|:-------|:------|:------|
| Write these specs | Done for Stages 1–2 | Signed `GATE2_APPROVAL.txt` |
| Method-body test repair after Gate 4 fails | Yes, up to 3 times | Review |
| New `pom` dependency, SQL, or a new upstream host | Halt | New signature |
| Coverage below 80% or `@Disabled` | Forbidden | — |
| Start Mule locally, merge, release | — | Operator |

## Metrics

`SDLC_METRICS.json` tracks compile success, syntax failures, retry count, MTTR, and stage timestamps. `stage7_epoch_ms` stays unset until Stage 7 actually finishes. Coverage is not stored as a guess in that file.
