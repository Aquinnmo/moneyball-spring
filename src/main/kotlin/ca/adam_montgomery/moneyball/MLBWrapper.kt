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
    fun getSchedule(date: String? = null): Schedule? {
        val url = if (date != null) {
            "${STATS_API_URL}v1/schedule?sportId=1&date=$date"
        } else {
            "${STATS_API_URL}v1/schedule?sportId=1"
        }
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

    /**
     * Fetches and parses pitch-by-pitch CSV data from Baseball Savant for a specific game.
     * This data includes advanced metrics like pitch delta run expectancy, exit velocity, and estimated BA/wOBA.
     */
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
                pitchDelta = try { record.get("delta_pitcher_run_exp").toDouble() } catch (e: Exception) { 0.0 },
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
    val nonABEvents = setOf("walk", "hit_by_pitch", "intent_walk", "sac_bunt", "sac_fly", "catcher_interf", "sac_bunt_double_play", "sac_fly_double_play")
    val nonWobaEvents = setOf("sac_bunt", "catcher_interf", "intent_walk", "sac_bunt_double_play")

    //process all batters
    val batters: List<Batter> = basicGame.gameData.players.map { player ->
        val rawBatPitches: List<Pitch> = pitchData.filter { pitch -> pitch.batterId == player.value.id }
        val maxBatSpeed = rawBatPitches.map { pitch -> pitch.batSpeed }.filterNotNull().maxOrNull() ?: 0.0
        val avgBatSpeed = rawBatPitches.map { pitch -> pitch.batSpeed }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }

        val hitEvents = setOf("single", "double", "triple", "home_run")
        val hits : Int = rawBatPitches.filter { it.event in hitEvents }.size

        val batPitches = rawBatPitches.groupBy{ it.nPrioirPA }.map{ it.value.maxByOrNull { pitch -> pitch.pitchNumber }!! }
        
        val expTimesOnBase = batPitches.sumOf { pitch ->
            pitch.estWOBA ?: when (pitch.event) {
                "walk" -> 0.69
                "hit_by_pitch" -> 0.72
                else -> 0.0
            }
        }
        val expBases = batPitches.sumOf { pitch -> pitch.estSLG ?: 0.0 }
        val tOPS = expTimesOnBase + expBases
        val nPA = batPitches.maxOfOrNull { pitch -> pitch.nPrioirPA }?.plus(1) ?: 0
        val maxExitVelo = batPitches.map { pitch -> pitch.exitVelo }.filterNotNull().maxOrNull() ?: 0.0
        val avgExitVelo = batPitches.map { pitch -> pitch.exitVelo }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }

        val abPitches = batPitches.filter { it.event !in nonABEvents }
        val xBA = abPitches.map { it.estBA ?: 0.0 }.let { if (it.isEmpty()) 0.0 else it.average() }
        val xSLG = abPitches.map { it.estSLG ?: 0.0 }.let { if (it.isEmpty()) 0.0 else it.average() }
        val abCount = abPitches.size

        val wobaPitches = batPitches.filter { it.event !in nonWobaEvents }
        val wOBA = wobaPitches.map { pitch ->
            pitch.estWOBA ?: when (pitch.event) {
                "walk" -> 0.69
                "hit_by_pitch" -> 0.72
                else -> 0.0
            }
        }.let { if (it.isEmpty()) 0.0 else it.average() }
        val wobaCount = wobaPitches.size

        val wOPS = xSLG + wOBA
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
            fullName = player.value.fullName, hits = hits, runs = 0, errors = 0,
            primaryNumber = player.value.primaryNumber, xBa = xBA, wOBA = wOBA, xSLG = xSLG, wOPS = wOPS,
            nPA = nPA, abCount = abCount, wobaCount = wobaCount, maxBatSpeed = maxBatSpeed, position = player.value.primaryPosition.code,
            batHand = player.value.batHand?.code, avgBatSpeed = avgBatSpeed, maxExitVelo = maxExitVelo,
            avgExitVelo = avgExitVelo, expTimesOnBase = expTimesOnBase, expBases = expBases, tOPS = tOPS, onHomeTeam = onHomeTeam)
    }.filter { it.nPA > 0 }

    //process all pitchers
    val pitchers: List<Pitcher> = basicGame.gameData.players.map { player ->
        var pitches: List<Pitch> = pitchData.filter { pitch -> pitch.pitcherId == player.value.id }
        val maxBatSpeed = pitches.map { pitch -> pitch.batSpeed }.filterNotNull().maxOrNull() ?: 0.0
        val avgBatSpeed = pitches.map { pitch -> pitch.batSpeed }.filterNotNull().let { if (it.isEmpty()) 0.0 else it.average() }

        //I think this clamps this at minimum 0 for each plate appearance, may need to be the maximum though
        val expRunsAgainst = pitches.groupBy { it.batterId to it.nPrioirPA }
            .values.sumOf { pitchList -> maxOf(0.0, pitchList.sumOf { pitch -> pitch.pitchDelta } ) }

        pitches = pitches.groupBy{ it.nPrioirPA }.map{ it.value.maxByOrNull { pitch -> pitch.pitchNumber }!! }
        val expBases = pitches.sumOf { pitch -> pitch.estSLG ?: 0.0 }

        val abPitches = pitches.filter { it.event !in nonABEvents }
        val xBA = abPitches.map { it.estBA ?: 0.0 }.let { if (it.isEmpty()) 0.0 else it.average() }
        val xSLG = abPitches.map { it.estSLG ?: 0.0 }.let { if (it.isEmpty()) 0.0 else it.average() }

        val wobaPitches = pitches.filter { it.event !in nonWobaEvents }
        val wOBA = wobaPitches.map { pitch ->
            pitch.estWOBA ?: when (pitch.event) {
                "walk" -> 0.69
                "hit_by_pitch" -> 0.72
                else -> 0.0
            }
        }.let { if (it.isEmpty()) 0.0 else it.average() }

        val wOPS = xSLG + wOBA
        val expTimesOnBase = pitches.sumOf { pitch ->
            pitch.estWOBA ?: when (pitch.event) {
                "walk" -> 0.69
                "hit_by_pitch" -> 0.72
                else -> 0.0
            }
        }
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

        Pitcher(id=player.value.id, firstName = player.value.fullName, lastName = player.value.lastName,
            fullName = player.value.fullName, primaryNumber = player.value.primaryNumber,
            maxBatSpeed = maxBatSpeed, pitchHand = player.value.pitchHand?.code, avgBatSpeed = avgBatSpeed,
            maxExitVelo = maxExitVelo, avgExitVelo = avgExitVelo, xBA = xBA, wOBA = wOBA, xSLG = xSLG,
            wOPS = wOPS, battersFaced = plateAppearancesAgainst, outs = outsRecorded, expBases = expBases,
            expTimesOnBase = expTimesOnBase, avgLA = avgLaunchAngle, expRunsAgainst = expRunsAgainst,
            onHomeTeam = onHomeTeam)
    }.filter { it.battersFaced > 0 }

    //process team data
    val homeTeamStats : Map<String, Any> = getTeamData(true, batters, pitchers)
    val awayTeamStats = getTeamData(false, batters, pitchers)

    fun calcExpWin(runsFor: Double, runsAgainst: Double): Double {
        if (runsFor == 0.0 && runsAgainst == 0.0) return 0.5
        return (runsFor * runsFor) / ((runsFor * runsFor) + (runsAgainst * runsAgainst))
    }

    val homeActualRuns = (basicGame.liveData.linescore.teams.home.runs ?: 0).toDouble()
    val awayActualRuns = (basicGame.liveData.linescore.teams.away.runs ?: 0).toDouble()

    val homeExpWinBat = calcExpWin(homeTeamStats["expRunsFor"] as Double, awayActualRuns)
    val homeExpWinPitch = calcExpWin(homeActualRuns, homeTeamStats["expRunsAgainst"] as Double)
    val homeExpWin = (homeExpWinBat + homeExpWinPitch) / 2.0

    val awayExpWinBat = calcExpWin(awayTeamStats["expRunsFor"] as Double, homeActualRuns)
    val awayExpWinPitch = calcExpWin(awayActualRuns, awayTeamStats["expRunsAgainst"] as Double)
    val awayExpWin = (awayExpWinBat + awayExpWinPitch) / 2.0

    val homeTeam = Team(
        id=basicGame.gameData.teams.home.id,
        name=basicGame.gameData.teams.home.name,
        abbreviation=basicGame.gameData.teams.home.abbreviation,
        errors = basicGame.liveData.linescore.teams.home.errors,
        hits = basicGame.liveData.linescore.teams.home.hits,
        runs = basicGame.liveData.linescore.teams.home.runs ?: 0,
        leftOnBase = basicGame.liveData.linescore.teams.home.leftOnBase,
        expRunsFor = homeTeamStats["expRunsFor"] as Double,
        expWin = homeExpWin,
        expWinBat = homeExpWinBat,
        expWinPitch = homeExpWinPitch,
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
        expWin = awayExpWin,
        expWinBat = awayExpWinBat,
        expWinPitch = awayExpWinPitch,
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
    val teamBatters = batters.filter { it.onHomeTeam == forHomeTeam }
    val teamPitchers = pitchers.filter { it.onHomeTeam == forHomeTeam }
    val activeBatters = teamBatters.filter { it.nPA > 0 }
    val nPA = teamBatters.sumOf { it.nPA }

    val xBA = if (nPA == 0) 0.0 else activeBatters.map{ it.xBa * it.abCount }.sum() / activeBatters.sumOf { it.abCount }
    val wOBA = if (nPA == 0) 0.0 else activeBatters.map{ it.wOBA * it.wobaCount }.sum() / activeBatters.sumOf { it.wobaCount }
    val xSLG = if (nPA == 0) 0.0 else activeBatters.map{ it.xSLG * it.abCount }.sum() / activeBatters.sumOf { it.abCount }
    val wOPS = xSLG + wOBA
    val expRunsFor = teamBatters.sumOf{ it.xSLG }
    val expTimesOnBase = teamBatters.sumOf{ it.expTimesOnBase }
    val expRunsAgainst = teamPitchers.sumOf { it.expRunsAgainst }
    return mapOf(
        "nPA" to nPA,
        "xBA" to xBA,
        "wOBA" to wOBA,
        "xSLG" to xSLG,
        "wOPS" to wOPS,
        "expRunsFor" to expRunsFor,
        "expRunsAgainst" to expRunsAgainst,
        "expTimesOnBase" to expTimesOnBase,
    )
}

data class ParsedGame(
    val gamePk: Int,
    val teams: Teams,
    val venue: Venue,
    val gameDate: String
)