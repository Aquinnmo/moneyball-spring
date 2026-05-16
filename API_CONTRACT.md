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
Returns enriched and processed game data including advanced metrics (xBA, wOBA, batted-ball quality, plate discipline, expected-vs-actual deltas, game shares, leaders, and a frontend-ready linescore) for a specific game. This endpoint aggregates data from MLB Stats API and Statcast.

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
  "isStolenGame": "boolean",
  "teams": {
    "home": "Team object",
    "away": "Team object"
  },
  "batters": [ "Batter objects" ],
  "pitchers": [ "Pitcher objects" ],
  "summary": "GameSummary object"
}
```

#### GameSummary object
Frontend-ready game-level context and comparisons.
```json
{
  "linescore": {
    "currentInning": "number",
    "currentInningOrdinal": "string",
    "inningState": "string",
    "isTopInning": "boolean",
    "scheduledInnings": "number",
    "home": { "runs": "number", "hits": "number", "errors": "number", "leftOnBase": "number" },
    "away": { "runs": "number", "hits": "number", "errors": "number", "leftOnBase": "number" },
    "innings": [
      { "inning": "number", "ordinal": "string", "homeRuns": "number", "awayRuns": "number" }
    ]
  },
  "shares": {
    "runs": { "home": "number", "away": "number" },
    "expectedRuns": { "home": "number", "away": "number" },
    "qualityAdjustedRuns": { "home": "number", "away": "number" },
    "expectedRunDifferential": { "home": "number", "away": "number" },
    "hits": { "home": "number", "away": "number" },
    "totalBases": { "home": "number", "away": "number" },
    "hardHitBalls": { "home": "number", "away": "number" },
    "winProbability": { "home": "number", "away": "number" }
  },
  "differentials": {
    "homeRunDifferential": "number",
    "awayRunDifferential": "number",
    "homeExpectedRunDifferential": "number",
    "awayExpectedRunDifferential": "number",
    "homeQualityAdjustedRunDifferential": "number",
    "awayQualityAdjustedRunDifferential": "number",
    "homeRunsAboveExpected": "number",
    "awayRunsAboveExpected": "number",
    "homeRunsAllowedAboveExpected": "number",
    "awayRunsAllowedAboveExpected": "number"
  },
  "leaders": {
    "topBattersByWOps": [ "LeaderEntry objects" ],
    "topBattersByHardHitRate": [ "LeaderEntry objects" ],
    "topPitchersByWhiffRate": [ "LeaderEntry objects" ],
    "topPitchersByExpectedRunsAllowed": [ "LeaderEntry objects" ]
  },
  "expectedOutcome": "GameExpectedOutcome object"
}
```

`shares` values are 0-to-1 shares of the game total, similar to MoneyPuck-style "for/against/share" views. If both teams have zero for a category, both share values are `0.0`.
`qualityAdjustedRuns` is the share of deserved runs after blending expected offense with opposing expected pitching allowed. `expectedRunDifferential` is a 0-to-1 advantage share derived from that deserved-run gap.

#### GameExpectedOutcome object
Game-level "what should have happened" model.
```json
{
  "homeExpectedWinPercentage": "number",
  "awayExpectedWinPercentage": "number",
  "homeExpectedRuns": "number",
  "awayExpectedRuns": "number",
  "homeExpectedRunsAllowed": "number",
  "awayExpectedRunsAllowed": "number",
  "homeQualityAdjustedRuns": "number",
  "awayQualityAdjustedRuns": "number",
  "homeQualityAdjustedRunsAllowed": "number",
  "awayQualityAdjustedRunsAllowed": "number",
  "homeDeservedRunDifferential": "number",
  "awayDeservedRunDifferential": "number",
  "modelDescription": "string"
}
```

#### LeaderEntry object
```json
{
  "id": "number",
  "fullName": "string",
  "teamSide": "home | away",
  "value": "number",
  "label": "string"
}
```

#### Team object
```json
{
  "id": "number",
  "name": "string",
  "abbreviation": "string",
  "runs": "number",
  "runsAgainst": "number",
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
  "expTimesOn": "number",
  "expRunsAgainst": "number",
  "batting": "BattingLine object",
  "expectedBatting": "ExpectedBattingLine object",
  "battedBall": "BattedBallProfile object",
  "plateDiscipline": "PlateDiscipline object",
  "pitching": "PitchingLine object",
  "expectedPitching": "ExpectedPitchingLine object",
  "contactAllowed": "BattedBallProfile object",
  "expectedOutcome": "TeamExpectedOutcome object"
}
```

#### TeamExpectedOutcome object
Team-level expected outcome model.
```json
{
  "expectedRunsFor": "number",
  "expectedRunsAgainst": "number",
  "qualityAdjustedRunsFor": "number",
  "qualityAdjustedRunsAgainst": "number",
  "expectedRunDifferential": "number",
  "qualityAdjustedRunDifferential": "number",
  "expectedWinPercentage": "number",
  "contactAdvantageRuns": "number",
  "disciplineAdvantageRuns": "number",
  "deservedRunsAboveActual": "number",
  "actualRunsAboveExpected": "number",
  "actualRunsAllowedAboveExpected": "number"
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
  "tOPS": "number (expTimesOnBase + expBases)",
  "maxExitVelo": "number",
  "avgBatSpeed": "number",
  "maxBatSpeed": "number",
  "avgExitVelo": "number",
  "onHomeTeam": "boolean",
  "batting": "BattingLine object",
  "expected": "ExpectedBattingLine object",
  "battedBall": "BattedBallProfile object",
  "plateDiscipline": "PlateDiscipline object"
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
  "hitsAgainst": "number",
  "strikeouts": "number",
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
  "onHomeTeam": "boolean",
  "pitching": "PitchingLine object",
  "expected": "ExpectedPitchingLine object",
  "contactAllowed": "BattedBallProfile object",
  "plateDiscipline": "PlateDiscipline object"
}
```

#### BattingLine object
Actual batting production with common rate stats precomputed.
```json
{
  "plateAppearances": "number",
  "atBats": "number",
  "hits": "number",
  "singles": "number",
  "doubles": "number",
  "triples": "number",
  "homeRuns": "number",
  "walks": "number",
  "hitByPitch": "number",
  "strikeouts": "number",
  "sacFlies": "number",
  "totalBases": "number",
  "battingAverage": "number",
  "onBasePercentage": "number",
  "sluggingPercentage": "number",
  "ops": "number",
  "isolatedPower": "number",
  "babip": "number",
  "walkRate": "number",
  "strikeoutRate": "number"
}
```

#### ExpectedBattingLine object
Statcast-derived expected production and actual-minus-expected deltas.
```json
{
  "xBA": "number",
  "xOBP": "number",
  "xWOBA": "number",
  "xSLG": "number",
  "xOPS": "number",
  "xHits": "number",
  "xTotalBases": "number",
  "xWeightedTimesOnBase": "number",
  "xRunsCreated": "number",
  "xRunsCreatedPerPA": "number",
  "xLinearWeightRuns": "number",
  "qualityAdjustedRuns": "number",
  "contactRunValue": "number",
  "disciplineRunValue": "number",
  "xHomeRuns": "number",
  "hitsAboveExpected": "number",
  "totalBasesAboveExpected": "number",
  "opsAboveExpected": "number",
  "runsCreatedAboveExpected": "number"
}
```

`xRunsCreated` is a BaseRuns-style estimate using expected hits, expected total bases, expected home runs, walks, hit-by-pitches, and outs. `qualityAdjustedRuns` starts with `xRunsCreated`, then applies small contact-quality and discipline adjustments so the endpoint can answer which team deserved more offense, not only which team had better raw outcomes.

#### BattedBallProfile object
Contact quality profile. Hard-hit balls use the Statcast 95+ mph threshold. Barrels use Baseball Savant's `launch_speed_angle == 6` classification. Sweet-spot balls use launch angles from 8 to 32 degrees.
```json
{
  "ballsInPlay": "number",
  "hardHitBalls": "number",
  "barrels": "number",
  "sweetSpotBalls": "number",
  "avgExitVelo": "number",
  "maxExitVelo": "number",
  "avgLaunchAngle": "number",
  "hardHitRate": "number",
  "barrelRate": "number",
  "sweetSpotRate": "number"
}
```

#### PlateDiscipline object
Pitch-level approach and command profile. `cswRate` is called strikes plus whiffs divided by pitches.
```json
{
  "pitches": "number",
  "strikes": "number",
  "balls": "number",
  "swings": "number",
  "whiffs": "number",
  "calledStrikes": "number",
  "calledStrikesPlusWhiffs": "number",
  "firstPitchStrikes": "number",
  "strikeRate": "number",
  "swingRate": "number",
  "whiffRate": "number",
  "cswRate": "number",
  "firstPitchStrikeRate": "number"
}
```

#### PitchingLine object
Actual pitching results and command rates.
```json
{
  "battersFaced": "number",
  "outs": "number",
  "inningsPitched": "string",
  "pitches": "number",
  "strikes": "number",
  "balls": "number",
  "hitsAllowed": "number",
  "walksAllowed": "number",
  "hitByPitchAllowed": "number",
  "strikeouts": "number",
  "homeRunsAllowed": "number",
  "strikeRate": "number",
  "strikeoutRate": "number",
  "walkRate": "number"
}
```

#### ExpectedPitchingLine object
Expected offensive production allowed by a pitcher or pitching staff.
```json
{
  "xBAAllowed": "number",
  "xOBPAllowed": "number",
  "xWOBAAllowed": "number",
  "xSLGAllowed": "number",
  "xOPSAllowed": "number",
  "xHitsAllowed": "number",
  "xTotalBasesAllowed": "number",
  "xWeightedTimesOnBaseAllowed": "number",
  "expectedRunsAllowed": "number",
  "qualityAdjustedRunsAllowed": "number",
  "xHomeRunsAllowed": "number",
  "contactRunValueAllowed": "number",
  "disciplineRunValueAllowed": "number",
  "runPreventionValue": "number"
}
```

`expectedRunsAllowed` blends expected offensive production allowed with pitch-level run expectancy. `qualityAdjustedRunsAllowed` additionally accounts for contact allowed and pitcher discipline/command.

### Rate and Metric Notes
- All rates are raw 0-to-1 numbers so the frontend can render either decimals or percentages.
- All divide-by-zero cases return `0.0`.
- `xWeightedTimesOnBase` preserves the service's existing wOBA-style expected on-base weighting. It should be treated as a weighted expected on-base contribution, not a literal count of baserunners.
- `expRunsFor` now maps to `expectedBatting.qualityAdjustedRuns`, replacing the older `sum(xSLG)` proxy.
- `expRunsAgainst` now maps to `expectedPitching.qualityAdjustedRunsAllowed`.
- `expWin` is a holistic expected win percentage using a Pythagorean-style baseball exponent of `1.83` over deserved runs. Deserved runs blend a team's quality-adjusted offense with the opponent's quality-adjusted pitching allowed.
- `expWinBat` compares the two teams' quality-adjusted offensive expected runs only.
- `expWinPitch` compares the two teams' quality-adjusted pitching prevention only.
- `contactAllowed` is the pitcher's or team's opponent batted-ball quality profile.

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
*   `https://aquinnmo.github.io`

Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.
Allowed headers: `Authorization`, `Content-Type`.
