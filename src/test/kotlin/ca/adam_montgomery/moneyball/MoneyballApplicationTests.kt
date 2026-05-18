package ca.adam_montgomery.moneyball

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

import ca.adam_montgomery.moneyball.structures.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.web.client.RestTemplate

@SpringBootTest
class MoneyballApplicationTests {

	@Test
    fun contextLoads() {
	}

    @Test
    fun `parsed games use official date and parsed UTC start time`() {
        val api = APIInterface(StatsApiWrapper(RestTemplate()), StatcastWrapper(RestTemplate()))
        val schedule = Schedule(
            totalItems = 1,
            totalEvents = 0,
            totalGames = 1,
            totalGamesInProgress = 0,
            dates = listOf(
                ScheduleDate(
                    date = "2024-05-10",
                    totalItems = 1,
                    totalEvents = 0,
                    totalGamesInProgress = 0,
                    games = listOf(
                        Game(
                            gamePk = 745421,
                            gameGuid = "f8622a09-ec66-4cc8-bb70-697adeed7ced",
                            link = "/api/v1.1/game/745421/feed/live",
                            gameType = "R",
                            season = "2024",
                            gameDate = "2024-05-11T01:40:00Z",
                            officialDate = "2024-05-10",
                            status = Status(
                                abstractGameState = "Final",
                                codedGameState = "F",
                                detailedState = "Final",
                                statusCode = "F",
                                startTimeTBD = false,
                                abstractGameCode = "F",
                            ),
                            teams = Teams(
                                away = TeamGameData(
                                    team = TeamInfo(119, "Los Angeles Dodgers"),
                                    leagueRecord = LeagueRecord(26, 14, ".650"),
                                    splitSquad = false,
                                    seriesNumber = 14,
                                ),
                                home = TeamGameData(
                                    team = TeamInfo(135, "San Diego Padres"),
                                    leagueRecord = LeagueRecord(21, 20, ".512"),
                                    splitSquad = false,
                                    seriesNumber = 14,
                                ),
                            ),
                            venue = Venue(2680, "Petco Park"),
                            gameNumber = 1,
                            publicFacing = "true",
                            gamedayType = "P",
                            tiebreaker = "N",
                            calendarEventID = "14-745421-2024-05-10",
                            seasonDisplay = "2024",
                            dayNight = "night",
                            scheduledInnings = 9,
                            reverseHomeAwayStatus = false,
                            inningBreakLength = 120,
                            gamesInSeries = 3,
                            seriesGameNumber = 1,
                            seriesDescription = "Regular Season",
                            recordSource = "S",
                        ),
                    ),
                ),
            ),
        )

        val parsed = api.getGamesFromSchedule(schedule)?.single()

        assertEquals("2024-05-10", parsed?.gameDate)
        assertEquals("2024-05-10", parsed?.officialDate)
        assertEquals("2024-05-11T01:40:00Z", parsed?.scheduledStartUtc)
        assertEquals("01:40:00", parsed?.scheduledStartTimeUtc)
    }

    @Test
    fun testProcessGameStrikeouts() {
        val statcastGame = StatcastGame(
            game_status_code = "F",
            game_status = "Final",
            gamedayType = "P",
            gameDate = "2024-05-10",
            hasAbs = true,
            scoreboard = null,
            venue_id = 1,
            away_lineup = listOf(1),
            home_lineup = listOf(2),
            away_pitcher_lineup = listOf(2),
            home_pitcher_lineup = listOf(1)
        )

        // Mocking Player
        val player1 = Player(
            id = 1,
            fullName = "Pitcher One",
            firstName = "Pitcher",
            lastName = "One",
            primaryNumber = "10",
            active = true,
            primaryPosition = Position("P", "Pitcher", "Pitcher", "P"),
            boxscoreName = "P. One",
            batHand = Sidedness("R", "Right"),
            pitchHand = Sidedness("R", "Right")
        )

        val player2 = Player(
            id = 2,
            fullName = "Batter Two",
            firstName = "Batter",
            lastName = "Two",
            primaryNumber = "20",
            active = true,
            primaryPosition = Position("C", "Catcher", "Catcher", "C"),
            boxscoreName = "B. Two",
            batHand = Sidedness("L", "Left"),
            pitchHand = Sidedness("R", "Right")
        )

        val gameData = GameData(
            datetime = DateTime("2024-05-10", "2024-05-10", "D", "1:00", "PM"),
            status = GameDataStatus("F", "Final", false),
            teams = GameDataTeamWrapper(
                home = GameDataTeam(1, "H", "Home", "Home", "City", Division(1, "East"), TeamRecord(1, 1, 0)),
                away = GameDataTeam(2, "A", "Away", "Away", "City", Division(1, "East"), TeamRecord(1, 0, 1))
            ),
            players = mapOf("ID1" to player1, "ID2" to player2),
            probablePitchers = ProbablePitchersWrapper(ProbablePitcher(1, "P. One"), ProbablePitcher(2, "P. Two"))
        )

        val lineScore = LineScore(
            currentInning = 9,
            currentInningOrdinal = "9th",
            inningState = "Final",
            isTopInning = false,
            scheduledInnings = 9,
            innings = emptyList(),
            teams = TeamBoxScore(
                home = InningStats(1, 1, 0, 0),
                away = InningStats(0, 0, 0, 0)
            )
        )

        val basicGame = BasicGame(
            gamePk = 1,
            metadata = null,
            gameData = gameData,
            liveData = LiveData(
                linescore = lineScore,
                decisions = null,
                boxscore = Boxscore(
                    teams = BoxscoreTeams(
                        home = BoxscoreTeam(players = mapOf("ID1" to BoxscorePlayer())),
                        away = BoxscoreTeam(
                            players = mapOf(
                                "ID2" to BoxscorePlayer(
                                    stats = BoxscoreStats(
                                        batting = BoxscoreBatting(runs = 1),
                                        fielding = BoxscoreFielding(errors = 1),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        )

        val pitchData = listOf(
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, pitchNumber = 1, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "called_strike"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, pitchNumber = 2, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "B", description = "ball"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = "strikeout", pitchNumber = 3, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "swinging_strike"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = "single", pitchNumber = 1, nPrioirPA = 1, topOfInning = true, pitchName = "Fastball", estBA = 0.8, estSLG = 1.5, estWOBA = 0.7, launchAngle = 20.0, exitVelo = 100.0, pitchResult = "X", description = "hit_into_play", launchSpeedAngle = 6)
        )

        val processed = processGame(statcastGame, basicGame, pitchData)
        val pitcher = processed.pitchers.find { it.id == 1 }
        
        assertNotNull(pitcher)
        assertEquals(1, pitcher?.strikeouts?.toInt())
        assertEquals(1, pitcher?.hitsAgainst?.toInt())
        assertEquals(4, pitcher?.pitching?.pitches)
        assertEquals(1, pitcher?.plateDiscipline?.whiffs)

        val batter = processed.batters.find { it.id == 2 }
        assertNotNull(batter)
        assertEquals(2, batter?.batting?.plateAppearances)
        assertEquals(1, batter?.batting?.hits)
        assertEquals(1, batter?.runs)
        assertEquals(1, batter?.errors)
        assertEquals(1, batter?.battedBall?.hardHitBalls)
        assertEquals(1, batter?.battedBall?.barrels)
        assertEquals(0.5, batter?.batting?.battingAverage ?: 0.0, 0.0001)
        assertTrue((batter?.expected?.xRunsCreated ?: 0.0) > 0.0)
        assertTrue((batter?.expected?.qualityAdjustedRuns ?: 0.0) > (batter?.expected?.xRunsCreated ?: 0.0))

        assertEquals(1, processed.teams.home.pitching.strikeouts)
        assertTrue((processed.teams.away.expWin ?: 0.0) > (processed.teams.home.expWin ?: 0.0))
        assertEquals(processed.teams.away.expWin ?: 0.0, processed.summary.expectedOutcome.awayExpectedWinPercentage, 0.0001)
        assertTrue(processed.summary.expectedOutcome.awayDeservedRunDifferential > 0.0)
        assertEquals(1.0, processed.summary.shares.hardHitBalls.away, 0.0001)
        assertEquals(2, processed.summary.leaders.topBattersByHardHitRate.first().id)
    }

    @Test
    fun `process game tracks RISP scoring chances and conversions`() {
        val statcastGame = StatcastGame(
            game_status_code = "F",
            game_status = "Final",
            gamedayType = "P",
            gameDate = "2024-05-10",
            hasAbs = true,
            scoreboard = null,
            venue_id = 1,
            away_lineup = listOf(2),
            home_lineup = listOf(1),
            away_pitcher_lineup = listOf(2),
            home_pitcher_lineup = listOf(1)
        )

        val pitcher = Player(
            id = 1,
            fullName = "Pitcher One",
            firstName = "Pitcher",
            lastName = "One",
            primaryNumber = "10",
            active = true,
            primaryPosition = Position("P", "Pitcher", "Pitcher", "P"),
            boxscoreName = "P. One",
            batHand = Sidedness("R", "Right"),
            pitchHand = Sidedness("R", "Right")
        )
        val batter = Player(
            id = 2,
            fullName = "Batter Two",
            firstName = "Batter",
            lastName = "Two",
            primaryNumber = "20",
            active = true,
            primaryPosition = Position("C", "Catcher", "Catcher", "C"),
            boxscoreName = "B. Two",
            batHand = Sidedness("L", "Left"),
            pitchHand = Sidedness("R", "Right")
        )

        val basicGame = BasicGame(
            gamePk = 1,
            metadata = null,
            gameData = GameData(
                datetime = DateTime("2024-05-10", "2024-05-10", "D", "1:00", "PM"),
                status = GameDataStatus("F", "Final", false),
                teams = GameDataTeamWrapper(
                    home = GameDataTeam(1, "H", "Home", "Home", "City", Division(1, "East"), TeamRecord(1, 1, 0)),
                    away = GameDataTeam(2, "A", "Away", "Away", "City", Division(1, "East"), TeamRecord(1, 0, 1))
                ),
                players = mapOf("ID1" to pitcher, "ID2" to batter),
                probablePitchers = ProbablePitchersWrapper(ProbablePitcher(1, "P. One"), ProbablePitcher(2, "B. Two"))
            ),
            liveData = LiveData(
                linescore = LineScore(
                    currentInning = 9,
                    currentInningOrdinal = "9th",
                    inningState = "Final",
                    isTopInning = false,
                    scheduledInnings = 9,
                    innings = emptyList(),
                    teams = TeamBoxScore(
                        home = InningStats(0, 1, 0, 0),
                        away = InningStats(2, 1, 0, 0)
                    )
                ),
                decisions = null
            )
        )

        val pitchData = listOf(
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 10.0, pitchNumber = 1, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 20.0, pitchNumber = 2, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 30.0, pitchNumber = 3, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 40.0, pitchNumber = 4, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 50.0, pitchNumber = 5, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 60.0, pitchNumber = 6, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 70.0, pitchNumber = 7, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 80.0, pitchNumber = 8, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = null, runnerOnThird = 99, batSpeed = 90.0, pitchNumber = 9, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball", pitchResult = "S", description = "foul"),
            Pitch(
                pitcherId = 1,
                batterId = 2,
                pitchDelta = 0.0,
                event = "field_error",
                runnerOnThird = 99,
                batSpeed = 100.0,
                batScore = 0,
                postBatScore = 1,
                pitchNumber = 10,
                nPrioirPA = 0,
                topOfInning = true,
                pitchName = "Fastball",
                pitchResult = "X",
                description = "hit_into_play",
            ),
            Pitch(
                pitcherId = 1,
                batterId = 2,
                pitchDelta = 0.0,
                event = "field_out",
                runnerOnSecond = 98,
                batScore = 1,
                postBatScore = 1,
                pitchNumber = 1,
                nPrioirPA = 1,
                topOfInning = true,
                pitchName = "Fastball",
                pitchResult = "X",
                description = "hit_into_play",
            ),
            Pitch(
                pitcherId = 1,
                batterId = 2,
                pitchDelta = 0.0,
                event = null,
                batScore = 1,
                postBatScore = 1,
                pitchNumber = 1,
                nPrioirPA = 2,
                topOfInning = true,
                pitchName = "Fastball",
                pitchResult = "B",
                description = "ball",
            ),
            Pitch(
                pitcherId = 1,
                batterId = 2,
                pitchDelta = 0.0,
                event = "single",
                runnerOnSecond = 98,
                batScore = 1,
                postBatScore = 2,
                pitchNumber = 2,
                nPrioirPA = 2,
                topOfInning = true,
                pitchName = "Fastball",
                pitchResult = "X",
                description = "hit_into_play",
            )
        )

        val processed = processGame(statcastGame, basicGame, pitchData)
        val batterStats = processed.batters.single { it.id == 2 }

        assertEquals(2, batterStats.rispPlateAppearances)
        assertEquals(1, batterStats.rispConversions)
        assertEquals(0.5, batterStats.rispConversionRate, 0.0001)
        assertEquals(60.0, batterStats.avgBatSpeed, 0.0001)
        assertEquals(100.0, batterStats.maxBatSpeed, 0.0001)
        assertEquals(2, processed.teams.away.scoringChances)
        assertEquals(1, processed.teams.away.scoringChanceConversions)
        assertEquals(0.5, processed.teams.away.scoringChanceConversionRate, 0.0001)
        assertEquals(0, processed.teams.home.scoringChances)
    }

    @Test
    fun `process game uses accurate stat denominators and pitch row filtering`() {
        val statcastGame = StatcastGame(
            game_status_code = "F",
            game_status = "Final",
            gamedayType = "P",
            gameDate = "2024-05-10",
            hasAbs = true,
            scoreboard = null,
            venue_id = 1,
            away_lineup = listOf(2, 3),
            home_lineup = listOf(1),
            away_pitcher_lineup = listOf(2, 3),
            home_pitcher_lineup = listOf(1)
        )

        val pitcher = Player(
            id = 1,
            fullName = "Pitcher One",
            firstName = "Pitcher",
            lastName = "One",
            primaryNumber = "10",
            active = true,
            primaryPosition = Position("P", "Pitcher", "Pitcher", "P"),
            boxscoreName = "P. One",
            batHand = Sidedness("R", "Right"),
            pitchHand = Sidedness("R", "Right")
        )
        val batterOne = Player(
            id = 2,
            fullName = "Batter Two",
            firstName = "Batter",
            lastName = "Two",
            primaryNumber = "20",
            active = true,
            primaryPosition = Position("C", "Catcher", "Catcher", "C"),
            boxscoreName = "B. Two",
            batHand = Sidedness("L", "Left"),
            pitchHand = Sidedness("R", "Right")
        )
        val batterTwo = Player(
            id = 3,
            fullName = "Batter Three",
            firstName = "Batter",
            lastName = "Three",
            primaryNumber = "30",
            active = true,
            primaryPosition = Position("1B", "First Base", "Infielder", "1B"),
            boxscoreName = "B. Three",
            batHand = Sidedness("R", "Right"),
            pitchHand = Sidedness("R", "Right")
        )

        val basicGame = BasicGame(
            gamePk = 1,
            metadata = null,
            gameData = GameData(
                datetime = DateTime("2024-05-10", "2024-05-10", "D", "1:00", "PM"),
                status = GameDataStatus("F", "Final", false),
                teams = GameDataTeamWrapper(
                    home = GameDataTeam(1, "H", "Home", "Home", "City", Division(1, "East"), TeamRecord(1, 1, 0)),
                    away = GameDataTeam(2, "A", "Away", "Away", "City", Division(1, "East"), TeamRecord(1, 0, 1))
                ),
                players = mapOf("ID1" to pitcher, "ID2" to batterOne, "ID3" to batterTwo),
                probablePitchers = ProbablePitchersWrapper(ProbablePitcher(1, "P. One"), ProbablePitcher(2, "B. Two"))
            ),
            liveData = LiveData(
                linescore = LineScore(
                    currentInning = 9,
                    currentInningOrdinal = "9th",
                    inningState = "Final",
                    isTopInning = false,
                    scheduledInnings = 9,
                    innings = emptyList(),
                    teams = TeamBoxScore(
                        home = InningStats(0, 1, 0, 0),
                        away = InningStats(1, 1, 0, 0)
                    )
                ),
                decisions = null
            )
        )

        val pitchData = listOf(
            Pitch(
                pitcherId = 1,
                batterId = 2,
                pitchDelta = 0.4,
                event = "single",
                estBA = 1.0,
                estSLG = 1.0,
                estWOBA = 0.9,
                exitVelo = 100.0,
                launchAngle = 10.0,
                pitchNumber = 1,
                nPrioirPA = 0,
                topOfInning = true,
                pitchName = "Fastball",
                pitchResult = "X",
                description = "hit_into_play",
            ),
            Pitch(
                pitcherId = 1,
                batterId = 2,
                pitchDelta = 0.0,
                event = "stolen_base_2b",
                pitchNumber = 2,
                nPrioirPA = 0,
                topOfInning = true,
                pitchName = "Unknown",
            ),
            Pitch(
                pitcherId = 1,
                batterId = 2,
                pitchDelta = 0.1,
                event = "intent_walk",
                pitchNumber = 1,
                nPrioirPA = 1,
                topOfInning = true,
                pitchName = "Intentional Ball",
                pitchResult = "B",
                description = "intent_ball",
            ),
            Pitch(
                pitcherId = 1,
                batterId = 3,
                pitchDelta = -0.2,
                event = "strikeout_double_play",
                pitchNumber = 1,
                nPrioirPA = 0,
                topOfInning = true,
                pitchName = "Slider",
                pitchResult = "S",
                description = "swinging_strike",
            ),
            Pitch(
                pitcherId = 1,
                batterId = 3,
                pitchDelta = -0.1,
                event = "field_out",
                estBA = 0.0,
                estSLG = 0.0,
                estWOBA = 0.1,
                launchAngle = 30.0,
                pitchNumber = 1,
                nPrioirPA = 1,
                topOfInning = true,
                pitchName = "Sinker",
                pitchResult = "X",
                description = "hit_into_play",
            )
        )

        val processed = processGame(statcastGame, basicGame, pitchData)
        val away = processed.teams.away
        val homePitcher = processed.pitchers.single { it.id == 1 }

        assertEquals(4, away.batting.plateAppearances)
        assertEquals(3, away.batting.atBats)
        assertEquals(1, away.batting.strikeouts)
        assertEquals(1, processed.teams.home.pitching.strikeouts)
        assertEquals(4, processed.teams.home.pitching.pitches)
        assertEquals(3, processed.teams.home.pitching.outs)
        assertEquals("1.0", processed.teams.home.pitching.inningsPitched)

        assertEquals(2, away.battedBall.ballsInPlay)
        assertEquals(100.0, away.battedBall.avgExitVelo, 0.0001)
        assertEquals(20.0, away.battedBall.avgLaunchAngle, 0.0001)
        assertEquals(1.0 / 3.0, away.expectedBatting.xWOBA, 0.0001)
        assertEquals(away.expectedBatting.xWOBA, away.wOBA, 0.0001)
        assertEquals(homePitcher.expected.expectedRunsAllowed, homePitcher.expRunsAgainst, 0.0001)
        assertEquals(
            homePitcher.expected.expectedRunsAllowed,
            processed.summary.leaders.topPitchersByExpectedRunsAllowed.single().value,
            0.0001,
        )
    }
}
