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
    fun `test averages for non circular wake times `() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        // Log 1: Wake at 11:00
        val log1 = mapOf(
            "logDate" to today.minusDays(1).toString(),
            "bedTime" to today.minusDays(1).atTime(3, 0).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.minusDays(1).atTime(11, 0).atOffset(ZoneOffset.UTC).toString(),
            "mood" to "GOOD"
        )

        // Log 2: Wake at 13:00
        val log2 = mapOf(
            "logDate" to today.toString(),
            "bedTime" to today.atTime(5, 0).atOffset(ZoneOffset.UTC).toString(),
            "wakeTime" to today.atTime(13, 0).atOffset(ZoneOffset.UTC).toString(),
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
                // Avg duration: both are 8h → 28800
                jsonPath("$.avgTotalTimeSeconds") { value(28800) }

                // Expected midpoint between 11:00 and 13:00 → 12:00Z
                jsonPath("$.avgWakeTime") { value("12:00Z") }

                // Bed times: 03:00 and 05:00 → avg = 04:00Z
                jsonPath("$.avgBedTime") { value("04:00Z") }

                // Mood counts
                jsonPath("$.moodFrequencies.GOOD") { value(1) }
                jsonPath("$.moodFrequencies.OK") { value(1) }
                jsonPath("$.moodFrequencies.BAD") { value(0) }

                // Date range
                jsonPath("$.startDate") { value(today.minusDays(1).toString()) }
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
    fun `test cross-midnight duration is calculated correctly`() {
        val userId = UUID.randomUUID()

        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "logDate"  to "2024-01-15",
                    "bedTime"  to "2024-01-14T21:45:00Z",
                    "wakeTime" to "2024-01-15T06:15:00Z",
                    "mood"     to "OK"
                )
            )
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/sleep/latest") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalDurationSeconds") { value(30600) }
        }
    }

    @Test
    fun `test stats should only aggregate logs from the last 30 days`() {
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
    fun `stats - log exactly 30 days ago is included`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "logDate"  to today.minusDays(30).toString(),
                    "bedTime"  to today.minusDays(30).atTime(22, 0).atOffset(ZoneOffset.UTC).toString(),
                    "wakeTime" to today.minusDays(29).atTime(6, 0).atOffset(ZoneOffset.UTC).toString(),
                    "mood"     to "GOOD"
                )
            )
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }
        }
    }

    @Test
    fun `stats - log 31 days ago is excluded`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        // Old log: 4h sleep with BAD mood — if included it would skew averages
        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "logDate"  to today.minusDays(31).toString(),
                    "bedTime"  to today.minusDays(31).atTime(0, 0).atOffset(ZoneOffset.UTC).toString(),
                    "wakeTime" to today.minusDays(31).atTime(4, 0).atOffset(ZoneOffset.UTC).toString(),
                    "mood"     to "BAD"
                )
            )
        }.andExpect { status { isCreated() } }

        // Recent log: 8h sleep with GOOD mood
        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "logDate"  to today.toString(),
                    "bedTime"  to today.atTime(22, 0).atOffset(ZoneOffset.UTC).toString(),
                    "wakeTime" to today.plusDays(1).atTime(6, 0).atOffset(ZoneOffset.UTC).toString(),
                    "mood"     to "GOOD"
                )
            )
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.avgTotalTimeSeconds") { value(28800) }
            jsonPath("$.moodFrequencies.GOOD") { value(1) }
            jsonPath("$.moodFrequencies.BAD") { value(0) }
            jsonPath("$.startDate") { value(today.toString()) }
            jsonPath("$.endDate") { value(today.toString()) }
        }
    }

    @Test
    fun `test stats when bed times are exactly 12 hours apart`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now(ZoneOffset.UTC)

        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "logDate"  to today.minusDays(1).toString(),
                    "bedTime"  to today.minusDays(1).atTime(22, 0).atOffset(ZoneOffset.UTC).toString(),
                    "wakeTime" to today.atTime(6, 0).atOffset(ZoneOffset.UTC).toString(),
                    "mood"     to "OK"
                )
            )
        }.andExpect { status { isCreated() } }

        mockMvc.post("/api/v1/sleep") {
            header("X-User-Id", userId)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "logDate" to today.toString(),
                    "bedTime" to today.atTime(10, 0).atOffset(ZoneOffset.UTC).toString(),
                    "wakeTime" to today.atTime(16, 0).atOffset(ZoneOffset.UTC).toString(),
                    "mood" to "OK"
                )
            )
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/sleep/stats") {
            header("X-User-Id", userId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.avgBedTime") { value("04:00Z") }
        }
    }
}