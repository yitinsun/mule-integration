---
name: spring-boot-agentic-sdlc
description: >-
  Runs the gated spec-first Spring Boot SDLC from this repo's SOP (PRD, design,
  stack, tests, Gate 2 HITL, JaCoCo, audit log). Use when starting a new Spring
  Boot service from markdown, copying this blueprint, or when the user mentions
  AGENTS.md, .ai/greenfield, Gate 2, or agentic Spring Boot SOP.
disable-model-invocation: true
---

# Spring Boot agentic SDLC

Load the SOP in this folder, then follow it. Do not invent APIs, `pom` dependencies, or coverage numbers.

## Instructions

1. Read [../../HOW_TO_USE.md](../../HOW_TO_USE.md).
2. Read [SOP.md](SOP.md) end to end.
3. Use [FILE_MAP.md](FILE_MAP.md) to map each markdown file to code.
4. If this workspace is **url-shortener**, treat `.ai/greenfield/` as binding product specs; do not replace them from a blank template.
5. If the user is starting a **new** app, copy the file **roles** from the SOP, then write new PRD / design / stack / tests. Do not reuse url-shortener routes or `AUTO_INCREMENT` seed unless the new PRD requires them.
6. Halt on Gate 2 until `GATE2_APPROVAL.txt` contains `[APPROVED: …]`.
7. Implement only the current stage. After code: `mvn clean test`, JaCoCo from `jacoco.csv`, append `SDLC_AUDIT_LOG.json`.

## Examples

**New service:** "Follow the Spring Boot agentic SOP in `.assets/sop` and scaffold specs for a notification API." → write greenfield markdown first, wait for Gate 2, then 3A/3B/3C.

**This repo:** "Implement TESTING_PLAN" → read `.ai/greenfield/TESTING_PLAN.md`, do not skip Gate 3 artifacts.

## Additional resources

- How to use later: [../../HOW_TO_USE.md](../../HOW_TO_USE.md)
- Human playbook: [SOP.md](SOP.md)
- Spec → code: [FILE_MAP.md](FILE_MAP.md)
- Worked example: repo `.ai/greenfield/`, `.ai/brownfield/`, `AGENTS.md`
