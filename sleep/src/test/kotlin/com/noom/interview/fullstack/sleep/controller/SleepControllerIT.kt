package com.noom.interview.fullstack.sleep.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import java.util.UUID
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SleepControllerIT(@Autowired val mockMvc: MockMvc, @Autowired val objectMapper: ObjectMapper) {
    @Test
    fun `test single record workflow across all endpoints`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        // 1. Setup Data: 8 hours sleep (10 PM to 6 AM)
        val bedTime = today.minusDays(1).atTime(22, 0).atOffset(ZoneOffset.UTC)
        val wakeTime = today.atTime(6, 0).atOffset(ZoneOffset.UTC)

        val createRequest = mapOf(
            "logDate" to today.toString(),
            "bedTime" to bedTime.toString(),
            "wakeTime" to wakeTime.toString(),
            "mood" to "GOOD"
        )

        // --- STEP 1: POST (Create) ---
        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.totalDurationSeconds") { value(28800) } // 8h * 3600
        }

        // --- STEP 2: GET LATEST ---
        mockMvc.get("/api/v1/sleep/latest") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.mood") { value("GOOD") }
            jsonPath("$.logDate") { value(today.toString()) }
        }

        // --- STEP 3: GET STATS ---
        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            content {
                // Averages should equal the single record exactly
                jsonPath("$.avgTotalTimeSeconds") { value(28800) }

                // Using a flexible match to handle "06:00Z" vs "06:00:00Z"
                jsonPath("$.avgBedTime") { value(org.hamcrest.Matchers.containsString("22:00")) }
                jsonPath("$.avgWakeTime") { value(org.hamcrest.Matchers.containsString("06:00")) }

                // Frequencies
                jsonPath("$.moodFrequencies.GOOD") { value(1) }
                jsonPath("$.moodFrequencies.OK") { value(0) }

                // Range
                jsonPath("$.startDate") { value(today.toString()) }
                jsonPath("$.endDate") { value(today.toString()) }
            }
        }
    }

    @Test
    fun `test aggregated stats handles UTC and circular averages correctly`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        // Log 1: 8 hours sleep (23:00 to 07:00)
        val log1 = mapOf(
            "logDate" to today.minusDays(1).toString(),
            "bedTime" to today.minusDays(1).atTime(23, 0).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.atTime(7, 0).atOffset(ZoneOffset.UTC).toString(),
            "mood" to "GOOD"
        )

        // Log 2: 6 hours sleep (01:00 to 07:00)
        val log2 = mapOf(
            "logDate" to today.toString(),
            "bedTime" to today.atTime(1, 0).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.atTime(7, 0).atOffset(ZoneOffset.UTC).toString(),
            "mood" to "OK"
        )

        // POST both logs
        listOf(log1, log2).forEach { body ->
            mockMvc.post("/api/v1/sleep") {
                header("X-User-Id", userId)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect { status { isCreated() } }
        }

        // GET and Assert Stats
        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            content {
                // Average of 8h (28800s) and 6h (21600s) = 7h (25200s)
                jsonPath("$.avgTotalTimeSeconds") { value(25200) }

                // Average of 23:00 and 01:00 should be 00:00:00Z
                jsonPath("$.avgBedTime") { value("00:00Z") }

                // Average of 07:00 and 07:00 should be 07:00:00Z
                jsonPath("$.avgWakeTime") { value("07:00Z") }

                // Check frequencies
                jsonPath("$.moodFrequencies.GOOD") { value(1) }
                jsonPath("$.moodFrequencies.OK") { value(1) }
                jsonPath("$.moodFrequencies.BAD") { value(0) }

                // Check date range
                jsonPath("$.startDate") { value(today.minusDays(1).toString()) }
                jsonPath("$.endDate") { value(today.toString()) }
            }
        }
    }

    @Test
    fun `get stats should only aggregate logs from the last 30 days`() {
        val userId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()

        // 1. DATA TO BE INCLUDED (Recent: 5 days ago)
        // 10 hours sleep: 22:00 -> 08:00
        val recentLog = mapOf(
            "logDate" to today.minusDays(5).toString(),
            "bedTime" to today.minusDays(6).atTime(22, 0).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.minusDays(5).atTime(8, 0).atOffset(ZoneOffset.UTC).toString(),
            "mood" to "GOOD"
        )

        // 2. DATA TO BE EXCLUDED (Old: 65 days ago)
        // 4 hours sleep: 00:00 -> 04:00
        val oldLog = mapOf(
            "logDate" to today.minusDays(65).toString(),
            "bedTime" to today.minusDays(65).atTime(0, 0).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.minusDays(65).atTime(4, 0).atOffset(ZoneOffset.UTC).toString(),
            "mood" to "BAD"
        )

        // POST both logs
        listOf(recentLog, oldLog).forEach { body ->
            mockMvc.post("/api/v1/sleep") {
                header("X-User-Id", userId)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect { status { isCreated() } }
        }

        // --- ASSERTIONS ---
        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            content {
                // Should ONLY count the recent log (10 hours = 36000 seconds)
                // If the old log was included, average would be 7 hours.
                jsonPath("$.avgTotalTimeSeconds") { value(36000) }

                // Mood frequencies should reflect only the recent log
                jsonPath("$.moodFrequencies.GOOD") { value(1) }
                jsonPath("$.moodFrequencies.BAD") { value(0) } // Old log''s BAD is ignored

                // Date range should reflect only the included data
                jsonPath("$.startDate") { value(today.minusDays(5).toString()) }
                jsonPath("$.endDate") { value(today.minusDays(5).toString()) }
            }
        }
    }

    @Test
    fun `get stats circular average handles bed times straddling midnight`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        // Log 1: bed at 23:30, wake at 07:00
        val log1 = mapOf(
            "logDate" to today.minusDays(1).toString(),
            "bedTime" to today.minusDays(1).atTime(23, 30).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.atTime(7, 0).atOffset(ZoneOffset.UTC).toString(),
            "mood" to "GOOD"
        )

        // Log 2: bed at 00:30, wake at 07:00
        // Naive arithmetic mean of 23:30 and 00:30 = 12:00 (wrong)
        // Circular mean = 00:00 (correct)
        val log2 = mapOf(
            "logDate" to today.toString(),
            "bedTime" to today.atTime(0, 30).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.atTime(7, 0).atOffset(ZoneOffset.UTC).toString(),
            "mood" to "OK"
        )

        listOf(log1, log2).forEach { body ->
            mockMvc.post("/api/v1/sleep") {
                header("X-User-Id", userId)
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect { status { isCreated() } }
        }

        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            content {
                // Circular mean of 23:30 and 00:30 = 00:00, not 12:00
                jsonPath("$.avgBedTime") { value(org.hamcrest.Matchers.containsString("00:00")) }

                // Both wake times identical — should be exact
                jsonPath("$.avgWakeTime") { value(org.hamcrest.Matchers.containsString("07:00")) }

                // Average of 7.5h (27000s) and 6.5h (23400s) = 7h (25200s)
                jsonPath("$.avgTotalTimeSeconds") { value(25200) }

                jsonPath("$.moodFrequencies.GOOD") { value(1) }
                jsonPath("$.moodFrequencies.OK") { value(1) }
                jsonPath("$.moodFrequencies.BAD") { value(0) }
            }
        }
    }
}