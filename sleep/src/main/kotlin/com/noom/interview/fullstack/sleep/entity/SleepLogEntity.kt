package com.noom.interview.fullstack.sleep.entity

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class SleepLogEntity(
    val id: Int? = null,
    val userId: UUID,
    val logDate: LocalDate,
    val bedTime: OffsetDateTime,
    val wakeTime: OffsetDateTime,
    val mood: SleepQuality,
    val totalDurationSeconds: Long? = null
)
