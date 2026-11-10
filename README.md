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

Gate 2 is approved. Status of what has actually been built and measured: `.ai/greenfield/MULE_INTEGRATION_SUMMARY.md`.

```text
mvn -f public-api-edge/pom.xml clean test
```

That command is the Gate 4 check. Run it on JDK 21. Package the Mule app on JDK 17:

```text
mvn -f mule-public-api/pom.xml package
```

Playbook used to produce these specs: `.assets/sop/spring-boot-agentic-sdlc/SOP.md`.
