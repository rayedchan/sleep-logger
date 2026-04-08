package com.noom.interview.fullstack.sleep.dto

import com.noom.interview.fullstack.sleep.entity.SleepQuality
import java.time.LocalDate
import java.time.OffsetDateTime

data class CreateSleepLogRequest(
    val logDate: LocalDate,
    val bedTime: OffsetDateTime,
    val wakeTime: OffsetDateTime,
    val mood: SleepQuality
)