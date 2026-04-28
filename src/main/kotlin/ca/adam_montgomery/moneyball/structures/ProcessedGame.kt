package ca.adam_montgomery.moneyball.structures

data class ProcessedGame(
    val gamePk: Int,
    val dateTime: DateTime,
    val venue: Int,
    val status: String,
    val teams: TeamWrapper,
    val batters: List<Batter>,
    val pitchers: List<Pitcher>
)

data class Batter(
    val id: Int,
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val primaryNumber: String?,
    val position: String,
    val batHand: String?,
    val hits: Int,
    val runs: Int,
    val errors: Int,
    val nPA: Int,
    val xBa: Double,
    val wOBA: Double,
    val xSLG: Double,
    val wOPS: Double,
    val expTimesOnBase: Double,
    val expBases: Double,
    val maxExitVelo: Double,
    val avgBatSpeed: Double,
    val maxBatSpeed: Double,
    val avgExitVelo: Double,
    val onHomeTeam: Boolean,
)

data class Pitcher(
    val id: Int,
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val primaryNumber: String?,
    val pitchHand: String?,
    val xBA: Double,
    val wOBA: Double,
    val xSLG: Double,
    val wOPS: Double,
    val expTimesOnBase: Double,
    val expBases: Double,
    val battersFaced: Int,
    val outs: Int,
    val expRunsAgainst: Double,
    val maxExitVelo: Double,
    val avgExitVelo: Double,
    val avgLA: Double,
    val avgBatSpeed: Double,
    val maxBatSpeed: Double,
    val onHomeTeam: Boolean,
)

data class TeamWrapper(
    val home: Team,
    val away: Team,
)

data class Team(
    val id: Int,
    val name: String,
    val abbreviation: String,
    val runs: Int,
    val hits: Int,
    val errors: Int,
    val leftOnBase: Int,
    val xBA: Double,
    val wOBA: Double,
    val xSLG: Double,
    val wOPS: Double,
    val nPA: Int,
    val expRunsFor: Double,
    val expWin: Double?,
    val expWinBat: Double?,
    val expWinPitch: Double?,
    val expTimesOn: Double,
)