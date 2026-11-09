# AI Agent Instructions: Public API Integration with Lifecycle Governance

You are an advanced, context-aware AI software engineering node operating inside a stateful orchestration graph. You will specify, build, test, and containerize the public-API integration under the gates in this folder.

Governing documents (do not contradict them):

* Product: `PRD.md`
* Architecture: `SYSTEM_DESIGN.md`
* Toolchain: `TECH_STACK.md`
* Tests: `TESTING_PLAN.md`
* Lifecycle gates: `ORCHESTRATION_MANIFEST.md`

The SOP in `.assets/sop/spring-boot-agentic-sdlc/` is the playbook. Its url-shortener schema and routes are not this product.

## 1. Observability, Lineage & Decision Logging

Maintain `SDLC_AUDIT_LOG.json` in the **workspace root**. The file is a JSON array. Append one object for every file generation, class modification, test execution, or self-correction run:

```json
{
  "timestamp": "2026-11-09T18:40:00Z",
  "stage": "REQUIREMENTS",
  "action_taken": "Wrote PRD.md from .notes/mule-project.docx.",
  "upstream_dependency": ".notes/mule-project.docx",
  "upstream_dependency_hash": "<SHA-256 of the governing file>",
  "rationale": "Gate 1 needs a named API for every public API in the brief, plus specific and general usage."
}
```

Allowed `stage` values: `REQUIREMENTS`, `ARCHITECTURE`, `IMPLEMENTATION`, `TESTING`, `COMPLIANCE`, `OBSERVABILITY`, `RELEASE`, `REPAIR`, `REPLAN`.

Preserve historical entries. Never overwrite the log on re-plan; only append.

## 2. Reliability Metrics Tracking

Write aggregated telemetry to `SDLC_METRICS.json` in the workspace root after every execution loop. Do not leave metrics as unnamed context variables.

```json
{
  "success_rate": {
    "compiled_ok": 0,
    "syntax_failures": 0
  },
  "retry_rollback_frequency": 0,
  "mttr_ms": null,
  "stage1_epoch_ms": null,
  "stage7_epoch_ms": null,
  "end_to_end_latency_ms": null,
  "updated_at": "2026-11-09T18:40:00Z"
}
```

* **Success Rate:** successful compiled code blocks vs syntax failures generated. Spec-only stages leave both counters unchanged.
* **Retry/Rollback Frequency:** increment on each Gate 4 fallback loop.
* **MTTR:** milliseconds from a failing `mvn -f public-api-edge/pom.xml clean test` line to a successful re-run of that suite.
* **End-to-End Latency:** `stage7_epoch_ms - stage1_epoch_ms` when Stage 7 completes.

JaCoCo percentages are copied from `public-api-edge/target/site/jacoco/jacoco.csv` into `MULE_INTEGRATION_SUMMARY.md` only after a real run. Do not put an estimated coverage figure in the metrics file.

## 3. Hard Policy Compliance Guardrails

* **Integration boundary:** Java in `public-api-edge` may call only `${MULE_BASE_URL}`. The strings `api.ipify.org`, `agify.io`, `httpbin.org`, `zippopotam.us`, and `coinmarketcap.com` must not appear under `public-api-edge/src/main`. Test fixtures under `src/test/resources/fixtures` may hold sample JSON bodies. They must not be used as live URLs.
* **No database:** v1 has no SQL. A repository, JDBC URL, or `schema.sql` is a Gate 4C failure and a high-impact change.
* **Secret isolation:** Do not hardcode credentials or API keys. The CoinMarketCap path in `PRD.md` is keyless: do not send `X-CMC_PRO_API_KEY`. Map any future secret through `${PROPERTY:DEFAULT}` only after a re-plan that names it in `TECH_STACK.md`.
* **No test bypass:** Never add `@Ignore` or `@Disabled` (JUnit 4 or 5) to make a suite pass.
* **No live public calls in Gate 4:** tests use `MockRestServiceServer` or fixture files. A test that opens a socket to a public API fails the compliance check.
* **JDK split:** `mvn -f public-api-edge/pom.xml clean test` runs on JDK 21. Mule package/run uses JDK 17. Do not “fix” one module by changing the other’s release.

## 4. Work Execution Cycle (State Gate Constrained)

1. **Gate 2 lock:** Do not generate `pom.xml`, `public-api-edge/**`, `mule-public-api/**`, or `src/**` until Gate 2 is approved. Approval is a workspace file `GATE2_APPROVAL.txt` containing a line of the form `[APPROVED: HUMAN_ENGINEER_ID]`. Documentation, `.assets`, `.cursor`, and this `.ai/greenfield` folder may exist before Gate 2; they do not unlock Stage 3.
2. **Bounded retry:** If compilation or tests fail, follow the 3-retry protocol in `ORCHESTRATION_MANIFEST.md`. Fix production or test code so assertions pass. Do not weaken coverage below 80%.
3. **Lineage:** After each mutation, append to `SDLC_AUDIT_LOG.json` with the SHA-256 of the file that justified the change (`PRD.md`, `SYSTEM_DESIGN.md`, `TECH_STACK.md`, `TESTING_PLAN.md`, or, for the PRD itself, `.notes/mule-project.docx`).
