package ca.adam_montgomery.moneyball

import ca.adam_montgomery.moneyball.structures.Schedule
import ca.adam_montgomery.moneyball.structures.GameData
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

private const val BASE_URL = "https://statsapi.mlb.com/api/"

@Configuration
class AppConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}

@Service
class MLBAPIWrapper(private val restTemplate: RestTemplate) {
    fun getSchedule(): Schedule? {
        val url = "${BASE_URL}v1/schedule?sportId=1"
        return restTemplate.getForObject(url, Schedule::class.java)
    }

    fun getGameData(gamePk:Int): GameData? {
        val url = "${BASE_URL}v1.1/game/${gamePk}/feed/live"
        return restTemplate.getForObject(url, GameData::class.java)
    }
}
