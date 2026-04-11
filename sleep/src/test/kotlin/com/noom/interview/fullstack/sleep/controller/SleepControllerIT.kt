package com.noom.interview.fullstack.sleep.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SleepControllerIT(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper
) {
    @Test
    fun `single record workflow across all endpoints`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today.minusDays(1), 22, 0),
                wakeTime = utcDateTime(today, 6, 0),
                mood = "GOOD"
            )
        ).andExpect {
            status { isCreated() }
            jsonPath("$.totalDurationSeconds") { value(28800) }
        }

        getLatestSleepLog(userId).andExpect {
            status { isOk() }
            jsonPath("$.mood") { value("GOOD") }
            jsonPath("$.logDate") { value(today.toString()) }
        }

        getSleepStats(userId).andExpect {
            status { isOk() }
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }
            jsonPath("$.avgBedTime") { value(org.hamcrest.Matchers.containsString("22:00")) }
            jsonPath("$.avgWakeTime") { value(org.hamcrest.Matchers.containsString("06:00")) }
            jsonPath("$.moodFrequencies.GOOD") { value(1) }
            jsonPath("$.moodFrequencies.OK") { value(0) }
            jsonPath("$.moodFrequencies.BAD") { value(0) }
            jsonPath("$.startDate") { value(today.toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    @Test
    fun `averages for non circular wake times`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(1), 3, 0),
                wakeTime = utcDateTime(today.minusDays(1), 11, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today, 5, 0),
                wakeTime = utcDateTime(today, 13, 0),
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }
            jsonPath("$.avgWakeTime") { value("12:00Z") }
            jsonPath("$.avgBedTime") { value("04:00Z") }
            jsonPath("$.moodFrequencies.GOOD") { value(1) }
            jsonPath("$.moodFrequencies.OK") { value(1) }
            jsonPath("$.moodFrequencies.BAD") { value(0) }
            jsonPath("$.startDate") { value(today.minusDays(1).toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    @Test
    fun `aggregated stats handles UTC and circular averages correctly`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(1), 23, 0),
                wakeTime = utcDateTime(today, 7, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today, 1, 0),
                wakeTime = utcDateTime(today, 7, 0),
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }
            jsonPath("$.avgTotalTimeSeconds") { value(25200) }
            jsonPath("$.avgBedTime") { value("00:00Z") }
            jsonPath("$.avgWakeTime") { value("07:00Z") }
            jsonPath("$.moodFrequencies.GOOD") { value(1) }
            jsonPath("$.moodFrequencies.OK") { value(1) }
            jsonPath("$.moodFrequencies.BAD") { value(0) }
            jsonPath("$.startDate") { value(today.minusDays(1).toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    @Test
    fun `cross-midnight duration is calculated correctly`() {
        val userId = UUID.randomUUID()

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = LocalDate.parse("2024-01-15"),
                bedTime = OffsetDateTime.parse("2024-01-14T21:45:00Z"),
                wakeTime = OffsetDateTime.parse("2024-01-15T06:15:00Z"),
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        getLatestSleepLog(userId).andExpect {
            status { isOk() }
            jsonPath("$.totalDurationSeconds") { value(30600) }
        }
    }

    @Test
    fun `stats should only aggregate logs from the last 30 days`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(5),
                bedTime = utcDateTime(today.minusDays(6), 22, 0),
                wakeTime = utcDateTime(today.minusDays(5), 8, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(65),
                bedTime = utcDateTime(today.minusDays(65), 0, 0),
                wakeTime = utcDateTime(today.minusDays(65), 4, 0),
                mood = "BAD"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }
            jsonPath("$.avgTotalTimeSeconds") { value(36000) }
            jsonPath("$.moodFrequencies.GOOD") { value(1) }
            jsonPath("$.moodFrequencies.BAD") { value(0) }
            jsonPath("$.startDate") { value(today.minusDays(5).toString()) }
            jsonPath("$.endDate") { value(today.minusDays(5).toString()) }
        }
    }

    @Test
    fun `stats log exactly 30 days ago is included`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(30),
                bedTime = utcDateTime(today.minusDays(30), 22, 0),
                wakeTime = utcDateTime(today.minusDays(29), 6, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }
        }
    }

    @Test
    fun `stats when bed times are exactly 12 hours apart`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(1), 22, 0),
                wakeTime = utcDateTime(today, 6, 0),
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today, 10, 0),
                wakeTime = utcDateTime(today, 16, 0),
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }
            jsonPath("$.avgBedTime") { value("04:00Z") }
        }
    }

    @Test
    fun `stats uses circular average for bed times 20 22 and 02 UTC`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(2),
                bedTime = utcDateTime(today.minusDays(2), 20, 0),
                wakeTime = utcDateTime(today.minusDays(1), 4, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(1), 22, 0),
                wakeTime = utcDateTime(today, 6, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today, 2, 0),
                wakeTime = utcDateTime(today, 10, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }
            jsonPath("$.avgBedTime") { value("22:35Z") }
            jsonPath("$.moodFrequencies.GOOD") { value(3) }
        }
    }

    @Test
    fun `stats for typical normal sleep pattern`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(4),
                bedTime = utcDateTime(today.minusDays(5), 22, 0),   // 10:00 PM
                wakeTime = utcDateTime(today.minusDays(4), 6, 0),   // 8h
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(3),
                bedTime = utcDateTime(today.minusDays(4), 21, 30),  // 9:30 PM
                wakeTime = utcDateTime(today.minusDays(3), 5, 30),  // 8h
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(2),
                bedTime = utcDateTime(today.minusDays(3), 23, 0),   // 11:00 PM
                wakeTime = utcDateTime(today.minusDays(2), 7, 0),   // 8h
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(2), 22, 45),  // 10:45 PM
                wakeTime = utcDateTime(today.minusDays(1), 6, 45),  // 8h
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today.minusDays(1), 23, 30),  // 11:30 PM
                wakeTime = utcDateTime(today, 7, 30),               // 8h
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }

            // All durations are 8h
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }

            // Average bedtime should stay around late evening
            jsonPath("$.avgBedTime") { value("22:33Z") }

            // Average wake time should be around morning
            jsonPath("$.avgWakeTime") { value("06:33Z") }

            // Mood counts
            jsonPath("$.moodFrequencies.GOOD") { value(3) }
            jsonPath("$.moodFrequencies.OK") { value(2) }
            jsonPath("$.moodFrequencies.BAD") { value(0) }

            // Date range
            jsonPath("$.startDate") { value(today.minusDays(4).toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    @Test
    fun `stats for bed times between 10pm and 2am (circular case)`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(4),
                bedTime = utcDateTime(today.minusDays(5), 22, 0),  // 22:00
                wakeTime = utcDateTime(today.minusDays(4), 6, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(3),
                bedTime = utcDateTime(today.minusDays(4), 23, 0),  // 23:00
                wakeTime = utcDateTime(today.minusDays(3), 7, 0),
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(2),
                bedTime = utcDateTime(today.minusDays(2), 0, 0),   // 00:00
                wakeTime = utcDateTime(today.minusDays(2), 8, 0),
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(1), 1, 0),   // 01:00
                wakeTime = utcDateTime(today.minusDays(1), 9, 0),
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today, 2, 0),                // 02:00
                wakeTime = utcDateTime(today, 10, 0),
                mood = "BAD"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }

            // All durations = 8h
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }

            // Circular average centered around midnight
            jsonPath("$.avgBedTime") { value("00:00Z") }

            // Wake times should average to ~08:00
            jsonPath("$.avgWakeTime") { value("08:00Z") }

            // Mood counts
            jsonPath("$.moodFrequencies.GOOD") { value(2) }
            jsonPath("$.moodFrequencies.OK") { value(2) }
            jsonPath("$.moodFrequencies.BAD") { value(1) }

            // Date range
            jsonPath("$.startDate") { value(today.minusDays(4).toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    @Test
    fun `stats for night owl sleep pattern`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(4),
                bedTime = utcDateTime(today.minusDays(4), 0, 30),   // 12:30 AM
                wakeTime = utcDateTime(today.minusDays(4), 8, 30),  // 8h
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(3),
                bedTime = utcDateTime(today.minusDays(3), 1, 0),    // 1:00 AM
                wakeTime = utcDateTime(today.minusDays(3), 9, 0),   // 8h
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(2),
                bedTime = utcDateTime(today.minusDays(2), 1, 30),   // 1:30 AM
                wakeTime = utcDateTime(today.minusDays(2), 9, 30),  // 8h
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(1), 2, 0),    // 2:00 AM
                wakeTime = utcDateTime(today.minusDays(1), 10, 0),  // 8h
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today, 2, 30),                // 2:30 AM
                wakeTime = utcDateTime(today, 10, 30),              // 8h
                mood = "BAD"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }

            // All durations are 8 hours
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }

            // Average of 00:30, 01:00, 01:30, 02:00, 02:30 = 01:30
            jsonPath("$.avgBedTime") { value("01:30Z") }

            // Average of 08:30, 09:00, 09:30, 10:00, 10:30 = 09:30
            jsonPath("$.avgWakeTime") { value("09:30Z") }

            // Mood frequencies
            jsonPath("$.moodFrequencies.GOOD") { value(2) }
            jsonPath("$.moodFrequencies.OK") { value(2) }
            jsonPath("$.moodFrequencies.BAD") { value(1) }

            // Date range
            jsonPath("$.startDate") { value(today.minusDays(4).toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    @Test
    fun `stats for day sleeper pattern`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(4),
                bedTime = utcDateTime(today.minusDays(4), 8, 0),    // 8:00 AM
                wakeTime = utcDateTime(today.minusDays(4), 16, 0),  // 4:00 PM
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(3),
                bedTime = utcDateTime(today.minusDays(3), 9, 0),    // 9:00 AM
                wakeTime = utcDateTime(today.minusDays(3), 17, 0),  // 5:00 PM
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(2),
                bedTime = utcDateTime(today.minusDays(2), 10, 0),   // 10:00 AM
                wakeTime = utcDateTime(today.minusDays(2), 18, 0),  // 6:00 PM
                mood = "GOOD"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today.minusDays(1),
                bedTime = utcDateTime(today.minusDays(1), 11, 0),   // 11:00 AM
                wakeTime = utcDateTime(today.minusDays(1), 19, 0),  // 7:00 PM
                mood = "OK"
            )
        ).andExpect { status { isCreated() } }

        createSleepLog(
            userId,
            sleepLogRequest(
                logDate = today,
                bedTime = utcDateTime(today, 12, 0),                // 12:00 PM
                wakeTime = utcDateTime(today, 20, 0),               // 8:00 PM
                mood = "BAD"
            )
        ).andExpect { status { isCreated() } }

        getSleepStats(userId).andExpect {
            status { isOk() }

            // All durations are 8 hours
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }

            // Average of 08:00, 09:00, 10:00, 11:00, 12:00 = 10:00
            jsonPath("$.avgBedTime") { value("10:00Z") }

            // Average of 16:00, 17:00, 18:00, 19:00, 20:00 = 18:00
            jsonPath("$.avgWakeTime") { value("18:00Z") }

            // Mood frequencies
            jsonPath("$.moodFrequencies.GOOD") { value(2) }
            jsonPath("$.moodFrequencies.OK") { value(2) }
            jsonPath("$.moodFrequencies.BAD") { value(1) }

            // Date range
            jsonPath("$.startDate") { value(today.minusDays(4).toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    private fun createSleepLog(userId: UUID, body: Map<String, String>) =
        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }

    private fun getLatestSleepLog(userId: UUID) =
        mockMvc.get("/api/v1/sleep/latest") {
            header("X-User-Id", userId.toString())
        }

    private fun getSleepStats(userId: UUID) =
        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId.toString())
        }

    private fun sleepLogRequest(
        logDate: LocalDate,
        bedTime: OffsetDateTime,
        wakeTime: OffsetDateTime,
        mood: String
    ): Map<String, String> {
        return mapOf(
            "logDate" to logDate.toString(),
            "bedTime" to bedTime.toString(),
            "wakeTime" to wakeTime.toString(),
            "mood" to mood
        )
    }

    private fun utcDateTime(date: LocalDate, hour: Int, minute: Int): OffsetDateTime {
        return date.atTime(hour, minute).atOffset(ZoneOffset.UTC)
    }
}