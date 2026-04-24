package ca.adam_montgomery.moneyball.structures

import java.util.TimeZone

data class StatcastGame (
    val game_status_code: String,
    val game_status: String,
    val gamedayType: String,
    val gameDate: String,
    val scoreboard: Scoreboard,

)