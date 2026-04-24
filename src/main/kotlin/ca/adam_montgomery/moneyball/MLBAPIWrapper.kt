package ca.adam_montgomery.moneyball

import ca.adam_montgomery.moneyball.structures.Schedule
import ca.adam_montgomery.moneyball.structures.GameData
import ca.adam_montgomery.moneyball.structures.StatcastGame
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

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
        val url = "${BASE_URL}v1/schedule?sportId=1"
        return restTemplate.getForObject(url, Schedule::class.java)
    }
}

@Service
class StatcastWrapper(private val restTemplate: RestTemplate) {
    fun getGameData(gamePk:Int): GameData? {
        val url = "${BASE_URL}gf?game_pk=$gamePk"
        return restTemplate.getForObject(url, StatcastGame::class.java)
    }
}
