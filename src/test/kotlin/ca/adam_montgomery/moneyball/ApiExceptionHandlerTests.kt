package ca.adam_montgomery.moneyball

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestClientException

class ApiExceptionHandlerTests {
    private val statsApi = mock(StatsApiWrapper::class.java)
    private val statcastApi = mock(StatcastWrapper::class.java)
    private val mockMvc = MockMvcBuilders
        .standaloneSetup(APIInterface(statsApi, statcastApi))
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun `invalid schedule date returns bad request problem detail`() {
        mockMvc.perform(get("/schedule/not-a-date"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value("Invalid date 'not-a-date'. Use YYYY-MM-DD."))
    }

    @Test
    fun `missing game data returns not found problem detail`() {
        `when`(statcastApi.getGameData(123)).thenReturn(null)

        mockMvc.perform(get("/game=123"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.detail").value("No Statcast data found for game 123"))
    }

    @Test
    fun `upstream client errors return bad gateway problem detail`() {
        `when`(statsApi.getSchedule()).thenThrow(RestClientException("upstream failed"))

        mockMvc.perform(get("/today-schedule"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.status").value(502))
            .andExpect(jsonPath("$.title").value("Bad Gateway"))
            .andExpect(jsonPath("$.detail").value("Unable to fetch data from MLB or Statcast."))
    }
}
