package com.noom.interview.fullstack.sleep.mapper

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.dto.SleepLogDto
import com.noom.interview.fullstack.sleep.dto.SleepStatsRaw
import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import com.noom.interview.fullstack.sleep.entity.SleepQuality
import com.noom.interview.fullstack.sleep.util.TimeAveragingEngine
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

fun SleepStatsRaw.toResponse(): SleepAnalyticsResponse {
    val avgBed = TimeAveragingEngine.average(bedTimes)
    val avgWake = TimeAveragingEngine.average(wakeTimes)

    return SleepAnalyticsResponse(
        startDate = startDate,
        endDate = endDate,
        avgTotalTimeSeconds = avgSeconds,

        avgBedTime = avgBed,
        avgWakeTime = avgWake,

        moodFrequencies = mapOf(
            SleepQuality.GOOD to countGood,
            SleepQuality.OK   to countOk,
            SleepQuality.BAD  to countBad
        )
    )
}