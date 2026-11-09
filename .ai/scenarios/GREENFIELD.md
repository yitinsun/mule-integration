# Greenfield scenario

**Intent:** specify, then build, the MuleSoft + Spring Boot public-API integration. This page is an index. It does not override `.ai/greenfield/`.

**Requirements source:** `.notes/mule-project.docx`.

**Decomposition**

1. Requirements → `.ai/greenfield/PRD.md` (Gate 1: five public APIs, specific usage, general usage, Java 21, Spring Boot 3.x, local Mule).
2. Architecture → `SYSTEM_DESIGN.md`, `TECH_STACK.md` (Gate 2: human `[APPROVED: …]` in `GATE2_APPROVAL.txt`).
3. Implementation in parallel after approval (Gate 3 sync):
   - **3A** `mule-public-api/` flows and upstream configs (JDK 17 runtime)
   - **3B** `RequestRules`, `Provider`, response records
   - **3C** `public-api-edge/` Spring Boot module (JDK 21)
4. Tests / compliance (Gate 4): `mvn -f public-api-edge/pom.xml clean test` on JDK 21, JaCoCo line coverage ≥ 80%, no live calls to the public internet, no `@Disabled`.

**Not in this scenario:** url-shortener routes, MySQL, Base62, CloudHub.

Policy: `.ai/greenfield/ORCHESTRATION_MANIFEST.md`.
