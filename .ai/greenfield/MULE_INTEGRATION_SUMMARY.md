# Public API integration — status

Honest status only. Do not fill measured fields until the matching command has been run in this workspace.

| Item | State on this writing |
|:-----|:----------------------|
| Stage | Specifications for Stages 1 and 2 are in `.ai/greenfield/`. Stage 3 has not started. |
| Gate 2 | `GATE2_APPROVAL.txt` is not in the workspace. Java and Mule sources are locked. |
| `public-api-edge` | Not created. |
| `mule-public-api` | Not created. |
| `mvn -f public-api-edge/pom.xml clean test` | Not run. |
| JaCoCo line coverage | Not measured. Do not invent a percentage. |
| Docker image | Not built. |
| Mule local runtime | Not started by this authoring pass. |

Stage 7 replaces the “not run” rows with artifacts and with the line-coverage figure from `public-api-edge/target/site/jacoco/jacoco.csv`.
