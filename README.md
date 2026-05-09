# Moneyball Spring Boot API

A Spring Boot backend application providing aggregated and advanced baseball statistics by blending MLB's Stats API with Baseball Savant's Statcast data.

## Agent Workflow & Development

If you are an AI assistant working in this repository, follow these workflows to ensure stability and correctness:

### 1. Navigating the Source
- **Do not guess** the file structure. Reference `ARCHITECTURE.md` to find the exact file you need.
- If modifying the API output, always verify and update `API_CONTRACT.md`.
- Read and respect `.instructions.md` for coding conventions (Kotlin & Spring).

### 2. Build & Running
This is a standard Gradle project. Use the provided wrapper commands in your terminal tools:

**To build the project and compile classes:**
```bash
./gradlew build
```

**To start the local development server (runs on `http://localhost:8080/`):**
```bash
./gradlew bootRun
```

### 2.1 Docker Deployment

Build the container image from the repository root:
```bash
docker build -t moneyball-spring:latest .
```
The Docker build compiles the Spring Boot jar inside the image build (no pre-built `build/libs` directory required).

Run the REST API as a containerized server on port `8080`:
```bash
docker run --rm -p 8080:8080 moneyball-spring:latest
```

### 3. Testing
Before proposing a final code change to the user, ensure it compiles and tests currently pass:
```bash
./gradlew test
```

*Note on Windows:* Use `gradlew.bat` instead of `./gradlew`.

## Overview
This system serves a frontend client (expected at `http://localhost:5173` or `https://adam-montgomery.ca`) by processing raw, pitch-by-pitch data and calculating in-game metrics like expected Batting Average (xBA) and weighted On-Base Average (wOBA).

See `AI.md` for a comprehensive breakdown of how this pipeline operates.
