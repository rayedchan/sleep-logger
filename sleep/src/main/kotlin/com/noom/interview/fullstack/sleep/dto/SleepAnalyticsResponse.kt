package com.noom.interview.fullstack.sleep.dto

import com.noom.interview.fullstack.sleep.entity.SleepQuality
import java.time.LocalDate
import java.time.LocalTime

data class SleepAnalyticsResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val avgTotalTime: String,
    val avgBedTime: LocalTime,
    val avgWakeTime: LocalTime,
    val moodFrequencies: Map<SleepQuality, Int>
)
