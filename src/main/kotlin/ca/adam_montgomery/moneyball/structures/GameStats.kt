package ca.adam_montgomery.moneyball.structures

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


//Below is the Basic Game
@JsonIgnoreProperties(ignoreUnknown = true)
data class BasicGame(
    val gamePk: Int,
    val metadata: Metadata?,
    val gameData: GameData,
    val liveData: LiveData,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LiveData(
    val linescore: LineScore,
    val decisions: Decisions?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LineScore(
    val currentInning: Int,
    val currentInningOrdinal: String,
    val inningState: String,
    val isTopInning: Boolean,
    val scheduledInnings: Int,
    val innings: List<Inning>,
    val teams: TeamBoxScore,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Decisions(
    val winner: ProbablePitcher?,
    val loser: ProbablePitcher?,
    val save: ProbablePitcher?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TeamBoxScore(
    val home: InningStats,
    val away: InningStats,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Inning(
    val num: Int,
    val ordinalNum: String,
    val home: InningStats,
    val away: InningStats,
)

data class InningStats(
    val runs: Int?,
    val hits: Int,
    val errors: Int,
    val leftOnBase: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameDataTeamWrapper(
    val home: GameDataTeam,
    val away: GameDataTeam,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameDataTeam(
    val id: Int,
    val abbreviation: String,
    val name: String,
    val teamName: String,
    val locationName: String,
    val division: Division,
    val record: TeamRecord,
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Player(
    val id: Int,
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val primaryNumber: String?,
    val active: Boolean,
    val primaryPosition: Position,
    val boxscoreName: String,
    val batHand: Sidedness?,
    val pitchHand: Sidedness?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BatSide(
    val code: String,
    val description: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PitchSide(
    val code: String,
    val description: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TeamRecord(
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
) {
    val pct: Double = wins.toDouble() / gamesPlayed.toDouble()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Division(
    val id: Int,
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameDataStatus(
    val statusCode: String,
    val detailedState: String,
    val startTimeTBD: Boolean,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameData(
    val datetime: DateTime,
    val status: GameDataStatus,
    val teams: GameDataTeamWrapper,
    val players: Map<String, Player>,
    val probablePitchers: ProbablePitchersWrapper,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbablePitchersWrapper(
    val home: ProbablePitcher,
    val away: ProbablePitcher,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbablePitcher(
    val id: Int,
    val fullName: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DateTime(
    val originalDate: String?,
    val officialDate: String?,
    val dayNight: String?,
    val time: String?,
    val ampm: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Metadata(
    val timeStamp: String,
    val logicalEvents: List<String>?,
)
