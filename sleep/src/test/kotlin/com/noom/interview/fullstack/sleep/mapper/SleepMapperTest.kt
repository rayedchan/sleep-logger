package com.noom.interview.fullstack.sleep.mapper

import com.noom.interview.fullstack.sleep.dto.SleepStatsRaw
import com.noom.interview.fullstack.sleep.entity.SleepQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SleepMapperTest {
    @Test
    fun `maps raw stats to response correctly`() {
        val raw = SleepStatsRaw(
            avgSeconds = 28800,
            bedTimes = listOf(82800.0, 3600.0),
            wakeTimes = listOf(25200.0, 28800.0),
            countGood = 1,
            countOk = 1,
            countBad = 0,
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 4, 2)
        )

        val result = raw.toResponse()

        assertEquals(28800, result.avgTotalTimeSeconds)
        assertEquals(1, result.moodFrequencies[SleepQuality.GOOD])
        assertEquals(LocalDate.of(2026, 4, 1), result.startDate)
    }
}