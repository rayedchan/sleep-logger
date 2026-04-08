package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.dto.SleepLogDto
import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import com.noom.interview.fullstack.sleep.entity.SleepQuality
import com.noom.interview.fullstack.sleep.mapper.toDto
import com.noom.interview.fullstack.sleep.mapper.toEntity
import com.noom.interview.fullstack.sleep.repository.SleepRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

@Service
class SleepService(private val sleepRepository: SleepRepository) {
    fun createSleepLog(userId: UUID, sleepLogDto: CreateSleepLogRequest): SleepLogDto {
        val entity: SleepLogEntity = sleepLogDto.toEntity(userId)
        val newlyCreatedEntity: SleepLogEntity = sleepRepository.saveSleepLog(userId, entity)
        return newlyCreatedEntity.toDto()
    }

    fun getMostRecentSleepLog(userId: UUID): SleepLogDto? {
       return sleepRepository.findMostRecentSleepLog(userId)?.toDto()
    }

    fun getThirtyDayStats(userId: UUID): SleepAnalyticsResponse {
        val data = sleepRepository.getAggregatedStats(userId, 30)

        // If the database returns null for averages, the user has no logs in this range
        val avgSeconds = (data["avg_seconds"] as? Number)?.toDouble()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No sleep data found for the last 30 days")

        val duration = Duration.ofSeconds(avgSeconds.toLong())

        // Map the raw database results into our clean Response DTO
        return SleepAnalyticsResponse(
            startDate = (data["range_start"] as java.sql.Date).toLocalDate(),
            endDate = (data["range_end"] as java.sql.Date).toLocalDate(),
            avgTotalTime = formatDuration(duration),
            avgBedTime = LocalTime.parse(data["avg_start"].toString()),
            avgWakeTime = LocalTime.parse(data["avg_end"].toString()),
            moodFrequencies = mapOf(
                SleepQuality.GOOD to (data["count_good"] as Long).toInt(),
                SleepQuality.OK to (data["count_ok"] as Long).toInt(),
                SleepQuality.BAD to (data["count_bad"] as Long).toInt()
            )
        )
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        return "${hours}h ${minutes}m"
    }
}