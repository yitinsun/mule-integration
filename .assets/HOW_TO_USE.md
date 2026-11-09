# How to use this blueprint later

**Yes — these guidelines belong in `.assets`.** That folder is for playbooks and screenshots. Cursor still loads the **skill** from `.cursor/skills/spring-boot-agentic-sdlc/SKILL.md`; this file is the human/agent how-to.

Canonical SOP: `sop/spring-boot-agentic-sdlc/SOP.md`. Spec → code: `sop/spring-boot-agentic-sdlc/FILE_MAP.md`.

---

## In this repo (url-shortener)

1. Product specs stay **binding** in `.ai/greenfield/`. Do not regenerate the app from this how-to.
2. Attach the skill: `@spring-boot-agentic-sdlc` (it is opt-in, not every chat).
3. Or tell the agent: “Follow `.assets/sop/spring-boot-agentic-sdlc/SOP.md`.”
4. JPGs in `.assets/` are evidence only. Do not generate Java from images.

---

## New Spring Boot app (copy the template)

1. Create the new repo (or empty folder).
2. Copy:
   - `.assets/sop/spring-boot-agentic-sdlc/` (this playbook)
   - `.assets/HOW_TO_USE.md`
   - `.cursor/skills/spring-boot-agentic-sdlc/`
   - `AGENTS.md` **pattern** (gates, halt rules)
   - `.ai/greenfield/` **file names** — then **rewrite** PRD, design, stack, tests for the new product
   - `.cursor/rules/` if you want the same Gate 2 lock
   - Optional: `.ai/brownfield/` playbooks after v1 is green
3. Do **not** copy url-shortener routes, DTO names, or `schema.sql` `AUTO_INCREMENT` seed unless the new PRD requires them.
4. Write specs first. Human writes `GATE2_APPROVAL.txt` with `[APPROVED: …]` **before** `pom.xml` / `src/**`.
5. Ask the agent to implement **only the current stage** (3A schema ∥ 3B domain ∥ 3C Boot, then tests).
6. Require `mvn clean test`, JaCoCo from `target/site/jacoco/jacoco.csv`, and an honest summary. No invented coverage.

---

## Every machine (optional)

To use the skill outside this repo, copy `.cursor/skills/spring-boot-agentic-sdlc/` into the new project, or into your Cursor **user Agent Store** `skills/spring-boot-agentic-sdlc/`.

---

## Checklist before you call the agent

- [ ] New PRD names APIs and v1 exclusions
- [ ] Design + TECH_STACK list the only allowed `pom` deps
- [ ] TESTING_PLAN lists mandatory tests
- [ ] `GATE2_APPROVAL.txt` exists if you want Java generated
- [ ] You said which stage to run (do not skip Gate 3 before `mvn test`)
