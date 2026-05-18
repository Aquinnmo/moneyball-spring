package ca.adam_montgomery.moneyball

import ca.adam_montgomery.moneyball.structures.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.apache.commons.csv.CSVFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.apache.commons.csv.CSVRecord
import kotlin.math.ceil
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
                pitcherId = record.intOrZero("pitcher"),
                batterId = record.intOrZero("batter"),
                pitchDelta = record.doubleOrZero("delta_pitcher_run_exp"),
                batSpeed = record.doubleOrNull("bat_speed"),
                estBA = record.doubleOrNull("estimated_ba_using_speedangle"),
                estSLG = record.doubleOrNull("estimated_slg_using_speedangle"),
                estWOBA = record.doubleOrNull("estimated_woba_using_speedangle"),
                launchAngle = record.doubleOrNull("launch_angle"),
                pitchName = record.stringOrNull("pitch_name") ?: "Unknown",
                pitchNumber = record.intOrZero("pitch_number"),
                nPrioirPA = record.intOrZero("n_priorpa_thisgame_player_at_bat"),
                exitVelo = record.doubleOrNull("launch_speed"),
                event = record.stringOrNull("events"),
                runnerOnSecond = record.intOrNull("on_2b"),
                runnerOnThird = record.intOrNull("on_3b"),
                batScore = record.intOrNull("bat_score"),
                postBatScore = record.intOrNull("post_bat_score"),
                topOfInning = record.stringOrNull("inning_topbot") == "Top",
                pitchType = record.stringOrNull("pitch_type"),
                description = record.stringOrNull("description"),
                pitchResult = record.stringOrNull("type"),
                battedBallType = record.stringOrNull("bb_type"),
                launchSpeedAngle = record.intOrNull("launch_speed_angle"),
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
    val runnerOnSecond: Int? = null,
    val runnerOnThird: Int? = null,
    val batScore: Int? = null,
    val postBatScore: Int? = null,
    val topOfInning: Boolean,
    val pitchType: String? = null,
    val description: String? = null,
    val pitchResult: String? = null,
    val battedBallType: String? = null,
    val launchSpeedAngle: Int? = null,
)

private fun CSVRecord.stringOrNull(name: String): String? =
    if (isMapped(name)) get(name).trim().takeIf { it.isNotEmpty() } else null

private fun CSVRecord.doubleOrNull(name: String): Double? = stringOrNull(name)?.toDoubleOrNull()

private fun CSVRecord.doubleOrZero(name: String): Double = doubleOrNull(name) ?: 0.0

private fun CSVRecord.intOrNull(name: String): Int? = stringOrNull(name)?.toIntOrNull()

private fun CSVRecord.intOrZero(name: String): Int = intOrNull(name) ?: 0

private val nonABEvents = setOf(
    "walk",
    "hit_by_pitch",
    "intent_walk",
    "sac_bunt",
    "sac_fly",
    "catcher_interf",
    "sac_bunt_double_play",
    "sac_fly_double_play",
)

private val nonWobaEvents = setOf("sac_bunt", "catcher_interf", "intent_walk", "sac_bunt_double_play")
private val nonPlateAppearanceEvents = setOf(
    "stolen_base_2b",
    "stolen_base_3b",
    "stolen_base_home",
    "caught_stealing_2b",
    "caught_stealing_3b",
    "caught_stealing_home",
    "pickoff_1b",
    "pickoff_2b",
    "pickoff_3b",
    "pickoff_caught_stealing_2b",
    "pickoff_caught_stealing_3b",
    "pickoff_caught_stealing_home",
    "pickoff_error_1b",
    "pickoff_error_2b",
    "pickoff_error_3b",
    "wild_pitch",
    "passed_ball",
    "balk",
    "defensive_indiff",
)
private val hitEvents = setOf("single", "double", "triple", "home_run")
private val walkEvents = setOf("walk", "intent_walk")
private val pitchResults = setOf("B", "S", "X")

private fun rate(numerator: Number, denominator: Number): Double {
    val den = denominator.toDouble()
    return if (den == 0.0) 0.0 else numerator.toDouble() / den
}

private fun clamp(value: Double, min: Double, max: Double): Double = value.coerceIn(min, max)

private fun inningsPitched(outs: Int): String = "${outs / 3}.${outs % 3}"

private data class PlateAppearance(val firstPitch: Pitch, val finalPitch: Pitch)

private data class ScoringChanceLine(
    val chances: Int,
    val conversions: Int,
    val conversionRate: Double,
)

private fun isPlateAppearanceEvent(event: String?): Boolean =
    event != null && event !in nonPlateAppearanceEvents

private fun isPitchRow(pitch: Pitch): Boolean = pitch.pitchResult in pitchResults

private fun plateAppearances(pitches: List<Pitch>): List<PlateAppearance> =
    pitches.groupBy { it.batterId to it.nPrioirPA }
        .values
        .mapNotNull { pitchList ->
            val pitchRows = pitchList.filter { isPitchRow(it) }
            val firstPitch = pitchRows.minByOrNull { it.pitchNumber }
            val finalPitch = pitchList.filter { isPlateAppearanceEvent(it.event) }
                .maxByOrNull { it.pitchNumber }

            if (firstPitch != null && finalPitch != null && isPlateAppearanceEvent(finalPitch.event)) {
                PlateAppearance(firstPitch, finalPitch)
            } else {
                null
            }
        }

private fun finalPlateAppearancePitches(pitches: List<Pitch>): List<Pitch> =
    plateAppearances(pitches).map { it.finalPitch }

private fun hasRunnerInScoringPosition(pitch: Pitch): Boolean =
    pitch.runnerOnSecond != null || pitch.runnerOnThird != null

private fun battingRunsScored(pitch: Pitch): Int {
    val before = pitch.batScore ?: return 0
    val after = pitch.postBatScore ?: return 0
    return maxOf(0, after - before)
}

private fun averageBatSpeed(batSpeeds: List<Double>): Double {
    if (batSpeeds.isEmpty()) return 0.0
    val sampleSize = ceil(batSpeeds.size * 0.9).toInt().coerceAtLeast(1)
    return batSpeeds.sortedDescending().take(sampleSize).average()
}

private fun summarizeScoringChances(plateAppearances: List<PlateAppearance>): ScoringChanceLine {
    val chances = plateAppearances.filter { hasRunnerInScoringPosition(it.firstPitch) }
    val conversions = chances.count { battingRunsScored(it.finalPitch) > 0 }

    return ScoringChanceLine(
        chances = chances.size,
        conversions = conversions,
        conversionRate = rate(conversions, chances.size),
    )
}

private fun Boxscore?.player(playerKey: String, onHomeTeam: Boolean): BoxscorePlayer? {
    val teams = this?.teams ?: return null
    val team = if (onHomeTeam) teams.home else teams.away
    return team.players[playerKey]
}

private fun isSwing(pitch: Pitch): Boolean {
    val description = pitch.description ?: return pitch.pitchResult == "X"
    return pitch.pitchResult == "X" ||
        description == "foul" ||
        description == "foul_tip" ||
        description == "foul_bunt" ||
        description == "swinging_strike" ||
        description == "swinging_strike_blocked" ||
        description == "missed_bunt" ||
        description.startsWith("hit_into_play")
}

private fun isWhiff(pitch: Pitch): Boolean =
    pitch.description == "swinging_strike" ||
        pitch.description == "swinging_strike_blocked" ||
        pitch.description == "missed_bunt"

private fun isStrikeOrBallInPlay(pitch: Pitch): Boolean = pitch.pitchResult == "S" || pitch.pitchResult == "X"

private fun summarizePlateDiscipline(pitches: List<Pitch>, plateAppearances: List<PlateAppearance>): PlateDiscipline {
    val strikes = pitches.count { isStrikeOrBallInPlay(it) }
    val balls = pitches.count { it.pitchResult == "B" }
    val swings = pitches.count { isSwing(it) }
    val whiffs = pitches.count { isWhiff(it) }
    val calledStrikes = pitches.count { it.description == "called_strike" }
    val csw = calledStrikes + whiffs
    val firstPitchStrikes = plateAppearances.count { isStrikeOrBallInPlay(it.firstPitch) }

    return PlateDiscipline(
        pitches = pitches.size,
        strikes = strikes,
        balls = balls,
        swings = swings,
        whiffs = whiffs,
        calledStrikes = calledStrikes,
        calledStrikesPlusWhiffs = csw,
        firstPitchStrikes = firstPitchStrikes,
        strikeRate = rate(strikes, pitches.size),
        swingRate = rate(swings, pitches.size),
        whiffRate = rate(whiffs, swings),
        cswRate = rate(csw, pitches.size),
        firstPitchStrikeRate = rate(firstPitchStrikes, plateAppearances.size),
    )
}

private fun summarizeBattedBalls(pitches: List<Pitch>): BattedBallProfile {
    val ballsInPlay = pitches.filter { it.exitVelo != null || it.launchAngle != null || it.pitchResult == "X" }
    val exitVelos = ballsInPlay.mapNotNull { it.exitVelo }
    val launchAngles = ballsInPlay.mapNotNull { it.launchAngle }
    val hardHitBalls = ballsInPlay.count { (it.exitVelo ?: 0.0) >= 95.0 }
    val barrels = ballsInPlay.count { it.launchSpeedAngle == 6 }
    val sweetSpotBalls = ballsInPlay.count { (it.launchAngle ?: Double.NaN) in 8.0..32.0 }

    return BattedBallProfile(
        ballsInPlay = ballsInPlay.size,
        hardHitBalls = hardHitBalls,
        barrels = barrels,
        sweetSpotBalls = sweetSpotBalls,
        avgExitVelo = if (exitVelos.isEmpty()) 0.0 else exitVelos.average(),
        maxExitVelo = exitVelos.maxOrNull() ?: 0.0,
        avgLaunchAngle = if (launchAngles.isEmpty()) 0.0 else launchAngles.average(),
        hardHitRate = rate(hardHitBalls, ballsInPlay.size),
        barrelRate = rate(barrels, ballsInPlay.size),
        sweetSpotRate = rate(sweetSpotBalls, ballsInPlay.size),
        exitVeloSamples = exitVelos.size,
        launchAngleSamples = launchAngles.size,
    )
}

private fun summarizeBatting(finalPitches: List<Pitch>): BattingLine {
    val events = finalPitches.mapNotNull { it.event }
    val hits = events.count { it in hitEvents }
    val singles = events.count { it == "single" }
    val doubles = events.count { it == "double" }
    val triples = events.count { it == "triple" }
    val homeRuns = events.count { it == "home_run" }
    val walks = events.count { it in walkEvents }
    val hitByPitch = events.count { it == "hit_by_pitch" }
    val strikeouts = events.count { it == "strikeout" || it == "strikeout_double_play" }
    val sacFlies = events.count { it == "sac_fly" || it == "sac_fly_double_play" }
    val atBats = finalPitches.count { it.event !in nonABEvents }
    val totalBases = singles + (2 * doubles) + (3 * triples) + (4 * homeRuns)
    val obpDenominator = atBats + walks + hitByPitch + sacFlies
    val babipDenominator = atBats - strikeouts - homeRuns + sacFlies

    return BattingLine(
        plateAppearances = finalPitches.size,
        atBats = atBats,
        hits = hits,
        singles = singles,
        doubles = doubles,
        triples = triples,
        homeRuns = homeRuns,
        walks = walks,
        hitByPitch = hitByPitch,
        strikeouts = strikeouts,
        sacFlies = sacFlies,
        totalBases = totalBases,
        battingAverage = rate(hits, atBats),
        onBasePercentage = rate(hits + walks + hitByPitch, obpDenominator),
        sluggingPercentage = rate(totalBases, atBats),
        ops = rate(hits + walks + hitByPitch, obpDenominator) + rate(totalBases, atBats),
        isolatedPower = rate(totalBases, atBats) - rate(hits, atBats),
        babip = rate(hits - homeRuns, babipDenominator),
        walkRate = rate(walks, finalPitches.size),
        strikeoutRate = rate(strikeouts, finalPitches.size),
    )
}

private fun estimateExpectedHomeRuns(expectedTotalBases: Double, battedBall: BattedBallProfile): Double {
    val contactBasedHomeRuns = (battedBall.barrels * 0.55) +
        (maxOf(0, battedBall.hardHitBalls - battedBall.barrels) * 0.06)
    return minOf(expectedTotalBases / 4.0, contactBasedHomeRuns)
}

private fun estimateBaseRuns(batting: BattingLine, xHits: Double, xTotalBases: Double, xHomeRuns: Double): Double {
    val expectedBaserunners = xHits + batting.walks + batting.hitByPitch - xHomeRuns
    val advancement = (1.4 * xTotalBases) - (0.6 * xHits) - (3.0 * xHomeRuns) +
        (0.1 * (batting.walks + batting.hitByPitch))
    val expectedOuts = batting.atBats - xHits
    val denominator = advancement + expectedOuts
    val baseRuns = if (denominator <= 0.0) xHomeRuns else ((expectedBaserunners * advancement) / denominator) + xHomeRuns
    return maxOf(0.0, baseRuns)
}

private fun estimateLinearWeightRuns(
    batting: BattingLine,
    xWeightedTimesOnBase: Double,
    xTotalBases: Double,
): Double {
    val rawRuns = (xWeightedTimesOnBase * 1.15) + (xTotalBases * 0.22) -
        (batting.plateAppearances * 0.22)
    return maxOf(0.0, rawRuns)
}

private fun contactRunValue(battedBall: BattedBallProfile): Double {
    if (battedBall.ballsInPlay == 0) return 0.0

    val hardHitValue = (battedBall.hardHitRate - 0.38) * battedBall.ballsInPlay * 0.20
    val barrelValue = (battedBall.barrelRate - 0.08) * battedBall.ballsInPlay * 0.45
    val sweetSpotValue = (battedBall.sweetSpotRate - 0.33) * battedBall.ballsInPlay * 0.10
    return hardHitValue + barrelValue + sweetSpotValue
}

private fun batterDisciplineRunValue(batting: BattingLine, discipline: PlateDiscipline): Double {
    val walkValue = (batting.walkRate - 0.085) * batting.plateAppearances * 0.32
    val strikeoutValue = (0.225 - batting.strikeoutRate) * batting.plateAppearances * 0.18
    val pitchQualityValue = (0.27 - discipline.cswRate) * discipline.pitches * 0.025
    return walkValue + strikeoutValue + pitchQualityValue
}

private fun pitcherDisciplineRunValue(pitching: PitchingLine, discipline: PlateDiscipline): Double {
    val strikeoutValue = (pitching.strikeoutRate - 0.225) * pitching.battersFaced * 0.18
    val walkValue = (0.085 - pitching.walkRate) * pitching.battersFaced * 0.32
    val pitchQualityValue = (discipline.cswRate - 0.27) * discipline.pitches * 0.025
    return strikeoutValue + walkValue + pitchQualityValue
}

private fun buildExpectedBattingLine(
    batting: BattingLine,
    xHits: Double,
    xTotalBases: Double,
    xWeightedTimesOnBase: Double,
    battedBall: BattedBallProfile,
    discipline: PlateDiscipline,
    xWobaDenominator: Int,
): ExpectedBattingLine {
    val xHomeRuns = estimateExpectedHomeRuns(xTotalBases, battedBall)
    val xBA = rate(xHits, batting.atBats)
    val xOBP = rate(xHits + batting.walks + batting.hitByPitch, batting.atBats + batting.walks + batting.hitByPitch + batting.sacFlies)
    val xWOBA = rate(xWeightedTimesOnBase, xWobaDenominator)
    val xSLG = rate(xTotalBases, batting.atBats)
    val xOPS = xOBP + xSLG
    val baseRuns = estimateBaseRuns(batting, xHits, xTotalBases, xHomeRuns)
    val linearWeightRuns = estimateLinearWeightRuns(batting, xWeightedTimesOnBase, xTotalBases)
    val xRunsCreated = maxOf(0.0, (baseRuns * 0.7) + (linearWeightRuns * 0.3))
    val contactRuns = contactRunValue(battedBall)
    val disciplineRuns = batterDisciplineRunValue(batting, discipline)
    val qualityAdjustedRuns = maxOf(0.0, xRunsCreated + contactRuns + disciplineRuns)

    return ExpectedBattingLine(
        xBA = xBA,
        xOBP = xOBP,
        xWOBA = xWOBA,
        xSLG = xSLG,
        xOPS = xOPS,
        xHits = xHits,
        xTotalBases = xTotalBases,
        xWeightedTimesOnBase = xWeightedTimesOnBase,
        xRunsCreated = xRunsCreated,
        xRunsCreatedPerPA = rate(xRunsCreated, batting.plateAppearances),
        xLinearWeightRuns = linearWeightRuns,
        qualityAdjustedRuns = qualityAdjustedRuns,
        contactRunValue = contactRuns,
        disciplineRunValue = disciplineRuns,
        xHomeRuns = xHomeRuns,
        hitsAboveExpected = batting.hits - xHits,
        totalBasesAboveExpected = batting.totalBases - xTotalBases,
        opsAboveExpected = batting.ops - xOPS,
        runsCreatedAboveExpected = estimateBaseRuns(
            batting,
            batting.hits.toDouble(),
            batting.totalBases.toDouble(),
            batting.homeRuns.toDouble(),
        ) - xRunsCreated,
        xWobaDenominator = xWobaDenominator,
    )
}

private fun summarizeExpectedBatting(
    finalPitches: List<Pitch>,
    batting: BattingLine,
    battedBall: BattedBallProfile,
    discipline: PlateDiscipline,
): ExpectedBattingLine {
    val abPitches = finalPitches.filter { it.event !in nonABEvents }
    val wobaPitches = finalPitches.filter { it.event !in nonWobaEvents }
    val xHits = abPitches.sumOf { it.estBA ?: 0.0 }
    val xTotalBases = abPitches.sumOf { it.estSLG ?: 0.0 }
    val xWeightedTimesOnBase = wobaPitches.sumOf { pitch ->
        pitch.estWOBA ?: when (pitch.event) {
            "walk" -> 0.69
            "hit_by_pitch" -> 0.72
            else -> 0.0
        }
    }
    return buildExpectedBattingLine(
        batting,
        xHits,
        xTotalBases,
        xWeightedTimesOnBase,
        battedBall,
        discipline,
        wobaPitches.size,
    )
}

private fun summarizePitching(
    pitchRows: List<Pitch>,
    finalPitches: List<Pitch>,
    outs: Int,
    discipline: PlateDiscipline,
): PitchingLine {
    val battingAgainst = summarizeBatting(finalPitches)

    return PitchingLine(
        battersFaced = finalPitches.size,
        outs = outs,
        inningsPitched = inningsPitched(outs),
        pitches = pitchRows.size,
        strikes = discipline.strikes,
        balls = discipline.balls,
        hitsAllowed = battingAgainst.hits,
        walksAllowed = battingAgainst.walks,
        hitByPitchAllowed = battingAgainst.hitByPitch,
        strikeouts = battingAgainst.strikeouts,
        homeRunsAllowed = battingAgainst.homeRuns,
        strikeRate = discipline.strikeRate,
        strikeoutRate = battingAgainst.strikeoutRate,
        walkRate = battingAgainst.walkRate,
    )
}

private fun summarizeExpectedPitching(
    finalPitches: List<Pitch>,
    expRunsAgainst: Double,
    pitching: PitchingLine,
    contactAllowed: BattedBallProfile,
    discipline: PlateDiscipline,
): ExpectedPitchingLine {
    val battingAgainst = summarizeBatting(finalPitches)
    val expectedAgainst = summarizeExpectedBatting(finalPitches, battingAgainst, contactAllowed, discipline)
    val expectedRunsAllowed = (expectedAgainst.xRunsCreated * 0.7) + (expRunsAgainst * 0.3)
    val pitcherDisciplineRuns = pitcherDisciplineRunValue(pitching, discipline)
    val qualityAdjustedRunsAllowed = maxOf(
        0.0,
        (expectedAgainst.qualityAdjustedRuns * 0.7) + (expRunsAgainst * 0.3) - pitcherDisciplineRuns,
    )

    return ExpectedPitchingLine(
        xBAAllowed = expectedAgainst.xBA,
        xOBPAllowed = expectedAgainst.xOBP,
        xWOBAAllowed = expectedAgainst.xWOBA,
        xSLGAllowed = expectedAgainst.xSLG,
        xOPSAllowed = expectedAgainst.xOPS,
        xHitsAllowed = expectedAgainst.xHits,
        xTotalBasesAllowed = expectedAgainst.xTotalBases,
        xWeightedTimesOnBaseAllowed = expectedAgainst.xWeightedTimesOnBase,
        expectedRunsAllowed = expectedRunsAllowed,
        qualityAdjustedRunsAllowed = qualityAdjustedRunsAllowed,
        xHomeRunsAllowed = expectedAgainst.xHomeRuns,
        contactRunValueAllowed = expectedAgainst.contactRunValue,
        disciplineRunValueAllowed = pitcherDisciplineRuns,
        runPreventionValue = -qualityAdjustedRunsAllowed,
        xAtBatsAllowed = battingAgainst.atBats,
        xObpDenominatorAllowed = battingAgainst.atBats + battingAgainst.walks + battingAgainst.hitByPitch + battingAgainst.sacFlies,
        xWobaDenominatorAllowed = expectedAgainst.xWobaDenominator,
    )
}

private fun combineBatting(lines: List<BattingLine>): BattingLine {
    val plateAppearances = lines.sumOf { it.plateAppearances }
    val atBats = lines.sumOf { it.atBats }
    val hits = lines.sumOf { it.hits }
    val singles = lines.sumOf { it.singles }
    val doubles = lines.sumOf { it.doubles }
    val triples = lines.sumOf { it.triples }
    val homeRuns = lines.sumOf { it.homeRuns }
    val walks = lines.sumOf { it.walks }
    val hitByPitch = lines.sumOf { it.hitByPitch }
    val strikeouts = lines.sumOf { it.strikeouts }
    val sacFlies = lines.sumOf { it.sacFlies }
    val totalBases = lines.sumOf { it.totalBases }
    val obpDenominator = atBats + walks + hitByPitch + sacFlies
    val babipDenominator = atBats - strikeouts - homeRuns + sacFlies

    return BattingLine(
        plateAppearances = plateAppearances,
        atBats = atBats,
        hits = hits,
        singles = singles,
        doubles = doubles,
        triples = triples,
        homeRuns = homeRuns,
        walks = walks,
        hitByPitch = hitByPitch,
        strikeouts = strikeouts,
        sacFlies = sacFlies,
        totalBases = totalBases,
        battingAverage = rate(hits, atBats),
        onBasePercentage = rate(hits + walks + hitByPitch, obpDenominator),
        sluggingPercentage = rate(totalBases, atBats),
        ops = rate(hits + walks + hitByPitch, obpDenominator) + rate(totalBases, atBats),
        isolatedPower = rate(totalBases, atBats) - rate(hits, atBats),
        babip = rate(hits - homeRuns, babipDenominator),
        walkRate = rate(walks, plateAppearances),
        strikeoutRate = rate(strikeouts, plateAppearances),
    )
}

private fun combineExpectedBatting(
    lines: List<ExpectedBattingLine>,
    batting: BattingLine,
    battedBall: BattedBallProfile,
    discipline: PlateDiscipline,
): ExpectedBattingLine {
    val xHits = lines.sumOf { it.xHits }
    val xTotalBases = lines.sumOf { it.xTotalBases }
    val xWeightedTimesOnBase = lines.sumOf { it.xWeightedTimesOnBase }
    val xWobaDenominator = lines.sumOf { it.xWobaDenominator }
    return buildExpectedBattingLine(
        batting,
        xHits,
        xTotalBases,
        xWeightedTimesOnBase,
        battedBall,
        discipline,
        xWobaDenominator,
    )
}

private fun combineBattedBalls(profiles: List<BattedBallProfile>): BattedBallProfile {
    val ballsInPlay = profiles.sumOf { it.ballsInPlay }
    val hardHitBalls = profiles.sumOf { it.hardHitBalls }
    val barrels = profiles.sumOf { it.barrels }
    val sweetSpotBalls = profiles.sumOf { it.sweetSpotBalls }
    val exitVeloSamples = profiles.sumOf { it.exitVeloSamples }
    val launchAngleSamples = profiles.sumOf { it.launchAngleSamples }

    return BattedBallProfile(
        ballsInPlay = ballsInPlay,
        hardHitBalls = hardHitBalls,
        barrels = barrels,
        sweetSpotBalls = sweetSpotBalls,
        avgExitVelo = rate(profiles.sumOf { it.avgExitVelo * it.exitVeloSamples }, exitVeloSamples),
        maxExitVelo = profiles.maxOfOrNull { it.maxExitVelo } ?: 0.0,
        avgLaunchAngle = rate(profiles.sumOf { it.avgLaunchAngle * it.launchAngleSamples }, launchAngleSamples),
        hardHitRate = rate(hardHitBalls, ballsInPlay),
        barrelRate = rate(barrels, ballsInPlay),
        sweetSpotRate = rate(sweetSpotBalls, ballsInPlay),
        exitVeloSamples = exitVeloSamples,
        launchAngleSamples = launchAngleSamples,
    )
}

private fun combinePlateDiscipline(profiles: List<PlateDiscipline>, plateAppearances: Int): PlateDiscipline {
    val pitches = profiles.sumOf { it.pitches }
    val strikes = profiles.sumOf { it.strikes }
    val balls = profiles.sumOf { it.balls }
    val swings = profiles.sumOf { it.swings }
    val whiffs = profiles.sumOf { it.whiffs }
    val calledStrikes = profiles.sumOf { it.calledStrikes }
    val csw = profiles.sumOf { it.calledStrikesPlusWhiffs }
    val firstPitchStrikes = profiles.sumOf { it.firstPitchStrikes }

    return PlateDiscipline(
        pitches = pitches,
        strikes = strikes,
        balls = balls,
        swings = swings,
        whiffs = whiffs,
        calledStrikes = calledStrikes,
        calledStrikesPlusWhiffs = csw,
        firstPitchStrikes = firstPitchStrikes,
        strikeRate = rate(strikes, pitches),
        swingRate = rate(swings, pitches),
        whiffRate = rate(whiffs, swings),
        cswRate = rate(csw, pitches),
        firstPitchStrikeRate = rate(firstPitchStrikes, plateAppearances),
    )
}

private fun combinePitching(lines: List<PitchingLine>): PitchingLine {
    val battersFaced = lines.sumOf { it.battersFaced }
    val outs = lines.sumOf { it.outs }
    val pitches = lines.sumOf { it.pitches }
    val strikes = lines.sumOf { it.strikes }
    val balls = lines.sumOf { it.balls }
    val hitsAllowed = lines.sumOf { it.hitsAllowed }
    val walksAllowed = lines.sumOf { it.walksAllowed }
    val hitByPitchAllowed = lines.sumOf { it.hitByPitchAllowed }
    val strikeouts = lines.sumOf { it.strikeouts }
    val homeRunsAllowed = lines.sumOf { it.homeRunsAllowed }

    return PitchingLine(
        battersFaced = battersFaced,
        outs = outs,
        inningsPitched = inningsPitched(outs),
        pitches = pitches,
        strikes = strikes,
        balls = balls,
        hitsAllowed = hitsAllowed,
        walksAllowed = walksAllowed,
        hitByPitchAllowed = hitByPitchAllowed,
        strikeouts = strikeouts,
        homeRunsAllowed = homeRunsAllowed,
        strikeRate = rate(strikes, pitches),
        strikeoutRate = rate(strikeouts, battersFaced),
        walkRate = rate(walksAllowed, battersFaced),
    )
}

private fun combineExpectedPitching(lines: List<ExpectedPitchingLine>, pitching: PitchingLine): ExpectedPitchingLine {
    val xHitsAllowed = lines.sumOf { it.xHitsAllowed }
    val xTotalBasesAllowed = lines.sumOf { it.xTotalBasesAllowed }
    val xWeightedTimesOnBaseAllowed = lines.sumOf { it.xWeightedTimesOnBaseAllowed }
    val xAtBatsAllowed = lines.sumOf { it.xAtBatsAllowed }
    val xObpDenominatorAllowed = lines.sumOf { it.xObpDenominatorAllowed }
    val xWobaDenominatorAllowed = lines.sumOf { it.xWobaDenominatorAllowed }
    val expectedRunsAllowed = lines.sumOf { it.expectedRunsAllowed }
    val qualityAdjustedRunsAllowed = lines.sumOf { it.qualityAdjustedRunsAllowed }
    val xHomeRunsAllowed = lines.sumOf { it.xHomeRunsAllowed }
    val contactRunValueAllowed = lines.sumOf { it.contactRunValueAllowed }
    val disciplineRunValueAllowed = lines.sumOf { it.disciplineRunValueAllowed }
    val xOBPAllowed = rate(
        xHitsAllowed + pitching.walksAllowed + pitching.hitByPitchAllowed,
        xObpDenominatorAllowed,
    )
    val xBAAllowed = rate(xHitsAllowed, xAtBatsAllowed)
    val xWOBAAllowed = rate(xWeightedTimesOnBaseAllowed, xWobaDenominatorAllowed)
    val xSLGAllowed = rate(xTotalBasesAllowed, xAtBatsAllowed)

    return ExpectedPitchingLine(
        xBAAllowed = xBAAllowed,
        xOBPAllowed = xOBPAllowed,
        xWOBAAllowed = xWOBAAllowed,
        xSLGAllowed = xSLGAllowed,
        xOPSAllowed = xOBPAllowed + xSLGAllowed,
        xHitsAllowed = xHitsAllowed,
        xTotalBasesAllowed = xTotalBasesAllowed,
        xWeightedTimesOnBaseAllowed = xWeightedTimesOnBaseAllowed,
        expectedRunsAllowed = expectedRunsAllowed,
        qualityAdjustedRunsAllowed = qualityAdjustedRunsAllowed,
        xHomeRunsAllowed = xHomeRunsAllowed,
        contactRunValueAllowed = contactRunValueAllowed,
        disciplineRunValueAllowed = disciplineRunValueAllowed,
        runPreventionValue = -qualityAdjustedRunsAllowed,
        xAtBatsAllowed = xAtBatsAllowed,
        xObpDenominatorAllowed = xObpDenominatorAllowed,
        xWobaDenominatorAllowed = xWobaDenominatorAllowed,
    )
}

fun processGame(statcastGame: StatcastGame, basicGame: BasicGame, pitchData: List<Pitch>) : ProcessedGame {
    val batters: List<Batter> = basicGame.gameData.players.map { player ->
        val rawBatPitches = pitchData.filter { pitch -> pitch.batterId == player.value.id }
        val batPitchRows = rawBatPitches.filter { isPitchRow(it) }
        val batPlateAppearances = plateAppearances(rawBatPitches)
        val batPitches = batPlateAppearances.map { it.finalPitch }
        val batting = summarizeBatting(batPitches)
        val battedBall = summarizeBattedBalls(batPitches)
        val nPA = batting.plateAppearances
        val plateDiscipline = summarizePlateDiscipline(batPitchRows, batPlateAppearances)
        val expected = summarizeExpectedBatting(batPitches, batting, battedBall, plateDiscipline)
        val scoringChances = summarizeScoringChances(batPlateAppearances)
        val batSpeeds = batPitchRows.mapNotNull { pitch -> pitch.batSpeed }
        val onHomeTeam = if (batPitches.isEmpty()) false else !batPitches.first().topOfInning
        val boxscorePlayer = basicGame.liveData.boxscore.player(player.key, onHomeTeam)

        Batter(
            id = player.value.id,
            firstName = player.value.firstName,
            lastName = player.value.lastName,
            fullName = player.value.fullName,
            hits = batting.hits,
            runs = boxscorePlayer?.stats?.batting?.runs ?: 0,
            errors = boxscorePlayer?.stats?.fielding?.errors ?: 0,
            primaryNumber = player.value.primaryNumber,
            xBa = expected.xBA,
            wOBA = expected.xWOBA,
            xSLG = expected.xSLG,
            wOPS = expected.xWOBA + expected.xSLG,
            nPA = nPA,
            abCount = batting.atBats,
            wobaCount = expected.xWobaDenominator,
            maxBatSpeed = batSpeeds.maxOrNull() ?: 0.0,
            position = player.value.primaryPosition.code,
            batHand = player.value.batHand?.code,
            avgBatSpeed = averageBatSpeed(batSpeeds),
            maxExitVelo = battedBall.maxExitVelo,
            avgExitVelo = battedBall.avgExitVelo,
            rispPlateAppearances = scoringChances.chances,
            rispConversions = scoringChances.conversions,
            rispConversionRate = scoringChances.conversionRate,
            expTimesOnBase = expected.xWeightedTimesOnBase,
            expBases = expected.xTotalBases,
            tOPS = expected.xWeightedTimesOnBase + expected.xTotalBases,
            onHomeTeam = onHomeTeam,
            batting = batting,
            expected = expected,
            battedBall = battedBall,
            plateDiscipline = plateDiscipline,
        )
    }.filter { it.nPA > 0 }

    val pitchers: List<Pitcher> = basicGame.gameData.players.map { player ->
        val rawPitches = pitchData.filter { pitch -> pitch.pitcherId == player.value.id }
        val pitchRows = rawPitches.filter { isPitchRow(it) }
        val pitcherPlateAppearances = plateAppearances(rawPitches)
        val finalPitches = pitcherPlateAppearances.map { it.finalPitch }
        val batSpeeds = pitchRows.mapNotNull { pitch -> pitch.batSpeed }

        val expRunsAgainst = rawPitches.groupBy { it.batterId to it.nPrioirPA }
            .values.sumOf { pitchList -> maxOf(0.0, pitchList.sumOf { pitch -> pitch.pitchDelta } ) }

        val singleOut = setOf("strikeout", "field_out", "force_out", "fielders_choice", "fielders_choice_out", "sac_fly", "sac_bunt", "caught_stealing_2b",
            "caught_stealing_3b", "caught_stealing_home", "pickoff_caught_stealing_2b", "pickoff_caught_stealing_3b", "pickoff_caught_stealing_home", "pickoff_1b", "pickoff_2b", "pickoff_3b")
        val twoOuts = setOf("grounded_into_double_play", "double_play", "strikeout_double_play", "sac_fly_double_play")
        val threeOuts = setOf("triple_play")
        val outsRecorded = rawPitches.mapNotNull { pitch -> pitch.event }.sumOf { event ->
            when (event) {
                in singleOut -> 1
                in twoOuts -> 2
                in threeOuts -> 3
                else -> 0
            }
        }

        val contactAllowed = summarizeBattedBalls(finalPitches)
        val plateDiscipline = summarizePlateDiscipline(pitchRows, pitcherPlateAppearances)
        val pitching = summarizePitching(pitchRows, finalPitches, outsRecorded, plateDiscipline)
        val expected = summarizeExpectedPitching(finalPitches, expRunsAgainst, pitching, contactAllowed, plateDiscipline)
        val onHomeTeam = if (finalPitches.isEmpty()) false else finalPitches.first().topOfInning

        Pitcher(
            id = player.value.id,
            firstName = player.value.firstName,
            lastName = player.value.lastName,
            fullName = player.value.fullName,
            primaryNumber = player.value.primaryNumber,
            maxBatSpeed = batSpeeds.maxOrNull() ?: 0.0,
            pitchHand = player.value.pitchHand?.code,
            avgBatSpeed = averageBatSpeed(batSpeeds),
            maxExitVelo = contactAllowed.maxExitVelo,
            avgExitVelo = contactAllowed.avgExitVelo,
            xBA = expected.xBAAllowed,
            wOBA = expected.xWOBAAllowed,
            xSLG = expected.xSLGAllowed,
            wOPS = expected.xWOBAAllowed + expected.xSLGAllowed,
            battersFaced = pitching.battersFaced,
            outs = pitching.outs,
            expBases = expected.xTotalBasesAllowed,
            expTimesOnBase = expected.xWeightedTimesOnBaseAllowed,
            avgLA = contactAllowed.avgLaunchAngle,
            expRunsAgainst = expected.expectedRunsAllowed,
            onHomeTeam = onHomeTeam,
            strikeouts = pitching.strikeouts,
            hitsAgainst = pitching.hitsAllowed,
            pitching = pitching,
            expected = expected,
            contactAllowed = contactAllowed,
            plateDiscipline = plateDiscipline,
        )
    }.filter { it.battersFaced > 0 }

    //process team data
    val homeTeamStats : Map<String, Any> = getTeamData(true, batters, pitchers)
    val awayTeamStats = getTeamData(false, batters, pitchers)

    fun calcExpWin(runsFor: Double, runsAgainst: Double): Double {
        if (runsFor == 0.0 && runsAgainst == 0.0) return 0.5
        val exponent = 1.83
        val runsForComponent = runsFor.coerceAtLeast(0.0).let { Math.pow(it, exponent) }
        val runsAgainstComponent = runsAgainst.coerceAtLeast(0.0).let { Math.pow(it, exponent) }
        return runsForComponent / (runsForComponent + runsAgainstComponent)
    }

    val homeExpectedBatting = homeTeamStats["expectedBatting"] as ExpectedBattingLine
    val awayExpectedBatting = awayTeamStats["expectedBatting"] as ExpectedBattingLine
    val homeExpectedPitching = homeTeamStats["expectedPitching"] as ExpectedPitchingLine
    val awayExpectedPitching = awayTeamStats["expectedPitching"] as ExpectedPitchingLine
    val homeQualityAdjustedRuns = homeExpectedBatting.qualityAdjustedRuns
    val awayQualityAdjustedRuns = awayExpectedBatting.qualityAdjustedRuns
    val homeQualityAdjustedRunsAllowed = homeExpectedPitching.qualityAdjustedRunsAllowed
    val awayQualityAdjustedRunsAllowed = awayExpectedPitching.qualityAdjustedRunsAllowed
    val homeDeservedRuns = (homeQualityAdjustedRuns + awayQualityAdjustedRunsAllowed) / 2.0
    val awayDeservedRuns = (awayQualityAdjustedRuns + homeQualityAdjustedRunsAllowed) / 2.0
    val homeExpWinBat = calcExpWin(homeQualityAdjustedRuns, awayQualityAdjustedRuns)
    val homeExpWinPitch = calcExpWin(awayQualityAdjustedRunsAllowed, homeQualityAdjustedRunsAllowed)
    val homeExpWin = calcExpWin(homeDeservedRuns, awayDeservedRuns)
    val awayExpWinBat = 1.0 - homeExpWinBat
    val awayExpWinPitch = 1.0 - homeExpWinPitch
    val awayExpWin = 1.0 - homeExpWin
    val homeActualRuns = (basicGame.liveData.linescore.teams.home.runs ?: 0).toDouble()
    val awayActualRuns = (basicGame.liveData.linescore.teams.away.runs ?: 0).toDouble()

    val homeExpectedOutcome = TeamExpectedOutcome(
        expectedRunsFor = homeExpectedBatting.xRunsCreated,
        expectedRunsAgainst = homeExpectedPitching.expectedRunsAllowed,
        qualityAdjustedRunsFor = homeQualityAdjustedRuns,
        qualityAdjustedRunsAgainst = homeQualityAdjustedRunsAllowed,
        expectedRunDifferential = homeExpectedBatting.xRunsCreated - homeExpectedPitching.expectedRunsAllowed,
        qualityAdjustedRunDifferential = homeDeservedRuns - awayDeservedRuns,
        expectedWinPercentage = homeExpWin,
        contactAdvantageRuns = homeExpectedBatting.contactRunValue - homeExpectedPitching.contactRunValueAllowed,
        disciplineAdvantageRuns = homeExpectedBatting.disciplineRunValue + homeExpectedPitching.disciplineRunValueAllowed,
        deservedRunsAboveActual = homeDeservedRuns - homeActualRuns,
        actualRunsAboveExpected = homeActualRuns - homeDeservedRuns,
        actualRunsAllowedAboveExpected = awayActualRuns - awayDeservedRuns,
    )
    val awayExpectedOutcome = TeamExpectedOutcome(
        expectedRunsFor = awayExpectedBatting.xRunsCreated,
        expectedRunsAgainst = awayExpectedPitching.expectedRunsAllowed,
        qualityAdjustedRunsFor = awayQualityAdjustedRuns,
        qualityAdjustedRunsAgainst = awayQualityAdjustedRunsAllowed,
        expectedRunDifferential = awayExpectedBatting.xRunsCreated - awayExpectedPitching.expectedRunsAllowed,
        qualityAdjustedRunDifferential = awayDeservedRuns - homeDeservedRuns,
        expectedWinPercentage = awayExpWin,
        contactAdvantageRuns = awayExpectedBatting.contactRunValue - awayExpectedPitching.contactRunValueAllowed,
        disciplineAdvantageRuns = awayExpectedBatting.disciplineRunValue + awayExpectedPitching.disciplineRunValueAllowed,
        deservedRunsAboveActual = awayDeservedRuns - awayActualRuns,
        actualRunsAboveExpected = awayActualRuns - awayDeservedRuns,
        actualRunsAllowedAboveExpected = homeActualRuns - homeDeservedRuns,
    )

    val homeTeam = Team(
        id=basicGame.gameData.teams.home.id,
        name=basicGame.gameData.teams.home.name,
        abbreviation=basicGame.gameData.teams.home.abbreviation,
        errors = basicGame.liveData.linescore.teams.home.errors,
        hits = basicGame.liveData.linescore.teams.home.hits,
        runs = basicGame.liveData.linescore.teams.home.runs ?: 0,
        runsAgainst = basicGame.liveData.linescore.teams.away.runs ?: 0,
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
        expRunsAgainst = homeTeamStats["expRunsAgainst"] as Double,
        scoringChances = homeTeamStats["scoringChances"] as Int,
        scoringChanceConversions = homeTeamStats["scoringChanceConversions"] as Int,
        scoringChanceConversionRate = homeTeamStats["scoringChanceConversionRate"] as Double,
        batting = homeTeamStats["batting"] as BattingLine,
        expectedBatting = homeTeamStats["expectedBatting"] as ExpectedBattingLine,
        battedBall = homeTeamStats["battedBall"] as BattedBallProfile,
        plateDiscipline = homeTeamStats["plateDiscipline"] as PlateDiscipline,
        pitching = homeTeamStats["pitching"] as PitchingLine,
        expectedPitching = homeTeamStats["expectedPitching"] as ExpectedPitchingLine,
        contactAllowed = homeTeamStats["contactAllowed"] as BattedBallProfile,
        expectedOutcome = homeExpectedOutcome,
        )

    val awayTeam = Team(
        id=basicGame.gameData.teams.away.id,
        name=basicGame.gameData.teams.away.name,
        abbreviation=basicGame.gameData.teams.away.abbreviation,
        errors = basicGame.liveData.linescore.teams.away.errors,
        hits = basicGame.liveData.linescore.teams.away.hits,
        runs = basicGame.liveData.linescore.teams.away.runs ?: 0,
        runsAgainst = basicGame.liveData.linescore.teams.home.runs ?: 0,
        leftOnBase = basicGame.liveData.linescore.teams.away.leftOnBase,
        expRunsFor = awayTeamStats["expRunsFor"] as Double,
        expTimesOn = awayTeamStats["expTimesOnBase"] as Double,
        expRunsAgainst = awayTeamStats["expRunsAgainst"] as Double,
        scoringChances = awayTeamStats["scoringChances"] as Int,
        scoringChanceConversions = awayTeamStats["scoringChanceConversions"] as Int,
        scoringChanceConversionRate = awayTeamStats["scoringChanceConversionRate"] as Double,
        expWin = awayExpWin,
        expWinBat = awayExpWinBat,
        expWinPitch = awayExpWinPitch,
        nPA = awayTeamStats["nPA"] as Int,
        xBA = awayTeamStats["xBA"] as Double,
        wOBA = awayTeamStats["wOBA"] as Double,
        xSLG = awayTeamStats["xSLG"] as Double,
        wOPS = awayTeamStats["wOPS"] as Double,
        batting = awayTeamStats["batting"] as BattingLine,
        expectedBatting = awayTeamStats["expectedBatting"] as ExpectedBattingLine,
        battedBall = awayTeamStats["battedBall"] as BattedBallProfile,
        plateDiscipline = awayTeamStats["plateDiscipline"] as PlateDiscipline,
        pitching = awayTeamStats["pitching"] as PitchingLine,
        expectedPitching = awayTeamStats["expectedPitching"] as ExpectedPitchingLine,
        contactAllowed = awayTeamStats["contactAllowed"] as BattedBallProfile,
        expectedOutcome = awayExpectedOutcome,
    )

    fun share(home: Number, away: Number): SideShare {
        val total = home.toDouble() + away.toDouble()
        return SideShare(home = rate(home, total), away = rate(away, total))
    }

    fun advantageShare(home: Double, away: Double): SideShare {
        val homeAdvantage = maxOf(home - away, 0.0)
        val awayAdvantage = maxOf(away - home, 0.0)
        if (homeAdvantage == 0.0 && awayAdvantage == 0.0) return SideShare(home = 0.5, away = 0.5)
        return share(homeAdvantage, awayAdvantage)
    }

    fun side(onHomeTeam: Boolean): String = if (onHomeTeam) "home" else "away"

    val summary = GameSummary(
        linescore = GameLineScore(
            currentInning = basicGame.liveData.linescore.currentInning,
            currentInningOrdinal = basicGame.liveData.linescore.currentInningOrdinal,
            inningState = basicGame.liveData.linescore.inningState,
            isTopInning = basicGame.liveData.linescore.isTopInning,
            scheduledInnings = basicGame.liveData.linescore.scheduledInnings,
            home = LineScoreTeam(homeTeam.runs, homeTeam.hits, homeTeam.errors, homeTeam.leftOnBase),
            away = LineScoreTeam(awayTeam.runs, awayTeam.hits, awayTeam.errors, awayTeam.leftOnBase),
            innings = basicGame.liveData.linescore.innings.map {
                InningLine(
                    inning = it.num,
                    ordinal = it.ordinalNum,
                    homeRuns = it.home.runs ?: 0,
                    awayRuns = it.away.runs ?: 0,
                )
            },
        ),
        shares = GameShares(
            runs = share(homeTeam.runs, awayTeam.runs),
            expectedRuns = share(homeTeam.expRunsFor, awayTeam.expRunsFor),
            qualityAdjustedRuns = share(homeDeservedRuns, awayDeservedRuns),
            expectedRunDifferential = advantageShare(homeDeservedRuns, awayDeservedRuns),
            hits = share(homeTeam.hits, awayTeam.hits),
            totalBases = share(homeTeam.batting.totalBases, awayTeam.batting.totalBases),
            hardHitBalls = share(homeTeam.battedBall.hardHitBalls, awayTeam.battedBall.hardHitBalls),
            winProbability = share(homeTeam.expWin ?: 0.0, awayTeam.expWin ?: 0.0),
        ),
        differentials = GameDifferentials(
            homeRunDifferential = homeTeam.runs - awayTeam.runs,
            awayRunDifferential = awayTeam.runs - homeTeam.runs,
            homeExpectedRunDifferential = homeExpectedOutcome.expectedRunDifferential,
            awayExpectedRunDifferential = awayExpectedOutcome.expectedRunDifferential,
            homeQualityAdjustedRunDifferential = homeExpectedOutcome.qualityAdjustedRunDifferential,
            awayQualityAdjustedRunDifferential = awayExpectedOutcome.qualityAdjustedRunDifferential,
            homeRunsAboveExpected = homeExpectedOutcome.actualRunsAboveExpected,
            awayRunsAboveExpected = awayExpectedOutcome.actualRunsAboveExpected,
            homeRunsAllowedAboveExpected = homeExpectedOutcome.actualRunsAllowedAboveExpected,
            awayRunsAllowedAboveExpected = awayExpectedOutcome.actualRunsAllowedAboveExpected,
        ),
        leaders = GameLeaders(
            topBattersByWOps = batters.sortedByDescending { it.wOPS }.take(5)
                .map { LeaderEntry(it.id, it.fullName, side(it.onHomeTeam), it.wOPS, "wOPS") },
            topBattersByHardHitRate = batters.filter { it.battedBall.ballsInPlay > 0 }
                .sortedByDescending { it.battedBall.hardHitRate }.take(5)
                .map { LeaderEntry(it.id, it.fullName, side(it.onHomeTeam), it.battedBall.hardHitRate, "hardHitRate") },
            topPitchersByWhiffRate = pitchers.filter { it.plateDiscipline.swings > 0 }
                .sortedByDescending { it.plateDiscipline.whiffRate }.take(5)
                .map { LeaderEntry(it.id, it.fullName, side(it.onHomeTeam), it.plateDiscipline.whiffRate, "whiffRate") },
            topPitchersByExpectedRunsAllowed = pitchers.sortedBy { it.expRunsAgainst }.take(5)
                .map { LeaderEntry(it.id, it.fullName, side(it.onHomeTeam), it.expRunsAgainst, "expectedRunsAllowed") },
        ),
        expectedOutcome = GameExpectedOutcome(
            homeExpectedWinPercentage = homeExpWin,
            awayExpectedWinPercentage = awayExpWin,
            homeExpectedRuns = homeExpectedBatting.xRunsCreated,
            awayExpectedRuns = awayExpectedBatting.xRunsCreated,
            homeExpectedRunsAllowed = homeExpectedPitching.expectedRunsAllowed,
            awayExpectedRunsAllowed = awayExpectedPitching.expectedRunsAllowed,
            homeQualityAdjustedRuns = homeDeservedRuns,
            awayQualityAdjustedRuns = awayDeservedRuns,
            homeQualityAdjustedRunsAllowed = homeQualityAdjustedRunsAllowed,
            awayQualityAdjustedRunsAllowed = awayQualityAdjustedRunsAllowed,
            homeDeservedRunDifferential = homeDeservedRuns - awayDeservedRuns,
            awayDeservedRunDifferential = awayDeservedRuns - homeDeservedRuns,
            modelDescription = "Expected win uses BaseRuns-style expected run creation, xwOBA/xSLG components, contact-quality adjustments, plate-discipline adjustments, and pitching prevention blended into deserved runs.",
        ),
    )

    return ProcessedGame(
        gamePk = basicGame.gamePk,
        dateTime = basicGame.gameData.datetime,
        status = basicGame.gameData.status.statusCode,
        venue = statcastGame.venue_id,
        teams = TeamWrapper(home = homeTeam, away = awayTeam),
        batters = batters,
        pitchers = pitchers,
        summary = summary,
    )
}

fun getTeamData(forHomeTeam: Boolean, batters: List<Batter>, pitchers: List<Pitcher>) : Map<String, Any> {
    val teamBatters = batters.filter { it.onHomeTeam == forHomeTeam }
    val teamPitchers = pitchers.filter { it.onHomeTeam == forHomeTeam }
    val activeBatters = teamBatters.filter { it.nPA > 0 }
    val nPA = teamBatters.sumOf { it.nPA }
    val batting = combineBatting(teamBatters.map { it.batting })
    val battedBall = combineBattedBalls(teamBatters.map { it.battedBall })
    val plateDiscipline = combinePlateDiscipline(teamBatters.map { it.plateDiscipline }, batting.plateAppearances)
    val expectedBatting = combineExpectedBatting(teamBatters.map { it.expected }, batting, battedBall, plateDiscipline)
    val pitching = combinePitching(teamPitchers.map { it.pitching })
    val expectedPitching = combineExpectedPitching(teamPitchers.map { it.expected }, pitching)
    val contactAllowed = combineBattedBalls(teamPitchers.map { it.contactAllowed })

    val xBA = rate(activeBatters.sumOf { it.xBa * it.abCount }, activeBatters.sumOf { it.abCount })
    val wOBA = rate(activeBatters.sumOf { it.wOBA * it.wobaCount }, activeBatters.sumOf { it.wobaCount })
    val xSLG = rate(activeBatters.sumOf { it.xSLG * it.abCount }, activeBatters.sumOf { it.abCount })
    val wOPS = xSLG + wOBA
    val expRunsFor = expectedBatting.qualityAdjustedRuns
    val expTimesOnBase = teamBatters.sumOf{ it.expTimesOnBase }
    val expRunsAgainst = expectedPitching.qualityAdjustedRunsAllowed
    val scoringChances = teamBatters.sumOf { it.rispPlateAppearances }
    val scoringChanceConversions = teamBatters.sumOf { it.rispConversions }
    val scoringChanceConversionRate = rate(scoringChanceConversions, scoringChances)
    return mapOf(
        "nPA" to nPA,
        "xBA" to xBA,
        "wOBA" to wOBA,
        "xSLG" to xSLG,
        "wOPS" to wOPS,
        "expRunsFor" to expRunsFor,
        "expRunsAgainst" to expRunsAgainst,
        "expTimesOnBase" to expTimesOnBase,
        "scoringChances" to scoringChances,
        "scoringChanceConversions" to scoringChanceConversions,
        "scoringChanceConversionRate" to scoringChanceConversionRate,
        "batting" to batting,
        "expectedBatting" to expectedBatting,
        "battedBall" to battedBall,
        "plateDiscipline" to plateDiscipline,
        "pitching" to pitching,
        "expectedPitching" to expectedPitching,
        "contactAllowed" to contactAllowed,
    )
}

data class ParsedGame(
    val gamePk: Int,
    val teams: Teams,
    val venue: Venue,
    val gameDate: String,
    val officialDate: String,
    val scheduledStartUtc: String,
    val scheduledStartTimeUtc: String,
)
