package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepAnalyticsResponse
import com.noom.interview.fullstack.sleep.dto.SleepLogDto
import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import com.noom.interview.fullstack.sleep.mapper.toDto
import com.noom.interview.fullstack.sleep.mapper.toEntity
import com.noom.interview.fullstack.sleep.repository.SleepRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.util.UUID

@Service
class SleepService(private val sleepRepository: SleepRepository) {
    fun createSleepLog(userId: UUID, sleepLogDto: CreateSleepLogRequest): SleepLogDto {
        // validate: bed time must before wake time
        if(sleepLogDto.bedTime.isAfter(sleepLogDto.wakeTime)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Bed time cannot be after wake time")
        }

        // validate: bed time and wake time difference is within 24 hour
        val duration = Duration.between(sleepLogDto.bedTime, sleepLogDto.wakeTime)
        if (duration.toHours() >= 24) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A single sleep log cannot exceed 24 hours")
        }

        val entity: SleepLogEntity = sleepLogDto.toEntity(userId)
        return sleepRepository.saveSleepLog(userId, entity).toDto()
    }

    fun getMostRecentSleepLog(userId: UUID): SleepLogDto? {
        return sleepRepository.findMostRecentSleepLog(userId)?.toDto()
    }

    fun getThirtyDayStats(userId: UUID): SleepAnalyticsResponse {
        return sleepRepository.getAggregatedStats(userId, 30)
    }
}