package com.noom.interview.fullstack.sleep.dto

import com.noom.interview.fullstack.sleep.entity.SleepQuality
import java.time.LocalDate
import java.time.OffsetTime

data class SleepAnalyticsResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val avgTotalTimeSeconds: Double,
    val avgBedTime: OffsetTime,
    val avgWakeTime: OffsetTime,
    val moodFrequencies: Map<SleepQuality, Int>
)
