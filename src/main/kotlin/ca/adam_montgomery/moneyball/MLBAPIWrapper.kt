package ca.adam_montgomery.moneyball

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

private const val BASE_URL = "https://statsapi.mlb.com/api/v1/"

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
        val url = "${BASE_URL}schedule?sportId=1"
        return restTemplate.getForObject(url, Schedule::class.java)
    }
}
