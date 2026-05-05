# Moneyball API Contract

This document provides a detailed specification of the exposed API for the Moneyball application.

## Base URL
The application is a Spring Boot service. When running locally, the base URL is:
`http://localhost:8080/`

## Endpoints

### 1. Get Today's Schedule
Returns the MLB schedule for the current date.

*   **URL:** `/today-schedule`
*   **Method:** `GET`
*   **Response Body:** `Schedule` object (see [Data Structures](#schedule))

### 2. Get Today's Games
Returns a simplified list of games scheduled for today.

*   **URL:** `/today-games`
*   **Method:** `GET`
*   **Response Body:** List of `ParsedGame` objects (see [Data Structures](#parsedgame))

### 3. Get Schedule by Date
Returns the MLB schedule for a specific date.

*   **URL:** `/schedule/{date}`
*   **Method:** `GET`
*   **Path Parameters:**
    *   `date`: The date in `YYYY-MM-DD` format.
*   **Response Body:** `Schedule` object (see [Data Structures](#schedule))

### 4. Get Games by Date
Returns a simplified list of games for a specific date.

*   **URL:** `/games/{date}`
*   **Method:** `GET`
*   **Path Parameters:**
    *   `date`: The date in `YYYY-MM-DD` format.
*   **Response Body:** List of `ParsedGame` objects (see [Data Structures](#parsedgame))

### 5. Get Statcast Game Data (Raw)
Returns raw Statcast data for a specific game.

*   **URL:** `/statcast-game={gamePk}`
*   **Method:** `GET`
*   **Path Parameters:**
    *   `gamePk`: The unique identifier for the MLB game.
*   **Response Body:** `StatcastGame` object (see [Data Structures](#statcastgame))

### 6. Get Processed Game Data
Returns enriched and processed game data including advanced metrics (xBA, wOBA, etc.) for a specific game. This endpoint aggregates data from MLB Stats API and Statcast.

*   **URL:** `/game={gamePk}`
*   **Method:** `GET`
*   **Path Parameters:**
    *   `gamePk`: The unique identifier for the MLB game.
*   **Response Body:** `ProcessedGame` object (see [Data Structures](#processedgame))

---

## Data Structures

### Schedule
Detailed MLB schedule information.
```json
{
  "totalItems": "number",
  "totalEvents": "number",
  "totalGames": "number",
  "totalGamesInProgress": "number",
  "dates": [
    {
      "date": "string",
      "totalItems": "number",
      "totalEvents": "number",
      "totalGamesInProgress": "number",
      "games": [
        {
          "gamePk": "number",
          "gameGuid": "string",
          "link": "string",
          "gameType": "string",
          "season": "string",
          "gameDate": "string",
          "officialDate": "string",
          "status": {
            "abstractGameState": "string",
            "codedGameState": "string",
            "detailedState": "string",
            "statusCode": "string",
            "startTimeTBD": "boolean",
            "abstractGameCode": "string"
          },
          "teams": {
            "away": {
              "team": { "id": "number", "name": "string" },
              "leagueRecord": { "wins": "number", "losses": "number", "pct": "string" },
              "splitSquad": "boolean",
              "seriesNumber": "number"
            },
            "home": {
              "team": { "id": "number", "name": "string" },
              "leagueRecord": { "wins": "number", "losses": "number", "pct": "string" },
              "splitSquad": "boolean",
              "seriesNumber": "number"
            }
          },
          "venue": { "id": "number", "name": "string" },
          "gameNumber": "number",
          "publicFacing": "string",
          "gamedayType": "string",
          "tiebreaker": "string",
          "calendarEventID": "string",
          "seasonDisplay": "string",
          "dayNight": "string",
          "scheduledInnings": "number",
          "reverseHomeAwayStatus": "boolean",
          "inningBreakLength": "number",
          "gamesInSeries": "number",
          "seriesGameNumber": "number",
          "seriesDescription": "string",
          "recordSource": "string"
        }
      ]
    }
  ]
}
```

### ParsedGame
Simplified game representation.
```json
{
  "gamePk": "number",
  "teams": {
    "away": { "team": { "id": "number", "name": "string" } },
    "home": { "team": { "id": "number", "name": "string" } }
  },
  "venue": { "id": "number", "name": "string" },
  "gameDate": "string"
}
```

### ProcessedGame
Comprehensive game data with advanced metrics.
```json
{
  "gamePk": "number",
  "dateTime": {
    "originalDate": "string | null",
    "officialDate": "string | null",
    "dayNight": "string | null",
    "time": "string | null",
    "ampm": "string | null"
  },
  "venue": "number",
  "status": "string",
  "teams": {
    "home": "Team object",
    "away": "Team object"
  },
  "batters": [ "Batter objects" ],
  "pitchers": [ "Pitcher objects" ]
}
```

#### Team object
```json
{
  "id": "number",
  "name": "string",
  "abbreviation": "string",
  "runs": "number",
  "hits": "number",
  "errors": "number",
  "leftOnBase": "number",
  "xBA": "number (Expected Batting Average)",
  "wOBA": "number (Weighted On-Base Average)",
  "xSLG": "number (Expected Slugging Percentage)",
  "wOPS": "number (Weighted On-Base Plus Slugging)",
  "nPA": "number (Plate Appearances)",
  "expRunsFor": "number",
  "expWin": "number | null",
  "expWinBat": "number | null",
  "expWinPitch": "number | null",
  "expTimesOn": "number"
}
```

#### Batter object
```json
{
  "id": "number",
  "fullName": "string",
  "firstName": "string",
  "lastName": "string",
  "primaryNumber": "string | null",
  "position": "string",
  "batHand": "string | null",
  "hits": "number",
  "runs": "number",
  "errors": "number",
  "nPA": "number",
  "abCount": "number",
  "wobaCount": "number",
  "xBa": "number",
  "wOBA": "number",
  "xSLG": "number",
  "wOPS": "number",
  "expTimesOnBase": "number",
  "expBases": "number",
  "maxExitVelo": "number",
  "avgBatSpeed": "number",
  "maxBatSpeed": "number",
  "avgExitVelo": "number",
  "onHomeTeam": "boolean"
}
```

#### Pitcher object
```json
{
  "id": "number",
  "fullName": "string",
  "firstName": "string",
  "lastName": "string",
  "primaryNumber": "string | null",
  "pitchHand": "string | null",
  "xBA": "number",
  "wOBA": "number",
  "xSLG": "number",
  "wOPS": "number",
  "expTimesOnBase": "number",
  "expBases": "number",
  "battersFaced": "number",
  "outs": "number",
  "expRunsAgainst": "number",
  "maxExitVelo": "number",
  "avgExitVelo": "number",
  "avgLA": "number",
  "avgBatSpeed": "number",
  "maxBatSpeed": "number",
  "onHomeTeam": "boolean"
}
```

### StatcastGame
Raw data from Baseball Savant.
```json
{
  "game_status_code": "string | null",
  "game_status": "string",
  "gamedayType": "string | null",
  "gameDate": "string | null",
  "hasAbs": "boolean | null",
  "scoreboard": {
    "gamePk": "number | null",
    "stats": {
        "wpa": "Wpa object | null",
        "exitVelocity": "ExitVelocityStats object | null",
        "pitchVelocity": "PitchVelocityStats object | null"
    }
  } | null,
  "venue_id": "number",
  "away_lineup": ["number"] | null,
  "home_lineup": ["number"] | null,
  "away_pitcher_lineup": ["number"] | null,
  "home_pitcher_lineup": ["number"] | null
}
```


## CORS Configuration
The API allows cross-origin requests from:
*   `https://adam-montgomery.ca`
*   `http://localhost:5173`

Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.
Allowed headers: `Authorization`, `Content-Type`.
