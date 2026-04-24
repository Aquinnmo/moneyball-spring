package ca.adam_montgomery.moneyball.structures

data class Schedule (
    val totalItems: Int,
    val totalEvents: Int,
    val totalGames: Int,
    val totalGamesInProgress: Int,
    val dates: List<ScheduleDate>
)

data class ScheduleDate(
    val date: String, //can this be parsed to a date object???
    val totalItems: Int,
    val totalEvents: Int,
    val totalGamesInProgress: Int,
    val games: List<Game>,
)

data class Game(
    val gamePk: Int,
    val gameGuid: String,
    val link: String,
    val gameType: String,
    val season: String,
    val gameDate: String,
    val officialDate: String,
    val status: Status,
    val teams: Teams,
    val venue: Venue,
    val gameNumber: Int,
    val publicFacing: String,
    val gamedayType: String,
    val tiebreaker: String,
    val calendarEventID: String,
    val seasonDisplay: String,
    val dayNight: String,
    val scheduledInnings: Int,
    val reverseHomeAwayStatus: Boolean,
    val inningBreakLength: Int,
    val gamesInSeries: Int,
    val seriesGameNumber: Int,
    val seriesDescription: String,
    val recordSource: String,
)

data class Status(
    val abstractGameState: String,
    val codedGameState: String,
    val detailedState: String,
    val statusCode: String,
    val startTimeTBD: Boolean,
    val abstractGameCode: String,
)

data class Teams(
    val away: TeamGameData,
    val home: TeamGameData
)

data class TeamGameData(
    val team: TeamInfo,
    val leagueRecord: LeagueRecord,
    val splitSquad: Boolean,
    val seriesNumber: Int,
)

data class TeamInfo(
    val id: Int,
    val name: String,
    val link: String
)

data class LeagueRecord(
    val wins: Int,
    val losses: Int,
    val pct: String,
)

data class Venue(
    val id: Int,
    val name: String,
    val link: String,
)