package ca.adam_montgomery.moneyball.structures

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatcastGame(
    val game_status_code: String?,
    val game_status: String,
    val gamedayType: String?,
    val gameDate: String?,
    val hasAbs: Boolean?,
    val scoreboard: Scoreboard?,
    val venue_id: Int,
    val away_lineup: List<Int>?,
    val home_lineup: List<Int>?,
    val away_pitcher_lineup: List<Int>?,
    val home_pitcher_lineup: List<Int>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Scoreboard(
    val gamePk: Int?,
    val stats: GameStats?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameStats(
    val wpa: Wpa?,
    val exitVelocity: ExitVelocityStats?,
    val pitchVelocity: PitchVelocityStats?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Wpa(
    val gameWpa: List<Map<String, Any>>?,
    val lastPlays: List<Map<String, Any>>?,
    val topWpaPlayers: List<Map<String, Any>>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExitVelocityStats(
    val top: List<Map<String, Any>>?,
    val lastEV: List<Map<String, Any>>?,
    val topDistance: List<Map<String, Any>>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PitchVelocityStats(
    val topPitches: List<Map<String, Any>>?,
    val currentPitcher: List<Map<String, Any>>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sidedness(
    val code: String?,
    val description: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Position(
    val code: String,
    val name: String?,
    val type: String?,
    val abbreviation: String?
)