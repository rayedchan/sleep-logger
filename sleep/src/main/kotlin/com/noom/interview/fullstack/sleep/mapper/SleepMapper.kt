package com.noom.interview.fullstack.sleep.mapper

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepLogDto
import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import java.util.UUID

fun CreateSleepLogRequest.toEntity(userId: UUID): SleepLogEntity {
    return SleepLogEntity(
        userId = userId,
        logDate = this.logDate,
        bedTime = this.bedTime,
        wakeTime = this.wakeTime,
        mood = this.mood
    )
}

fun SleepLogEntity.toDto(): SleepLogDto {
    return SleepLogDto(
        id = this.id!!,
        logDate = this.logDate,
        bedTime = this.bedTime,
        wakeTime = this.wakeTime,
        mood = this.mood,
        totalDurationSeconds = this.totalDurationSeconds!!
    )
}