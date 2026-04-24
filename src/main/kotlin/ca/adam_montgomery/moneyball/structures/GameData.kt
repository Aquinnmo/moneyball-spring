package ca.adam_montgomery.moneyball.structures

data class GameData (
    val gamePk: Int,
    val metadata: GameMetaData,
    val gameData: GameInfo,
    val dateTime: GameDateTime,
    val status: GameStatus,
    val teams: TeamWrapper,
)
data class GameMetaData(
    val wait: Int,
    val timeStamp: String,
    val gameEvents: List<String>,
    val logicalEvents: List<String>,
)

data class GameInfo(
    val type: String,
    val doubleHeader: String,
    val gamedayType: String,
    val tiebreaker: String,
    val gameNumber: Int,
    val calendarEventID: String,
    val season: String,
)

data class GameDateTime(
    val dateTime: String,
    val originalDate: String,
    val officialDate: String,
    val dayNight: String,
    val time: String,
    val ampm: String,
)

data class GameStatus(
    val abstractGameState: String,
    val codedGameState: String,
    val detailedState: String,
    val statusCode: String,
    val startTimeTBD: Boolean,
    val abstractGameCode: String,
)

data class TeamWrapper(
    val away: Team,
    val home: Team,
)

data class Team(
    val allStarStatus: String,
    val id: Int,
    val name: String,
    val link: String,
    val season: Int,
    val teamCode: String,
    val fileCode: String,
    val abbreviation: String,
    val teamName: String,
    val locationName: String,
    val division: Division,
    val shortName: String,
    val record: TeamRecord,
    val franchiseName: String,
    val clubName: String,
    val active: Boolean,
)

data class Division(
    val name: String,
    val id: Int,
)

data class TeamRecord(
    val gamesPlayed: Int,
    val wildCardGamesBack: String,
    val leagueGamesBack: String,
    val springLeagueGamesBack: String,
    val sportGamesBack: String,
    val divisionGamesBack: String,
    val conferenceGamesBack: String,
    val divisionLeader: Boolean,
    val wins: Int,
    val losses: Int,
) {
    val pct: Double get() = wins.toDouble() / gamesPlayed.toDouble()
}