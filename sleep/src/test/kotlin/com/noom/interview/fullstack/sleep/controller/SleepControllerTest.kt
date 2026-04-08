import com.noom.interview.fullstack.sleep.SleepApplication
import com.noom.interview.fullstack.sleep.controller.SleepController
import com.noom.interview.fullstack.sleep.service.SleepService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(SleepController::class)
@ContextConfiguration(classes = [SleepApplication::class])
class SleepControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockBean
    private lateinit var sleepService: SleepService

    @Test
    fun `getLatestSleepLog should return 204 when no data found`() {
        val userId = UUID.randomUUID()

        // Mock service to return null
        `when`(sleepService.getMostRecentSleepLog(userId)).thenReturn(null)

        mockMvc.perform(
            get("/api/v1/sleep/latest")
                .header("X-User-Id", userId.toString())
        )
            .andExpect(status().isNoContent) // Verifies your if/else logic in the controller
    }

    @Test
    fun `getLatestSleepLog should return 400 when user header is missing`() {
        mockMvc.perform(get("/api/v1/sleep/latest"))
            .andExpect(status().isBadRequest) // Verifies header 'required = true'
    }
}