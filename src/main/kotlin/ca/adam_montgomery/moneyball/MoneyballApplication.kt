package ca.adam_montgomery.moneyball

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class MoneyballApplication

fun main(args: Array<String>) {
	runApplication<MoneyballApplication>(*args)
}

@RestController
class HelloWorldController(private val mlbApiWrapper: MLBAPIWrapper) {

	@GetMapping("/hello-world")
	fun hello(): String {
		print("hit hello world endpoint")
		return "Hello World!"
	}

	@GetMapping("today-schedule")
	fun todaySchedule(): Schedule? {
		print("hit today's schedule endpoint")

		val sched = mlbApiWrapper.getSchedule()
		print("successfully got today's schedule")
		return sched
	}
}
