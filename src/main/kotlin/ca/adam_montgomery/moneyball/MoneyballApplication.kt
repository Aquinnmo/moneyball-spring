package ca.adam_montgomery.moneyball

import ca.adam_montgomery.moneyball.structures.ProcessedGame
import ca.adam_montgomery.moneyball.structures.Schedule
import ca.adam_montgomery.moneyball.structures.StatcastGame
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
class MoneyballApplication

@Configuration
class CorsConfig : WebMvcConfigurer {
	override fun addCorsMappings(registry: CorsRegistry) {
		registry.addMapping("/**")
			.allowedOrigins(
				"https://adam-montgomery.ca",
				"http://localhost:5173"
			)
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("Authorization", "Content-Type")
			.allowCredentials(true)
			.maxAge(86400)
	}
}


fun main(args: Array<String>) {
	runApplication<MoneyballApplication>(*args)
}

@RestController
class APIInterface(private val statsAPI: StatsApiWrapper, private val statcastAPI: StatcastWrapper) {
	@GetMapping("today-schedule")
	fun todaySchedule(): Schedule? {
		print("hit today's schedule endpoint\n")

		val sched = statsAPI.getSchedule()
		print("successfully got today's schedule\n")
		return sched
	}

	@GetMapping("statcast-game={gamePk}")
	fun gameStatcast(@PathVariable gamePk: Int): StatcastGame? {
		print("hit get data for game $gamePk")
		return statcastAPI.getGameData(gamePk)
	}

	@GetMapping("game={gamePk}")
	fun getGame(@PathVariable gamePk: Int) : ProcessedGame {
		println("getting game data for $gamePk")
		val statcastData = statcastAPI.getGameData(gamePk) ?: throw Exception("no statcast data for $gamePk")
		println("successfully got all statcast data for $gamePk")
		val basicData = statsAPI.getGame(gamePk) ?: throw Exception("no basic data for $gamePk")
		println("successfully got all game data for $gamePk")
		val pitchData = statcastAPI.fetchCSV(gamePk)
		if (pitchData.isEmpty()) {println("empty csv data for $gamePk")}
		println("successfully got all csv data for $gamePk")
		val game = processGame(statcastData, basicData, pitchData)
		print("successfully processed game data for $gamePk")
		return game
	}

	@GetMapping("today-games")
	fun todayGames(): List<ParsedGame>? {
		val sched = todaySchedule()
		print("successfully got today's schedule\n")
		return getGamesFromSchedule(sched)
	}

	fun getGamesFromSchedule(sched: Schedule?): List<ParsedGame>? {

		return sched?.dates?.flatMap { date ->
			date.games.map { game ->
				ParsedGame(game.gamePk, game.teams, game.venue, date.date)
			}
		} ?: emptyList()
	}
}
