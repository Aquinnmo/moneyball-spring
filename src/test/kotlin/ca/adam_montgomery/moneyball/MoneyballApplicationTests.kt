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

        val gameData = GameData(
            datetime = DateTime("2024-05-10", "2024-05-10", "D", "1:00", "PM"),
            status = GameDataStatus("F", "Final", false),
            teams = GameDataTeamWrapper(
                home = GameDataTeam(1, "H", "Home", "Home", "City", Division(1, "East"), TeamRecord(1, 1, 0)),
                away = GameDataTeam(2, "A", "Away", "Away", "City", Division(1, "East"), TeamRecord(1, 0, 1))
            ),
            players = mapOf("ID1" to player1),
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
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = "strikeout", pitchNumber = 3, nPrioirPA = 0, topOfInning = true, pitchName = "Fastball"),
            Pitch(pitcherId = 1, batterId = 2, pitchDelta = 0.0, event = "single", pitchNumber = 1, nPrioirPA = 1, topOfInning = true, pitchName = "Fastball")
        )

        val processed = processGame(statcastGame, basicGame, pitchData)
        val pitcher = processed.pitchers.find { it.id == 1 }
        
        assertNotNull(pitcher)
        assertEquals(1, pitcher?.strikeouts?.toInt())
        assertEquals(1, pitcher?.hitsAgainst?.toInt())
    }
}
