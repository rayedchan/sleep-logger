package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepLogDto
import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import com.noom.interview.fullstack.sleep.mapper.toDto
import com.noom.interview.fullstack.sleep.mapper.toEntity
import com.noom.interview.fullstack.sleep.repository.SleepRepository
import org.springframework.stereotype.Service
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
}