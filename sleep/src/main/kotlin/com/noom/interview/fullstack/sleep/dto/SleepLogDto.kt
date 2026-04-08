package com.noom.interview.fullstack.sleep.dto

import com.noom.interview.fullstack.sleep.entity.SleepQuality
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class SleepLogDto (
    val id: Int,
    val userId: UUID,
    val logDate: LocalDate,
    val bedTime: OffsetDateTime,
    val wakeTime: OffsetDateTime,
    val mood: SleepQuality,
    val totalDuration: String
)