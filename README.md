# mule-integration

Spring Boot 3.x (Java 21) microservices call a **local MuleSoft** integration layer. Mule is the only process that calls the public APIs.

| API | Microservice task |
|:----|:------------------|
| IPify | Public IP |
| Agify.io | Age prediction from a name |
| HTTPBin | Echo the outbound request |
| Zippopotam.us | Postal-code lookup |
| CoinMarketCap Public | Lightweight crypto spot price |

Each API has a **specific** HTTP task and a **general** `POST /api/v1/integrate` operation. Contracts, Mule flows, the allowed Maven dependencies, and the mandatory tests are in `.ai/greenfield/`.

The build is spec-first. Functional Java and Mule sources stay locked until `GATE2_APPROVAL.txt` contains `[APPROVED: …]`. Status of what has actually been built: `.ai/greenfield/MULE_INTEGRATION_SUMMARY.md`.

Playbook used to produce these specs: `.assets/sop/spring-boot-agentic-sdlc/SOP.md`.
