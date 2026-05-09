# Repository Architecture & File Responsibility Map

This guide helps AI agents quickly identify which files to read or modify to implement specific changes.

## Core Application

| File / Folder Path | Responsibility |
| --- | --- |
| `src/main/kotlin/ca/adam_montgomery/moneyball/MoneyballApplication.kt` | **Entry Point & Controllers:** Spring Boot initializers, REST endpoints (`GetMapping`), routing logic, and CORS configurations. Starts the `APIInterface`. |
| `src/main/kotlin/ca/adam_montgomery/moneyball/MLBWrapper.kt` | **Integration & Processing:** Contains `StatsApiWrapper` to fetch basic game data, `StatcastWrapper` to fetch advanced metrics + CSV pitch data from Baseball Savant, and domain assembling logic. |

## Data Models & DTOs
All structured data transfers exist in:
`src/main/kotlin/ca/adam_montgomery/moneyball/structures/`

| File | Responsibility |
| --- | --- |
| `GameData.kt` | DTOs for `StatcastGame`, `Scoreboard`, `GameStats`, etc. parsed from MLB Statcast APIs. |
| `GameStats.kt` | DTOs for `BasicGame`, `LiveData`, `GameDataTeam`, `Player`, etc. mapped from the MLB Stats API. |
| `ProcessedGame.kt` | Our internal merged domain model ready for HTTP response. Advanced heuristics (wOBA, xBA) live here. |
| `Schedule.kt` | DTOs representing the MLB Schedule. |

## Documentation (AI Context)
- `AI.md` - High-level operational conceptual model (The "what").
- `.instructions.md` - Coding guidelines and guardrails for agents (The "how").
- `API_CONTRACT.md` - The exact structure of available REST API endpoints and data shapes.
- `IMPLEMENTATION_PLAN.md` - Historical roadmap for how the project was structured for AI agents.
