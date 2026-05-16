package ca.adam_montgomery.moneyball.structures

data class ProcessedGame(
    val gamePk: Int,
    val dateTime: DateTime,
    val venue: Int,
    val status: String,
    val teams: TeamWrapper,
    val batters: List<Batter>,
    val pitchers: List<Pitcher>,
    val summary: GameSummary = GameSummary(),
)
{
    val isStolenGame: Boolean = (teams.home.runs > teams.away.runs && teams.home.runs - teams.away.runs < teams.home.expRunsAgainst - teams.home.runsAgainst) ||
            (teams.away.runs > teams.home
                .runs && teams.away.runs - teams.home.runs < teams.away.expRunsAgainst - teams.away.runsAgainst)
}

data class GameSummary(
    val linescore: GameLineScore = GameLineScore(),
    val shares: GameShares = GameShares(),
    val differentials: GameDifferentials = GameDifferentials(),
    val leaders: GameLeaders = GameLeaders(),
    val expectedOutcome: GameExpectedOutcome = GameExpectedOutcome(),
)

data class GameLineScore(
    val currentInning: Int = 0,
    val currentInningOrdinal: String = "",
    val inningState: String = "",
    val isTopInning: Boolean = false,
    val scheduledInnings: Int = 9,
    val home: LineScoreTeam = LineScoreTeam(),
    val away: LineScoreTeam = LineScoreTeam(),
    val innings: List<InningLine> = emptyList(),
)

data class LineScoreTeam(
    val runs: Int = 0,
    val hits: Int = 0,
    val errors: Int = 0,
    val leftOnBase: Int = 0,
)

data class InningLine(
    val inning: Int = 0,
    val ordinal: String = "",
    val homeRuns: Int = 0,
    val awayRuns: Int = 0,
)

data class GameShares(
    val runs: SideShare = SideShare(),
    val expectedRuns: SideShare = SideShare(),
    val qualityAdjustedRuns: SideShare = SideShare(),
    val expectedRunDifferential: SideShare = SideShare(),
    val hits: SideShare = SideShare(),
    val totalBases: SideShare = SideShare(),
    val hardHitBalls: SideShare = SideShare(),
    val winProbability: SideShare = SideShare(),
)

data class SideShare(
    val home: Double = 0.0,
    val away: Double = 0.0,
)

data class GameDifferentials(
    val homeRunDifferential: Int = 0,
    val awayRunDifferential: Int = 0,
    val homeExpectedRunDifferential: Double = 0.0,
    val awayExpectedRunDifferential: Double = 0.0,
    val homeQualityAdjustedRunDifferential: Double = 0.0,
    val awayQualityAdjustedRunDifferential: Double = 0.0,
    val homeRunsAboveExpected: Double = 0.0,
    val awayRunsAboveExpected: Double = 0.0,
    val homeRunsAllowedAboveExpected: Double = 0.0,
    val awayRunsAllowedAboveExpected: Double = 0.0,
)

data class GameExpectedOutcome(
    val homeExpectedWinPercentage: Double = 0.5,
    val awayExpectedWinPercentage: Double = 0.5,
    val homeExpectedRuns: Double = 0.0,
    val awayExpectedRuns: Double = 0.0,
    val homeExpectedRunsAllowed: Double = 0.0,
    val awayExpectedRunsAllowed: Double = 0.0,
    val homeQualityAdjustedRuns: Double = 0.0,
    val awayQualityAdjustedRuns: Double = 0.0,
    val homeQualityAdjustedRunsAllowed: Double = 0.0,
    val awayQualityAdjustedRunsAllowed: Double = 0.0,
    val homeDeservedRunDifferential: Double = 0.0,
    val awayDeservedRunDifferential: Double = 0.0,
    val modelDescription: String = "",
)

data class GameLeaders(
    val topBattersByWOps: List<LeaderEntry> = emptyList(),
    val topBattersByHardHitRate: List<LeaderEntry> = emptyList(),
    val topPitchersByWhiffRate: List<LeaderEntry> = emptyList(),
    val topPitchersByExpectedRunsAllowed: List<LeaderEntry> = emptyList(),
)

data class LeaderEntry(
    val id: Int = 0,
    val fullName: String = "",
    val teamSide: String = "",
    val value: Double = 0.0,
    val label: String = "",
)

data class BattingLine(
    val plateAppearances: Int = 0,
    val atBats: Int = 0,
    val hits: Int = 0,
    val singles: Int = 0,
    val doubles: Int = 0,
    val triples: Int = 0,
    val homeRuns: Int = 0,
    val walks: Int = 0,
    val hitByPitch: Int = 0,
    val strikeouts: Int = 0,
    val sacFlies: Int = 0,
    val totalBases: Int = 0,
    val battingAverage: Double = 0.0,
    val onBasePercentage: Double = 0.0,
    val sluggingPercentage: Double = 0.0,
    val ops: Double = 0.0,
    val isolatedPower: Double = 0.0,
    val babip: Double = 0.0,
    val walkRate: Double = 0.0,
    val strikeoutRate: Double = 0.0,
)

data class ExpectedBattingLine(
    val xBA: Double = 0.0,
    val xOBP: Double = 0.0,
    val xWOBA: Double = 0.0,
    val xSLG: Double = 0.0,
    val xOPS: Double = 0.0,
    val xHits: Double = 0.0,
    val xTotalBases: Double = 0.0,
    val xWeightedTimesOnBase: Double = 0.0,
    val xRunsCreated: Double = 0.0,
    val xRunsCreatedPerPA: Double = 0.0,
    val xLinearWeightRuns: Double = 0.0,
    val qualityAdjustedRuns: Double = 0.0,
    val contactRunValue: Double = 0.0,
    val disciplineRunValue: Double = 0.0,
    val xHomeRuns: Double = 0.0,
    val hitsAboveExpected: Double = 0.0,
    val totalBasesAboveExpected: Double = 0.0,
    val opsAboveExpected: Double = 0.0,
    val runsCreatedAboveExpected: Double = 0.0,
)

data class BattedBallProfile(
    val ballsInPlay: Int = 0,
    val hardHitBalls: Int = 0,
    val barrels: Int = 0,
    val sweetSpotBalls: Int = 0,
    val avgExitVelo: Double = 0.0,
    val maxExitVelo: Double = 0.0,
    val avgLaunchAngle: Double = 0.0,
    val hardHitRate: Double = 0.0,
    val barrelRate: Double = 0.0,
    val sweetSpotRate: Double = 0.0,
)

data class PlateDiscipline(
    val pitches: Int = 0,
    val strikes: Int = 0,
    val balls: Int = 0,
    val swings: Int = 0,
    val whiffs: Int = 0,
    val calledStrikes: Int = 0,
    val calledStrikesPlusWhiffs: Int = 0,
    val firstPitchStrikes: Int = 0,
    val strikeRate: Double = 0.0,
    val swingRate: Double = 0.0,
    val whiffRate: Double = 0.0,
    val cswRate: Double = 0.0,
    val firstPitchStrikeRate: Double = 0.0,
)

data class PitchingLine(
    val battersFaced: Int = 0,
    val outs: Int = 0,
    val inningsPitched: String = "0.0",
    val pitches: Int = 0,
    val strikes: Int = 0,
    val balls: Int = 0,
    val hitsAllowed: Int = 0,
    val walksAllowed: Int = 0,
    val hitByPitchAllowed: Int = 0,
    val strikeouts: Int = 0,
    val homeRunsAllowed: Int = 0,
    val strikeRate: Double = 0.0,
    val strikeoutRate: Double = 0.0,
    val walkRate: Double = 0.0,
)

data class ExpectedPitchingLine(
    val xBAAllowed: Double = 0.0,
    val xOBPAllowed: Double = 0.0,
    val xWOBAAllowed: Double = 0.0,
    val xSLGAllowed: Double = 0.0,
    val xOPSAllowed: Double = 0.0,
    val xHitsAllowed: Double = 0.0,
    val xTotalBasesAllowed: Double = 0.0,
    val xWeightedTimesOnBaseAllowed: Double = 0.0,
    val expectedRunsAllowed: Double = 0.0,
    val qualityAdjustedRunsAllowed: Double = 0.0,
    val xHomeRunsAllowed: Double = 0.0,
    val contactRunValueAllowed: Double = 0.0,
    val disciplineRunValueAllowed: Double = 0.0,
    val runPreventionValue: Double = 0.0,
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
    val abCount: Int = 0,
    val wobaCount: Int = 0,
    val xBa: Double,
    val wOBA: Double,
    val xSLG: Double,
    val wOPS: Double,
    val expTimesOnBase: Double,
    val expBases: Double,
    val tOPS: Double,
    val maxExitVelo: Double,
    val avgBatSpeed: Double,
    val maxBatSpeed: Double,
    val avgExitVelo: Double,
    val onHomeTeam: Boolean,
    val batting: BattingLine = BattingLine(),
    val expected: ExpectedBattingLine = ExpectedBattingLine(),
    val battedBall: BattedBallProfile = BattedBallProfile(),
    val plateDiscipline: PlateDiscipline = PlateDiscipline(),
)

data class Pitcher(
    val id: Int,
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val primaryNumber: String?,
    val pitchHand: String?,
    val hitsAgainst: Number,
    val strikeouts: Number,
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
    val pitching: PitchingLine = PitchingLine(),
    val expected: ExpectedPitchingLine = ExpectedPitchingLine(),
    val contactAllowed: BattedBallProfile = BattedBallProfile(),
    val plateDiscipline: PlateDiscipline = PlateDiscipline(),
)

data class TeamWrapper(
    val home: Team,
    val away: Team,
)

data class Team(
    val id: Int,
    val name: String,
    val abbreviation: String,
    val runsAgainst: Int,
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
    val expRunsAgainst: Double,
    val batting: BattingLine = BattingLine(),
    val expectedBatting: ExpectedBattingLine = ExpectedBattingLine(),
    val battedBall: BattedBallProfile = BattedBallProfile(),
    val plateDiscipline: PlateDiscipline = PlateDiscipline(),
    val pitching: PitchingLine = PitchingLine(),
    val expectedPitching: ExpectedPitchingLine = ExpectedPitchingLine(),
    val contactAllowed: BattedBallProfile = BattedBallProfile(),
    val expectedOutcome: TeamExpectedOutcome = TeamExpectedOutcome(),
)

data class TeamExpectedOutcome(
    val expectedRunsFor: Double = 0.0,
    val expectedRunsAgainst: Double = 0.0,
    val qualityAdjustedRunsFor: Double = 0.0,
    val qualityAdjustedRunsAgainst: Double = 0.0,
    val expectedRunDifferential: Double = 0.0,
    val qualityAdjustedRunDifferential: Double = 0.0,
    val expectedWinPercentage: Double = 0.5,
    val contactAdvantageRuns: Double = 0.0,
    val disciplineAdvantageRuns: Double = 0.0,
    val deservedRunsAboveActual: Double = 0.0,
    val actualRunsAboveExpected: Double = 0.0,
    val actualRunsAllowedAboveExpected: Double = 0.0,
)
