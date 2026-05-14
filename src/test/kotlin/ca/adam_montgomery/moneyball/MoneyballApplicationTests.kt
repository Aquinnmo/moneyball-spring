package ca.adam_montgomery.moneyball

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

import ca.adam_montgomery.moneyball.structures.*
import org.junit.jupiter.api.Assertions.*

@SpringBootTest
class MoneyballApplicationTests {

	@Test
	fun contextLoads() {
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
            liveData = LiveData(linescore = lineScore, decisions = null)
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
}
