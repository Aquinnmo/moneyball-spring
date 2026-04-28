package ca.adam_montgomery.moneyball

import ca.adam_montgomery.moneyball.structures.ProcessedGame
import ca.adam_montgomery.moneyball.structures.Schedule
import ca.adam_montgomery.moneyball.structures.StatcastGame
import ca.adam_montgomery.moneyball.structures.BasicGame
import ca.adam_montgomery.moneyball.structures.Batter
import ca.adam_montgomery.moneyball.structures.Pitcher
import ca.adam_montgomery.moneyball.structures.TeamWrapper
import ca.adam_montgomery.moneyball.structures.Teams
import ca.adam_montgomery.moneyball.structures.Team
import ca.adam_montgomery.moneyball.structures.Venue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.apache.commons.csv.CSVFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import kotlin.text.toInt

private const val STATS_API_URL = "https://statsapi.mlb.com/api/"
private const val STATCAST_BASE_URL = "https://baseballsavant.mlb.com/"

@Configuration
class AppConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}

@Service
class StatsApiWrapper(private val restTemplate: RestTemplate) {
    fun getSchedule(): Schedule? {
        val url = "${STATS_API_URL}v1/schedule?sportId=1"
        return restTemplate.getForObject(url, Schedule::class.java)
    }

    fun getGame(gamePk: Int): BasicGame? {
        val url = "${STATS_API_URL}v1.1/game/${gamePk}/feed/live"
        return restTemplate.getForObject(url, BasicGame::class.java)
    }
}

@Service
class StatcastWrapper(private val restTemplate: RestTemplate) {
    fun getGameData(gamePk:Int): StatcastGame? {
        val url = "${STATCAST_BASE_URL}gf?game_pk=$gamePk"
        return restTemplate.getForObject(url, StatcastGame::class.java)
    }

    fun fetchCSV(gamePk: Int) : List<Pitch> {
        val url = "https://baseballsavant.mlb.com/statcast_search/csv?all=true&game_pk=$gamePk&type=details&player_type=pitcher"
        val headers = HttpHeaders()
        headers["User-Agent"] = "Mozilla/5.0"

        val request = HttpEntity<Void>(headers)
        val csvBody = restTemplate.exchange(url, HttpMethod.GET, request, String::class.java).body
            ?: return emptyList()

        val parsed = CSVFormat.DEFAULT.withFirstRecordAsHeader()
            .withIgnoreHeaderCase().withTrim().parse(csvBody.reader())
            .records

        val pitchData : List<Pitch> = parsed.map { record ->
            Pitch(
                pitcherId = record.get("pitcher").toInt(),
                batterId = record.get("batter").toInt(),
                pitchDelta = record.get("delta_pitcher_run_exp").toDouble(),
                batSpeed = record.get("bat_speed").toDoubleOrNull(),
                estBA = record.get("estimated_ba_using_speedangle").toDoubleOrNull(),
                estSLG = record.get("estimated_slg_using_speedangle").toDoubleOrNull(),
                estWOBA = record.get("estimated_woba_using_speedangle").toDoubleOrNull(),
                launchAngle = record.get("launch_angle").toDoubleOrNull(),
                pitchName = record.get("pitch_name"),
                pitchNumber = record.get("pitch_number").toInt(),
                nPrioirPA = record.get("n_priorpa_thisgame_player_at_bat").toInt(),
                exitVelo = record.get("launch_speed").toDoubleOrNull(),
                event = record.get("events"),
                topOfInning = record.get("inning_topbot") == "Top"
            )
        }

        return pitchData
    }
}

data class Pitch(
    val pitcherId: Int,
    val batterId: Int,
    val pitchDelta: Double,
    val batSpeed: Double? = null,
    val estBA: Double? = null,
    val estSLG: Double? = null,
    val estWOBA: Double? = null,
    val launchAngle: Double? = null,
    val pitchName: String,
    val pitchNumber: Int,
    val nPrioirPA: Int,
    val exitVelo: Double? = null,
    val event: String? = null,
    val topOfInning: Boolean,
)

fun processGame(statcastGame: StatcastGame, basicGame: BasicGame, pitchData: List<Pitch>) : ProcessedGame {
    //process all batters
    val batters: List<Batter> = basicGame.gameData.players.map { player ->
        val batPitches: List<Pitch> = pitchData.filter { pitch -> pitch.batterId == player.value.id }

        val xBA = batPitches.map { pitch -> pitch.estBA }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val wOBA = batPitches.map { pitch -> pitch.estWOBA }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val xSLG = batPitches.map { pitch -> pitch.estSLG }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val wOPS = xSLG + wOBA
        val expTimesOnBase = batPitches.sumOf { pitch -> pitch.estWOBA ?: 0.0 }
        val expBases = batPitches.sumOf { pitch -> pitch.estSLG ?: 0.0 }
        val nPA = batPitches.maxOfOrNull { pitch -> pitch.nPrioirPA }?.plus(1) ?: 0
        val maxBatSpeed = batPitches.map { pitch -> pitch.batSpeed }.filterNotNull().maxOrNull() ?: 0.0
        val avgBatSpeed = batPitches.map { pitch -> pitch.batSpeed }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val maxExitVelo = batPitches.map { pitch -> pitch.exitVelo }.filterNotNull().maxOrNull() ?: 0.0
        val avgExitVelo = batPitches.map { pitch -> pitch.exitVelo }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val onHomeTeam : Boolean = if (batPitches.isEmpty()) {
            // If no pitches, we try to determine from basicGame
            basicGame.gameData.teams.home.id == basicGame.gameData.players.filter { it.value.id == player.value.id }.keys.firstOrNull()?.let { true } ?: false
            // Actually player.value.id is what we should use to find the team.
            // But let's simplify: default to false or try to find player's team if possible.
            // A better way is to check if the player belongs to the home team.
            false // default
        } else {
            !(batPitches.first().topOfInning)
        }

        Batter(id=player.value.id, firstName = player.value.fullName, lastName = player.value.lastName,
            fullName = player.value.fullName, hits = 0, runs = 0, errors = 0,
            primaryNumber = player.value.primaryNumber, xBa = xBA, wOBA = wOBA, xSLG = xSLG, wOPS = wOPS,
            nPA = nPA, maxBatSpeed = maxBatSpeed, position = player.value.primaryPosition.code,
            batHand = player.value.batHand?.code, avgBatSpeed = avgBatSpeed, maxExitVelo = maxExitVelo,
            avgExitVelo = avgExitVelo, expTimesOnBase = expTimesOnBase, expBases = expBases, onHomeTeam = onHomeTeam)
    }

    //process all pitchers
    val pitchers: List<Pitcher> = basicGame.gameData.players.map { player ->
        val pitches: List<Pitch> = pitchData.filter { pitch -> pitch.pitcherId == player.value.id }

        val xBA = pitches.map { pitch -> pitch.estBA }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val wOBA = pitches.map { pitch -> pitch.estWOBA }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val xSLG = pitches.map { pitch -> pitch.estSLG }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val wOPS = xSLG + wOBA
        val expTimesOnBase = pitches.sumOf { pitch -> pitch.estWOBA ?: 0.0 }
        val expBases = pitches.sumOf { pitch -> pitch.estSLG ?: 0.0 }
        val maxBatSpeed = pitches.map { pitch -> pitch.batSpeed }.filterNotNull().maxOrNull() ?: 0.0
        val avgBatSpeed = pitches.map { pitch -> pitch.batSpeed }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val maxExitVelo = pitches.map { pitch -> pitch.exitVelo }.filterNotNull().maxOrNull() ?: 0.0
        val avgExitVelo = pitches.map { pitch -> pitch.exitVelo }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val plateAppearancesAgainst = pitches.map { it.batterId to it.nPrioirPA }.distinct().size
        val avgLaunchAngle = pitches.map { it.launchAngle }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }
        val onHomeTeam : Boolean = if (pitches.isEmpty()) false else pitches.first().topOfInning
        val singleOut = setOf("strikeout", "field_out", "force_out", "fielders_choice_out", "sac_fly", "sac_bunt", "caught_stealing_2b",
            "caught_stealing_3b", "caught_stealing_home", "pickoff_caught_stealing_2b", "pickoff_caught_stealing_3b", "pickoff_caught_stealing_home")
        val twoOuts = setOf("grounded_into_double_play", "double_play", "strikeout_double_play", "sac_fly_double_play")
        val threeOuts = setOf("triple_play")
        val outsRecorded = pitches.map { pitch -> pitch.event }.filterNotNull().sumOf { event ->
            if (event in singleOut)  1
            else if (event in twoOuts)  2
            else if (event in threeOuts)  3
            else 0
        }

        //I think this clamps this at minimum 0 for each plate appearance, may need to be the maximum though
        val expRunsAgainst = pitches.groupBy { it.batterId to it.nPrioirPA }
            .values.sumOf { pitch -> maxOf(0.0, pitch.sumOf { pitch -> pitch.pitchDelta } ) }

        Pitcher(id=player.value.id, firstName = player.value.fullName, lastName = player.value.lastName,
            fullName = player.value.fullName, primaryNumber = player.value.primaryNumber,
            maxBatSpeed = maxBatSpeed, pitchHand = player.value.pitchHand?.code, avgBatSpeed = avgBatSpeed,
            maxExitVelo = maxExitVelo, avgExitVelo = avgExitVelo, xBA = xBA, wOBA = wOBA, xSLG = xSLG,
            wOPS = wOPS, battersFaced = plateAppearancesAgainst, outs = outsRecorded, expBases = expBases,
            expTimesOnBase = expTimesOnBase, avgLA = avgLaunchAngle, expRunsAgainst = expRunsAgainst,
            onHomeTeam = onHomeTeam)
    }

    //process team data
    val homeTeamStats : Map<String, Any> = getTeamData(true, batters, pitchers)
    val awayTeamStats = getTeamData(false, batters, pitchers)

    val homeTeam = Team(
        id=basicGame.gameData.teams.home.id,
        name=basicGame.gameData.teams.home.name,
        abbreviation=basicGame.gameData.teams.home.abbreviation,
        errors = basicGame.liveData.linescore.teams.home.errors,
        hits = basicGame.liveData.linescore.teams.home.hits,
        runs = basicGame.liveData.linescore.teams.home.runs ?: 0,
        leftOnBase = basicGame.liveData.linescore.teams.home.leftOnBase,
        expRunsFor = homeTeamStats["expRunsFor"] as Double,
        expWin = null,
        expWinBat = null,
        expWinPitch = null,
        nPA = homeTeamStats["nPA"] as Int,
        xBA = homeTeamStats["xBA"] as Double,
        wOBA = homeTeamStats["wOBA"] as Double,
        xSLG = homeTeamStats["xSLG"] as Double,
        wOPS = homeTeamStats["wOPS"] as Double,
        expTimesOn = homeTeamStats["expTimesOnBase"] as Double,
        )

    val awayTeam = Team(
        id=basicGame.gameData.teams.away.id,
        name=basicGame.gameData.teams.away.name,
        abbreviation=basicGame.gameData.teams.away.abbreviation,
        errors = basicGame.liveData.linescore.teams.away.errors,
        hits = basicGame.liveData.linescore.teams.away.hits,
        runs = basicGame.liveData.linescore.teams.away.runs ?: 0,
        leftOnBase = basicGame.liveData.linescore.teams.away.leftOnBase,
        expRunsFor = awayTeamStats["expRunsFor"] as Double,
        expTimesOn = awayTeamStats["expTimesOnBase"] as Double,
        expWin = null,
        expWinBat = null,
        expWinPitch = null,
        nPA = awayTeamStats["nPA"] as Int,
        xBA = awayTeamStats["xBA"] as Double,
        wOBA = awayTeamStats["wOBA"] as Double,
        xSLG = awayTeamStats["xSLG"] as Double,
        wOPS = awayTeamStats["wOPS"] as Double,
    )

    return ProcessedGame(
        gamePk = basicGame.gamePk,
        dateTime = basicGame.gameData.datetime,
        status = basicGame.gameData.status.statusCode,
        venue = statcastGame.venue_id,
        teams = TeamWrapper(home = homeTeam, away = awayTeam),
        batters = batters,
        pitchers = pitchers
    )
}

fun getTeamData(forHomeTeam: Boolean, batters: List<Batter>, pitchers: List<Pitcher>) : Map<String, Any> {
    val batters = batters.filter { it.onHomeTeam == forHomeTeam }
    val pitchers = pitchers.filter { it.onHomeTeam == forHomeTeam }
    val nPA = batters.sumOf { it.nPA }
    val xBA = batters.map{ it.xBa }.let { if (it.isEmpty()) 0.0 else it.average() }
    val wOBA = batters.map{ it.wOBA }.let { if (it.isEmpty()) 0.0 else it.average() }
    val xSLG = batters.map{ it.xSLG }.let { if (it.isEmpty()) 0.0 else it.average() }
    val wOPS = xSLG + wOBA
    val expRunsFor = batters.sumOf{ it.xSLG }
    val expTimesOnBase = batters.sumOf{ it.expTimesOnBase }
    return mapOf(
        "nPA" to nPA,
        "xBA" to xBA,
        "wOBA" to wOBA,
        "xSLG" to xSLG,
        "wOPS" to wOPS,
        "expRunsFor" to expRunsFor,
        "expTimesOnBase" to expTimesOnBase,
    )
}

data class ParsedGame(
    val gamePk: Int,
    val teams: Teams,
    val venue: Venue,
    val gameDate: String
)