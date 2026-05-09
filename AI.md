# Moneyball AI Mental Model

## Overview
Moneyball is a Spring Boot application written in Kotlin that provides aggregated MLB (Major League Baseball) statistics. It serves as a backend API for a frontend client.

## Core Data Flow
The system aggregates data from two primary external sources:
1. **MLB Stats API:** Provides basic schedule and game data (`BasicGame`, `Schedule`).
2. **Baseball Savant (Statcast):** Provides advanced analytics and pitch-by-pitch data (JSON `StatcastGame` and raw CSV pitch data).

These sources are combined in `processGame()` (in `MoneyballApplication.kt` / `MLBWrapper.kt`) to produce a `ProcessedGame`, which is an enriched object containing expected statistics (xBA, wOBA, etc.) for both batters and pitchers.

## Key Modules
- [src/main/kotlin/ca/adam_montgomery/moneyball/MoneyballApplication.kt](src/main/kotlin/ca/adam_montgomery/moneyball/MoneyballApplication.kt): Contains the REST controllers (`APIInterface`) and application entry point. Wiring of the endpoints.
- [src/main/kotlin/ca/adam_montgomery/moneyball/MLBWrapper.kt](src/main/kotlin/ca/adam_montgomery/moneyball/MLBWrapper.kt): Contains `StatsApiWrapper` and `StatcastWrapper` services for fetching external data and CSV parsing.
- [src/main/kotlin/ca/adam_montgomery/moneyball/structures/](src/main/kotlin/ca/adam_montgomery/moneyball/structures/): Domain models and DTOs representing external API responses and internal processed data.

## Stack
- **Language:** Kotlin
- **Framework:** Spring Boot
- **Dependency Management:** Gradle (Kotlin DSL)
- **Data Processing:** Apache Commons CSV, Jackson for JSON parsing.
